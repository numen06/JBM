"""Tenant navigation is presentation only; it never grants routes or API access."""
from __future__ import annotations

import re
from typing import Any, Mapping

from fastapi import HTTPException
from jbm_cluster_py.platform.center.modules.governance.application.access import (
    is_platform, require_tenant_admin, tenant_id,
)


def validate_navigation(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict) or set(value) - {"workspaces", "menus", "groups"}:
        raise HTTPException(422, "菜单设置格式不正确")
    result = {}
    for section in ("workspaces", "menus", "groups"):
        entries = value.get(section, {})
        if not isinstance(entries, dict) or len(entries) > 500:
            raise HTTPException(422, "菜单设置数量超出限制")
        result[section] = {}
        for key, entry in entries.items():
            if not isinstance(key, str) or len(key) > 240:
                raise HTTPException(422, "菜单标识不正确")
            if section == "workspaces" and key not in {"building", "iot", "jbm"}:
                raise HTTPException(422, "未知工作区")
            if section == "menus" and (not re.fullmatch(r"/[a-zA-Z0-9_/-]+", key) or "//" in key):
                raise HTTPException(422, "菜单路径不正确")
            if not isinstance(entry, dict) or set(entry) - {"label", "hidden", "order"}:
                raise HTTPException(422, "仅支持修改名称、显示状态和顺序")
            label = entry.get("label", "")
            if not isinstance(label, str) or len(label.strip()) > (12 if section == "workspaces" else 40) or any(ord(c) < 32 for c in label):
                raise HTTPException(422, "菜单名称过长或包含无效字符")
            if type(entry.get("hidden", False)) is not bool or type(entry.get("order", 0)) is not int or not 0 <= entry.get("order", 0) <= 9999:
                raise HTTPException(422, "显示状态或顺序不正确")
            result[section][key] = {"label": label.strip(), "hidden": entry.get("hidden", False), "order": entry.get("order", 0)}
    return result


async def can_manage_navigation(repository, identity: Mapping[str, Any], target: int | None = None) -> bool:
    if is_platform(identity):
        return True
    try:
        require_tenant_admin(identity)
    except HTTPException:
        return False
    user = await repository.get_user(int(identity.get("userId") or identity.get("user_id") or 0))
    return bool(user and int(user.get("companyId") or 0) == (target or tenant_id(identity)))
