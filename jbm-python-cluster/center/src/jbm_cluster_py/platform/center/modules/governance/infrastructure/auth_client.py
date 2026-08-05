from __future__ import annotations

from typing import Any, Mapping

import httpx
from fastapi import Request


class CenterAuthClient:
    def __init__(self, config: Mapping[str, Any], client: httpx.AsyncClient) -> None:
        self.config = dict(config)
        self.client = client
        self.enabled = bool(self.config.get("enabled", True))
        self.userinfo_url = str(
            self.config.get("userinfo-url") or "http://jbm-cluster-platform-auth:5555/oauth2/userinfo"
        )
        self.public_paths = tuple(
            self.config.get("public-paths")
            or ("/actuator/health", "/health", "/docs", "/redoc", "/openapi.json", "/internal/")
        )

    def is_public(self, path: str) -> bool:
        return any(path == item or (item.endswith("/") and path.startswith(item)) for item in self.public_paths)

    async def authenticate(self, request: Request) -> dict[str, Any]:
        if not self.enabled:
            return {
                "userId": int(self.config.get("dev-user-id") or 1),
                "appId": int(self.config.get("dev-app-id") or 1000),
                "admin": True,
            }
        authorization = request.headers.get("authorization", "").strip()
        if not authorization.lower().startswith("bearer "):
            raise PermissionError("未提供访问令牌")
        response = await self.client.get(self.userinfo_url, headers={"Authorization": authorization})
        body = response.json()
        identity = body.get("result") or body.get("data")
        if response.status_code >= 400 or not body.get("success") or not isinstance(identity, dict):
            raise PermissionError(str(body.get("message") or "访问令牌无效"))
        return identity
