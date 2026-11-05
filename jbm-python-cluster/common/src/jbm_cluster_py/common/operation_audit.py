"""Business operation facts use the existing log service, not business-history storage."""

from __future__ import annotations

import asyncio
import json
import logging
import os
import re
import time
import uuid
import tempfile
import threading
from contextlib import asynccontextmanager
from contextvars import ContextVar
from pathlib import Path

from .admin_security import (
    UNSET_AUDIT_TENANT,
    audit_actor,
    source_ip,
    AdminAccessPolicy,
    AdminPolicyStore,
)
from .api_access import read_operation
from .security_audit import (
    AUDIT_TOPIC,
    audit_event,
    audit_digest,
    OPERATION_ACK_PREFIX,
    OPERATION_HEALTH_KEY,
)

logger = logging.getLogger(__name__)
current_operation = ContextVar("jbm_business_audit_operation", default=None)
LIMIT = 16384
SENSITIVE = re.compile(
    r"password|passwd|secret|token|authorization|cookie|captcha|(?:private|encryption|integrity|access|api|signing)[._-]?key|^key$|^command$|^script$|credential|signature|certificate|密钥|密码|验证码",
    re.I,
)


def important_operation(method, path):
    if path.rstrip("/") in {"/securityAudit/query", "/loginLogs/pageList"}:
        return True  # Audit administration reads must themselves leave a durable trail.
    return not read_operation(method, path) or path.rstrip("/").rsplit("/", 1)[-1].lower() in {
        "export",
        "download",
        "file",
        "secret",
    }


def operation_name(method, path):
    if path.rstrip("/") in {"/securityAudit/query", "/loginLogs/pageList"}:
        return "查询审计日志"
    names = {
        "user": "用户",
        "baseOrg": "组织",
        "org": "组织",
        "role": "角色",
        "authority": "权限",
        "tenant-features": "成员权限",
        "menu": "菜单",
        "api": "接口权限",
        "apikey": "应用密钥",
        "sources": "数据库连接",
        "backups": "数据库备份",
        "restores": "数据库还原",
        "device": "设备",
        "devices": "设备",
        "project": "项目",
        "projects": "项目",
    }
    parts = path.strip("/").split("/")
    resource = next((names[p] for p in reversed(parts) if p in names), "业务资料")
    action = {"POST": "提交", "PUT": "修改", "PATCH": "修改", "DELETE": "删除", "GET": "读取"}.get(
        method, method
    )
    if parts[-1] in {"export", "download", "file"}:
        action = "导出"
    if parts[-1] == "password":
        return "修改用户密码"
    return action + resource


def safe_values(value, depth=0):
    if depth > 5:
        return "[层级过深]"
    if isinstance(value, dict):
        secret_value = any(
            SENSITIVE.search(str(value.get(k) or ""))
            for k in ("key", "configKey", "property", "name")
        )
        return {
            str(k)[:80]: "[已脱敏]"
            if SENSITIVE.search(str(k))
            or secret_value
            and str(k) in {"value", "configValue", "content"}
            else safe_values(v, depth + 1)
            for k, v in list(value.items())[:50]
        }
    if isinstance(value, list):
        return [safe_values(v, depth + 1) for v in value[:30]]
    if isinstance(value, str):
        if value.lstrip().startswith(("{", "[")):
            try:
                return safe_values(json.loads(value), depth + 1)
            except ValueError:
                pass
        if re.search(r"Bearer\s|-----BEGIN |(?:access_token|password|secret)=", value, re.I):
            return "[已脱敏]"
        return value[:256]
    return value if value is None or isinstance(value, (bool, int, float)) else "[非标量]"


