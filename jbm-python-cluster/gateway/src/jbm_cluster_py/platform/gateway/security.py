from __future__ import annotations

import ipaddress
import re
from dataclasses import dataclass
from html import unescape
from typing import Any, Mapping, Optional
from urllib.parse import unquote_plus

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, Response

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.result import fail


DEFAULT_PRIVATE_CIDRS = (
    "127.0.0.1/32",
    "::1/128",
    "10.0.0.0/8",
    "172.16.0.0/12",
    "192.168.0.0/16",
)

TEXT_CONTENT_TYPES = (
    "application/json",
    "application/x-www-form-urlencoded",
    "application/xml",
    "application/xhtml+xml",
    "multipart/form-data",
    "text/",
)

DEFAULT_XSS_PATTERNS = (
    r"<\s*/?\s*script\b",
    r"<\s*(iframe|object|embed|svg|math|link|meta|base|form)\b",
    r"\bon[a-z]{3,}\s*=",
    r"\bjavascript\s*:",
    r"\bvbscript\s*:",
    r"\bdata\s*:\s*text/html",
    r"\bexpression\s*\(",
    r"\bdocument\s*\.\s*cookie\b",
    r"\bwindow\s*\.\s*location\b",
)


@dataclass(frozen=True)
class SecurityDecision:
    allowed: bool
    reason: str = ""
    status_code: int = 400


def _bool(value: Any, default: bool = False) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "on"}


