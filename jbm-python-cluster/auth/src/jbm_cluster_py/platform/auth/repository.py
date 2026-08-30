from __future__ import annotations

import json
import logging
import secrets
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Mapping, Optional

from sqlalchemy import inspect, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine
from sqlalchemy.pool import NullPool

from jbm_cluster_py.integrations.database import configured_database_url, require_tables

logger = logging.getLogger(__name__)


def _int_or_none(value: Any) -> Optional[int]:
    if value is None or value == "":
        return None
    return int(value)


def _active_status(value: Any) -> bool:
    return value is None or int(value) == 1


def _row_dict(row: Any) -> Optional[dict[str, Any]]:
    if row is None:
        return None
    return dict(row._mapping)


class AuthRepository:
    def __init__(self, database_config: Mapping[str, Any]) -> None:
        database_url = configured_database_url(database_config) or "sqlite+aiosqlite:///./data/jbm-python-cluster.db"
        self.database_url = database_url
        self._sqlite = database_url.startswith("sqlite+aiosqlite:///")
        self._authority_relation_app_columns: tuple[bool, bool] | None = None
        if self._sqlite:
            db_path = database_url.replace("sqlite+aiosqlite:///", "", 1)
            if db_path and not db_path.startswith(":"):
                Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self.engine: AsyncEngine = create_async_engine(
            database_url, pool_pre_ping=True, poolclass=NullPool
        )

    async def start(self) -> None:
        if not self._sqlite:
            await require_tables(self.engine, ("base_app", "base_account", "base_user"))
            return
        async with self.engine.begin() as conn:
            for ddl in SQLITE_DDL:
                await conn.execute(text(ddl))

    async def stop(self) -> None:
        await self.engine.dispose()

    async def has_table(self, table_name: str) -> bool:
        async with self.engine.begin() as conn:
            return await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table(table_name))

    async def record_login(
        self,
        *,
        user_id: int | None,
        account: str,
        login_type: str,
        ip: str,
        user_agent: str,
        status: int = 1,
        message: str = "登录成功",
    ) -> None:
        if not await self.has_table("base_account_logs"):
            return
        now = datetime.now()
        async with self.engine.begin() as conn:
            await conn.execute(
                text(
                    """
                    INSERT INTO base_account_logs
                      (user_id, account, login_type, login_time, ip, user_agent, status, message, create_time, update_time)
                    VALUES
                      (:user_id, :account, :login_type, :login_time, :ip, :user_agent, :status, :message, :login_time, :login_time)
                    """
                ),
                {
                    "user_id": user_id,
                    "account": account,
                    "login_type": login_type,
                    "login_time": now,
                    "ip": ip[:64],
                    "user_agent": user_agent[:512],
                    "status": int(status),
                    "message": str(message or "")[:512],
                },
            )

    async def find_client(self, client_id: str) -> Optional[dict[str, Any]]:
        client_id = str(client_id or "").strip()
        if not client_id:
            return None
        async with self.engine.connect() as conn:
            if await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table("base_app")):
                columns = {
                    column["name"]
                    for column in await conn.run_sync(lambda sync_conn: inspect(sync_conn).get_columns("base_app"))
                }
                website_expr = "website" if "website" in columns else "NULL AS website"
                extend_data_expr = "extend_data" if "extend_data" in columns else "NULL AS extend_data"
                row = (
                    await conn.execute(
                        text(
                            f"""
                            SELECT app_id, api_key, secret_key, app_type, status, public_key, private_key,
                                   {website_expr}, {extend_data_expr}
                            FROM base_app
                            WHERE api_key = :client_id
                            LIMIT 1
                            """
                        ),
                        {"client_id": client_id},
                    )
                ).first()
                data = _row_dict(row)
                if data and _active_status(data.get("status")):
                    oauth = _client_oauth_settings(data)
                    registration = _client_registration_settings(data)
                    return {
                        "clientId": data.get("api_key"),
                        "clientSecret": data.get("secret_key"),
                        "appId": _int_or_none(data.get("app_id")),
                        "appType": data.get("app_type"),
                        "publicKey": data.get("public_key"),
                        "privateKey": data.get("private_key"),
                        "publicClient": bool(oauth.get("publicClient")),
                        "redirectUris": list(oauth.get("redirectUris") or []),
                        "registration": registration,
                        "source": "base_app",
                    }
            if await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table("base_api_key")):
                row = (
                    await conn.execute(
                        text(
                            """
                            SELECT key_id, api_key, secret_key, public_key, private_key, scope_modules, status,
                                   expire_time, revoke_time
                            FROM base_api_key
                            WHERE api_key = :client_id
                            LIMIT 1
                            """
                        ),
                        {"client_id": client_id},
                    )
                ).first()
                data = _row_dict(row)
                if data and _active_status(data.get("status")) and not data.get("revoke_time"):
                    return {
                        "clientId": data.get("api_key"),
                        "clientSecret": data.get("secret_key"),
                        "appId": _int_or_none(data.get("key_id")),
                        "publicKey": data.get("public_key"),
                        "privateKey": data.get("private_key"),
                        "scopeModules": data.get("scope_modules"),
                        "source": "base_api_key",
                    }
        return None

    async def list_clients(self) -> list[dict[str, Any]]:
        async with self.engine.connect() as conn:
            if not await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table("base_app")):
                return []
            columns = {
                col["name"]
                for col in await conn.run_sync(lambda sync_conn: inspect(sync_conn).get_columns("base_app"))
            }
            app_name_expr = "app_name" if "app_name" in columns else "NULL AS app_name"
            code_expr = "code" if "code" in columns else "NULL AS code"
            rows = (
                await conn.execute(
                    text(
                        f"""
                        SELECT app_id, api_key, {app_name_expr}, {code_expr}, app_type, status
                        FROM base_app
                        WHERE api_key IS NOT NULL AND api_key != '' AND (status IS NULL OR status = 1)
                        ORDER BY app_id DESC
                        LIMIT 200
                        """
                    )
                )
            ).mappings().all()
        return [
            {
                "appId": _int_or_none(row.get("app_id")),
                "appName": row.get("app_name") or row.get("code") or row.get("api_key"),
                "code": row.get("code"),
                "clientId": row.get("api_key"),
                "appType": row.get("app_type"),
                "status": _int_or_none(row.get("status")),
            }
            for row in rows
        ]

    async def find_account(self, username: str, account_type: str, domain: str) -> Optional[dict[str, Any]]:
        async with self.engine.connect() as conn:
            row = (
                await conn.execute(
                    text(
                        """
                        SELECT account_id, user_id, account, password, account_type, status, domain,
                               must_change_password, update_time
                        FROM base_account
                        WHERE account = :account AND account_type = :account_type AND domain = :domain
                        LIMIT 1
                        """
                    ),
                    {"account": username, "account_type": account_type, "domain": domain},
                )
            ).first()
        return _row_dict(row)

    async def find_user(self, user_id: int) -> Optional[dict[str, Any]]:
        async with self.engine.connect() as conn:
            row = (
                await conn.execute(
                    text(
                        """
                        SELECT user_id, user_name, user_type, company_id, department_id, nick_name, real_name,
                               avatar, email, mobile, status, close_time
                        FROM base_user
                        WHERE user_id = :user_id
                        LIMIT 1
                        """
                    ),
                    {"user_id": user_id},
                )
            ).first()
        return _row_dict(row)

    async def find_users_by_mobile(self, mobile: str) -> list[dict[str, Any]]:
        async with self.engine.connect() as conn:
            rows = (
                await conn.execute(
                    text(
                        """
                        SELECT user_id, user_name, user_type, company_id, department_id, nick_name, real_name,
                               avatar, email, mobile, status, close_time
                        FROM base_user
                        WHERE mobile = :mobile AND status = 1 AND close_time IS NULL
                        LIMIT 2
                        """
                    ),
                    {"mobile": mobile},
                )
            ).mappings().all()
        return [dict(row) for row in rows]

    async def find_users_by_email(self, email: str) -> list[dict[str, Any]]:
        async with self.engine.connect() as conn:
            rows = (
                await conn.execute(
                    text(
                        """
                        SELECT user_id, user_name, user_type, company_id, department_id, nick_name, real_name,
                               avatar, email, mobile, status, close_time
                        FROM base_user
                        WHERE email = :email AND status = 1 AND close_time IS NULL
                        LIMIT 2
                        """
                    ),
                    {"email": email.lower()},
                )
            ).mappings().all()
        return [dict(row) for row in rows]

    async def password_accounts_for_user(self, user_id: int) -> list[dict[str, Any]]:
        async with self.engine.connect() as conn:
            rows = (
                await conn.execute(
                    text(
                        "SELECT account_id,user_id,account,password,account_type,status,domain,"
                        "must_change_password FROM base_account "
                        "WHERE user_id=:user_id AND status=1 AND password IS NOT NULL AND password<>'' "
                        "ORDER BY CASE WHEN account_type='username' THEN 0 ELSE 1 END,account_id"
                    ),
                    {"user_id": user_id},
                )
            ).mappings().all()
        return [dict(row) for row in rows]

    async def bind_mobile_account(
        self, user_id: int, mobile: str, domain: str, password_hash: str | None = None
    ) -> dict[str, Any]:
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        try:
            async with self.engine.begin() as conn:
                user = (
                    await conn.execute(
                        text(
                            "SELECT user_id FROM base_user "
                            "WHERE user_id=:user_id AND status=1 AND close_time IS NULL"
                        ),
                        {"user_id": user_id},
                    )
                ).first()
                if not user:
                    raise ValueError("用户不存在或已停用")
                owner = (
                    await conn.execute(
                        text("SELECT user_id FROM base_user WHERE mobile=:mobile AND user_id<>:user_id LIMIT 1"),
                        {"mobile": mobile, "user_id": user_id},
                    )
                ).first()
                if owner:
                    raise ValueError("手机号已绑定其他用户")
                existing = (
                    await conn.execute(
                        text(
                            "SELECT account_id,user_id FROM base_account "
                            "WHERE account=:mobile AND account_type='mobile' AND domain=:domain LIMIT 1"
                        ),
                        {"mobile": mobile, "domain": domain},
                    )
                ).mappings().first()
                if existing:
                    if int(existing["user_id"]) != int(user_id):
                        raise ValueError("手机号已绑定其他用户")
                await conn.execute(
                    text(
                        "UPDATE base_account SET status=0,update_time=:now "
                        "WHERE user_id=:user_id AND account_type='mobile' AND domain=:domain"
                    ),
                    {"user_id": user_id, "domain": domain, "now": now},
                )
                if existing:
                    await conn.execute(
                        text(
                            "UPDATE base_account SET status=1,password=COALESCE(:password,password),"
                            "update_time=:now WHERE account_id=:account_id"
                        ),
                        {"account_id": existing["account_id"], "password": password_hash, "now": now},
                    )
                else:
                    await _insert_available(
                        conn,
                        "base_account",
                        {
                            "account_id": _new_id(),
                            "user_id": user_id,
                            "account": mobile,
                            "password": password_hash,
                            "account_type": "mobile",
                            "status": 1,
                            "domain": domain,
                            "must_change_password": 0,
                            "create_time": now,
                            "update_time": now,
                        },
                    )
                await conn.execute(
                    text("UPDATE base_user SET mobile=:mobile,update_time=:now WHERE user_id=:user_id"),
                    {"mobile": mobile, "now": now, "user_id": user_id},
                )
        except IntegrityError as exc:
            existing = await self.find_account(mobile, "mobile", domain)
            if not existing or int(existing["user_id"]) != int(user_id):
                raise ValueError("手机号已绑定其他用户") from exc
        return await self.find_account(mobile, "mobile", domain) or {}

    async def bind_email_account(
        self, user_id: int, email: str, domain: str, password_hash: str
    ) -> dict[str, Any]:
        email = email.lower()
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        try:
            async with self.engine.begin() as conn:
                user = (
                    await conn.execute(
                        text(
                            "SELECT user_id FROM base_user "
                            "WHERE user_id=:user_id AND status=1 AND close_time IS NULL"
                        ),
                        {"user_id": user_id},
                    )
                ).first()
                if not user:
                    raise ValueError("用户不存在或已停用")
                owner = (
                    await conn.execute(
                        text("SELECT user_id FROM base_user WHERE email=:email AND user_id<>:user_id LIMIT 1"),
                        {"email": email, "user_id": user_id},
                    )
                ).first()
                if owner:
                    raise ValueError("邮箱已绑定其他用户")
                existing = (
                    await conn.execute(
                        text(
                            "SELECT account_id,user_id FROM base_account "
                            "WHERE account=:email AND account_type='email' AND domain=:domain LIMIT 1"
                        ),
                        {"email": email, "domain": domain},
                    )
                ).mappings().first()
                if existing:
                    if int(existing["user_id"]) != int(user_id):
                        raise ValueError("邮箱已绑定其他用户")
                await conn.execute(
                    text(
                        "UPDATE base_account SET status=0,update_time=:now "
                        "WHERE user_id=:user_id AND account_type='email' AND domain=:domain"
                    ),
                    {"user_id": user_id, "domain": domain, "now": now},
                )
                if existing:
                    await conn.execute(
                        text(
                            "UPDATE base_account SET status=1,password=:password,update_time=:now "
                            "WHERE account_id=:account_id"
                        ),
                        {"account_id": existing["account_id"], "password": password_hash, "now": now},
                    )
                else:
                    await _insert_available(
                        conn,
                        "base_account",
                        {
                            "account_id": _new_id(),
                            "user_id": user_id,
                            "account": email,
                            "password": password_hash,
                            "account_type": "email",
                            "status": 1,
                            "domain": domain,
                            "must_change_password": 0,
                            "create_time": now,
                            "update_time": now,
                        },
                    )
                await conn.execute(
                    text("UPDATE base_user SET email=:email,update_time=:now WHERE user_id=:user_id"),
                    {"email": email, "now": now, "user_id": user_id},
                )
        except IntegrityError as exc:
            existing = await self.find_account(email, "email", domain)
            if not existing or int(existing["user_id"]) != int(user_id):
                raise ValueError("邮箱已绑定其他用户") from exc
        return await self.find_account(email, "email", domain) or {}

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
        async with self.engine.connect() as conn:
            rows = (
                await conn.execute(
                    text(
                        """
                        SELECT ru.user_id, r.role_id, r.role_code, r.role_name, r.role_desc, r.status, r.parent_id
                        FROM base_role_user ru
                        INNER JOIN base_role r ON ru.role_id = r.role_id
                        WHERE ru.user_id = :user_id AND r.status = 1
                        """ + app_clause + tenant_clause
                        + """
                        ORDER BY r.role_id
                        """
                    ),
                    params,
                )
            ).mappings().all()
        return [dict(row) for row in rows]

    async def tenant_app_enabled(self, tenant_id: int, app_id: int) -> bool:
        async with self.engine.connect() as conn:
            enabled = (
                await conn.execute(
                    text(
                        "SELECT 1 FROM base_tenant_app "
                        "WHERE tenant_id = :tenant_id AND app_id = :app_id AND status = 1 LIMIT 1"
                    ),
                    {"tenant_id": tenant_id, "app_id": app_id},
                )
            ).first()
        return enabled is not None

    async def expanded_role_ids(self, role_ids: list[int]) -> list[int]:
        expanded: list[int] = []
        visiting: set[int] = set()
        for role_id in role_ids:
            await self._collect_role_ancestor(role_id, expanded, visiting)
        return expanded

    async def _collect_role_ancestor(self, role_id: int, expanded: list[int], visiting: set[int]) -> None:
        if role_id in visiting:
            return
        visiting.add(role_id)
        if role_id not in expanded:
            expanded.append(role_id)
        async with self.engine.connect() as conn:
            row = (
                await conn.execute(
                    text("SELECT parent_id FROM base_role WHERE role_id = :role_id"),
                    {"role_id": role_id},
                )
            ).first()
        parent_id = _int_or_none(row[0] if row else None)
        if parent_id:
            await self._collect_role_ancestor(parent_id, expanded, visiting)
        visiting.remove(role_id)

    async def user_authorities(
        self,
        user_id: int,
        root: bool,
        app_id: int | None = None,
        tenant_id: int | None = None,
    ) -> list[str]:
        app_clause = "" if app_id is None else " AND (app_id IS NULL OR app_id = :app_id)"
        params: dict[str, Any] = {"user_id": user_id}
        if app_id is not None:
            params["app_id"] = app_id
        async with self.engine.connect() as conn:
            if root:
                rows = (
                    await conn.execute(
                        text("SELECT authority FROM base_authority WHERE status = 1 AND api_id IS NULL" + app_clause),
                        params,
                    )
                ).mappings().all()
                return sorted({str(row["authority"]) for row in rows if row.get("authority")})

        roles = await self.user_roles(user_id, app_id, tenant_id)
        role_ids = await self.expanded_role_ids([int(row["role_id"]) for row in roles])
        authorities: set[str] = set()
        async with self.engine.connect() as conn:
            if self._authority_relation_app_columns is None:
                role_relation_columns = {
                    column["name"]
                    for column in await conn.run_sync(
                        lambda sync_conn: inspect(sync_conn).get_columns("base_authority_role")
                    )
                }
                user_relation_columns = {
                    column["name"]
                    for column in await conn.run_sync(
                        lambda sync_conn: inspect(sync_conn).get_columns("base_authority_user")
                    )
                }
                self._authority_relation_app_columns = (
                    "app_id" in role_relation_columns,
                    "app_id" in user_relation_columns,
                )
            role_has_app_id, user_has_app_id = self._authority_relation_app_columns
            role_app_clause = (
                " AND (:app_id IS NULL OR (rp.app_id IS NULL OR rp.app_id = :app_id))"
                if role_has_app_id
                else ""
            )
            user_app_clause = (
                " AND (:app_id IS NULL OR (up.app_id IS NULL OR up.app_id = :app_id))"
                if user_has_app_id
                else ""
            )
            for role_id in role_ids:
                rows = (
                    await conn.execute(
                        text(
                            """
                            SELECT a.authority
                            FROM base_authority_role rp
                            INNER JOIN base_authority a ON rp.authority_id = a.authority_id
                            WHERE rp.role_id = :role_id AND a.status = 1
                              AND (:app_id IS NULL OR (a.app_id IS NULL OR a.app_id = :app_id))
                            """
                            + role_app_clause
                        ),
                        {"role_id": role_id, "app_id": app_id},
                    )
                ).mappings().all()
                authorities.update(str(row["authority"]) for row in rows if row.get("authority"))
            rows = (
                await conn.execute(
                    text(
                        """
                        SELECT a.authority
                        FROM base_authority_user up
                        INNER JOIN base_authority a ON up.authority_id = a.authority_id
                        WHERE up.user_id = :user_id AND a.status = 1
                          AND (:app_id IS NULL OR (a.app_id IS NULL OR a.app_id = :app_id))
                        """
                        + user_app_clause
                    ),
                    {"user_id": user_id, "app_id": app_id},
                )
            ).mappings().all()
            authorities.update(str(row["authority"]) for row in rows if row.get("authority"))
        return sorted(authorities)

    async def create_user_account(
        self,
        username: str,
        password_hash: str,
        nick_name: str | None = None,
        email: str | None = None,
        mobile: str | None = None,
        domain: str = "@admin.com",
    ) -> dict[str, Any]:
        account_type = infer_account_type(username)
        existing = await self.find_account(username, account_type, domain)
        if existing:
            raise ValueError("账号已存在")
        user_id = int(time.time() * 1000_000)
        account_id = user_id + 1
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        async with self.engine.begin() as conn:
            if mobile:
                mobile_owner = (
                    await conn.execute(
                        text("SELECT user_id FROM base_user WHERE mobile=:mobile LIMIT 1"),
                        {"mobile": mobile},
                    )
                ).first()
                if mobile_owner:
                    raise ValueError("手机号已归属其他用户")
            user_columns = {column["name"] for column in await conn.run_sync(lambda sync_conn: inspect(sync_conn).get_columns("base_user"))}
            account_columns = {
                column["name"]
                for column in await conn.run_sync(lambda sync_conn: inspect(sync_conn).get_columns("base_account"))
            }
            user_values = {
                "user_id": user_id,
                "user_name": username,
                "user_type": "normal",
                "nick_name": nick_name or username,
                "real_name": nick_name or username,
                "avatar": None,
                "email": email,
                "mobile": mobile,
                "status": 1,
                "close_time": None,
                "create_time": now,
                "update_time": now,
            }
            user_values = {key: value for key, value in user_values.items() if key in user_columns}
            await conn.execute(
                text(
                    "INSERT INTO base_user (%s) VALUES (%s)"
                    % (
                        ", ".join(user_values),
                        ", ".join(":" + key for key in user_values),
                    )
                ),
                user_values,
            )
            account_values = {
                "account_id": account_id,
                "user_id": user_id,
                "account": username,
                "password": password_hash,
                "account_type": account_type,
                "status": 1,
                "domain": domain,
                "must_change_password": 0,
                "create_time": now,
                "update_time": now,
            }
            account_values = {key: value for key, value in account_values.items() if key in account_columns}
            await conn.execute(
                text(
                    "INSERT INTO base_account (%s) VALUES (%s)"
                    % (
                        ", ".join(account_values),
                        ", ".join(":" + key for key in account_values),
                    )
                ),
                account_values,
            )
        return {
            "userId": user_id,
            "accountId": account_id,
            "userName": username,
            "accountType": account_type,
        }

    async def create_tenant_account(
        self,
        *,
        app_id: int,
        default_role_code: str,
        tenant_name: str,
        org_type: str = "personal",
        username: str,
        password_hash: str,
        nick_name: str | None = None,
        email: str | None = None,
        mobile: str | None = None,
        domain: str = "@admin.com",
    ) -> dict[str, Any]:
        account_type = infer_account_type(username)
        tenant_id = _new_id()
        user_id = _new_id()
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        try:
            async with self.engine.begin() as conn:
                existing = (
                    await conn.execute(
                        text(
                            "SELECT 1 FROM base_account "
                            "WHERE account=:account AND account_type=:account_type AND domain=:domain LIMIT 1"
                        ),
                        {"account": username, "account_type": account_type, "domain": domain},
                    )
                ).first()
                if existing:
                    raise ValueError("账号已存在")

                if mobile:
                    mobile_owner = (
                        await conn.execute(
                            text("SELECT user_id FROM base_user WHERE mobile=:mobile LIMIT 1"),
                            {"mobile": mobile},
                        )
                    ).first()
                    if mobile_owner:
                        raise ValueError("手机号已归属其他用户")

                if mobile and not (account_type == "mobile" and mobile == username):
                    existing_mobile = (
                        await conn.execute(
                            text(
                                "SELECT 1 FROM base_account "
                                "WHERE account=:account AND account_type='mobile' AND domain=:domain LIMIT 1"
                            ),
                            {"account": mobile, "domain": domain},
                        )
                    ).first()
                    if existing_mobile:
                        raise ValueError("手机号已绑定账号")

                role = (
                    await conn.execute(
                        text(
                            "SELECT role_id FROM base_role "
                            "WHERE role_code=:role_code AND status=1 "
                            "AND (app_id=:app_id OR app_id IS NULL) "
                            "ORDER BY CASE WHEN app_id=:app_id THEN 0 ELSE 1 END LIMIT 1"
                        ),
                        {"role_code": default_role_code, "app_id": app_id},
                    )
                ).first()
                if not role:
                    raise ValueError("应用注册默认角色不存在或未启用")
                role_id = int(role[0])

                await _insert_available(
                    conn,
                    "base_org",
                    {
                        "id": tenant_id,
                        "app_id": None,
                        "parent_id": None,
                        "level": 1,
                        "leaf_path": str(tenant_id),
                        "org_name": tenant_name,
                        "org_type": org_type,
                        "manager_id": user_id,
                        "group_id": str(tenant_id),
                        "org_code": f"tenant-{tenant_id}",
                        "status": 1,
                        "create_time": now,
                        "update_time": now,
                        "extend_data": json.dumps(
                            {
                                "tenant": True,
                                "organizationType": org_type,
                                "onboardingAppId": app_id,
                            },
                            ensure_ascii=False,
                        ),
                    },
                )
                await _insert_available(
                    conn,
                    "base_user",
                    {
                        "user_id": user_id,
                        "user_name": username,
                        "user_type": "tenant",
                        "company_id": tenant_id,
                        "nick_name": nick_name or username,
                        "real_name": nick_name or username,
                        "email": email,
                        "mobile": mobile,
                        "status": 1,
                        "create_time": now,
                        "update_time": now,
                    },
                )
                await _insert_available(
                    conn,
                    "base_account",
                    {
                        "account_id": _new_id(),
                        "user_id": user_id,
                        "account": username,
                        "password": password_hash,
                        "account_type": account_type,
                        "status": 1,
                        "domain": domain,
                        "must_change_password": 0,
                        "create_time": now,
                        "update_time": now,
                    },
                )
                if mobile and not (account_type == "mobile" and mobile == username):
                    await _insert_available(
                        conn,
                        "base_account",
                        {
                            "account_id": _new_id(),
                            "user_id": user_id,
                            "account": mobile,
                            "password": password_hash,
                            "account_type": "mobile",
                            "status": 1,
                            "domain": domain,
                            "must_change_password": 0,
                            "create_time": now,
                            "update_time": now,
                        },
                    )
                await _insert_available(
                    conn,
                    "base_user_org",
                    {
                        "id": _new_id(),
                        "user_id": user_id,
                        "org_id": tenant_id,
                        "create_time": now,
                        "update_time": now,
                    },
                )
                await _insert_available(
                    conn,
                    "base_tenant_app",
                    {
                        "id": _new_id(),
                        "tenant_id": tenant_id,
                        "app_id": app_id,
                        "status": 1,
                        "create_time": now,
                        "update_time": now,
                        "extend_data": json.dumps(
                            {"source": "self_registration", "ownerUserId": user_id},
                            ensure_ascii=False,
                        ),
                    },
                )
                await _insert_available(
                    conn,
                    "base_role_user",
                    {
                        "id": _new_id(),
                        "app_id": app_id,
                        "user_id": user_id,
                        "role_id": role_id,
                        "tenant_id": tenant_id,
                        "create_time": now,
                        "update_time": now,
                        "extend_data": json.dumps({"tenantOwner": True}, ensure_ascii=False),
                    },
                )
        except IntegrityError as exc:
            raise ValueError("账号已存在或注册数据冲突") from exc

        return {
            "tenantId": tenant_id,
            "tenantName": tenant_name,
            "userId": user_id,
            "userName": username,
            "accountType": account_type,
            "appId": app_id,
            "roleCode": default_role_code,
        }


