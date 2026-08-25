from __future__ import annotations

from typing import Any, Dict

from fastapi import APIRouter, Request, WebSocket
import yaml

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

    @router.get("/__gateway/gray-routes", include_in_schema=False)
    @router.get("/gateway/gray-routes", tags=["灰度发布"])
    async def list_gray_routes() -> Dict[str, Any]:
        return ok(traffic.snapshot()["grayRoutes"], "查询灰度引流规则成功")

    @router.post("/__gateway/gray-routes", include_in_schema=False)
    @router.post("/gateway/gray-routes", tags=["灰度发布"])
    async def add_gray_route(body: Dict[str, Any]) -> Dict[str, Any]:
        rule_id = str(body.get("id") or body.get("ruleId") or "").strip()
        if not rule_id:
            raise ValueError("灰度规则 ID 不能为空")
        if any(item.rule_id == rule_id for item in traffic.gray_routes):
            raise ValueError("灰度规则 ID 已存在")
        rule = traffic.add_gray_route(body)
        try:
            await _persist_gray_routes(discovery, traffic)
        except Exception:
            traffic.remove_gray_route(rule.rule_id)
            raise
        return ok(serialize_gray_rule(rule), "保存灰度引流规则成功")

    @router.delete("/__gateway/gray-routes/{rule_id}", include_in_schema=False)
    @router.delete("/gateway/gray-routes/{rule_id}", tags=["灰度发布"])
    async def delete_gray_route(rule_id: str) -> Dict[str, Any]:
        existing = next((item for item in traffic.gray_routes if item.rule_id == rule_id), None)
        if existing is None:
            raise ValueError("灰度规则不存在")
        serialized = serialize_gray_rule(existing)
        traffic.remove_gray_route(rule_id)
        try:
            await _persist_gray_routes(discovery, traffic)
        except Exception:
            traffic.add_gray_route(serialized)
            raise
        return ok(True, "删除灰度引流规则成功")

    @router.websocket("/{path:path}")
    async def websocket_proxy(websocket: WebSocket, path: str) -> None:
        await proxy.proxy_websocket(websocket, path)

    @router.api_route("/{path:path}", methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"])
    async def http_proxy(request: Request, path: str) -> Any:
        return await proxy.proxy_http(request, path)

    return router


async def _persist_gray_routes(
    discovery: NacosDiscoveryClient,
    traffic: TrafficPolicyManager,
) -> None:
    data_id = str(discovery.config.get("config-data-id") or "").strip()
    if not data_id:
        return
    current = await discovery.get_config(data_id)
    document = yaml.safe_load(current) if current.strip() else {}
    if document is None:
        document = {}
    if not isinstance(document, dict):
        raise ValueError(f"Nacos config {data_id} must contain a YAML object")
    jbm = document.setdefault("jbm", {})
    if not isinstance(jbm, dict):
        raise ValueError(f"Nacos config {data_id} has invalid jbm section")
    gateway = jbm.setdefault("gateway", {})
    if not isinstance(gateway, dict):
        raise ValueError(f"Nacos config {data_id} has invalid jbm.gateway section")
    gateway["gray-routes"] = traffic.snapshot()["grayRoutes"]
    await discovery.publish_config(
        data_id,
        yaml.safe_dump(document, allow_unicode=True, sort_keys=False),
    )