def _int(value: Any, default: int) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _list(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    if isinstance(value, (list, tuple, set)):
        return [str(item).strip() for item in value if str(item).strip()]
    return [str(value).strip()] if str(value).strip() else []


def _client_ip(request: Request) -> str:
    forwarded = request.headers.get("x-forwarded-for")
    if forwarded:
        return forwarded.split(",", 1)[0].strip()
    if request.client:
        return request.client.host
    return ""


def _host_name(host_header: str) -> str:
    host = (host_header or "").strip().lower()
    if host.startswith("[") and "]" in host:
        return host[1 : host.index("]")]
    if ":" in host:
        return host.rsplit(":", 1)[0]
    return host


def _host_allowed(host: str, patterns: list[str]) -> bool:
    if not patterns:
        return True
    current = _host_name(host)
    for pattern in patterns:
        rule = pattern.lower()
        if rule == "*":
            return True
        if rule.startswith("*.") and current.endswith(rule[1:]):
            return True
        if current == _host_name(rule):
            return True
    return False


def _ip_matches(pattern: str, ip: str) -> bool:
    try:
        if "/" in pattern:
            return ipaddress.ip_address(ip) in ipaddress.ip_network(pattern, strict=False)
        return ipaddress.ip_address(ip) == ipaddress.ip_address(pattern)
    except ValueError:
        return pattern == ip


def _is_text_body(content_type: str) -> bool:
    lowered = (content_type or "").split(";", 1)[0].strip().lower()
    if not lowered:
        return True
    return any(lowered == item or lowered.startswith(item) for item in TEXT_CONTENT_TYPES)


class GatewaySecurityPolicy:
    def __init__(self, config: Mapping[str, Any], fallback_max_body_bytes: int = 104857600) -> None:
        self.config = dict(config)
        self.enabled = _bool(self.config.get("enabled"), True)
        self.max_body_bytes = _int(self.config.get("max-body-bytes"), fallback_max_body_bytes)

        self.host_whitelist = _list(self.config.get("host-whitelist") or self.config.get("allowed-hosts"))
        self.header_config = dict(self.config.get("headers") or {})
        self.headers_enabled = _bool(self.header_config.get("enabled"), True)
        self.cors_config = dict(self.config.get("cors") or {})

        self.management_config = dict(self.config.get("management") or {})
        self.management_enabled = _bool(self.management_config.get("enabled"), True)
        self.management_prefix = str(self.management_config.get("path-prefix") or "/__gateway")
        self.management_prefixes = [
            self.management_prefix,
            *_list(self.management_config.get("path-prefixes")),
        ]
        self.management_token_header = str(
            self.management_config.get("token-header") or "X-Gateway-Admin-Token"
        )
        self.management_token = str(self.management_config.get("token") or "").strip()
        allowed_ips = _list(self.management_config.get("allowed-ips"))
        self.management_allowed_ips = allowed_ips or list(DEFAULT_PRIVATE_CIDRS)
        self.management_allow_local = _bool(self.management_config.get("allow-local"), True)

        self.xss_config = dict(self.config.get("xss") or {})
        self.xss_enabled = _bool(self.xss_config.get("enabled"), True)
        self.inspect_query = _bool(self.xss_config.get("inspect-query"), True)
        self.inspect_headers = _bool(self.xss_config.get("inspect-headers"), True)
        self.inspect_body_enabled = _bool(self.xss_config.get("inspect-body"), True)
        self.max_inspect_bytes = _int(self.xss_config.get("max-inspect-bytes"), 65536)
        self.excluded_paths = _list(self.xss_config.get("exclude-paths"))
        patterns = _list(self.xss_config.get("patterns"))
        self.xss_patterns = [re.compile(item, re.IGNORECASE) for item in (patterns or DEFAULT_XSS_PATTERNS)]
        self.xss_header_excludes = {
            item.lower()
            for item in (
                _list(self.xss_config.get("exclude-headers"))
                or [
                    "authorization",
                    "cookie",
                    "set-cookie",
                    "sec-websocket-key",
                    "sec-websocket-accept",
                ]
            )
        }

    @classmethod
    def from_app_config(cls, config: AppConfig) -> "GatewaySecurityPolicy":
        fallback = _int(config.get("jbm.gateway.max-body-bytes"), 104857600)
        return cls(config.get("jbm.gateway.security", {}) or {}, fallback)

    def response_headers(self) -> dict[str, str]:
        if not self.enabled or not self.headers_enabled:
            return {}
        configured = self.header_config
        headers = {
            "X-Content-Type-Options": str(configured.get("x-content-type-options") or "nosniff"),
            "X-Frame-Options": str(configured.get("x-frame-options") or "SAMEORIGIN"),
            "X-XSS-Protection": str(configured.get("x-xss-protection") or "1; mode=block"),
            "Referrer-Policy": str(configured.get("referrer-policy") or "no-referrer"),
            "Permissions-Policy": str(
                configured.get("permissions-policy")
                or "geolocation=(), microphone=(), camera=(), payment=()"
            ),
        }
        csp = str(configured.get("content-security-policy") or "").strip()
        if csp:
            headers["Content-Security-Policy"] = csp
        return headers

    def inspect_request_meta(
        self,
        path: str,
        query_string: str,
        headers: Mapping[str, str],
        remote_ip: str,
    ) -> SecurityDecision:
        if not self.enabled:
            return SecurityDecision(True)
        host = headers.get("host") or headers.get("Host") or ""
        if host and not _host_allowed(host, self.host_whitelist):
            return SecurityDecision(False, "Host不在允许列表", 403)
        length = headers.get("content-length") or headers.get("Content-Length")
        if length and self.max_body_bytes > 0 and _int(length, 0) > self.max_body_bytes:
            return SecurityDecision(False, "请求体超过网关限制", 413)
        management_decision = self._inspect_management(path, headers, remote_ip)
        if not management_decision.allowed:
            return management_decision
        if self._is_excluded_path(path):
            return SecurityDecision(True)
        if self.xss_enabled and self.inspect_query and self._looks_like_xss(query_string):
            return SecurityDecision(False, "请求参数包含疑似XSS内容", 400)
        if self.xss_enabled and self.inspect_headers:
            for key, value in headers.items():
                if key.lower() in self.xss_header_excludes:
                    continue
                if self._looks_like_xss(value):
                    return SecurityDecision(False, "请求头包含疑似XSS内容", 400)
        return SecurityDecision(True)

    def inspect_body(self, body: bytes, content_type: Optional[str]) -> SecurityDecision:
        if not self.enabled:
            return SecurityDecision(True)
        if self.max_body_bytes > 0 and len(body) > self.max_body_bytes:
            return SecurityDecision(False, "请求体超过网关限制", 413)
        if not self.xss_enabled or not self.inspect_body_enabled or not body:
            return SecurityDecision(True)
        if not _is_text_body(content_type or ""):
            return SecurityDecision(True)
        sample = body[: self.max_inspect_bytes].decode("utf-8", errors="ignore")
        if self._looks_like_xss(sample):
            return SecurityDecision(False, "请求体包含疑似XSS内容", 400)
        return SecurityDecision(True)

    def _inspect_management(
        self,
        path: str,
        headers: Mapping[str, str],
        remote_ip: str,
    ) -> SecurityDecision:
        if not self.management_enabled or not any(
            path.startswith(prefix) for prefix in self.management_prefixes
        ):
            return SecurityDecision(True)
        if self.management_token and headers.get(self.management_token_header) == self.management_token:
            return SecurityDecision(True)
        if self.management_allow_local and remote_ip in {"127.0.0.1", "::1"}:
            return SecurityDecision(True)
        for pattern in self.management_allowed_ips:
            if _ip_matches(pattern, remote_ip):
                return SecurityDecision(True)
        return SecurityDecision(False, "网关管理接口禁止访问", 403)

    def _is_excluded_path(self, path: str) -> bool:
        for pattern in self.excluded_paths:
            if pattern.endswith("/**") and path.startswith(pattern[:-3]):
                return True
            if pattern == path:
                return True
        return False

    def _looks_like_xss(self, value: str) -> bool:
        if not value:
            return False
        candidates = {value, unquote_plus(value), unescape(value), unescape(unquote_plus(value))}
        return any(pattern.search(candidate) for pattern in self.xss_patterns for candidate in candidates)


def install_security_middleware(app: FastAPI, config: AppConfig) -> GatewaySecurityPolicy:
    policy = GatewaySecurityPolicy.from_app_config(config)
    cors = policy.cors_config
    if policy.enabled and _bool(cors.get("enabled"), False):
        app.add_middleware(
            CORSMiddleware,
            allow_origins=_list(cors.get("allow-origins")) or [],
            allow_origin_regex=cors.get("allow-origin-regex"),
            allow_credentials=_bool(cors.get("allow-credentials"), True),
            allow_methods=_list(cors.get("allow-methods")) or ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
            allow_headers=_list(cors.get("allow-headers")) or ["*"],
            expose_headers=_list(cors.get("expose-headers")) or [],
            max_age=_int(cors.get("max-age"), 600),
        )

    @app.middleware("http")
    async def gateway_security_middleware(request: Request, call_next: Any) -> Response:
        decision = policy.inspect_request_meta(
            request.url.path,
            request.url.query,
            request.headers,
            _client_ip(request),
        )
        if not decision.allowed:
            return JSONResponse(
                status_code=decision.status_code,
                content=fail(None, decision.reason, decision.status_code),
                headers=policy.response_headers(),
            )
        response = await call_next(request)
        for key, value in policy.response_headers().items():
            response.headers.setdefault(key, value)
        return response

    return policy
