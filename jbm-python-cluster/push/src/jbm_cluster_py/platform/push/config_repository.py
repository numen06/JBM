from __future__ import annotations

from pathlib import Path
import json
import time
from datetime import datetime
from typing import Any, Dict, Iterable, Mapping, Optional

from sqlalchemy import inspect, text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from jbm_cluster_py.common.masterdata import java_page, page_form_from_body
from jbm_cluster_py.integrations.database import configured_database_url, require_tables

PUSH_CONFIG_TABLE = "push_config_info"
EMAIL_CONFIG_TABLE = "email_push_config"


class PushConfigRepository:
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
            await require_tables(self.engine, (PUSH_CONFIG_TABLE, EMAIL_CONFIG_TABLE))
            return
        async with self.engine.begin() as conn:
            if not await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table(PUSH_CONFIG_TABLE)):
                await conn.execute(text(PUSH_CONFIG_DDL))
            if not await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table(EMAIL_CONFIG_TABLE)):
                await conn.execute(text(EMAIL_CONFIG_DDL))

    async def stop(self) -> None:
        await self.engine.dispose()

    async def page_push_configs(self, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        payload = dict(body or {})
        entity = {key: value for key, value in payload.items() if key != "pageForm"}
        return await self._page(PUSH_CONFIG_TABLE, entity, payload, self._push_from_db)

    async def list_push_configs(self, body: Optional[Mapping[str, Any]]) -> list[Dict[str, Any]]:
        return (await self.page_push_configs({**dict(body or {}), "pageForm": {"currPage": 1, "pageSize": 1000}}))[
            "contents"
        ]

    async def save_push_config(self, entity: Mapping[str, Any]) -> Dict[str, Any]:
        row = self._push_to_db(entity)
        await self._save(PUSH_CONFIG_TABLE, row)
        return await self._get(PUSH_CONFIG_TABLE, int(row["id"]), self._push_from_db) or self._push_from_db(row)

    async def ensure_default_push_config(
        self,
        config_type: int,
        release_content: Mapping[str, Any],
        enable: bool = True,
    ) -> Optional[Dict[str, Any]]:
        if not release_content or await self._exists_by_column(PUSH_CONFIG_TABLE, "type", config_type):
            return None
        return await self.save_push_config(
            {
                "enable": enable,
                "type": config_type,
                "releaseContent": json.dumps(dict(release_content), ensure_ascii=False, indent=2, sort_keys=True),
            }
        )

    async def delete_push_configs(self, ids: Iterable[Any]) -> bool:
        return await self._delete(PUSH_CONFIG_TABLE, ids)

    async def page_email_configs(self, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        payload = dict(body or {})
        entity = dict(payload.get("entity") or payload)
        return await self._page(EMAIL_CONFIG_TABLE, entity, payload, self._email_from_db)

    async def list_email_configs(self, body: Optional[Mapping[str, Any]]) -> list[Dict[str, Any]]:
        return (await self.page_email_configs({"entity": dict(body or {}), "pageForm": {"currPage": 1, "pageSize": 1000}}))[
            "contents"
        ]

    async def save_email_config(self, entity: Mapping[str, Any]) -> Dict[str, Any]:
        row = self._email_to_db(entity)
        await self._save(EMAIL_CONFIG_TABLE, row)
        return await self._get(EMAIL_CONFIG_TABLE, int(row["id"]), self._email_from_db) or self._email_from_db(row)

    async def ensure_default_email_config(self, entity: Mapping[str, Any]) -> Optional[Dict[str, Any]]:
        if not entity.get("host") or await self._exists_by_column(EMAIL_CONFIG_TABLE, "host", entity.get("host")):
            return None
        return await self.save_email_config(entity)

    async def delete_email_configs(self, ids: Iterable[Any]) -> bool:
        return await self._delete(EMAIL_CONFIG_TABLE, ids)

    async def _page(
        self,
        table: str,
        entity: Mapping[str, Any],
        body: Mapping[str, Any],
        mapper: Any,
    ) -> Dict[str, Any]:
        page_form = page_form_from_body(body)
        where, params = self._where(entity)
        limit = max(int(page_form.page_size or 20), 1)
        offset = (max(int(page_form.curr_page or 1), 1) - 1) * limit
        async with self.engine.begin() as conn:
            total = (await conn.execute(text(f"SELECT COUNT(*) FROM {table} {where}"), params)).scalar_one()
            rows = (
                await conn.execute(
                    text(
                        f"""
                        SELECT * FROM {table}
                        {where}
                        ORDER BY COALESCE(update_time, create_time) DESC, id DESC
                        LIMIT :limit OFFSET :offset
                        """
                    ),
                    {**params, "limit": limit, "offset": offset},
                )
            ).mappings().all()
        return java_page([mapper(dict(row)) for row in rows], int(total), page_form)

    async def _get(self, table: str, row_id: int, mapper: Any) -> Optional[Dict[str, Any]]:
        async with self.engine.begin() as conn:
            row = (
                await conn.execute(text(f"SELECT * FROM {table} WHERE id=:id LIMIT 1"), {"id": row_id})
            ).mappings().first()
        return mapper(dict(row)) if row else None

    async def _save(self, table: str, row: Mapping[str, Any]) -> None:
        exists = await self._exists(table, int(row["id"]))
        async with self.engine.begin() as conn:
            if exists:
                assignments = ", ".join("%s=:%s" % (key, key) for key in row if key != "id")
                await conn.execute(text(f"UPDATE {table} SET {assignments} WHERE id=:id"), dict(row))
            else:
                columns = list(row)
                await conn.execute(
                    text(
                        f"""
                        INSERT INTO {table} ({", ".join(columns)})
                        VALUES ({", ".join(":" + key for key in columns)})
                        """
                    ),
                    dict(row),
                )

    async def _exists(self, table: str, row_id: int) -> bool:
        async with self.engine.begin() as conn:
            value = (
                await conn.execute(text(f"SELECT COUNT(*) FROM {table} WHERE id=:id"), {"id": row_id})
            ).scalar_one()
        return int(value) > 0

    async def _exists_by_column(self, table: str, column: str, value: Any) -> bool:
        async with self.engine.begin() as conn:
            count = (
                await conn.execute(text(f"SELECT COUNT(*) FROM {table} WHERE {column}=:value"), {"value": value})
            ).scalar_one()
        return int(count) > 0

    async def _delete(self, table: str, ids: Iterable[Any]) -> bool:
        id_values = [int(item) for item in ids if str(item).isdigit()]
        if not id_values:
            return False
        placeholders = ", ".join(f":id_{index}" for index, _ in enumerate(id_values))
        params = {f"id_{index}": value for index, value in enumerate(id_values)}
        async with self.engine.begin() as conn:
            result = await conn.execute(text(f"DELETE FROM {table} WHERE id IN ({placeholders})"), params)
        return bool(result.rowcount)

    def _where(self, entity: Mapping[str, Any]) -> tuple[str, Dict[str, Any]]:
        parts: list[str] = []
        params: Dict[str, Any] = {}
        for key, value in entity.items():
            if key in {"pageForm", "entity"} or value in (None, ""):
                continue
            column = _to_snake(key)
            parts.append(f"{column} = :{column}")
            params[column] = 1 if isinstance(value, bool) and value else 0 if isinstance(value, bool) else value
        return ("WHERE " + " AND ".join(parts), params) if parts else ("", params)

    def _push_to_db(self, value: Mapping[str, Any]) -> Dict[str, Any]:
        now = utc_now_iso()
        row_id = int(value.get("id") or current_millis())
        return {
            "id": row_id,
            "enable": 1 if _truthy(value.get("enable")) else 0,
            "type": value.get("type"),
            "release_content": value.get("releaseContent") or value.get("release_content"),
            "create_time": value.get("createTime") or now,
            "update_time": now,
        }

    def _push_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "id": row.get("id"),
            "enable": _truthy(row.get("enable")),
            "type": row.get("type"),
            "releaseContent": row.get("release_content"),
            "createTime": str(row.get("create_time") or ""),
            "updateTime": str(row.get("update_time") or ""),
        }

    def _email_to_db(self, value: Mapping[str, Any]) -> Dict[str, Any]:
        now = utc_now_iso()
        row_id = int(value.get("id") or current_millis())
        return {
            "id": row_id,
            "host": value.get("host"),
            "username": value.get("username"),
            "password": value.get("password"),
            "port": value.get("port"),
            "create_time": value.get("createTime") or now,
            "update_time": now,
        }

    def _email_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "id": row.get("id"),
            "host": row.get("host"),
            "username": row.get("username"),
            "password": row.get("password"),
            "port": row.get("port"),
            "createTime": str(row.get("create_time") or ""),
            "updateTime": str(row.get("update_time") or ""),
        }


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, (bytes, bytearray)):
        return any(value)
    if isinstance(value, (int, float)):
        return value != 0
    return str(value or "").strip().lower() in {"1", "true", "yes", "y", "on"}


def utc_now_iso() -> str:
    return datetime.now().isoformat()


def current_millis() -> int:
    return int(time.time() * 1000)


def _to_snake(value: str) -> str:
    text_value = str(value)
    result = []
    for char in text_value:
        if char.isupper():
            result.append("_")
            result.append(char.lower())
        else:
            result.append(char)
    return "".join(result).lstrip("_")


PUSH_CONFIG_DDL = f"""
CREATE TABLE IF NOT EXISTS {PUSH_CONFIG_TABLE} (
  id INTEGER PRIMARY KEY,
  enable INTEGER,
  type INTEGER,
  release_content TEXT,
  create_time VARCHAR(64),
  update_time VARCHAR(64)
)
"""

EMAIL_CONFIG_DDL = f"""
CREATE TABLE IF NOT EXISTS {EMAIL_CONFIG_TABLE} (
  id INTEGER PRIMARY KEY,
  host VARCHAR(255),
  username VARCHAR(255),
  password VARCHAR(512),
  port INTEGER,
  create_time VARCHAR(64),
  update_time VARCHAR(64)
)
"""
