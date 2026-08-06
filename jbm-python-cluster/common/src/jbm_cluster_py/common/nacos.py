from __future__ import annotations

import json
import threading
import time
from collections.abc import Mapping
from typing import Any
from urllib.parse import urlencode
from urllib.request import ProxyHandler, Request, build_opener


def create_nacos_client(
    nacos: Any,
    server_addr: str,
    namespace: str,
    config: Mapping[str, Any],
) -> Any:
    client = nacos.NacosClient(server_addresses=server_addr, namespace=namespace)
    username = str(config.get("username") or "")
    password = str(config.get("password") or "")
    if not (username and password):
        return client

    server = server_addr.split(",", 1)[0].strip().rstrip("/")
    if not server.startswith(("http://", "https://")):
        server = "http://" + server
    lock = threading.Lock()
    access_token = ""
    expires_at = 0.0

    def token() -> str:
        nonlocal access_token, expires_at
        with lock:
            if access_token and time.time() < expires_at - 60:
                return access_token
            request = Request(
                server + "/nacos/v1/auth/login",
                data=urlencode({"username": username, "password": password}).encode(),
                method="POST",
            )
            with build_opener(ProxyHandler({})).open(request, timeout=5) as response:
                body = json.loads(response.read().decode("utf-8"))
            access_token = str(body.get("accessToken") or "")
            if not access_token:
                raise RuntimeError("Nacos login did not return an access token")
            expires_at = time.time() + max(int(body.get("tokenTtl") or 3600), 60)
            return access_token

    def inject_auth_info(
        headers: dict[str, Any],
        params: dict[str, Any] | None,
        data: dict[str, Any] | None,
        module: str = "config",
    ) -> None:
        target = params if params is not None else data
        if target is not None:
            target["accessToken"] = token()

    client._inject_auth_info = inject_auth_info
    return client
