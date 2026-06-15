from __future__ import annotations

import asyncio
import json
import logging
import uuid
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Iterable, List, Mapping, Optional

import httpx
from pydantic import Field
from sqlalchemy import inspect, text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from jbm_cluster_py.common.masterdata import LooseModel, PageForm, java_page, page_form_from_body
from jbm_cluster_py.integrations.database import configured_database_url
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
from jbm_cluster_py.integrations.redis import RedisClient

logger = logging.getLogger(__name__)

WEBHOOK_EVENT_CONFIG_TABLE = "webhook_event_config"
WEBHOOK_TASK_TABLE = "webhook_task"
MAX_RETRY = 3


def now_text() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def object_id() -> str:
    return uuid.uuid4().hex


def clean_dict(value: Mapping[str, Any]) -> Dict[str, Any]:
    return {key: item for key, item in dict(value).items() if item is not None}


def as_bool(value: Any, default: bool = False) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return value != 0
    if isinstance(value, str):
        return value.strip().lower() in {"1", "true", "yes", "y", "on"}
    return bool(value)


class JbmClusterBusinessEventBean(LooseModel):
    event_code: Optional[str] = Field(default=None, alias="eventCode")
    event_name: Optional[str] = Field(default=None, alias="eventName")
    event_group: Optional[str] = Field(default=None, alias="eventGroup")
    event_body: Optional[str] = Field(default=None, alias="eventBody")
    url: Optional[str] = None
    service_name: Optional[str] = Field(default=None, alias="serviceName")
    cron: Optional[str] = None
    global_: Optional[bool] = Field(default=None, alias="global")
    enable: Optional[bool] = None
    method_type: Optional[str] = Field(default=None, alias="methodType")
    content_type: Optional[str] = Field(default=None, alias="contentType")
    description: Optional[str] = None


class JbmClusterBusinessEventResource(LooseModel):
    service_id: Optional[str] = Field(default=None, alias="serviceId")
    jbm_cluster_business_event_beans: List[JbmClusterBusinessEventBean] = Field(
        default_factory=list,
        alias="jbmClusterBusinessEventBeans",
    )


class WebhookEventConfig(LooseModel):
    event_id: Optional[str] = Field(default=None, alias="eventId")
    business_event_code: Optional[str] = Field(default=None, alias="businessEventCode")
    event_name: Optional[str] = Field(default=None, alias="eventName")
    event_group: Optional[str] = Field(default=None, alias="eventGroup")
    event_body: Optional[str] = Field(default=None, alias="eventBody")
    internal: Optional[bool] = None
    service_name: Optional[str] = Field(default=None, alias="serviceName")
    enable: Optional[bool] = True
    global_: Optional[bool] = Field(default=False, alias="global")
    url: Optional[str] = None
    auth_header: Optional[str] = Field(default=None, alias="authHeader")
    method_type: Optional[str] = Field(default="POST", alias="methodType")
    batch_time: Optional[str] = Field(default=None, alias="batchTime")
    create_time: Optional[str] = Field(default=None, alias="createTime")
    update_time: Optional[str] = Field(default=None, alias="updateTime")


class WebhookTask(LooseModel):
    task_id: Optional[str] = Field(default=None, alias="taskId")
    event_id: Optional[str] = Field(default=None, alias="eventId")
    task_url: Optional[str] = Field(default=None, alias="taskUrl")
    task_method: Optional[str] = Field(default=None, alias="taskMethod")
    request: Optional[str] = None
    response: Optional[str] = None
    http_status: Optional[int] = Field(default=None, alias="httpStatus")
    retry_number: Optional[int] = Field(default=0, alias="retryNumber")
    error_msg: Optional[str] = Field(default=None, alias="errorMsg")
    status: Optional[str] = None
    create_time: Optional[str] = Field(default=None, alias="createTime")
    update_time: Optional[str] = Field(default=None, alias="updateTime")


