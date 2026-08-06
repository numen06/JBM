from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Any

import httpx
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from jbm_cluster_py.common.auth import install_bearer_openapi
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.common.result import fail
from jbm_cluster_py.integrations.nacos import NacosRegistrar
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.center.modules.governance.api.router import build_governance_router
from jbm_cluster_py.platform.center.modules.governance.api.compatibility_router import build_compatibility_router
from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import CompatibilityService
from jbm_cluster_py.platform.center.modules.governance.application.service import GovernanceService
from jbm_cluster_py.platform.center.modules.governance.infrastructure.auth_client import CenterAuthClient
from jbm_cluster_py.platform.center.modules.governance.infrastructure.repository import (
    SqlGovernanceRepository,
)
from jbm_cluster_py.platform.center.modules.governance.infrastructure.crud_store import CrudStore
from jbm_cluster_py.platform.center.modules.governance.infrastructure.openapi_catalog import OpenApiCatalog


def create_app(config: AppConfig | None = None) -> FastAPI:
    app_config = config or AppConfig.load(app="center")
    configure_logging()
    init_telemetry(app_config.telemetry)
    repository = SqlGovernanceRepository(app_config.database)
    service = GovernanceService(repository)
    store = CrudStore(repository.engine)
    client = httpx.AsyncClient(timeout=httpx.Timeout(30.0, connect=5.0), trust_env=False)
    discovery = NacosDiscoveryClient(app_config.nacos_discovery)
    compatibility = CompatibilityService(store, service, OpenApiCatalog(store, discovery, client))
    auth = CenterAuthClient(dict(app_config.get("jbm.center.security", {}) or {}), client)
    registrar = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        app.state.config = app_config
        app.state.repository = repository
        app.state.service = service
        await repository.start()
        await discovery.start()
        await registrar.start()
        try:
            yield
        finally:
            await registrar.stop()
            await discovery.stop()
            await repository.stop()
            await client.aclose()

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Center"),
        description=str(openapi.get("description") or "Incremental Python replacement for JBM Center."),
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
            request.state.identity = await auth.authenticate(request)
        except PermissionError as exc:
            return JSONResponse(status_code=401, content=fail(None, str(exc), 401))
        except ConnectionError:
            return JSONResponse(
                status_code=503,
                content=fail(None, "认证服务不可用", 503),
            )
        return await call_next(request)

    install_exception_handlers(app)

    @app.get("/actuator/health")
    async def health() -> dict[str, Any]:
        database = await repository.health()
        return {
            "status": "UP" if database.get("ok") else "DOWN",
            "components": {"service": app_config.service_name, "database": database},
        }

    app.include_router(build_governance_router(service))
    app.include_router(build_compatibility_router(compatibility))
    install_bearer_openapi(app, auth.public_paths)
    return app
