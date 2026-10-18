from __future__ import annotations

from collections.abc import Mapping
from typing import Any

import hashlib
import os
from contextlib import asynccontextmanager
from contextvars import ContextVar

import httpx
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from jbm_cluster_py.common.api_access import metadata_path, normalized_path

request_authorization: ContextVar[str] = ContextVar("jbm_request_authorization", default="")


class UserInfoAuthClient:
    def __init__(
        self,
        config: Mapping[str, Any],
        client: httpx.AsyncClient,
        *,
        default_public_paths: tuple[str, ...] = (),
    ) -> None:
        self.config = dict(config)
        self.client = client
        self.enabled = bool(self.config.get("enabled", True))
        self.userinfo_url = str(
            self.config.get("userinfo-url")
            or "http://jbm-cluster-platform-auth:5555/oauth2/userinfo"
        )
        paths = self.config.get("public-paths", default_public_paths)
        if not isinstance(paths, (tuple, list)) or any(
            not isinstance(p, str) or not p.startswith("/") or p == "/" or "*" in p
            for p in paths
        ):
            raise ValueError("匿名路径必须逐项声明，不能使用根路径或通配符")
        self.public_paths = tuple(p for p in paths if not metadata_path(p))
        self.document_paths: dict[str, str] = {}
        self.access_url = str(
            self.config.get("access-url")
            or os.getenv("JBM_API_ACCESS_URL")
            or "http://jbm-cluster-platform-center:7777/security/authorize"
        )
        self.access_checker = None

    def is_public(self, path: str) -> bool:
        if self.document_path(path) is not None:
            return False
        return any(
            path == item or (item.endswith("/") and path.startswith(item))
            for item in self.public_paths
        )

    def document_path(self, path: str) -> str | None:
        if metadata_path(path):
            return path
        for prefix, canonical in self.document_paths.items():
            if path.rstrip("/") == prefix or path.startswith(prefix + "/"):
                return canonical
        return None

    def bind_app(self, app: FastAPI, *, profile: str) -> None:
        """Keep configured documentation URLs inside the same non-overridable boundary."""
        if profile.lower() in {"prod", "production"} and not self.enabled:
            raise ValueError("生产环境不能关闭统一接口鉴权")
        for attribute, canonical in (
            ("openapi_url", "/openapi.json"), ("docs_url", "/docs"),
            ("redoc_url", "/redoc"), ("swagger_ui_oauth2_redirect_url", "/docs/oauth2-redirect"),
        ):
            path = getattr(app, attribute, None)
            if path:
                self.document_paths[normalized_path(path)] = canonical
        self.public_paths = tuple(p for p in self.public_paths if self.document_path(p) is None)

    async def authenticate(self, authorization: str | None) -> dict[str, Any]:
        if not self.enabled:
            return {
                "userId": int(self.config.get("dev-user-id") or 1),
                "appId": int(self.config.get("dev-app-id") or 1000),
                "tenantId": int(self.config.get("dev-tenant-id") or 1),
                "admin": bool(self.config.get("dev-admin", True)),
            }
        value = str(authorization or "").strip()
        if not value.lower().startswith("bearer "):
            raise PermissionError("未提供访问令牌")
        try:
            response = await self.client.get(
                self.userinfo_url,
                headers={"Authorization": value},
            )
            body = response.json()
        except (httpx.HTTPError, ValueError) as exc:
            raise ConnectionError("认证服务不可用") from exc
        if response.status_code >= 500:
            raise ConnectionError("认证服务不可用")
        identity = body.get("result") or body.get("data")
        if response.status_code >= 400 or not body.get("success") or not isinstance(identity, dict):
            raise PermissionError(str(body.get("message") or "访问令牌无效"))
        return identity

    async def authenticate_request(self, request: Request, service: str) -> dict[str, Any]:
        """Same Bearer contract for all terminals; API-key proofs use a separate credential path."""
        if not self.enabled:
            return await self.authenticate(request.headers.get("authorization"))
        cached = getattr(request.state, "jbm_access_identity", None)
        if cached is not None:
            return cached
        headers = request.headers
        tenants = {
            headers.get(k)
            for k in ("tenantId", "X-Tenant-Id", "X-Resource-Tenant-Id")
            if headers.get(k)
        }
        if len(tenants) > 1:
            raise HTTPException(403, "租户请求头不一致")
        authorization = headers.get("authorization", "")
        signed = bool(headers.get("X-Signature") or (headers.get("X-App-Id") and not authorization))
        if signed and authorization:
            raise HTTPException(401, "不能混用Bearer与API Key凭据")
        if not signed and not authorization:
            # Internal credentials only acquire service-only permissions, never impersonate a user.
            authorization = headers.get("X-Internal-Authorization", "")
        if not signed and not authorization.lower().startswith("bearer "):
            raise HTTPException(401, "未提供访问令牌")
        body = {
            "service": service,
            "method": request.method,
            "path": normalized_path(self.document_path(request.url.path) or request.url.path),
            "query": request.scope.get("query_string", b"").decode("ascii"),
            "tenant": headers.get("tenantId") or headers.get("X-Tenant-Id") or "",
            "project": headers.get("X-Project-Id") or "",
        }
        if signed:
            if headers.get("X-Signature-Version") != "2":
                raise HTTPException(401, "API Key请求必须使用带nonce的V2签名")
            raw = await request.body()
            body.update(
                key=headers.get("X-App-Id", ""),
                timestamp=headers.get("X-Timestamp", ""),
                nonce=headers.get("X-Nonce", ""),
                signature=headers.get("X-Signature", ""),
                bodyHash=hashlib.sha256(raw).hexdigest(),
            )
        try:
            if self.access_checker:
                result = await self.access_checker(body, authorization)
            else:
                response = await self.client.post(
                    self.access_url,
                    json=body,
                    headers={"Authorization": authorization} if authorization else {},
                )
                payload = response.json()
                if response.status_code in {401, 403}:
                    raise HTTPException(
                        response.status_code,
                        "访问凭据无效"
                        if response.status_code == 401
                        else "未获准访问此接口或执行此操作",
                    )
                if response.status_code >= 400:
                    raise HTTPException(503, "统一鉴权服务不可用")
                result = payload.get("result") or {}
            if result.get("allowed") is not True or not isinstance(result.get("identity"), dict):
                raise HTTPException(403, "未获准访问此接口")
            request.state.jbm_access_identity = result["identity"]
            return result["identity"]
        except (httpx.HTTPError, ValueError, ConnectionError):
            raise HTTPException(503, "统一鉴权服务不可用") from None


