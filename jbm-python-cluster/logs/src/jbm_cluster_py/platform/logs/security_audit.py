"""Security logs use the existing Loki service; business history belongs in TD."""
from __future__ import annotations

import asyncio
import hashlib
import hmac
import json
import os
import re
import tempfile
import time
from datetime import UTC, datetime
from pathlib import Path
from zoneinfo import ZoneInfo

from sqlalchemy import text
from fastapi import HTTPException
from jbm_cluster_py.common.api_access import is_platform, permission_codes, role_codes


def audit_reader_scope(actor, headers):
    """Identity comes from unified authentication, never from query parameters."""
    if is_platform(actor) or ("security_auditor" in role_codes(actor) and "security.audit.read" in permission_codes(actor)):
        return None
    own = str(actor.get("tenantId") or actor.get("companyId") or "")
    if "tenant_admin" not in role_codes(actor) or not own.isdigit() or int(own) <= 0:
        raise HTTPException(403, "仅授权的安全审计人员可查询")
    for name in ("tenantId", "X-Tenant-Id", "X-Resource-Tenant-Id"):
        if headers.get(name) and headers[name] != own:
            raise HTTPException(403, "只能查看本租户审计日志")
    return own


def audit_query_scope(body, scope):
    if scope is None:
        return dict(body)
    if body.get("tenantId") is not None and str(body["tenantId"]) != scope:
        raise HTTPException(403, "只能查看本租户审计日志")
    return {**body, "tenantId": scope, "tenantScopeVerified": True}


