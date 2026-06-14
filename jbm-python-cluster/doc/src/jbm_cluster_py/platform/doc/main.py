from __future__ import annotations

from contextlib import asynccontextmanager
import logging
from typing import Any

import uvicorn
from fastapi import FastAPI

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.health import build_health_router
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.nacos import NacosRegistrar
from jbm_cluster_py.integrations.storage import FilesystemStorage, build_storage
from jbm_cluster_py.platform.doc.repository import DocRepository
from jbm_cluster_py.platform.doc.router import build_doc_router
from jbm_cluster_py.platform.doc.service import DocService

logger = logging.getLogger(__name__)


def create_app(config: AppConfig | None = None) -> FastAPI:
    app_config = config or AppConfig.load(app="doc")
    configure_logging()
    repository = DocRepository(app_config.database)
    storage = build_storage(app_config.storage, app_config.minio)
    doc_service = DocService(app_config, repository, storage)

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config = app_config
        app.state.doc_service = doc_service
        app.state.nacos = nacos
        await repository.start()
        try:
            await storage.start()
        except Exception as exc:
            logger.warning("Doc storage startup failed, falling back to filesystem: %s", exc, exc_info=True)
            fallback = FilesystemStorage(app_config.storage.get("local-dir") or "./data/files")
            doc_service.storage = fallback
            await fallback.start()
        await nacos.start()
        try:
            yield
        finally:
            await nacos.stop()
            await doc_service.stop()

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Doc Service"),
        description=str(openapi.get("description") or "Python implementation for jbm-cluster-platform-doc."),
        version=str(openapi.get("version") or "0.1.0"),
        docs_url=str(openapi.get("docs-url") or "/docs"),
        redoc_url=str(openapi.get("redoc-url") or "/redoc"),
        openapi_url=str(openapi.get("openapi-url") or "/openapi.json"),
        lifespan=lifespan,
    )
    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_doc_router(doc_service))
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="doc")
    uvicorn.run(
        "jbm_cluster_py.platform.doc.main:app",
        host=config.host,
        port=config.port,
        reload=False,
    )


if __name__ == "__main__":
    run()