def bean_to_webhook(bean: JbmClusterBusinessEventBean) -> tuple[WebhookEventConfig, WebhookTask]:
    config = WebhookEventConfig(
        businessEventCode=bean.event_code,
        eventName=bean.event_name,
        eventGroup=bean.event_group,
        eventBody=bean.event_body,
        serviceName=bean.service_name,
        enable=True if bean.enable is None else bean.enable,
        global_=False if bean.global_ is None else bean.global_,
        url=bean.url,
        methodType=bean.method_type or "POST",
    )
    task = WebhookTask(eventId=config.event_id, request=bean.event_body)
    return config, task


class BusinessEventRepository:
    def __init__(self, database_config: Mapping[str, Any]) -> None:
        database_url = configured_database_url(database_config) or "sqlite+aiosqlite:///./data/jbm-python-cluster.db"
        self.database_url = database_url
        if database_url.startswith("sqlite+aiosqlite:///"):
            db_path = database_url.replace("sqlite+aiosqlite:///", "", 1)
            if db_path and not db_path.startswith(":"):
                Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self.engine: AsyncEngine = create_async_engine(database_url, pool_pre_ping=True)

    async def start(self) -> None:
        async with self.engine.begin() as conn:
            has_config_table = await conn.run_sync(
                lambda sync_conn: inspect(sync_conn).has_table(WEBHOOK_EVENT_CONFIG_TABLE)
            )
            if not has_config_table:
                await conn.execute(
                    text(
                        f"""
                        CREATE TABLE {WEBHOOK_EVENT_CONFIG_TABLE} (
                          event_id VARCHAR(64) PRIMARY KEY,
                          business_event_code VARCHAR(256),
                          event_name VARCHAR(512),
                          event_group VARCHAR(512),
                          event_body TEXT,
                          internal TINYINT,
                          service_name VARCHAR(256),
                          enable TINYINT,
                          `global` TINYINT,
                          url VARCHAR(1024),
                          auth_header VARCHAR(2048),
                          method_type VARCHAR(32),
                          batch_time VARCHAR(64),
                          create_time VARCHAR(64),
                          update_time VARCHAR(64)
                        )
                        """
                    )
                )

            has_task_table = await conn.run_sync(lambda sync_conn: inspect(sync_conn).has_table(WEBHOOK_TASK_TABLE))
            if not has_task_table:
                await conn.execute(
                    text(
                        f"""
                        CREATE TABLE {WEBHOOK_TASK_TABLE} (
                          task_id VARCHAR(64) PRIMARY KEY,
                          event_id VARCHAR(64),
                          task_url VARCHAR(1024),
                          task_method VARCHAR(32),
                          request TEXT,
                          response TEXT,
                          http_status INTEGER,
                          retry_number INTEGER,
                          error_msg TEXT,
                          status VARCHAR(64),
                          create_time VARCHAR(64),
                          update_time VARCHAR(64)
                        )
                        """
                    )
                )

    async def stop(self) -> None:
        await self.engine.dispose()

    async def select_config_by_code_url(self, code: str, url: str) -> Optional[Dict[str, Any]]:
        async with self.engine.begin() as conn:
            row = (
                await conn.execute(
                    text(
                        f"""
                        SELECT * FROM {WEBHOOK_EVENT_CONFIG_TABLE}
                        WHERE business_event_code=:code AND url=:url
                        LIMIT 1
                        """
                    ),
                    {"code": code, "url": url},
                )
            ).mappings().first()
        return self._config_from_db(dict(row)) if row else None

    async def select_config_by_event_id(self, event_id: str) -> Optional[Dict[str, Any]]:
        async with self.engine.begin() as conn:
            row = (
                await conn.execute(
                    text(f"SELECT * FROM {WEBHOOK_EVENT_CONFIG_TABLE} WHERE event_id=:event_id LIMIT 1"),
                    {"event_id": event_id},
                )
            ).mappings().first()
        return self._config_from_db(dict(row)) if row else None

    async def select_configs_by_event_code(self, code: str) -> List[Dict[str, Any]]:
        async with self.engine.begin() as conn:
            rows = (
                await conn.execute(
                    text(
                        f"""
                        SELECT * FROM {WEBHOOK_EVENT_CONFIG_TABLE}
                        WHERE business_event_code=:code
                        ORDER BY update_time DESC
                        """
                    ),
                    {"code": code},
                )
            ).mappings().all()
        return [self._config_from_db(dict(row)) for row in rows]

    async def save_config(self, config: WebhookEventConfig | Mapping[str, Any]) -> Dict[str, Any]:
        row = (
            config.model_dump(by_alias=True, exclude_none=True)
            if isinstance(config, WebhookEventConfig)
            else dict(config)
        )
        now = now_text()
        row.setdefault("eventId", object_id())
        current = await self.select_config_by_event_id(str(row["eventId"]))
        row.setdefault("createTime", current.get("createTime") if current else now)
        row["updateTime"] = now
        db_row = self._config_to_db(row)
        if current:
            await self._update(WEBHOOK_EVENT_CONFIG_TABLE, "event_id", db_row)
        else:
            await self._insert(WEBHOOK_EVENT_CONFIG_TABLE, db_row)
        saved = await self.select_config_by_event_id(str(row["eventId"]))
        return saved or row

    async def delete_old_batch(self, service_id: Optional[str], batch_time: str) -> int:
        if not service_id:
            return 0
        async with self.engine.begin() as conn:
            result = await conn.execute(
                text(
                    f"""
                    DELETE FROM {WEBHOOK_EVENT_CONFIG_TABLE}
                    WHERE service_name=:service_id AND (batch_time IS NULL OR batch_time<>:batch_time)
                    """
                ),
                {"service_id": service_id, "batch_time": batch_time},
            )
        return int(result.rowcount or 0)

    async def save_task(self, task: WebhookTask | Mapping[str, Any]) -> Dict[str, Any]:
        row = task.model_dump(by_alias=True, exclude_none=True) if isinstance(task, WebhookTask) else dict(task)
        now = now_text()
        row.setdefault("taskId", object_id())
        current = await self.select_task(str(row["taskId"]))
        row.setdefault("createTime", current.get("createTime") if current else now)
        row["updateTime"] = now
        db_row = self._task_to_db(row)
        if current:
            await self._update(WEBHOOK_TASK_TABLE, "task_id", db_row)
        else:
            await self._insert(WEBHOOK_TASK_TABLE, db_row)
        saved = await self.select_task(str(row["taskId"]))
        return saved or row

    async def select_task(self, task_id: str) -> Optional[Dict[str, Any]]:
        async with self.engine.begin() as conn:
            row = (
                await conn.execute(
                    text(f"SELECT * FROM {WEBHOOK_TASK_TABLE} WHERE task_id=:task_id LIMIT 1"),
                    {"task_id": task_id},
                )
            ).mappings().first()
        return self._task_from_db(dict(row)) if row else None

    async def find_task(self, query: Mapping[str, Any]) -> Optional[Dict[str, Any]]:
        task_id = query.get("taskId") or query.get("task_id")
        if task_id:
            return await self.select_task(str(task_id))
        return None

    async def page_webhook_tasks(self, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        payload = body or {}
        task = dict(payload.get("webhookTask") or {})
        config = dict(payload.get("webhookEventConfig") or {})
        page_form = page_form_from_body(payload)
        where = []
        params: Dict[str, Any] = {}
        if task.get("taskId"):
            where.append("wt.task_id=:task_id")
            params["task_id"] = task["taskId"]
        if task.get("httpStatus") is not None:
            where.append("wt.http_status=:http_status")
            params["http_status"] = task["httpStatus"]
        if task.get("status"):
            where.append("wt.status=:status")
            params["status"] = task["status"]
        if config.get("eventId"):
            where.append("wec.event_id=:event_id")
            params["event_id"] = config["eventId"]
        for key, column in {
            "businessEventCode": "wec.business_event_code",
            "eventName": "wec.event_name",
            "eventGroup": "wec.event_group",
        }.items():
            if config.get(key):
                where.append(f"{column} LIKE :{key}")
                params[key] = f"%{config[key]}%"
        if payload.get("beginTime"):
            where.append("wt.create_time>=:begin_time")
            params["begin_time"] = payload["beginTime"]
        if payload.get("endTime"):
            where.append("wt.create_time<=:end_time")
            params["end_time"] = payload["endTime"]
        where_sql = " WHERE " + " AND ".join(where) if where else ""
        limit = max(int(page_form.page_size or 10), 1)
        offset = (max(int(page_form.curr_page or 1), 1) - 1) * limit
        async with self.engine.begin() as conn:
            total = (
                await conn.execute(
                    text(
                        f"""
                        SELECT COUNT(*) FROM {WEBHOOK_TASK_TABLE} wt
                        JOIN {WEBHOOK_EVENT_CONFIG_TABLE} wec ON wt.event_id=wec.event_id
                        {where_sql}
                        """
                    ),
                    params,
                )
            ).scalar()
            rows = (
                await conn.execute(
                    text(
                        f"""
                        SELECT wt.*, wec.event_name, wec.event_group, wec.business_event_code, wec.url
                        FROM {WEBHOOK_TASK_TABLE} wt
                        JOIN {WEBHOOK_EVENT_CONFIG_TABLE} wec ON wt.event_id=wec.event_id
                        {where_sql}
                        ORDER BY wt.create_time DESC
                        LIMIT :limit OFFSET :offset
                        """
                    ),
                    {**params, "limit": limit, "offset": offset},
                )
            ).mappings().all()
        return java_page([self._task_result_from_db(dict(row)) for row in rows], int(total or 0), page_form)

    async def page_webhook_event_configs(self, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        payload = body or {}
        config = dict(payload.get("webhookEventConfig") or payload)
        page_form = page_form_from_body(payload)
        where = []
        params: Dict[str, Any] = {}
        for key, column in {
            "eventId": "event_id",
            "businessEventCode": "business_event_code",
            "eventName": "event_name",
            "eventGroup": "event_group",
            "serviceName": "service_name",
            "url": "url",
        }.items():
            if config.get(key):
                if key in {"eventName", "eventGroup", "businessEventCode", "url"}:
                    where.append(f"{column} LIKE :{key}")
                    params[key] = f"%{config[key]}%"
                else:
                    where.append(f"{column}=:{key}")
                    params[key] = config[key]
        if config.get("enable") is not None:
            where.append("enable=:enable")
            params["enable"] = int(as_bool(config.get("enable"), True))
        keyword = str(payload.get("keyword") or page_form.model_dump(by_alias=True).get("keyword") or "").strip()
        if keyword:
            where.append(
                "(business_event_code LIKE :keyword OR event_name LIKE :keyword OR event_group LIKE :keyword "
                "OR service_name LIKE :keyword OR url LIKE :keyword)"
            )
            params["keyword"] = f"%{keyword}%"
        where_sql = " WHERE " + " AND ".join(where) if where else ""
        limit = max(int(page_form.page_size or 10), 1)
        offset = (max(int(page_form.curr_page or 1), 1) - 1) * limit
        async with self.engine.begin() as conn:
            total = (
                await conn.execute(
                    text(f"SELECT COUNT(*) FROM {WEBHOOK_EVENT_CONFIG_TABLE}{where_sql}"),
                    params,
                )
            ).scalar()
            rows = (
                await conn.execute(
                    text(
                        f"""
                        SELECT * FROM {WEBHOOK_EVENT_CONFIG_TABLE}
                        {where_sql}
                        ORDER BY update_time DESC
                        LIMIT :limit OFFSET :offset
                        """
                    ),
                    {**params, "limit": limit, "offset": offset},
                )
            ).mappings().all()
        return java_page([self._config_from_db(dict(row)) for row in rows], int(total or 0), page_form)

    async def _insert(self, table: str, row: Mapping[str, Any]) -> None:
        cleaned = clean_dict(row)
        columns = list(cleaned.keys())
        sql = (
            f"INSERT INTO {table} ({', '.join(quote_column(column) for column in columns)}) "
            f"VALUES ({', '.join(':' + column for column in columns)})"
        )
        async with self.engine.begin() as conn:
            await conn.execute(text(sql), cleaned)

    async def _update(self, table: str, key_column: str, row: Mapping[str, Any]) -> None:
        cleaned = clean_dict(row)
        columns = [column for column in cleaned if column != key_column]
        assignments = ", ".join(f"{quote_column(column)}=:{column}" for column in columns)
        sql = f"UPDATE {table} SET {assignments} WHERE {quote_column(key_column)}=:{key_column}"
        async with self.engine.begin() as conn:
            await conn.execute(text(sql), cleaned)

    def _config_to_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "event_id": row.get("eventId"),
            "business_event_code": row.get("businessEventCode"),
            "event_name": row.get("eventName"),
            "event_group": row.get("eventGroup"),
            "event_body": row.get("eventBody"),
            "internal": int(as_bool(row.get("internal"))) if row.get("internal") is not None else None,
            "service_name": row.get("serviceName"),
            "enable": int(as_bool(row.get("enable"), True)),
            "global": int(as_bool(row.get("global"), False)),
            "url": row.get("url"),
            "auth_header": row.get("authHeader"),
            "method_type": row.get("methodType") or "POST",
            "batch_time": row.get("batchTime"),
            "create_time": row.get("createTime"),
            "update_time": row.get("updateTime"),
        }

    def _config_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "eventId": row.get("event_id"),
            "businessEventCode": row.get("business_event_code"),
            "eventName": row.get("event_name"),
            "eventGroup": row.get("event_group"),
            "eventBody": row.get("event_body"),
            "internal": as_bool(row.get("internal")) if row.get("internal") is not None else None,
            "serviceName": row.get("service_name"),
            "enable": as_bool(row.get("enable"), True),
            "global": as_bool(row.get("global"), False),
            "url": row.get("url"),
            "authHeader": row.get("auth_header"),
            "methodType": row.get("method_type"),
            "batchTime": row.get("batch_time"),
            "createTime": row.get("create_time"),
            "updateTime": row.get("update_time"),
        }

    def _task_to_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "task_id": row.get("taskId"),
            "event_id": row.get("eventId"),
            "task_url": row.get("taskUrl"),
            "task_method": row.get("taskMethod"),
            "request": row.get("request"),
            "response": row.get("response"),
            "http_status": row.get("httpStatus"),
            "retry_number": row.get("retryNumber") or 0,
            "error_msg": row.get("errorMsg"),
            "status": row.get("status"),
            "create_time": row.get("createTime"),
            "update_time": row.get("updateTime"),
        }

    def _task_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        return {
            "taskId": row.get("task_id"),
            "eventId": row.get("event_id"),
            "taskUrl": row.get("task_url"),
            "taskMethod": row.get("task_method"),
            "request": row.get("request"),
            "response": row.get("response"),
            "httpStatus": row.get("http_status"),
            "retryNumber": row.get("retry_number") or 0,
            "errorMsg": row.get("error_msg"),
            "status": row.get("status"),
            "createTime": row.get("create_time"),
            "updateTime": row.get("update_time"),
        }

    def _task_result_from_db(self, row: Mapping[str, Any]) -> Dict[str, Any]:
        result = self._task_from_db(row)
        result.update(
            {
                "eventName": row.get("event_name"),
                "eventGroup": row.get("event_group"),
                "businessEventCode": row.get("business_event_code"),
                "url": row.get("url"),
            }
        )
        return result