def install_api_protection(
    app: FastAPI,
    config: Any,
    *,
    public_paths: tuple[str, ...] = ("/health", "/actuator/health", "/actuator/live"),
    protected_prefixes: tuple[str, ...] = (),
) -> None:
    """Resource-service middleware; direct service requests receive the same checks as gateway requests."""
    client = httpx.AsyncClient(timeout=10, trust_env=False)
    auth = UserInfoAuthClient(
        dict(config.get("jbm.api-access", {}) or {}), client, default_public_paths=public_paths
    )
    auth.bind_app(app, profile=config.profile)

    previous_lifespan = app.router.lifespan_context

    @asynccontextmanager
    async def lifespan(application):
        async with previous_lifespan(application) as state:
            try:
                yield state
            finally:
                await client.aclose()

    app.router.lifespan_context = lifespan

    @app.middleware("http")
    async def protect(request: Request, call_next: Any):
        if auth.is_public(request.url.path) or (
            auth.document_path(request.url.path) is None
            and
            protected_prefixes
            and not any(
                request.url.path == p or request.url.path.startswith(p + "/")
                for p in protected_prefixes
            )
        ):
            return await call_next(request)
        try:
            request.state.identity = await auth.authenticate_request(request, config.service_name)
        except HTTPException as exc:
            return JSONResponse(
                status_code=exc.status_code,
                content={
                    "success": False,
                    "code": exc.status_code,
                    "message": exc.detail,
                    "result": None,
                },
            )
        return await call_next(request)


def identity_user_id(identity: Mapping[str, Any]) -> int:
    if not isinstance(identity, Mapping):
        raise PermissionError("无效的登录信息")
    for key in ("userId", "user_id", "sub"):
        value = identity.get(key)
        if value is None:
            continue
        try:
            return int(str(value).split("::", 1)[0])
        except ValueError:
            continue
    raise PermissionError("登录信息缺少 userId")


def install_bearer_openapi(app: FastAPI, public_paths: tuple[str, ...]) -> None:
    original_openapi = app.openapi

    def openapi() -> dict[str, Any]:
        schema = original_openapi()
        components = schema.setdefault("components", {})
        components.setdefault("securitySchemes", {})["bearerAuth"] = {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
        }
        components.setdefault("schemas", {})["JbmErrorResponse"] = {
            "type": "object",
            "required": ["code", "success", "message"],
            "properties": {
                "code": {"type": "integer"},
                "success": {"type": "boolean"},
                "message": {"type": "string"},
                "result": {},
            },
        }
        for path, path_item in schema.get("paths", {}).items():
            if any(
                path == item or (item.endswith("/") and path.startswith(item))
                for item in public_paths
            ):
                continue
            for method, operation in path_item.items():
                if method.lower() not in {"get", "post", "put", "patch", "delete"}:
                    continue
                operation.setdefault("security", [{"bearerAuth": []}])
                responses = operation.setdefault("responses", {})
                for status, description in (("401", "未认证"), ("403", "无权限")):
                    responses.setdefault(
                        status,
                        {
                            "description": description,
                            "content": {
                                "application/json": {
                                    "schema": {"$ref": "#/components/schemas/JbmErrorResponse"}
                                }
                            },
                        },
                    )
        return schema

    app.openapi = openapi
