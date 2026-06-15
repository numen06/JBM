import asyncio
import json
from pathlib import Path
from typing import Any, Mapping, Optional

import httpx

from jbm_cluster_py.common.config import AppConfig, parse_properties
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
from jbm_cluster_py.integrations.rabbitmq import MAX_DELIVERY_RETRY, RabbitMQClient
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.push.business_events import (
    BusinessEventRepository,
    BusinessEventService,
    WebhookDeliveryConsumer,
    WebhookHttpClient,
)


class FakeRabbitMQClient:
    def __init__(self) -> None:
        self.enabled = True
        self.channel = object()
        self.delivery_tasks: list[dict[str, Any]] = []
        self.retry_tasks: list[tuple[int, dict[str, Any]]] = []
        self.dlt_tasks: list[dict[str, Any]] = []

    async def start(self) -> None:
        return None

    async def stop(self) -> None:
        return None

    async def declare_delivery_topology(self) -> None:
        return None

    async def consume_json(self, queue_name: str, handler) -> None:
        return None

    async def publish_delivery_task(self, payload: Mapping[str, Any], retry_count: int = 0) -> None:
        self.delivery_tasks.append({"payload": dict(payload), "retry_count": retry_count})

    async def consume_delivery(self, handler) -> None:
        return None

    async def consume_dlt(self, handler) -> None:
        return None

    async def route_failure(self, payload: Mapping[str, Any], retry_count: int) -> None:
        next_retry = retry_count + 1
        if next_retry <= MAX_DELIVERY_RETRY:
            self.retry_tasks.append((next_retry, dict(payload)))
            return
        self.dlt_tasks.append(dict(payload))


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
    http_client: FakeHttpClient,
    rabbitmq: Optional[FakeRabbitMQClient] = None,
) -> BusinessEventService:
    fake_rabbit = rabbitmq or FakeRabbitMQClient()
    redis = RedisClient({"enabled": False})
    return BusinessEventService(
        repo,
        redis,
        NacosDiscoveryClient({"enabled": False}),
        http_client=http_client,
        rabbitmq=fake_rabbit,
    )


async def process_delivery_queue(
    svc: BusinessEventService,
    fake_rabbit: FakeRabbitMQClient,
    http_client: FakeHttpClient,
) -> None:
    while fake_rabbit.delivery_tasks:
        item = fake_rabbit.delivery_tasks.pop(0)
        payload = item["payload"]
        try:
            await svc.delivery_consumer.handle(
                json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
            )
        except Exception:
            retry_count = int(payload.get("retryNumber") or 0)
            await fake_rabbit.route_failure(payload, retry_count)
            if fake_rabbit.retry_tasks:
                retry_num, retry_payload = fake_rabbit.retry_tasks.pop(0)
                retry_payload["retryNumber"] = retry_num
                fake_rabbit.delivery_tasks.append({"payload": retry_payload, "retry_count": retry_num})
                if http_client.statuses:
                    await process_delivery_queue(svc, fake_rabbit, http_client)


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
        fake_http = FakeHttpClient([200])
        svc = service(repo, fake_http)

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


