from __future__ import annotations

from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any

import uvicorn
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from jbm_cluster_py.common.banner import print_jbm_banner
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.health import build_health_router
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.nacos import NacosRegistrar
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository
from jbm_cluster_py.platform.bigscreen.router import build_bigscreen_router
from jbm_cluster_py.platform.bigscreen.service import BigscreenService


def create_app(config: AppConfig | None = None) -> FastAPI:
    app_config = config or AppConfig.load(app="bigscreen")
    configure_logging()
    print_jbm_banner()
    init_telemetry(app_config.telemetry)
    settings = dict(app_config.get("jbm.bigscreen", {}) or {})
    views_dir = str(settings.get("views-dir") or "./data/views")
    repository = BigscreenRepository(app_config.database)
    service = BigscreenService(
        repository,
        views_dir,
        str(settings.get("doc-base-url") or "http://jbm-cluster-platform-doc:9999"),
    )

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config, app.state.repository, app.state.service = app_config, repository, service
        await service.start()
        await nacos.start()
        await service.load_all()
        try:
            yield
        finally:
            await nacos.stop()
            await service.stop()

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Bigscreen Service"),
        description=str(openapi.get("description") or "Bigscreen package deployment and preview."),
        version=str(openapi.get("version") or "0.1.0"),
        docs_url=str(openapi.get("docs-url") or "/docs"),
        redoc_url=str(openapi.get("redoc-url") or "/redoc"),
        openapi_url=str(openapi.get("openapi-url") or "/openapi.json"),
        lifespan=lifespan,
    )
    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_bigscreen_router(repository, service))
    Path(views_dir).mkdir(parents=True, exist_ok=True)
    app.mount("/static", StaticFiles(directory=views_dir), name="bigscreen-static")
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="bigscreen")
    uvicorn.run(
        "jbm_cluster_py.platform.bigscreen.main:app",
        host=config.host,
        port=config.port,
        reload=False,
    )


if __name__ == "__main__":
    run()
