from __future__ import annotations

import asyncio
import json
import logging
from typing import Any, Awaitable, Callable, Mapping, Optional

logger = logging.getLogger(__name__)

MessageHandler = Callable[[Mapping[str, Any]], Awaitable[None]]
DeliveryHandler = Callable[[bytes], Awaitable[None]]
RawMessageHandler = Callable[[Any], Awaitable[None]]

MAX_DELIVERY_RETRY = 3


class RabbitMQClient:
    DELIVERY_QUEUE = "jbm.webhook.delivery"
    DLX_EXCHANGE = "jbm.webhook.delivery.dlx"
    DLT_QUEUE = "jbm.webhook.delivery.dlt"
    RETRY_QUEUES = {
        1: ("jbm.webhook.delivery.retry.1", 2000),
        2: ("jbm.webhook.delivery.retry.2", 10000),
        3: ("jbm.webhook.delivery.retry.3", 30000),
    }

    def __init__(self, config: Mapping[str, Any]) -> None:
        self.enabled = bool(config.get("enabled"))
        self.url = str(config.get("url") or "")
        self.prefetch_count = int(config.get("prefetch-count") or 50)
        self.delivery_queue = str(config.get("delivery-queue") or self.DELIVERY_QUEUE)
        self.dlt_queue = str(config.get("delivery-dlt-queue") or self.DLT_QUEUE)
        self.dlx_exchange = str(config.get("delivery-dlx-exchange") or self.DLX_EXCHANGE)
        self.connection: Optional[Any] = None
        self.channel: Optional[Any] = None
        self._delivery_consumer_tag: Optional[str] = None
        self._dlt_consumer_tag: Optional[str] = None
        self._stopped = False

    async def start(self) -> None:
        if not self.enabled:
            logger.info("RabbitMQ client disabled")
            return
        if self.channel is not None:
            return
        import aio_pika

        self._stopped = False
        self.connection = await aio_pika.connect_robust(self.url)
        self.channel = await self.connection.channel()
        await self.channel.set_qos(prefetch_count=self.prefetch_count)

    async def declare_delivery_topology(self) -> None:
        if self.channel is None:
            return
        import aio_pika

        dlx = await self.channel.declare_exchange(self.dlx_exchange, aio_pika.ExchangeType.DIRECT, durable=True)
        await self.channel.declare_queue(
            self.delivery_queue,
            durable=True,
            arguments={
                "x-dead-letter-exchange": self.dlx_exchange,
                "x-dead-letter-routing-key": "retry.1",
            },
        )
        dlt_q = await self.channel.declare_queue(self.dlt_queue, durable=True)
        await dlt_q.bind(dlx, routing_key="failed")
        for retry_num, (queue_name, ttl_ms) in self.RETRY_QUEUES.items():
            retry_q = await self.channel.declare_queue(
                queue_name,
                durable=True,
                arguments={
                    "x-message-ttl": ttl_ms,
                    "x-dead-letter-exchange": "",
                    "x-dead-letter-routing-key": self.delivery_queue,
                },
            )
            await retry_q.bind(dlx, routing_key="retry.%s" % retry_num)

    async def consume_json(self, queue_name: str, handler: MessageHandler) -> None:
        if self.channel is None:
            return
        queue = await self.channel.declare_queue(queue_name, durable=True)

        async def on_message(message: Any) -> None:
            async with message.process():
                payload = json.loads(message.body.decode("utf-8"))
                if isinstance(payload, str):
                    payload = json.loads(payload)
                await handler(payload)

        await queue.consume(on_message)
        logger.info("Consuming RabbitMQ queue %s", queue_name)

    async def publish_json(self, queue_name: str, payload: Mapping[str, Any]) -> None:
        if self.channel is None:
            raise RuntimeError("RabbitMQ channel is not available")
        import aio_pika

        body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        await self.channel.declare_queue(queue_name, durable=True)
        await self.channel.default_exchange.publish(
            aio_pika.Message(body=body, delivery_mode=aio_pika.DeliveryMode.PERSISTENT),
            routing_key=queue_name,
        )

    async def publish_delivery_task(self, payload: Mapping[str, Any], retry_count: int = 0) -> None:
        if self.channel is None:
            raise RuntimeError("RabbitMQ channel is not available")
        import aio_pika

        body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        await self.channel.default_exchange.publish(
            aio_pika.Message(
                body=body,
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                headers={"x-retry-count": retry_count},
            ),
            routing_key=self.delivery_queue,
        )

    async def consume_delivery(self, handler: DeliveryHandler) -> None:
        if self.channel is None:
            return
        queue = await self.channel.declare_queue(self.delivery_queue, durable=True, passive=True)

        async def on_message(message: Any) -> None:
            try:
                await handler(message.body)
                await message.ack()
            except Exception:
                logger.exception("Webhook delivery failed; routing retry or dead letter")
                await self._route_retry_or_dlt(message)
                await message.ack()

        self._delivery_consumer_tag = await queue.consume(on_message)
        logger.info("Consuming RabbitMQ delivery queue %s", self.delivery_queue)

    async def consume_dlt(self, handler: DeliveryHandler) -> None:
        if self.channel is None:
            return
        queue = await self.channel.declare_queue(self.dlt_queue, durable=True, passive=True)

        async def on_message(message: Any) -> None:
            async with message.process():
                await handler(message.body)

        self._dlt_consumer_tag = await queue.consume(on_message)
        logger.info("Consuming RabbitMQ dead-letter queue %s", self.dlt_queue)

    async def _route_retry_or_dlt(self, message: Any) -> None:
        if self.channel is None:
            return
        import aio_pika

        headers = dict(message.headers or {})
        retry_count = int(headers.get("x-retry-count", 0)) + 1
        next_headers = {"x-retry-count": retry_count}
        body = message.body
        if retry_count <= MAX_DELIVERY_RETRY:
            routing_key = self.RETRY_QUEUES[retry_count][0]
            await self.channel.default_exchange.publish(
                aio_pika.Message(
                    body=body,
                    delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                    headers=next_headers,
                ),
                routing_key=routing_key,
            )
            return
        await self.channel.default_exchange.publish(
            aio_pika.Message(
                body=body,
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                headers=next_headers,
            ),
            routing_key=self.dlt_queue,
        )

    async def stop(self) -> None:
        self._stopped = True
        if self.connection is not None:
            await asyncio.shield(self.connection.close())
            self.connection = None
            self.channel = None
            self._delivery_consumer_tag = None
            self._dlt_consumer_tag = None


# Backward-compatible alias used by existing imports.
RabbitMQConsumer = RabbitMQClient
