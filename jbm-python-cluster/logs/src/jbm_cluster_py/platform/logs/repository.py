from __future__ import annotations

import json
import uuid
from collections.abc import Mapping
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any

from jbm_cluster_py.common.masterdata import PageForm, java_page
from jbm_cluster_py.integrations.database import configured_database_url
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine


def now_iso() -> str:
    return datetime.now(UTC).isoformat()


def _json(value: Any) -> str | None:
    return (
        json.dumps(value, ensure_ascii=False, separators=(",", ":")) if value is not None else None
    )


def _loads(value: Any, default: Any = None) -> Any:
    if value in (None, ""):
        return default
    try:
        return json.loads(str(value))
    except (TypeError, ValueError):
        return value


class LogsRepository:
    def __init__(self, database_config: Mapping[str, Any]) -> None:
        url = (
            configured_database_url(database_config)
            or "sqlite+aiosqlite:///./data/jbm-python-cluster.db"
        )
        if url.startswith("sqlite+aiosqlite:///"):
            db_path = url.replace("sqlite+aiosqlite:///", "", 1)
            if db_path and not db_path.startswith(":"):
                Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self.engine: AsyncEngine = create_async_engine(url, pool_pre_ping=True)

    async def start(self) -> None:
        ddl = (
            """CREATE TABLE IF NOT EXISTS gateway_logs (
                access_id VARCHAR(64) PRIMARY KEY, loglevel INTEGER, path VARCHAR(1024), api_path VARCHAR(1024),
                request_user_id VARCHAR(64), request_real_name VARCHAR(255), api_name VARCHAR(255),
                operation_type VARCHAR(64), app_id VARCHAR(64), key_id VARCHAR(64), app_key VARCHAR(128),
                api_id VARCHAR(64), app_name VARCHAR(255), method VARCHAR(16), ip VARCHAR(64),
                http_status INTEGER, request_time VARCHAR(64), response_time VARCHAR(64), response_body TEXT,
                use_time BIGINT, params TEXT, headers TEXT, user_agent TEXT, region VARCHAR(255),
                authentication TEXT, service_id VARCHAR(255), error TEXT
            )""",
            """CREATE TABLE IF NOT EXISTS gateway_log_filter_rule (
                rule_id VARCHAR(64) PRIMARY KEY, rule_name VARCHAR(255) NOT NULL, enabled INTEGER NOT NULL,
                builtin INTEGER NOT NULL, path_pattern VARCHAR(1024), method VARCHAR(16), service_id VARCHAR(255),
                status_code VARCHAR(32), remark VARCHAR(1024), hit_count BIGINT NOT NULL,
                last_hit_time VARCHAR(64), create_time VARCHAR(64), update_time VARCHAR(64)
            )""",
            """CREATE TABLE IF NOT EXISTS business_log (
                log_id VARCHAR(64) PRIMARY KEY, module VARCHAR(255), operation VARCHAR(255), user_id VARCHAR(64),
                username VARCHAR(255), status VARCHAR(32), request_ip VARCHAR(64), trace_id VARCHAR(128),
                remark TEXT, business_type VARCHAR(128), business_id VARCHAR(255), source VARCHAR(255),
                expire_days INTEGER, expire_date VARCHAR(64), total_lines INTEGER NOT NULL,
                create_time VARCHAR(64), update_time VARCHAR(64)
            )""",
            """CREATE TABLE IF NOT EXISTS business_log_line (
                log_id VARCHAR(64) NOT NULL, line_number INTEGER NOT NULL, content TEXT NOT NULL,
                is_append INTEGER NOT NULL, create_time VARCHAR(64), trace_id VARCHAR(128),
                business_type VARCHAR(128), business_id VARCHAR(255), source VARCHAR(255),
                stage_code VARCHAR(128), stage_name VARCHAR(255), stage_index INTEGER,
                stage_progress INTEGER, stage_status VARCHAR(32), stage_count INTEGER,
                overall_progress INTEGER, stage_event INTEGER NOT NULL,
                PRIMARY KEY (log_id, line_number)
            )""",
            """CREATE TABLE IF NOT EXISTS business_log_stage (
                log_id VARCHAR(64) PRIMARY KEY, snapshot_json TEXT NOT NULL, version BIGINT NOT NULL,
                update_time VARCHAR(64)
            )""",
        )
        async with self.engine.begin() as conn:
            for statement in ddl:
                await conn.execute(text(statement))
            for rule in self._builtin_rules():
                exists = (
                    await conn.execute(
                        text("SELECT rule_id FROM gateway_log_filter_rule WHERE rule_id=:rule_id"),
                        rule,
                    )
                ).first()
                if not exists:
                    await conn.execute(
                        text("""INSERT INTO gateway_log_filter_rule
                        (rule_id,rule_name,enabled,builtin,path_pattern,method,service_id,status_code,remark,
                         hit_count,last_hit_time,create_time,update_time)
                        VALUES (:rule_id,:rule_name,1,1,:path_pattern,NULL,NULL,NULL,:remark,0,NULL,:now,:now)"""),
                        {**rule, "now": now_iso()},
                    )

    async def stop(self) -> None:
        await self.engine.dispose()

    @staticmethod
    def _builtin_rules() -> list[dict[str, str]]:
        return [
            {
                "rule_id": "builtin-logs",
                "rule_name": "日志服务自访问",
                "path_pattern": "/logs/**",
                "remark": "防止日志查询递归采集",
            },
            {
                "rule_id": "builtin-health",
                "rule_name": "健康检查",
                "path_pattern": "/actuator/**",
                "remark": "忽略健康探针",
            },
            {
                "rule_id": "builtin-docs",
                "rule_name": "接口文档",
                "path_pattern": "/docs*",
                "remark": "忽略接口文档",
            },
            {
                "rule_id": "builtin-openapi",
                "rule_name": "OpenAPI",
                "path_pattern": "/openapi.json",
                "remark": "忽略 OpenAPI 拉取",
            },
        ]

    async def save_gateway_log(self, row: Mapping[str, Any]) -> dict[str, Any]:
        data = dict(row)
        data.setdefault("accessId", uuid.uuid4().hex)
        data.setdefault("requestTime", now_iso())
        columns = {
            "access_id": data.get("accessId"),
            "loglevel": data.get("loglevel", 1),
            "path": data.get("path"),
            "api_path": data.get("apiPath"),
            "request_user_id": data.get("requestUserId"),
            "request_real_name": data.get("requestRealName"),
            "api_name": data.get("apiName"),
            "operation_type": data.get("operationType"),
            "app_id": data.get("appId"),
            "key_id": data.get("keyId"),
            "app_key": data.get("appKey"),
            "api_id": data.get("apiId"),
            "app_name": data.get("appName"),
            "method": data.get("method") or data.get("requestMethod"),
            "ip": data.get("ip") or data.get("requestIp"),
            "http_status": data.get("httpStatus") or data.get("status"),
            "request_time": data.get("requestTime"),
            "response_time": data.get("responseTime"),
            "response_body": data.get("responseBody"),
            "use_time": data.get("useTime") or data.get("spendTime") or data.get("costTime"),
            "params": _json(data.get("params"))
            if not isinstance(data.get("params"), str)
            else data.get("params"),
            "headers": _json(data.get("headers"))
            if not isinstance(data.get("headers"), str)
            else data.get("headers"),
            "user_agent": data.get("userAgent"),
            "region": data.get("region"),
            "authentication": _json(data.get("authentication"))
            if not isinstance(data.get("authentication"), str)
            else data.get("authentication"),
            "service_id": data.get("serviceId"),
            "error": data.get("error"),
        }
        async with self.engine.begin() as conn:
            await conn.execute(text("DELETE FROM gateway_logs WHERE access_id=:access_id"), columns)
            await conn.execute(
                text(
                    f"INSERT INTO gateway_logs ({', '.join(columns)}) VALUES ({', '.join(':' + c for c in columns)})"
                ),
                columns,
            )
        return self._gateway_from_db(columns)

    async def gateway_log(self, access_id: str) -> dict[str, Any] | None:
        async with self.engine.connect() as conn:
            row = (
                (
                    await conn.execute(
                        text("SELECT * FROM gateway_logs WHERE access_id=:id"), {"id": access_id}
                    )
                )
                .mappings()
                .first()
            )
        return self._gateway_from_db(dict(row)) if row else None

    async def page_gateway_logs(
        self, body: Mapping[str, Any], operation_only: bool = False
    ) -> dict[str, Any]:
        query = body.get("gatewayLogs") if isinstance(body.get("gatewayLogs"), Mapping) else body
        page = PageForm(**(body.get("pageForm") or {}))
        field_map = {
            "accessId": ("access_id", "="),
            "serviceId": ("service_id", "="),
            "apiId": ("api_id", "="),
            "path": ("path", "LIKE"),
            "requestRealName": ("request_real_name", "LIKE"),
            "apiName": ("api_name", "LIKE"),
            "appName": ("app_name", "LIKE"),
            "region": ("region", "LIKE"),
            "method": ("method", "="),
            "ip": ("ip", "LIKE"),
            "operationType": ("operation_type", "="),
            "appKey": ("app_key", "="),
            "httpStatus": ("http_status", "="),
            "status": ("http_status", "="),
        }
        where, params = self._filters(query, field_map)
        if query.get("keyword"):
            params["keyword"] = f"%{query['keyword']}%"
            where.append("(path LIKE :keyword OR service_id LIKE :keyword OR error LIKE :keyword)")
        if body.get("beginTime"):
            params["begin"] = body["beginTime"]
            where.append("request_time >= :begin")
        if body.get("endTime"):
            params["end"] = body["endTime"]
            where.append("request_time <= :end")
        if operation_only:
            where.append("request_user_id IS NOT NULL")
        return await self._page(
            "gateway_logs", where, params, page, self._gateway_from_db, "request_time"
        )

    async def cluster_access(self) -> dict[str, Any]:
        today = datetime.now(UTC).date().isoformat()
        async with self.engine.connect() as conn:
            total = (await conn.execute(text("SELECT COUNT(*) FROM gateway_logs"))).scalar()
            today_count = (
                await conn.execute(
                    text("SELECT COUNT(*) FROM gateway_logs WHERE request_time LIKE :day"),
                    {"day": today + "%"},
                )
            ).scalar()
        return {
            "time": now_iso(),
            "total": int(total or 0),
            "today": int(today_count or 0),
            "maxQps": 0,
        }

    async def list_rules(self) -> list[dict[str, Any]]:
        async with self.engine.connect() as conn:
            rows = (
                (
                    await conn.execute(
                        text(
                            "SELECT * FROM gateway_log_filter_rule ORDER BY builtin DESC, create_time"
                        )
                    )
                )
                .mappings()
                .all()
            )
        return [self._rule_from_db(dict(row)) for row in rows]

    async def save_rule(
        self, body: Mapping[str, Any], rule_id: str | None = None
    ) -> dict[str, Any]:
        current = await self.rule(rule_id) if rule_id else None
        if current and current.get("builtin"):
            raise ValueError("系统内置规则不可修改")
        now = now_iso()
        row = {
            "rule_id": rule_id or uuid.uuid4().hex,
            "rule_name": str(body.get("ruleName") or "").strip(),
            "enabled": 0 if body.get("enabled") is False else 1,
            "builtin": 0,
            "path_pattern": str(body.get("pathPattern") or "").strip() or None,
            "method": str(body.get("method") or "").strip().upper() or None,
            "service_id": str(body.get("serviceId") or "").strip() or None,
            "status_code": str(body.get("statusCode") or "").strip() or None,
            "remark": str(body.get("remark") or "").strip() or None,
            "hit_count": int((current or {}).get("hitCount") or 0),
            "last_hit_time": (current or {}).get("lastHitTime"),
            "create_time": (current or {}).get("createTime") or now,
            "update_time": now,
        }
        if not row["rule_name"]:
            raise ValueError("规则名称不能为空")
        if not any(row[key] for key in ("path_pattern", "method", "service_id", "status_code")):
            raise ValueError("至少需要一个匹配条件")
        async with self.engine.begin() as conn:
            await conn.execute(
                text("DELETE FROM gateway_log_filter_rule WHERE rule_id=:rule_id"), row
            )
            await conn.execute(
                text(
                    f"INSERT INTO gateway_log_filter_rule ({', '.join(row)}) VALUES ({', '.join(':' + c for c in row)})"
                ),
                row,
            )
        return self._rule_from_db(row)

    async def rule(self, rule_id: str | None) -> dict[str, Any] | None:
        if not rule_id:
            return None
        async with self.engine.connect() as conn:
            row = (
                (
                    await conn.execute(
                        text("SELECT * FROM gateway_log_filter_rule WHERE rule_id=:id"),
                        {"id": rule_id},
                    )
                )
                .mappings()
                .first()
            )
        return self._rule_from_db(dict(row)) if row else None

    async def delete_rule(self, rule_id: str) -> bool:
        current = await self.rule(rule_id)
        if not current:
            return True
        if current.get("builtin"):
            raise ValueError("系统内置规则不可删除")
        async with self.engine.begin() as conn:
            await conn.execute(
                text("DELETE FROM gateway_log_filter_rule WHERE rule_id=:id"), {"id": rule_id}
            )
        return True

    async def mark_rule_hits(self, rule_ids: list[str]) -> None:
        if not rule_ids:
            return
        async with self.engine.begin() as conn:
            for rule_id in rule_ids:
                await conn.execute(
                    text(
                        "UPDATE gateway_log_filter_rule SET hit_count=hit_count+1,last_hit_time=:now,update_time=:now WHERE rule_id=:id"
                    ),
                    {"id": rule_id, "now": now_iso()},
                )

    async def create_business_log(self, body: Mapping[str, Any]) -> dict[str, Any]:
        log_id = str(body.get("logId") or uuid.uuid4().hex)
        created = now_iso()
        expire_days = max(int(body.get("expireDays") or 30), 1)
        summary = {
            "log_id": log_id,
            "module": body.get("module"),
            "operation": body.get("operation"),
            "user_id": body.get("userId"),
            "username": body.get("username"),
            "status": "ACTIVE",
            "request_ip": body.get("requestIp"),
            "trace_id": body.get("traceId"),
            "remark": body.get("remark"),
            "business_type": body.get("businessType"),
            "business_id": body.get("businessId"),
            "source": body.get("source"),
            "expire_days": expire_days,
            "expire_date": (datetime.now(UTC) + timedelta(days=expire_days)).isoformat(),
            "total_lines": 0,
            "create_time": created,
            "update_time": created,
        }
        async with self.engine.begin() as conn:
            await conn.execute(
                text(
                    f"INSERT INTO business_log ({', '.join(summary)}) VALUES ({', '.join(':' + c for c in summary)})"
                ),
                summary,
            )
        if body.get("content"):
            await self.append_business_log(log_id, str(body["content"]), body, False)
        result = self._business_from_db(summary)
        result["logId"] = log_id
        return result

    async def append_business_log(
        self, log_id: str, content: str, metadata: Mapping[str, Any], is_append: bool = True
    ) -> list[dict[str, Any]]:
        summary = await self.business_summary(log_id)
        if not summary:
            raise ValueError("业务日志不存在")
        lines = content.splitlines() or [content]
        if metadata.get("autoTimestamp"):
            stamp = datetime.now(UTC).strftime("%Y-%m-%d %H:%M:%S")
            lines = [f"[{stamp}] {line}" for line in lines]
        start = int(summary.get("totalLines") or 0) + 1
        rows = []
        for offset, line in enumerate(lines):
            rows.append(
                {
                    "log_id": log_id,
                    "line_number": start + offset,
                    "content": line,
                    "is_append": 1 if is_append else 0,
                    "create_time": now_iso(),
                    "trace_id": metadata.get("traceId") or summary.get("traceId"),
                    "business_type": metadata.get("businessType") or summary.get("businessType"),
                    "business_id": metadata.get("businessId") or summary.get("businessId"),
                    "source": metadata.get("source") or summary.get("source"),
                    "stage_code": metadata.get("stageCode"),
                    "stage_name": metadata.get("stageName"),
                    "stage_index": metadata.get("stageIndex"),
                    "stage_progress": metadata.get("stageProgress"),
                    "stage_status": metadata.get("stageStatus"),
                    "stage_count": metadata.get("stageCount"),
                    "overall_progress": metadata.get("overallProgress"),
                    "stage_event": 1 if metadata.get("stageEvent") else 0,
                }
            )
        async with self.engine.begin() as conn:
            for row in rows:
                await conn.execute(
                    text(
                        f"INSERT INTO business_log_line ({', '.join(row)}) VALUES ({', '.join(':' + c for c in row)})"
                    ),
                    row,
                )
            await conn.execute(
                text(
                    "UPDATE business_log SET total_lines=:total,update_time=:now WHERE log_id=:id"
                ),
                {"total": start + len(rows) - 1, "now": now_iso(), "id": log_id},
            )
        return [self._line_from_db(row) for row in rows]

    async def business_summary(self, log_id: str) -> dict[str, Any] | None:
        async with self.engine.connect() as conn:
            row = (
                (
                    await conn.execute(
                        text("SELECT * FROM business_log WHERE log_id=:id"), {"id": log_id}
                    )
                )
                .mappings()
                .first()
            )
        return self._business_from_db(dict(row)) if row else None

    async def business_lines(
        self, log_id: str, start: int = 1, end: int = -1
    ) -> list[dict[str, Any]]:
        params: dict[str, Any] = {"id": log_id, "start": max(start, 1)}
        end_sql = ""
        if end >= 0:
            params["end"] = end
            end_sql = " AND line_number<=:end"
        async with self.engine.connect() as conn:
            rows = (
                (
                    await conn.execute(
                        text(
                            f"SELECT * FROM business_log_line WHERE log_id=:id AND line_number>=:start{end_sql} ORDER BY line_number"
                        ),
                        params,
                    )
                )
                .mappings()
                .all()
            )
        return [self._line_from_db(dict(row)) for row in rows]

    async def page_business_logs(self, body: Mapping[str, Any]) -> dict[str, Any]:
        query = (
            dict(body.get("businessLog") or {})
            if isinstance(body.get("businessLog"), Mapping)
            else dict(body)
        )
        page = PageForm(**(body.get("pageForm") or {}))
        field_map = {
            "logId": ("log_id", "LIKE"),
            "module": ("module", "LIKE"),
            "operation": ("operation", "LIKE"),
            "userId": ("user_id", "="),
            "status": ("status", "="),
            "traceId": ("trace_id", "LIKE"),
            "businessType": ("business_type", "LIKE"),
            "businessId": ("business_id", "LIKE"),
            "source": ("source", "LIKE"),
        }
        where, params = self._filters(query, field_map)
        if query.get("keyword"):
            params["keyword"] = f"%{query['keyword']}%"
            where.append(
                "(module LIKE :keyword OR operation LIKE :keyword OR business_id LIKE :keyword OR remark LIKE :keyword)"
            )
        return await self._page(
            "business_log", where, params, page, self._business_from_db, "update_time"
        )

    async def update_business_expiry(self, log_id: str, expire_days: int) -> bool:
        expires = (datetime.now(UTC) + timedelta(days=max(expire_days, 1))).isoformat()
        async with self.engine.begin() as conn:
            result = await conn.execute(
                text(
                    "UPDATE business_log SET expire_days=:days,expire_date=:expires,update_time=:now WHERE log_id=:id"
                ),
                {"days": expire_days, "expires": expires, "now": now_iso(), "id": log_id},
            )
        return bool(result.rowcount)

    async def delete_business_log(self, log_id: str) -> bool:
        async with self.engine.begin() as conn:
            await conn.execute(
                text("DELETE FROM business_log_line WHERE log_id=:id"), {"id": log_id}
            )
            await conn.execute(
                text("DELETE FROM business_log_stage WHERE log_id=:id"), {"id": log_id}
            )
            await conn.execute(text("DELETE FROM business_log WHERE log_id=:id"), {"id": log_id})
        return True

    async def business_id_log(self, business_type: str, business_id: str) -> str | None:
        async with self.engine.connect() as conn:
            value = (
                await conn.execute(
                    text(
                        "SELECT log_id FROM business_log WHERE business_type=:t AND business_id=:i ORDER BY create_time DESC LIMIT 1"
                    ),
                    {"t": business_type, "i": business_id},
                )
            ).scalar()
        return str(value) if value else None

    async def stage(self, log_id: str) -> dict[str, Any] | None:
        async with self.engine.connect() as conn:
            row = (
                await conn.execute(
                    text("SELECT snapshot_json,version FROM business_log_stage WHERE log_id=:id"),
                    {"id": log_id},
                )
            ).first()
        if not row:
            return None
        result = _loads(row[0], {})
        result["version"] = int(row[1] or 0)
        return result

    async def save_stage(self, snapshot: Mapping[str, Any]) -> dict[str, Any]:
        data = dict(snapshot)
        data["updateTime"] = now_iso()
        version = int(data.get("version") or 0) + 1
        data["version"] = version
        row = {
            "id": data["logId"],
            "snapshot": _json(data),
            "version": version,
            "now": data["updateTime"],
        }
        async with self.engine.begin() as conn:
            await conn.execute(text("DELETE FROM business_log_stage WHERE log_id=:id"), row)
            await conn.execute(
                text(
                    "INSERT INTO business_log_stage (log_id,snapshot_json,version,update_time) VALUES (:id,:snapshot,:version,:now)"
                ),
                row,
            )
        return data

    async def _page(
        self,
        table: str,
        where: list[str],
        params: dict[str, Any],
        page: PageForm,
        mapper: Any,
        order: str,
    ) -> dict[str, Any]:
        clause = " WHERE " + " AND ".join(where) if where else ""
        curr, size = max(page.curr_page, 1), max(page.page_size, 1)
        async with self.engine.connect() as conn:
            total = (
                await conn.execute(text(f"SELECT COUNT(*) FROM {table}{clause}"), params)
            ).scalar()
            rows = (
                (
                    await conn.execute(
                        text(
                            f"SELECT * FROM {table}{clause} ORDER BY {order} DESC LIMIT :limit OFFSET :offset"
                        ),
                        {**params, "limit": size, "offset": (curr - 1) * size},
                    )
                )
                .mappings()
                .all()
            )
        return java_page([mapper(dict(row)) for row in rows], int(total or 0), page)

    @staticmethod
    def _filters(
        query: Mapping[str, Any], field_map: Mapping[str, tuple[str, str]]
    ) -> tuple[list[str], dict[str, Any]]:
        where, params = [], {}
        for key, (column, operator) in field_map.items():
            value = query.get(key)
            if value in (None, ""):
                continue
            params[key] = f"%{value}%" if operator == "LIKE" else value
            where.append(f"{column} {operator} :{key}")
        return where, params

    @staticmethod
    def _gateway_from_db(row: Mapping[str, Any]) -> dict[str, Any]:
        mapping = {
            "access_id": "accessId",
            "api_path": "apiPath",
            "request_user_id": "requestUserId",
            "request_real_name": "requestRealName",
            "api_name": "apiName",
            "operation_type": "operationType",
            "app_id": "appId",
            "key_id": "keyId",
            "app_key": "appKey",
            "api_id": "apiId",
            "app_name": "appName",
            "http_status": "httpStatus",
            "request_time": "requestTime",
            "response_time": "responseTime",
            "response_body": "responseBody",
            "use_time": "useTime",
            "user_agent": "userAgent",
            "service_id": "serviceId",
        }
        return {mapping.get(key, key): value for key, value in row.items()}

    @staticmethod
    def _rule_from_db(row: Mapping[str, Any]) -> dict[str, Any]:
        mapping = {
            "rule_id": "ruleId",
            "rule_name": "ruleName",
            "path_pattern": "pathPattern",
            "service_id": "serviceId",
            "status_code": "statusCode",
            "hit_count": "hitCount",
            "last_hit_time": "lastHitTime",
            "create_time": "createTime",
            "update_time": "updateTime",
        }
        result = {mapping.get(key, key): value for key, value in row.items()}
        result["enabled"], result["builtin"] = (
            bool(result.get("enabled")),
            bool(result.get("builtin")),
        )
        return result

    @staticmethod
    def _business_from_db(row: Mapping[str, Any]) -> dict[str, Any]:
        mapping = {
            "log_id": "logId",
            "user_id": "userId",
            "request_ip": "requestIp",
            "trace_id": "traceId",
            "business_type": "businessType",
            "business_id": "businessId",
            "expire_days": "expireDays",
            "expire_date": "expireDate",
            "total_lines": "totalLines",
            "create_time": "createTime",
            "update_time": "updateTime",
        }
        return {mapping.get(key, key): value for key, value in row.items()}

    @staticmethod
    def _line_from_db(row: Mapping[str, Any]) -> dict[str, Any]:
        mapping = {
            "log_id": "logId",
            "line_number": "lineNumber",
            "is_append": "isAppend",
            "create_time": "createTime",
            "trace_id": "traceId",
            "business_type": "businessType",
            "business_id": "businessId",
            "stage_code": "stageCode",
            "stage_name": "stageName",
            "stage_index": "stageIndex",
            "stage_progress": "stageProgress",
            "stage_status": "stageStatus",
            "stage_count": "stageCount",
            "overall_progress": "overallProgress",
            "stage_event": "stageEvent",
        }
        result = {mapping.get(key, key): value for key, value in row.items()}
        result["isAppend"], result["stageEvent"] = (
            bool(result.get("isAppend")),
            bool(result.get("stageEvent")),
        )
        return result
