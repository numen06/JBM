from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, Iterable, List, Mapping, Optional

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from jbm_cluster_py.common.masterdata import PageForm, java_page, now_iso
from jbm_cluster_py.integrations.database import configured_database_url


DOC_TABLE = "base_doc"
GROUP_TABLE = "base_doc_group"
TOKEN_TABLE = "base_doc_token"
VERSION_TABLE = "base_doc_version"


def _clean(data: Mapping[str, Any]) -> Dict[str, Any]:
    return {key: value for key, value in data.items() if value is not None}


class DocRepository:
    def __init__(self, database_config: Mapping[str, Any]) -> None:
        database_url = configured_database_url(database_config) or "sqlite+aiosqlite:///./data/jbm-python-cluster.db"
        if database_url.startswith("sqlite+aiosqlite:///"):
            db_path = database_url.replace("sqlite+aiosqlite:///", "", 1)
            if db_path and not db_path.startswith(":"):
                Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self.engine: AsyncEngine = create_async_engine(database_url, pool_pre_ping=True)

    async def start(self) -> None:
        async with self.engine.begin() as conn:
            await conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {DOC_TABLE} (
                      doc_id VARCHAR(64) PRIMARY KEY,
                      doc_name VARCHAR(512),
                      size BIGINT,
                      doc_group_id VARCHAR(64),
                      doc_group VARCHAR(512),
                      doc_path VARCHAR(1024) UNIQUE,
                      state VARCHAR(64),
                      content_type VARCHAR(256),
                      effective_time BIGINT,
                      expiration_time VARCHAR(64),
                      version TEXT,
                      creator VARCHAR(64),
                      create_time VARCHAR(64),
                      update_time VARCHAR(64)
                    )
                    """
                )
            )
            await conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {GROUP_TABLE} (
                      group_id VARCHAR(64) PRIMARY KEY,
                      group_path VARCHAR(512) UNIQUE,
                      expiration_time VARCHAR(64),
                      auto_clear TINYINT,
                      max_quantity INTEGER,
                      token_key VARCHAR(64),
                      doc_group_name VARCHAR(256),
                      create_time VARCHAR(64),
                      update_time VARCHAR(64)
                    )
                    """
                )
            )
            await conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {TOKEN_TABLE} (
                      token_key VARCHAR(64) PRIMARY KEY,
                      expiration_time VARCHAR(64),
                      effective_time BIGINT,
                      effective_time_type INTEGER,
                      doc_group_id VARCHAR(64),
                      doc_id VARCHAR(64),
                      create_time VARCHAR(64),
                      update_time VARCHAR(64)
                    )
                    """
                )
            )
            await conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {VERSION_TABLE} (
                      id VARCHAR(96) PRIMARY KEY,
                      doc_id VARCHAR(64),
                      version_no INTEGER,
                      object_key VARCHAR(1024),
                      doc_name VARCHAR(512),
                      size BIGINT,
                      content_type VARCHAR(256),
                      modifier VARCHAR(64),
                      create_time VARCHAR(64)
                    )
                    """
                )
            )

    async def stop(self) -> None:
        await self.engine.dispose()

    async def save_doc(self, doc: Mapping[str, Any]) -> Dict[str, Any]:
        row = _clean(dict(doc))
        now = now_iso()
        row.setdefault("createTime", now)
        row["updateTime"] = now
        current = await self.get_doc_by_id(str(row["docId"]))
        if current:
            merged = {**current, **row}
            await self._update(DOC_TABLE, "doc_id", "docId", self._doc_to_db(merged))
            return merged
        await self._insert(DOC_TABLE, self._doc_to_db(row))
        return row

    async def get_doc_by_id(self, doc_id: str) -> Optional[Dict[str, Any]]:
        return await self._one(DOC_TABLE, "doc_id", doc_id, self._doc_from_db)

    async def get_doc_by_path(self, doc_path: str) -> Optional[Dict[str, Any]]:
        return await self._one(DOC_TABLE, "doc_path", doc_path, self._doc_from_db)

    async def page_docs(self, query: Mapping[str, Any], page_form: PageForm) -> Dict[str, Any]:
        return await self._page(
            DOC_TABLE,
            query,
            page_form,
            self._doc_from_db,
            {
                "docId": "doc_id",
                "docName": "doc_name",
                "docPath": "doc_path",
                "docGroup": "doc_group",
                "contentType": "content_type",
                "state": "state",
            },
        )

    async def list_docs(self, query: Mapping[str, Any], limit: int = 100) -> List[Dict[str, Any]]:
        result = await self.page_docs(query, PageForm(currPage=1, pageSize=limit))
        return list(result["contents"])

    async def existing_doc_paths(self) -> set[str]:
        async with self.engine.begin() as conn:
            rows = (await conn.execute(text(f"SELECT doc_path FROM {DOC_TABLE} WHERE doc_path IS NOT NULL"))).all()
        return {str(row[0]) for row in rows if row[0]}

    async def existing_doc_ids(self) -> set[str]:
        async with self.engine.begin() as conn:
            rows = (await conn.execute(text(f"SELECT doc_id FROM {DOC_TABLE} WHERE doc_id IS NOT NULL"))).all()
        return {str(row[0]) for row in rows if row[0]}

    async def insert_docs(self, docs: Iterable[Mapping[str, Any]]) -> int:
        rows = [self._doc_to_db(_clean(dict(doc))) for doc in docs]
        if not rows:
            return 0
        columns = list(rows[0].keys())
        sql = f"INSERT INTO {DOC_TABLE} ({', '.join(columns)}) VALUES ({', '.join(':' + column for column in columns)})"
        async with self.engine.begin() as conn:
            await conn.execute(text(sql), rows)
        return len(rows)

    async def delete_docs_by_ids(self, ids: Iterable[str]) -> bool:
        async with self.engine.begin() as conn:
            for doc_id in ids:
                await conn.execute(text(f"DELETE FROM {DOC_TABLE} WHERE doc_id=:value"), {"value": doc_id})
        return True

    async def delete_docs_by_paths(self, paths: Iterable[str]) -> bool:
        async with self.engine.begin() as conn:
            for doc_path in paths:
                await conn.execute(text(f"DELETE FROM {DOC_TABLE} WHERE doc_path=:value"), {"value": doc_path})
        return True

    async def save_group(self, group: Mapping[str, Any]) -> Dict[str, Any]:
        row = _clean(dict(group))
        now = now_iso()
        row.setdefault("createTime", now)
        row["updateTime"] = now
        current = await self.get_group_by_id(str(row["groupId"]))
        if current:
            merged = {**current, **row}
            await self._update(GROUP_TABLE, "group_id", "groupId", self._group_to_db(merged))
            return merged
        await self._insert(GROUP_TABLE, self._group_to_db(row))
        return row

    async def get_group_by_id(self, group_id: str) -> Optional[Dict[str, Any]]:
        return await self._one(GROUP_TABLE, "group_id", group_id, self._group_from_db)

    async def page_groups(self, query: Mapping[str, Any], page_form: PageForm) -> Dict[str, Any]:
        return await self._page(GROUP_TABLE, query, page_form, self._group_from_db, {"groupId": "group_id", "groupPath": "group_path"})

    async def delete_groups_by_ids(self, ids: Iterable[str]) -> bool:
        async with self.engine.begin() as conn:
            for group_id in ids:
                await conn.execute(text(f"DELETE FROM {GROUP_TABLE} WHERE group_id=:value"), {"value": group_id})
        return True

    async def save_token(self, token: Mapping[str, Any]) -> Dict[str, Any]:
        row = _clean(dict(token))
        now = now_iso()
        row.setdefault("createTime", now)
        row["updateTime"] = now
        current = await self.get_token(str(row["tokenKey"]))
        if current:
            merged = {**current, **row}
            await self._update(TOKEN_TABLE, "token_key", "tokenKey", self._token_to_db(merged))
            return merged
        await self._insert(TOKEN_TABLE, self._token_to_db(row))
        return row

    async def get_token(self, token_key: str) -> Optional[Dict[str, Any]]:
        return await self._one(TOKEN_TABLE, "token_key", token_key, self._token_from_db)

    async def page_tokens(self, query: Mapping[str, Any], page_form: PageForm) -> Dict[str, Any]:
        return await self._page(TOKEN_TABLE, query, page_form, self._token_from_db, {"tokenKey": "token_key", "docGroupId": "doc_group_id", "docId": "doc_id"})

    async def delete_tokens_by_ids(self, ids: Iterable[str]) -> bool:
        async with self.engine.begin() as conn:
            for token_key in ids:
                await conn.execute(text(f"DELETE FROM {TOKEN_TABLE} WHERE token_key=:value"), {"value": token_key})
        return True

    async def save_version(self, version: Mapping[str, Any]) -> Dict[str, Any]:
        await self._insert(VERSION_TABLE, self._version_to_db(version))
        return dict(version)

    async def latest_version_no(self, doc_id: str) -> int:
        async with self.engine.begin() as conn:
            value = (
                await conn.execute(
                    text(f"SELECT MAX(version_no) FROM {VERSION_TABLE} WHERE doc_id=:doc_id"),
                    {"doc_id": doc_id},
                )
            ).scalar()
        return int(value or 0)

    async def versions(self, doc_id: str, offset: int = 0, count: int = 10) -> List[Dict[str, Any]]:
        async with self.engine.begin() as conn:
            rows = (
                await conn.execute(
                    text(
                        f"SELECT * FROM {VERSION_TABLE} WHERE doc_id=:doc_id ORDER BY version_no DESC LIMIT :limit OFFSET :offset"
                    ),
                    {"doc_id": doc_id, "limit": count or 10, "offset": offset or 0},
                )
            ).mappings().all()
        return [self._version_from_db(dict(row)) for row in rows]

    async def version(self, doc_id: str, version_no: int) -> Optional[Dict[str, Any]]:
        async with self.engine.begin() as conn:
            row = (
                await conn.execute(
                    text(
                        f"SELECT * FROM {VERSION_TABLE} WHERE doc_id=:doc_id AND version_no=:version_no"
                    ),
                    {"doc_id": doc_id, "version_no": version_no},
                )
            ).mappings().first()
        return self._version_from_db(dict(row)) if row else None

    async def _one(self, table: str, column: str, value: str, mapper: Any) -> Optional[Dict[str, Any]]:
        async with self.engine.begin() as conn:
            row = (
                await conn.execute(text(f"SELECT * FROM {table} WHERE {column}=:value"), {"value": value})
            ).mappings().first()
        return mapper(dict(row)) if row else None

    async def _page(
        self,
        table: str,
        query: Mapping[str, Any],
        page_form: PageForm,
        mapper: Any,
        field_map: Mapping[str, str],
    ) -> Dict[str, Any]:
        filters = []
        params: Dict[str, Any] = {}
        for field, column in field_map.items():
            value = query.get(field)
            if value not in (None, ""):
                params[field] = "%%%s%%" % value
                filters.append(f"{column} LIKE :{field}")
        where = (" WHERE " + " AND ".join(filters)) if filters else ""
        curr_page = max(int(page_form.curr_page or 1), 1)
        page_size = max(int(page_form.page_size or 10), 1)
        params.update({"limit": page_size, "offset": (curr_page - 1) * page_size})
        async with self.engine.begin() as conn:
            total = (
                await conn.execute(text(f"SELECT COUNT(*) FROM {table}{where}"), params)
            ).scalar()
            rows = (
                await conn.execute(
                    text(f"SELECT * FROM {table}{where} ORDER BY update_time DESC LIMIT :limit OFFSET :offset"),
                    params,
                )
            ).mappings().all()
        return java_page([mapper(dict(row)) for row in rows], int(total or 0), page_form)

    async def _insert(self, table: str, row: Mapping[str, Any]) -> None:
        columns = list(row.keys())
        sql = f"INSERT INTO {table} ({', '.join(columns)}) VALUES ({', '.join(':' + column for column in columns)})"
        async with self.engine.begin() as conn:
            await conn.execute(text(sql), dict(row))

    async def _update(self, table: str, key_column: str, key_field: str, row: Mapping[str, Any]) -> None:
        columns = [column for column in row.keys() if column != key_column]
        sql = f"UPDATE {table} SET {', '.join(column + '=:' + column for column in columns)} WHERE {key_column}=:{key_column}"
        async with self.engine.begin() as conn:
            await conn.execute(text(sql), dict(row))

    def _doc_to_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "doc_id": row.get("docId"),
            "doc_name": row.get("docName"),
            "size": row.get("size"),
            "doc_group_id": row.get("docGroupId"),
            "doc_group": row.get("docGroup"),
            "doc_path": row.get("docPath"),
            "state": row.get("state"),
            "content_type": row.get("contentType"),
            "effective_time": row.get("effectiveTime"),
            "expiration_time": row.get("expirationTime"),
            "version": json.dumps(row.get("version") or {"major": 1}, ensure_ascii=False),
            "creator": row.get("creator"),
            "create_time": row.get("createTime"),
            "update_time": row.get("updateTime"),
        }

    def _doc_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        version = row.get("version")
        try:
            version = json.loads(version) if version else None
        except Exception:
            pass
        return {
            "docId": row.get("doc_id"),
            "docName": row.get("doc_name"),
            "size": row.get("size"),
            "docGroupId": row.get("doc_group_id"),
            "docGroup": row.get("doc_group"),
            "docPath": row.get("doc_path"),
            "state": row.get("state"),
            "contentType": row.get("content_type"),
            "effectiveTime": row.get("effective_time"),
            "expirationTime": row.get("expiration_time"),
            "version": version,
            "creator": row.get("creator"),
            "createTime": row.get("create_time"),
            "updateTime": row.get("update_time"),
        }

    def _group_to_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "group_id": row.get("groupId"),
            "group_path": row.get("groupPath"),
            "expiration_time": row.get("expirationTime"),
            "auto_clear": 1 if row.get("autoClear") else 0 if row.get("autoClear") is not None else None,
            "max_quantity": row.get("maxQuantity"),
            "token_key": row.get("tokenKey"),
            "doc_group_name": row.get("docGroupName"),
            "create_time": row.get("createTime"),
            "update_time": row.get("updateTime"),
        }

    def _group_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "groupId": row.get("group_id"),
            "groupPath": row.get("group_path"),
            "expirationTime": row.get("expiration_time"),
            "autoClear": bool(row.get("auto_clear")) if row.get("auto_clear") is not None else None,
            "maxQuantity": row.get("max_quantity"),
            "tokenKey": row.get("token_key"),
            "docGroupName": row.get("doc_group_name"),
            "createTime": row.get("create_time"),
            "updateTime": row.get("update_time"),
        }

    def _token_to_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "token_key": row.get("tokenKey"),
            "expiration_time": row.get("expirationTime"),
            "effective_time": row.get("effectiveTime"),
            "effective_time_type": row.get("effectiveTimeType"),
            "doc_group_id": row.get("docGroupId"),
            "doc_id": row.get("docId"),
            "create_time": row.get("createTime"),
            "update_time": row.get("updateTime"),
        }

    def _token_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "tokenKey": row.get("token_key"),
            "expirationTime": row.get("expiration_time"),
            "effectiveTime": row.get("effective_time"),
            "effectiveTimeType": row.get("effective_time_type"),
            "docGroupId": row.get("doc_group_id"),
            "docId": row.get("doc_id"),
            "createTime": row.get("create_time"),
            "updateTime": row.get("update_time"),
        }

    def _version_to_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "id": row.get("id"),
            "doc_id": row.get("docId"),
            "version_no": row.get("version"),
            "object_key": row.get("objectKey"),
            "doc_name": row.get("docName"),
            "size": row.get("size"),
            "content_type": row.get("contentType"),
            "modifier": row.get("modifier"),
            "create_time": row.get("createTime"),
        }

    def _version_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "id": row.get("id"),
            "docId": row.get("doc_id"),
            "version": row.get("version_no"),
            "objectKey": row.get("object_key"),
            "docName": row.get("doc_name"),
            "size": row.get("size"),
            "contentType": row.get("content_type"),
            "modifier": row.get("modifier"),
            "createTime": row.get("create_time"),
        }
