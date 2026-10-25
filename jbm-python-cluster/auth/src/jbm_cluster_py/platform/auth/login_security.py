"""Offline IP attribution and account-scoped, durable login warnings."""

from __future__ import annotations

import ipaddress
import json
import uuid
from functools import lru_cache
from pathlib import Path


def add_login_context(form, request):
    config = getattr(getattr(request.app.state, "auth_service", None), "config", {})
    networks = (config.get("login-security") or {}).get(
        "trusted-proxy-networks", ["127.0.0.0/8", "::1/128", "172.16.0.0/12"]
    )
    trusted = [ipaddress.ip_network(value) for value in networks]
    peer = request.client.host if request.client else ""
    try:
        current = ipaddress.ip_address(peer)
        forwarded = request.headers.get("x-forwarded-for", "").split(",")
        if len(forwarded) <= 16:
            for value in reversed(forwarded):
                if not any(current in network for network in trusted):
                    break
                if not value.strip():
                    break
                current = ipaddress.ip_address(value.strip())
        peer = str(current)
    except ValueError:
        # Invalid forwarding chains cannot select a forged first entry.
        peer = request.client.host if request.client else ""
    form["login_ip"] = peer[:64]
    form["user_agent"] = request.headers.get("user-agent", "")[:512]
    form["request_id"] = uuid.uuid4().hex


class OfflineRegions:
    def __init__(self, config):
        self.enabled = config.get("geo-enabled", True) is not False
        self.path = Path(
            config.get("geo-data-directory") or Path(__file__).resolve().parents[4] / "data"
        )
        self.searchers = {}
        self.lookup = lru_cache(maxsize=10000)(self._lookup)

    def start(self):
        if not self.enabled:
            return
        import ip2region.searcher as xdb
        import ip2region.util as util

        for version, db_version in [(4, util.IPv4), (6, util.IPv6)]:
            path = self.path / f"ip2region_v{version}.xdb"
            self.searchers[version] = xdb.new_with_buffer(
                db_version, util.load_content_from_file(str(path))
            )

    def _lookup(self, value):
        try:
            address = ipaddress.ip_address(value)
        except ValueError:
            return {}
        if address.version == 6 and address.ipv4_mapped:
            address = address.ipv4_mapped
        if not address.is_global or address.version not in self.searchers:
            return {}
        raw = self.searchers[address.version].search(str(address))
        parts = raw.split("|") if raw else []
        if len(parts) < 3:
            return {}
        country, province, city = [
            part if part not in ("0", "未知", "内网IP") else "" for part in parts[:3]
        ]
        return (
            {
                "country": country,
                "province": province,
                "city": city,
                "label": " / ".join(part for part in (country, province, city) if part),
            }
            if country
            else {}
        )


def different_location(previous, current):
    # Compare only jointly known levels. An IP change or unknown city is not evidence.
    return any(
        previous.get(key) and current.get(key) and previous[key] != current[key]
        for key in ("country", "province", "city")
    )


class LoginSecurity:
    def __init__(self, repository, config):
        self.repository = repository
        self.geo = OfflineRegions(config)

    async def start(self):
        await self.repository.require_login_security_schema()
        self.geo.start()

    async def record(
        self,
        *,
        user_id,
        account,
        login_type,
        ip,
        user_agent,
        status,
        message,
        client_id="",
        credential_id="",
        request_id="",
    ):
        location = self.geo.lookup(ip)
        risk = {}
        if status == 1 and user_id is not None and location:
            for previous in await self.repository.recent_successful_logins(user_id):
                previous_location = self.geo.lookup(previous["ip"])
                if not previous_location:
                    continue
                if different_location(previous_location, location):
                    risk = {
                        "reason": "LOCATION_CHANGED",
                        "previous": {**previous, "location": previous_location["label"]},
                        "current": {"ip": ip, "location": location["label"]},
                    }
                    message = (
                        "登录成功，检测到异地登录："
                        + previous_location["label"]
                        + " → "
                        + location["label"]
                    )
                break
        await self.repository.record_login(
            user_id=user_id,
            account=account,
            login_type=login_type,
            ip=ip,
            user_agent=user_agent,
            status=status,
            message=message,
            details={
                "login_location": location.get("label", ""),
                "client_id": client_id[:128],
                "credential_id": credential_id[:64],
                "request_id": request_id[:64],
                "risk_level": "warning" if risk else "normal",
                "risk_detail": json.dumps(risk, ensure_ascii=False) if risk else None,
            },
        )
