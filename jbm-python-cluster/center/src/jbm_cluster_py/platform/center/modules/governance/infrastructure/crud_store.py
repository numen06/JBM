from __future__ import annotations

import secrets
import time
from datetime import datetime
from typing import Any, Mapping

from sqlalchemy import inspect, text
from sqlalchemy.ext.asyncio import AsyncEngine


class CrudStore:
    """Schema-aware CRUD for the fixed Center compatibility tables."""

    TABLES = {
        "action": ("base_action", "action_id"),
        "api": ("base_api", "api_id"),
        "apikey": ("base_api_key", "key_id"),
        "app": ("base_app", "app_id"),
        "tenantApp": ("base_tenant_app", "id"),
        "tenantDelegation": ("base_tenant_delegation", "id"),
        "operatorApplication": ("base_operator_application", "id"),
        "developer": ("base_developer", "user_id"),
        "menu": ("base_menu", "menu_id"),
        "role": ("base_role", "role_id"),
        "user": ("base_user", "user_id"),
        "org": ("base_org", "id"),
        "dic": ("base_dic", "id"),
        "account": ("base_account", "account_id"),
        "accountLogs": ("base_account_logs", "id"),
        "area": ("base_area", "id"),
        "authority": ("base_authority", "authority_id"),
        "route": ("gateway_route", "route_id"),
        "rateLimit": ("gateway_rate_limit", "policy_id"),
        "ipLimit": ("gateway_ip_limit", "policy_id"),
        "customForm": ("custom_forms", "id"),
        "customFormItem": ("custom_forms_item", "id"),
        "extendForm": ("extend_form_definition", "id"),
        "openApiDocument": ("open_api_document", "doc_id"),
        "openApiOperation": ("open_api_operation", "operation_id"),
        "publishedApiDoc": ("published_api_doc", "published_id"),
        "appConfig": ("base_app_config", "id"),
        "releaseInfo": ("base_release_info", "id"),
        "certification": ("base_user_certification", "id"),
        "dataSource": ("data_source_management", "id"),
    }

    def __init__(self, engine: AsyncEngine) -> None:
        self.engine = engine
        self._metadata: dict[str, tuple[set[str], str, bool]] = {}

    async def list(
        self,
        resource: str,
        filters: Mapping[str, Any] | None = None,
        page: int = 1,
        size: int = 10,
        root_only: bool = False,
        include_secrets: bool = False,
    ) -> tuple[list[dict[str, Any]], int]:
        table, pk = self._resource(resource)
        columns, _, _ = await self._meta(resource)
        if not columns:
            return [], 0
        clauses: list[str] = []
        params: dict[str, Any] = {}
        for key, value in (filters or {}).items():
            column = _snake(key)
            if value in (None, "") or column not in columns or column in {"page", "page_size"}:
                continue
            if isinstance(value, str) and column not in {pk, "status", "app_id", "parent_id", "user_id", "role_id", "policy_id"}:
                clauses.append(f"{column} LIKE :{column}")
                params[column] = f"%{value}%"
            else:
                clauses.append(f"{column} = :{column}")
                params[column] = value
        if root_only and "parent_id" in columns:
            clauses.append("parent_id IS NULL")
        where = " WHERE " + " AND ".join(clauses) if clauses else ""
        page = max(int(page), 1)
        size = min(max(int(size), 1), 100)
        async with self.engine.connect() as conn:
            total = int((await conn.execute(text(f"SELECT COUNT(*) FROM {table}{where}"), params)).scalar() or 0)
            rows = (
                await conn.execute(
                    text(f"SELECT * FROM {table}{where} ORDER BY {pk} LIMIT :limit OFFSET :offset"),
                    {**params, "limit": size, "offset": (page - 1) * size},
                )
            ).mappings().all()
        return [_row(row, include_secrets) for row in rows], total

    async def list_active_delegations(
        self,
        operator_tenant_id: int,
        operator_user_id: int,
        app_id: int,
        page: int = 1,
        size: int = 10,
    ) -> tuple[list[dict[str, Any]], int]:
        """List currently effective grants received by one operator tenant."""
        page = max(int(page), 1)
        size = min(max(int(size), 1), 100)
        where = """
            WHERE operator_tenant_id = :operator_tenant_id
              AND (operator_user_id IS NULL OR operator_user_id = :operator_user_id)
              AND app_id = :app_id
              AND status = 1
              AND (valid_from IS NULL OR valid_from <= CURRENT_TIMESTAMP)
              AND (valid_to IS NULL OR valid_to > CURRENT_TIMESTAMP)
        """
        params = {
            "operator_tenant_id": operator_tenant_id,
            "operator_user_id": operator_user_id,
            "app_id": app_id,
        }
        async with self.engine.connect() as conn:
            total = int(
                (
                    await conn.execute(
                        text(f"SELECT COUNT(*) FROM base_tenant_delegation {where}"),
                        params,
                    )
                ).scalar()
                or 0
            )
            rows = (
                await conn.execute(
                    text(
                        f"SELECT * FROM base_tenant_delegation {where} "
                        "ORDER BY id LIMIT :limit OFFSET :offset"
                    ),
                    {**params, "limit": size, "offset": (page - 1) * size},
                )
            ).mappings().all()
        return [_row(row, False) for row in rows], total

    async def review_operator_application(
        self,
        application_id: int,
        status: int,
        reviewed_by: int,
        review_remark: str = "",
    ) -> dict[str, Any]:
        """Review an application and switch the applicant's app role atomically."""
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        async with self.engine.begin() as conn:
            application = (
                await conn.execute(
                    text("SELECT * FROM base_operator_application WHERE id = :id FOR UPDATE"),
                    {"id": application_id},
                )
            ).mappings().first()
            if application is None:
                raise ValueError("运营申请不存在")
            if int(application.get("status") or 0) != 0:
                raise ValueError("运营申请已处理")
            if status == 1:
                role_id = (
                    await conn.execute(
                        text(
                            "SELECT role_id FROM base_role "
                            "WHERE app_id = :app_id AND role_code = 'iot_operator' AND status = 1 LIMIT 1"
                        ),
                        {"app_id": application["app_id"]},
                    )
                ).scalar()
                if role_id is None:
                    raise ValueError("当前应用未配置 iot_operator 角色")
                scope = {
                    "user_id": application["applicant_user_id"],
                    "tenant_id": application["tenant_id"],
                    "app_id": application["app_id"],
                }
                await conn.execute(
                    text(
                        "DELETE FROM base_role_user "
                        "WHERE user_id = :user_id AND tenant_id = :tenant_id AND app_id = :app_id"
                    ),
                    scope,
                )
                await conn.execute(
                    text(
                        "INSERT INTO base_role_user "
                        "(id, user_id, role_id, tenant_id, app_id, create_time, update_time) "
                        "VALUES (:id, :user_id, :role_id, :tenant_id, :app_id, :now, :now)"
                    ),
                    {**scope, "id": _new_id(), "role_id": role_id, "now": now},
                )
            await conn.execute(
                text(
                    "UPDATE base_operator_application SET status = :status, "
                    "review_remark = :review_remark, reviewed_by = :reviewed_by, "
                    "reviewed_at = :reviewed_at, update_time = :reviewed_at WHERE id = :id"
                ),
                {
                    "id": application_id,
                    "status": status,
                    "review_remark": review_remark or None,
                    "reviewed_by": reviewed_by,
                    "reviewed_at": now,
                },
            )
        return await self.get("operatorApplication", application_id) or {}

    async def get(
        self, resource: str, value: Any, include_secrets: bool = False
    ) -> dict[str, Any] | None:
        table, pk = self._resource(resource)
        columns, _, _ = await self._meta(resource)
        if not columns:
            return None
        async with self.engine.connect() as conn:
            row = (await conn.execute(text(f"SELECT * FROM {table} WHERE {pk} = :id LIMIT 1"), {"id": value})).mappings().first()
        return _row(row, include_secrets) if row else None

    async def find_user_by_username(self, username: str) -> dict[str, Any] | None:
        async with self.engine.connect() as conn:
            row = (
                await conn.execute(
                    text("SELECT * FROM base_user WHERE user_name=:username LIMIT 1"),
                    {"username": username},
                )
            ).mappings().first()
        return _row(row, False) if row else None

    async def is_user_member(self, user_id: Any, tenant_id: Any) -> bool:
        async with self.engine.connect() as conn:
            row = (
                await conn.execute(
                    text(
                        "SELECT 1 FROM base_user_org WHERE user_id=:user_id AND org_id=:tenant_id "
                        "AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP) LIMIT 1"
                    ),
                    {"user_id": user_id, "tenant_id": tenant_id},
                )
            ).first()
        return bool(row)

    async def save(
        self, resource: str, payload: Mapping[str, Any], identity: Any | None = None
    ) -> dict[str, Any]:
        table, pk = self._resource(resource)
        columns, _, autoincrement = await self._meta(resource)
        if not columns:
            raise ValueError(f"数据表不存在: {table}")
        values = {_snake(key): value for key, value in payload.items() if _snake(key) in columns}
        if identity is not None:
            values[pk] = identity
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        if "update_time" in columns:
            values["update_time"] = now
        current_id = values.get(pk)
        exists = await self.get(resource, current_id) if current_id is not None else None
        if exists:
            updates = {key: value for key, value in values.items() if key != pk}
            if updates:
                assignments = ", ".join(f"{key} = :{key}" for key in updates)
                async with self.engine.begin() as conn:
                    await conn.execute(text(f"UPDATE {table} SET {assignments} WHERE {pk} = :_pk"), {**updates, "_pk": current_id})
            return await self.get(resource, current_id) or {}
        if current_id is None and not autoincrement:
            current_id = _new_id()
            values[pk] = current_id
        if "create_time" in columns:
            values.setdefault("create_time", now)
        keys = list(values)
        async with self.engine.begin() as conn:
            result = await conn.execute(
                text(f"INSERT INTO {table} ({', '.join(keys)}) VALUES ({', '.join(':' + key for key in keys)})"),
                values,
            )
            if current_id is None:
                current_id = result.lastrowid
        return await self.get(resource, current_id) or {}

    async def delete(self, resource: str, identity: Any) -> bool:
        table, pk = self._resource(resource)
        columns, _, _ = await self._meta(resource)
        if not columns:
            return False
        async with self.engine.begin() as conn:
            result = await conn.execute(text(f"DELETE FROM {table} WHERE {pk} = :id"), {"id": identity})
        return bool(result.rowcount)

    async def update_where(
        self, resource: str, filters: Mapping[str, Any], values: Mapping[str, Any]
    ) -> int:
        table, _ = self._resource(resource)
        columns, _, _ = await self._meta(resource)
        safe_filters = {_snake(k): v for k, v in filters.items() if _snake(k) in columns}
        safe_values = {_snake(k): v for k, v in values.items() if _snake(k) in columns}
        if not safe_filters or not safe_values:
            return 0
        assignments = ", ".join(f"{key} = :set_{key}" for key in safe_values)
        where = " AND ".join(f"{key} = :where_{key}" for key in safe_filters)
        params = {**{f"set_{k}": v for k, v in safe_values.items()}, **{f"where_{k}": v for k, v in safe_filters.items()}}
        async with self.engine.begin() as conn:
            result = await conn.execute(text(f"UPDATE {table} SET {assignments} WHERE {where}"), params)
        return int(result.rowcount or 0)

    async def replace_links(
        self,
        table: str,
        owner_column: str,
        owner_id: Any,
        target_column: str,
        target_ids: list[Any],
    ) -> None:
        allowed = {
            "base_role_user",
            "base_user_org",
            "base_authority_role",
            "base_authority_user",
            "base_authority_app",
            "base_authority_action",
            "base_authority_apikey",
            "gateway_rate_limit_api",
            "gateway_ip_limit_api",
        }
        if table not in allowed:
            raise ValueError("不允许的关联表")
        async with self.engine.begin() as conn:
            columns = {
                column["name"]
                for column in await conn.run_sync(lambda sync: inspect(sync).get_columns(table))
            }
            await conn.execute(text(f"DELETE FROM {table} WHERE {owner_column} = :owner"), {"owner": owner_id})
            for target_id in dict.fromkeys(target_ids):
                values: dict[str, Any] = {owner_column: owner_id, target_column: target_id}
                if "id" in columns:
                    values["id"] = _new_id()
                now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                if "create_time" in columns:
                    values["create_time"] = now
                if "update_time" in columns:
                    values["update_time"] = now
                await conn.execute(
                    text(
                        f"INSERT INTO {table} ({', '.join(values)}) "
                        f"VALUES ({', '.join(':' + key for key in values)})"
                    ),
                    values,
                )

    async def replace_scoped_links(
        self,
        table: str,
        owner_column: str,
        owner_id: Any,
        target_column: str,
        target_ids: list[Any],
        scope_column: str,
        scope_value: Any,
        tenant_column: str | None = None,
        tenant_value: Any | None = None,
    ) -> None:
        if table not in {"base_role_user"} or scope_column != "app_id":
            raise ValueError("不允许的作用域关联表")
        if tenant_column not in {None, "tenant_id"}:
            raise ValueError("不允许的租户作用域")
        async with self.engine.begin() as conn:
            columns = {
                column["name"]
                for column in await conn.run_sync(lambda sync: inspect(sync).get_columns(table))
            }
            delete_clause = (
                f"DELETE FROM {table} WHERE {owner_column} = :owner "
                f"AND {scope_column} = :scope"
            )
            delete_params: dict[str, Any] = {"owner": owner_id, "scope": scope_value}
            if tenant_column and tenant_column in columns:
                delete_clause += f" AND {tenant_column} = :tenant"
                delete_params["tenant"] = tenant_value
            await conn.execute(text(delete_clause), delete_params)
            for target_id in dict.fromkeys(target_ids):
                values: dict[str, Any] = {
                    owner_column: owner_id,
                    target_column: target_id,
                    scope_column: scope_value,
                }
                if tenant_column and tenant_column in columns:
                    values[tenant_column] = tenant_value
                if "id" in columns:
                    values["id"] = _new_id()
                now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                if "create_time" in columns:
                    values["create_time"] = now
                if "update_time" in columns:
                    values["update_time"] = now
                await conn.execute(
                    text(
                        f"INSERT INTO {table} ({', '.join(values)}) "
                        f"VALUES ({', '.join(':' + key for key in values)})"
                    ),
                    values,
                )

    async def ensure_link(
        self,
        table: str,
        owner_column: str,
        owner_id: Any,
        target_column: str,
        target_id: Any,
    ) -> None:
        if table != "base_user_org":
            raise ValueError("不允许的成员关联表")
        async with self.engine.begin() as conn:
            exists = (
                await conn.execute(
                    text(
                        f"SELECT 1 FROM {table} WHERE {owner_column} = :owner "
                        f"AND {target_column} = :target LIMIT 1"
                    ),
                    {"owner": owner_id, "target": target_id},
                )
            ).first()
            if exists:
                return
            columns = {
                column["name"]
                for column in await conn.run_sync(lambda sync: inspect(sync).get_columns(table))
            }
            values: dict[str, Any] = {owner_column: owner_id, target_column: target_id}
            if "id" in columns:
                values["id"] = _new_id()
            now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            if "create_time" in columns:
                values["create_time"] = now
            if "update_time" in columns:
                values["update_time"] = now
            await conn.execute(
                text(
                    f"INSERT INTO {table} ({', '.join(values)}) "
                    f"VALUES ({', '.join(':' + key for key in values)})"
                ),
                values,
            )

    async def replace_role_users(
        self, role_id: Any, app_id: Any, users: list[tuple[Any, Any]]
    ) -> None:
        async with self.engine.begin() as conn:
            columns = {
                column["name"]
                for column in await conn.run_sync(
                    lambda sync: inspect(sync).get_columns("base_role_user")
                )
            }
            await conn.execute(
                text("DELETE FROM base_role_user WHERE role_id = :role_id AND app_id = :app_id"),
                {"role_id": role_id, "app_id": app_id},
            )
            for user_id, tenant_id in dict.fromkeys(users):
                values: dict[str, Any] = {
                    "role_id": role_id,
                    "app_id": app_id,
                    "user_id": user_id,
                }
                if "tenant_id" in columns:
                    values["tenant_id"] = tenant_id
                if "id" in columns:
                    values["id"] = _new_id()
                now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                if "create_time" in columns:
                    values["create_time"] = now
                if "update_time" in columns:
                    values["update_time"] = now
                await conn.execute(
                    text(
                        "INSERT INTO base_role_user "
                        f"({', '.join(values)}) VALUES "
                        f"({', '.join(':' + key for key in values)})"
                    ),
                    values,
                )

    async def linked_ids(
        self, table: str, owner_column: str, owner_id: Any, target_column: str
    ) -> list[Any]:
        allowed = {
            "base_role_user",
            "base_user_org",
            "base_authority_role",
            "base_authority_user",
            "base_authority_app",
            "base_authority_action",
            "base_authority_apikey",
            "gateway_rate_limit_api",
            "gateway_ip_limit_api",
        }
        if table not in allowed:
            raise ValueError("不允许的关联表")
        async with self.engine.connect() as conn:
            rows = (
                await conn.execute(
                    text(f"SELECT {target_column} FROM {table} WHERE {owner_column} = :owner"),
                    {"owner": owner_id},
                )
            ).scalars().all()
        return list(rows)

    async def link_rows(
        self, table: str, owner_column: str, owner_id: Any, target_table: str, target_pk: str
    ) -> list[dict[str, Any]]:
        allowed = {
            "base_role_user",
            "base_user_org",
            "base_authority_role",
            "base_authority_user",
            "base_authority_app",
            "base_authority_action",
            "base_authority_apikey",
            "gateway_rate_limit_api",
            "gateway_ip_limit_api",
        }
        if table not in allowed or target_table not in {value[0] for value in self.TABLES.values()}:
            raise ValueError("不允许的关联查询")
        async with self.engine.connect() as conn:
            rows = (
                await conn.execute(
                    text(
                        f"SELECT t.* FROM {table} l JOIN {target_table} t ON t.{target_pk} = l.{target_pk} "
                        f"WHERE l.{owner_column} = :owner ORDER BY t.{target_pk}"
                    ),
                    {"owner": owner_id},
                )
            ).mappings().all()
        return [_row(row) for row in rows]

    async def _meta(self, resource: str) -> tuple[set[str], str, bool]:
        if resource in self._metadata:
            return self._metadata[resource]
        table, fallback_pk = self._resource(resource)
        async with self.engine.connect() as conn:
            has_table = await conn.run_sync(lambda sync: inspect(sync).has_table(table))
            if not has_table:
                result = (set(), fallback_pk, False)
            else:
                columns_data = await conn.run_sync(lambda sync: inspect(sync).get_columns(table))
                pk_data = await conn.run_sync(lambda sync: inspect(sync).get_pk_constraint(table))
                pk = next(iter(pk_data.get("constrained_columns") or []), fallback_pk)
                autoincrement = any(
                    column["name"] == pk and column.get("autoincrement") in (True, "auto")
                    for column in columns_data
                )
                result = ({column["name"] for column in columns_data}, pk, autoincrement)
        self._metadata[resource] = result
        return result

    def _resource(self, resource: str) -> tuple[str, str]:
        if resource not in self.TABLES:
            raise ValueError(f"未知 Center 资源: {resource}")
        return self.TABLES[resource]


def new_secret(size: int = 32) -> str:
    return secrets.token_urlsafe(size)


def _new_id() -> int:
    return int(time.time_ns() // 1_000) + secrets.randbelow(1000)


def _snake(value: str) -> str:
    result = ""
    for char in value:
        result += "_" + char.lower() if char.isupper() else char
    return result.lstrip("_")


def _camel(value: str) -> str:
    head, *tail = value.split("_")
    return head + "".join(part[:1].upper() + part[1:] for part in tail)


def _row(row: Mapping[str, Any], include_secrets: bool = False) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in row.items():
        if not include_secrets and key.lower() in {
            "password",
            "secret_key",
            "private_key",
            "access_key_secret",
        }:
            continue
        if isinstance(value, datetime):
            value = value.isoformat()
        elif isinstance(value, int) and abs(value) > 9_007_199_254_740_991:
            value = str(value)
        result[_camel(key)] = value
    return result
