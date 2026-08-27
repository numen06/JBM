from __future__ import annotations

from typing import Any, Mapping

from fastapi import HTTPException


PLATFORM_ROLES = {"super_admin", "platform_operator"}
TENANT_ADMIN_ROLES = {"tenant_admin", "iot_admin"}


def is_super_admin(identity: Mapping[str, Any]) -> bool:
    roles = {str(role) for role in identity.get("roles") or []}
    return bool(identity.get("admin")) or "super_admin" in roles or str(identity.get("username") or "") == "admin"


def is_platform(identity: Mapping[str, Any]) -> bool:
    roles = {str(role) for role in identity.get("roles") or []}
    return bool(identity.get("admin")) or bool(roles & PLATFORM_ROLES) or str(identity.get("username") or "") == "admin"


def tenant_id(identity: Mapping[str, Any]) -> int:
    value = identity.get("tenantId", identity.get("tenant_id"))
    try:
        result = int(value)
    except (TypeError, ValueError):
        raise HTTPException(status_code=403, detail="登录账号未绑定租户") from None
    if result <= 0:
        raise HTTPException(status_code=403, detail="登录账号未绑定租户")
    return result


def require_platform(identity: Mapping[str, Any]) -> None:
    if not is_platform(identity):
        raise HTTPException(status_code=403, detail="仅平台运营账号可执行此操作")


def require_super_admin(identity: Mapping[str, Any]) -> None:
    if not is_super_admin(identity):
        raise HTTPException(status_code=403, detail="仅超级管理员可执行此操作")


def is_tenant_admin(identity: Mapping[str, Any]) -> bool:
    roles = {str(role) for role in identity.get("roles") or []}
    return is_platform(identity) or bool(roles & TENANT_ADMIN_ROLES)


def require_tenant_admin(identity: Mapping[str, Any]) -> None:
    if not is_tenant_admin(identity):
        raise HTTPException(status_code=403, detail="仅租户管理员可执行此操作")


def require_internal(identity: Mapping[str, Any]) -> None:
    if is_platform(identity):
        return
    scope = identity.get("scope") or ""
    scopes = {str(item) for item in scope} if isinstance(scope, (list, tuple, set)) else set(
        str(scope).replace(",", " ").split()
    )
    if "internal" not in scopes:
        raise HTTPException(status_code=403, detail="仅内部服务可执行此操作")


def require_tenant_record(identity: Mapping[str, Any], record: Mapping[str, Any] | None, key: str) -> None:
    if is_platform(identity) or (record and int(record.get(key) or 0) == tenant_id(identity)):
        return
    raise HTTPException(status_code=403, detail="无权访问其他租户的数据")
