from __future__ import annotations

import asyncio
import json
import logging
import time
import uuid
from collections import deque
from datetime import datetime, timezone
from typing import Any, Mapping, Optional

import httpx
from fastapi import Request, WebSocket
from fastapi.responses import JSONResponse, Response, StreamingResponse
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.result import fail
from jbm_cluster_py.integrations.kafka import KafkaClient
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
from jbm_cluster_py.platform.gateway.circuit_breaker import CircuitBreakerRegistry
from jbm_cluster_py.platform.gateway.ip_limits import IpLimitRepository
from jbm_cluster_py.platform.gateway.routes import (
    GatewayRoute,
    RouteRepository,
    join_target_url,
    strip_prefix,
)
from jbm_cluster_py.platform.gateway.security import GatewaySecurityPolicy
from jbm_cluster_py.platform.gateway.traffic import TrafficPolicyManager

logger = logging.getLogger(__name__)

HOP_BY_HOP_HEADERS = {
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
    "host",
    "content-length",
}

WEBSOCKET_HANDSHAKE_HEADERS = {
    "sec-websocket-accept",
    "sec-websocket-extensions",
    "sec-websocket-key",
    "sec-websocket-protocol",
    "sec-websocket-version",
}


def websocket_forward_headers(headers: Mapping[str, str]) -> dict[str, str]:
    excluded = HOP_BY_HOP_HEADERS | WEBSOCKET_HANDSHAKE_HEADERS
    return {key: value for key, value in headers.items() if key.lower() not in excluded}


def websocket_subprotocols(headers: Mapping[str, str]) -> list[str]:
    header = next(
        (value for key, value in headers.items() if key.lower() == "sec-websocket-protocol"),
        "",
    )
    return [item.strip() for item in header.split(",") if item.strip()]


def utc_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def client_ip(request: Request) -> str:
    forwarded = request.headers.get("x-forwarded-for")
    if forwarded:
        return forwarded.split(",", 1)[0].strip()
    if request.client:
        return request.client.host
    return ""


def sanitize_headers(headers: Mapping[str, str]) -> dict[str, str]:
    redacted = {}
    for key, value in headers.items():
        lowered = key.lower()
        if lowered in {"authorization", "x-internal-authorization", "cookie", "set-cookie"}:
            redacted[key] = "***"
        else:
            redacted[key] = value
    return redacted


class ServiceTokenProvider:
    def __init__(
        self,
        config: Mapping[str, Any],
        discovery: NacosDiscoveryClient,
        service_name: str,
        http_client: httpx.AsyncClient,
    ) -> None:
        self.config = dict(config)
        self.discovery = discovery
        self.service_name = service_name
        self.http_client = http_client
        self._token: Optional[str] = str(self.config.get("value") or "").strip() or None
        self._expires_at = int(time.time()) + 3600 if self._token else 0
        self._lock = asyncio.Lock()

    async def get_token(self) -> Optional[str]:
        if self._token and int(time.time()) < self._expires_at - 60:
            return self._token
        async with self._lock:
            if self._token and int(time.time()) < self._expires_at - 60:
                return self._token
            self._token, self._expires_at = await self._fetch_token()
            return self._token

    async def _fetch_token(self) -> tuple[Optional[str], int]:
        service = str(self.config.get("service") or "jbm-cluster-platform-auth")
        path = str(self.config.get("path") or "/oauth2/token")
        client_id = str(self.config.get("client-id") or self.config.get("clientId") or "")
        client_secret = str(self.config.get("client-secret") or self.config.get("clientSecret") or "")
        scope = str(self.config.get("scope") or "internal")
        if not client_id or not client_secret:
            logger.warning("Cannot fetch service token: client-id/client-secret are empty")
            return None, 0
        instance = await self.discovery.choose_instance(service)
        if not instance:
            logger.warning("Cannot fetch service token: no Nacos instance for %s", service)
            return None, 0
        host = instance.get("ip") or instance.get("host")
        port = instance.get("port")
        if not host or not port:
            return None, 0
        url = "http://%s:%s%s" % (host, port, path)
        try:
            response = await self.http_client.post(
                url,
                data={
                    "grant_type": "client_credentials",
                    "client_id": client_id,
                    "client_secret": client_secret,
                    "scope": scope,
                },
                headers={"X-Internal-Service": self.service_name},
            )
            response.raise_for_status()
            payload = response.json()
        except Exception as exc:
            logger.warning("Failed to fetch service token from %s: %s", url, exc)
            return None, 0
        result = payload.get("result") if isinstance(payload, dict) else None
        if not isinstance(result, dict):
            result = payload.get("data") if isinstance(payload, dict) else None
        token = result.get("access_token") if isinstance(result, dict) else None
        expires_in = int(result.get("expires_in") or 7200) if isinstance(result, dict) else 0
        return (str(token).strip() if token else None), int(time.time()) + max(expires_in, 300)


