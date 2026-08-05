from __future__ import annotations

import hashlib
import hmac
import time
from collections.abc import Mapping
from datetime import UTC, datetime
from typing import Any

from jbm_cluster_py.platform.logs.repository import LogsRepository


class BusinessLogService:
    def __init__(self, repository: LogsRepository, signing_secret: str) -> None:
        self.repository = repository
        self.signing_secret = signing_secret.encode("utf-8")

    async def create(self, body: Mapping[str, Any]) -> dict[str, Any]:
        log = await self.repository.create_business_log(body)
        stages = body.get("stages")
        if isinstance(stages, list) and stages:
            await self.init_stages({"logId": log["logId"], "stages": stages})
        return {"logId": log["logId"]}

    async def append(
        self, log_id: str, content: str, body: Mapping[str, Any] | None = None
    ) -> bool:
        if not log_id.strip() or not content:
            raise ValueError("logId和日志内容不能为空")
        await self.repository.append_business_log(log_id, content, body or {}, True)
        return True

    async def lines(self, log_id: str, start: int = 1, end: int = -1) -> list[dict[str, Any]]:
        return await self.repository.business_lines(log_id, start, end)

    async def content(self, log_id: str, formatted: bool = True) -> str:
        summary = await self.repository.business_summary(log_id)
        if not summary:
            raise ValueError("业务日志不存在")
        lines = await self.lines(log_id)
        raw = "\n".join(str(line.get("content") or "") for line in lines)
        if not formatted:
            return raw
        header = [
            f"Log ID: {log_id}",
            f"Module: {summary.get('module') or '-'}",
            f"Operation: {summary.get('operation') or '-'}",
            f"Created: {summary.get('createTime') or '-'}",
            "-" * 60,
        ]
        body = [
            f"{int(line.get('lineNumber') or 0):06d}  {line.get('content') or ''}" for line in lines
        ]
        return "\n".join(header + body)

    async def init_stages(self, body: Mapping[str, Any]) -> dict[str, Any]:
        log_id = str(body.get("logId") or "").strip()
        stages = body.get("stages")
        if not log_id or not isinstance(stages, list) or not stages:
            raise ValueError("logId不能为空且至少需要一个阶段")
        normalized = []
        for index, source in enumerate(stages, 1):
            item = dict(source or {})
            normalized.append(
                {
                    "stageCode": str(item.get("stageCode") or f"stage-{index}"),
                    "stageName": str(item.get("stageName") or f"阶段 {index}"),
                    "orderIndex": int(item.get("orderIndex") or index),
                    "progress": max(0, min(int(item.get("progress") or 0), 100)),
                    "status": str(item.get("status") or "WAITING").upper(),
                    "message": item.get("message"),
                    "updateTime": datetime.now(UTC).isoformat(),
                }
            )
        normalized.sort(key=lambda item: item["orderIndex"])
        return await self.repository.save_stage(
            {
                "logId": log_id,
                "stageCount": len(normalized),
                "overallProgress": 0,
                "overallStatus": "WAITING",
                "activeStageIndex": 1,
                "activeStageName": normalized[0]["stageName"],
                "stages": normalized,
                "version": 0,
            }
        )

    async def update_stage(self, body: Mapping[str, Any]) -> dict[str, Any]:
        log_id = str(body.get("logId") or "").strip()
        snapshot = await self.repository.stage(log_id)
        if not snapshot:
            raise ValueError("阶段配置不存在")
        stages = list(snapshot.get("stages") or [])
        stage_code, stage_index = body.get("stageCode"), body.get("stageIndex")
        target = next(
            (item for item in stages if stage_code and item.get("stageCode") == stage_code), None
        )
        if target is None and stage_index:
            target = next(
                (item for item in stages if int(item.get("orderIndex") or 0) == int(stage_index)),
                None,
            )
        if target is None:
            raise ValueError("阶段不存在")
        if body.get("stageName") is not None:
            target["stageName"] = body["stageName"]
        if body.get("progress") is not None:
            target["progress"] = max(0, min(int(body["progress"]), 100))
        if body.get("status") is not None:
            target["status"] = str(body["status"]).upper()
        if body.get("message") is not None:
            target["message"] = body["message"]
        target["updateTime"] = datetime.now(UTC).isoformat()
        statuses = [str(item.get("status") or "WAITING") for item in stages]
        overall = (
            int(body.get("overallProgress"))
            if body.get("overallProgress") is not None
            else round(sum(int(item.get("progress") or 0) for item in stages) / len(stages))
        )
        overall_status = (
            "FAILED"
            if "FAILED" in statuses
            else "DONE"
            if all(value == "DONE" for value in statuses)
            else "RUNNING"
            if any(value in {"RUNNING", "DONE"} for value in statuses)
            else "WAITING"
        )
        active = next(
            (item for item in stages if item.get("status") in {"RUNNING", "FAILED"}), target
        )
        snapshot.update(
            {
                "overallProgress": max(0, min(overall, 100)),
                "overallStatus": overall_status,
                "activeStageIndex": active.get("orderIndex"),
                "activeStageName": active.get("stageName"),
                "stages": stages,
            }
        )
        saved = await self.repository.save_stage(snapshot)
        if body.get("appendLog", True):
            content = str(
                body.get("content")
                or f"[{target.get('stageName')}] {target.get('status')} {target.get('progress')}%"
            )
            await self.repository.append_business_log(
                log_id,
                content,
                {
                    "autoTimestamp": body.get("autoTimestamp", True),
                    "stageCode": target.get("stageCode"),
                    "stageName": target.get("stageName"),
                    "stageIndex": target.get("orderIndex"),
                    "stageProgress": target.get("progress"),
                    "stageStatus": target.get("status"),
                    "stageCount": len(stages),
                    "overallProgress": saved.get("overallProgress"),
                    "stageEvent": True,
                },
            )
        return saved

    async def demo(self, mode: str = "multi-stage") -> dict[str, str]:
        created = await self.create(
            {
                "module": "DEMO",
                "operation": f"DEMO_{mode.upper()}",
                "username": "demo",
                "userId": "demo",
                "autoTimestamp": True,
            }
        )
        log_id = created["logId"]
        if mode != "simple":
            names = ["准备", "执行", "完成"] if mode == "multi-stage" else ["执行"]
            await self.init_stages(
                {
                    "logId": log_id,
                    "stages": [
                        {"stageCode": f"stage-{i}", "stageName": name, "orderIndex": i}
                        for i, name in enumerate(names, 1)
                    ],
                }
            )
        await self.append(log_id, "演示任务已创建", {"autoTimestamp": True})
        return {"logId": log_id, "module": "DEMO", "mode": mode}

    def temporary_params(self, log_id: str, expire_minutes: int) -> dict[str, str]:
        expires = int(time.time()) + max(int(expire_minutes), 1) * 60
        signature = self._signature(log_id, expires)
        return {
            "fileName": f"raw-{log_id}.log",
            "expires": str(expires),
            "accessKeyId": "jbm-logs",
            "signature": signature,
        }

    def token(self, log_id: str, expire_minutes: int = 60) -> str:
        expires = int(time.time()) + max(int(expire_minutes), 1) * 60
        return f"{expires}.{self._signature(log_id, expires)}"

    def verify(self, log_id: str, expires: int, signature: str) -> None:
        if expires < int(time.time()):
            raise ValueError("URL已过期")
        if not hmac.compare_digest(self._signature(log_id, expires), signature or ""):
            raise ValueError("签名无效")

    def verify_token(self, log_id: str, token: str) -> None:
        try:
            expires, signature = token.split(".", 1)
            self.verify(log_id, int(expires), signature)
        except (ValueError, AttributeError) as exc:
            raise ValueError("临时访问token无效") from exc

    def _signature(self, log_id: str, expires: int) -> str:
        return hmac.new(
            self.signing_secret, f"{log_id}:{expires}".encode(), hashlib.sha256
        ).hexdigest()

    async def handle_event(self, event: Mapping[str, Any]) -> None:
        event_type = str(event.get("eventType") or "").upper()
        log_id = str(event.get("logId") or "")
        if event_type == "CREATE":
            await self.create(event)
        elif event_type == "APPEND":
            if not log_id and event.get("businessType") and event.get("businessId"):
                log_id = (
                    await self.repository.business_id_log(
                        str(event["businessType"]), str(event["businessId"])
                    )
                    or ""
                )
            await self.append(log_id, str(event.get("content") or ""), event)
        elif event_type == "DELETE":
            await self.repository.delete_business_log(log_id)
        elif event_type == "UPDATE_EXPIRE":
            await self.repository.update_business_expiry(log_id, int(event.get("expireDays") or 30))
        elif event_type == "STAGE_INIT":
            stages = []
            for index, raw in enumerate(str(event.get("content") or "").split(";"), 1):
                parts = [part.strip() for part in raw.split(",", 2)]
                if len(parts) >= 2:
                    stages.append(
                        {
                            "stageCode": parts[0],
                            "stageName": parts[1],
                            "orderIndex": int(parts[2]) if len(parts) > 2 else index,
                        }
                    )
            await self.init_stages({"logId": log_id, "stages": stages})
        elif event_type == "STAGE_UPDATE":
            parts = [part.strip() for part in str(event.get("content") or "").split(",", 4)]
            if len(parts) >= 3:
                await self.update_stage(
                    {
                        "logId": log_id,
                        "stageCode": parts[0],
                        "status": parts[1],
                        "progress": int(parts[2]),
                        "message": parts[3] if len(parts) > 3 else "",
                        "overallProgress": int(parts[4]) if len(parts) > 4 and parts[4] else None,
                        "appendLog": False,
                    }
                )
