import asyncio

import yaml
from fastapi import FastAPI
from fastapi.testclient import TestClient

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.gateway.ip_limits import IpLimitRepository, IpLimitRule, full_gateway_path, ip_matches
from jbm_cluster_py.platform.gateway.router import _persist_gray_routes
from jbm_cluster_py.platform.gateway.routes import GatewayRoute, RouteRepository, join_target_url, strip_prefix
from jbm_cluster_py.platform.gateway.circuit_breaker import CircuitBreakerRegistry, HALF_OPEN, OPEN
from jbm_cluster_py.platform.gateway.security import GatewaySecurityPolicy, install_security_middleware
from jbm_cluster_py.platform.gateway.service import (
    websocket_forward_headers,
    websocket_subprotocols,
)
from jbm_cluster_py.platform.gateway.traffic import TrafficPolicyManager


def test_strip_prefix_keeps_root_when_all_segments_removed() -> None:
    assert strip_prefix("/push/ws", 1) == "/ws"
    assert strip_prefix("/push", 1) == "/"
    assert strip_prefix("/doc/baseDoc/list", 1) == "/baseDoc/list"


def test_join_target_url_preserves_base_path_and_query() -> None:
    assert join_target_url("http://127.0.0.1:8080/api", "/users", "a=1") == "http://127.0.0.1:8080/api/users?a=1"


def test_websocket_headers_are_regenerated_without_losing_subprotocols() -> None:
    headers = {
        "Host": "debug.example.com",
        "Connection": "Upgrade",
        "Sec-WebSocket-Key": "duplicate-key",
        "Sec-WebSocket-Protocol": "v12.stomp, v11.stomp",
        "Origin": "https://debug.example.com",
        "X-Trace-Id": "trace-1",
    }

    assert websocket_forward_headers(headers) == {
        "Origin": "https://debug.example.com",
        "X-Trace-Id": "trace-1",
    }
    assert websocket_subprotocols(headers) == ["v12.stomp", "v11.stomp"]


def test_route_repository_uses_fallback_when_database_disabled() -> None:
    repo = RouteRepository(
        {},
        [
            {
                "routeName": "legacy-auth",
                "path": "/auth/**",
                "serviceId": "jbm-cluster-platform-auth",
                "stripPrefix": 0,
            },
            {
                "routeName": "legacy-logs",
                "path": "/logs/**",
                "serviceId": "jbm-cluster-platform-logs",
                "stripPrefix": 0,
            },
            {
                "routeName": "legacy-doc",
                "path": "/doc/**",
                "serviceId": "jbm-cluster-platform-doc",
                "stripPrefix": 0,
            }
        ],
    )

    asyncio.run(repo.start())

    assert repo.match("/push/ws") == GatewayRoute(
        "push-service", "/push/**", "jbm-cluster-platform-push", None, 1
    )
    assert repo.match("/logs/GatewayLogs/findLogs") == GatewayRoute(
        "legacy-logs", "/logs/**", "jbm-cluster-platform-logs", None, 1
    )
    assert repo.match("/bigscreen/screen/page") == GatewayRoute(
        "bigscreen-service", "/bigscreen/**", "jbm-cluster-platform-bigscreen", None, 1
    )
    assert repo.match("/online/pageList") == GatewayRoute(
        "auth-online",
        "/online/**",
        "jbm-cluster-platform-auth",
        None,
        0,
    )
    assert repo.match("/auth/oauth2/publicKey") == GatewayRoute(
        "legacy-auth",
        "/auth/**",
        "jbm-cluster-platform-auth",
        None,
        1,
    )
    assert repo.match("/center/current/user") == GatewayRoute(
        "center-service",
        "/center/**",
        "jbm-cluster-platform-center",
        None,
        1,
    )
    assert repo.match("/doc/upload") == GatewayRoute(
        "legacy-doc",
        "/doc/**",
        "jbm-cluster-platform-doc",
        None,
        1,
    )
    assert repo.loaded_from == "fallback"


def test_ip_match_supports_exact_and_cidr() -> None:
    assert ip_matches("127.0.0.1", "127.0.0.1") is True
    assert ip_matches("10.0.0.0/24", "10.0.0.42") is True
    assert ip_matches("10.0.0.0/24", "10.0.1.42") is False