def operation_result(status, raw):
    """Accepted jobs never imply that a device or asynchronous worker succeeded."""
    try:
        body = json.loads(raw)
    except (ValueError, UnicodeError):
        body = {}
    success = body.get("success") if isinstance(body, dict) else None
    data = (
        (body.get("result") if "result" in body else body.get("data"))
        if isinstance(body, dict)
        else None
    )
    candidates = data if isinstance(data, list) else [data] if isinstance(data, dict) else []
    if isinstance(data, dict):
        for name in ("tasks", "results", "items"):
            if isinstance(data.get(name), list):
                candidates += data[name]
    candidates = [item for item in candidates if isinstance(item, dict)]
    states = [
        str(item.get("executionStatus") or item.get("state") or item.get("status") or "").lower()
        for item in candidates
    ]
    pending_states = {
        "accepted",
        "queued",
        "running",
        "pending",
        "sent",
        "written",
        "confirming",
        "processing",
        "received",
        "issued",
        "waiting",
    }
    accepted = status == 202 or any(state in pending_states for state in states) or any(
        state == "success" and item.get("confirmationMode") == "state"
        for state, item in zip(states, candidates)
    )
    unknown = any(state in {"timeout", "unknown", "uncertain"} for state in states)
    failed = (
        status >= 400
        or success is False
        or (
            isinstance(data, dict)
            and str(
                data.get("executionStatus") or data.get("state") or data.get("status") or ""
            ).lower()
            in {"failed", "failure", "error", "rejected", "cancelled", "canceled"}
        )
        or (
            not accepted
            and any(
                state in {"failed", "failure", "error", "rejected", "cancelled", "canceled"}
                for state in states
            )
        )
    )
    outcome = (
        "FAILED"
        if failed
        else "ACCEPTED"
        if accepted
        else "UNKNOWN"
        if unknown
        else "SUCCEEDED"
        if success is True or status == 204
        else "UNKNOWN"
    )
    refs = (
        {
            k: str(v)[:128]
            for k, v in (data or {}).items()
            if k
            in {
                "id",
                "taskId",
                "commandId",
                "requestId",
                "groupId",
                "jobId",
                "documentId",
                "deviceId",
                "projectId",
                "associationId",
                "resourceId",
            }
            and isinstance(v, (str, int))
        }
        if isinstance(data, dict)
        else {}
    )
    if accepted:
        task_ids = []
        primary = next(
            (
                data[k]
                for k in ("taskId", "commandId", "requestId", "groupId", "jobId")
                if isinstance(data, dict) and data.get(k) is not None
            ),
            None,
        )
        if primary is not None:
            task_ids.append(str(primary))
        for item in [] if primary is not None else candidates:
            identifier = next(
                (
                    item[k]
                    for k in ("taskId", "commandId", "requestId", "groupId", "jobId", "id")
                    if item.get(k) is not None
                ),
                None,
            )
            if identifier is not None and str(identifier) not in task_ids:
                task_ids.append(str(identifier))
        if task_ids:
            refs["taskIds"] = task_ids[:100]
    message = (body.get("message") or body.get("detail") or "") if isinstance(body, dict) else ""
    return outcome, refs, safe_values(str(message))