class WebhookConfigSelectionStrategy:
    def __init__(self) -> None:
        self.last_success_config: Dict[str, str] = {}
        self.online_services: set[str] = set()
        self.service_failure_count: Dict[str, int] = defaultdict(int)

    def select_config(self, group: str, configs: Iterable[Mapping[str, Any]]) -> Optional[Dict[str, Any]]:
        enabled = [dict(config) for config in configs if as_bool(config.get("enable"), True)]
        if not enabled:
            return None
        last_success = self.last_success_config.get(group)
        if last_success:
            for config in enabled:
                if config.get("eventId") == last_success and self._online(config) and self._healthy(config):
                    return config
        for config in enabled:
            if self._online(config) and self._healthy(config):
                return config
        return min(enabled, key=self._failure_count)

    def handle_success(self, group: str, config: Mapping[str, Any]) -> None:
        event_id = config.get("eventId")
        if event_id:
            self.last_success_config[group] = str(event_id)
        service_name = config.get("serviceName")
        if service_name:
            self.service_failure_count.pop(str(service_name), None)

    def handle_failure(self, group: str, config: Mapping[str, Any]) -> None:
        event_id = config.get("eventId")
        if event_id and self.last_success_config.get(group) == event_id:
            self.last_success_config.pop(group, None)
        service_name = config.get("serviceName")
        if service_name:
            self.service_failure_count[str(service_name)] += 1

    def _online(self, config: Mapping[str, Any]) -> bool:
        service_name = config.get("serviceName")
        return not service_name or str(service_name) in self.online_services

    def _healthy(self, config: Mapping[str, Any]) -> bool:
        return self._failure_count(config) < 3

    def _failure_count(self, config: Mapping[str, Any]) -> int:
        service_name = config.get("serviceName")
        if not service_name:
            return 0
        return self.service_failure_count.get(str(service_name), 0)


