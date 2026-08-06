from __future__ import annotations

import json
import logging
import uuid
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Mapping, Optional

from sqlalchemy import inspect, text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from jbm_cluster_py.common.masterdata import java_page, page_form_from_body
from jbm_cluster_py.integrations.database import configured_database_url, require_tables

logger = logging.getLogger(__name__)

BODY_TABLE = "push_message_body"
ITEM_TABLE = "push_message_item"

EXCLUDE_TEST_MESSAGE_BODY_SQL = """
SELECT id FROM push_message_body
WHERE template_code = '__push_test__'
   OR (
        (template_code IS NULL OR template_code <> '__push_test_visible__')
        AND title IN ('Push通讯测试', 'Push 通讯测试')
      )
"""


def now_text() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def _parse_extend(value: Any) -> Dict[str, Any]:
    if value is None:
        return {}
    if isinstance(value, dict):
        return dict(value)
    if isinstance(value, str) and value.strip():
        try:
            parsed = json.loads(value)
            return dict(parsed) if isinstance(parsed, dict) else {}
        except json.JSONDecodeError:
            return {}
    return {}


def _bool_value(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return value != 0
    if isinstance(value, str):
        return value.strip().lower() in {"1", "true", "yes", "y"}
    return bool(value)


class PushMessageRepository:
    def __init__(self, database_config: Mapping[str, Any]) -> None:
        database_url = configured_database_url(database_config) or "sqlite+aiosqlite:///./data/jbm-python-cluster.db"
        self.database_url = database_url
        self._sqlite = database_url.startswith("sqlite+aiosqlite:///")
        if self._sqlite:
            db_path = database_url.replace("sqlite+aiosqlite:///", "", 1)
            if db_path and not db_path.startswith(":"):
                Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self.engine: AsyncEngine = create_async_engine(database_url, pool_pre_ping=True)
        self._body_columns: set[str] = set()
        self._item_columns: set[str] = set()
        self._extend_column: Optional[str] = None

    async def start(self) -> None:
        if not self._sqlite:
            await require_tables(self.engine, (BODY_TABLE, ITEM_TABLE))
        async with self.engine.begin() as conn:
            has_body = await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table(BODY_TABLE))
            has_item = await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table(ITEM_TABLE))
            if has_body and has_item:
                await self._load_columns(conn)
                return
            await conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {BODY_TABLE} (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      send_user_id BIGINT,
                      title VARCHAR(512),
                      tags VARCHAR(512),
                      content TEXT,
                      template_code VARCHAR(256),
                      type VARCHAR(64),
                      level INTEGER,
                      url VARCHAR(1024),
                      extend_data TEXT,
                      create_time VARCHAR(64),
                      update_time VARCHAR(64)
                    )
                    """
                )
            )
            await conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {ITEM_TABLE} (
                      msg_id VARCHAR(64) PRIMARY KEY,
                      msg_body_id BIGINT,
                      rec_user_id BIGINT,
                      send_user_id BIGINT,
                      push_status VARCHAR(32),
                      push_way VARCHAR(32),
                      read_flag INTEGER,
                      create_time VARCHAR(64),
                      update_time VARCHAR(64)
                    )
                    """
                )
            )
            await self._load_columns(conn)

    async def stop(self) -> None:
        await self.engine.dispose()

    @property
    def enabled(self) -> bool:
        return self.engine is not None

    async def save_message(self, message: Mapping[str, Any]) -> Dict[str, Any]:
        now = now_text()
        row = dict(message)
        msg_id = str(row.get("msgId") or uuid.uuid4().hex)
        sys_msg = bool(row.get("sysMsg"))
        send_user_id = None if sys_msg else row.get("sendUserId")
        extend = _parse_extend(row.get("extend"))
        extend_json = json.dumps(extend, ensure_ascii=False) if extend else None
        push_status = str(row.get("pushStatus") or "issued")
        push_way = str(row.get("pushWay") or "internal")
        rec_user_id = row.get("recUserId")
        body_values: Dict[str, Any] = {
            "send_user_id": send_user_id,
            "title": row.get("title"),
            "content": row.get("content"),
            "type": row.get("type") or "notification",
            "level": row.get("level") or 0,
            "url": row.get("url"),
            "template_code": row.get("templateCode"),
            "create_time": now,
            "update_time": now,
        }
        if self._extend_column:
            body_values[self._extend_column] = extend_json
        columns = [name for name in body_values if not self._body_columns or name in self._body_columns]
        placeholders = [f":{name}" for name in columns]
        async with self.engine.begin() as conn:
            body_result = await conn.execute(
                text(
                    f"""
                    INSERT INTO {BODY_TABLE}
                      ({", ".join(columns)})
                    VALUES
                      ({", ".join(placeholders)})
                    """
                ),
                {name: body_values[name] for name in columns},
            )
            body_id = int(body_result.lastrowid or 0)
            await conn.execute(
                text(
                    f"""
                    INSERT INTO {ITEM_TABLE}
                      (msg_id, msg_body_id, rec_user_id, send_user_id, push_status, push_way, read_flag, create_time, update_time)
                    VALUES
                      (:msg_id, :msg_body_id, :rec_user_id, :send_user_id, :push_status, :push_way, :read_flag, :create_time, :update_time)
                    """
                ),
                {
                    "msg_id": msg_id,
                    "msg_body_id": body_id,
                    "rec_user_id": rec_user_id,
                    "send_user_id": send_user_id,
                    "push_status": push_status,
                    "push_way": push_way,
                    "read_flag": 1 if _bool_value(row.get("readFlag")) else 0,
                    "create_time": now,
                    "update_time": now,
                },
            )
        saved = dict(row)
        saved["msgId"] = msg_id
        saved["msgBodyId"] = body_id
        saved["pushStatus"] = push_status
        saved["createTime"] = now
        return saved

    async def page_messages(
        self,
        body: Optional[Mapping[str, Any]] = None,
        current_user_id: Optional[int] = None,
    ) -> Dict[str, Any]:
        payload = dict(body or {})
        page_form = page_form_from_body(payload)
        where_parts: List[str] = []
        params: Dict[str, Any] = {}

        if not _bool_value(payload.get("includeTestMessages")):
            where_parts.append(f"i.msg_body_id NOT IN ({EXCLUDE_TEST_MESSAGE_BODY_SQL})")

        if current_user_id is not None:
            where_parts.append("(i.rec_user_id = :current_user_id OR i.rec_user_id = 0)")
            params["current_user_id"] = current_user_id
        elif payload.get("recUserId") is not None and payload.get("recUserId") != "":
            where_parts.append("i.rec_user_id = :rec_user_id")
            params["rec_user_id"] = payload.get("recUserId")

        for key, column in (
            ("readFlag", "i.read_flag"),
            ("pushWay", "i.push_way"),
            ("pushStatus", "i.push_status"),
        ):
            if key in payload and payload.get(key) not in (None, ""):
                where_parts.append(f"{column} = :{key}")
                if key == "readFlag":
                    params[key] = 1 if _bool_value(payload.get(key)) else 0
                else:
                    params[key] = payload.get(key)

        if payload.get("type") not in (None, ""):
            where_parts.append("b.type = :type")
            params["type"] = payload.get("type")

        source_type = str(payload.get("sourceType") or "").strip().lower()
        if source_type == "system":
            where_parts.append("i.send_user_id IS NULL")
        elif source_type == "user":
            where_parts.append("i.send_user_id IS NOT NULL")

        keyword = str(payload.get("keyword") or "").strip()
        page_dict = payload.get("pageForm") or {}
        if not keyword and isinstance(page_dict, dict):
            keyword = str(page_dict.get("keyword") or "").strip()
        if keyword:
            where_parts.append(
                "("
                "i.msg_id LIKE :keyword"
                " OR b.title LIKE :keyword"
                " OR b.content LIKE :keyword"
                " OR IFNULL(b.template_code, '') LIKE :keyword"
                ")"
            )
            params["keyword"] = f"%{keyword}%"

        where_sql = " AND ".join(where_parts) if where_parts else "1=1"
        count_sql = f"""
            SELECT COUNT(1) AS total
            FROM {ITEM_TABLE} i
            INNER JOIN {BODY_TABLE} b ON i.msg_body_id = b.id
            WHERE {where_sql}
        """
        query_sql = f"""
            SELECT
              i.msg_id,
              i.msg_body_id,
              i.rec_user_id,
              i.send_user_id,
              i.push_status,
              i.push_way,
              i.read_flag,
              i.create_time AS item_create_time,
              b.title,
              b.content,
              b.type,
              b.level,
              b.url,
              b.template_code,
              {self._extend_select_sql()},
              b.create_time AS body_create_time
            FROM {ITEM_TABLE} i
            INNER JOIN {BODY_TABLE} b ON i.msg_body_id = b.id
            WHERE {where_sql}
            ORDER BY i.create_time DESC
            LIMIT :limit OFFSET :offset
        """
        curr_page = max(int(page_form.curr_page or 1), 1)
        page_size = max(int(page_form.page_size or 20), 1)
        offset = (curr_page - 1) * page_size
        params_with_page = dict(params)
        params_with_page["limit"] = page_size
        params_with_page["offset"] = offset

        async with self.engine.begin() as conn:
            total_row = (await conn.execute(text(count_sql), params)).mappings().first()
            rows = (await conn.execute(text(query_sql), params_with_page)).mappings().all()

        total = int((total_row or {}).get("total") or 0)
        contents = [self._to_api_row(dict(row)) for row in rows]
        return java_page(contents, total, page_form)

    async def count_unread(self, user_id: int) -> int:
        sql = f"""
            SELECT COUNT(1) AS total
            FROM {ITEM_TABLE} i
            WHERE (i.rec_user_id = :user_id OR i.rec_user_id = 0)
              AND (i.read_flag IS NULL OR i.read_flag = 0)
              AND i.msg_body_id NOT IN ({EXCLUDE_TEST_MESSAGE_BODY_SQL})
        """
        async with self.engine.begin() as conn:
            row = (await conn.execute(text(sql), {"user_id": user_id})).mappings().first()
        return int((row or {}).get("total") or 0)

    async def update_read_flag(self, ids: List[str], read_flag: bool, user_id: int) -> None:
        if not ids:
            return
        placeholders = ", ".join(f":id_{index}" for index, _ in enumerate(ids))
        params = {f"id_{index}": str(item) for index, item in enumerate(ids)}
        params["read_flag"] = 1 if read_flag else 0
        params["update_time"] = now_text()
        params["user_id"] = user_id
        sql = f"""
            UPDATE {ITEM_TABLE}
            SET read_flag = :read_flag, update_time = :update_time
            WHERE msg_id IN ({placeholders})
              AND rec_user_id = :user_id
        """
        async with self.engine.begin() as conn:
            await conn.execute(text(sql), params)

    async def mark_all_read(self, user_id: int) -> None:
        sql = f"""
            UPDATE {ITEM_TABLE}
            SET read_flag = 1, update_time = :update_time
            WHERE rec_user_id = :user_id
              AND (read_flag IS NULL OR read_flag = 0)
        """
        async with self.engine.begin() as conn:
            await conn.execute(text(sql), {"user_id": user_id, "update_time": now_text()})

    async def delete_by_ids(self, ids: List[str], user_id: int) -> None:
        if not ids:
            return
        placeholders = ", ".join(f":id_{index}" for index, _ in enumerate(ids))
        params = {f"id_{index}": str(item) for index, item in enumerate(ids)}
        params["user_id"] = user_id
        sql = (
            f"DELETE FROM {ITEM_TABLE} WHERE msg_id IN ({placeholders}) "
            "AND rec_user_id = :user_id"
        )
        async with self.engine.begin() as conn:
            await conn.execute(text(sql), params)

    def _to_api_row(self, row: Dict[str, Any]) -> Dict[str, Any]:
        send_user_id = row.get("send_user_id")
        extend = _parse_extend(row.get("extend_json"))
        create_time = row.get("item_create_time") or row.get("body_create_time")
        return {
            "msgId": row.get("msg_id"),
            "msgBodyId": row.get("msg_body_id"),
            "recUserId": row.get("rec_user_id"),
            "sendUserId": send_user_id,
            "sysMsg": send_user_id is None,
            "pushStatus": row.get("push_status"),
            "pushWay": row.get("push_way"),
            "readFlag": _bool_value(row.get("read_flag")),
            "title": row.get("title"),
            "content": row.get("content"),
            "type": row.get("type"),
            "level": row.get("level"),
            "url": row.get("url"),
            "templateCode": row.get("template_code"),
            "createTime": create_time,
            "extend": extend,
        }

    async def _load_columns(self, conn: Any) -> None:
        self._body_columns = set(
            await conn.run_sync(lambda sync_conn: [col["name"] for col in inspect(sync_conn).get_columns(BODY_TABLE)])
        )
        self._item_columns = set(
            await conn.run_sync(lambda sync_conn: [col["name"] for col in inspect(sync_conn).get_columns(ITEM_TABLE)])
        )
        if "extend_data" in self._body_columns:
            self._extend_column = "extend_data"
        elif "extend" in self._body_columns:
            self._extend_column = "extend"
        else:
            self._extend_column = None

    def _extend_select_sql(self) -> str:
        if self._extend_column:
            return f"b.{self._extend_column} AS extend_json"
        return "NULL AS extend_json"
