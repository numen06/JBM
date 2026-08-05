from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Query, Request

from jbm_cluster_py.common.masterdata import first_payload, page_form_from_body
from jbm_cluster_py.common.result import ok
from jbm_cluster_py.platform.center.modules.governance.application.service import GovernanceService


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
        counts = await service.repository.dashboard_counts()
        return ok(
            {
                "identity": {
                    "userId": user.get("userId"),
                    "userName": user.get("userName"),
                    "nickName": user.get("nickName"),
                    "roles": [row.get("roleCode") for row in user.get("roles") or []],
                    "appId": request.state.identity.get("appId"),
                    "visibleMenuCount": sum(1 for _ in _walk(menus)),
                    "scope": "platform",
                },
                "sections": {key: True for key in ("system", "authority", "api", "gateway", "developer", "audit")},
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
        keyword: str | None = None,
        page: int = Query(1, alias="pageForm.currPage"),
        size: int = Query(10, alias="pageForm.pageSize"),
        status: int | None = None,
        company_id: int | None = Query(None, alias="companyId"),
    ) -> dict[str, Any]:
        return ok(await service.users(page, size, keyword, {"status": status, "companyId": company_id}))

    @router.get("/user/{user_id}")
    async def user(user_id: int) -> dict[str, Any]:
        return ok(await service.repository.get_user(user_id))

    @router.get("/user/{user_id}/roles")
    async def user_roles(user_id: int) -> dict[str, Any]:
        return ok(await service.repository.user_roles(user_id))

    @router.get("/user/{user_id}/orgs")
    async def user_orgs(user_id: int) -> dict[str, Any]:
        return ok(await service.repository.user_orgs(user_id))

    @router.get("/user/{user_id}/accounts")
    async def user_accounts(user_id: int) -> dict[str, Any]:
        return ok(await service.repository.user_accounts(user_id))

    @router.post("/baseOrg/root")
    async def org_roots() -> dict[str, Any]:
        return ok(await service.org_roots())

    @router.post("/baseOrg/tree")
    async def org_tree(body: dict[str, Any] | None = None) -> dict[str, Any]:
        entity = first_payload(body, ("baseOrg", "masterData"))
        value = entity.get("id") or entity.get("orgId")
        return ok(await service.org_tree(int(value) if value is not None else None))

    @router.post("/baseOrg/pageList")
    async def org_page(body: dict[str, Any] | None = None) -> dict[str, Any]:
        page_form = page_form_from_body(body)
        entity = first_payload(body, ("baseOrg", "masterData"))
        return ok(
            await service.org_page(
                page_form.curr_page, page_form.page_size, str(entity.get("orgName") or "").strip() or None
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
        return ok(await service.apps(page, size, filters))

    @router.get("/role")
    async def roles(
        page: int = Query(1, alias="pageForm.currPage"),
        size: int = Query(10, alias="pageForm.pageSize"),
        role_name: str | None = Query(None, alias="roleName"),
        role_code: str | None = Query(None, alias="roleCode"),
        status: int | None = None,
    ) -> dict[str, Any]:
        filters = {"roleName": role_name, "roleCode": role_code, "status": status}
        return ok(await service.roles(page, size, filters))

    @router.get("/role/all")
    async def all_roles() -> dict[str, Any]:
        page = await service.roles(1, 100, {})
        return ok(page["contents"])

    @router.get("/gateway/routes")
    async def routes(
        page: int = Query(1, alias="pageForm.currPage"),
        size: int = Query(10, alias="pageForm.pageSize"),
        route_name: str | None = Query(None, alias="routeName"),
        path: str | None = None,
        service_id: str | None = Query(None, alias="serviceId"),
        status: int | None = None,
    ) -> dict[str, Any]:
        filters = {"routeName": route_name, "path": path, "serviceId": service_id, "status": status}
        return ok(await service.routes(page, size, filters))

    return router


async def _dict_page(
    service: GovernanceService, body: dict[str, Any] | None, parent_id: int | None
) -> dict[str, Any]:
    page_form = page_form_from_body(body)
    entity = first_payload(body, ("baseDic", "masterData"))
    keyword = str(entity.get("name") or entity.get("code") or entity.get("remark") or "").strip() or None
    return await service.dict_page(parent_id, page_form.curr_page, page_form.page_size, keyword)


def _walk(nodes: list[dict[str, Any]]):
    for node in nodes:
        yield node
        yield from _walk(node.get("children") or [])
