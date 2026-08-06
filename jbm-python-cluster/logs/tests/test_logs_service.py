from __future__ import annotations

import httpx
import pytest
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
from jbm_cluster_py.platform.gateway.service import AccessLogger
from jbm_cluster_py.platform.logs.loki import LokiSink
from jbm_cluster_py.platform.logs.repository import LogsRepository
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
    async with httpx.AsyncClient() as client:
        access_logger = AccessLogger(
            {"enabled": True, "ingest-enabled": False},
            NacosDiscoveryClient({"enabled": False}),
            client,
            database_config(tmp_path),
        )
        await access_logger.start()
        try:
            await access_logger.record(
                {
                    "accessId": "a-1",
                    "path": "/center/current/user",
                    "method": "GET",
                    "httpStatus": 200,
                    "serviceId": "jbm-cluster-platform-center",
                }
            )
            await access_logger.record(
                {"accessId": "a-2", "path": "/logs/GatewayLogs/findLogs", "method": "POST"}
            )

            assert (await access_logger.repository.gateway_log("a-1"))["httpStatus"] == 200
            assert await access_logger.repository.gateway_log("a-2") is None
            rules = await access_logger.repository.list_rules()
            assert next(rule for rule in rules if rule["ruleId"] == "builtin-logs")["hitCount"] == 1
        finally:
            await access_logger.stop()


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
    async with httpx.AsyncClient() as client:
        access_logger = AccessLogger(
            {"enabled": True},
            NacosDiscoveryClient({"enabled": False}),
            client,
            database_config(tmp_path),
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
            assert await access_logger.repository.gateway_log("kafka-a-1") is None

            await GatewayLogIngestService(
                access_logger.repository, LokiSink({"enabled": False})
            ).handle(kafka.messages[0][1])
            assert (await access_logger.repository.gateway_log("kafka-a-1"))["httpStatus"] == 200
        finally:
            await access_logger.stop()

    payload = LokiSink.gateway_payload(row)
    assert payload["streams"][0]["stream"] == {
        "job": "jbm-gateway",
        "service": "jbm-cluster-platform-center",
        "level": "info",
    }