def test_delivery_task_produced_to_rabbitmq_queue(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        fake_rabbit = FakeRabbitMQClient()
        fake_http = FakeHttpClient([200])
        svc = service(repo, fake_http, fake_rabbit)
        await svc.receive_resource_payload(
            {
                "serviceId": "demo-service",
                "jbmClusterBusinessEventBeans": [
                    {
                        "eventCode": "TestBusinessEvent",
                        "eventName": "测试事件",
                        "eventGroup": "demo",
                        "eventBody": "{}",
                        "url": "http://receiver/businessEventListener",
                        "methodType": "POST",
                    }
                ],
            }
        )

        await svc.send_business_event_payload({"eventCode": "TestBusinessEvent", "eventBody": "{\"hello\":\"world\"}"})

        assert len(fake_rabbit.delivery_tasks) == 1
        payload = fake_rabbit.delivery_tasks[0]["payload"]
        assert payload["taskUrl"] == "http://receiver/businessEventListener"
        assert payload["request"] == "{\"hello\":\"world\"}"
        await repo.stop()

    asyncio.run(run())


def test_business_event_dispatch_writes_successful_task(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        fake_rabbit = FakeRabbitMQClient()
        fake_http = FakeHttpClient([200])
        svc = service(repo, fake_http, fake_rabbit)
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
        await process_delivery_queue(svc, fake_rabbit, fake_http)

        assert result["sent"] == 1
        task_id = result["tasks"][0]["taskId"]
        task = await repo.select_task(task_id)
        assert task is not None
        assert task["status"] == "SUCCESS"
        assert task["httpStatus"] == 200
        assert fake_http.requests[0][2] == "{\"hello\":\"world\"}"
        await repo.stop()

    asyncio.run(run())


def test_delivery_retry_on_http_failure(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        fake_rabbit = FakeRabbitMQClient()
        fake_http = FakeHttpClient([500, 202])
        svc = service(repo, fake_http, fake_rabbit)
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
        await process_delivery_queue(svc, fake_rabbit, fake_http)

        task = await repo.select_task(result["tasks"][0]["taskId"])
        assert task is not None
        assert task["status"] == "SUCCESS"
        assert task["retryNumber"] == 1
        assert len(fake_http.requests) == 2
        await repo.stop()

    asyncio.run(run())


def test_delivery_dlt_after_max_retry(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        fake_rabbit = FakeRabbitMQClient()
        fake_http = FakeHttpClient([500, 500, 500, 500])
        svc = service(repo, fake_http, fake_rabbit)
        await svc.receive_resource_payload(
            {
                "serviceId": "demo-service",
                "jbmClusterBusinessEventBeans": [
                    {
                        "eventCode": "FailEvent",
                        "eventName": "失败事件",
                        "eventGroup": "demo",
                        "eventBody": "{}",
                        "url": "http://receiver/fail",
                        "methodType": "POST",
                    }
                ],
            }
        )

        result = await svc.send_business_event_payload({"eventCode": "FailEvent", "eventBody": "{}"})
        await process_delivery_queue(svc, fake_rabbit, fake_http)
        while fake_rabbit.delivery_tasks:
            await process_delivery_queue(svc, fake_rabbit, fake_http)

        assert fake_rabbit.dlt_tasks
        dlt_payload = fake_rabbit.dlt_tasks[0]
        await svc.delivery_consumer.handle_dlt(
            json.dumps(dlt_payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        )
        task = await repo.select_task(result["tasks"][0]["taskId"])
        assert task is not None
        assert task["status"] == "FAILED"
        assert "最大重试次数" in (task.get("errorMsg") or "")
        await repo.stop()

    asyncio.run(run())


def test_group_selection_strategy_picks_enabled_config(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        fake_rabbit = FakeRabbitMQClient()
        fake_http = FakeHttpClient([200, 200])
        svc = service(repo, fake_http, fake_rabbit)
        await svc.receive_resource_payload(
            {
                "serviceId": "demo-service",
                "jbmClusterBusinessEventBeans": [
                    {
                        "eventCode": "GroupEvent",
                        "eventName": "分组1",
                        "eventGroup": "group-a",
                        "eventBody": "{}",
                        "url": "http://receiver/a",
                        "methodType": "POST",
                    },
                    {
                        "eventCode": "GroupEvent",
                        "eventName": "分组2",
                        "eventGroup": "group-b",
                        "eventBody": "{}",
                        "url": "http://receiver/b",
                        "methodType": "POST",
                    },
                ],
            }
        )

        result = await svc.send_business_event_payload({"eventCode": "GroupEvent", "eventBody": "{}"})
        assert result["sent"] == 2
        urls = {item["payload"]["taskUrl"] for item in fake_rabbit.delivery_tasks}
        assert urls == {"http://receiver/a", "http://receiver/b"}
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


def test_page_webhook_event_configs_supports_keyword_filter(tmp_path: Path) -> None:
    async def run() -> None:
        repo = repository(tmp_path)
        await repo.start()
        await repo.save_config(
            {
                "businessEventCode": "OrderCreatedEvent",
                "eventName": "订单创建",
                "eventGroup": "order",
                "url": "http://receiver/order",
                "enable": True,
            }
        )
        page = await repo.page_webhook_event_configs({"keyword": "OrderCreated"})
        assert page["total"] == 1
        assert page["contents"][0]["businessEventCode"] == "OrderCreatedEvent"
        await repo.stop()

    asyncio.run(run())


def test_rabbitmq_client_retry_routing_constants() -> None:
    assert RabbitMQClient.DELIVERY_QUEUE == "jbm.webhook.delivery"
    assert MAX_DELIVERY_RETRY == 3