class AccessLogger:
    def __init__(
        self,
        config: Mapping[str, Any],
        discovery: NacosDiscoveryClient,
        http_client: httpx.AsyncClient,
        kafka: KafkaClient | None = None,
    ) -> None:
        self.config = dict(config)
        self.enabled = bool(self.config.get("enabled", True))
        self.ingest_enabled = bool(self.config.get("ingest-enabled", True))
        self.discovery = discovery
        self.http_client = http_client
        self.recent: deque[dict[str, Any]] = deque(maxlen=int(self.config.get("recent-size") or 200))
        self.kafka = kafka
        self.kafka_topic = str(
            self.config.get("topic")
            or ((kafka.config.get("access-logs-topic") if kafka else None))
            or "jbm.logs.access.v1"
        )

    async def start(self) -> None:
        if self.kafka is not None and self.kafka.enabled:
            await self.kafka.start()

    async def stop(self) -> None:
        if self.kafka is not None:
            await self.kafka.stop()

    async def record(self, row: Mapping[str, Any]) -> None:
        if not self.enabled:
            return
        payload = dict(row)
        self.recent.appendleft(payload)
        logger.info("gateway_access %s", json.dumps(payload, ensure_ascii=False, default=str))
        if self.kafka is not None and self.kafka.enabled:
            try:
                await self.kafka.publish_json(
                    self.kafka_topic,
                    payload,
                    key=str(payload.get("accessId") or "") or None,
                )
                return
            except Exception:
                logger.exception("Kafka access-log publish failed; falling back to HTTP ingest")
        if self.ingest_enabled:
            asyncio.create_task(self._ingest(payload))

    async def _ingest(self, payload: Mapping[str, Any]) -> None:
        service = str(self.config.get("service") or "jbm-cluster-platform-logs")
        path = str(self.config.get("path") or "/GatewayLogs/ingest")
        instance = await self.discovery.choose_instance(service)
        if not instance:
            logger.debug("Skip gateway log ingest: no Nacos instance for %s", service)
            return
        host = instance.get("ip") or instance.get("host")
        port = instance.get("port")
        if not host or not port:
            return
        url = "http://%s:%s%s" % (host, port, path)
        try:
            response = await self.http_client.post(url, json=dict(payload), timeout=5.0)
            response.raise_for_status()
        except Exception as exc:
            logger.debug("Gateway log ingest failed: %s", exc)

    def snapshot(self) -> list[dict[str, Any]]:
        return list(self.recent)


