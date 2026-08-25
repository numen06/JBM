from __future__ import annotations

from typing import Any, Mapping

from fastapi import APIRouter, Query, Request

from jbm_cluster_py.common.masterdata import first_payload, page_form_from_body
from jbm_cluster_py.common.result import ok
from jbm_cluster_py.platform.center.modules.governance.application.service import GovernanceService
from jbm_cluster_py.platform.center.modules.governance.application.access import (
    is_platform,
    tenant_id,
)


def build_governance_router(service: GovernanceService) -> APIRouter:
    router = APIRouter()

    @router.get("/current/user")
    async def current_user(request: Request) -> dict[str, Any]:
        return ok(await service.current_user(request.state.identity))

    @router.get("/current/user/menus")
    async def current_menus(request: Request) -> dict[str, Any]:
        return ok(await service.current_menus(request.state.identity))

    @router.get("/current/dashboard")
    async def dashboard(request: Request) -> dict[str, Any]:
        user = await service.current_user(request.state.identity)
        menus = await service.current_menus(request.state.identity)
        platform_scope = is_platform(request.state.identity)
        counts = await service.repository.dashboard_counts(
            None if platform_scope else tenant_id(request.state.identity)
        )
        return ok(
            {
                "identity": {
                    "userId": user.get("userId"),
                    "userName": user.get("userName"),
                    "nickName": user.get("nickName"),
                    "roles": [row.get("roleCode") for row in user.get("roles") or []],
                    "appId": request.state.identity.get("appId"),
                    "visibleMenuCount": sum(1 for _ in _walk(menus)),
                    "scope": "platform" if platform_scope else "tenant",
                    "tenantId": request.state.identity.get("tenantId"),
                },
                "sections": {
                    key: platform_scope or key in {"system", "api"}
                    for key in ("system", "authority", "api", "gateway", "developer", "audit")
                },
                "metrics": {
                    "usersTotal": counts.get("userTotal", 0),
                    "onlineUser": 0,
                    "appCount": counts.get("appTotal", 0),
                    "orgCount": counts.get("orgTotal", 0),
                    "roleCount": counts.get("roleTotal", 0),
                    "authorityResourceCount": counts.get("authorityTotal", 0),
                    "apiCount": counts.get("apiTotal", 0),
                    "apiKeyCount": counts.get("apiKeyTotal", 0),
                },
                "risks": [],
            }
        )

    @router.get("/user")
    async def users(
        request: Request,
        keyword: str | None = None,
        page: int = Query(1, alias="pageForm.currPage"),
        size: int = Query(10, alias="pageForm.pageSize"),
        status: int | None = None,
        company_id: int | None = Query(None, alias="companyId"),
    ) -> dict[str, Any]:
        return ok(
            await service.users(
                page,
                size,
                keyword,
                {"status": status, "companyId": company_id},
                request.state.identity,
            )
        )

    @router.get("/user/{user_id}")
    async def user(user_id: int, request: Request) -> dict[str, Any]:
        return ok(await service.user(user_id, request.state.identity))

    @router.get("/user/{user_id}/roles")
    async def user_roles(user_id: int, request: Request) -> dict[str, Any]:
        user = await service.user(user_id, request.state.identity) or {}
        target_tenant_id = (
            int(user.get("companyId") or tenant_id(request.state.identity))
            if is_platform(request.state.identity)
            else tenant_id(request.state.identity)
        )
        return ok(
            await service.repository.user_roles(
                user_id,
                _identity_app_id(request.state.identity),
                target_tenant_id,
            )
        )

    @router.get("/user/{user_id}/orgs")
    async def user_orgs(user_id: int, request: Request) -> dict[str, Any]:
        await service.user(user_id, request.state.identity)
        return ok(await service.repository.user_orgs(user_id))

    @router.get("/user/{user_id}/accounts")
    async def user_accounts(user_id: int, request: Request) -> dict[str, Any]:
        await service.user(user_id, request.state.identity)
        return ok(await service.repository.user_accounts(user_id))

    @router.post("/baseOrg/root")
    async def org_roots(request: Request) -> dict[str, Any]:
        return ok(await service.org_roots(request.state.identity))

    @router.post("/baseOrg/tree")
    async def org_tree(request: Request, body: dict[str, Any] | None = None) -> dict[str, Any]:
        entity = first_payload(body, ("baseOrg", "masterData"))
        value = entity.get("id") or entity.get("orgId")
        return ok(await service.org_tree(request.state.identity, int(value) if value is not None else None))

    @router.post("/baseOrg/pageList")
    async def org_page(request: Request, body: dict[str, Any] | None = None) -> dict[str, Any]:
        page_form = page_form_from_body(body)
        entity = first_payload(body, ("baseOrg", "masterData"))
        return ok(
            await service.org_page(
                page_form.curr_page,
                page_form.page_size,
                str(entity.get("orgName") or "").strip() or None,
                request.state.identity,
            )
        )

    @router.post("/baseDic/root")
    async def dict_roots() -> dict[str, Any]:
        return ok(await service.dict_roots())

    @router.post("/baseDic/list")
    async def dict_list() -> dict[str, Any]:
        rows = await service.dict_roots()
        for row in rows:
            row["children"] = await service.repository.list_dicts(int(row["id"]))
        return ok(rows)

    @router.post("/baseDic/root/pageList")
    async def dict_root_page(body: dict[str, Any] | None = None) -> dict[str, Any]:
        return ok(await _dict_page(service, body, None))

    @router.post("/baseDic/items/pageList")
    async def dict_items_page(body: dict[str, Any] | None = None) -> dict[str, Any]:
        entity = first_payload(body, ("baseDic", "masterData"))
        parent_id = entity.get("parentId")
        if parent_id is None:
            raise ValueError("缺少字典分组 parentId")
        return ok(await _dict_page(service, body, int(parent_id)))

    @router.get("/baseDic/getDicMap")
    async def dict_map() -> dict[str, Any]:
        return ok(await service.dict_map())

    @router.get("/app")
    async def apps(
        request: Request,
        page: int = Query(1, alias="pageForm.currPage"),
        size: int = Query(10, alias="pageForm.pageSize"),
        app_name: str | None = Query(None, alias="appName"),
        code: str | None = None,
        status: int | None = None,
        org_id: int | None = Query(None, alias="orgId"),
        app_type: str | None = Query(None, alias="appType"),
    ) -> dict[str, Any]:
        filters = {
            "appName": app_name,
            "code": code,
            "status": status,
            "orgId": org_id,
            "appType": app_type,
        }
        return ok(await service.apps(page, size, filters, request.state.identity))

    @router.get("/role")
    async def roles(
        request: Request,
        page: int = Query(1, alias="pageForm.currPage"),
        size: int = Query(10, alias="pageForm.pageSize"),
        role_name: str | None = Query(None, alias="roleName"),
        role_code: str | None = Query(None, alias="roleCode"),
        app_id: int | None = Query(None, alias="appId"),
        status: int | None = None,
    ) -> dict[str, Any]:
        filters = {
            "roleName": role_name,
            "roleCode": role_code,
            "status": status,
            "appId": app_id,
        }
        return ok(await service.roles(page, size, filters, request.state.identity))

    @router.get("/role/all")
    async def all_roles(request: Request) -> dict[str, Any]:
        if not is_platform(request.state.identity):
            rows, _ = await service.repository.list_roles(
                1, 100, {"appId": _identity_app_id(request.state.identity)}
            )
            return ok(
                [
                    row
                    for row in rows
                    if row.get("roleCode")
                    not in {"super_admin", "platform_operator", "iot_operator"}
                ]
            )
        page = await service.roles(1, 100, {}, request.state.identity)
        return ok(page["contents"])

    @router.get("/gateway/routes")
    async def routes(
        request: Request,
        page: int = Query(1, alias="pageForm.currPage"),
        size: int = Query(10, alias="pageForm.pageSize"),
        route_name: str | None = Query(None, alias="routeName"),
        path: str | None = None,
        service_id: str | None = Query(None, alias="serviceId"),
        status: int | None = None,
    ) -> dict[str, Any]:
        filters = {"routeName": route_name, "path": path, "serviceId": service_id, "status": status}
        return ok(await service.routes(page, size, filters, request.state.identity))

    @router.get("/delegations/check")
    async def check_delegation(
        request: Request,
        owner_tenant_id: int = Query(..., alias="ownerTenantId"),
        permission: str = Query(...),
        resource_type: str | None = Query(None, alias="resourceType"),
    ) -> dict[str, Any]:
        return ok(
            await service.check_delegation(
                request.state.identity,
                owner_tenant_id,
                permission,
                resource_type,
            )
        )

    @router.get("/tenant-features/context")
    async def tenant_feature_context(request: Request) -> dict[str, Any]:
        return ok(await service.feature_context(request.state.identity))

    @router.post("/tenant-features/catalog")
    async def create_tenant_feature_catalog(
        request: Request, body: dict[str, Any]
    ) -> dict[str, Any]:
        return ok(
            await service.create_app_feature(
                request.state.identity,
                str(body.get("featureCode") or ""),
                str(body.get("featureName") or ""),
                str(body.get("featureDesc") or "").strip() or None,
            )
        )

    @router.delete("/tenant-features/catalog/{feature_code:path}")
    async def disable_tenant_feature_catalog(
        feature_code: str, request: Request
    ) -> dict[str, Any]:
        await service.disable_app_feature(request.state.identity, feature_code)
        return ok()

    @router.put("/tenant-features/tenants/{target_tenant_id}")
    async def grant_tenant_features(
        target_tenant_id: int, request: Request, body: dict[str, Any]
    ) -> dict[str, Any]:
        return ok(
            await service.replace_tenant_features(
                request.state.identity,
                target_tenant_id,
                [str(value) for value in body.get("featureCodes") or []],
            )
        )

    @router.put("/tenant-features/members/{target_user_id}")
    async def grant_member_features(
        target_user_id: int, request: Request, body: dict[str, Any]
    ) -> dict[str, Any]:
        return ok(
            await service.replace_member_features(
                request.state.identity,
                target_user_id,
                [str(value) for value in body.get("featureCodes") or []],
            )
        )

    return router


def _identity_app_id(identity: Mapping[str, Any]) -> int | None:
    value = identity.get("appId", identity.get("app_id"))
    try:
        return int(value) if value is not None else None
    except (TypeError, ValueError):
        return None


async def _dict_page(service: GovernanceService, body: dict[str, Any] | None, parent_id: int | None) -> dict[str, Any]:
    page_form = page_form_from_body(body)
    entity = first_payload(body, ("baseDic", "masterData"))
    keyword = str(entity.get("name") or entity.get("code") or entity.get("remark") or "").strip() or None
    return await service.dict_page(parent_id, page_form.curr_page, page_form.page_size, keyword)


def _walk(nodes: list[dict[str, Any]]):
    for node in nodes:
        yield node
        yield from _walk(node.get("children") or [])
