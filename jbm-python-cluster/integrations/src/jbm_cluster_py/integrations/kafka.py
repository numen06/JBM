from __future__ import annotations

import asyncio
import json
import logging
from collections.abc import Awaitable, Callable, Mapping
from typing import Any

logger = logging.getLogger(__name__)

MessageHandler = Callable[[Mapping[str, Any]], Awaitable[None]]


class KafkaClient:
    def __init__(self, config: Mapping[str, Any]) -> None:
        self.config = dict(config)
        self.enabled = bool(self.config.get("enabled"))
        self.bootstrap_servers = str(
            self.config.get("bootstrap-servers") or "localhost:9092"
        )
        self.retry_seconds = float(self.config.get("retry-seconds") or 2)
        self.client_options = self._client_options()
        self.producer: Any = None
        self._consumers: list[Any] = []
        self._tasks: list[asyncio.Task[None]] = []
        self._stopped = False

    async def start(self) -> None:
        if not self.enabled or self.producer is not None:
            return
        from aiokafka import AIOKafkaProducer

        self._stopped = False
        self.producer = AIOKafkaProducer(
            bootstrap_servers=self.bootstrap_servers,
            acks="all",
            **self.client_options,
        )
        await self.producer.start()

    async def publish_json(
        self, topic: str, payload: Mapping[str, Any], key: str | None = None
    ) -> None:
        if not self.enabled:
            raise RuntimeError("Kafka client is disabled")
        if self.producer is None:
            await self.start()
        body = json.dumps(payload, ensure_ascii=False, separators=(",", ":"), default=str)
        await self.producer.send_and_wait(
            topic,
            body.encode("utf-8"),
            key=key.encode("utf-8") if key else None,
        )

    async def consume_json(
        self, topic: str, group_id: str, handler: MessageHandler
    ) -> None:
        if not self.enabled:
            logger.info("Kafka client disabled")
            return
        from aiokafka import AIOKafkaConsumer

        self._stopped = False
        consumer = AIOKafkaConsumer(
            topic,
            bootstrap_servers=self.bootstrap_servers,
            group_id=group_id,
            enable_auto_commit=False,
            auto_offset_reset="earliest",
            **self.client_options,
        )
        await consumer.start()
        self._consumers.append(consumer)
        self._tasks.append(asyncio.create_task(self._consume(consumer, topic, handler)))
        logger.info("Consuming Kafka topic %s as %s", topic, group_id)

    async def _consume(self, consumer: Any, topic: str, handler: MessageHandler) -> None:
        async for message in consumer:
            while not self._stopped:
                try:
                    payload = json.loads(message.value.decode("utf-8"))
                    if isinstance(payload, str):
                        payload = json.loads(payload)
                    if not isinstance(payload, Mapping):
                        raise ValueError("Kafka log message must be a JSON object")
                    await handler(payload)
                    await consumer.commit()
                    break
                except asyncio.CancelledError:
                    raise
                except Exception:
                    logger.exception(
                        "Kafka handler failed for %s partition=%s offset=%s; retrying",
                        topic,
                        message.partition,
                        message.offset,
                    )
                    await asyncio.sleep(self.retry_seconds)

    async def stop(self) -> None:
        self._stopped = True
        for task in self._tasks:
            task.cancel()
        if self._tasks:
            await asyncio.gather(*self._tasks, return_exceptions=True)
        self._tasks.clear()
        for consumer in self._consumers:
            await consumer.stop()
        self._consumers.clear()
        if self.producer is not None:
            await self.producer.stop()
            self.producer = None

    def _client_options(self) -> dict[str, str]:
        security_protocol = str(self.config.get("security-protocol") or "PLAINTEXT")
        if not security_protocol.startswith("SASL_"):
            return {"security_protocol": security_protocol}
        username = str(self.config.get("username") or "")
        password = str(self.config.get("password") or "")
        if not (username and password):
            raise ValueError("Kafka username and password are required for SASL")
        return {
            "security_protocol": security_protocol,
            "sasl_mechanism": str(self.config.get("sasl-mechanism") or "PLAIN"),
            "sasl_plain_username": username,
            "sasl_plain_password": password,
        }
