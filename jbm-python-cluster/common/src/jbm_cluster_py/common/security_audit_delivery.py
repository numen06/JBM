"""Durable delivery of login and gateway security audit to the log service."""

import asyncio
import json

from sqlalchemy import inspect

from .operation_audit import OperationAuditor
from .security_audit import AUDIT_TOPIC, OPERATION_ACK_PREFIX, AuditOutbox, audit_digest


class SecurityAuditPublisher(OperationAuditor):
    def __init__(self, config, *, source, legacy_database=None):
        super().__init__(config, source_role=f"{source}:security-audit")
        self.legacy = (
            AuditOutbox({"enabled": self.enabled}, self.kafka, legacy_database)
            if legacy_database is not None and self.enabled else None
        )

    async def start(self):
        if not self.enabled:
            return
        self.prepare()
        if self.legacy:
            async with self.legacy.engine.connect() as conn:
                exists = await conn.run_sync(lambda sync: inspect(sync).has_table("security_audit_outbox"))
            if exists:
                await self.legacy.start(background=False)
            else:
                await self.legacy.stop()
                self.legacy = None
        self.task = asyncio.create_task(self._flush_loop())

    async def record(self, event):
        if self.enabled:
            try:
                await asyncio.to_thread(self._write, event)
                self.last_spool_error = False
            except OSError:
                self.last_spool_error = True
                raise

    async def flush(self):
        if self.receipts.client is None:
            await self.receipts.start()
        submitted = self.directory / "submitted"
        sent = 0
        for path in sorted(self.directory.glob("*.json")):
            try:
                event = json.loads(await asyncio.to_thread(path.read_text, encoding="utf-8"))
            except FileNotFoundError:
                continue
            marker = submitted / path.name
            receipt = await self.receipts.client.get(OPERATION_ACK_PREFIX + event["eventId"])
            if receipt == audit_digest(event):
                await asyncio.to_thread(marker.unlink, missing_ok=True)
                await asyncio.to_thread(path.unlink, missing_ok=True)
            elif not marker.exists() and sent < 100:
                await asyncio.wait_for(
                    self.kafka.publish_json(AUDIT_TOPIC, event, key=event["eventId"]), timeout=10
                )
                # Retain both the source and the broker receipt across restarts.
                await asyncio.to_thread(self._write, event, directory=submitted)
                sent += 1
        if self.legacy:
            await self.legacy.flush()

    async def stop(self):
        if self.legacy:
            await self.legacy.stop()
        await super().stop()