def _client_oauth_settings(data: Mapping[str, Any]) -> dict[str, Any]:
    parsed = _extend_data(data)
    oauth = parsed.get("oauth") if isinstance(parsed, Mapping) else {}
    if not isinstance(oauth, Mapping):
        oauth = {}
    redirect_uris = oauth.get("redirectUris") or oauth.get("redirect_uris") or []
    if isinstance(redirect_uris, str):
        redirect_uris = [item.strip() for item in redirect_uris.split(",") if item.strip()]
    website = str(data.get("website") or "").strip()
    if not redirect_uris and website and website != "*":
        redirect_uris = [website]
    return {
        "publicClient": bool(oauth.get("publicClient") or oauth.get("public_client")),
        "redirectUris": [str(item) for item in redirect_uris if str(item).strip()],
    }


def _client_registration_settings(data: Mapping[str, Any]) -> dict[str, Any]:
    raw = _extend_data(data).get("registration")
    registration = dict(raw) if isinstance(raw, Mapping) else {}
    return {
        "enabled": bool(registration.get("enabled")),
        "mode": str(registration.get("mode") or "tenant"),
        "defaultRoleCode": str(
            registration.get("defaultRoleCode") or registration.get("default_role_code") or ""
        ).strip(),
    }


def _extend_data(data: Mapping[str, Any]) -> dict[str, Any]:
    raw = data.get("extend_data")
    if isinstance(raw, Mapping):
        return dict(raw)
    try:
        parsed = json.loads(str(raw)) if raw else {}
    except (TypeError, ValueError):
        return {}
    return dict(parsed) if isinstance(parsed, Mapping) else {}


