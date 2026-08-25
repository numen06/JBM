from __future__ import annotations

from typing import Any, Mapping

import httpx
from fastapi import Request

from jbm_cluster_py.common.auth import UserInfoAuthClient


class CenterAuthClient(UserInfoAuthClient):
    def __init__(self, config: Mapping[str, Any], client: httpx.AsyncClient) -> None:
        super().__init__(
            config,
            client,
            default_public_paths=(
                "/actuator/health",
                "/health",
                "/docs",
                "/redoc",
                "/openapi.json",
                "/captcha/pkey",
                "/baseAppConfig/getAppConfigByKey",
            ),
        )

    async def authenticate(self, request: Request) -> dict[str, Any]:
        return await super().authenticate(request.headers.get("authorization"))