class OperationAuditor:
    def __init__(self, config, *, source_role=None):
        from jbm_cluster_py.integrations.kafka import KafkaClient
        # Hosts can keep their own business configuration/discovery. This small,
        # read-only deployment secret contains only the shared audit transports
        # and policy-centre settings; it never replaces host business settings.
        if os.getenv("JBM_OPERATION_AUDIT_CONFIG_FILE"):
            from .config import AppConfig, deep_merge
            override = json.loads(Path(os.environ["JBM_OPERATION_AUDIT_CONFIG_FILE"]).read_text(encoding="utf8"))
            config = AppConfig(deep_merge(config.raw, override), config.profile, config.config_dir, config.app)
        self.config = config

        self.enabled = config.get("jbm.security-audit.enabled", False) is True
        self.service = os.getenv("JBM_AUDIT_SERVICE_NAME") or config.service_name
        self.admin_access = AdminAccessPolicy(config.get("jbm.admin-access", {}) or {})
        self.policy_store = AdminPolicyStore(config)
        self.publisher_key = source_role or self.service
        self.kafka = KafkaClient(config.kafka)
        from jbm_cluster_py.integrations.redis import RedisClient

        self.receipts = RedisClient(getattr(config, "redis", {}))
        role = re.sub(r"[^A-Za-z0-9_-]", "_", self.publisher_key)
        self.directory = (
            Path(
                config.get(
                    "jbm.security-audit.operation-spool-directory", "/app/data/security-audit-spool"
                )
            )
            / role
        )
        self.proxies = config.get(
            "jbm.admin-access.trusted-proxy-networks", ["127.0.0.0/8", "::1/128", "172.16.0.0/12"]
        )
        self.task = None
        self.pending_results = []
        self.sent = {}
        self.last_spool_error = False
        self.scan_sources = []
        self.scan_error = False

    def prepare(self):
        if not self.kafka.enabled:
            raise ValueError("业务操作审计必须使用现有持久消息通道")
        if not self.receipts.enabled:
            raise ValueError("业务操作审计需要现有Redis提供归档确认及投递状态")
        if not any(
            os.path.ismount(parent) for parent in self.directory.parents if parent.parent != parent
        ):
            raise ValueError("业务操作审计暂存目录必须挂载持久卷，不能使用容器临时层")
        self.directory.mkdir(parents=True, exist_ok=True, mode=0o700)

    async def start(self):
        await asyncio.to_thread(self.policy_store.start)
        if not self.enabled:
            return
        self.prepare()
        self.task = asyncio.create_task(self._flush_loop())

    def _write(self, event, *, directory=None):
        directory = directory or self.directory
        directory.mkdir(parents=True, exist_ok=True, mode=0o700)
        raw = json.dumps(event, ensure_ascii=False, separators=(",", ":")).encode()
        if len(json.dumps(event, ensure_ascii=True, separators=(",", ":")).encode()) > 60000:
            raise ValueError("业务审计事件超长")
        path = directory / (event["eventId"] + ".json")
        fd, temporary = tempfile.mkstemp(dir=directory, prefix=".pending-")
        try:
            with os.fdopen(fd, "wb") as stream:
                stream.write(raw)
                stream.flush()
                os.fsync(stream.fileno())
            if os.name != "nt":
                os.chmod(temporary, 0o400)
            try:
                os.link(temporary, path)
            except FileExistsError:
                if path.read_bytes() != raw:
                    raise ValueError("禁止覆盖已有业务审计事实")
        finally:
            os.unlink(temporary)
        if os.name != "nt":
            fd = os.open(directory, os.O_RDONLY)
            try:
                os.fsync(fd)
            finally:
                os.close(fd)
        if path.read_bytes() != raw:
            raise ValueError("业务审计暂存回读失败")

    async def record(self, event):
        if self.enabled:
            if len(json.dumps(event, ensure_ascii=True).encode()) > 55000:
                event = {
                    **event,
                    "submittedChanges": {"content": "[内容过长，仅保留操作对象和结果]"},
                }
            try:
                await asyncio.to_thread(self._write, event)
                self.last_spool_error = False
            except OSError:
                self.last_spool_error = True
                # If the local disk fails, a broker ACK is another durable path.
                await asyncio.wait_for(
                    self.kafka.publish_json(AUDIT_TOPIC, event, key=event["eventId"]), timeout=2
                )

    async def flush(self):
        if self.receipts.client is None:
            await self.receipts.start()
        while self.pending_results:
            await self.record(self.pending_results[0])
            self.pending_results.pop(0)
        for path in sorted(self.directory.glob("*.json"))[:100]:
            try:
                event = json.loads(await asyncio.to_thread(path.read_text, encoding="utf8"))
            except FileNotFoundError:
                continue
            ack = await self.receipts.client.get(OPERATION_ACK_PREFIX + event["eventId"])
            if ack == audit_digest(event):
                # Only the log service issues this receipt, after independent
                # signed backup and exact Loki read-back. A broker ACK alone
                # never removes the source's persistent copy.
                await asyncio.to_thread(path.unlink, missing_ok=True)
                self.sent.pop(event["eventId"], None)
                continue
            if time.monotonic() - self.sent.get(event["eventId"], -30) >= 30:
                await asyncio.wait_for(
                    self.kafka.publish_json(AUDIT_TOPIC, event, key=event["eventId"]), timeout=10
                )
                self.sent[event["eventId"]] = time.monotonic()

    def _pending_health(self):
        rows = list(self.pending_results)
        for path in self.directory.glob("*.json"):
            try:
                rows.append(json.loads(path.read_text(encoding="utf8")))
            except FileNotFoundError:
                continue
        by_tenant = {}
        for event in rows:
            if event.get("tenantScopeVerified") and event.get("tenantId"):
                item = by_tenant.setdefault(
                    str(event["tenantId"]), {"pending": 0, "oldest": event["timestamp"]}
                )
                item["pending"] += 1
                item["oldest"] = min(item["oldest"], event["timestamp"])
        return {
            "pending": len(rows),
            "oldest": min((r["timestamp"] for r in rows), default=None),
            "byTenant": by_tenant,
            "diskError": self.last_spool_error,
            "heartbeat": int(time.time() * 1000),
        }

    async def report_health(self):
        if self.receipts.client is None:
            await self.receipts.start()
        row = await asyncio.to_thread(self._pending_health)
        row["scanError"] = self.scan_error
        await self.receipts.client.hset(
            OPERATION_HEALTH_KEY, self.publisher_key, json.dumps(row, separators=(",", ":"))
        )

    async def publisher_health(self):
        if self.receipts.client is None:
            await self.receipts.start()
        return [
            json.loads(value)
            for value in (await self.receipts.client.hgetall(OPERATION_HEALTH_KEY)).values()
        ]

    async def acknowledge(self, event):
        if event.get("category") != "operation" and event.get("source") not in {
            "legacy-job",
            "legacy-feige",
        }:
            return
        if self.receipts.client is None:
            await self.receipts.start()
        await self.receipts.client.set(
            OPERATION_ACK_PREFIX + event["eventId"], audit_digest(event), ex=7 * 86400
        )

    async def _flush_loop(self):
        while True:
            try:
                await self.report_health()
                await self.flush()
                self.scan_error = False
                for scanner in self.scan_sources:
                    try:
                        await asyncio.to_thread(scanner)
                    except Exception:
                        self.scan_error = True
                        logger.exception("Business audit source scan postponed; source retained")
                await self.report_health()
            except asyncio.CancelledError:
                raise
            except Exception:
                logger.exception("Business audit delivery pending; persistent spool retained")
            await asyncio.sleep(0.5)

    async def stop(self):
        await asyncio.to_thread(self.policy_store.stop)
        if self.task:
            self.task.cancel()
            await asyncio.gather(self.task, return_exceptions=True)
        await self.kafka.stop()
        await self.receipts.stop()
        if getattr(self, "sync_receipts", None) is not None:
            self.sync_receipts.close()

    def archive_confirmed_sync(self, event):
        if getattr(self, "sync_receipts", None) is None:
            import redis

            self.sync_receipts = redis.Redis.from_url(
                self.receipts.url, decode_responses=True, socket_connect_timeout=1, socket_timeout=2
            )
        return self.sync_receipts.get(OPERATION_ACK_PREFIX + event["eventId"]) == audit_digest(
            event
        )

    def complete_sync(
        self,
        context,
        outcome,
        reference,
        finished_at_ms,
        message="",
        *,
        event_key="",
        phase="RESULT",
        business_ref=None,
        business_revision=None,
    ):
        """Trusted workers persist the original operation ID with their task.

        A stable event ID/time lets a restarted worker replay its completion
        without changing or duplicating an already archived fact.
        """
        if not self.enabled or not context:
            return
        row = dict(context)
        row.update(
            eventId=uuid.uuid5(
                uuid.NAMESPACE_URL,
                str(context["operationId"]) + ":" + str(reference) + ":" + str(event_key),
            ).hex,
            timestamp=finished_at_ms,
            phase=phase,
            operationStatus=outcome,
            success=outcome == "SUCCEEDED" if outcome in {"SUCCEEDED", "FAILED"} else None,
            businessRef={"taskId": str(reference), **(business_ref or {})},
            businessTaskId=str(reference),
            completionKind="TASK",
            message=safe_values(message),
        )
        if business_revision is not None:
            row["businessRevision"] = int(business_revision)
        self._write(row)
        return row

    def start_background(self):
        """Sync history workers own this publisher; control dispatch never does."""
        if not self.enabled:
            return
        self.prepare()
        self.background_stop = threading.Event()
        ready = threading.Event()
        errors = []

        async def run():
            try:
                await self.start()
            except Exception as exc:
                errors.append(exc)
                ready.set()
                return
            ready.set()
            try:
                while not self.background_stop.is_set():
                    await asyncio.sleep(0.2)
            finally:
                await self.stop()

        self.background_thread = threading.Thread(
            target=lambda: asyncio.run(run()), name="business-audit-delivery", daemon=True
        )
        self.background_thread.start()
        if not ready.wait(30):
            self.background_stop.set()
            raise RuntimeError("Business audit publisher startup timed out; worker not started")
        if errors:
            self.background_thread.join(timeout=2)
            raise RuntimeError("Business audit publisher startup failed; worker not started") from errors[0]

    def stop_background(self):
        if getattr(self, "background_stop", None):
            self.background_stop.set()
            self.background_thread.join(timeout=5)


