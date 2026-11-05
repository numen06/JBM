from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Any

import uvicorn
from fastapi import FastAPI
from jbm_cluster_py.common.auth import install_api_protection
from jbm_cluster_py.common.banner import print_jbm_banner
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.health import build_health_router
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.kafka import KafkaClient
from jbm_cluster_py.integrations.nacos import NacosRegistrar
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.logs.loki import LokiSink
from jbm_cluster_py.platform.logs.repository import LogsRepository
from jbm_cluster_py.platform.logs.router import build_logs_router
from jbm_cluster_py.platform.logs.service import BusinessLogService, GatewayLogIngestService
from jbm_cluster_py.platform.logs.security_audit import SecurityAuditStore, audit_reader_scope, audit_query_scope
from jbm_cluster_py.common.security_audit import AUDIT_TOPIC
from fastapi import Body, HTTPException, Request
from jbm_cluster_py.common.result import ok


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
    kafka = KafkaClient(app_config.kafka)
    loki = LokiSink(logs_config.get("loki", {}) or {})
    access_ingest = GatewayLogIngestService(repository, loki)
    security_audit = SecurityAuditStore({**dict(app_config.get("jbm.security-audit", {}) or {}), **dict(logs_config.get("security-audit", {}) or {})}, loki=loki, outbox_engine=repository.engine)
    access_ingest.security_audit = security_audit

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config, app.state.repository, app.state.service = app_config, repository, service
        app.state.kafka, app.state.loki = kafka, loki
        await repository.start()
        await security_audit.start()
        await loki.start()
        access_topic = str(
            app_config.kafka.get("access-logs-topic") or "jbm.logs.access.v1"
        )
        business_topic = str(
            app_config.kafka.get("business-logs-topic") or "jbm.logs.business.v1"
        )
        group = str(app_config.kafka.get("consumer-group") or "jbm-cluster-platform-logs")
        await kafka.consume_json(access_topic, group + "-access", access_ingest.handle)
        await kafka.consume_json(business_topic, group + "-business", service.handle_event)
        if security_audit.enabled:
            await kafka.consume_json(AUDIT_TOPIC, group + "-security-audit", security_audit.handle)
        await nacos.start()
        try:
            yield
        finally:
            await nacos.stop()
            await kafka.stop()
            await loki.stop()
            await security_audit.stop()
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
    # Temporary downloads/access already require the log-bound expiring signature.
    install_api_protection(app, app_config, public_paths=("/health", "/actuator/health", "/businessLog/download/", "/businessLog/access/"))
    security_audit.publisher_health = app.state.operation_audit.publisher_health
    security_audit.archive_ack = app.state.operation_audit.acknowledge
    app.include_router(build_health_router(app_config.service_name, app_config.profile))
    app.include_router(build_logs_router(repository, service, access_ingest))

    def reader(request):
        actor = getattr(request.state, "identity", {})
        scope = audit_reader_scope(actor, request.headers)
        if not security_audit.enabled:
            raise HTTPException(503, "安全审计归档尚未启用")
        return scope

    @app.post("/securityAudit/query", tags=["安全审计"])
    async def query_security_audit(request: Request, body: dict = Body(default_factory=dict)):
        scope = reader(request)
        return ok(await security_audit.query(audit_query_scope(body, scope)))

    @app.post("/loginLogs/pageList", tags=["登录日志"])
    async def query_login_logs(request: Request, body: dict = Body(default_factory=dict)):
        reader(request)
        return ok(await security_audit.login_page(body))

    @app.get("/securityAudit/status", tags=["安全审计"])
    async def security_audit_status(request: Request):
        scope = reader(request)
        status = await security_audit.health(scope)
        if not status["healthy"]:
            raise HTTPException(503, "日志归档存在延迟或校验异常，请检查日志服务")
        return ok(status)
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="logs")
    uvicorn.run(
        "jbm_cluster_py.platform.logs.main:app", host=config.host, port=config.port, reload=False
    )


if __name__ == "__main__":
    run()
