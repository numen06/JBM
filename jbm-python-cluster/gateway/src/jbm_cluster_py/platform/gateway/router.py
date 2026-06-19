from __future__ import annotations

from typing import Any, Dict

from fastapi import APIRouter, Request, WebSocket

from jbm_cluster_py.common.result import ok
from jbm_cluster_py.platform.gateway.circuit_breaker import CircuitBreakerRegistry
from jbm_cluster_py.platform.gateway.ip_limits import IpLimitRepository
from jbm_cluster_py.platform.gateway.routes import RouteRepository
from jbm_cluster_py.platform.gateway.service import AccessLogger, GatewayProxy
from jbm_cluster_py.platform.gateway.traffic import TrafficPolicyManager, serialize_gray_rule, serialize_path_rule


def build_gateway_router(
    proxy: GatewayProxy,
    routes: RouteRepository,
    ip_limits: IpLimitRepository,
    circuit_breakers: CircuitBreakerRegistry,
    traffic: TrafficPolicyManager,
    access_logger: AccessLogger,
) -> APIRouter:
    router = APIRouter()

    @router.get("/__gateway/routes")
    async def list_routes() -> Dict[str, Any]:
        return ok(
            {
                "loadedFrom": routes.loaded_from,
                "updatedAt": routes.updated_at,
                "routes": routes.snapshot(),
            },
            "查询网关路由成功",
        )

    @router.post("/__gateway/routes/reload")
    async def reload_routes() -> Dict[str, Any]:
        loaded = await routes.reload()
        await ip_limits.reload(routes.routes)
        return ok({"count": len(loaded), "loadedFrom": routes.loaded_from}, "刷新网关路由成功")

    @router.get("/__gateway/ip-limits")
    async def list_ip_limits() -> Dict[str, Any]:
        return ok(ip_limits.snapshot(), "查询IP黑白名单成功")

    @router.post("/__gateway/ip-limits/reload")
    async def reload_ip_limits() -> Dict[str, Any]:
        await ip_limits.reload(routes.routes)
        snapshot = ip_limits.snapshot()
        return ok(
            {
                "blackCount": len(snapshot["blackRules"]),
                "whiteCount": len(snapshot["whiteRules"]),
                "loadedFrom": snapshot["loadedFrom"],
            },
            "刷新IP黑白名单成功",
        )

    @router.get("/__gateway/logs/recent")
    async def recent_logs() -> Dict[str, Any]:
        return ok(access_logger.snapshot(), "查询最近网关日志成功")

    @router.get("/__gateway/circuit-breakers")
    async def list_circuit_breakers() -> Dict[str, Any]:
        return ok(circuit_breakers.snapshot(), "查询熔断状态成功")

    @router.post("/__gateway/circuit-breakers/reset")
    async def reset_circuit_breakers(body: Dict[str, Any] | None = None) -> Dict[str, Any]:
        key = str((body or {}).get("key") or "").strip() or None
        circuit_breakers.reset(key)
        return ok(True, "重置熔断状态成功")

    @router.get("/__gateway/path-blacklist")
    async def list_path_blacklist() -> Dict[str, Any]:
        return ok(traffic.snapshot()["pathBlacklist"], "查询访问路径黑名单成功")

    @router.post("/__gateway/path-blacklist")
    async def add_path_blacklist(body: Dict[str, Any]) -> Dict[str, Any]:
        rule = traffic.add_path_blacklist(body)
        return ok(serialize_path_rule(rule), "保存访问路径黑名单成功")

    @router.delete("/__gateway/path-blacklist/{rule_id}")
    async def delete_path_blacklist(rule_id: str) -> Dict[str, Any]:
        return ok(traffic.remove_path_blacklist(rule_id), "删除访问路径黑名单成功")

    @router.get("/__gateway/gray-routes")
    async def list_gray_routes() -> Dict[str, Any]:
        return ok(traffic.snapshot()["grayRoutes"], "查询灰度引流规则成功")

    @router.post("/__gateway/gray-routes")
    async def add_gray_route(body: Dict[str, Any]) -> Dict[str, Any]:
        rule = traffic.add_gray_route(body)
        return ok(serialize_gray_rule(rule), "保存灰度引流规则成功")

    @router.delete("/__gateway/gray-routes/{rule_id}")
    async def delete_gray_route(rule_id: str) -> Dict[str, Any]:
        return ok(traffic.remove_gray_route(rule_id), "删除灰度引流规则成功")

    @router.websocket("/{path:path}")
    async def websocket_proxy(websocket: WebSocket, path: str) -> None:
        await proxy.proxy_websocket(websocket, path)

    @router.api_route("/{path:path}", methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"])
    async def http_proxy(request: Request, path: str) -> Any:
        return await proxy.proxy_http(request, path)

    return router
