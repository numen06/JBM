from __future__ import annotations

from collections.abc import Mapping
from typing import Annotated, Any

from fastapi import APIRouter, Body, File, Form, HTTPException, Request, UploadFile
from fastapi.responses import RedirectResponse
from jbm_cluster_py.common.result import ok
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository
from jbm_cluster_py.platform.bigscreen.service import BigscreenService


def build_bigscreen_router(repository: BigscreenRepository, service: BigscreenService) -> APIRouter:
    router = APIRouter()

    @router.get("/view/{view}", tags=["大屏预览"])
    async def view(view: str) -> RedirectResponse:
        return RedirectResponse(f"/static/{service._view_key(view)}/index.html")

    @router.post("/bigscreenView/pageList", tags=["大屏管理"])
    async def page_list(
        request: Request, body: Annotated[dict[str, Any] | None, Body()] = None
    ) -> dict[str, Any]:
        page = await repository.page(
            body or {}, tenant_id=_tenant_scope(request), project_id=_project_scope(request)
        )
        for row in page["contents"]:
            row.update(service.deployment_status(row))
        return ok(page, "查询分页列表成功")

    @router.post("/bigscreenView/list", tags=["大屏管理"])
    async def list_rows(
        request: Request, body: Annotated[dict[str, Any] | None, Body()] = None
    ) -> dict[str, Any]:
        page = await repository.page(
            body or {}, True, tenant_id=_tenant_scope(request), project_id=_project_scope(request)
        )
        return ok(page["contents"], "查询列表成功")

    @router.post("/bigscreenView/model", tags=["大屏管理"])
    async def model(request: Request, body: dict[str, Any]) -> dict[str, Any]:
        view_id = str(body.get("id") or "")
        row = await repository.get(view_id, _tenant_scope(request))
        if not row and _is_platform(_identity(request)):
            row = await repository.get(view_id)
        if row:
            row.update(service.deployment_status(row))
        return ok(row, "查询对象成功")

    @router.post("/bigscreenView/save", tags=["大屏管理"])
    async def save(request: Request, body: dict[str, Any]) -> dict[str, Any]:
        _require_manager(request)
        return ok(
            await service.save(body, tenant_id=_tenant_scope(request), user_id=_user_id(request)),
            "保存对象成功",
        )

    @router.post("/bigscreenView/saveBatch", tags=["大屏管理"])
    async def save_batch(request: Request, body: Annotated[Any, Body()]) -> dict[str, Any]:
        _require_manager(request)
        values = body if isinstance(body, list) else body.get("list") or body.get("contents") or []
        tenant = _tenant_scope(request)
        user = _user_id(request)
        return ok(
            [await service.save(item, tenant_id=tenant, user_id=user) for item in values],
            "保存对象成功",
        )

    @router.post("/bigscreenView/delete", tags=["大屏管理"])
    async def delete(request: Request, body: dict[str, Any]) -> dict[str, Any]:
        _require_manager(request)
        return ok(
            await service.delete(str(body.get("id") or ""), _tenant_scope(request)),
            "删除对象成功",
        )

    @router.post("/bigscreenView/deleteByIds", tags=["大屏管理"])
    async def delete_ids(request: Request, body: dict[str, Any]) -> dict[str, Any]:
        _require_manager(request)
        tenant = _tenant_scope(request)
        for view_id in body.get("ids") or []:
            await service.delete(str(view_id), tenant)
        return ok(True, "批量成功删除")

    @router.post("/bigscreenView/upload", tags=["大屏管理"])
    async def upload(request: Request, body: dict[str, Any]) -> dict[str, Any]:
        _require_manager(request)
        return ok(await service.upload(body, _tenant_scope(request)), "上载大屏包成功")

    @router.post("/bigscreenView/package", tags=["大屏管理"])
    async def upload_package(
        request: Request,
        package: Annotated[UploadFile, File()],
        view_name: Annotated[str, Form(alias="viewName")],
        view_id: Annotated[str | None, Form(alias="id")] = None,
        project_id: Annotated[str | None, Form(alias="projectId")] = None,
        app_id: Annotated[str | None, Form(alias="appId")] = None,
    ) -> dict[str, Any]:
        _require_manager(request)
        body = {
            "id": view_id,
            "viewName": view_name,
            "projectId": project_id or _project_scope(request),
            "appId": app_id or _identity(request).get("appId"),
        }
        try:
            result = await service.upload_package(
                package, body, _tenant_scope(request), _user_id(request)
            )
        finally:
            await package.close()
        return ok(result, "大屏包部署成功")

    @router.post("/bigscreenView/isUpload", tags=["大屏管理"])
    async def is_upload(request: Request, body: dict[str, Any]) -> dict[str, Any]:
        return ok(await service.is_uploaded(body, _tenant_scope(request)), "判断成功")

    @router.post("/bigscreenView/reload", tags=["大屏管理"])
    async def reload_view(request: Request, body: dict[str, Any]) -> dict[str, Any]:
        _require_manager(request)
        return ok(await service.reload(body, _tenant_scope(request)), "重新加载大屏成功")

    @router.post("/bigscreenView/cleanView", tags=["大屏管理"])
    async def clean(request: Request, body: dict[str, Any]) -> dict[str, Any]:
        _require_manager(request)
        return ok(await service.clean(body, _tenant_scope(request)), "清理视图成功")

    return router


def _identity(request: Request) -> Mapping[str, Any]:
    value = getattr(request.state, "identity", None)
    if not isinstance(value, Mapping):
        raise HTTPException(status_code=401, detail="未提供访问令牌")
    return value


def _role_codes(identity: Mapping[str, Any]) -> set[str]:
    result: set[str] = set()
    for role in identity.get("roles") or []:
        if isinstance(role, Mapping):
            value = role.get("roleCode") or role.get("code") or role.get("roleName")
        else:
            value = role
        if value:
            result.add(str(value).lower())
    return result


def _is_platform(identity: Mapping[str, Any]) -> bool:
    return (
        bool(identity.get("admin"))
        or str(identity.get("username") or identity.get("userName") or "").lower() == "admin"
        or bool(_role_codes(identity) & {"super_admin", "platform_operator"})
    )


def _tenant_scope(request: Request) -> str:
    identity = _identity(request)
    own = str(identity.get("tenantId") or identity.get("tenant_id") or "").strip()
    requested = str(request.headers.get("x-tenant-id") or "").strip()
    value = requested if requested and _is_platform(identity) else own
    if not value or value == "0":
        raise HTTPException(status_code=403, detail="登录账号未绑定租户")
    return value


def _project_scope(request: Request) -> str | None:
    return str(request.headers.get("x-project-id") or "").strip() or None


def _user_id(request: Request) -> str:
    identity = _identity(request)
    value = identity.get("userId") or identity.get("user_id") or identity.get("sub")
    if value in (None, ""):
        raise HTTPException(status_code=403, detail="登录信息缺少用户标识")
    return str(value).split("::", 1)[0]


def _require_manager(request: Request) -> None:
    identity = _identity(request)
    if _is_platform(identity) or _role_codes(identity) & {
        "tenant_admin",
        "iot_admin",
        "building_admin",
    }:
        return
    raise HTTPException(status_code=403, detail="仅平台或租户管理员可管理大屏")
