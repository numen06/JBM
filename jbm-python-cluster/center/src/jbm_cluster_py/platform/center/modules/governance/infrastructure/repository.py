from __future__ import annotations

import json
import secrets
import time
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
        if filters.get("tenantId") not in (None, ""):
            where.append(
                "EXISTS (SELECT 1 FROM base_user_org uo WHERE uo.user_id=u.user_id "
                "AND uo.org_id=:tenantId AND (uo.expire_time IS NULL OR uo.expire_time > CURRENT_TIMESTAMP))"
            )
            params["tenantId"] = filters["tenantId"]
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

    async def is_user_member(self, user_id: int, tenant_id: int) -> bool:
        return bool(
            await self._one(
                """SELECT 1 AS member FROM base_user_org
                   WHERE user_id=:user_id AND org_id=:tenant_id
                     AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)""",
                {"user_id": user_id, "tenant_id": tenant_id},
            )
        )

    async def user_roles(
        self, user_id: int, app_id: int | None = None, tenant_id: int | None = None
    ) -> list[dict[str, Any]]:
        app_clause = ""
        tenant_clause = ""
        params: dict[str, Any] = {"user_id": user_id, "tenant_id": tenant_id}
        if app_id is not None:
            app_clause = " AND (r.app_id IS NULL OR r.app_id = :app_id) AND (ru.app_id IS NULL OR ru.app_id = :app_id)"
            params["app_id"] = app_id
        if tenant_id is not None:
            tenant_clause = " AND (ru.tenant_id IS NULL OR ru.tenant_id = :tenant_id)"
        return await self._all(
            f"""
            SELECT r.* FROM base_role_user ru
            JOIN base_role r ON r.role_id = ru.role_id
            WHERE ru.user_id = :user_id AND (r.status IS NULL OR r.status = 1)
            {app_clause} {tenant_clause}
            ORDER BY r.role_id
            """,
            params,
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

    async def user_authorities(
        self,
        user_id: int,
        is_admin: bool,
        app_id: int | None = None,
        tenant_id: int | None = None,
    ) -> list[dict[str, Any]]:
        app_clause = "" if app_id is None else " AND (app_id IS NULL OR app_id = :app_id)"
        params: dict[str, Any] = {"user_id": user_id, "tenant_id": tenant_id}
        if app_id is not None:
            params["app_id"] = app_id
        if is_admin:
            return await self._all(
                "SELECT authority_id, authority FROM base_authority WHERE status = 1"
                + app_clause
                + " ORDER BY authority_id",
                params,
            )
        return await self._all(
            """
            SELECT DISTINCT a.authority_id, a.authority
            FROM base_authority a
            WHERE a.status = 1
              AND (:app_id IS NULL OR (a.app_id IS NULL OR a.app_id = :app_id))
              AND (
              EXISTS (
                SELECT 1 FROM base_authority_user au
                WHERE au.authority_id = a.authority_id AND au.user_id = :user_id
                  AND (au.expire_time IS NULL OR au.expire_time > CURRENT_TIMESTAMP)
                  AND (:app_id IS NULL OR (au.app_id IS NULL OR au.app_id = :app_id))
              ) OR EXISTS (
                SELECT 1 FROM base_role_user ru
                JOIN base_authority_role ar ON ar.role_id = ru.role_id
                JOIN base_role r ON r.role_id = ru.role_id
                WHERE ru.user_id = :user_id AND ar.authority_id = a.authority_id
                  AND (:tenant_id IS NULL OR (ru.tenant_id IS NULL OR ru.tenant_id = :tenant_id))
                  AND (ar.expire_time IS NULL OR ar.expire_time > CURRENT_TIMESTAMP)
                  AND (:app_id IS NULL OR (r.app_id IS NULL OR r.app_id = :app_id))
                  AND (:app_id IS NULL OR (ru.app_id IS NULL OR ru.app_id = :app_id))
                  AND (:app_id IS NULL OR (ar.app_id IS NULL OR ar.app_id = :app_id))
              )
            ) ORDER BY a.authority_id
            """,
            {"user_id": user_id, "app_id": app_id, "tenant_id": tenant_id},
        )

    async def user_menus(
        self, user_id: int, app_id: int | None, is_admin: bool, tenant_id: int | None = None
    ) -> list[dict[str, Any]]:
        params: dict[str, Any] = {"user_id": user_id, "app_id": app_id, "tenant_id": tenant_id}
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
                  EXISTS (
                    SELECT 1 FROM base_authority_user au
                    WHERE au.authority_id = a.authority_id AND au.user_id = :user_id
                      AND (:app_id IS NULL OR (au.app_id IS NULL OR au.app_id = :app_id))
                  )
                  OR EXISTS (
                    SELECT 1 FROM base_role_user ru
                    JOIN base_authority_role ar ON ar.role_id = ru.role_id
                    JOIN base_role r ON r.role_id = ru.role_id
                    WHERE ru.user_id = :user_id AND ar.authority_id = a.authority_id
                      AND (:tenant_id IS NULL OR (ru.tenant_id IS NULL OR ru.tenant_id = :tenant_id))
                      AND (:app_id IS NULL OR (r.app_id IS NULL OR r.app_id = :app_id))
                      AND (:app_id IS NULL OR (ru.app_id IS NULL OR ru.app_id = :app_id))
                      AND (:app_id IS NULL OR (ar.app_id IS NULL OR ar.app_id = :app_id))
                  )
                ) AND (:app_id IS NULL OR (a.app_id IS NULL OR a.app_id = :app_id))
                """,
                {"user_id": user_id, "app_id": app_id, "tenant_id": tenant_id},
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
            {"status": "r.status", "appId": "r.app_id"},
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

    async def find_tenant_delegation(
        self,
        owner_tenant_id: int,
        operator_tenant_id: int,
        operator_user_id: int,
        app_id: int,
        permission: str,
        resource_type: str | None = None,
    ) -> dict[str, Any] | None:
        rows = await self._all(
            """
            SELECT * FROM base_tenant_delegation
            WHERE owner_tenant_id = :owner_tenant_id
              AND operator_tenant_id = :operator_tenant_id
              AND (operator_user_id IS NULL OR operator_user_id = :operator_user_id)
              AND app_id = :app_id
              AND status = 1
              AND (valid_from IS NULL OR valid_from <= CURRENT_TIMESTAMP)
              AND (valid_to IS NULL OR valid_to > CURRENT_TIMESTAMP)
            ORDER BY id
            """,
            {
                "owner_tenant_id": owner_tenant_id,
                "operator_tenant_id": operator_tenant_id,
                "operator_user_id": operator_user_id,
                "app_id": app_id,
            },
        )
        for row in rows:
            permissions = _json_values(row.get("permissionCodes"))
            resources = _json_values(row.get("resourceTypes"))
            if permission not in permissions and "*" not in permissions:
                continue
            if resource_type and resources and resource_type not in resources and "*" not in resources:
                continue
            for key in ("dataScope", "fieldPolicy"):
                row[key] = _json_value(row.get(key))
            return row
        return None

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

    async def list_app_features(self, app_id: int) -> list[dict[str, Any]]:
        return await self._all(
            """SELECT * FROM base_app_feature
               WHERE app_id = :app_id AND status = 1
               ORDER BY sort_order, id""",
            {"app_id": app_id},
        )

    async def create_app_feature(
        self, app_id: int, feature_code: str, feature_name: str, feature_desc: str | None
    ) -> dict[str, Any]:
        async with self.engine.begin() as conn:
            existing = (
                await conn.execute(
                    text(
                        "SELECT id FROM base_app_feature "
                        "WHERE app_id=:app_id AND feature_code=:feature_code FOR UPDATE"
                    ),
                    {"app_id": app_id, "feature_code": feature_code},
                )
            ).mappings().first()
            now = datetime.now()
            if existing:
                feature_id = int(existing["id"])
                await conn.execute(
                    text(
                        "UPDATE base_app_feature SET feature_name=:feature_name, "
                        "feature_desc=:feature_desc, status=1, update_time=:now WHERE id=:id"
                    ),
                    {
                        "id": feature_id,
                        "feature_name": feature_name,
                        "feature_desc": feature_desc,
                        "now": now,
                    },
                )
            else:
                feature_id = _new_id()
                await conn.execute(
                    text(
                        "INSERT INTO base_app_feature "
                        "(id, app_id, feature_code, feature_name, feature_desc, status, create_time, update_time) "
                        "VALUES (:id, :app_id, :feature_code, :feature_name, :feature_desc, 1, :now, :now)"
                    ),
                    {
                        "id": feature_id,
                        "app_id": app_id,
                        "feature_code": feature_code,
                        "feature_name": feature_name,
                        "feature_desc": feature_desc,
                        "now": now,
                    },
                )
        return (await self._one("SELECT * FROM base_app_feature WHERE id=:id", {"id": feature_id})) or {}

    async def disable_app_feature(self, app_id: int, feature_code: str) -> None:
        async with self.engine.begin() as conn:
            params = {"app_id": app_id, "feature_code": feature_code}
            await conn.execute(
                text(
                    "UPDATE base_app_feature SET status=0, update_time=CURRENT_TIMESTAMP "
                    "WHERE app_id=:app_id AND feature_code=:feature_code"
                ),
                params,
            )
            await conn.execute(
                text(
                    "UPDATE base_tenant_feature SET status=0, valid_to=CURRENT_TIMESTAMP, "
                    "update_time=CURRENT_TIMESTAMP WHERE app_id=:app_id AND feature_code=:feature_code"
                ),
                params,
            )
            await conn.execute(
                text(
                    "DELETE FROM base_user_feature_grant "
                    "WHERE app_id=:app_id AND feature_code=:feature_code"
                ),
                params,
            )

    async def list_tenant_features(self, tenant_id: int, app_id: int) -> list[dict[str, Any]]:
        return await self._all(
            """SELECT tf.*, af.feature_name, af.feature_desc
               FROM base_tenant_feature tf
               JOIN base_app_feature af
                 ON af.app_id=tf.app_id AND af.feature_code=tf.feature_code
               WHERE tf.tenant_id=:tenant_id AND tf.app_id=:app_id AND tf.status=1
                 AND (tf.valid_from IS NULL OR tf.valid_from <= CURRENT_TIMESTAMP)
                 AND (tf.valid_to IS NULL OR tf.valid_to > CURRENT_TIMESTAMP)
               ORDER BY af.sort_order, tf.id""",
            {"tenant_id": tenant_id, "app_id": app_id},
        )

    async def effective_user_features(
        self, user_id: int, tenant_id: int, app_id: int, tenant_admin: bool
    ) -> list[str]:
        if tenant_admin:
            rows = await self.list_tenant_features(tenant_id, app_id)
            return [str(row["featureCode"]) for row in rows]
        rows = await self._all(
            """SELECT DISTINCT ug.feature_code
               FROM base_user_feature_grant ug
               JOIN base_tenant_feature tf
                 ON tf.tenant_id=ug.tenant_id AND tf.app_id=ug.app_id
                AND tf.feature_code=ug.feature_code
               WHERE ug.user_id=:user_id AND ug.tenant_id=:tenant_id AND ug.app_id=:app_id
                 AND ug.status=1 AND tf.status=1
                 AND (tf.valid_from IS NULL OR tf.valid_from <= CURRENT_TIMESTAMP)
                 AND (tf.valid_to IS NULL OR tf.valid_to > CURRENT_TIMESTAMP)
               ORDER BY ug.feature_code""",
            {"user_id": user_id, "tenant_id": tenant_id, "app_id": app_id},
        )
        return [str(row["featureCode"]) for row in rows]

    async def list_feature_tenants(self, app_id: int) -> list[dict[str, Any]]:
        tenants = await self._all(
            """SELECT DISTINCT o.id AS tenant_id, o.org_name AS tenant_name, o.org_code AS tenant_code
               FROM base_tenant_app ta
               JOIN base_org o ON o.id=ta.tenant_id
               WHERE ta.app_id=:app_id AND ta.status=1
               ORDER BY o.org_name, o.id""",
            {"app_id": app_id},
        )
        grants = await self._all(
            """SELECT tenant_id, feature_code FROM base_tenant_feature
               WHERE app_id=:app_id AND status=1
                 AND (valid_to IS NULL OR valid_to > CURRENT_TIMESTAMP)
               ORDER BY feature_code""",
            {"app_id": app_id},
        )
        by_tenant: dict[str, list[str]] = {}
        for row in grants:
            by_tenant.setdefault(str(row["tenantId"]), []).append(str(row["featureCode"]))
        for row in tenants:
            row["featureCodes"] = by_tenant.get(str(row["tenantId"]), [])
        return tenants

    async def replace_tenant_features(
        self,
        tenant_id: int,
        app_id: int,
        feature_codes: list[str],
        granted_by: int,
    ) -> list[str]:
        codes = list(dict.fromkeys(feature_codes))
        async with self.engine.begin() as conn:
            tenant_app = (
                await conn.execute(
                    text(
                        "SELECT 1 FROM base_tenant_app "
                        "WHERE tenant_id=:tenant_id AND app_id=:app_id AND status=1"
                    ),
                    {"tenant_id": tenant_id, "app_id": app_id},
                )
            ).first()
            if not tenant_app:
                raise ValueError("目标租户未接入当前应用")
            available = {
                str(value)
                for value in (
                    await conn.execute(
                        text(
                            "SELECT feature_code FROM base_app_feature "
                            "WHERE app_id=:app_id AND status=1"
                        ),
                        {"app_id": app_id},
                    )
                ).scalars().all()
            }
            if set(codes) - available:
                raise ValueError("包含不存在或已停用的功能")
            await conn.execute(
                text(
                    "UPDATE base_tenant_feature SET status=0, valid_to=CURRENT_TIMESTAMP, "
                    "update_time=CURRENT_TIMESTAMP WHERE tenant_id=:tenant_id AND app_id=:app_id"
                ),
                {"tenant_id": tenant_id, "app_id": app_id},
            )
            now = datetime.now()
            for feature_code in codes:
                params = {
                    "id": _new_id(),
                    "tenant_id": tenant_id,
                    "app_id": app_id,
                    "feature_code": feature_code,
                    "granted_by": granted_by,
                    "now": now,
                }
                existing = (
                    await conn.execute(
                        text(
                            "SELECT id FROM base_tenant_feature WHERE tenant_id=:tenant_id "
                            "AND app_id=:app_id AND feature_code=:feature_code"
                        ),
                        params,
                    )
                ).mappings().first()
                if existing:
                    await conn.execute(
                        text(
                            "UPDATE base_tenant_feature SET status=1, valid_from=:now, valid_to=NULL, "
                            "granted_by=:granted_by, update_time=:now WHERE id=:existing_id"
                        ),
                        {**params, "existing_id": existing["id"]},
                    )
                else:
                    await conn.execute(
                        text(
                            "INSERT INTO base_tenant_feature "
                            "(id, tenant_id, app_id, feature_code, status, valid_from, granted_by, create_time, update_time) "
                            "VALUES (:id, :tenant_id, :app_id, :feature_code, 1, :now, :granted_by, :now, :now)"
                        ),
                        params,
                    )
            await conn.execute(
                text(
                    "DELETE ug FROM base_user_feature_grant ug "
                    "LEFT JOIN base_tenant_feature tf ON tf.tenant_id=ug.tenant_id "
                    "AND tf.app_id=ug.app_id AND tf.feature_code=ug.feature_code AND tf.status=1 "
                    "WHERE ug.tenant_id=:tenant_id AND ug.app_id=:app_id AND tf.id IS NULL"
                ),
                {"tenant_id": tenant_id, "app_id": app_id},
            )
        return codes

    async def list_tenant_members(self, tenant_id: int, app_id: int) -> list[dict[str, Any]]:
        members = await self._all(
            """SELECT u.user_id, u.user_name, u.nick_name, u.company_id
               FROM base_user_org uo
               JOIN base_user u ON u.user_id=uo.user_id
               WHERE uo.org_id=:tenant_id
                 AND (uo.expire_time IS NULL OR uo.expire_time > CURRENT_TIMESTAMP)
                 AND (u.status IS NULL OR u.status=1)
               ORDER BY u.create_time, u.user_id""",
            {"tenant_id": tenant_id},
        )
        grants = await self._all(
            """SELECT user_id, feature_code FROM base_user_feature_grant
               WHERE tenant_id=:tenant_id AND app_id=:app_id AND status=1
               ORDER BY feature_code""",
            {"tenant_id": tenant_id, "app_id": app_id},
        )
        by_user: dict[str, list[str]] = {}
        for row in grants:
            by_user.setdefault(str(row["userId"]), []).append(str(row["featureCode"]))
        for member in members:
            member["featureCodes"] = by_user.get(str(member["userId"]), [])
        return members

    async def replace_member_features(
        self,
        tenant_id: int,
        app_id: int,
        user_id: int,
        feature_codes: list[str],
        granted_by: int,
    ) -> list[str]:
        codes = list(dict.fromkeys(feature_codes))
        async with self.engine.begin() as conn:
            member = (
                await conn.execute(
                    text(
                        "SELECT 1 FROM base_user_org WHERE user_id=:user_id AND org_id=:tenant_id "
                        "AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)"
                    ),
                    {"user_id": user_id, "tenant_id": tenant_id},
                )
            ).first()
            if not member:
                raise ValueError("目标用户不是当前租户成员")
            allowed = {
                str(value)
                for value in (
                    await conn.execute(
                        text(
                            "SELECT feature_code FROM base_tenant_feature WHERE tenant_id=:tenant_id "
                            "AND app_id=:app_id AND status=1 "
                            "AND (valid_to IS NULL OR valid_to > CURRENT_TIMESTAMP)"
                        ),
                        {"tenant_id": tenant_id, "app_id": app_id},
                    )
                ).scalars().all()
            }
            if set(codes) - allowed:
                raise ValueError("只能向下授权本租户已开通的功能")
            await conn.execute(
                text(
                    "DELETE FROM base_user_feature_grant "
                    "WHERE tenant_id=:tenant_id AND app_id=:app_id AND user_id=:user_id"
                ),
                {"tenant_id": tenant_id, "app_id": app_id, "user_id": user_id},
            )
            now = datetime.now()
            for feature_code in codes:
                await conn.execute(
                    text(
                        "INSERT INTO base_user_feature_grant "
                        "(id, tenant_id, app_id, user_id, feature_code, status, granted_by, create_time, update_time) "
                        "VALUES (:id, :tenant_id, :app_id, :user_id, :feature_code, 1, :granted_by, :now, :now)"
                    ),
                    {
                        "id": _new_id(),
                        "tenant_id": tenant_id,
                        "app_id": app_id,
                        "user_id": user_id,
                        "feature_code": feature_code,
                        "granted_by": granted_by,
                        "now": now,
                    },
                )
        return codes

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


def _json_value(value: Any) -> Any:
    if not isinstance(value, str):
        return value
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return value


def _json_values(value: Any) -> set[str]:
    parsed = _json_value(value)
    if isinstance(parsed, list):
        return {str(item) for item in parsed}
    return {item for item in str(parsed or "").replace(",", " ").split() if item}


def _camel(value: str) -> str:
    head, *tail = value.split("_")
    return head + "".join(part[:1].upper() + part[1:] for part in tail)


def _new_id() -> int:
    return int(time.time_ns() // 1_000) + secrets.randbelow(1000)
