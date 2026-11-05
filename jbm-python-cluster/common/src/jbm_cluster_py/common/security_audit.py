"""Durable transport outbox. Only explicitly selected audit facts may enter it."""
from __future__ import annotations

import asyncio
import json
import logging
import time
import uuid
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine
from jbm_cluster_py.integrations.database import configured_database_url, require_tables

logger = logging.getLogger(__name__)
AUDIT_TOPIC = "jbm.security.audit.v1"


def login_succeeded(value):
    # MySQL BIT is returned as bytes by asyncmy; TINYINT as int.
    if isinstance(value, (bytes, bytearray, memoryview)):
        return int.from_bytes(value, "big") == 1
    if value is None:
        return None
    try:
        return int(value) == 1
    except (TypeError, ValueError):
        return None


def audit_event(source, **facts):
    allowed = {"userId", "account", "tenantId", "appId", "clientId", "target", "operation", "ip", "success", "httpStatus", "requestId", "phase", "message"}
    event = {k: v if isinstance(v, (bool, int, float)) or v is None else str(v)[:1024] for k, v in facts.items() if k in allowed}
    event.update(eventId=uuid.uuid4().hex, timestamp=time.time_ns() // 1000000, source=source)
    return event


class AuditOutbox:
    def __init__(self, config, kafka, database):
        self.enabled = config.get("enabled", False) is True
        self.url = configured_database_url(database) or "sqlite+aiosqlite:///:memory:"
        self.engine = create_async_engine(self.url, pool_pre_ping=True)
        self.kafka = kafka
        self.task = None
        self.last_error = None

    async def start(self):
        if not self.enabled:
            return
        if not self.kafka.enabled:
            raise ValueError("安全审计需要启用持久消息通道")
        if self.url.startswith("sqlite"):
            async with self.engine.begin() as conn:
                await conn.execute(text("CREATE TABLE IF NOT EXISTS security_audit_outbox (event_id VARCHAR(64) PRIMARY KEY, payload TEXT NOT NULL, published_at BIGINT)"))
        else:
            await require_tables(self.engine, ("security_audit_outbox",))
        self.task = asyncio.create_task(self._run())

    async def record(self, event):
        if not self.enabled:
            return
        insert = "INSERT OR IGNORE" if self.url.startswith("sqlite") else "INSERT IGNORE"
        async with self.engine.begin() as conn:
            await conn.execute(text(insert + " INTO security_audit_outbox (event_id,payload) VALUES (:id,:payload)"), {"id": event["eventId"], "payload": json.dumps(event, ensure_ascii=False, separators=(",", ":"))})

    async def flush(self):
        async with self.engine.connect() as conn:
            rows = (await conn.execute(text("SELECT event_id,payload FROM security_audit_outbox WHERE published_at IS NULL OR published_at<:retry ORDER BY event_id LIMIT 100"), {"retry": int(time.time()) - 30})).all()
        for event_id, payload in rows:
            await self.kafka.publish_json(AUDIT_TOPIC, json.loads(payload), key=event_id)
            async with self.engine.begin() as conn:
                await conn.execute(text("UPDATE security_audit_outbox SET published_at=:now WHERE event_id=:id"), {"id": event_id, "now": int(time.time())})
        self.last_error = None

    async def _run(self):
        while True:
            try:
                await self.flush()
            except asyncio.CancelledError:
                raise
            except Exception:
                self.last_error = "安全审计消息尚未归档，将自动重试"
                logger.exception("Security audit outbox delivery pending")
            await asyncio.sleep(2)

    async def stop(self):
        if self.task:
            self.task.cancel()
            await asyncio.gather(self.task, return_exceptions=True)
        await self.engine.dispose()
