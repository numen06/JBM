from __future__ import annotations

from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any

import httpx
import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from jbm_cluster_py.common.auth import UserInfoAuthClient, install_bearer_openapi
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
        max_package_bytes=int(settings.get("max-package-bytes") or 100 * 1024 * 1024),
        max_unpacked_bytes=int(settings.get("max-unpacked-bytes") or 500 * 1024 * 1024),
        max_package_files=int(settings.get("max-package-files") or 5000),
    )
    auth_http_client = httpx.AsyncClient(
        timeout=httpx.Timeout(5.0, connect=3.0), trust_env=False
    )
    auth = UserInfoAuthClient(
        dict(app_config.get("jbm.bigscreen.security", {}) or {}),
        auth_http_client,
        default_public_paths=(
            "/actuator/health",
            "/health",
            "/docs",
            "/redoc",
            "/openapi.json",
            "/static/",
            "/view/",
        ),
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
            await auth_http_client.aclose()
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
    @app.middleware("http")
    async def authenticate(request: Request, call_next: Any):
        if auth.is_public(request.url.path):
            request.state.identity = {}
            return await call_next(request)
        try:
            request.state.identity = await auth.authenticate(
                request.headers.get("authorization")
            )
        except PermissionError as exc:
            return JSONResponse(
                status_code=401,
                content={"success": False, "code": 401, "message": str(exc), "result": None},
            )
        except ConnectionError:
            return JSONResponse(
                status_code=503,
                content={
                    "success": False,
                    "code": 503,
                    "message": "认证服务不可用",
                    "result": None,
                },
            )
        return await call_next(request)

    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_bigscreen_router(repository, service))
    Path(views_dir).mkdir(parents=True, exist_ok=True)
    app.mount("/static", StaticFiles(directory=views_dir), name="bigscreen-static")
    install_bearer_openapi(app, auth.public_paths)
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
