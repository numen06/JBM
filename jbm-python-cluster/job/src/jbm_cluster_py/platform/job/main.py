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
from jbm_cluster_py.platform.job.router import build_job_router
from jbm_cluster_py.platform.job.service import JobService


def create_app(config: Optional[AppConfig] = None) -> FastAPI:
    app_config = config or AppConfig.load(app="job")
    configure_logging()
    init_telemetry(app_config.telemetry)
    job_service = JobService()

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config = app_config
        app.state.job_service = job_service
        app.state.nacos = nacos
        await nacos.start()
        try:
            yield
        finally:
            await nacos.stop()

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Job Service"),
        description=str(openapi.get("description") or "Python compatibility service for jbm-cluster-platform-job."),
        version=str(openapi.get("version") or "0.1.0"),
        docs_url=str(openapi.get("docs-url") or "/docs"),
        redoc_url=str(openapi.get("redoc-url") or "/redoc"),
        openapi_url=str(openapi.get("openapi-url") or "/openapi.json"),
        lifespan=lifespan,
    )
    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_job_router(job_service))
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="job")
    uvicorn.run("jbm_cluster_py.platform.job.main:app", host=config.host, port=config.port, reload=False)


if __name__ == "__main__":
    run()