class WebhookHttpClient:
    def __init__(self, discovery: NacosDiscoveryClient, timeout: float = 10.0) -> None:
        self.discovery = discovery
        self.timeout = timeout
        self.client: Optional[httpx.AsyncClient] = None

    async def start(self) -> None:
        self.client = httpx.AsyncClient(timeout=self.timeout)

    async def stop(self) -> None:
        if self.client is not None:
            await self.client.aclose()
            self.client = None

    async def request(self, url: str, method: Optional[str], body: Optional[str]) -> httpx.Response:
        client = self.client or httpx.AsyncClient(timeout=self.timeout)
        close_after = self.client is None
        try:
            resolved_url = await self._resolve_url(url)
            headers = {"Content-Type": "application/json;charset=UTF-8"}
            content = body or ""
            return await client.request((method or "POST").upper(), resolved_url, content=content, headers=headers)
        finally:
            if close_after:
                await client.aclose()

    async def _resolve_url(self, url: str) -> str:
        if not url.startswith("feign://"):
            return url
        without_scheme = url[len("feign://") :]
        service_name, _, path = without_scheme.partition("/")
        instance = await self.discovery.choose_instance(service_name)
        if not instance:
            raise RuntimeError("No healthy Nacos instance for %s" % service_name)
        ip = instance.get("ip")
        port = instance.get("port")
        if not ip or not port:
            raise RuntimeError("Invalid Nacos instance for %s" % service_name)
        return "http://%s:%s/%s" % (ip, port, path)