def test_ip_limit_decision_black_and_white_rules() -> None:
    repo = IpLimitRepository({})
    repo.black_rules = [
        IpLimitRule(1, "deny-local", 0, "svc", "/svc/internal", ("127.0.0.1",)),
        IpLimitRule(3, "deny-path", 0, "svc", "/svc/blocked", ()),
    ]
    repo.white_rules = [
        IpLimitRule(2, "allow-office", 1, "svc", "/svc/private", ("10.0.0.0/24",)),
    ]

    assert repo.evaluate("/svc/internal", "127.0.0.1").allowed is False
    assert repo.evaluate("/svc/blocked", "10.0.9.9").allowed is False
    assert repo.evaluate("/svc/blocked", "127.0.0.1").reason == "访问地址命中黑名单"
    assert repo.evaluate("/svc/private", "10.0.0.5").allowed is True
    assert repo.evaluate("/svc/private", "10.0.1.5").allowed is False
    assert repo.evaluate("/svc/public", "10.0.1.5").allowed is True


def test_ip_limit_path_is_expanded_with_gateway_route() -> None:
    routes = [GatewayRoute("center", "/center/**", "jbm-cluster-platform-center-jbm7", "lb://jbm-cluster-platform-center-jbm7", 1)]

    assert full_gateway_path(routes, "jbm-cluster-platform-center-jbm7", "/internal/oauth/token") == "/center/internal/oauth/token"


def test_circuit_breaker_opens_and_recovers_automatically() -> None:
    breaker = CircuitBreakerRegistry(
        {
            "failure-threshold": 2,
            "recovery-seconds": 0,
            "half-open-max-requests": 1,
            "half-open-success-threshold": 1,
        }
    )

    assert breaker.before_request("svc").allowed is True
    breaker.after_request("svc", status_code=500)
    breaker.after_request("svc", error="timeout")
    denied = breaker.before_request("svc")
    assert denied.allowed is True
    assert denied.state == HALF_OPEN
    breaker.after_request("svc", status_code=200)

    snapshot = breaker.snapshot()
    state = snapshot["states"][0]
    assert state["state"] == "closed"
    assert state["consecutiveFailures"] == 0


def test_circuit_breaker_rejects_while_open_before_recovery_window() -> None:
    breaker = CircuitBreakerRegistry({"failure-threshold": 1, "recovery-seconds": 60})

    breaker.after_request("svc", status_code=503)
    denied = breaker.before_request("svc")

    assert denied.allowed is False
    assert denied.state == OPEN


def test_path_blacklist_is_independent_from_ip() -> None:
    traffic = TrafficPolicyManager(
        [{"id": "deny-health", "path": "/center/actuator/**", "serviceId": "center"}],
        [],
    )

    denied = traffic.evaluate_path("/center/actuator/health", "center", "GET")
    allowed = traffic.evaluate_path("/center/open", "center", "GET")

    assert denied.allowed is False
    assert denied.reason == "访问路径命中黑名单"
    assert allowed.allowed is True


def test_gray_route_selects_target_instance_by_header_and_weight() -> None:
    traffic = TrafficPolicyManager(
        [],
        [
            {
                "id": "gray-center",
                "path": "/center/**",
                "serviceId": "center",
                "percent": 100,
                "headerName": "X-Gray",
                "headerValue": "1",
                "targetInstances": [{"ip": "10.0.0.2", "port": 7777, "weight": 1}],
            }
        ],
    )
    instances = [
        {"ip": "10.0.0.1", "port": 7777, "healthy": True, "enabled": True, "weight": 1},
        {"ip": "10.0.0.2", "port": 7777, "healthy": True, "enabled": True, "weight": 1},
    ]

    chosen = traffic.choose_instance("center", "/center/users", {"X-Gray": "1"}, "127.0.0.1", instances)

    assert chosen is not None
    assert chosen["ip"] == "10.0.0.2"


def test_gray_route_can_target_nacos_metadata() -> None:
    traffic = TrafficPolicyManager(
        [],
        [
            {
                "id": "gray-metadata",
                "path": "/center/**",
                "serviceId": "center",
                "percent": 100,
                "metadata": {"version": "gray"},
            }
        ],
    )
    instances = [
        {"ip": "10.0.0.1", "port": 7777, "healthy": True, "enabled": True, "weight": 1, "metadata": {"version": "stable"}},
        {"ip": "10.0.0.2", "port": 7777, "healthy": True, "enabled": True, "weight": 1, "metadata": {"version": "gray"}},
    ]

    chosen = traffic.choose_instance("center", "/center/users", {}, "127.0.0.1", instances)

    assert chosen is not None
    assert chosen["ip"] == "10.0.0.2"


