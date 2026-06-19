from __future__ import annotations

import hashlib
import random
from dataclasses import dataclass, field
from typing import Any, Mapping, Optional

from jbm_cluster_py.platform.gateway.routes import _matches


@dataclass(frozen=True)
class PathBlacklistRule:
    rule_id: str
    path: str
    service_id: Optional[str] = None
    method: Optional[str] = None
    enabled: bool = True
    reason: str = "访问路径命中黑名单"

    def matches(self, path: str, service_id: Optional[str], method: str) -> bool:
        if not self.enabled:
            return False
        if self.service_id and service_id != self.service_id:
            return False
        if self.method and self.method.upper() != method.upper():
            return False
        return _matches(self.path, path)


@dataclass(frozen=True)
class GrayRouteRule:
    rule_id: str
    path: str
    service_id: Optional[str] = None
    enabled: bool = True
    percent: int = 0
    header_name: Optional[str] = None
    header_value: Optional[str] = None
    metadata: dict[str, str] = field(default_factory=dict)
    target_instances: tuple[tuple[str, int, int], ...] = ()
    sticky_header: str = "X-Gray-Key"

    def matches(self, path: str, service_id: Optional[str], headers: Mapping[str, str], sticky_key: str) -> bool:
        if not self.enabled:
            return False
        if self.service_id and service_id != self.service_id:
            return False
        if not _matches(self.path, path):
            return False
        if self.header_name:
            actual = _header(headers, self.header_name)
            if self.header_value is None:
                if actual is None:
                    return False
            elif actual != self.header_value:
                return False
        percent = max(0, min(int(self.percent), 100))
        if percent >= 100:
            return True
        if percent <= 0:
            return False
        return stable_bucket(sticky_key, self.rule_id) < percent


@dataclass(frozen=True)
class PathAccessDecision:
    allowed: bool
    reason: Optional[str] = None
    rule: Optional[PathBlacklistRule] = None


def stable_bucket(value: str, salt: str = "") -> int:
    digest = hashlib.sha256(("%s:%s" % (salt, value)).encode("utf-8")).hexdigest()
    return int(digest[:8], 16) % 100


def _header(headers: Mapping[str, str], name: str) -> Optional[str]:
    lowered = name.lower()
    for key, value in headers.items():
        if key.lower() == lowered:
            return value
    return None


def _rule_id(prefix: str, index: int, row: Mapping[str, Any]) -> str:
    return str(row.get("id") or row.get("ruleId") or row.get("name") or "%s-%s" % (prefix, index))


def _path_rule(index: int, row: Mapping[str, Any]) -> PathBlacklistRule:
    return PathBlacklistRule(
        rule_id=_rule_id("path-blacklist", index, row),
        path=str(row.get("path") or row.get("pattern") or "/"),
        service_id=_none(row.get("serviceId") or row.get("service-id")),
        method=_none(row.get("method")),
        enabled=bool(row.get("enabled", True)),
        reason=str(row.get("reason") or "访问路径命中黑名单"),
    )


def _gray_rule(index: int, row: Mapping[str, Any]) -> GrayRouteRule:
    target_instances = []
    for item in list(row.get("targetInstances") or row.get("target-instances") or []):
        host = str(item.get("ip") or item.get("host") or "").strip()
        port = int(item.get("port") or 0)
        weight = int(item.get("weight") or 1)
        if host and port > 0 and weight > 0:
            target_instances.append((host, port, weight))
    metadata = {
        str(key): str(value)
        for key, value in dict(row.get("metadata") or {}).items()
        if value is not None
    }
    return GrayRouteRule(
        rule_id=_rule_id("gray", index, row),
        path=str(row.get("path") or row.get("pattern") or "/"),
        service_id=_none(row.get("serviceId") or row.get("service-id")),
        enabled=bool(row.get("enabled", True)),
        percent=int(row.get("percent") or row.get("rate") or 0),
        header_name=_none(row.get("headerName") or row.get("header-name")),
        header_value=_none(row.get("headerValue") or row.get("header-value")),
        metadata=metadata,
        target_instances=tuple(target_instances),
        sticky_header=str(row.get("stickyHeader") or row.get("sticky-header") or "X-Gray-Key"),
    )


def _none(value: Any) -> Optional[str]:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


