from __future__ import annotations

import base64

from sqlalchemy import inspect
from jbm_cluster_py.platform.logs.repository import now_iso

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


class BusinessArchive:
    def __init__(self):
        self.rows = {}

    async def read(self, summary, start=1, end=-1, *, event_key=None):
        return [item for (log_id, number), item in sorted(self.rows.items())
                if log_id == summary["logId"] and number >= start and (end < 0 or number <= end)
                and (not event_key or item["eventKey"] == event_key)]

    async def write_verified(self, summary, rows, event_key):
        for row in rows:
            number = row["line_number"]
            key = summary["logId"], number
            assert key not in self.rows
            self.rows[key] = {"lineNumber": number, "eventKey": event_key, "row": row}


class GatewayArchive:
    enabled = True

    def __init__(self, directory):
        self.directory = directory
        self.events = {}

    def backup_path(self, event):
        return self.directory / event["eventId"]

    async def handle(self, event):
        existing = self.events.setdefault(event["eventId"], event)
        assert existing == event

    async def gateway_page(self, body, operation_only=False):
        values = list(self.events.values())
        return {"contents": [row["gateway"] for row in values], "total": len(values)}


@pytest.mark.asyncio
async def test_business_log_lifecycle_and_stages(tmp_path) -> None:
    repository = LogsRepository(database_config(tmp_path))
    await repository.start()
    repository.business_archive = BusinessArchive()
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
        event = {"eventType": "APPEND", "logId": log_id, "content": "Kafka",
                 "_kafkaRecordId": "confirmed-partition-offset"}
        await service.handle_event(event)
        await service.handle_event(event)
        assert [item["content"] for item in await service.lines(log_id, 5, 5)] == ["Kafka"]
        assert (await repository.business_summary(log_id))["totalLines"] == 5
        async with repository.engine.connect() as conn:
            assert not await conn.run_sync(lambda c: inspect(c).has_table("business_log_line"))
    finally:
        await repository.stop()


@pytest.mark.asyncio
async def test_gateway_access_log_is_integrated_and_filterable(tmp_path) -> None:
    repository = LogsRepository(database_config(tmp_path))
    await repository.start()
    archive = GatewayArchive(tmp_path)
    ingest = GatewayLogIngestService(archive)
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
                    "requestTime": now_iso(),
                    "path": "/center/current/user",
                    "method": "GET",
                    "httpStatus": 200,
                    "serviceId": "jbm-cluster-platform-center",
                },
            )
            assert response.status_code == 200
            page = await client.post("/GatewayLogs/findLogs", json={})
            assert page.json()["result"]["total"] == 1
            assert page.json()["result"]["contents"][0]["httpStatus"] == 200
            assert (await client.post("/GatewayLogs/filterRules/test", json={
                "path": "/logs/GatewayLogs/findLogs", "method": "POST"
            })).json()["result"]["matched"]
            archive.enabled = False
            assert (await client.post("/GatewayLogs/findLogs", json={})).status_code == 503
        async with repository.engine.connect() as conn:
            assert not await conn.run_sync(lambda c: inspect(c).has_table("gateway_logs"))
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
                "requestTime": now_iso(),
                "path": "/center/current/user",
                "method": "GET",
                "httpStatus": 200,
                "serviceId": "jbm-cluster-platform-center",
            }
            await access_logger.record(row)
            assert kafka.messages == [("jbm.logs.access.v1", row, "kafka-a-1")]
            archive = GatewayArchive(tmp_path)
            ingest = GatewayLogIngestService(archive)
            await ingest.handle(kafka.messages[0][1])
            await ingest.handle(kafka.messages[0][1])
            assert len(archive.events) == 1
            event, = archive.events.values()
            assert event["gateway"]["httpStatus"] == 200
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