def test_gray_routes_are_persisted_without_losing_gateway_config() -> None:
    class FakeDiscovery:
        config = {"config-data-id": "gateway-dev.yml"}
        published = ""

        async def get_config(self, _data_id: str) -> str:
            return "jbm:\n  gateway:\n    access-log:\n      enabled: true\n"

        async def publish_config(self, _data_id: str, content: str) -> None:
            self.published = content

    discovery = FakeDiscovery()
    traffic = TrafficPolicyManager(
        [],
        [{"id": "gray", "path": "/center/**", "percent": 10, "metadata": {"version": "canary"}}],
    )

    asyncio.run(_persist_gray_routes(discovery, traffic))

    persisted = yaml.safe_load(discovery.published)
    assert persisted["jbm"]["gateway"]["access-log"]["enabled"] is True
    assert persisted["jbm"]["gateway"]["gray-routes"][0]["id"] == "gray"


def test_security_policy_blocks_xss_query_and_body() -> None:
    policy = GatewaySecurityPolicy({"enabled": True, "max-body-bytes": 1024})

    query_decision = policy.inspect_request_meta(
        "/center/users",
        "name=%3Cscript%3Ealert(1)%3C/script%3E",
        {"host": "jbm.example"},
        "10.0.0.2",
    )
    body_decision = policy.inspect_body(
        b'{"name":"<img src=x onerror=alert(1)>"}',
        "application/json",
    )

    assert query_decision.allowed is False
    assert query_decision.reason == "请求参数包含疑似XSS内容"
    assert body_decision.allowed is False
    assert body_decision.reason == "请求体包含疑似XSS内容"


def test_security_policy_does_not_scan_multipart_file_bytes_as_text() -> None:
    policy = GatewaySecurityPolicy({"enabled": True, "max-body-bytes": 1024})

    decision = policy.inspect_body(
        b'--boundary\r\nContent-Disposition: form-data; name="file"; filename="image.png"\r\n'
        b'Content-Type: image/png\r\n\r\nPNG onerror=payload\r\n--boundary--\r\n',
        "multipart/form-data; boundary=boundary",
    )

    assert decision.allowed is True


def test_security_policy_limits_body_size_and_host() -> None:
    policy = GatewaySecurityPolicy(
        {
            "enabled": True,
            "max-body-bytes": 4,
            "host-whitelist": ["api.example.com"],
        }
    )

    host_decision = policy.inspect_request_meta(
        "/center/users",
        "",
        {"host": "evil.example.com"},
        "10.0.0.2",
    )
    size_decision = policy.inspect_body(b"12345", "text/plain")

    assert host_decision.allowed is False
    assert host_decision.status_code == 403
    assert size_decision.allowed is False
    assert size_decision.status_code == 413


def test_security_middleware_protects_management_endpoint_and_adds_headers() -> None:
    config = AppConfig(
        {
            "spring": {"application": {"name": "gateway"}},
            "server": {"port": 6060},
            "jbm": {
                "gateway": {
                    "security": {
                        "enabled": True,
                        "management": {
                            "allowed-ips": ["10.0.0.0/8"],
                            "allow-local": False,
                            "path-prefixes": ["/gateway/gray-routes"],
                        },
                    }
                }
            },
        },
        "test",
        None,
        app="gateway",
    )
    app = FastAPI()
    install_security_middleware(app, config)

    @app.get("/__gateway/routes")
    async def management_route() -> dict[str, bool]:
        return {"ok": True}

    @app.get("/center/health")
    async def health_route() -> dict[str, bool]:
        return {"ok": True}

    @app.get("/gateway/gray-routes")
    async def gray_routes() -> dict[str, bool]:
        return {"ok": True}

    with TestClient(app) as client:
        denied = client.get("/__gateway/routes")
        gray_denied = client.get("/gateway/gray-routes")
        allowed = client.get("/center/health")

    assert denied.status_code == 403
    assert denied.json()["message"] == "网关管理接口禁止访问"
    assert gray_denied.status_code == 403
    assert allowed.status_code == 200
    assert allowed.headers["x-content-type-options"] == "nosniff"
