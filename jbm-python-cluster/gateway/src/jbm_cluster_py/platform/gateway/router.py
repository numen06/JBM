from __future__ import annotations

from typing import Any, Dict

from fastapi import APIRouter, Body, Request, WebSocket

from jbm_cluster_py.common.result import ok
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
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
    discovery: NacosDiscoveryClient,
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

    @router.post("/logs/GatewayLogs/ingest", tags=["网关日志"])
    async def ingest_gateway_log(body: Dict[str, Any]) -> Dict[str, Any]:
        await access_logger.record(body)
        return ok(True, "采集网关日志成功")

    @router.post("/logs/GatewayLogs/findLogs", tags=["网关日志"])
    async def find_gateway_logs(body: Dict[str, Any] | None = Body(default=None)) -> Dict[str, Any]:
        return ok(await access_logger.repository.page_gateway_logs(body or {}), "查询分页列表成功")

    @router.post("/logs/GatewayLogs/findOperationLogs", tags=["网关日志"])
    async def find_operation_logs(body: Dict[str, Any] | None = Body(default=None)) -> Dict[str, Any]:
        return ok(await access_logger.repository.page_gateway_logs(body or {}, True), "查询分页列表成功")

    @router.post("/logs/GatewayLogs/getByAccessId", tags=["网关日志"])
    async def get_gateway_log(body: Dict[str, Any]) -> Dict[str, Any]:
        return ok(await access_logger.repository.gateway_log(str(body.get("accessId") or "")), "查询日志成功")

    @router.get("/logs/GatewayLogs/filterRules", tags=["网关日志"])
    async def list_filter_rules() -> Dict[str, Any]:
        return ok(await access_logger.repository.list_rules(), "查询过滤规则成功")

    @router.post("/logs/GatewayLogs/filterRules", tags=["网关日志"])
    async def create_filter_rule(body: Dict[str, Any]) -> Dict[str, Any]:
        return ok(await access_logger.repository.save_rule(body), "保存过滤规则成功")

    @router.put("/logs/GatewayLogs/filterRules/{rule_id}", tags=["网关日志"])
    async def update_filter_rule(rule_id: str, body: Dict[str, Any]) -> Dict[str, Any]:
        return ok(await access_logger.repository.save_rule(body, rule_id), "保存过滤规则成功")

    @router.delete("/logs/GatewayLogs/filterRules/{rule_id}", tags=["网关日志"])
    async def delete_filter_rule(rule_id: str) -> Dict[str, Any]:
        return ok(await access_logger.repository.delete_rule(rule_id), "删除过滤规则成功")

    @router.post("/logs/GatewayLogs/filterRules/{rule_id}/toggle", tags=["网关日志"])
    async def toggle_filter_rule(rule_id: str, body: Dict[str, Any]) -> Dict[str, Any]:
        current = await access_logger.repository.rule(rule_id)
        if not current:
            raise ValueError("过滤规则不存在")
        return ok(await access_logger.repository.save_rule({**current, "enabled": bool(body.get("enabled"))}, rule_id), "切换过滤规则成功")

    @router.post("/logs/GatewayLogs/filterRules/test", tags=["网关日志"])
    async def test_filter_rule(body: Dict[str, Any]) -> Dict[str, Any]:
        rules = await access_logger.matching_rules({"path": body.get("path"), "method": body.get("method"), "serviceId": body.get("serviceId"), "httpStatus": body.get("statusCode")})
        return ok({"matched": bool(rules), "rules": rules}, "测试过滤规则成功")

    @router.post("/logs/clusterAccess/getClusterAccessInfo", tags=["网关日志"])
    async def cluster_access() -> Dict[str, Any]:
        return ok(await access_logger.repository.cluster_access(), "查询访问统计成功")

    @router.get("/__gateway/circuit-breakers")
    async def list_circuit_breakers() -> Dict[str, Any]:
        return ok(circuit_breakers.snapshot(), "查询熔断状态成功")

    @router.post("/__gateway/circuit-breakers/reset")
    async def reset_circuit_breakers(body: Dict[str, Any] | None = None) -> Dict[str, Any]:
        key = str((body or {}).get("key") or "").strip() or None
        circuit_breakers.reset(key)
        return ok(True, "重置熔断状态成功")

    @router.get("/gateway/discovery/services", tags=["服务发现"])
    async def discovery_services() -> Dict[str, Any]:
        result = []
        for service_id in await discovery.list_services():
            instances = await discovery.list_instances(service_id)
            healthy = [item for item in instances if item.get("healthy", True)]
            result.append(
                {
                    "serviceId": service_id,
                    "serviceName": service_id,
                    "instanceCount": len(instances),
                    "healthyCount": len(healthy),
                    "versions": sorted(
                        {str((item.get("metadata") or {}).get("version")) for item in instances if (item.get("metadata") or {}).get("version")}
                    ),
                    "clusters": sorted({str(item.get("clusterName") or item.get("cluster")) for item in instances if item.get("clusterName") or item.get("cluster")}),
                }
            )
        return ok(result, "查询服务列表成功")

    @router.get("/gateway/discovery/services/{service_id}/instances", tags=["服务发现"])
    async def discovery_instances(service_id: str) -> Dict[str, Any]:
        rows = []
        for item in await discovery.list_instances(service_id):
            host = item.get("ip") or item.get("host")
            port = item.get("port")
            rows.append(
                {
                    "instanceId": item.get("instanceId") or (f"{host}:{port}" if host and port else None),
                    "host": host,
                    "port": port,
                    "uri": f"http://{host}:{port}" if host and port else None,
                    "secure": False,
                    "scheme": "http",
                    "metadata": item.get("metadata") or {},
                    "healthy": item.get("healthy", True),
                    "cluster": item.get("clusterName") or item.get("cluster"),
                }
            )
        return ok(rows, "查询服务实例成功")

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
