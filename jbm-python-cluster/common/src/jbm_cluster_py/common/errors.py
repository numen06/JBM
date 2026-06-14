import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from jbm_cluster_py.common.result import fail

logger = logging.getLogger(__name__)


def install_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(Exception)
    async def handle_unexpected_error(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("Unhandled request error: %s %s", request.method, request.url.path)
        return JSONResponse(status_code=500, content=fail(None, "服务异常: %s" % exc))
