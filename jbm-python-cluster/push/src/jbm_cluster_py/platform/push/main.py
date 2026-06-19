from contextlib import asynccontextmanager
from typing import Any, Optional

import uvicorn
from fastapi import FastAPI

from jbm_cluster_py.common.banner import print_jbm_banner
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.health import build_health_router
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient, NacosRegistrar
from jbm_cluster_py.integrations.rabbitmq import RabbitMQClient
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.push.business_events import (
    BusinessEventRepository,
    BusinessEventService,
    WebhookDeliveryConsumer,
    WebhookHttpClient,
)
from jbm_cluster_py.platform.push.push_message_repository import PushMessageRepository
from jbm_cluster_py.platform.push.router import build_push_router
from jbm_cluster_py.platform.push.service import PushService
from jbm_cluster_py.platform.push.worker import PushWorker


def create_app(config: Optional[AppConfig] = None) -> FastAPI:
    app_config = config or AppConfig.load(app="push")
    configure_logging()
    print_jbm_banner()
    init_telemetry(app_config.telemetry)
    rabbitmq = RabbitMQClient(app_config.rabbitmq)
    message_repository = PushMessageRepository(app_config.database)
    push_service = PushService(rabbitmq, app_config.rabbitmq, message_repository)
    repository = BusinessEventRepository(app_config.database)
    discovery = NacosDiscoveryClient(app_config.nacos_discovery)
    http_client = WebhookHttpClient(discovery)
    business_event_service = BusinessEventService(
        repository,
        RedisClient(app_config.redis),
        discovery,
        http_client=http_client,
        rabbitmq=rabbitmq,
        rabbitmq_config=app_config.rabbitmq,
    )
    delivery_consumer = business_event_service.delivery_consumer

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        push_worker = PushWorker(push_service, rabbitmq, app_config.rabbitmq)
        app.state.config = app_config
        app.state.push_service = push_service
        app.state.message_repository = message_repository
        app.state.business_event_service = business_event_service
        app.state.push_worker = push_worker
        app.state.nacos = nacos
        await nacos.start()
        await message_repository.start()
        if rabbitmq.enabled:
            await rabbitmq.start()
            await push_worker.start()
            await rabbitmq.declare_delivery_topology()
            await business_event_service.start()
            await rabbitmq.consume_delivery(delivery_consumer.handle)
            await rabbitmq.consume_dlt(delivery_consumer.handle_dlt)
        else:
            await business_event_service.start()
        try:
            yield
        finally:
            await business_event_service.stop()
            await message_repository.stop()
            if rabbitmq.enabled:
                await rabbitmq.stop()
            await nacos.stop()

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Push Service"),
        description=str(openapi.get("description") or "Python compatibility service for jbm-cluster-platform-push."),
        version=str(openapi.get("version") or "0.1.0"),
        docs_url=str(openapi.get("docs-url") or "/docs"),
        redoc_url=str(openapi.get("redoc-url") or "/redoc"),
        openapi_url=str(openapi.get("openapi-url") or "/openapi.json"),
        lifespan=lifespan,
    )
    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_push_router(push_service, business_event_service))
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="push")
    uvicorn.run("jbm_cluster_py.platform.push.main:app", host=config.host, port=config.port, reload=False)


if __name__ == "__main__":
    run()
