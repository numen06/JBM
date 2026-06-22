from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Any, Optional

import uvicorn
import httpx
from fastapi import FastAPI

from jbm_cluster_py.common.banner import print_jbm_banner
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.health import build_health_router
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient, NacosRegistrar
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.auth.repository import AuthRepository
from jbm_cluster_py.platform.auth.router import build_auth_router
from jbm_cluster_py.platform.auth.service import AuthService, TokenCache


def create_app(config: Optional[AppConfig] = None) -> FastAPI:
    app_config = config or AppConfig.load(app="auth")
    configure_logging()
    print_jbm_banner()
    init_telemetry(app_config.telemetry)
    repository = AuthRepository(app_config.database)
    auth_config = dict(app_config.get("jbm.auth", {}) or {})
    cache = TokenCache(RedisClient(app_config.redis), str(auth_config.get("cache-prefix") or "jbm:auth"))
    discovery = NacosDiscoveryClient(app_config.nacos_discovery)
    http_client = httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=3.0), trust_env=False)
    auth_service = AuthService(repository, cache, auth_config, discovery=discovery, http_client=http_client)

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config = app_config
        app.state.repository = repository
        app.state.auth_service = auth_service
        app.state.discovery = discovery
        app.state.http_client = http_client
        app.state.nacos = nacos
        await repository.start()
        await cache.start()
        await discovery.start()
        await nacos.start()
        try:
            yield
        finally:
            await nacos.stop()
            await discovery.stop()
            await http_client.aclose()
            await cache.stop()
            await repository.stop()

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Auth Service"),
        description=str(openapi.get("description") or "OAuth2/OIDC compatible auth service for JBM."),
        version=str(openapi.get("version") or "0.1.0"),
        docs_url=str(openapi.get("docs-url") or "/docs"),
        redoc_url=str(openapi.get("redoc-url") or "/redoc"),
        openapi_url=str(openapi.get("openapi-url") or "/openapi.json"),
        lifespan=lifespan,
    )
    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_auth_router(auth_service))
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="auth")
    uvicorn.run("jbm_cluster_py.platform.auth.main:app", host=config.host, port=config.port, reload=False)


if __name__ == "__main__":
    run()