class OperationAuditMiddleware:
    def __init__(self, app, auditor):
        self.app, self.auditor = app, auditor

    async def __call__(self, scope, receive, send):
        state = scope.setdefault("state", {})
        identity = state.get("identity") or {}
        if scope["type"] != "http" or (identity.get("userId") is None and identity.get("user_id") is None):
            return await self.app(scope, receive, send)
        headers = dict(
            (k.decode("latin1").lower(), v.decode("latin1")) for k, v in scope.get("headers", [])
        )
        ip = source_ip((scope.get("client") or ("", 0))[0], headers, self.auditor.proxies)
        state["source_ip"] = ip
        policy = getattr(self.auditor, "admin_access", None)
        if policy is not None and scope["path"] != "/oauth2/userinfo":
            from fastapi import HTTPException, Request
            from starlette.responses import JSONResponse

            try:
                state["admin_source_allowed"] = policy.require(identity, ip)
            except HTTPException as exc:
                await record_denied_operation(
                    scope["app"], Request(scope, receive), identity, exc.status_code
                )
                return await JSONResponse(
                    {
                        "success": False,
                        "code": exc.status_code,
                        "message": exc.detail,
                        "result": None,
                    },
                    status_code=exc.status_code,
                )(scope, receive, send)
        if not self.auditor.enabled:
            return await self.app(scope, receive, send)
        operation_id = uuid.uuid4().hex
        supplied = state.get("request_id") or dict(scope.get("headers", [])).get(
            b"x-request-id", b""
        ).decode("latin1")
        request_id = (
            str(supplied)
            if re.fullmatch(r"[A-Za-z0-9._:-]{1,128}", str(supplied))
            else uuid.uuid4().hex
        )
        state["request_id"] = request_id
        context = {"operationId": operation_id, "requestId": request_id}

        def event(phase, outcome=None):
            actor = audit_actor(identity, state.get("audit_tenant_id", UNSET_AUDIT_TENANT))
            row = audit_event(
                "operation",
                userId=actor.get("userId") if actor.get("userId") is not None else identity.get("user_id"),
                account=actor.get("userName") or actor.get("username"),
                tenantId=actor.get("tenantId"),
                appId=actor.get("appId"),
                clientId=actor.get("clientId"),
                operation=scope["method"],
                target=scope["path"],
                ip=ip,
                requestId=request_id,
                phase=phase,
                success=True if outcome == "SUCCEEDED" else False if outcome == "FAILED" else None,
            )
            return {
                **row,
                **context,
                "category": "operation",
                "operationName": operation_name(scope["method"], scope["path"]),
                "serviceId": self.auditor.service,
                "operationStatus": outcome or "STARTED",
                "tenantScopeVerified": actor["tenantScopeVerified"],
                "platformActor": actor["platformActor"],
                "actorTenantId": actor.get("actorTenantId"),
                "adminSourceAllowed": state.get("admin_source_allowed"),
            }

        if not important_operation(scope["method"], scope["path"]):
            read_status, read_body = 200, bytearray()

            async def read_send(message):
                nonlocal read_status
                if message["type"] == "http.response.start":
                    read_status = message["status"]
                if message["type"] == "http.response.body":
                    read_body.extend(message.get("body", b"")[: max(0, LIMIT + 1 - len(read_body))])
                    if not message.get("more_body", False):
                        result, _, reason = operation_result(read_status, read_body)
                        if read_status >= 400 or result == "FAILED":
                            row = event("RESULT", "FAILED")
                            row.update(httpStatus=read_status, message=reason)
                            try:
                                await self.auditor.record(row)
                            except Exception:
                                logger.critical(
                                    "Denied read audit pending; request_id=%s",
                                    request_id,
                                    exc_info=True,
                                )
                                self.auditor.pending_results.append(row)
                await send(message)

            return await self.app(scope, receive, read_send)

        # Never execute a mutation whose initial audit fact cannot be persisted.
        start_event = event("START")
        try:
            await self.auditor.record(start_event)
        except Exception:
            logger.exception(
                "Business audit unavailable; operation not executed; request_id=%s", request_id
            )
            from starlette.responses import JSONResponse

            return await JSONResponse(
                {
                    "success": False,
                    "code": 503,
                    "message": "操作记录暂不可用，本次未执行，请稍后重试",
                    "result": None,
                },
                status_code=503,
            )(scope, receive, send)
        request_body, response_body = bytearray(), bytearray()
        status, finished = 500, False

        async def capture_receive():
            message = await receive()
            if message["type"] == "http.request":
                request_body.extend(
                    message.get("body", b"")[: max(0, LIMIT + 1 - len(request_body))]
                )
            return message

        async def capture_send(message):
            nonlocal status, finished
            if message["type"] == "http.response.start":
                status = message["status"]
                message = {
                    **message,
                    "headers": [
                        *message.get("headers", []),
                        (b"x-audit-operation-id", operation_id.encode()),
                    ],
                }
            if message["type"] == "http.response.body":
                response_body.extend(
                    message.get("body", b"")[: max(0, LIMIT + 1 - len(response_body))]
                )
                if not message.get("more_body", False):
                    outcome, refs, reason = operation_result(status, response_body)
                    row = event("ACCEPTED" if outcome == "ACCEPTED" else "RESULT", outcome)
                    row.update(
                        httpStatus=status,
                        businessRef=refs,
                        completionKind="HTTP",
                        message=reason,
                        resourceIds=safe_values(scope.get("path_params", {})),
                    )
                    if start_event.get("businessTaskIds"):
                        row["businessRef"]["taskIds"] = list(
                            dict.fromkeys(start_event["businessTaskIds"])
                        )[:100]
                    if start_event.get("expectedTasks"):
                        row["expectedTasks"] = start_event["expectedTasks"]
                    try:
                        body = (
                            json.loads(request_body)
                            if len(request_body) <= LIMIT and request_body
                            else {}
                        )
                    except (ValueError, UnicodeError):
                        body = {"content": "[文件或非JSON内容不记录]"}
                    row["submittedChanges"] = (
                        safe_values(body)
                        if len(request_body) <= LIMIT
                        else {"content": "[内容过长不记录]"}
                    )
                    # The actual result is durable before the response completes.
                    try:
                        await self.auditor.record(row)
                    except Exception:
                        # The START fact survives; never turn an executed
                        # mutation into a false failure inviting a duplicate.
                        logger.critical(
                            "Business audit result pending; operation_id=%s request_id=%s",
                            operation_id,
                            request_id,
                            exc_info=True,
                        )
                        pending = getattr(self.auditor, "pending_results", None)
                        if pending is not None:
                            pending.append(row)
                    finished = True
            await send(message)

        marker = current_operation.set(start_event)
        try:
            await self.app(scope, capture_receive, capture_send)
        except (Exception, asyncio.CancelledError):
            if not finished:
                row = event("RESULT", "UNKNOWN")
                row["message"] = "请求异常中断，需核查实际业务结果，不能视为未执行"
                try:
                    await asyncio.shield(self.auditor.record(row))
                except Exception:
                    logger.critical(
                        "Interrupted operation result pending; operation_id=%s",
                        operation_id,
                        exc_info=True,
                    )
            raise
        finally:
            current_operation.reset(marker)


