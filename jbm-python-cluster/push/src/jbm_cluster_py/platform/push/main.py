from contextlib import asynccontextmanager
from typing import Any, Optional

import uvicorn
from fastapi import FastAPI

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.health import build_health_router
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.nacos import NacosRegistrar
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.push.router import build_push_router
from jbm_cluster_py.platform.push.service import PushService


def create_app(config: Optional[AppConfig] = None) -> FastAPI:
    app_config = config or AppConfig.load(app="push")
    configure_logging()
    init_telemetry(app_config.telemetry)
    push_service = PushService()

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config = app_config
        app.state.push_service = push_service
        app.state.nacos = nacos
        await nacos.start()
        try:
            yield
        finally:
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
    app.include_router(build_push_router(push_service))
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="push")
    uvicorn.run("jbm_cluster_py.platform.push.main:app", host=config.host, port=config.port, reload=False)


if __name__ == "__main__":
    run()
