import logging
import re
import time
import uuid

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from jbm_cluster_py.common.result import fail

logger = logging.getLogger(__name__)
REQUEST_ID_PATTERN = re.compile(r"^[A-Za-z0-9._:-]{1,128}$")


def install_exception_handlers(app: FastAPI) -> None:
    @app.middleware("http")
    async def request_context(request: Request, call_next):
        supplied_request_id = request.headers.get("X-Request-ID", "")
        request_id = (
            supplied_request_id
            if REQUEST_ID_PATTERN.fullmatch(supplied_request_id)
            else uuid.uuid4().hex
        )
        request.state.request_id = request_id
        started = time.perf_counter()
        response_value = await call_next(request)
        duration_ms = (time.perf_counter() - started) * 1000
        response_value.headers["X-Request-ID"] = request_id
        response_value.headers["Server-Timing"] = f"app;dur={duration_ms:.2f}"
        if duration_ms >= 3000:
            logger.warning(
                "Slow request: method=%s path=%s status=%s duration_ms=%.2f request_id=%s",
                request.method,
                request.url.path,
                response_value.status_code,
                duration_ms,
                request_id,
            )
        return response_value

    def response(
        request: Request,
        status: int,
        message: str,
        headers: dict[str, str] | None = None,
    ) -> JSONResponse:
        if request.url.path.startswith("/v1/"):
            return JSONResponse(
                status_code=status,
                media_type="application/problem+json",
                headers=headers,
                content={
                    "type": "about:blank",
                    "title": message,
                    "status": status,
                    "detail": message,
                    "instance": request.url.path,
                    "requestId": getattr(request.state, "request_id", None),
                },
            )
        return JSONResponse(
            status_code=status,
            content=fail(None, message, status),
            headers=headers,
        )

    @app.exception_handler(HTTPException)
    async def handle_http_error(request: Request, exc: HTTPException) -> JSONResponse:
        return response(
            request,
            exc.status_code,
            str(exc.detail or "请求失败"),
            dict(exc.headers or {}),
        )

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(
        request: Request, exc: RequestValidationError
    ) -> JSONResponse:
        message = "; ".join(
            str(item.get("msg") or "字段校验失败") for item in exc.errors()
        )
        return response(request, 422, message or "字段校验失败")

    @app.exception_handler(ValueError)
    async def handle_value_error(request: Request, exc: ValueError) -> JSONResponse:
        message = str(exc).strip() or "参数错误"
        return response(request, 400, message)

    @app.exception_handler(Exception)
    async def handle_unexpected_error(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("Unhandled request error: %s %s", request.method, request.url.path)
        return response(request, 500, "服务异常")
