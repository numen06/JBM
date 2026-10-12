"""Shared API authorization. Navigation visibility is never an authorization input."""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass
from typing import Any, Mapping

from fastapi import HTTPException

logger = logging.getLogger(__name__)

# These endpoints cannot be made anonymous by a service whitelist or API grant.
METADATA_PATHS = (
    "/openapi.json", "/v3/api-docs", "/v2/api-docs", "/docs", "/redoc",
    "/swagger-ui", "/swagger-ui.html", "/swagger-resources",
)


def metadata_path(path: str) -> bool:
    path = path.rstrip("/") or "/"
    return any(path == p or path.startswith(p + "/") for p in METADATA_PATHS)


def permission_codes(identity: Mapping[str, Any]) -> set[str]:
    result = set()
    for field in ("permissions", "authorities", "scope"):
        values = identity.get(field) or []
        if isinstance(values, str):
            values = values.split()
        for value in values:
            code = (
                value.get("authority") or value.get("authorityId")
                if isinstance(value, Mapping)
                else value
            )
            if code:
                result.add(str(code))
                if str(code).startswith("ACTION_"):
                    result.add(str(code)[7:])
    return result


def role_codes(identity: Mapping[str, Any]) -> set[str]:
    return {
        str(role.get("roleCode") or role.get("role_code") or "")
        if isinstance(role, Mapping)
        else str(role)
        for role in identity.get("roles") or []
    }


def is_platform(identity: Mapping[str, Any]) -> bool:
    return (
        identity.get("admin") is True
        or bool(role_codes(identity) & {"super_admin", "platform_operator"})
        or identity.get("username") == "admin"
    )


def is_service(identity: Mapping[str, Any]) -> bool:
    return (
        identity.get("userId") is None
        and identity.get("user_id") is None
        and bool(identity.get("clientId") or identity.get("client_id"))
        and "internal" in permission_codes(identity)
    )


def normalized_path(path: str) -> str:
    if (
        not path.startswith("/")
        or "\\" in path
        or "//" in path
        or any(part in {".", ".."} for part in path.split("/"))
    ):
        raise HTTPException(403, "非法接口路径")
    return path.rstrip("/") or "/"


def read_operation(method: str, path: str) -> bool:
    action = path.rstrip("/").rsplit("/", 1)[-1].lower()
    if any(
        part.lower().startswith(("delete", "remove", "save", "clear", "reset"))
        for part in path.split("/")[1:]
    ):
        return False
    if action in {
        "run",
        "trigger",
        "execute",
        "pause",
        "resume",
        "save",
        "delete",
        "reset",
        "clear",
        "close",
        "mock",
    }:
        return False
    return (
        method.upper() in {"GET", "HEAD"}
        or action.startswith(("find", "select", "query", "get"))
        or action
        in {
            "pagelist",
            "list",
            "root",
            "tree",
            "unreadcount",
            "catalog",
            "context",
            "model",
            "presentation",
            "storage",
            "isupload",
        }
    )


def menu_codes(*codes: str) -> frozenset[str]:
    return frozenset("MENU_" + code for code in codes)


@dataclass(frozen=True)
class ApiRule:
    prefix: str
    read: tuple[frozenset[str], ...] = ()
    write: tuple[frozenset[str], ...] = ()
    platform_only: bool = False
    authenticated: bool = False
    internal_only: bool = False
    action_codes: tuple[str, ...] = ()

    def matches(self, path: str) -> bool:
        return path == self.prefix or path.startswith(self.prefix + "/")


def resource_rule(prefix: str, *codes: str) -> ApiRule:
    return ApiRule(prefix, (menu_codes(*codes),), action_codes=codes)


