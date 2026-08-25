from __future__ import annotations

from typing import Any, Mapping

from jbm_cluster_py.common.masterdata import PageForm, java_page
from jbm_cluster_py.platform.center.modules.governance.application.access import (
    is_platform,
    is_tenant_admin,
    require_platform,
    require_tenant_admin,
    require_tenant_record,
    tenant_id,
)
from jbm_cluster_py.platform.center.modules.governance.domain.ports import GovernanceRepository


class GovernanceService:
    def __init__(self, repository: GovernanceRepository) -> None:
        self.repository = repository

    async def users(
        self,
        page: int,
        size: int,
        keyword: str | None,
        filters: Mapping[str, Any],
        identity: Mapping[str, Any],
    ) -> dict[str, Any]:
        if not is_platform(identity):
            require_tenant_admin(identity)
        scoped = dict(filters)
        if not is_platform(identity):
            scoped.pop("companyId", None)
            scoped["tenantId"] = tenant_id(identity)
        rows, total = await self.repository.list_users(page, size, keyword, scoped)
        return java_page(rows, total, PageForm(currPage=page, pageSize=size))

    async def user(self, user_id: int, identity: Mapping[str, Any]) -> dict[str, Any] | None:
        row = await self.repository.get_user(user_id)
        if not is_platform(identity) and not await self.repository.is_user_member(
            user_id, tenant_id(identity)
        ):
            require_tenant_record(identity, None, "companyId")
        return row

    async def current_user(self, identity: Mapping[str, Any]) -> dict[str, Any]:
        user_id = _identity_int(identity, "userId", "user_id", "sub")
        if user_id is None:
            raise ValueError("登录信息缺少 userId")
        user = await self.repository.get_user(user_id)
        if user is None:
            raise ValueError("用户不存在")
        is_admin = _is_admin(user, identity)
        app_id = _identity_int(identity, "appId", "app_id")
        active_tenant = tenant_id(identity)
        user["roles"] = await self.repository.user_roles(user_id, app_id, active_tenant)
        user["authorities"] = await self.repository.user_authorities(
            user_id, is_admin, app_id, active_tenant
        )
        return user

    async def current_menus(
        self,
        identity: Mapping[str, Any],
        requested_app_id: int | None = None,
        *,
        tree: bool = True,
    ) -> list[dict[str, Any]]:
        user_id = _identity_int(identity, "userId", "user_id", "sub")
        if user_id is None:
            raise ValueError("登录信息缺少 userId")
        app_id = _identity_int(identity, "appId", "app_id")
        if requested_app_id is not None:
            require_platform(identity)
            app_id = requested_app_id
        user = await self.repository.get_user(user_id) or {}
        rows = await self.repository.user_menus(
            user_id, app_id, _is_admin(user, identity), tenant_id(identity)
        )
        return _tree(rows, "menuId") if tree else rows

    async def org_roots(self, identity: Mapping[str, Any]) -> list[dict[str, Any]]:
        rows = await self._orgs(identity)
        if is_platform(identity):
            return [row for row in rows if not row.get("parentId")]
        root = tenant_id(identity)
        return [row for row in rows if int(row.get("id") or 0) == root]

    async def org_tree(self, identity: Mapping[str, Any], root_id: int | None = None) -> list[dict[str, Any]]:
        rows = await self._orgs(identity)
        tree = _tree(rows, "id")
        if not is_platform(identity):
            root_id = tenant_id(identity)
        if root_id is None:
            return tree
        return [node for node in _walk(tree) if str(node.get("id")) == str(root_id)]

    async def org_page(self, page: int, size: int, keyword: str | None, identity: Mapping[str, Any]) -> dict[str, Any]:
        rows = await self._orgs(identity, keyword)
        start = (page - 1) * size
        return java_page(rows[start : start + size], len(rows), PageForm(currPage=page, pageSize=size))

    async def dict_roots(self) -> list[dict[str, Any]]:
        return await self.repository.list_dicts(None)

    async def dict_page(self, parent_id: int | None, page: int, size: int, keyword: str | None) -> dict[str, Any]:
        rows = await self.repository.list_dicts(parent_id)
        if keyword:
            needle = keyword.lower()
            rows = [
                row
                for row in rows
                if needle in str(row.get("code") or "").lower()
                or needle in str(row.get("name") or "").lower()
                or needle in str(row.get("remark") or "").lower()
            ]
        start = (page - 1) * size
        return java_page(rows[start : start + size], len(rows), PageForm(currPage=page, pageSize=size))

    async def dict_map(self) -> dict[str, list[dict[str, Any]]]:
        result: dict[str, list[dict[str, Any]]] = {}
        for root in await self.dict_roots():
            result[str(root.get("code") or "")] = await self.repository.list_dicts(int(root["id"]))
        return result

    async def apps(
        self, page: int, size: int, filters: Mapping[str, Any], identity: Mapping[str, Any]
    ) -> dict[str, Any]:
        scoped = dict(filters)
        if not is_platform(identity):
            scoped["orgId"] = tenant_id(identity)
        rows, total = await self.repository.list_apps(page, size, scoped)
        return java_page(rows, total, PageForm(currPage=page, pageSize=size))

    async def roles(
        self, page: int, size: int, filters: Mapping[str, Any], identity: Mapping[str, Any]
    ) -> dict[str, Any]:
        require_platform(identity)
        rows, total = await self.repository.list_roles(page, size, filters)
        return java_page(rows, total, PageForm(currPage=page, pageSize=size))

    async def routes(
        self, page: int, size: int, filters: Mapping[str, Any], identity: Mapping[str, Any]
    ) -> dict[str, Any]:
        require_platform(identity)
        rows, total = await self.repository.list_routes(page, size, filters)
        return java_page(rows, total, PageForm(currPage=page, pageSize=size))

    async def check_delegation(
        self,
        identity: Mapping[str, Any],
        owner_tenant_id: int,
        permission: str,
        resource_type: str | None = None,
    ) -> dict[str, Any]:
        operator_tenant_id = tenant_id(identity)
        operator_user_id = _identity_int(identity, "userId", "user_id", "sub")
        app_id = _identity_int(identity, "appId", "app_id")
        if app_id is None or operator_user_id is None or not permission:
            return {"allowed": False}
        grant = await self.repository.find_tenant_delegation(
            owner_tenant_id,
            operator_tenant_id,
            operator_user_id,
            app_id,
            permission,
            resource_type,
        )
        if grant is None:
            return {"allowed": False}
        return {
            "allowed": True,
            "delegationId": grant.get("id"),
            "actorTenantId": operator_tenant_id,
            "resourceTenantId": owner_tenant_id,
            "appId": app_id,
            "dataScope": grant.get("dataScope"),
            "fieldPolicy": grant.get("fieldPolicy"),
            "validTo": grant.get("validTo"),
        }

    async def feature_context(self, identity: Mapping[str, Any]) -> dict[str, Any]:
        app_id = _identity_int(identity, "appId", "app_id")
        user_id = _identity_int(identity, "userId", "user_id", "sub")
        if app_id is None or user_id is None:
            raise ValueError("登录信息缺少应用或用户标识")
        active_tenant = tenant_id(identity)
        platform = is_platform(identity)
        tenant_admin = is_tenant_admin(identity)
        context: dict[str, Any] = {
            "appId": str(app_id),
            "tenantId": str(active_tenant),
            "userId": str(user_id),
            "platform": platform,
            "tenantAdmin": tenant_admin,
            "catalog": await self.repository.list_app_features(app_id),
            "tenantFeatures": await self.repository.list_tenant_features(active_tenant, app_id),
            "effectiveFeatureCodes": await self.repository.effective_user_features(
                user_id, active_tenant, app_id, tenant_admin
            ),
        }
        if platform:
            context["tenants"] = await self.repository.list_feature_tenants(app_id)
        if tenant_admin and not platform:
            context["members"] = await self.repository.list_tenant_members(active_tenant, app_id)
        return context

    async def create_app_feature(
        self, identity: Mapping[str, Any], feature_code: str, feature_name: str, feature_desc: str | None
    ) -> dict[str, Any]:
        require_platform(identity)
        app_id = _identity_int(identity, "appId", "app_id")
        code = feature_code.strip().lower()
        name = feature_name.strip()
        if app_id is None:
            raise ValueError("登录信息缺少应用标识")
        if not code or not name:
            raise ValueError("功能编码和名称不能为空")
        if not code.replace(".", "").replace("_", "").replace("-", "").isalnum():
            raise ValueError("功能编码只能包含字母、数字、点、下划线和短横线")
        return await self.repository.create_app_feature(app_id, code, name, feature_desc)

    async def disable_app_feature(
        self, identity: Mapping[str, Any], feature_code: str
    ) -> None:
        require_platform(identity)
        app_id = _identity_int(identity, "appId", "app_id")
        if app_id is None:
            raise ValueError("登录信息缺少应用标识")
        await self.repository.disable_app_feature(app_id, feature_code.strip().lower())

    async def replace_tenant_features(
        self, identity: Mapping[str, Any], target_tenant_id: int, feature_codes: list[str]
    ) -> list[str]:
        require_platform(identity)
        app_id = _identity_int(identity, "appId", "app_id")
        user_id = _identity_int(identity, "userId", "user_id", "sub")
        if app_id is None or user_id is None:
            raise ValueError("登录信息缺少应用或用户标识")
        return await self.repository.replace_tenant_features(
            target_tenant_id, app_id, feature_codes, user_id
        )

    async def replace_member_features(
        self, identity: Mapping[str, Any], target_user_id: int, feature_codes: list[str]
    ) -> list[str]:
        require_tenant_admin(identity)
        if is_platform(identity):
            raise ValueError("平台管理员应先切换到明确租户后再分配子账号权限")
        app_id = _identity_int(identity, "appId", "app_id")
        user_id = _identity_int(identity, "userId", "user_id", "sub")
        if app_id is None or user_id is None:
            raise ValueError("登录信息缺少应用或用户标识")
        return await self.repository.replace_member_features(
            tenant_id(identity), app_id, target_user_id, feature_codes, user_id
        )

    async def _orgs(self, identity: Mapping[str, Any], keyword: str | None = None) -> list[dict[str, Any]]:
        rows = await self.repository.list_orgs(keyword)
        if is_platform(identity):
            return rows
        allowed = {tenant_id(identity)}
        changed = True
        while changed:
            before = len(allowed)
            allowed.update(int(row["id"]) for row in rows if row.get("parentId") in allowed)
            changed = len(allowed) != before
        return [row for row in rows if int(row.get("id") or 0) in allowed]


