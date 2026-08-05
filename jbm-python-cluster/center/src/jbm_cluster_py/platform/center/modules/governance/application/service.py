from __future__ import annotations

from typing import Any, Mapping

from jbm_cluster_py.common.masterdata import PageForm, java_page
from jbm_cluster_py.platform.center.modules.governance.domain.ports import GovernanceRepository


class GovernanceService:
    def __init__(self, repository: GovernanceRepository) -> None:
        self.repository = repository

    async def users(
        self, page: int, size: int, keyword: str | None, filters: Mapping[str, Any]
    ) -> dict[str, Any]:
        rows, total = await self.repository.list_users(page, size, keyword, filters)
        return java_page(rows, total, PageForm(currPage=page, pageSize=size))

    async def current_user(self, identity: Mapping[str, Any]) -> dict[str, Any]:
        user_id = _identity_int(identity, "userId", "user_id", "sub")
        if user_id is None:
            raise ValueError("登录信息缺少 userId")
        user = await self.repository.get_user(user_id)
        if user is None:
            raise ValueError("用户不存在")
        is_admin = _is_admin(user, identity)
        user["roles"] = await self.repository.user_roles(user_id)
        user["authorities"] = await self.repository.user_authorities(user_id, is_admin)
        return user

    async def current_menus(self, identity: Mapping[str, Any]) -> list[dict[str, Any]]:
        user_id = _identity_int(identity, "userId", "user_id", "sub")
        if user_id is None:
            raise ValueError("登录信息缺少 userId")
        app_id = _identity_int(identity, "appId", "app_id")
        user = await self.repository.get_user(user_id) or {}
        rows = await self.repository.user_menus(user_id, app_id, _is_admin(user, identity))
        return _tree(rows, "menuId")

    async def org_roots(self) -> list[dict[str, Any]]:
        return [row for row in await self.repository.list_orgs() if not row.get("parentId")]

    async def org_tree(self, root_id: int | None = None) -> list[dict[str, Any]]:
        rows = await self.repository.list_orgs()
        tree = _tree(rows, "id")
        if root_id is None:
            return tree
        return [node for node in _walk(tree) if str(node.get("id")) == str(root_id)]

    async def org_page(self, page: int, size: int, keyword: str | None) -> dict[str, Any]:
        rows = await self.repository.list_orgs(keyword)
        start = (page - 1) * size
        return java_page(rows[start : start + size], len(rows), PageForm(currPage=page, pageSize=size))

    async def dict_roots(self) -> list[dict[str, Any]]:
        return await self.repository.list_dicts(None)

    async def dict_page(
        self, parent_id: int | None, page: int, size: int, keyword: str | None
    ) -> dict[str, Any]:
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

    async def apps(self, page: int, size: int, filters: Mapping[str, Any]) -> dict[str, Any]:
        rows, total = await self.repository.list_apps(page, size, filters)
        return java_page(rows, total, PageForm(currPage=page, pageSize=size))

    async def roles(self, page: int, size: int, filters: Mapping[str, Any]) -> dict[str, Any]:
        rows, total = await self.repository.list_roles(page, size, filters)
        return java_page(rows, total, PageForm(currPage=page, pageSize=size))

    async def routes(self, page: int, size: int, filters: Mapping[str, Any]) -> dict[str, Any]:
        rows, total = await self.repository.list_routes(page, size, filters)
        return java_page(rows, total, PageForm(currPage=page, pageSize=size))


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
    return int(user.get("userId") or 0) == 1 or str(user.get("userName") or "") == "admin" or bool(
        identity.get("admin")
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
