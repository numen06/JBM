"""Manage the dedicated Loki runtime policy for the audit log streams."""
from __future__ import annotations

import asyncio
import hashlib
import os
import re
import tempfile
from pathlib import Path

import httpx
import yaml
from fastapi import HTTPException

if os.name != "nt":
    import fcntl

SELECTORS = {
    "login": '{job="jbm-security-audit",source=~"auth|legacy-login"}',
    "operation": '{job="jbm-security-audit",source="operation"}',
    "access": '{job="jbm-security-audit",source=~"gateway|legacy-gateway"}',
}
POLICY_FILE = Path("/run/jbm-audit-retention/overrides.yml")
MAX_DAYS = 3650


def retention_days(value):
    match = re.fullmatch(r"(?:(\d+)y)?(?:(\d+)w)?(?:(\d+)d)?(?:(\d+)h)?", str(value))
    if not match:
        raise ValueError("日志库的留存周期不是整天")
    years, weeks, days, hours = (int(part or 0) for part in match.groups())
    if hours % 24 or not any((years, weeks, days, hours)):
        raise ValueError("日志库的留存周期必须为正整数天")
    return years * 365 + weeks * 7 + days + hours // 24


def audit_retention(limits):
    rules = limits.get("retention_stream") or []
    if not rules:
        return dict.fromkeys(SELECTORS, retention_days(limits["retention_period"]))
    by_selector = {rule["selector"]: rule["period"] for rule in rules}
    if len(rules) != len(SELECTORS) or set(by_selector) != set(SELECTORS.values()):
        raise ValueError("日志库存在其他按类别留存规则，请先核对配置")
    return {name: retention_days(by_selector[selector]) for name, selector in SELECTORS.items()}


class AuditRetention:
    def __init__(self, url, tenant, minimum_days, policy_file=POLICY_FILE):
        self.url = url.rstrip("/")
        self.tenant = tenant
        self.minimum_days = minimum_days
        self.policy_file = policy_file
        self.lock_file = policy_file.with_suffix(".lock")

    async def _read(self, client, path):
        response = await client.get(self.url + path)
        response.raise_for_status()
        return yaml.safe_load(response.text) or {}

    async def _loaded(self, client, content):
        response = await client.get(self.url + "/metrics")
        response.raise_for_status()
        digest = hashlib.sha256(content).hexdigest()
        # Loki emits the digest after parsing and installing its merged runtime configuration.
        return f'loki_runtime_config_hash{{config="loki",sha256="{digest}"}} 1' in response.text.splitlines()

    async def _active(self, client):
        if not self.url:
            raise HTTPException(503, "日志库留存配置读取尚未接入")
        config = await self._read(client, "/config")
        if config.get("compactor", {}).get("retention_enabled") is not True:
            raise HTTPException(503, "日志库留存尚未启用")
        tenant = self.tenant if config.get("auth_enabled") else "fake"
        runtime = config.get("runtime_config", {}).get("file") or config["limits_config"].get("per_tenant_override_config")
        limits = config["limits_config"]
        if runtime:
            if runtime != str(self.policy_file):
                raise HTTPException(503, "日志库使用了独立管理的运行配置")
            content = self.policy_file.read_bytes()
            if not await self._loaded(client, content):
                raise HTTPException(503, "日志库尚未确认当前留存配置生效")
            policy = yaml.safe_load(content) or {}
            overrides = policy.get("overrides") or {}
            tenant_policy = overrides.get(tenant) or {}
            if set(policy) - {"overrides"} or set(overrides) - {tenant} or set(tenant_policy) - {"retention_stream"}:
                raise HTTPException(503, "日志库运行配置包含其他策略，请由运维核对")
            if "retention_stream" in tenant_policy:
                limits = {**limits, **tenant_policy}
        days = audit_retention(limits)
        editable = (
            os.name != "nt"
            and runtime == str(self.policy_file)
            and self.policy_file.is_file()
            and os.access(self.policy_file.parent, os.W_OK)
            and (not self.lock_file.exists() or os.access(self.lock_file, os.W_OK))
        )
        return {"days": days, "minimumDays": self.minimum_days, "maximumDays": MAX_DAYS, "editable": editable}, tenant

    async def status(self):
        async with httpx.AsyncClient(timeout=4, trust_env=False) as client:
            try:
                return (await self._active(client))[0]
            except (httpx.HTTPError, OSError, KeyError, TypeError, ValueError, yaml.YAMLError) as exc:
                raise HTTPException(503, "暂无法核实日志库的留存策略") from exc

    def _replace(self, content):
        fd, name = tempfile.mkstemp(dir=self.policy_file.parent, prefix=".retention-")
        try:
            with os.fdopen(fd, "wb") as stream:
                stream.write(content)
                stream.flush()
                os.fchmod(stream.fileno(), 0o644)
                os.fsync(stream.fileno())
            os.replace(name, self.policy_file)
            directory = os.open(self.policy_file.parent, os.O_RDONLY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
        finally:
            Path(name).unlink(missing_ok=True)

    async def save(self, days, expected):
        if not isinstance(days, dict) or set(days) != set(SELECTORS) or any(
            type(value) is not int or not self.minimum_days <= value <= MAX_DAYS for value in days.values()
        ):
            raise HTTPException(422, f"三类日志均需设置 {self.minimum_days} 至 {MAX_DAYS} 天的整数")
        async with httpx.AsyncClient(timeout=4, trust_env=False) as client:
            try:
                current, tenant = await self._active(client)
                if not current["editable"]:
                    raise HTTPException(503, "日志库留存设置的持久配置尚未接入")
                with self.lock_file.open("a") as lock:
                    try:
                        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    except BlockingIOError as exc:
                        raise HTTPException(409, "另一项留存设置正在应用，请稍后刷新") from exc
                    current, tenant = await self._active(client)
                    if not current["editable"] or current["days"] != expected:
                        raise HTTPException(409, "留存设置已变化，请刷新后重新编辑")
                    original = self.policy_file.read_bytes()
                    if not await self._loaded(client, original):
                        raise HTTPException(409, "运行配置已变化，请刷新后重新编辑")
                    if not re.fullmatch(r"[a-zA-Z][a-zA-Z0-9_-]*", tenant) or yaml.safe_load(tenant) != tenant:
                        raise HTTPException(503, "请由运维核对日志库租户配置")
                    # Match Loki's Go YAML encoding for this dedicated, single-file policy.
                    lines = ["overrides:", f"    {tenant}:", "        retention_stream:"]
                    for name, selector in SELECTORS.items():
                        lines.extend((
                            f"            - period: {days[name]}d",
                            "              priority: 100",
                            f"              selector: '{selector}'",
                        ))
                    content = ("\n".join(lines) + "\n").encode()
                    try:
                        self._replace(content)
                        for _ in range(16):
                            if await self._loaded(client, content):
                                return {**current, "days": days}
                            await asyncio.sleep(1)
                        raise HTTPException(503, "日志库尚未确认生效，已恢复原留存配置")
                    except BaseException:
                        self._replace(original)
                        raise
            except (httpx.HTTPError, OSError, KeyError, TypeError, ValueError, yaml.YAMLError) as exc:
                raise HTTPException(503, "无法保存或核实日志库留存配置") from exc
