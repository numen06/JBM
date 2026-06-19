from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Any, Optional

import httpx
import uvicorn
from fastapi import FastAPI

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.health import build_health_router
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient, NacosRegistrar
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.gateway.circuit_breaker import CircuitBreakerRegistry
from jbm_cluster_py.platform.gateway.ip_limits import IpLimitRepository
from jbm_cluster_py.platform.gateway.router import build_gateway_router
from jbm_cluster_py.platform.gateway.routes import RouteRepository
from jbm_cluster_py.platform.gateway.service import AccessLogger, GatewayProxy, TrustTokenProvider
from jbm_cluster_py.platform.gateway.traffic import TrafficPolicyManager


def create_app(config: Optional[AppConfig] = None) -> FastAPI:
    app_config = config or AppConfig.load(app="gateway")
    configure_logging()
    init_telemetry(app_config.telemetry)

    gateway_config = dict(app_config.get("jbm.gateway", {}) or {})
    timeout = float(gateway_config.get("request-timeout-seconds") or 60)
    http_client = httpx.AsyncClient(timeout=httpx.Timeout(timeout, connect=10.0), trust_env=False)
    discovery = NacosDiscoveryClient(app_config.nacos_discovery)
    registrar = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
    routes = RouteRepository(app_config.database, list(gateway_config.get("fallback-routes") or []))
    ip_limits = IpLimitRepository(app_config.database)
    circuit_breakers = CircuitBreakerRegistry(gateway_config.get("circuit-breaker") or {})
    traffic = TrafficPolicyManager(
        list(gateway_config.get("path-blacklist") or []),
        list(gateway_config.get("gray-routes") or []),
    )
    trust_tokens = TrustTokenProvider(
        gateway_config.get("trust-token") or {},
        discovery,
        app_config.service_name,
        http_client,
    )
    access_logger = AccessLogger(
        gateway_config.get("access-log") or {},
        discovery,
        http_client,
    )
    proxy = GatewayProxy(
        app_config,
        routes,
        ip_limits,
        circuit_breakers,
        traffic,
        discovery,
        http_client,
        trust_tokens,
        access_logger,
    )

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        app.state.config = app_config
        app.state.discovery = discovery
        app.state.nacos = registrar
        app.state.routes = routes
        app.state.ip_limits = ip_limits
        app.state.circuit_breakers = circuit_breakers
        app.state.traffic = traffic
        app.state.proxy = proxy
        app.state.access_logger = access_logger
        await discovery.start()
        await routes.start()
        await ip_limits.start(routes.routes)
        await registrar.start()
        try:
            yield
        finally:
            await registrar.stop()
            await ip_limits.stop()
            await routes.stop()
            await discovery.stop()
            await http_client.aclose()

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Gateway"),
        description=str(openapi.get("description") or "Python gateway sidecar."),
        version=str(openapi.get("version") or "0.1.0"),
        docs_url=str(openapi.get("docs-url") or "/docs"),
        redoc_url=str(openapi.get("redoc-url") or "/redoc"),
        openapi_url=str(openapi.get("openapi-url") or "/openapi.json"),
        lifespan=lifespan,
    )
    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_gateway_router(proxy, routes, ip_limits, circuit_breakers, traffic, access_logger))
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="gateway")
    uvicorn.run(
        "jbm_cluster_py.platform.gateway.main:app",
        host=config.host,
        port=config.port,
        reload=False,
    )


if __name__ == "__main__":
    run()
