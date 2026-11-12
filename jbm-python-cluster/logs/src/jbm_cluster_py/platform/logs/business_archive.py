"""Loki is the source of truth for business-log lines."""

from __future__ import annotations

import hashlib
import json
import time
from collections.abc import Mapping
from datetime import UTC, datetime
from typing import Any

from jbm_cluster_py.platform.logs.loki import LokiSink


class BusinessLineArchive:
    def __init__(self, loki: LokiSink, tenant: str) -> None:
        self.loki = loki
        self.tenant = tenant
        self.base_url = loki.url.removesuffix("/loki/api/v1/push")

    @property
    def headers(self):
        return {"X-Scope-OrgID": self.tenant, "Cache-Control": "no-cache"}

    @staticmethod
    def _generation(summary: Mapping[str, Any]) -> str:
        raw = json.dumps([str(summary["logId"]), str(summary["createTime"])], separators=(",", ":"))
        return hashlib.sha256(raw.encode()).hexdigest()

    @staticmethod
    def _ns(iso: str) -> int:
        stamp = datetime.fromisoformat(iso.replace("Z", "+00:00"))
        if stamp.tzinfo is None:
            stamp = stamp.replace(tzinfo=UTC)
        return int(stamp.timestamp()) * 1_000_000_000 + stamp.microsecond * 1000

    async def _client(self):
        if not self.loki.enabled:
            raise RuntimeError("业务日志归档未启用")
        await self.loki.start()
        return self.loki.client

    async def read(self, summary, start=1, end=-1, *, event_key=None):
        generation = self._generation(summary)
        selector = (
            '{job="jbm-business-log",shard="' + generation[:1] + '"}'
            ' | json generation="generation", lineNumber="lineNumber", eventKey="eventKey"'
            ' | __error__="" | generation="' + generation + '"'
            f" | lineNumber >= {max(1, int(start))}"
        )
        if end >= 0:
            selector += f" | lineNumber <= {int(end)}"
        if event_key:
            selector += " | eventKey = " + json.dumps(event_key)
        client = await self._client()
        start_ns = self._ns(str(summary["createTime"])) - 1_000_000
        end_ns = time.time_ns() + 1_000_000_000
        found = {}
        while start_ns <= end_ns:
            window_end = min(end_ns, start_ns + 7 * 86_400_000_000_000)
            response = await client.get(
                self.base_url + "/loki/api/v1/query_range",
                params={"query": selector, "start": str(start_ns), "end": str(window_end),
                        "limit": 1000, "direction": "forward"},
                headers=self.headers,
            )
            response.raise_for_status()
            result = response.json()
            if result.get("status") != "success":
                raise RuntimeError("业务日志查询失败")
            values = [value for stream in result["data"]["result"] for value in stream["values"]]
            for stamp, body, *_ in values:
                row = json.loads(body)
                number = int(row["lineNumber"])
                if (
                    row["generation"] != generation or str(row["row"]["log_id"]) != str(summary["logId"])
                    or int(row["row"]["line_number"]) != number or number < start
                    or (end >= 0 and number > end) or (event_key and row["eventKey"] != event_key)
                ):
                    raise ValueError("业务日志库返回了不属于查询范围的记录")
                previous = found.get(number)
                if previous is not None and previous != row:
                    raise ValueError("业务日志行号存在冲突")
                found[number] = row
            if len(values) < 1000:
                start_ns = window_end + 1
            else:
                # Lines within a generation have distinct Loki timestamps.
                start_ns = max(int(value[0]) for value in values) + 1
        return [found[number] for number in sorted(found)]

    async def write_verified(self, summary, rows, event_key):
        if not rows:
            return
        generation = self._generation(summary)
        bodies = [{"generation": generation, "lineNumber": int(row["line_number"]),
                   "eventKey": event_key, "row": row} for row in rows]
        start, end = bodies[0]["lineNumber"], bodies[-1]["lineNumber"]
        present = await self.read(summary, start, end)
        if present == bodies:
            return
        if present:
            raise ValueError("业务日志行号已存在且原文不同")
        # Store the original create_time inside the row. Ingestion time gives
        # old SQL records a valid Loki timestamp without changing their history.
        base = time.time_ns()
        period = datetime.fromtimestamp(base // 1_000_000_000, UTC).strftime("%Y%m%d%H")
        values = [[str(base + i), json.dumps(row, ensure_ascii=False, separators=(",", ":"), default=str)]
                  for i, row in enumerate(bodies)]
        if any(len(value[1].encode()) > 250_000 for value in values):
            raise ValueError("单行日志超过 Loki 长度限制")
        client = await self._client()
        response = await client.post(
            self.loki.url,
            json={"streams": [{"stream": {"job": "jbm-business-log", "shard": generation[:1], "period": period},
                               "values": values}]},
            headers=self.headers,
        )
        response.raise_for_status()
        if await self.read(summary, start, end) != bodies:
            raise ValueError("业务日志库回读与原文不一致")
