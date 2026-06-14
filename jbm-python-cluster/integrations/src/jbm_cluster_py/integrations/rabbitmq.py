import asyncio
import json
import logging
from typing import Any, Awaitable, Callable, Mapping, Optional

logger = logging.getLogger(__name__)
MessageHandler = Callable[[Mapping[str, Any]], Awaitable[None]]


class RabbitMQConsumer:
    def __init__(self, config: Mapping[str, Any]) -> None:
        self.enabled = bool(config.get("enabled"))
        self.url = str(config.get("url") or "")
        self.prefetch_count = int(config.get("prefetch-count") or 50)
        self.connection: Optional[Any] = None
        self.channel: Optional[Any] = None

    async def start(self) -> None:
        if not self.enabled:
            logger.info("RabbitMQ consumer disabled")
            return
        import aio_pika

        self.connection = await aio_pika.connect_robust(self.url)
        self.channel = await self.connection.channel()
        await self.channel.set_qos(prefetch_count=self.prefetch_count)

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

    async def stop(self) -> None:
        if self.connection is not None:
            await asyncio.shield(self.connection.close())
            self.connection = None
            self.channel = None
