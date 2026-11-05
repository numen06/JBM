"""Management-terminal policy shared by authentication and resource services."""
from __future__ import annotations

import ipaddress
import base64
import json
from collections.abc import Mapping
from typing import Any

from fastapi import HTTPException

from .api_access import is_super_admin, role_codes
from .api_access import is_platform

UNSET_AUDIT_TENANT = object()


def attach_audit_actor(response, identity, resource_tenant=UNSET_AUDIT_TENANT):
    actor = {k: identity[k] for k in ("userId", "username", "userName", "realName", "tenantId", "companyId", "appId", "clientId") if identity.get(k) is not None}
    owner = identity.get("tenantId") or identity.get("companyId")
    platform_actor = is_platform(identity) or "security_auditor" in role_codes(identity)
    scope = (None if platform_actor else owner) if resource_tenant is UNSET_AUDIT_TENANT else resource_tenant
    actor.pop("companyId", None)
    actor.update(actorTenantId=owner, platformActor=platform_actor, tenantId=str(scope) if scope not in (None, "", 0, "0") else None,
                 tenantScopeVerified=scope not in (None, "", 0, "0"))
    response.headers["X-JBM-Audit-Actor"] = base64.urlsafe_b64encode(json.dumps(actor, ensure_ascii=True).encode()).decode()
    return response


def source_ip(peer: str, headers: Mapping[str, str], trusted_proxies=()) -> str:
    """Discard forwarded claims beyond the first untrusted hop (including IPv6)."""
    try:
        current = ipaddress.ip_address(peer)
        trusted = [ipaddress.ip_network(str(v), strict=False) for v in trusted_proxies]
        chain = headers.get("x-forwarded-for", "").split(",")
        if len(chain) > 16:
            return peer
        for hop in reversed(chain):
            if not hop.strip() or not any(current in network for network in trusted):
                break
            current = ipaddress.ip_address(hop.strip())
        return str(current)
    except ValueError:
        return peer


class AdminAccessPolicy:
    def __init__(self, config: Mapping[str, Any] | None = None):
        self.config = dict(config or {})
        self.mode = self.config.get("mode", "monitor")
        if self.mode not in {"monitor", "enforce"}:
            raise ValueError("admin-access.mode必须为monitor或enforce")
        self.proxies = self.config.get("trusted-proxy-networks", ["127.0.0.0/8", "::1/128", "172.16.0.0/12"])
        self.networks = [ipaddress.ip_network(str(v), strict=False) for v in self.config.get("allowed-networks", [])]
        # Enabling an empty policy is an error, never an implicit allow-all.
        if self.mode == "enforce" and not self.networks:
            raise ValueError("启用管理员接入限制前必须配置允许的IP或网段")
        self.roles = set(self.config.get("roles", ["super_admin", "platform_operator", "tenant_admin", "security_auditor"]))

    def evaluate(self, identity: Mapping[str, Any], ip: str) -> bool:
        if not (is_super_admin(identity) or self.roles.intersection(role_codes(identity))):
            return True
        try:
            address = ipaddress.ip_address(ip)
            return any(address in network for network in self.networks)
        except ValueError:
            return False

    def require(self, identity: Mapping[str, Any], ip: str) -> bool:
        allowed = self.evaluate(identity, ip)
        if not allowed and self.mode == "enforce":
            raise HTTPException(403, "当前网络未获准访问管理功能，请使用授权网络")
        return allowed