def install_operation_audit(app, config):
    if getattr(app.state, "operation_audit", None) is not None:
        return app.state.operation_audit
    auditor = OperationAuditor(config)
    app.state.operation_audit = auditor
    previous = app.router.lifespan_context

    @asynccontextmanager
    async def lifespan(application):
        await auditor.start()
        try:
            async with previous(application) as state:
                yield state
        finally:
            await auditor.stop()

    app.router.lifespan_context = lifespan
    app.add_middleware(OperationAuditMiddleware, auditor=auditor)
    return auditor


async def record_denied_operation(app, request, identity, status=403):
    auditor = getattr(app.state, "operation_audit", None)
    if (
        not auditor
        or not auditor.enabled
        or (identity.get("userId") is None and identity.get("user_id") is None)
    ):
        return
    actor = audit_actor(identity)
    row = audit_event(
        "operation",
        userId=actor.get("userId") if actor.get("userId") is not None else identity.get("user_id"),
        account=actor.get("userName") or actor.get("username"),
        tenantId=actor.get("tenantId"),
        operation=request.method,
        target=request.url.path,
        ip=getattr(request.state, "source_ip", None)
        or source_ip(
            request.client.host if request.client else "", request.headers, auditor.proxies
        ),
        success=False,
        httpStatus=status,
        requestId=getattr(request.state, "request_id", None) or uuid.uuid4().hex,
        phase="RESULT",
        message="权限校验拒绝，本次未执行",
    )
    row.update(
        category="operation",
        operationId=uuid.uuid4().hex,
        operationStatus="FAILED",
        tenantScopeVerified=actor["tenantScopeVerified"],
        platformActor=actor["platformActor"],
        actorTenantId=actor.get("actorTenantId"),
        serviceId=auditor.service,
    )
    try:
        await auditor.record(row)
    except Exception:
        logger.critical(
            "Denied operation audit pending; request_id=%s", row["requestId"], exc_info=True
        )
        auditor.pending_results.append(row)
