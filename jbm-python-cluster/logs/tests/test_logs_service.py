from __future__ import annotations

import base64

import httpx
import pytest
from fastapi import FastAPI
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
from jbm_cluster_py.platform.gateway.service import AccessLogger
from jbm_cluster_py.platform.logs.loki import LokiSink
from jbm_cluster_py.platform.logs.repository import LogsRepository
from jbm_cluster_py.platform.logs.router import build_logs_router
from jbm_cluster_py.platform.logs.service import BusinessLogService, GatewayLogIngestService


def database_config(tmp_path) -> dict[str, str]:
    return {"url": f"sqlite+aiosqlite:///{(tmp_path / 'logs.db').as_posix()}"}


@pytest.mark.asyncio
async def test_business_log_lifecycle_and_stages(tmp_path) -> None:
    repository = LogsRepository(database_config(tmp_path))
    await repository.start()
    service = BusinessLogService(repository, "test-secret")
    try:
        created = await service.create(
            {
                "module": "DEPLOY",
                "operation": "PUBLISH",
                "businessType": "release",
                "businessId": "r-1",
                "content": "prepare",
            }
        )
        log_id = created["logId"]
        await service.append(log_id, "build\npublish")

        lines = await service.lines(log_id)
        page = await repository.page_business_logs(
            {"pageForm": {"currPage": 1, "pageSize": 10}, "businessLog": {"businessId": "r-1"}}
        )
        assert [line["content"] for line in lines] == ["prepare", "build", "publish"]
        assert page["total"] == 1
        assert await repository.business_id_log("release", "r-1") == log_id

        await service.init_stages(
            {"logId": log_id, "stages": [{"stageCode": "build", "stageName": "构建"}]}
        )
        snapshot = await service.update_stage(
            {"logId": log_id, "stageCode": "build", "status": "DONE", "progress": 100}
        )
        assert snapshot["overallStatus"] == "DONE"
        assert snapshot["overallProgress"] == 100

        token = service.token(log_id)
        service.verify_token(log_id, token)
        assert "publish" in await service.content(log_id, False)
    finally:
        await repository.stop()


@pytest.mark.asyncio
async def test_gateway_access_log_is_integrated_and_filterable(tmp_path) -> None:
    repository = LogsRepository(database_config(tmp_path))
    await repository.start()
    ingest = GatewayLogIngestService(repository, LokiSink({"enabled": False}))
    app = FastAPI()
    app.include_router(
        build_logs_router(repository, BusinessLogService(repository, "test"), ingest)
    )
    try:
        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/GatewayLogs/ingest",
                json={
                    "accessId": "a-1",
                    "path": "/center/current/user",
                    "method": "GET",
                    "httpStatus": 200,
                    "serviceId": "jbm-cluster-platform-center",
                },
            )
            assert response.status_code == 200
            page = await client.post("/GatewayLogs/findLogs", json={})
            assert page.json()["result"]["total"] == 1
        await ingest.handle(
            {"accessId": "a-2", "path": "/logs/GatewayLogs/findLogs", "method": "POST"}
        )

        assert (await repository.gateway_log("a-1"))["httpStatus"] == 200
        assert await repository.gateway_log("a-2") is None
        rules = await repository.list_rules()
        assert next(rule for rule in rules if rule["ruleId"] == "builtin-logs")["hitCount"] == 1
    finally:
        await repository.stop()


@pytest.mark.asyncio
async def test_gateway_access_log_flows_through_kafka_to_loki_sink(tmp_path) -> None:
    class FakeKafka:
        enabled = True
        config = {"access-logs-topic": "jbm.logs.access.v1"}

        def __init__(self) -> None:
            self.messages: list[tuple[str, dict, str | None]] = []

        async def start(self) -> None:
            pass

        async def stop(self) -> None:
            pass

        async def publish_json(self, topic: str, payload: dict, key: str | None = None) -> None:
            self.messages.append((topic, dict(payload), key))

    kafka = FakeKafka()
    repository = LogsRepository(database_config(tmp_path))
    await repository.start()
    async with httpx.AsyncClient() as client:
        access_logger = AccessLogger(
            {"enabled": True},
            NacosDiscoveryClient({"enabled": False}),
            client,
            kafka,
        )
        await access_logger.start()
        try:
            row = {
                "accessId": "kafka-a-1",
                "path": "/center/current/user",
                "method": "GET",
                "httpStatus": 200,
                "serviceId": "jbm-cluster-platform-center",
            }
            await access_logger.record(row)
            assert kafka.messages == [("jbm.logs.access.v1", row, "kafka-a-1")]
            assert await repository.gateway_log("kafka-a-1") is None

            await GatewayLogIngestService(
                repository, LokiSink({"enabled": False})
            ).handle(kafka.messages[0][1])
            assert (await repository.gateway_log("kafka-a-1"))["httpStatus"] == 200
        finally:
            await access_logger.stop()
            await repository.stop()

    payload = LokiSink.gateway_payload(row)
    assert payload["streams"][0]["stream"] == {
        "job": "jbm-gateway",
        "service": "jbm-cluster-platform-center",
        "level": "info",
    }


@pytest.mark.asyncio
async def test_loki_sink_requires_and_sends_basic_auth() -> None:
    with pytest.raises(ValueError, match="username and password"):
        LokiSink({"enabled": True})

    expected = "Basic " + base64.b64encode(b"jbm:secret").decode()

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.headers["Authorization"] == expected
        return httpx.Response(204)

    sink = LokiSink(
        {
            "enabled": True,
            "url": "http://loki/loki/api/v1/push",
            "username": "jbm",
            "password": "secret",
        }
    )
    sink.client = httpx.AsyncClient(transport=httpx.MockTransport(handler), auth=sink.auth)
    try:
        await sink.send_gateway({"accessId": "auth-check", "httpStatus": 200})
    finally:
        await sink.stop()
