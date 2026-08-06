from __future__ import annotations

import json
import logging
import re
import time
from collections.abc import Mapping
from datetime import datetime
from typing import Any

import httpx

logger = logging.getLogger(__name__)


class LokiSink:
    def __init__(self, config: Mapping[str, Any]) -> None:
        self.config = dict(config)
        self.enabled = bool(self.config.get("enabled"))
        self.url = str(
            self.config.get("url") or "http://loki:3100/loki/api/v1/push"
        )
        self.timeout = float(self.config.get("timeout-seconds") or 5)
        self.default_tenant = str(self.config.get("tenant-id") or "platform")
        self.client: httpx.AsyncClient | None = None

    async def start(self) -> None:
        if self.enabled and self.client is None:
            self.client = httpx.AsyncClient(timeout=self.timeout, trust_env=False)

    async def stop(self) -> None:
        if self.client is not None:
            await self.client.aclose()
            self.client = None

    async def send_gateway(self, row: Mapping[str, Any]) -> None:
        if not self.enabled:
            return
        if self.client is None:
            await self.start()
        tenant = str(row.get("tenantId") or row.get("tenant_id") or self.default_tenant)
        response = await self.client.post(
            self.url,
            json=self.gateway_payload(row),
            headers={"X-Scope-OrgID": tenant},
        )
        response.raise_for_status()

    @staticmethod
    def gateway_payload(row: Mapping[str, Any]) -> dict[str, Any]:
        status = int(row.get("httpStatus") or row.get("status") or 0)
        level = "error" if status >= 500 else "warn" if status >= 400 else "info"
        service = _label(str(row.get("serviceId") or "jbm-gateway"))
        line = json.dumps(dict(row), ensure_ascii=False, separators=(",", ":"), default=str)
        metadata = {
            key: str(value)
            for key, value in {
                "access_id": row.get("accessId"),
                "trace_id": row.get("traceId"),
                "tenant_id": row.get("tenantId") or row.get("tenant_id"),
            }.items()
            if value not in (None, "")
        }
        value: list[Any] = [_timestamp_ns(row.get("requestTime")), line]
        if metadata:
            value.append(metadata)
        return {
            "streams": [
                {
                    "stream": {
                        "job": "jbm-gateway",
                        "service": service,
                        "level": level,
                    },
                    "values": [value],
                }
            ]
        }


def _label(value: str) -> str:
    return re.sub(r"[^a-zA-Z0-9_.:-]", "_", value)[:128] or "unknown"


def _timestamp_ns(value: Any) -> str:
    if value:
        try:
            parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
            return str(int(parsed.timestamp() * 1_000_000_000))
        except ValueError:
            logger.warning("Invalid Loki event timestamp %r; using ingestion time", value)
    return str(time.time_ns())
