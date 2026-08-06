from __future__ import annotations

import json
from datetime import date, datetime
from typing import Any, Mapping

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from jbm_cluster_py.integrations.database import configured_database_url


class SqlGovernanceRepository:
    def __init__(self, database_config: Mapping[str, Any]) -> None:
        url = configured_database_url(database_config)
        if not url:
            raise ValueError("Center database is not configured")
        self.engine: AsyncEngine = create_async_engine(url, pool_pre_ping=True)

    async def start(self) -> None:
        await self.health()

    async def stop(self) -> None:
        await self.engine.dispose()

    async def health(self) -> dict[str, Any]:
        async with self.engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        return {"ok": True}

    async def list_users(
        self, page: int, size: int, keyword: str | None, filters: Mapping[str, Any]
    ) -> tuple[list[dict[str, Any]], int]:
        where = ["1=1"]
        params: dict[str, Any] = {}
        if keyword:
            where.append(
                "(u.user_name LIKE :keyword OR u.nick_name LIKE :keyword OR u.mobile LIKE :keyword OR u.email LIKE :keyword)"
            )
            params["keyword"] = f"%{keyword}%"
        for key, column in (("status", "u.status"), ("companyId", "u.company_id")):
            if filters.get(key) not in (None, ""):
                where.append(f"{column} = :{key}")
                params[key] = filters[key]
        clause = " AND ".join(where)
        return await self._page(
            f"SELECT u.* FROM base_user u WHERE {clause} ORDER BY u.user_id DESC",
            f"SELECT COUNT(*) FROM base_user u WHERE {clause}",
            params,
            page,
            size,
        )

    async def get_user(self, user_id: int) -> dict[str, Any] | None:
        return await self._one("SELECT * FROM base_user WHERE user_id = :id", {"id": user_id})

    async def user_roles(self, user_id: int) -> list[dict[str, Any]]:
        return await self._all(
            """
            SELECT r.* FROM base_role_user ru
            JOIN base_role r ON r.role_id = ru.role_id
            WHERE ru.user_id = :user_id AND (r.status IS NULL OR r.status = 1)
            ORDER BY r.role_id
            """,
            {"user_id": user_id},
        )

    async def user_orgs(self, user_id: int) -> list[dict[str, Any]]:
        return await self._all(
            "SELECT * FROM base_user_org WHERE user_id = :user_id ORDER BY org_id",
            {"user_id": user_id},
        )

    async def user_accounts(self, user_id: int) -> list[dict[str, Any]]:
        rows = await self._all(
            "SELECT * FROM base_account WHERE user_id = :user_id ORDER BY account_id",
            {"user_id": user_id},
        )
        for row in rows:
            row.pop("password", None)
        return rows

    async def user_authorities(self, user_id: int, is_admin: bool) -> list[dict[str, Any]]:
        if is_admin:
            return await self._all(
                "SELECT authority_id, authority FROM base_authority WHERE status = 1 ORDER BY authority_id"
            )
        return await self._all(
            """
            SELECT DISTINCT a.authority_id, a.authority
            FROM base_authority a
            WHERE a.status = 1 AND (
              EXISTS (
                SELECT 1 FROM base_authority_user au
                WHERE au.authority_id = a.authority_id AND au.user_id = :user_id
                  AND (au.expire_time IS NULL OR au.expire_time > CURRENT_TIMESTAMP)
              ) OR EXISTS (
                SELECT 1 FROM base_role_user ru
                JOIN base_authority_role ar ON ar.role_id = ru.role_id
                WHERE ru.user_id = :user_id AND ar.authority_id = a.authority_id
                  AND (ar.expire_time IS NULL OR ar.expire_time > CURRENT_TIMESTAMP)
              )
            ) ORDER BY a.authority_id
            """,
            {"user_id": user_id},
        )

    async def user_menus(self, user_id: int, app_id: int | None, is_admin: bool) -> list[dict[str, Any]]:
        params: dict[str, Any] = {"user_id": user_id}
        app_clause = ""
        if app_id is not None:
            app_clause = " AND (m.app_id IS NULL OR m.app_id = :app_id)"
            params["app_id"] = app_id
        all_rows = await self._all(
            f"""
            SELECT m.*, a.authority_id
            FROM base_menu m
            LEFT JOIN base_authority a ON a.menu_id = m.menu_id AND a.status = 1
            WHERE m.status = 1 {app_clause}
            ORDER BY COALESCE(m.priority, 0), m.menu_id
            """,
            params,
        )
        if is_admin:
            return all_rows
        granted = {
            str(row.get("menuId"))
            for row in await self._all(
                """
                SELECT DISTINCT a.menu_id FROM base_authority a
                WHERE a.menu_id IS NOT NULL AND a.status = 1 AND (
                  EXISTS (SELECT 1 FROM base_authority_user au WHERE au.authority_id = a.authority_id AND au.user_id = :user_id)
                  OR EXISTS (
                    SELECT 1 FROM base_role_user ru
                    JOIN base_authority_role ar ON ar.role_id = ru.role_id
                    WHERE ru.user_id = :user_id AND ar.authority_id = a.authority_id
                  )
                )
                """,
                {"user_id": user_id},
            )
        }
        by_id = {str(row.get("menuId")): row for row in all_rows}
        visible = set(granted)
        for menu_id in list(granted):
            parent_id = by_id.get(menu_id, {}).get("parentId")
            while parent_id:
                visible.add(str(parent_id))
                parent_id = by_id.get(str(parent_id), {}).get("parentId")
        return [row for row in all_rows if str(row.get("menuId")) in visible]

    async def list_orgs(self, keyword: str | None = None) -> list[dict[str, Any]]:
        params: dict[str, Any] = {}
        where = ""
        if keyword:
            where = "WHERE org_name LIKE :keyword OR org_code LIKE :keyword"
            params["keyword"] = f"%{keyword}%"
        return await self._all(f"SELECT * FROM base_org {where} ORDER BY COALESCE(level, 0), id", params)

    async def list_dicts(self, parent_id: int | None = None) -> list[dict[str, Any]]:
        if parent_id is None:
            return await self._all("SELECT * FROM base_dic WHERE parent_id IS NULL ORDER BY id")
        return await self._all(
            "SELECT * FROM base_dic WHERE parent_id = :parent_id ORDER BY id",
            {"parent_id": parent_id},
        )

    async def list_apps(self, page: int, size: int, filters: Mapping[str, Any]) -> tuple[list[dict[str, Any]], int]:
        where, params = _filters(
            filters,
            {"status": "a.status", "orgId": "a.org_id", "appType": "a.app_type"},
            ("appName", "code", "apiKey"),
            ("a.app_name", "a.code", "a.api_key"),
        )
        rows, total = await self._page(
            f"SELECT a.* FROM base_app a WHERE {where} ORDER BY a.app_id DESC",
            f"SELECT COUNT(*) FROM base_app a WHERE {where}",
            params,
            page,
            size,
        )
        for row in rows:
            row.pop("secretKey", None)
            row.pop("privateKey", None)
        return rows, total

    async def list_roles(self, page: int, size: int, filters: Mapping[str, Any]) -> tuple[list[dict[str, Any]], int]:
        where, params = _filters(
            filters,
            {"status": "r.status"},
            ("roleName", "roleCode"),
            ("r.role_name", "r.role_code"),
        )
        return await self._page(
            f"SELECT r.* FROM base_role r WHERE {where} ORDER BY r.role_id",
            f"SELECT COUNT(*) FROM base_role r WHERE {where}",
            params,
            page,
            size,
        )

    async def list_routes(self, page: int, size: int, filters: Mapping[str, Any]) -> tuple[list[dict[str, Any]], int]:
        where, params = _filters(
            filters,
            {"status": "g.status", "serviceId": "g.service_id"},
            ("routeName", "path"),
            ("g.route_name", "g.path"),
        )
        return await self._page(
            f"SELECT g.* FROM gateway_route g WHERE {where} ORDER BY g.route_id",
            f"SELECT COUNT(*) FROM gateway_route g WHERE {where}",
            params,
            page,
            size,
        )

    async def dashboard_counts(self, tenant_id: int | None = None) -> dict[str, int]:
        if tenant_id is not None:
            return {
                "userTotal": await self._count(
                    "SELECT COUNT(*) FROM base_user WHERE company_id=:tenant_id",
                    {"tenant_id": tenant_id},
                ),
                "appTotal": await self._count(
                    "SELECT COUNT(*) FROM base_app WHERE org_id=:tenant_id",
                    {"tenant_id": tenant_id},
                ),
                "orgTotal": len(
                    [
                        row
                        for row in await self.list_orgs()
                        if int(row.get("id") or 0) == tenant_id or int(row.get("parentId") or 0) == tenant_id
                    ]
                ),
                "roleTotal": 1,
                "authorityTotal": 0,
                "apiTotal": 0,
                "apiKeyTotal": 0,
            }
        tables = {
            "userTotal": "base_user",
            "appTotal": "base_app",
            "orgTotal": "base_org",
            "roleTotal": "base_role",
            "authorityTotal": "base_authority",
            "apiTotal": "base_api",
            "apiKeyTotal": "base_api_key",
        }
        return {key: await self._count(f"SELECT COUNT(*) FROM {table}") for key, table in tables.items()}

    async def _page(
        self,
        select_sql: str,
        count_sql: str,
        params: Mapping[str, Any],
        page: int,
        size: int,
    ) -> tuple[list[dict[str, Any]], int]:
        page = max(int(page), 1)
        size = min(max(int(size), 1), 100)
        query_params = {**params, "limit": size, "offset": (page - 1) * size}
        rows = await self._all(select_sql + " LIMIT :limit OFFSET :offset", query_params)
        return rows, await self._count(count_sql, params)

    async def _all(self, sql: str, params: Mapping[str, Any] | None = None) -> list[dict[str, Any]]:
        async with self.engine.connect() as conn:
            result = await conn.execute(text(sql), dict(params or {}))
            return [_serialize(row) for row in result.mappings().all()]

    async def _one(self, sql: str, params: Mapping[str, Any] | None = None) -> dict[str, Any] | None:
        rows = await self._all(sql, params)
        return rows[0] if rows else None

    async def _count(self, sql: str, params: Mapping[str, Any] | None = None) -> int:
        async with self.engine.connect() as conn:
            value = (await conn.execute(text(sql), dict(params or {}))).scalar()
        return int(value or 0)