CENTER_RULES = tuple(
    ApiRule(p, platform_only=True)
    for p in (
        "/app",
        "/api",
        "/apikey",
        "/authority",
        "/role",
        "/menu",
        "/action",
        "/gateway",
        "/baseAccountLogs",
        "/openApiDocument",
        "/openApiOperation",
        "/baseAppConfig",
    )
) + (
    ApiRule("/internal", internal_only=True),
    ApiRule("/current", authenticated=True),
    ApiRule("/tenant-features/context", authenticated=True),
    ApiRule("/tenant-features", write=(frozenset({"tenant.features.manage"}),)),
    ApiRule(
        "/tenant-delegation",
        (menu_codes("iot_delegations", "tenant_delegations"),),
        (frozenset({"tenant.delegation.manage"}),),
    ),
    ApiRule("/tenant-delegation/received", authenticated=True),
    ApiRule("/delegations/check", authenticated=True),
    ApiRule("/operator-application/current", authenticated=True),
    ApiRule("/user/self", authenticated=True),
    ApiRule("/user/password", authenticated=True),
    resource_rule("/user", "users", "user"),
    ApiRule("/userOrg", platform_only=True),
    ApiRule("/org", (menu_codes("orgs", "org", "users"),), platform_only=False),
    ApiRule("/baseOrg", (menu_codes("orgs", "org", "users"),)),
    ApiRule("/baseOrg/root", authenticated=True),
    ApiRule("/dict", authenticated=True),
    ApiRule("/baseDic", authenticated=True),
    ApiRule("/extend", platform_only=True),
    ApiRule("/operator-application", platform_only=True),
)


def iot_rule(
    prefix: str,
    menus: tuple[str, ...],
    write_permission: str = "iot.platform.operate",
    read_permission: str = "iot.platform.read",
) -> ApiRule:
    aliases = (*menus, *("iot_app_" + code[4:] for code in menus if code.startswith("iot_")))
    return ApiRule(
        prefix,
        (menu_codes(*aliases), frozenset({read_permission})),
        (menu_codes(*aliases), frozenset({write_permission})),
    )


IOT_RULES = tuple(
    iot_rule(p, menus, write, read)
    for p, menus, write, read in (
        ("/workers/flow", ("iot_console",), "iot.platform.admin", "iot.platform.read"),
        ("/workers", ("iot_operations",), "iot.platform.admin", "iot.platform.admin"),
        ("/observability", ("iot_diagnostics",), "iot.platform.admin", "iot.platform.admin"),
        ("/emqx", ("iot_diagnostics",), "iot.platform.admin", "iot.platform.admin"),
        ("/tdengine", ("iot_diagnostics",), "iot.platform.admin", "iot.platform.admin"),
        (
            "/air-conditioning/connections",
            ("iot_air_conditioning",),
            "iot.platform.admin",
            "iot.platform.admin",
        ),
        (
            "/air-conditioning",
            ("iot_air_conditioning",),
            "iot.platform.operate",
            "iot.platform.operate",
        ),
        ("/streetlights", ("iot_streetlights",), "iot.platform.operate", "iot.platform.read"),
        (
            "/feige-manager/resources",
            ("iot_feige_resources",),
            "iot.platform.admin",
            "iot.platform.admin",
        ),
        ("/feige-manager/projects", ("iot_console",), "iot.platform.admin", "iot.platform.read"),
        ("/feige-manager", ("iot_feige_access",), "iot.platform.operate", "iot.platform.operate"),
        ("/feige", ("iot_feige_access",), "iot.platform.operate", "iot.platform.operate"),
        ("/devices", ("iot_resources", "iot_console"), "iot.platform.admin", "iot.platform.read"),
        ("/device", ("iot_resources",), "iot.platform.admin", "iot.platform.read"),
        ("/deviceType", ("iot_resources",), "iot.platform.admin", "iot.platform.admin"),
        ("/deviceProduct", ("iot_resources",), "iot.platform.admin", "iot.platform.admin"),
        ("/deviceModel", ("iot_resources",), "iot.platform.admin", "iot.platform.admin"),
        ("/deviceProtocol", ("iot_resources",), "iot.platform.admin", "iot.platform.admin"),
        ("/unitManagement", ("iot_resources",), "iot.platform.admin", "iot.platform.admin"),
        ("/measureManagement", ("iot_resources",), "iot.platform.admin", "iot.platform.admin"),
        (
            "/deviceAlarm",
            ("iot_operations", "iot_console"),
            "iot.platform.operate",
            "iot.platform.read",
        ),
        ("/deviceAction", ("iot_operations",), "iot.platform.operate", "iot.platform.operate"),
        ("/deviceAlarmRule", ("iot_operations",), "iot.platform.operate", "iot.platform.operate"),
        ("/deviceNoticeRule", ("iot_operations",), "iot.platform.operate", "iot.platform.operate"),
        ("/ota", ("iot_operations",), "iot.platform.operate", "iot.platform.operate"),
        ("/rule", ("iot_operations",), "iot.platform.operate", "iot.platform.operate"),
        (
            "/processDiagram",
            ("iot_resources", "building_bigscreen"),
            "iot.platform.operate",
            "iot.platform.read",
        ),
    )
) + (ApiRule("/auth/me", authenticated=True), ApiRule("/auth/permissions", authenticated=True))


