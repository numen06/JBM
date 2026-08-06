from __future__ import annotations

from collections.abc import Mapping
from typing import Any

import httpx
from fastapi import FastAPI


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
        self.public_paths = tuple(self.config.get("public-paths") or default_public_paths)

    def is_public(self, path: str) -> bool:
        return any(
            path == item or (item.endswith("/") and path.startswith(item))
            for item in self.public_paths
        )

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
                                    "schema": {
                                        "$ref": "#/components/schemas/JbmErrorResponse"
                                    }
                                }
                            },
                        },
                    )
        return schema

    app.openapi = openapi