class WebhookDeliveryConsumer:
    """Consumes RabbitMQ delivery tasks and performs HTTP webhook calls."""

    def __init__(
        self,
        repository: BusinessEventRepository,
        http_client: WebhookHttpClient,
        selection: Optional[WebhookConfigSelectionStrategy] = None,
    ) -> None:
        self.repository = repository
        self.http_client = http_client
        self.selection = selection

    async def handle(self, body: bytes) -> None:
        task = json.loads(body.decode("utf-8"))
        if not task.get("taskUrl"):
            raise ValueError("taskUrl is empty")
        task.setdefault("taskId", object_id())
        task.setdefault("retryNumber", 0)
        task["status"] = "PENDING"
        saved = await self.repository.save_task(task)
        response = await self.http_client.request(
            str(saved["taskUrl"]),
            saved.get("taskMethod"),
            saved.get("request"),
        )
        updated = dict(saved)
        updated["httpStatus"] = response.status_code
        updated["response"] = response.text
        if 200 <= response.status_code < 300:
            updated["status"] = "SUCCESS"
            await self.repository.save_task(updated)
            await self._mark_success(updated)
            return
        updated["errorMsg"] = self._append_error(updated.get("errorMsg"), "HTTP %s" % response.status_code)
        updated["status"] = "RETRYING"
        await self.repository.save_task(updated)
        raise RuntimeError("HTTP %s" % response.status_code)

    async def handle_dlt(self, body: bytes) -> None:
        task = json.loads(body.decode("utf-8"))
        task_id = str(task.get("taskId") or "")
        current = await self.repository.select_task(task_id) if task_id else None
        updated = dict(current or task)
        updated["status"] = "FAILED"
        updated["retryNumber"] = int(updated.get("retryNumber") or 0)
        updated["errorMsg"] = self._append_error(
            updated.get("errorMsg"),
            "已达最大重试次数 %s" % MAX_RETRY,
        )
        await self.repository.save_task(updated)

    async def _mark_success(self, task: Mapping[str, Any]) -> None:
        if self.selection is None:
            return
        event_id = task.get("eventId")
        if not event_id:
            return
        config = await self.repository.select_config_by_event_id(str(event_id))
        if not config:
            return
        group = str(config.get("eventGroup") or "")
        if group:
            self.selection.handle_success(group, config)

    def _append_error(self, current: Optional[str], message: str) -> str:
        prefix = (current or "").strip()
        line = "%s : %s" % (now_text(), message or "无")
        return (prefix + "\r\n" + line).strip() if prefix else line


