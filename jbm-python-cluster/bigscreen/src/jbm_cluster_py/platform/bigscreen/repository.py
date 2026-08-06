from __future__ import annotations

import json
import uuid
from collections.abc import Mapping
from pathlib import Path
from typing import Any

from jbm_cluster_py.common.masterdata import PageForm, java_page, now_iso
from jbm_cluster_py.integrations.database import configured_database_url, require_tables
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine


class BigscreenRepository:
    def __init__(self, database_config: Mapping[str, Any]) -> None:
        url = (
            configured_database_url(database_config)
            or "sqlite+aiosqlite:///./data/jbm-python-cluster.db"
        )
        self._sqlite = url.startswith("sqlite+aiosqlite:///")
        if self._sqlite:
            db_path = url.replace("sqlite+aiosqlite:///", "", 1)
            if db_path and not db_path.startswith(":"):
                Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self.engine: AsyncEngine = create_async_engine(url, pool_pre_ping=True)

    async def start(self) -> None:
        if not self._sqlite:
            await require_tables(self.engine, ("bigscreen_view",))
            return
        async with self.engine.begin() as conn:
            await conn.execute(
                text("""CREATE TABLE IF NOT EXISTS bigscreen_view (
                id VARCHAR(64) PRIMARY KEY, code VARCHAR(128), app_id VARCHAR(64), parent_id VARCHAR(64),
                view_name VARCHAR(255), view_url VARCHAR(255), static_params TEXT, resource_path VARCHAR(1024),
                preview_picture VARCHAR(1024), version VARCHAR(64), config_data TEXT,
                create_time VARCHAR(64), update_time VARCHAR(64), extend_data TEXT
            )""")
            )

    async def stop(self) -> None:
        await self.engine.dispose()

    async def get(self, view_id: str) -> dict[str, Any] | None:
        async with self.engine.connect() as conn:
            row = (
                (
                    await conn.execute(
                        text("SELECT * FROM bigscreen_view WHERE id=:id"), {"id": view_id}
                    )
                )
                .mappings()
                .first()
            )
        return self._from_db(dict(row)) if row else None

    async def save(self, body: Mapping[str, Any]) -> dict[str, Any]:
        current = await self.get(str(body.get("id") or "")) if body.get("id") else None
        data = {
            **(current or {}),
            **{key: value for key, value in body.items() if value is not None},
        }
        data.setdefault("id", uuid.uuid4().hex)
        data.setdefault("createTime", now_iso())
        data["updateTime"] = now_iso()
        row = self._to_db(data)
        async with self.engine.begin() as conn:
            await conn.execute(text("DELETE FROM bigscreen_view WHERE id=:id"), row)
            await conn.execute(
                text(
                    f"INSERT INTO bigscreen_view ({', '.join(row)}) VALUES ({', '.join(':' + c for c in row)})"
                ),
                row,
            )
        return self._from_db(row)

    async def page(self, body: Mapping[str, Any], all_rows: bool = False) -> dict[str, Any]:
        query = (
            body.get("bigscreenView") if isinstance(body.get("bigscreenView"), Mapping) else body
        )
        page = (
            PageForm(currPage=1, pageSize=10000)
            if all_rows
            else PageForm(**(body.get("pageForm") or {}))
        )
        mapping = {
            "id": "id",
            "code": "code",
            "appId": "app_id",
            "parentId": "parent_id",
            "viewName": "view_name",
            "viewUrl": "view_url",
            "version": "version",
        }
        where, params = [], {}
        for key, column in mapping.items():
            value = query.get(key)
            if value in (None, ""):
                continue
            params[key] = f"%{value}%"
            where.append(f"{column} LIKE :{key}")
        clause = " WHERE " + " AND ".join(where) if where else ""
        curr, size = max(page.curr_page, 1), max(page.page_size, 1)
        async with self.engine.connect() as conn:
            total = (
                await conn.execute(text(f"SELECT COUNT(*) FROM bigscreen_view{clause}"), params)
            ).scalar()
            rows = (
                (
                    await conn.execute(
                        text(
                            f"SELECT * FROM bigscreen_view{clause} ORDER BY update_time DESC LIMIT :limit OFFSET :offset"
                        ),
                        {**params, "limit": size, "offset": (curr - 1) * size},
                    )
                )
                .mappings()
                .all()
            )
        return java_page([self._from_db(dict(row)) for row in rows], int(total or 0), page)

    async def children(self, parent_id: str) -> int:
        async with self.engine.connect() as conn:
            value = (
                await conn.execute(
                    text("SELECT COUNT(*) FROM bigscreen_view WHERE parent_id=:id"),
                    {"id": parent_id},
                )
            ).scalar()
        return int(value or 0)

    async def delete(self, view_id: str) -> bool:
        async with self.engine.begin() as conn:
            await conn.execute(text("DELETE FROM bigscreen_view WHERE id=:id"), {"id": view_id})
        return True

    @staticmethod
    def _to_db(row: Mapping[str, Any]) -> dict[str, Any]:
        return {
            "id": str(row.get("id")),
            "code": row.get("code"),
            "app_id": row.get("appId"),
            "parent_id": row.get("parentId"),
            "view_name": row.get("viewName"),
            "view_url": row.get("viewUrl"),
            "static_params": row.get("staticParams"),
            "resource_path": row.get("resourcePath"),
            "preview_picture": row.get("previewPicture"),
            "version": row.get("version"),
            "config_data": row.get("configData")
            if isinstance(row.get("configData"), str)
            else json.dumps(row.get("configData"), ensure_ascii=False)
            if row.get("configData") is not None
            else None,
            "create_time": row.get("createTime"),
            "update_time": row.get("updateTime"),
            "extend_data": row.get("extendData")
            if isinstance(row.get("extendData"), str)
            else json.dumps(row.get("extendData"), ensure_ascii=False)
            if row.get("extendData") is not None
            else None,
        }

    @staticmethod
    def _from_db(row: Mapping[str, Any]) -> dict[str, Any]:
        mapping = {
            "app_id": "appId",
            "parent_id": "parentId",
            "view_name": "viewName",
            "view_url": "viewUrl",
            "static_params": "staticParams",
            "resource_path": "resourcePath",
            "preview_picture": "previewPicture",
            "config_data": "configData",
            "create_time": "createTime",
            "update_time": "updateTime",
            "extend_data": "extendData",
        }
        return {mapping.get(key, key): value for key, value in row.items()}
