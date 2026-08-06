from __future__ import annotations

import asyncio
import json
from collections.abc import AsyncIterator
from typing import Any
from urllib.parse import quote

from fastapi import APIRouter, Body, Query, Request
from fastapi.responses import PlainTextResponse, StreamingResponse
from jbm_cluster_py.common.result import ok
from jbm_cluster_py.platform.logs.repository import LogsRepository
from jbm_cluster_py.platform.logs.service import BusinessLogService, GatewayLogIngestService


def build_logs_router(
    repository: LogsRepository,
    service: BusinessLogService,
    gateway_ingest: GatewayLogIngestService,
) -> APIRouter:
    router = APIRouter()

    @router.post("/GatewayLogs/ingest", tags=["网关日志"])
    async def ingest_gateway_log(body: dict[str, Any]) -> dict[str, Any]:
        await gateway_ingest.handle(body)
        return ok(True, "采集网关日志成功")

    @router.post("/GatewayLogs/findLogs", tags=["网关日志"])
    async def find_gateway_logs(
        body: dict[str, Any] | None = Body(default=None),
    ) -> dict[str, Any]:
        return ok(await repository.page_gateway_logs(body or {}), "查询分页列表成功")

    @router.post("/GatewayLogs/findOperationLogs", tags=["网关日志"])
    async def find_operation_logs(
        body: dict[str, Any] | None = Body(default=None),
    ) -> dict[str, Any]:
        return ok(
            await repository.page_gateway_logs(body or {}, True),
            "查询分页列表成功",
        )

    @router.post("/GatewayLogs/getByAccessId", tags=["网关日志"])
    async def get_gateway_log(body: dict[str, Any]) -> dict[str, Any]:
        return ok(
            await repository.gateway_log(str(body.get("accessId") or "")),
            "查询日志成功",
        )

    @router.get("/GatewayLogs/filterRules", tags=["网关日志"])
    async def list_filter_rules() -> dict[str, Any]:
        return ok(await repository.list_rules(), "查询过滤规则成功")

    @router.post("/GatewayLogs/filterRules", tags=["网关日志"])
    async def create_filter_rule(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await repository.save_rule(body), "保存过滤规则成功")

    @router.put("/GatewayLogs/filterRules/{rule_id}", tags=["网关日志"])
    async def update_filter_rule(rule_id: str, body: dict[str, Any]) -> dict[str, Any]:
        return ok(await repository.save_rule(body, rule_id), "保存过滤规则成功")

    @router.delete("/GatewayLogs/filterRules/{rule_id}", tags=["网关日志"])
    async def delete_filter_rule(rule_id: str) -> dict[str, Any]:
        return ok(await repository.delete_rule(rule_id), "删除过滤规则成功")

    @router.post("/GatewayLogs/filterRules/{rule_id}/toggle", tags=["网关日志"])
    async def toggle_filter_rule(
        rule_id: str, body: dict[str, Any]
    ) -> dict[str, Any]:
        current = await repository.rule(rule_id)
        if not current:
            raise ValueError("过滤规则不存在")
        return ok(
            await repository.save_rule(
                {**current, "enabled": bool(body.get("enabled"))}, rule_id
            ),
            "切换过滤规则成功",
        )

    @router.post("/GatewayLogs/filterRules/test", tags=["网关日志"])
    async def test_filter_rule(body: dict[str, Any]) -> dict[str, Any]:
        rules = await repository.matching_rules(
            {
                "path": body.get("path"),
                "method": body.get("method"),
                "serviceId": body.get("serviceId"),
                "httpStatus": body.get("statusCode"),
            }
        )
        return ok({"matched": bool(rules), "rules": rules}, "测试过滤规则成功")

    @router.post("/clusterAccess/getClusterAccessInfo", tags=["网关日志"])
    async def cluster_access() -> dict[str, Any]:
        return ok(await repository.cluster_access(), "查询访问统计成功")

    @router.post("/businessLog/create", tags=["业务日志"])
    async def create(body: dict[str, Any] = Body(default_factory=dict)) -> dict[str, Any]:
        return ok(await service.create(body), "创建业务日志成功")

    @router.post("/businessLog/append", tags=["业务日志"])
    async def append(body: dict[str, Any]) -> dict[str, Any]:
        return ok(
            await service.append(
                str(body.get("logId") or ""), str(body.get("content") or ""), body
            ),
            "追加日志成功",
        )

    @router.post("/businessLog/append/{log_id}", tags=["业务日志"])
    async def append_simple(log_id: str, content: Any = Body(...)) -> dict[str, Any]:
        raw = content if isinstance(content, str) else str((content or {}).get("content") or "")
        return ok(
            await service.append(log_id, raw, content if isinstance(content, dict) else {}),
            "追加日志成功",
        )

    @router.get(
        "/businessLog/get/{log_id}.log", response_class=PlainTextResponse, tags=["业务日志"]
    )
    async def preview(log_id: str, formatted: bool = Query(default=False)) -> PlainTextResponse:
        return PlainTextResponse(
            await service.content(log_id, formatted),
            headers={"Content-Disposition": f"inline; filename*=UTF-8''{quote(log_id)}.log"},
        )

    @router.get("/businessLog/get/{log_id}/lines", tags=["业务日志"])
    async def total_lines(log_id: str) -> dict[str, Any]:
        summary = await repository.business_summary(log_id)
        return ok(int((summary or {}).get("totalLines") or 0), "获取总行数成功")

    @router.get("/businessLog/get/{log_id}", tags=["业务日志"])
    async def get_log(
        log_id: str,
        format: str = Query(default="multiline"),
        formatted: bool = Query(default=True),
        startLine: int = Query(default=1),
        endLine: int = Query(default=-1),
    ) -> dict[str, Any]:
        if format.lower() == "full":
            result: Any = await service.content(log_id, formatted)
        else:
            result = await service.lines(
                log_id,
                startLine if format.lower() == "range" else 1,
                endLine if format.lower() == "range" else -1,
            )
        return ok(result, "查询业务日志成功")

    @router.post("/businessLog/query", tags=["业务日志"])
    async def query(body: dict[str, Any] | None = Body(default=None)) -> dict[str, Any]:
        return ok(await repository.page_business_logs(body or {}), "查询业务日志成功")

    @router.delete("/businessLog/{log_id}", tags=["业务日志"])
    async def delete(log_id: str) -> dict[str, Any]:
        return ok(await repository.delete_business_log(log_id), "删除业务日志成功")

    @router.put("/businessLog/updateExpireTime/{log_id}/{expire_days}", tags=["业务日志"])
    async def update_expiry(log_id: str, expire_days: int) -> dict[str, Any]:
        return ok(await repository.update_business_expiry(log_id, expire_days), "更新过期时间成功")

    @router.get("/businessLog/generateUrl/{log_id}", tags=["业务日志"])
    async def generate_url(
        request: Request,
        log_id: str,
        expireMinutes: int = Query(default=60),
        baseUrl: str | None = Query(default=None),
    ) -> dict[str, Any]:
        params = service.temporary_params(log_id, expireMinutes)
        base = (baseUrl or str(request.base_url).rstrip("/")).rstrip("/")
        url = f"{base}/businessLog/download/{params['fileName']}?Expires={params['expires']}&OSSAccessKeyId={params['accessKeyId']}&Signature={params['signature']}"
        return ok(
            {**params, "url": url, "logId": log_id, "expireMinutes": str(expireMinutes)},
            "生成临时访问URL成功",
        )

    @router.get("/businessLog/download/raw-{log_id}.log", tags=["业务日志"])
    async def download(
        log_id: str,
        Expires: int = Query(...),
        OSSAccessKeyId: str = Query(...),
        Signature: str = Query(...),
    ) -> PlainTextResponse:
        if OSSAccessKeyId != "jbm-logs":
            raise ValueError("AccessKeyId无效")
        service.verify(log_id, Expires, Signature)
        return PlainTextResponse(
            await service.content(log_id, False),
            headers={"Content-Disposition": f"attachment; filename=raw-{quote(log_id)}.log"},
        )

    @router.get("/businessLog/access/{log_id}", tags=["业务日志"])
    async def access(log_id: str, token: str = Query(...)) -> dict[str, Any]:
        service.verify_token(log_id, token)
        return ok(await service.content(log_id, False), "获取日志成功")

    @router.get("/businessLog/stream/{log_id}", tags=["业务日志"])
    async def stream(
        request: Request, log_id: str, intervalMillis: int = Query(default=2000)
    ) -> StreamingResponse:
        async def events() -> AsyncIterator[str]:
            next_line, version = 1, -1
            yield (
                "event: status\ndata: "
                + json.dumps(
                    {"status": "CONNECTED", "message": "等待业务日志写入..."}, ensure_ascii=False
                )
                + "\n\n"
            )
            while not await request.is_disconnected():
                lines = await service.lines(log_id, next_line, -1)
                if lines:
                    next_line = int(lines[-1].get("lineNumber") or next_line) + 1
                    yield "event: log\ndata: " + json.dumps(lines, ensure_ascii=False) + "\n\n"
                snapshot = await repository.stage(log_id)
                if snapshot and int(snapshot.get("version") or 0) > version:
                    version = int(snapshot["version"])
                    yield (
                        "event: progress\ndata: "
                        + json.dumps(snapshot, ensure_ascii=False)
                        + "\n\n"
                    )
                await asyncio.sleep(max(0.5, min(intervalMillis / 1000, 5)))

        return StreamingResponse(events(), media_type="text/event-stream")

    @router.post("/businessLog/stage/init", tags=["业务日志"])
    async def init_stage(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await service.init_stages(body), "初始化阶段成功")

    @router.post("/businessLog/stage/update", tags=["业务日志"])
    async def update_stage(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await service.update_stage(body), "更新阶段成功")

    @router.get("/businessLog/stage/{log_id}", tags=["业务日志"])
    async def stage(log_id: str) -> dict[str, Any]:
        return ok(await repository.stage(log_id), "查询阶段快照成功")

    @router.post("/businessLog/demo", tags=["业务日志"])
    async def demo(
        body: dict[str, Any] | None = Body(default=None), mode: str | None = Query(default=None)
    ) -> dict[str, Any]:
        selected = mode or str((body or {}).get("mode") or "multi-stage")
        return ok(await service.demo(selected), "演示任务创建成功")

    @router.post("/businessLog/demo/stage", tags=["业务日志"])
    async def stage_demo() -> dict[str, Any]:
        return ok(await service.demo("multi-stage"), "演示任务创建成功")

    return router
