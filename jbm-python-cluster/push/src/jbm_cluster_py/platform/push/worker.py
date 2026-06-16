from __future__ import annotations

import logging
from typing import Any, Mapping

from jbm_cluster_py.platform.push.service import PushService

logger = logging.getLogger(__name__)


class PushWorker:
    def __init__(self, push_service: PushService, rabbitmq: Any, config: Mapping[str, Any]) -> None:
        self.push_service = push_service
        self.rabbitmq = rabbitmq
        self.config = dict(config)

    async def start(self) -> None:
        await self.rabbitmq.consume_json(
            str(self.config.get("push-message-queue") or "pushMessage-in-0.jbm-cluster-platform-push"),
            self.handle_push_message,
        )

    async def handle_push_message(self, payload: Mapping[str, Any]) -> None:
        await self.push_service.handle_push_event(payload)
