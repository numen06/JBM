"""Management-terminal policy shared by authentication and resource services."""

from __future__ import annotations

import ipaddress
import base64
import json
import weakref
import hashlib
import logging
import threading
from collections.abc import Mapping
from typing import Any

from fastapi import HTTPException

from .api_access import is_super_admin, role_codes
from .api_access import is_platform

UNSET_AUDIT_TENANT = object()
DEFAULT_ADMIN_NETWORKS = (
    "127.0.0.0/8",
    "::1/128",
    "10.0.0.0/8",
    "172.16.0.0/12",
    "192.168.0.0/16",
    "fc00::/7",
)
_POLICIES = weakref.WeakSet()
_POLICY_LOCK = threading.RLock()
logger = logging.getLogger(__name__)


def audit_actor(identity, resource_tenant=UNSET_AUDIT_TENANT):
    actor = {
        k: identity[k]
        for k in (
            "userId",
            "username",
            "userName",
            "realName",
            "tenantId",
            "companyId",
            "appId",
            "clientId",
        )
        if identity.get(k) is not None
    }
    owner = identity.get("tenantId") or identity.get("companyId")
    platform_actor = is_platform(identity) or "security_auditor" in role_codes(identity)
    scope = (
        (None if platform_actor else owner)
        if resource_tenant is UNSET_AUDIT_TENANT
        else resource_tenant
    )
    actor.pop("companyId", None)
    actor.update(
        actorTenantId=owner,
        platformActor=platform_actor,
        tenantId=str(scope) if scope not in (None, "", 0, "0") else None,
        tenantScopeVerified=scope not in (None, "", 0, "0"),
    )
    return actor


def attach_audit_actor(response, identity, resource_tenant=UNSET_AUDIT_TENANT):
    actor = audit_actor(identity, resource_tenant)
    response.headers["X-JBM-Audit-Actor"] = base64.urlsafe_b64encode(
        json.dumps(actor, ensure_ascii=True).encode()
    ).decode()
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
        self.proxies = self.config.get(
            "trusted-proxy-networks", ["127.0.0.0/8", "::1/128", "172.16.0.0/12"]
        )
        self.networks = [
            ipaddress.ip_network(str(v), strict=False)
            for v in (
                (self.config.get("allowed-networks") or DEFAULT_ADMIN_NETWORKS)
                if self.mode == "monitor"
                else self.config.get("allowed-networks", DEFAULT_ADMIN_NETWORKS)
            )
        ]
        # Enabling an empty policy is an error, never an implicit allow-all.
        if self.mode == "enforce" and not self.networks:
            raise ValueError("启用管理员接入限制前必须配置允许的IP或网段")
        self.roles = set(
            self.config.get(
                "roles", ["super_admin", "platform_operator", "tenant_admin", "security_auditor"]
            )
        )
        self.revision = ""
        _POLICIES.add(self)

    def evaluate(self, identity: Mapping[str, Any], ip: str) -> bool:
        if not (is_super_admin(identity) or self.roles.intersection(role_codes(identity))):
            return True
        try:
            address = ipaddress.ip_address(ip)
            return any(address in network for network in self.networks)
        except ValueError:
            return False

    def require(self, identity: Mapping[str, Any], ip: str) -> bool:
        with _POLICY_LOCK:
            allowed = self.evaluate(identity, ip)
            if not allowed and self.mode == "enforce":
                raise HTTPException(403, "当前网络未获准访问管理功能，请使用授权网络")
            return allowed


class AdminPolicyStore:
    """Reuse the configuration centre and its long-poll watcher, never a request-time DB lookup."""

    data_id = "jbm-admin-access-policy.json"

    def __init__(self, config):
        self.config = config.get("spring.cloud.nacos.config", {}) or {}
        self.group = self.config.get("group") or "DEFAULT_GROUP"
        self.client = None
        self.raw = None

    def apply(self, raw):
        if not raw:
            return  # Removing a policy must not silently disable a previous restriction.
        values = json.loads(raw)
        validated = AdminAccessPolicy(values)
        revision = hashlib.sha256(raw.encode()).hexdigest()
        with _POLICY_LOCK:
            for policy in list(_POLICIES):
                policy.networks = validated.networks
                policy.mode = validated.mode
                policy.revision = revision
        self.raw = raw

    def changed(self, args):
        try:
            self.apply(args.get("content"))
        except Exception:
            logger.exception("Invalid administrator network policy; retaining last valid policy")

    def start(self):
        if not self.config.get("enabled") or not self.config.get("server-addr"):
            return
        import nacos
        from .nacos import create_nacos_client

        self.client = create_nacos_client(
            nacos, self.config["server-addr"], self.config.get("namespace") or "public", self.config
        )
        self.apply(self.client.get_config(self.data_id, self.group, no_snapshot=True))
        self.client.add_config_watcher(self.data_id, self.group, self.changed)

    def stop(self):
        if self.client:
            self.client.remove_config_watcher(self.data_id, self.group, self.changed)

    def save(self, payload, identity, ip):
        if self.client is None:
            raise HTTPException(503, "配置中心暂不可用，未修改接入策略")
        mode = payload.get("mode")
        networks = payload.get("allowedNetworks")
        if (
            mode not in {"monitor", "enforce"}
            or not isinstance(networks, list)
            or not 1 <= len(networks) <= 100
        ):
            raise HTTPException(400, "请选择限制模式，并填写1～100个IP或网段")
        try:
            canonical = list(
                dict.fromkeys(str(ipaddress.ip_network(str(v), strict=False)) for v in networks)
            )
            if any(ipaddress.ip_network(v).prefixlen == 0 for v in canonical):
                raise ValueError()
            candidate = AdminAccessPolicy({"mode": mode, "allowed-networks": canonical})
        except ValueError:
            raise HTTPException(400, "IP或网段无效；不限制访问时请使用观察模式")
        if mode == "enforce" and not candidate.evaluate(identity, ip):
            raise HTTPException(400, "当前管理地址不在允许范围内，请先加入当前地址，避免锁定自己")
        current = self.client.get_config(self.data_id, self.group, no_snapshot=True)
        revision = hashlib.sha256(current.encode()).hexdigest() if current else ""
        if payload.get("revision", "") != revision:
            raise HTTPException(409, "接入策略已被更新，请刷新后重试")
        raw = json.dumps(
            {"mode": mode, "allowed-networks": canonical}, ensure_ascii=True, sort_keys=True
        )
        # SDK's public publish_config lacks CAS. Use the same authenticated SDK transport
        # with Nacos' native casMd5, also guarding first creation with the API's Redis lock.
        params = {
            "dataId": self.data_id,
            "group": self.group,
            "tenant": self.client.namespace,
            "content": raw,
            "type": "json",
            "casMd5": hashlib.md5((current or "").encode()).hexdigest() if current else "",
        }
        response = self.client._do_sync_req("/nacos/v1/cs/configs", None, None, params, 5, "POST")
        if response.read() != b"true":
            raise HTTPException(409, "配置未保存或被并发修改，请刷新后重试")
        actual = self.client.get_config(self.data_id, self.group, no_snapshot=True)
        if actual != raw:
            raise HTTPException(409, "配置回读不一致，请刷新核对")
        self.apply(actual)