SERVICE_RULES = {
    "center": CENTER_RULES,
    "iot": IOT_RULES,
    "doc": tuple(
        resource_rule(p, "documents", "doc_files", "doc_mgmt", "base_doc")
        for p in (
            "/put",
            "/upload",
            "/remove",
            "/get",
            "/download",
            "/getViewUrl",
            "/baseDoc",
            "/baseDocGroup",
            "/baseDocFile",
        )
    ),
    "job": (ApiRule("/sysJob", platform_only=True), ApiRule("/sysJobLog", platform_only=True)),
    "logs": (
        ApiRule("/GatewayLogs/ingest", internal_only=True),
        ApiRule("/GatewayLogs", platform_only=True),
        ApiRule("/clusterAccess", platform_only=True),
        ApiRule("/BusinessLog", platform_only=True),
        ApiRule("/businessLog", platform_only=True),
    ),
    "bigscreen": tuple(
        resource_rule(p, "bigscreen", "bigscreen_views", "building_bigscreen")
        for p in ("/bigscreenView", "/bigscreenGroup", "/bigscreen")
    ),
    "push": (
        ApiRule("/pushMessage", authenticated=True),
        *(
            ApiRule(p, platform_only=True)
            for p in (
                "/pushMessageConfig",
                "/messageTemplate",
                "/pushChannelConfig",
                "/smsPushConfig",
                "/emailPushConfig",
                "/notification",
                "/webhookTask",
                "/webhookEventConfig",
            )
        ),
    ),
    "gateway": (ApiRule("/__gateway", platform_only=True), ApiRule("/gateway", platform_only=True)),
}

BUILDING_FAMILIES = {
    "overview": ("building_dashboard",),
    "operations": ("building_dashboard", "building_energy_map", "building_energy_monitor"),
    "energy": (
        "building_dashboard",
        "building_energy_monitor",
        "building_history",
        "building_reports",
        "building_household_energy",
        "building_substation_monitor",
    ),
    "attendance": ("building_attendance", "building_attendance_reports"),
    "structures": (
        "building_structures",
        "building_substation_structures",
        "building_household_structures",
    ),
    "structure-types": ("building_structure_types",),
    "master-data/projects": ("building_projects",),
    "master-data/structure-types": ("building_structure_types",),
    "master-data/structures": (
        "building_structures",
        "building_substation_structures",
        "building_household_structures",
    ),
    "master-data/device-bindings": ("building_device_bindings",),
    "master-data/energy-types": ("building_energy_types",),
    "master-data/energy-items": ("building_energy_items",),
    "master-data/energy-prices": ("building_energy_prices",),
    "master-data/drawings": ("building_drawings",),
    "master-data/structure-configs": ("building_structure_configs",),
    "device-bindings": ("building_device_bindings",),
    "master-data/custom-structures": ("building_custom_structures",),
    "master-data/monthly-reports": ("building_monthly_reports",),
    "master-data/energy-price-details": ("building_energy_price_details",),
    "master-data/attendance-reports": ("building_attendance_reports",),
    **{
        "master-data/" + x: ("building_attendance",)
        for x in ("shifts", "employees", "clock-devices", "schedules", "leave-records")
    },
}
SERVICE_RULES["building"] = tuple(
    ApiRule(
        "/api/v1/building/" + p,
        (menu_codes(*codes),),
        (menu_codes(*codes), frozenset({"building.platform.operate"})),
    )
    for p, codes in BUILDING_FAMILIES.items()
) + (ApiRule(
    "/api/v1/building/project-scope",
    (frozenset({"all", "iot.platform.read", "iot.platform.operate", "building.platform.read", "building.platform.operate"}),),
),)
SERVICE_RULES["building"] += (
    ApiRule("/api/v1/building/master-data/icons", authenticated=True),
    ApiRule(
        "/associationDeviceStructure/usagePoints",
        (menu_codes("building_device_bindings"),),
        (menu_codes("building_device_bindings"), frozenset({"building.platform.operate"})),
    ),
)


