from typing import Any, Mapping, Optional


class RedisClient:
    def __init__(self, config: Mapping[str, Any]) -> None:
        self.enabled = bool(config.get("enabled"))
        self.url = str(config.get("url") or "")
        self.client: Optional[Any] = None

    async def start(self) -> None:
        if not self.enabled:
            return
        import redis.asyncio as redis

        self.client = redis.from_url(self.url, decode_responses=True)
        await self.client.ping()

    async def stop(self) -> None:
        if self.client is not None:
            await self.client.aclose()
            self.client = None