def _identity_int(identity: Mapping[str, Any], *keys: str) -> int | None:
    for key in keys:
        value = identity.get(key)
        if value is None:
            continue
        try:
            return int(str(value).split("::", 1)[0])
        except ValueError:
            continue
    return None


def _is_admin(user: Mapping[str, Any], identity: Mapping[str, Any]) -> bool:
    return (
        int(user.get("userId") or 0) == 1 or str(user.get("userName") or "") == "admin" or bool(identity.get("admin"))
    )


def _tree(rows: list[dict[str, Any]], id_key: str) -> list[dict[str, Any]]:
    nodes = {str(row[id_key]): {**row, "children": []} for row in rows if row.get(id_key) is not None}
    roots: list[dict[str, Any]] = []
    for node in nodes.values():
        parent = nodes.get(str(node.get("parentId"))) if node.get("parentId") else None
        if parent is None:
            roots.append(node)
        else:
            parent["children"].append(node)
    for node in nodes.values():
        node["children"].sort(key=lambda item: (int(item.get("priority") or 0), str(item.get(id_key))))
        if not node["children"]:
            node.pop("children")
    roots.sort(key=lambda item: (int(item.get("priority") or 0), str(item.get(id_key))))
    return roots


def _walk(nodes: list[dict[str, Any]]):
    for node in nodes:
        yield node
        yield from _walk(node.get("children") or [])