async def _insert_available(conn: Any, table: str, values: Mapping[str, Any]) -> None:
    columns = {
        column["name"]
        for column in await conn.run_sync(lambda sync_conn: inspect(sync_conn).get_columns(table))
    }
    selected = {key: value for key, value in values.items() if key in columns}
    await conn.execute(
        text(
            "INSERT INTO %s (%s) VALUES (%s)"
            % (table, ", ".join(selected), ", ".join(":" + key for key in selected))
        ),
        selected,
    )


def _new_id() -> int:
    return (int(time.time() * 1000) << 20) | secrets.randbelow(1 << 20)


def infer_account_type(username: str) -> str:
    if "@" in username:
        return "email"
    if username.isdigit() and len(username) >= 6:
        return "mobile"
    return "username"


def user_is_active(user: Mapping[str, Any]) -> bool:
    if not _active_status(user.get("status")):
        return False
    close_time = user.get("close_time")
    if not close_time:
        return True
    if isinstance(close_time, datetime):
        return close_time.date() >= datetime.now().date()
    try:
        text_value = str(close_time).strip().replace("T", " ")
        return datetime.fromisoformat(text_value).date() >= datetime.now().date()
    except ValueError:
        logger.warning("Ignore unparsable base_user.close_time=%s", close_time)
        return True