class SecurityAuditStore:
    def __init__(self, config, *, loki, outbox_engine=None):
        self.enabled = config.get("enabled", False) is True
        self.keep_days = int(config.get("keep-days", 365))
        if self.keep_days < 365:
            raise ValueError("安全日志保留期至少配置一年，覆盖六个自然月要求")
        self.secret = str(config.get("integrity-key") or "").encode()
        self.key_id = str(config.get("integrity-key-id") or "v1")
        if not re.fullmatch(r"[A-Za-z0-9_-]{1,24}", self.key_id):
            raise ValueError("审计密钥编号非法")
        self.keys = {str(k): str(v).encode() for k, v in (config.get("verification-keys") or {}).items()}
        self.keys[self.key_id] = self.secret
        self.backup = Path(config.get("backup-directory") or "./data/security-audit-backup")
        self.loki = loki
        self.base_url = loki.url.split("/loki/api/v1/")[0]
        self.tenant = str(config.get("loki-tenant") or "platform-security")
        self.lock = asyncio.Lock()
        self.outbox_engine = outbox_engine
        self.last_error = None

    async def start(self):
        if not self.enabled:
            return
        if len(self.secret) < 32:
            raise ValueError("审计完整性密钥必须至少32字节并通过秘密配置注入")
        if not self.loki.enabled:
            raise ValueError("安全日志需要启用现有Loki日志库")
        self.backup.mkdir(parents=True, exist_ok=True)
        await self.loki.start()

    def envelope(self, event):
        if not re.fullmatch(r"[a-f0-9]{32}", str(event.get("eventId") or "")):
            raise ValueError("审计事件编号无效")
        if not isinstance(event.get("timestamp"), int) or event["timestamp"] <= 0:
            raise ValueError("审计事件时间无效")
        payload = json.dumps(event, ensure_ascii=True, sort_keys=True, separators=(",", ":"))
        if len(payload.encode()) > 64000:
            raise ValueError("审计事件超出长度限制")
        signature = self.key_id + ":" + hmac.new(self.secret, payload.encode(), hashlib.sha256).hexdigest()
        return payload, signature

    def verify(self, payload, signature):
        key_id, _, digest = signature.partition(":")
        key = self.keys.get(key_id)
        expected = hmac.new(key or b"", payload.encode(), hashlib.sha256).hexdigest()
        if not key or not hmac.compare_digest(expected, digest):
            raise ValueError("安全审计完整性校验失败")

    def backup_path(self, event):
        return self.backup / datetime.fromtimestamp(event["timestamp"] / 1000, UTC).strftime("%Y%m%d") / (event["eventId"] + ".json")

    def backup_event(self, event, payload, signature):
        path = self.backup_path(event)
        path.parent.mkdir(parents=True, exist_ok=True)
        content = json.dumps({"payload": payload, "signature": signature}, sort_keys=True).encode()
        fd, temporary = tempfile.mkstemp(dir=path.parent, prefix=".pending-")
        try:
            with os.fdopen(fd, "wb") as stream:
                stream.write(content)
                stream.flush()
                os.fsync(stream.fileno())
            if os.name != "nt":
                os.chmod(temporary, 0o400)
            try:
                os.link(temporary, path)
            except FileExistsError:
                if path.read_bytes() != content:
                    raise ValueError("审计备份与已有事件不一致")
            if os.name != "nt":
                directory = os.open(path.parent, os.O_RDONLY)
                try:
                    os.fsync(directory)
                finally:
                    os.close(directory)
        finally:
            os.unlink(temporary)
        if os.name == "nt":
            os.chmod(path, 0o400)
        if path.read_bytes() != content:
            raise ValueError("审计备份回读校验失败")

    async def _range(self, selector, start, end, limit=100):
        response = await self.loki.client.get(self.base_url + "/loki/api/v1/query_range", params={"query": selector, "start": str(start), "end": str(end), "limit": limit, "direction": "backward"}, headers={"X-Scope-OrgID": self.tenant, "Cache-Control": "no-cache"})
        response.raise_for_status()
        data = response.json()
        if data.get("status") != "success":
            raise RuntimeError("日志库查询失败")
        values = [value for stream in data["data"]["result"] for value in stream["values"]]
        return sorted(values, key=lambda value: int(value[0]), reverse=True)

    async def _find(self, event):
        stamp = event["timestamp"] * 1000000
        selector = '{job="jbm-security-audit"} | json | eventId = ' + json.dumps(event["eventId"])
        rows = await self._range(selector, stamp, stamp + 999999, 10)
        return [json.loads(line) for _, line, *_ in rows]

    async def handle(self, event):
        if not self.enabled:
            raise RuntimeError("安全日志归档未启用")
        payload, signature = self.envelope(event)
        async with self.lock:
            try:
                path = self.backup_path(event)
                if path.exists():
                    previous = json.loads(path.read_text(encoding="utf-8"))
                    if previous["payload"] != payload:
                        raise ValueError("审计备份与事件冲突")
                    signature = previous["signature"]
                    self.verify(payload, signature)
                await asyncio.to_thread(self.backup_event, event, payload, signature)
                line = {**event, "integritySignature": signature}
                existing = await self._find(event)
                if existing and any(row != line for row in existing):
                    raise ValueError("审计事件冲突，禁止覆盖")
                if not existing:
                    # Two live sources give <=17,520 hourly streams per retained
                    # year. A resumed backlog stays within Loki's 1h ordering
                    # window even after newer events have already arrived.
                    period = datetime.fromtimestamp(event["timestamp"]/1000, UTC).strftime("%Y%m%d%H")
                    response = await self.loki.client.post(self.loki.url, json={"streams": [{"stream": {"job": "jbm-security-audit", "source": str(event.get("source") or "unknown")[:128], "period": period}, "values": [[str(event["timestamp"] * 1000000), json.dumps(line, ensure_ascii=False, separators=(",", ":"))]]}]}, headers={"X-Scope-OrgID": self.tenant})
                    response.raise_for_status()
                    if await self._find(event) != [line]:
                        raise ValueError("审计日志库回读校验失败")
                if self.outbox_engine is not None:
                    async with self.outbox_engine.begin() as conn:
                        await conn.execute(text("DELETE FROM security_audit_outbox WHERE event_id=:id AND payload=:payload"), {"id": event["eventId"], "payload": json.dumps(event, ensure_ascii=False, separators=(",", ":"))})
                self.last_error = None
            except Exception:
                self.last_error = "安全日志归档或完整性校验失败，消息保留重试"
                raise

    async def query(self, body):
        def timestamp(raw, default):
            if not raw:
                return default
            if isinstance(raw, (int, float)):
                return int(raw) * 1000000
            parsed = datetime.fromisoformat(str(raw))
            if parsed.tzinfo is None:
                parsed = parsed.replace(tzinfo=ZoneInfo("Asia/Shanghai"))
            return int(parsed.timestamp() * 1000000000)
        end = timestamp(body.get("endTime"), time.time_ns())
        start = timestamp(body.get("beginTime"), end - 86400 * 1000000000)
        if end < start or end - start > 31 * 86400 * 1000000000:
            raise ValueError("每次请选择不超过31天的查询区间，历史日志可按日期分段查询")
        page = max(1, int(body.get("page", 1)))
        size = max(1, min(100, int(body.get("size", 20))))
        if page * size > 4900:
            raise ValueError("结果较多，请缩小时间或账号范围后查询")
        selector = '{job="jbm-security-audit"} | json'
        for name in ("userId", "account", "tenantId", "operation", "category", "originalId", "httpStatus", "ip", "method", "serviceId"):
            if body.get(name):
                selector += " | " + name + " = " + json.dumps(str(body[name]))
        if body.get("path"):
            selector += " | target =~ " + json.dumps(".*" + re.escape(str(body["path"])) + ".*")
        for name in ("account", "ip"):
            if body.get(name + "Contains"):
                selector += " | " + name + " =~ " + json.dumps(".*" + re.escape(str(body[name + "Contains"])) + ".*")
        if body.get("operationOnly"):
            selector += ' | userId != "" | operation != "READ"'
        if body.get("success") is not None:
            selector += " | success = " + json.dumps(str(bool(body["success"])).lower())
        if body.get("tenantScopeVerified") is True:
            selector += ' | tenantScopeVerified = "true"'
        rows = await self._range(selector, start, end, page * size + 1)
        result = []
        for _, line, *_ in rows[(page-1)*size:page*size]:
            event = json.loads(line)
            signature = event.pop("integritySignature")
            self.verify(json.dumps(event, ensure_ascii=True, sort_keys=True, separators=(",", ":")), signature)
            result.append({**event, "integrityVerified": True})
        # Loki pagination is bounded; count an exact result separately, never present
        # a sampled row count as the total.
        duration = max(1, (end-start)//1000000000 + 1)
        response = await self.loki.client.get(self.base_url + "/loki/api/v1/query", params={"query": f"sum(count_over_time(({selector})[{duration}s]))", "time": str(end)}, headers={"X-Scope-OrgID": self.tenant})
        response.raise_for_status()
        counts = response.json()
        if counts.get("status") != "success":
            raise RuntimeError("日志数量查询失败")
        total = sum(int(float(row["value"][1])) for row in counts["data"]["result"])
        return {"records": result, "total": total, "keepDays": self.keep_days}

    async def handle_many(self, events):
        """Bulk historical import; verify the same facts with bounded LogQL reads."""
        if not self.enabled or not 1 <= len(events) <= 100:
            raise ValueError("日志批次必须为1～100条并启用日志归档")
        groups, backups = {}, []
        for event in events:
            payload, signature = self.envelope(event)
            path = self.backup_path(event)
            if path.exists():
                previous = json.loads(path.read_text(encoding="utf8"))
                if previous["payload"] != payload:
                    raise ValueError("审计备份与事件冲突")
                signature = previous["signature"]
                self.verify(payload, signature)
            backups.append((event, payload, signature))
            # Bound each verification request below Loki's normal 30d1h limit.
            key = (str(event["source"]), event["timestamp"] // (30*86400000))
            groups.setdefault(key, []).append({**event, "integritySignature": signature})
        # One bounded batch shares filesystem commits; every file is still
        # fsynced and read back before any original SQL row may be removed.
        await asyncio.gather(*(asyncio.to_thread(self.backup_event, *args) for args in backups))
        for (source, _), lines in groups.items():
            expected = {line["eventId"]: line for line in lines}
            selector = '{job="jbm-security-audit",source=' + json.dumps(source) + '} | json | eventId =~ ' + json.dumps("|".join(expected))
            start = min(line["timestamp"] for line in lines)*1000000-1000000
            end = max(line["timestamp"] for line in lines)*1000000+1000000
            async def read():
                result = {}
                for _, raw, *_ in await self._range(selector, start, end, len(lines)*2+1):
                    line = json.loads(raw)
                    if line["eventId"] in result:
                        raise ValueError("日志库存在重复事实，停止清理原数据")
                    result[line["eventId"]] = line
                return result
            existing = await read()
            if any(expected.get(key) != value for key, value in existing.items()):
                raise ValueError("日志库已有事实不一致，禁止覆盖")
            streams = {}
            for key, line in expected.items():
                if key in existing:
                    continue
                period = datetime.fromtimestamp(line["timestamp"]/1000, UTC).strftime("%Y%m%d%H")
                streams.setdefault(period, []).append([str(line["timestamp"]*1000000), json.dumps(line, ensure_ascii=False, separators=(",", ":"))])
            if streams:
                response = await self.loki.client.post(self.loki.url, json={"streams": [{"stream": {"job": "jbm-security-audit", "source": source, "period": period}, "values": sorted(values, key=lambda row: int(row[0]))} for period, values in streams.items()]}, headers={"X-Scope-OrgID": self.tenant})
                response.raise_for_status()
                if await read() != expected:
                    raise ValueError("批量日志回读校验未通过，保留原数据")

    async def gateway_page(self, body, operation_only=False):
        source = body.get("gatewayLogs") or body
        page = body.get("pageForm") or {}
        query = {"category": "access", "page": page.get("currPage", 1), "size": page.get("pageSize", 20),
                 "operationOnly": operation_only, "path": source.get("path"), "accountContains": source.get("requestRealName"),
                 "ipContains": source.get("ip"), "httpStatus": source.get("status") or source.get("httpStatus"),
                 "method": source.get("method"), "serviceId": source.get("serviceId"),
                 "beginTime": body.get("beginTime"), "endTime": body.get("endTime")}
        result = await self.query({k: v for k, v in query.items() if v is not None})
        return {"contents": [row["gateway"] for row in result["records"]], "total": result["total"]}

    async def login_page(self, body):
        source = body.get("baseAccountLogs") or {}
        page = body.get("pageForm") or {}
        query = {"category": "login", "page": page.get("currPage", 1), "size": page.get("pageSize", 20),
                 "accountContains": source.get("account"), "ipContains": source.get("loginIp"),
                 "operation": source.get("accountType"), "beginTime": source.get("beginTime"), "endTime": source.get("endTime")}
        if source.get("loginStatus") not in (None, ""):
            query["success"] = source["loginStatus"] in (True, 1, "true", "1")
        result = await self.query({k: v for k, v in query.items() if v is not None})
        rows = []
        for row in result["records"]:
            rows.append({**row, "id": row.get("originalId") or row["eventId"], "loginTime": datetime.fromtimestamp(row["timestamp"]/1000, UTC).isoformat(),
                "loginType": row.get("loginType") or row.get("operation"), "accountType": row.get("loginType") or row.get("operation"),
                "loginStatus": row.get("success"), "loginIp": row.get("ip"), "browser": row.get("userAgent"), "loginLocation": row.get("loginLocation"),
                "riskLevel": row.get("riskLevel"), "requestId": row.get("requestId"), "clientId": row.get("clientId")})
        return {"contents": rows, "total": result["total"]}

    async def stop(self):
        pass  # The existing LokiSink owns the shared HTTP client.

    async def health(self, tenant=None):
        await self._range('{job="jbm-security-audit"}', time.time_ns()-1000000000, time.time_ns(), 1)
        pending, oldest = 0, None
        if self.outbox_engine is not None:
            async with self.outbox_engine.connect() as conn:
                where, params = "", {}
                if tenant is not None:
                    expr = "JSON_EXTRACT(payload,'$.tenantId')"
                    if self.outbox_engine.dialect.name == "mysql":
                        expr = "JSON_UNQUOTE(" + expr + ")"
                    where, params = " WHERE " + expr + "=:tenant AND JSON_EXTRACT(payload,'$.tenantScopeVerified')=true", {"tenant": tenant}
                row = (await conn.execute(text("SELECT COUNT(*), MIN(CAST(JSON_EXTRACT(payload,'$.timestamp') AS SIGNED)) FROM security_audit_outbox" + where), params)).one()
                pending, oldest = int(row[0]), row[1]
        lag = max(0, int(time.time()*1000)-int(oldest)) if oldest is not None else 0
        return {"enabled": self.enabled, "storage": "Loki", "keepDays": self.keep_days, "appendOnly": True,
                "tenantScoped": tenant is not None, "tenantId": tenant,
                "backupEnabled": True, "pendingEvents": pending, "oldestPendingMs": lag,
                "healthy": not self.last_error and lag < 30000, "error": self.last_error}
