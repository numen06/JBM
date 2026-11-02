import asyncio
import logging
import os
from contextlib import asynccontextmanager, suppress
from typing import Any, Optional

import uvicorn
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from jbm_cluster_py.common.auth import install_api_protection

from jbm_cluster_py.common.banner import print_jbm_banner
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.errors import install_exception_handlers
from jbm_cluster_py.common.logging import configure_logging
from jbm_cluster_py.integrations.nacos import NacosRegistrar
from jbm_cluster_py.integrations.telemetry import init_telemetry
from jbm_cluster_py.platform.job.router import build_job_router
from jbm_cluster_py.platform.job.service import JobService
from jbm_cluster_py.platform.job.database_backups import DatabaseBackups
from jbm_cluster_py.platform.job.database_backup_router import build_database_backup_router

logger = logging.getLogger(__name__)


async def run_backup_runtime(app, root, database_config):
    """A rolling-update standby serves HTTP while waiting for the previous owner."""
    waiting = False
    while True:
        initializing = asyncio.create_task(asyncio.to_thread(DatabaseBackups, root, database_config))
        try:
            try:
                backups = await asyncio.shield(initializing)
            except asyncio.CancelledError:
                # An in-flight constructor may just have acquired the lock.
                with suppress(Exception):
                    backups = await initializing
                    backups.worker_lock.close()
                raise
        except BlockingIOError:
            app.state.database_backup_status = "waiting-for-owner"
            if not waiting:
                logger.info("Database backup worker waiting for the previous owner; retry in 2 seconds")
                waiting = True
            await asyncio.sleep(2)
            continue
        except Exception:
            app.state.database_backup_status = "failed"
            logger.exception("Database backup worker initialization failed")
            return  # Health reports 503; the stack restart policy handles startup failure.
        app.state.database_backups = backups
        app.state.database_backup_status = "ready"
        logger.info("Database backup worker acquired ownership")
        try:
            await asyncio.to_thread(backups.test_source, "jbm-default")
            await backups.worker()
        finally:
            app.state.database_backups = None
            app.state.database_backup_status = "stopped"
            backups.worker_lock.close()
        return


def create_app(config: Optional[AppConfig] = None) -> FastAPI:
    app_config = config or AppConfig.load(app="job")
    configure_logging()
    print_jbm_banner()
    init_telemetry(app_config.telemetry)
    job_service = JobService()

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> Any:
        nacos = NacosRegistrar(app_config.service_name, app_config.port, app_config.nacos_discovery)
        app.state.config = app_config
        app.state.job_service = job_service
        app.state.nacos = nacos
        database_config = AppConfig.load(profile=app_config.profile, app="center").database
        app.state.database_backups = None
        app.state.database_backup_status = "starting"
        backup_worker = asyncio.create_task(run_backup_runtime(
            app, os.getenv("JBM_DB_BACKUP_ROOT", "/app/data/database-backups"), database_config))
        await nacos.start()
        try:
            yield
        finally:
            backup_worker.cancel()
            with suppress(asyncio.CancelledError):
                await backup_worker
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
    install_api_protection(app, app_config)
    @app.get("/actuator/health")
    async def health():
        phase = getattr(app.state, "database_backup_status", "starting")
        failed = phase in {"failed", "stopped"}
        return JSONResponse({"status": "DOWN" if failed else "UP", "components": {
            "service": {"status": "UP", "details": {"name": app_config.service_name, "profile": app_config.profile}},
            "databaseBackupWorker": {"status": "DOWN" if failed else "UP" if phase == "ready" else "WAITING",
                                     "details": {"phase": phase}},
        }}, status_code=503 if failed else 200)

    @app.get("/actuator/ready")
    async def ready():
        phase = getattr(app.state, "database_backup_status", "starting")
        return JSONResponse({"status": "UP" if phase == "ready" else "OUT_OF_SERVICE", "phase": phase},
                            status_code=200 if phase == "ready" else 503)

    app.include_router(build_job_router(job_service))
    app.include_router(build_database_backup_router())
    return app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="job")
    uvicorn.run("jbm_cluster_py.platform.job.main:app", host=config.host, port=config.port, reload=False)


if __name__ == "__main__":
    run()