class TrafficPolicyManager:
    def __init__(
        self,
        path_blacklist: list[Mapping[str, Any]] | None = None,
        gray_routes: list[Mapping[str, Any]] | None = None,
    ) -> None:
        self.path_blacklist = [_path_rule(index, row) for index, row in enumerate(path_blacklist or [], start=1)]
        self.gray_routes = [_gray_rule(index, row) for index, row in enumerate(gray_routes or [], start=1)]

    def evaluate_path(self, path: str, service_id: Optional[str], method: str) -> PathAccessDecision:
        for rule in self.path_blacklist:
            if rule.matches(path, service_id, method):
                return PathAccessDecision(False, rule.reason, rule)
        return PathAccessDecision(True)

    def choose_instance(
        self,
        service_id: str,
        path: str,
        headers: Mapping[str, str],
        client_ip: str,
        instances: list[Mapping[str, Any]],
    ) -> Optional[dict[str, Any]]:
        healthy = [dict(item) for item in instances if _instance_enabled(item)]
        if not healthy:
            return None
        sticky_key = self._sticky_key(path, headers, client_ip)
        for rule in self.gray_routes:
            if not rule.matches(path, service_id, headers, sticky_key):
                continue
            candidates = self._gray_candidates(rule, healthy)
            if candidates:
                return weighted_pick(candidates, sticky_key, rule.rule_id)
        return weighted_pick(healthy, sticky_key, service_id)

    def add_path_blacklist(self, row: Mapping[str, Any]) -> PathBlacklistRule:
        rule = _path_rule(len(self.path_blacklist) + 1, row)
        self.path_blacklist.append(rule)
        return rule

    def remove_path_blacklist(self, rule_id: str) -> bool:
        before = len(self.path_blacklist)
        self.path_blacklist = [rule for rule in self.path_blacklist if rule.rule_id != rule_id]
        return len(self.path_blacklist) != before

    def add_gray_route(self, row: Mapping[str, Any]) -> GrayRouteRule:
        rule = _gray_rule(len(self.gray_routes) + 1, row)
        self.gray_routes.append(rule)
        return rule

    def remove_gray_route(self, rule_id: str) -> bool:
        before = len(self.gray_routes)
        self.gray_routes = [rule for rule in self.gray_routes if rule.rule_id != rule_id]
        return len(self.gray_routes) != before

    def snapshot(self) -> dict[str, Any]:
        return {
            "pathBlacklist": [serialize_path_rule(rule) for rule in self.path_blacklist],
            "grayRoutes": [serialize_gray_rule(rule) for rule in self.gray_routes],
        }

    def _sticky_key(self, path: str, headers: Mapping[str, str], client_ip: str) -> str:
        explicit = _header(headers, "X-Gray-Key")
        if explicit:
            return explicit
        auth = _header(headers, "Authorization")
        if auth:
            return auth
        return "%s:%s" % (client_ip, path)

    def _gray_candidates(
        self,
        rule: GrayRouteRule,
        instances: list[dict[str, Any]],
    ) -> list[dict[str, Any]]:
        if rule.target_instances:
            desired = {(host, port): weight for host, port, weight in rule.target_instances}
            result = []
            for instance in instances:
                key = (str(instance.get("ip") or instance.get("host") or ""), int(instance.get("port") or 0))
                if key in desired:
                    copied = dict(instance)
                    copied["weight"] = desired[key]
                    result.append(copied)
            return result
        if rule.metadata:
            return [instance for instance in instances if _metadata_matches(instance, rule.metadata)]
        return []


def _instance_enabled(instance: Mapping[str, Any]) -> bool:
    return bool(instance.get("healthy", True)) and bool(instance.get("enabled", True)) and float(instance.get("weight") or 1) > 0


def _metadata_matches(instance: Mapping[str, Any], metadata: Mapping[str, str]) -> bool:
    values = {str(key): str(value) for key, value in dict(instance.get("metadata") or {}).items()}
    return all(values.get(key) == value for key, value in metadata.items())


def weighted_pick(instances: list[Mapping[str, Any]], sticky_key: str, salt: str = "") -> Optional[dict[str, Any]]:
    if not instances:
        return None
    total = sum(max(float(item.get("weight") or 1), 0.0) for item in instances)
    if total <= 0:
        return dict(instances[0])
    if sticky_key:
        point = (stable_bucket(sticky_key, salt) / 100.0) * total
    else:
        point = random.random() * total
    cursor = 0.0
    for item in instances:
        cursor += max(float(item.get("weight") or 1), 0.0)
        if point <= cursor:
            return dict(item)
    return dict(instances[-1])


def serialize_path_rule(rule: PathBlacklistRule) -> dict[str, Any]:
    return {
        "id": rule.rule_id,
        "path": rule.path,
        "serviceId": rule.service_id,
        "method": rule.method,
        "enabled": rule.enabled,
        "reason": rule.reason,
    }


def serialize_gray_rule(rule: GrayRouteRule) -> dict[str, Any]:
    return {
        "id": rule.rule_id,
        "path": rule.path,
        "serviceId": rule.service_id,
        "enabled": rule.enabled,
        "percent": rule.percent,
        "headerName": rule.header_name,
        "headerValue": rule.header_value,
        "metadata": dict(rule.metadata),
        "targetInstances": [
            {"ip": host, "port": port, "weight": weight}
            for host, port, weight in rule.target_instances
        ],
        "stickyHeader": rule.sticky_header,
    }
