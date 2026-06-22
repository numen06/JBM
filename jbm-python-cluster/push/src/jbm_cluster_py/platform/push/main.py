from contextlib import asynccontextmanager
from typing import Any, Mapping, Optional

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
from jbm_cluster_py.platform.push.config_repository import PushConfigRepository
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
    database_config = app_config.database
    message_repository = PushMessageRepository(database_config) if database_config else None
    config_repository = PushConfigRepository(database_config) if database_config else None
    push_service = PushService(
        rabbitmq,
        app_config.rabbitmq,
        message_repository,
        config_repository,
        sms_config=app_config.get("aliyun.sms", {}) or {},
        email_config=app_config.get("spring.mail", {}) or {},
        push_config=app_config.get("jbm.push", {}) or {},
    )
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
        app.state.config_repository = config_repository
        app.state.business_event_service = business_event_service
        app.state.push_worker = push_worker
        app.state.nacos = nacos
        await nacos.start()
        if message_repository is not None:
            await message_repository.start()
        if config_repository is not None:
            await config_repository.start()
            await _ensure_system_channel_configs(config_repository, app_config)
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
            if config_repository is not None:
                await config_repository.stop()
            if message_repository is not None:
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


async def _ensure_system_channel_configs(config_repository: PushConfigRepository, app_config: AppConfig) -> None:
    sms_config = _compact_mapping(app_config.get("aliyun.sms", {}) or {})
    if sms_config:
        push_config = dict(app_config.get("jbm.push", {}) or {})
        if "jaja7-dry-run" in push_config:
            sms_config["jaja7-dry-run"] = push_config["jaja7-dry-run"]
        elif "jaja7DryRun" in push_config:
            sms_config["jaja7DryRun"] = push_config["jaja7DryRun"]
        await config_repository.ensure_default_push_config(3, sms_config, True)

    mail_config = _compact_mapping(app_config.get("spring.mail", {}) or {})
    if mail_config:
        await config_repository.ensure_default_push_config(2, mail_config, True)
        await config_repository.ensure_default_email_config(
            {
                "host": mail_config.get("host"),
                "username": mail_config.get("username"),
                "password": mail_config.get("password"),
                "port": mail_config.get("port"),
            }
        )

    mqtt_config = _compact_mapping(app_config.get("spring.mqtt", {}) or {})
    if mqtt_config:
        await config_repository.ensure_default_push_config(6, mqtt_config, True)


def _compact_mapping(value: Mapping[str, Any]) -> dict[str, Any]:
    return {key: item for key, item in dict(value).items() if item not in (None, "")}