def service_key(service: str) -> str:
    value = service.removeprefix("jbm-cluster-platform-").removesuffix("-jbm7")
    if value in {"iot-platform", "tpm-iot-platform"}:
        return "iot"
    return "building" if value.startswith("smart-building") else value


def require_api_boundary(service: str, path: str, identity: Mapping[str, Any]) -> None:
    """API/action grants cannot turn platform or internal management into tenant APIs."""
    path = normalized_path(path)
    matches = [rule for rule in SERVICE_RULES.get(service_key(service), ()) if rule.matches(path)]
    if not matches:
        return
    rule = max(matches, key=lambda item: len(item.prefix))
    if rule.internal_only and not (is_service(identity) or is_platform(identity)):
        raise HTTPException(403, "仅内部服务或平台管理员可访问")
    if rule.platform_only and not is_platform(identity):
        raise HTTPException(403, "仅平台管理员可访问")


def authorize_api(
    service: str,
    method: str,
    path: str,
    identity: Mapping[str, Any],
    *,
    extra_rules: tuple[ApiRule, ...] = (),
) -> None:
    path = normalized_path(path)
    require_api_boundary(service, path, identity)
    matches = [
        r for r in (*SERVICE_RULES.get(service_key(service), ()), *extra_rules) if r.matches(path)
    ]
    if metadata_path(path):
        if not (is_service(identity) or is_platform(identity)):
            raise HTTPException(403, "接口文档仅限平台管理员或内部服务")
        return
    if not matches:
        raise HTTPException(403, "接口尚未登记访问权限")
    rule = max(matches, key=lambda r: len(r.prefix))
    if rule.internal_only:
        allowed = is_service(identity) or is_platform(identity)
    elif is_platform(identity):
        allowed = True
    elif rule.platform_only or is_service(identity):
        allowed = False
    elif rule.authenticated:
        # Shared dictionaries are readable; modifying global reference data is privileged.
        allowed = bool(identity.get("userId") or identity.get("user_id")) and (
            read_operation(method, path) or rule.prefix not in {"/dict", "/baseDic"}
        )
    else:
        groups = rule.read if read_operation(method, path) else rule.write
        if not read_operation(method, path) and rule.action_codes:
            action = path.rsplit("/", 1)[-1].lower()
            operation = (
                "delete"
                if method.upper() == "DELETE" or action.startswith(("delete", "remove"))
                else "edit"
                if method.upper() in {"PUT", "PATCH"}
                or action in {"edit", "update", "save", "status", "password", "close", "reset"}
                else "add"
            )
            groups = (
                menu_codes(*rule.action_codes),
                frozenset("ACTION_" + code + "_" + operation for code in rule.action_codes),
            )
        codes = permission_codes(identity)
        if service_key(service) == "building" and "tenant_admin" in role_codes(identity):
            codes.add("building.platform.operate")
        if not (identity.get("userId") or identity.get("user_id")) and (
            identity.get("clientId") or identity.get("client_id")
        ):
            # Service terminals have explicit operation grants instead of browser navigation menus.
            groups = tuple(
                group for group in groups if not all(code.startswith("MENU_") for code in group)
            )
        allowed = bool(groups) and all(bool(group & codes) for group in groups)
    if not allowed:
        logger.warning(
            "api_access_denied service=%s method=%s path=%s actor=%s tenant=%s",
            service,
            method,
            path,
            identity.get("userId") or identity.get("clientId"),
            identity.get("tenantId"),
        )
        raise HTTPException(403, "未获准访问此接口或执行此操作")


def signature_payload(
    service: str,
    method: str,
    path: str,
    query: str,
    body_hash: str,
    key: str,
    timestamp: str,
    nonce: str,
    tenant: str = "",
    project: str = "",
) -> bytes:
    """Version 2: service-relative path keeps the same proof through gateway prefix rewrites."""
    fields = [
        "JBM-REQUEST-V2",
        service,
        method.upper(),
        normalized_path(path),
        query,
        body_hash,
        key,
        timestamp,
        nonce,
        tenant,
        project,
    ]
    if any("\n" in x or "\r" in x for x in fields) or not re.fullmatch(r"[0-9a-f]{64}", body_hash):
        raise HTTPException(401, "非法签名字段")
    return "\n".join(fields).encode("utf-8")
