from __future__ import annotations

from contextlib import asynccontextmanager
import json
import logging
from typing import Any

import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import Response
from sqlalchemy import text
from jbm_cluster_py.common.auth import install_api_protection

from jbm_cluster_py.common.banner import print_jbm_banner
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
    print_jbm_banner()
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
    # Scoped Doc-Token-Key operations retain their own capability verification in DocService.
    install_api_protection(app, app_config, public_paths=("/health", "/actuator/health", "/baseDocGroup/findGroupItemByToken", "/baseDocGroup/removeGroupItemByToken", "/baseDocGroup/uploadByToken"))

    @app.middleware("http")
    async def public_login_image(request: Request, call_next: Any):
        # Login images must load before sign-in. Only the exact images referenced
        # by existing public application branding are public, never the directory.
        prefixes = ("/get/app-branding/", "/get/login-branding/")
        if request.method != "GET" or not request.url.path.startswith(prefixes):
            return await call_next(request)
        key = request.url.path[len("/get/"):]
        if any(part in {"", ".", ".."} for part in key.split("/")) or any(
            c in key for c in ("\\", "%", "\x00", "\r", "\n")
        ):
            return await call_next(request)
        try:
            async with repository.engine.connect() as conn:
                rows = (await conn.execute(text(
                    "SELECT config_content FROM base_app_config "
                    "WHERE config_content LIKE :branding"
                ), {"branding": "%-branding/%"})).scalars().all()
            configured = False
            for raw in rows:
                try:
                    branding = json.loads(raw) if isinstance(raw, str) else raw
                except ValueError:
                    continue
                if not isinstance(branding, dict):
                    continue
                for field in ("sysBg", "sysLogo"):
                    value = str(branding.get(field) or "").strip()
                    for root in ("/jbm-api/doc/get/", "/doc/get/"):
                        if value.startswith(root):
                            value = value[len(root):]
                            break
                    configured |= value == key
            if not configured:
                return await call_next(request)
            obj = await doc_service.storage.get_object(key)
            body = obj.body
            image_type = (
                "image/png" if body.startswith(b"\x89PNG\r\n\x1a\n") else
                "image/jpeg" if body.startswith(b"\xff\xd8\xff") else
                "image/gif" if body.startswith((b"GIF87a", b"GIF89a")) else
                "image/webp" if body.startswith(b"RIFF") and body[8:12] == b"WEBP" else None
            )
            if image_type is None:
                return await call_next(request)
            return Response(body, media_type=image_type, headers={
                "Cache-Control": "public, max-age=300",
                "X-Content-Type-Options": "nosniff",
            })
        except Exception:
            logger.exception("Unable to read configured public login image")
            return Response(status_code=503)

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
