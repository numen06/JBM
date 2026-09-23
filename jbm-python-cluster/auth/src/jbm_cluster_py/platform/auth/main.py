from __future__ import annotations

from contextlib import AsyncExitStack, asynccontextmanager
from typing import Any
from urllib.parse import urlsplit

import httpx
import uvicorn
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from sqlalchemy import text

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


def create_app(config: AppConfig | None = None) -> FastAPI:
    app_config = config or AppConfig.load(app="auth")
    configure_logging()
    print_jbm_banner()
    init_telemetry(app_config.telemetry)
    repository = AuthRepository(app_config.database)
    auth_config = dict(app_config.get("jbm.auth", {}) or {})
    _validate_production_auth_config(app_config.profile, auth_config)
    profile = str(app_config.profile).strip().lower()
    cache = TokenCache(
        RedisClient(app_config.redis),
        str(auth_config.get("cache-prefix") or "jbm:auth"),
        required=profile not in {"dev", "test", "local", "default"}
        or _flag(auth_config.get("require-redis", False)),
    )
    oidc_config = dict(auth_config.get("oidc") or {})
    oidc_enabled = _flag(oidc_config.get("enabled", False))
    discovery = NacosDiscoveryClient(app_config.nacos_discovery)
    http_client = httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=3.0), trust_env=False)
    auth_service = AuthService(
        repository, cache, auth_config, discovery=discovery, http_client=http_client
    )

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config = app_config
        app.state.repository = repository
        app.state.auth_service = auth_service
        app.state.discovery = discovery
        app.state.http_client = http_client
        app.state.nacos = nacos
        async with AsyncExitStack() as stack:
            stack.push_async_callback(http_client.aclose)
            stack.push_async_callback(repository.stop)
            await repository.start()
            if oidc_enabled:
                await repository.require_oidc_schema()
            stack.push_async_callback(cache.stop)
            await cache.start()
            stack.push_async_callback(discovery.stop)
            await discovery.start()
            stack.push_async_callback(nacos.stop)
            await nacos.start()
            yield

    openapi = app_config.openapi
    app = FastAPI(
        title=str(openapi.get("title") or "JBM Python Auth Service"),
        description=str(
            openapi.get("description") or "OAuth2/OIDC compatible auth service for JBM."
        ),
        version=str(openapi.get("version") or "0.1.0"),
        docs_url=str(openapi.get("docs-url") or "/docs"),
        redoc_url=str(openapi.get("redoc-url") or "/redoc"),
        openapi_url=str(openapi.get("openapi-url") or "/openapi.json"),
        lifespan=lifespan,
    )
    install_exception_handlers(app)
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_auth_router(auth_service))
    if oidc_enabled:
        from jbm_cluster_py.platform.auth.oidc_router import build_oidc_router

        oidc_router = build_oidc_router(auth_service, auth_config)
        app.include_router(oidc_router)
        app.state.oidc_service = oidc_router.oidc_service

    @app.get("/actuator/health/readiness", include_in_schema=False)
    async def readiness() -> JSONResponse:
        try:
            async with repository.engine.connect() as conn:
                await conn.execute(text("SELECT 1"))
            ready = await cache.readiness()
        except Exception:
            ready = False
        return JSONResponse(
            status_code=200 if ready else 503,
            content={"status": "UP" if ready else "DOWN"},
        )

    return app


def _flag(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"true", "1", "yes", "on"}


def _validate_production_auth_config(profile: str, config: dict[str, Any]) -> None:
    if str(profile).strip().lower() in {"dev", "test", "local", "default"}:
        return
    jwt = dict(config.get("jwt") or {})
    errors: list[str] = []
    if not _flag(config.get("require-pkce", True)):
        errors.append("require-pkce必须启用")
    if not _flag(config.get("require-https-redirects", True)):
        errors.append("require-https-redirects必须启用")
    if _flag(config.get("legacy-password-grant-enabled", False)):
        errors.append("legacy-password-grant-enabled必须关闭")
    if _flag(config.get("dev-bypass-enabled", False)):
        errors.append("dev-bypass-enabled必须关闭")
    if _flag(config.get("allow-plaintext-secrets", False)):
        errors.append("allow-plaintext-secrets必须关闭")
    if str(config.get("fixed-captcha-code") or "").strip():
        errors.append("fixed-captcha-code必须清空")
    if not str(jwt.get("private-key") or "").strip():
        errors.append("jwt.private-key必须由Nacos或环境密钥注入")
    if not str(jwt.get("issuer") or "").lower().startswith("https://"):
        errors.append("jwt.issuer必须使用HTTPS")
    oidc = dict(config.get("oidc") or {})
    if _flag(oidc.get("enabled", False)):
        issuer = urlsplit(str(oidc.get("issuer") or ""))
        if (
            issuer.scheme != "https"
            or not issuer.hostname
            or issuer.username
            or issuer.password
            or issuer.query
            or issuer.fragment
            or issuer.path != "/oidc"
        ):
            errors.append("oidc.issuer必须为HTTPS地址且路径为/oidc")
        if not _flag(oidc.get("cookie-secure", True)):
            errors.append("oidc.cookie-secure必须启用")
    for name, raw in dict(config.get("login-providers") or {}).items():
        provider = dict(raw or {})
        if _flag(provider.get("dev-mock-enabled", False)):
            errors.append(f"login-providers.{name}.dev-mock-enabled必须关闭")
        verify_url = str(provider.get("verify-url") or "")
        if (
            _flag(provider.get("enabled", False))
            and verify_url
            and not verify_url.lower().startswith("https://")
        ):
            errors.append(f"login-providers.{name}.verify-url必须使用HTTPS")
    if errors:
        raise RuntimeError("生产认证配置不安全: " + "；".join(errors))


app = create_app()


def run() -> None:
    config = AppConfig.load(app="auth")
    uvicorn.run(
        "jbm_cluster_py.platform.auth.main:app", host=config.host, port=config.port, reload=False
    )


if __name__ == "__main__":
    run()