class BusinessEventService:
    def __init__(
        self,
        repository: BusinessEventRepository,
        redis_client: RedisClient,
        discovery: NacosDiscoveryClient,
        http_client: Optional[WebhookHttpClient] = None,
        rabbitmq: Optional[Any] = None,
        rabbitmq_config: Optional[Mapping[str, Any]] = None,
        delivery_consumer: Optional[WebhookDeliveryConsumer] = None,
    ) -> None:
        self.repository = repository
        self.redis_client = redis_client
        self.discovery = discovery
        self.http_client = http_client or WebhookHttpClient(discovery)
        self.rabbitmq = rabbitmq
        self.rabbitmq_config = dict(rabbitmq_config or {})
        self.selection = WebhookConfigSelectionStrategy()
        self.delivery_consumer = delivery_consumer or WebhookDeliveryConsumer(
            repository,
            self.http_client,
            self.selection,
        )

    async def start(self) -> None:
        await self.repository.start()
        await self.redis_client.start()
        await self.discovery.start()
        await self.http_client.start()
        await self._subscribe_rabbitmq()

    async def stop(self) -> None:
        await self.http_client.stop()
        await self.discovery.stop()
        await self.redis_client.stop()
        await self.repository.stop()

    async def _subscribe_rabbitmq(self) -> None:
        if self.rabbitmq is None:
            return
        if self.rabbitmq.channel is None:
            await self.rabbitmq.start()
        event_queue = str(
            self.rabbitmq_config.get("business-event-queue")
            or "businessEvent-in-0.jbm-cluster-platform-push"
        )
        resource_queue = str(
            self.rabbitmq_config.get("business-event-resource-queue")
            or "businessEventResource-in-0.jbm-cluster-platform-push"
        )
        await self.rabbitmq.consume_json(resource_queue, self.receive_resource_payload)
        await self.rabbitmq.consume_json(event_queue, self.send_business_event_payload)

    async def receive_resource_payload(self, payload: Mapping[str, Any]) -> Dict[str, Any]:
        resource = JbmClusterBusinessEventResource(**dict(payload))
        batch_time = now_text()
        saved = 0
        for bean in resource.jbm_cluster_business_event_beans:
            config, _ = bean_to_webhook(bean)
            if not config.business_event_code or not config.url:
                continue
            current = await self.repository.select_config_by_code_url(config.business_event_code, config.url)
            if current:
                config.event_id = str(current["eventId"])
            config.batch_time = batch_time
            await self.repository.save_config(config)
            saved += 1
        deleted = await self.repository.delete_old_batch(resource.service_id, batch_time)
        return {"saved": saved, "deleted": deleted, "batchTime": batch_time}

    async def send_business_event_payload(self, payload: Mapping[str, Any]) -> Dict[str, Any]:
        bean = JbmClusterBusinessEventBean(**dict(payload))
        config, task = bean_to_webhook(bean)
        return await self.send_business_event(config, task)

    async def send_business_event_by_event_id(self, event_id: str) -> Dict[str, Any]:
        config = await self.repository.select_config_by_event_id(event_id)
        if not config:
            raise ValueError("事件为空")
        task = WebhookTask(eventId=event_id)
        return await self._enqueue_config(config, task.model_dump(by_alias=True, exclude_none=True))

    async def send_task(self, task_payload: Mapping[str, Any]) -> Dict[str, Any]:
        task = WebhookTask(**dict(task_payload))
        if not task.event_id:
            raise ValueError("eventId不能为空")
        config = await self.repository.select_config_by_event_id(task.event_id)
        if not config:
            raise ValueError("事件为空")
        return await self.send_business_event(WebhookEventConfig(**config), task)

    async def retry_event_task(self, task_id: str) -> Dict[str, Any]:
        task = await self.repository.select_task(task_id)
        if not task:
            raise ValueError("任务不存在")
        return await self.send_task(task)

    async def send_business_event(self, config: WebhookEventConfig, task: WebhookTask) -> Dict[str, Any]:
        configs = await self._enable_event_configs(config)
        groups: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
        for item in configs:
            group = str(item.get("eventGroup") or "")
            if group:
                groups[group].append(item)
        results = []
        for group, group_configs in groups.items():
            selected = self.selection.select_config(group, group_configs)
            if selected is None:
                continue
            delivery = await self._enqueue_config(selected, task.model_dump(by_alias=True, exclude_none=True))
            results.append(delivery)
        return {"sent": len(results), "tasks": results}

    async def _enable_event_configs(self, config: WebhookEventConfig) -> List[Dict[str, Any]]:
        if config.event_id:
            config.enable = True
            configs = [config.model_dump(by_alias=True, exclude_none=True)]
        elif config.business_event_code:
            configs = await self.repository.select_configs_by_event_code(config.business_event_code)
        else:
            configs = []
        enabled = [item for item in configs if as_bool(item.get("enable"), True)]
        if not enabled:
            raise ValueError("不存在可用的发送配置")
        return enabled

    async def _enqueue_config(self, config: Mapping[str, Any], source_task: Mapping[str, Any]) -> Dict[str, Any]:
        delivery = self._build_delivery_task(config, source_task)
        if not as_bool(config.get("enable"), True):
            delivery["errorMsg"] = self._append_error(delivery.get("errorMsg"), "事件未启用")
            delivery["status"] = "FAILED"
            return await self.repository.save_task(delivery)
        saved = await self.repository.save_task({**delivery, "status": "PENDING"})
        await self._publish_delivery_task(saved)
        latest = await self.repository.select_task(str(saved["taskId"]))
        return latest or saved

    def _build_delivery_task(self, config: Mapping[str, Any], source_task: Mapping[str, Any]) -> Dict[str, Any]:
        task = dict(source_task or {})
        if task.get("eventId") != config.get("eventId"):
            task = {"request": task.get("request")}
        return {
            "taskId": object_id(),
            "eventId": config.get("eventId"),
            "taskUrl": config.get("url"),
            "taskMethod": config.get("methodType") or "POST",
            "request": task.get("request") or config.get("eventBody"),
            "retryNumber": int(task.get("retryNumber") or 0),
            "createTime": now_text(),
        }

    async def _publish_delivery_task(self, task: Mapping[str, Any]) -> None:
        if self.rabbitmq is None:
            await self.delivery_consumer.handle(
                json.dumps(task, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
            )
            return
        await self.rabbitmq.publish_delivery_task(task)

    def _append_error(self, current: Optional[str], message: str) -> str:
        prefix = (current or "").strip()
        line = "%s : %s" % (now_text(), message or "无")
        return (prefix + "\r\n" + line).strip() if prefix else line


def quote_column(column: str) -> str:
    return "`%s`" % column
