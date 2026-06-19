from __future__ import annotations

import asyncio
import ipaddress
import logging
import time
from dataclasses import dataclass
from typing import Any, Mapping, Optional

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from jbm_cluster_py.integrations.database import configured_database_url
from jbm_cluster_py.platform.gateway.routes import GatewayRoute, _matches

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class IpLimitRule:
    policy_id: int
    policy_name: str
    policy_type: int
    service_id: str
    path: str
    ip_addresses: tuple[str, ...]

    @property
    def is_black(self) -> bool:
        return self.policy_type == 0

    @property
    def is_white(self) -> bool:
        return self.policy_type == 1

    def path_matches(self, request_path: str) -> bool:
        return _matches(self.path, request_path)

    def ip_matches(self, remote_ip: str) -> bool:
        if not self.ip_addresses:
            return True
        for pattern in self.ip_addresses:
            if ip_matches(pattern, remote_ip):
                return True
        return False


@dataclass(frozen=True)
class IpLimitDecision:
    allowed: bool
    reason: Optional[str] = None
    rule: Optional[IpLimitRule] = None


def ip_matches(pattern: str, remote_ip: str) -> bool:
    pattern = str(pattern or "").strip()
    remote_ip = str(remote_ip or "").strip()
    if not pattern or not remote_ip:
        return False
    try:
        if "/" in pattern:
            return ipaddress.ip_address(remote_ip) in ipaddress.ip_network(pattern, strict=False)
        return ipaddress.ip_address(remote_ip) == ipaddress.ip_address(pattern)
    except ValueError:
        return pattern == remote_ip


def split_ip_addresses(value: Any) -> tuple[str, ...]:
    return tuple(item.strip() for item in str(value or "").split(";") if item.strip())


def full_gateway_path(routes: list[GatewayRoute], service_id: str, api_path: str) -> str:
    normalized = api_path if str(api_path or "").startswith("/") else "/" + str(api_path or "")
    for route in routes:
        route_service = route.service_id
        if route.url and route.url.startswith("lb://"):
            route_service = route.url.replace("lb://", "", 1).strip("/") or route_service
        if route_service == service_id and route.path.endswith("/**"):
            return route.path[:-3].rstrip("/") + normalized
    return normalized


class IpLimitRepository:
    def __init__(self, database_config: Mapping[str, Any]) -> None:
        self.database_config = dict(database_config)
        self.black_rules: list[IpLimitRule] = []
        self.white_rules: list[IpLimitRule] = []
        self.loaded_from = "empty"
        self.updated_at = 0.0
        self._engine: Optional[AsyncEngine] = None
        self._lock = asyncio.Lock()

    async def start(self, routes: list[GatewayRoute]) -> None:
        url = configured_database_url(self.database_config)
        if url:
            self._engine = create_async_engine(url, pool_pre_ping=True)
        await self.reload(routes)

    async def stop(self) -> None:
        if self._engine is not None:
            await self._engine.dispose()
            self._engine = None

    async def reload(self, routes: list[GatewayRoute]) -> None:
        async with self._lock:
            rules = await self._load_database_rules(routes)
            self.black_rules = [rule for rule in rules if rule.is_black]
            self.white_rules = [rule for rule in rules if rule.is_white]
            self.loaded_from = "database" if rules else "empty"
            self.updated_at = time.time()
            logger.info(
                "Loaded IP limit rules: black=%s white=%s",
                len(self.black_rules),
                len(self.white_rules),
            )

    async def _load_database_rules(self, routes: list[GatewayRoute]) -> list[IpLimitRule]:
        if self._engine is None:
            return []
        sql = text(
            """
            select
                i.policy_id,
                p.policy_name,
                p.policy_type,
                a.service_id,
                a.path,
                p.ip_address
            from gateway_ip_limit_api i
            inner join gateway_ip_limit p on i.policy_id = p.policy_id
            inner join base_api a on i.api_id = a.api_id
            where p.policy_type in (0, 1)
            """
        )
        try:
            async with self._engine.connect() as conn:
                result = await conn.execute(sql)
                rows = [dict(row._mapping) for row in result.fetchall()]
        except Exception as exc:
            logger.warning("Failed to load IP limit rules: %s", exc)
            return []

        rules: list[IpLimitRule] = []
        for row in rows:
            addresses = split_ip_addresses(row.get("ip_address"))
            service_id = str(row.get("service_id") or "")
            path = full_gateway_path(routes, service_id, str(row.get("path") or "/"))
            if not service_id:
                continue
            rules.append(
                IpLimitRule(
                    policy_id=int(row.get("policy_id") or 0),
                    policy_name=str(row.get("policy_name") or ""),
                    policy_type=int(row.get("policy_type") or 0),
                    service_id=service_id,
                    path=path,
                    ip_addresses=addresses,
                )
            )
        return rules

    def evaluate(self, request_path: str, remote_ip: str) -> IpLimitDecision:
        for rule in self.black_rules:
            if rule.path_matches(request_path) and rule.ip_matches(remote_ip):
                if rule.ip_addresses:
                    return IpLimitDecision(False, "IP命中黑名单", rule)
                return IpLimitDecision(False, "访问地址命中黑名单", rule)
        matched_white = [rule for rule in self.white_rules if rule.path_matches(request_path)]
        if matched_white and not any(rule.ip_matches(remote_ip) for rule in matched_white):
            return IpLimitDecision(False, "IP不在白名单", matched_white[0])
        return IpLimitDecision(True)

    def snapshot(self) -> dict[str, Any]:
        return {
            "loadedFrom": self.loaded_from,
            "updatedAt": self.updated_at,
            "blackRules": [serialize_rule(rule) for rule in self.black_rules],
            "whiteRules": [serialize_rule(rule) for rule in self.white_rules],
        }


def serialize_rule(rule: IpLimitRule) -> dict[str, Any]:
    return {
        "policyId": rule.policy_id,
        "policyName": rule.policy_name,
        "policyType": rule.policy_type,
        "serviceId": rule.service_id,
        "path": rule.path,
        "ipAddresses": list(rule.ip_addresses),
        "pathOnly": not rule.ip_addresses,
    }
