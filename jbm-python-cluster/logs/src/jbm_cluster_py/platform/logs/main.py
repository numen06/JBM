from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Any

import uvicorn
from fastapi import FastAPI
from jbm_cluster_py.common.banner import print_jbm_banner
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.health import build_health_router
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.nacos import NacosRegistrar
from jbm_cluster_py.integrations.rabbitmq import RabbitMQClient
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.logs.repository import LogsRepository
from jbm_cluster_py.platform.logs.router import build_logs_router
from jbm_cluster_py.platform.logs.service import BusinessLogService


def create_app(config: AppConfig | None = None) -> FastAPI:
    app_config = config or AppConfig.load(app="logs")
    configure_logging()
    print_jbm_banner()
    init_telemetry(app_config.telemetry)
    repository = LogsRepository(app_config.database)
    logs_config = dict(app_config.get("jbm.logs", {}) or {})
    service = BusinessLogService(
        repository, str(logs_config.get("signing-secret") or "jbm-local-business-log")
    )
    rabbit = RabbitMQClient(app_config.rabbitmq)

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config, app.state.repository, app.state.service = app_config, repository, service
        await repository.start()
        await rabbit.start()
        rabbit_settings = dict(logs_config.get("rabbitmq", {}) or {})
        queue = str(
            rabbit_settings.get("queue")
            or app_config.rabbitmq.get("business-log-queue")
            or "businessLog-in-0.jbm-cluster-platform-logs"
        )
        await rabbit.consume_json(queue, service.handle_event)
        await nacos.start()
        try:
            yield
        finally:
            await nacos.stop()
            await rabbit.stop()
            await repository.stop()

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Logs Service"),
        description=str(openapi.get("description") or "Persistent business log service."),
        version=str(openapi.get("version") or "0.1.0"),
        docs_url=str(openapi.get("docs-url") or "/docs"),
        redoc_url=str(openapi.get("redoc-url") or "/redoc"),
        openapi_url=str(openapi.get("openapi-url") or "/openapi.json"),
        lifespan=lifespan,
    )
    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_logs_router(repository, service))
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="logs")
    uvicorn.run(
        "jbm_cluster_py.platform.logs.main:app", host=config.host, port=config.port, reload=False
    )


if __name__ == "__main__":
    run()