class GatewayProxy:
    def __init__(
        self,
        config: AppConfig,
        routes: RouteRepository,
        ip_limits: IpLimitRepository,
        circuit_breakers: CircuitBreakerRegistry,
        traffic: TrafficPolicyManager,
        discovery: NacosDiscoveryClient,
        http_client: httpx.AsyncClient,
        service_tokens: ServiceTokenProvider,
        access_logger: AccessLogger,
    ) -> None:
        self.config = config
        self.routes = routes
        self.ip_limits = ip_limits
        self.circuit_breakers = circuit_breakers
        self.traffic = traffic
        self.discovery = discovery
        self.http_client = http_client
        self.service_tokens = service_tokens
        self.access_logger = access_logger
        self.security = GatewaySecurityPolicy.from_app_config(config)

    async def proxy_http(self, request: Request, path: str) -> Response:
        request_path = "/" + path if path else "/"
        route = self.routes.match(request_path)
        if route is None:
            await self._log_http(request, request_path, None, 404, 0, "route not found")
            return JSONResponse(status_code=404, content=fail(None, "网关路由不存在: %s" % request_path, 404))
        remote_ip = client_ip(request)
        ip_decision = self.ip_limits.evaluate(request_path, remote_ip)
        if not ip_decision.allowed:
            message = ip_decision.reason or "IP访问受限"
            await self._log_http(request, request_path, route, 403, 0, message)
            return JSONResponse(status_code=403, content=fail(None, message, 403))
        service_id = self._service_id(route)
        path_decision = self.traffic.evaluate_path(request_path, service_id, request.method)
        if not path_decision.allowed:
            message = path_decision.reason or "访问路径被限制"
            await self._log_http(request, request_path, route, 403, 0, message)
            return JSONResponse(status_code=403, content=fail(None, message, 403))
        circuit_key = self._circuit_key(route)
        circuit_decision = self.circuit_breakers.before_request(circuit_key)
        if not circuit_decision.allowed:
            message = circuit_decision.reason or "服务熔断中"
            await self._log_http(request, request_path, route, 503, 0, message)
            return JSONResponse(status_code=503, content=fail(None, message, 503))
        started = time.perf_counter()
        body = await request.body()
        body_decision = self.security.inspect_body(body, request.headers.get("content-type"))
        if not body_decision.allowed:
            await self._log_http(request, request_path, route, body_decision.status_code, 0, body_decision.reason)
            return JSONResponse(
                status_code=body_decision.status_code,
                content=fail(None, body_decision.reason, body_decision.status_code),
            )
        try:
            target = await self._target_url(
                route,
                request_path,
                request.url.query,
                request_headers=request.headers,
                remote_ip=remote_ip,
            )
            headers = await self._forward_headers(request, request_path)
            response = await self.http_client.request(
                request.method,
                target,
                content=body,
                headers=headers,
            )
            elapsed = time.perf_counter() - started
            self.circuit_breakers.after_request(circuit_key, status_code=response.status_code)
            await self._log_http(request, request_path, route, response.status_code, elapsed, None)
            return StreamingResponse(
                response.aiter_bytes(),
                status_code=response.status_code,
                headers=self._response_headers(response.headers),
                background=None,
            )
        except Exception as exc:
            elapsed = time.perf_counter() - started
            self.circuit_breakers.after_request(circuit_key, error=str(exc))
            await self._log_http(request, request_path, route, 502, elapsed, str(exc))
            logger.warning("Gateway proxy failed for %s via %s: %s", request_path, route.target_label, exc)
            return JSONResponse(status_code=502, content=fail(None, "网关转发失败: %s" % exc, 502))

    async def _target_url(
        self,
        route: GatewayRoute,
        request_path: str,
        query: str = "",
        request_headers: Optional[Mapping[str, str]] = None,
        remote_ip: str = "",
    ) -> str:
        stripped_path = strip_prefix(request_path, route.strip_prefix)
        service_id = self._service_id(route)
        if route.url and not route.url.startswith("lb://"):
            return join_target_url(route.url, stripped_path, query)
        if not service_id:
            raise ValueError("route has neither url nor serviceId")
        instances = await self.discovery.list_instances(service_id)
        instance = self.traffic.choose_instance(
            service_id,
            request_path,
            request_headers or {},
            remote_ip,
            instances,
        )
        if not instance:
            raise RuntimeError("No healthy Nacos instance for %s" % service_id)
        host = instance.get("ip") or instance.get("host")
        port = instance.get("port")
        if not host or not port:
            raise RuntimeError("Invalid Nacos instance for %s" % service_id)
        return join_target_url("http://%s:%s" % (host, port), stripped_path, query)

    def _service_id(self, route: GatewayRoute) -> Optional[str]:
        if route.url and route.url.startswith("lb://"):
            return route.url.replace("lb://", "", 1).strip("/") or route.service_id
        return route.service_id

    def _circuit_key(self, route: GatewayRoute) -> str:
        if route.url and route.url.startswith("lb://"):
            return route.url.replace("lb://", "", 1).strip("/") or route.target_label
        return route.service_id or route.target_label

    async def _forward_headers(self, request: Request, original_path: str) -> dict[str, str]:
        headers = {
            key: value
            for key, value in request.headers.items()
            if key.lower() not in HOP_BY_HOP_HEADERS
        }
        token = await self.service_tokens.get_token()
        if token:
            headers["X-Internal-Authorization"] = "Bearer " + token
        headers["X-Internal-Service"] = self.config.service_name
        headers["X-Internal-Instance"] = "%s:%s" % (self.config.service_name, self.config.port)
        headers["X-Original-Path"] = original_path
        if request.client:
            headers.setdefault("X-Forwarded-For", request.client.host)
        return headers

    def _response_headers(self, headers: Mapping[str, str]) -> dict[str, str]:
        return {
            key: value
            for key, value in headers.items()
            if key.lower() not in HOP_BY_HOP_HEADERS
        }

    async def _log_http(
        self,
        request: Request,
        path: str,
        route: Optional[GatewayRoute],
        status: int,
        elapsed: float,
        error: Optional[str],
    ) -> None:
        now = utc_iso()
        row = {
            "loglevel": 1,
            "accessId": uuid.uuid4().hex,
            "path": path,
            "method": request.method,
            "httpStatus": status,
            "useTime": int(round(elapsed * 1000)),
            "ip": client_ip(request),
            "serviceId": route.service_id if route else self.config.service_name,
            "requestTime": now,
            "responseTime": utc_iso(),
            "userAgent": request.headers.get("user-agent"),
            "headers": json.dumps(sanitize_headers(dict(request.headers)), ensure_ascii=False),
            "appId": request.headers.get("X-App-Id") or request.headers.get("x-app-id"),
            "error": error,
        }
        await self.access_logger.record(row)

    async def proxy_websocket(self, websocket: WebSocket, path: str) -> None:
        import websockets

        request_path = "/" + path if path else "/"
        security_decision = self.security.inspect_request_meta(
            request_path,
            str(websocket.url.query),
            websocket.headers,
            websocket.client.host if websocket.client else "",
        )
        if not security_decision.allowed:
            await websocket.close(code=1008)
            return
        route = self.routes.match(request_path)
        if route is None:
            await websocket.close(code=1008)
            return
        target = await self._target_url(
            route,
            request_path,
            str(websocket.url.query),
            request_headers=websocket.headers,
            remote_ip=websocket.client.host if websocket.client else "",
        )
        target = target.replace("http://", "ws://", 1).replace("https://", "wss://", 1)
        headers = websocket_forward_headers(websocket.headers)
        subprotocols = websocket_subprotocols(websocket.headers)
        token = await self.service_tokens.get_token()
        if token:
            headers["X-Internal-Authorization"] = "Bearer " + token
        headers["X-Internal-Service"] = self.config.service_name
        headers["X-Internal-Instance"] = "%s:%s" % (self.config.service_name, self.config.port)
        headers["X-Original-Path"] = request_path
        started = time.perf_counter()
        status = 101
        error = None
        try:
            async with websockets.connect(
                target,
                additional_headers=headers,
                subprotocols=subprotocols or None,
            ) as backend:
                await websocket.accept(subprotocol=backend.subprotocol)
                await asyncio.gather(
                    self._client_to_backend(websocket, backend),
                    self._backend_to_client(websocket, backend),
                )
        except Exception as exc:
            status = 502
            error = str(exc)
            logger.warning("Gateway websocket failed for %s via %s: %s", request_path, target, exc)
        finally:
            elapsed = time.perf_counter() - started
            row = {
                "loglevel": 1,
                "accessId": uuid.uuid4().hex,
                "path": request_path,
                "method": "WEBSOCKET",
                "httpStatus": status,
                "useTime": int(elapsed),
                "ip": websocket.client.host if websocket.client else "",
                "serviceId": route.service_id if route else self.config.service_name,
                "requestTime": utc_iso(),
                "responseTime": utc_iso(),
                "userAgent": websocket.headers.get("user-agent"),
                "error": error,
            }
            await self.access_logger.record(row)

    async def _client_to_backend(self, websocket: WebSocket, backend: Any) -> None:
        while True:
            message = await websocket.receive()
            if "text" in message:
                await backend.send(message["text"])
            elif "bytes" in message:
                await backend.send(message["bytes"])
            elif message.get("type") == "websocket.disconnect":
                await backend.close()
                return

    async def _backend_to_client(self, websocket: WebSocket, backend: Any) -> None:
        async for message in backend:
            if isinstance(message, bytes):
                await websocket.send_bytes(message)
            else:
                await websocket.send_text(str(message))
