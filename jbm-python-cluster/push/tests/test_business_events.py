import asyncio
from pathlib import Path
from typing import Any, Mapping, Optional

import httpx

from jbm_cluster_py.common.config import AppConfig, parse_properties
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.push.business_events import (
    BusinessEventRepository,
    BusinessEventService,
    WebhookHttpClient,
)


class FakeRedis:
    def __init__(self) -> None:
        self.values: dict[str, list[str]] = {}

    async def rpush(self, key: str, value: str) -> None:
        self.values.setdefault(key, []).append(value)

    async def expire(self, key: str, seconds: int) -> None:
        return None

    async def lrange(self, key: str, start: int, end: int) -> list[str]:
        values = self.values.get(key, [])
        return values[start : end + 1]

    async def ltrim(self, key: str, start: int, end: int) -> None:
        values = self.values.get(key, [])
        self.values[key] = values[start:] if end == -1 else values[start : end + 1]

    async def scan_iter(self, pattern: str):
        prefix = pattern.rstrip("*")
        for key in list(self.values):
            if key.startswith(prefix):
                yield key


class FakeHttpClient:
    def __init__(self, statuses: list[int]) -> None:
        self.statuses = statuses
        self.requests: list[tuple[str, Optional[str], Optional[str]]] = []

    async def start(self) -> None:
        return None

    async def stop(self) -> None:
        return None

    async def request(self, url: str, method: Optional[str], body: Optional[str]) -> httpx.Response:
        self.requests.append((url, method, body))
        status = self.statuses.pop(0)
        return httpx.Response(status, text="ok")


def repository(tmp_path: Path) -> BusinessEventRepository:
    return BusinessEventRepository({"url": "sqlite+aiosqlite:///%s" % (tmp_path / "bevent.db")})


def service(
    repo: BusinessEventRepository,
    redis_client: RedisClient,
    http_client: FakeHttpClient,
) -> BusinessEventService:
    return BusinessEventService(
        repo,
        redis_client,
        NacosDiscoveryClient({"enabled": False}),
        http_client=http_client,
    )


def test_parse_properties_builds_nested_spring_config() -> None:
    parsed = parse_properties(
        "\n".join(
            [
                "spring.datasource.url=jdbc:mysql://mysql:3306/jbm",
                "spring.rabbitmq.host=rabbit",
                "spring.redis.database=1",
            ]
        )
    )

    assert parsed["spring"]["datasource"]["url"] == "jdbc:mysql://mysql:3306/jbm"
    assert parsed["spring"]["rabbitmq"]["host"] == "rabbit"
    assert parsed["spring"]["redis"]["database"] == 1


def test_app_config_maps_spring_database_redis_and_rabbit() -> None:
    config = AppConfig(
        {
            "spring": {
                "datasource": {
                    "url": "jdbc:mysql://mysql:3306/jbm",
                    "username": "u",
                    "password": "p",
                },
                "rabbitmq": {"host": "rabbit", "port": 5672, "username": "admin", "password": "admin"},
                "redis": {"host": "redis", "port": 6379, "database": 2},
            }
        },
        profile="test",
        config_dir=None,
    )

    assert config.database["url"] == "jdbc:mysql://mysql:3306/jbm"
    assert config.rabbitmq["enabled"] is True
    assert config.rabbitmq["url"] == "amqp://admin:admin@rabbit:5672/"
    assert config.redis["enabled"] is True
    assert config.redis["url"] == "redis://redis:6379/2"


def test_business_event_resource_registration_preserves_event_id(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        redis = RedisClient({"enabled": False})
        redis.client = FakeRedis()
        svc = service(repo, redis, FakeHttpClient([200]))

        payload = {
            "serviceId": "demo-service",
            "jbmClusterBusinessEventBeans": [
                {
                    "eventCode": "TestBusinessEvent",
                    "eventName": "测试事件",
                    "eventGroup": "demo",
                    "eventBody": "{\"name\":\"a\"}",
                    "url": "http://receiver/businessEventListener",
                    "serviceName": "demo-service",
                    "methodType": "POST",
                }
            ],
        }
        first = await svc.receive_resource_payload(payload)
        current = await repo.select_config_by_code_url("TestBusinessEvent", "http://receiver/businessEventListener")
        assert first["saved"] == 1
        assert current is not None

        second = await svc.receive_resource_payload(payload)
        updated = await repo.select_config_by_code_url("TestBusinessEvent", "http://receiver/businessEventListener")
        assert second["saved"] == 1
        assert updated["eventId"] == current["eventId"]
        await repo.stop()

    asyncio.run(run())


def test_business_event_dispatch_writes_successful_task(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        redis = RedisClient({"enabled": False})
        redis.client = FakeRedis()
        fake_http = FakeHttpClient([200])
        svc = service(repo, redis, fake_http)
        await svc.receive_resource_payload(
            {
                "serviceId": "demo-service",
                "jbmClusterBusinessEventBeans": [
                    {
                        "eventCode": "TestBusinessEvent",
                        "eventName": "测试事件",
                        "eventGroup": "demo",
                        "eventBody": "{\"default\":true}",
                        "url": "http://receiver/businessEventListener",
                        "serviceName": "",
                        "methodType": "POST",
                    }
                ],
            }
        )

        result = await svc.send_business_event_payload(
            {"eventCode": "TestBusinessEvent", "eventBody": "{\"hello\":\"world\"}"}
        )

        assert result["sent"] == 1
        task = result["tasks"][0]
        assert task["status"] == "SUCCESS"
        assert task["httpStatus"] == 200
        assert fake_http.requests[0][2] == "{\"hello\":\"world\"}"
        await repo.stop()

    asyncio.run(run())


def test_failed_direct_delivery_uses_redis_retry_semantics(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        redis = RedisClient({"enabled": False})
        fake_redis = FakeRedis()
        redis.client = fake_redis
        svc = service(repo, redis, FakeHttpClient([500, 202]))
        await svc.receive_resource_payload(
            {
                "serviceId": "demo-service",
                "jbmClusterBusinessEventBeans": [
                    {
                        "eventCode": "RetryEvent",
                        "eventName": "重试事件",
                        "eventGroup": "demo",
                        "eventBody": "{}",
                        "url": "http://receiver/retry",
                        "methodType": "POST",
                    }
                ],
            }
        )

        result = await svc.send_business_event_payload({"eventCode": "RetryEvent", "eventBody": "{}"})

        task = result["tasks"][0]
        assert task["status"] == "SUCCESS"
        assert task["retryNumber"] == 1
        assert fake_redis.values["jbm:bevent:http://receiver/retry"] == []
        await repo.stop()

    asyncio.run(run())


def test_feign_url_resolves_with_nacos_instance() -> None:
    class FakeDiscovery:
        async def choose_instance(self, service_name: str) -> Mapping[str, Any]:
            assert service_name == "demo-service"
            return {"ip": "127.0.0.1", "port": 8080}

    async def run() -> None:
        client = WebhookHttpClient(FakeDiscovery())  # type: ignore[arg-type]
        assert await client._resolve_url("feign://demo-service/api/test") == "http://127.0.0.1:8080/api/test"

    asyncio.run(run())