SQLITE_DDL = [
    """
    CREATE TABLE IF NOT EXISTS base_account_logs (
      account_log_id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER,
      account VARCHAR(128),
      login_type VARCHAR(32),
      login_time VARCHAR(64),
      ip VARCHAR(64),
      user_agent VARCHAR(512),
      status INTEGER,
      message VARCHAR(512),
      create_time VARCHAR(64),
      update_time VARCHAR(64)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_app (
      app_id INTEGER PRIMARY KEY,
      api_key VARCHAR(128) UNIQUE,
      secret_key VARCHAR(256),
      app_type VARCHAR(64),
      status INTEGER,
      public_key TEXT,
      private_key TEXT,
      website VARCHAR(512),
      extend_data TEXT
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_api_key (
      key_id INTEGER PRIMARY KEY,
      api_key VARCHAR(128) UNIQUE,
      secret_key VARCHAR(256),
      public_key TEXT,
      private_key TEXT,
      scope_modules TEXT,
      status INTEGER,
      expire_time VARCHAR(64),
      revoke_time VARCHAR(64)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_account (
      account_id INTEGER PRIMARY KEY,
      user_id INTEGER,
      account VARCHAR(128),
      password VARCHAR(256),
      account_type VARCHAR(32),
      status INTEGER,
      domain VARCHAR(128),
      must_change_password INTEGER,
      create_time VARCHAR(64),
      update_time VARCHAR(64)
      , UNIQUE (account, account_type, domain)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_user (
      user_id INTEGER PRIMARY KEY,
      user_name VARCHAR(128),
      user_type VARCHAR(64),
      company_id INTEGER,
      department_id INTEGER,
      nick_name VARCHAR(128),
      real_name VARCHAR(128),
      avatar VARCHAR(512),
      email VARCHAR(128),
      mobile VARCHAR(64),
      status INTEGER,
      close_time VARCHAR(64),
      create_time VARCHAR(64),
      update_time VARCHAR(64),
      UNIQUE (mobile),
      UNIQUE (email)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_role (
      role_id INTEGER PRIMARY KEY,
      app_id INTEGER,
      role_code VARCHAR(128),
      role_name VARCHAR(128),
      role_desc VARCHAR(512),
      status INTEGER,
      parent_id INTEGER
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_org (
      id INTEGER PRIMARY KEY, app_id INTEGER, parent_id INTEGER, level INTEGER,
      leaf_path VARCHAR(512), org_name VARCHAR(128), org_type VARCHAR(32),
      manager_id INTEGER, group_id VARCHAR(64), org_code VARCHAR(64), status INTEGER,
      create_time VARCHAR(64), update_time VARCHAR(64), extend_data TEXT
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_user_org (
      id INTEGER PRIMARY KEY, user_id INTEGER, org_id INTEGER,
      create_time VARCHAR(64), update_time VARCHAR(64), UNIQUE (user_id, org_id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_role_user (
      id INTEGER PRIMARY KEY, user_id INTEGER, role_id INTEGER, app_id INTEGER,
      tenant_id INTEGER, create_time VARCHAR(64), update_time VARCHAR(64), extend_data TEXT,
      UNIQUE (tenant_id, app_id, user_id, role_id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_tenant_app (
      id INTEGER PRIMARY KEY, tenant_id INTEGER, app_id INTEGER, status INTEGER,
      create_time VARCHAR(64), update_time VARCHAR(64), extend_data TEXT,
      UNIQUE (tenant_id, app_id)
    )
    """,
    "CREATE TABLE IF NOT EXISTS base_authority (authority_id INTEGER PRIMARY KEY, app_id INTEGER, authority VARCHAR(256), resource_type VARCHAR(32), menu_id INTEGER, api_id INTEGER, action_id INTEGER, status INTEGER)",
    "CREATE TABLE IF NOT EXISTS base_authority_role (authority_id INTEGER, role_id INTEGER, app_id INTEGER, expire_time VARCHAR(64))",
    "CREATE TABLE IF NOT EXISTS base_authority_user (authority_id INTEGER, user_id INTEGER, app_id INTEGER, expire_time VARCHAR(64))",
]