def _filters(
    source: Mapping[str, Any],
    equals: Mapping[str, str],
    keyword_keys: tuple[str, ...],
    keyword_columns: tuple[str, ...],
) -> tuple[str, dict[str, Any]]:
    clauses = ["1=1"]
    params: dict[str, Any] = {}
    for key, column in equals.items():
        if source.get(key) not in (None, ""):
            clauses.append(f"{column} = :{key}")
            params[key] = source[key]
    keyword = next((str(source[key]).strip() for key in keyword_keys if source.get(key)), "")
    if keyword:
        clauses.append("(" + " OR ".join(f"{column} LIKE :keyword" for column in keyword_columns) + ")")
        params["keyword"] = f"%{keyword}%"
    return " AND ".join(clauses), params


def _serialize(row: Mapping[str, Any]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in row.items():
        camel = _camel(key)
        if isinstance(value, (datetime, date)):
            value = value.isoformat()
        elif isinstance(value, int) and abs(value) > 9_007_199_254_740_991:
            value = str(value)
        elif camel == "extendData" and isinstance(value, str):
            try:
                value = json.loads(value)
            except json.JSONDecodeError:
                pass
        result[camel] = value
    return result


def _camel(value: str) -> str:
    head, *tail = value.split("_")
    return head + "".join(part[:1].upper() + part[1:] for part in tail)
