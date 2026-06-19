from __future__ import annotations

import logging
from datetime import datetime
from pathlib import Path
from typing import Any, Mapping, Optional

from sqlalchemy import inspect, text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from jbm_cluster_py.integrations.database import configured_database_url

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
        if self._sqlite:
            db_path = database_url.replace("sqlite+aiosqlite:///", "", 1)
            if db_path and not db_path.startswith(":"):
                Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self.engine: AsyncEngine = create_async_engine(database_url, pool_pre_ping=True)

    async def start(self) -> None:
        if not self._sqlite:
            return
        async with self.engine.begin() as conn:
            for ddl in SQLITE_DDL:
                await conn.execute(text(ddl))

    async def stop(self) -> None:
        await self.engine.dispose()

    async def has_table(self, table_name: str) -> bool:
        async with self.engine.begin() as conn:
            return await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table(table_name))

    async def find_client(self, client_id: str) -> Optional[dict[str, Any]]:
        client_id = str(client_id or "").strip()
        if not client_id:
            return None
        async with self.engine.connect() as conn:
            if await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table("base_app")):
                row = (
                    await conn.execute(
                        text(
                            """
                            SELECT app_id, api_key, secret_key, app_type, status, public_key, private_key
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
                    return {
                        "clientId": data.get("api_key"),
                        "clientSecret": data.get("secret_key"),
                        "appId": _int_or_none(data.get("app_id")),
                        "appType": data.get("app_type"),
                        "publicKey": data.get("public_key"),
                        "privateKey": data.get("private_key"),
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

    async def find_account(self, username: str, account_type: str, domain: str) -> Optional[dict[str, Any]]:
        async with self.engine.connect() as conn:
            row = (
                await conn.execute(
                    text(
                        """
                        SELECT account_id, user_id, account, password, account_type, status, domain,
                               must_change_password
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

    async def user_roles(self, user_id: int) -> list[dict[str, Any]]:
        async with self.engine.connect() as conn:
            rows = (
                await conn.execute(
                    text(
                        """
                        SELECT ru.user_id, r.role_id, r.role_code, r.role_name, r.role_desc, r.status, r.parent_id
                        FROM base_role_user ru
                        INNER JOIN base_role r ON ru.role_id = r.role_id
                        WHERE ru.user_id = :user_id AND r.status = 1
                        """
                    ),
                    {"user_id": user_id},
                )
            ).mappings().all()
        return [dict(row) for row in rows]

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

    async def user_authorities(self, user_id: int, root: bool) -> list[str]:
        async with self.engine.connect() as conn:
            if root:
                rows = (
                    await conn.execute(
                        text("SELECT authority FROM base_authority WHERE status = 1 AND api_id IS NULL")
                    )
                ).mappings().all()
                return sorted({str(row["authority"]) for row in rows if row.get("authority")})

        roles = await self.user_roles(user_id)
        role_ids = await self.expanded_role_ids([int(row["role_id"]) for row in roles])
        authorities: set[str] = set()
        async with self.engine.connect() as conn:
            for role_id in role_ids:
                rows = (
                    await conn.execute(
                        text(
                            """
                            SELECT a.authority
                            FROM base_authority_role rp
                            INNER JOIN base_authority a ON rp.authority_id = a.authority_id
                            WHERE rp.role_id = :role_id AND a.status = 1
                            """
                        ),
                        {"role_id": role_id},
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
                        """
                    ),
                    {"user_id": user_id},
                )
            ).mappings().all()
            authorities.update(str(row["authority"]) for row in rows if row.get("authority"))
        return sorted(authorities)


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
    CREATE TABLE IF NOT EXISTS base_app (
      app_id INTEGER PRIMARY KEY,
      api_key VARCHAR(128) UNIQUE,
      secret_key VARCHAR(256),
      app_type VARCHAR(64),
      status INTEGER,
      public_key TEXT,
      private_key TEXT
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
      must_change_password INTEGER
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
      close_time VARCHAR(64)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS base_role (
      role_id INTEGER PRIMARY KEY,
      role_code VARCHAR(128),
      role_name VARCHAR(128),
      role_desc VARCHAR(512),
      status INTEGER,
      parent_id INTEGER
    )
    """,
    "CREATE TABLE IF NOT EXISTS base_role_user (user_id INTEGER, role_id INTEGER)",
    "CREATE TABLE IF NOT EXISTS base_authority (authority_id INTEGER PRIMARY KEY, authority VARCHAR(256), resource_type VARCHAR(32), menu_id INTEGER, api_id INTEGER, action_id INTEGER, status INTEGER)",
    "CREATE TABLE IF NOT EXISTS base_authority_role (authority_id INTEGER, role_id INTEGER, expire_time VARCHAR(64))",
    "CREATE TABLE IF NOT EXISTS base_authority_user (authority_id INTEGER, user_id INTEGER, expire_time VARCHAR(64))",
]
