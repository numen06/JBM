# -*- coding: utf-8 -*-
"""JBM jaja7 集群：HTTP 客户端、登录、健康检查（供 ops / 测试脚本复用）。"""
from __future__ import annotations

import json
import os
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Optional

from jbm_rest_profile import REST_PROFILE

_NO_PROXY = urllib.request.build_opener(urllib.request.ProxyHandler({}))

DEFAULT_GATEWAY = os.environ.get("JBM_GATEWAY", "http://127.0.0.1:6060").rstrip("/")
DEFAULT_AUTH = os.environ.get("JBM_AUTH", "http://127.0.0.1:5555").rstrip("/")
DEFAULT_CENTER = os.environ.get("JBM_CENTER", "http://127.0.0.1:7777").rstrip("/")
DEFAULT_CLIENT_ID = os.environ.get("OAUTH_CLIENT_ID", "jbmSeedDevAppKey00000001")
DEFAULT_CLIENT_SECRET = os.environ.get("OAUTH_CLIENT_SECRET", "jbmSeedDevSecret0000000001")
DEFAULT_TENANT = os.environ.get("JBM_TENANT_ID", "0")


def http_request(
    method: str,
    url: str,
    headers: Optional[dict] = None,
    body: Any = None,
    *,
    form: bool = False,
    timeout: int = 45,
) -> tuple[int, str]:
    h = dict(headers or {})
    h.setdefault("tenantId", DEFAULT_TENANT)
    data = None
    if body is not None:
        if form:
            data = urllib.parse.urlencode(body).encode("utf-8") if isinstance(body, dict) else body.encode("utf-8")
            h["Content-Type"] = "application/x-www-form-urlencoded"
        elif isinstance(body, (bytes, bytearray)):
            data = body
        elif isinstance(body, str):
            data = body.encode("utf-8")
        else:
            data = json.dumps(body).encode("utf-8")
            h["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with _NO_PROXY.open(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace") if e.fp else ""
        return e.code, raw
    except Exception as e:
        return 0, str(e)


def parse_json(raw: str) -> Optional[dict]:
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return None


def unwrap(jb: Optional[dict]) -> Any:
    if not jb:
        raise RuntimeError("empty response")
    if jb.get("success") is False and jb.get("code") not in (200, None):
        raise RuntimeError(jb.get("message") or json.dumps(jb, ensure_ascii=False)[:400])
    return jb.get("result")


def gateway_api(method: str, path: str, token: str = "", body: Any = None, *, form: bool = False):
    url = DEFAULT_GATEWAY + (path if path.startswith("/") else "/" + path)
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}" if not token.startswith("Bearer ") else token
    status, raw = http_request(method, url, headers, body, form=form)
    jb = parse_json(raw)
    if status >= 400 and not jb:
        raise RuntimeError(f"HTTP {status}: {raw[:300]}")
    return status, jb, raw


def login_password(username: str, password: str, *, gateway: str = "") -> str:
    if gateway:
        # 直连指定 Gateway 根地址
        url = gateway.rstrip("/") + "/oauth2/token"
        headers = {"tenantId": DEFAULT_TENANT}
        status, raw = http_request(
            "POST",
            url,
            headers,
            {
                "grant_type": "password",
                "client_id": DEFAULT_CLIENT_ID,
                "client_secret": DEFAULT_CLIENT_SECRET,
                "username": username,
                "password": password,
                "scope": "all",
                "loginType": "PASSWORD",
                "vcode": os.environ.get("LOGIN_VCODE", "9999"),
            },
            form=True,
        )
        jb = parse_json(raw)
        if not jb:
            raise RuntimeError(f"login failed HTTP {status}: {raw[:200]}")
        result = unwrap(jb)
        if isinstance(result, dict):
            return result.get("access_token") or ""
        raise RuntimeError("no access_token")
    status, jb, _ = gateway_api(
        "POST",
        "/oauth2/token",
        body={
            "grant_type": "password",
            "client_id": DEFAULT_CLIENT_ID,
            "client_secret": DEFAULT_CLIENT_SECRET,
            "username": username,
            "password": password,
            "scope": "all",
            "loginType": "PASSWORD",
            "vcode": os.environ.get("LOGIN_VCODE", "9999"),
        },
        form=True,
    )
    if not jb:
        raise RuntimeError(f"login failed HTTP {status}")
    result = unwrap(jb)
    if isinstance(result, dict):
        return result.get("access_token") or ""
    raise RuntimeError("no access_token in response")


def reset_jaja7_seed(*, via_gateway: bool = False) -> dict:
    """恢复 admin 密码与 JBM 种子 OAuth 凭证（jaja7 开发环境）。"""
    path = "/internal/dev/reset-jaja7-seed"
    if via_gateway:
        status, jb, raw = gateway_api("POST", path)
    else:
        status, raw = http_request("POST", DEFAULT_AUTH + path, {"tenantId": DEFAULT_TENANT})
        jb = parse_json(raw)
    if status >= 400 or not jb or jb.get("success") is False:
        raise RuntimeError(f"seed reset failed HTTP {status}: {(jb or {}).get('message') or raw[:200]}")
    result = unwrap(jb)
    return result if isinstance(result, dict) else {}


def probe_url(url: str, headers: Optional[dict] = None) -> bool:
    status, raw = http_request("GET", url, headers or {}, timeout=8)
    if status != 200:
        return False
    jb = parse_json(raw)
    if jb is None:
        return "UP" in raw
    return jb.get("success") is True or "UP" in raw


def wait_services(
    *,
    gateway: bool = True,
    auth: bool = True,
    center: bool = False,
    timeout: int = 120,
    interval: float = 2.0,
) -> dict[str, bool]:
    import time

    deadline = time.time() + timeout
    result = {"gateway": False, "auth": False, "center": False}
    headers = {"tenantId": DEFAULT_TENANT}
    while time.time() < deadline:
        if gateway:
            for path in ("/actuator/health", "/role/all"):
                if probe_url(DEFAULT_GATEWAY + path, headers):
                    result["gateway"] = True
                    break
        if auth:
            result["auth"] = probe_url(DEFAULT_AUTH + "/actuator/health", headers)
        if center:
            result["center"] = probe_url(DEFAULT_CENTER + "/actuator/health", headers)
        ready = True
        if gateway and not result["gateway"]:
            ready = False
        if auth and not result["auth"]:
            ready = False
        if center and not result["center"]:
            ready = False
        if ready:
            return result
        time.sleep(interval)
    return result
