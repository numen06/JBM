from __future__ import annotations

import base64
import json
import re
import secrets
from datetime import datetime
from typing import Any, Mapping

import bcrypt
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

from jbm_cluster_py.common.result import ok, page_result
from jbm_cluster_py.common.security import validate_password
from jbm_cluster_py.platform.center.modules.governance.application.access import (
    is_platform,
    require_internal,
    require_platform,
    require_tenant_admin,
    require_tenant_record,
    tenant_id,
)
from jbm_cluster_py.platform.center.modules.governance.application.service import GovernanceService
from jbm_cluster_py.platform.center.modules.governance.infrastructure.crud_store import (
    CrudStore,
    new_secret,
)
from jbm_cluster_py.platform.center.modules.governance.infrastructure.openapi_catalog import (
    OpenApiCatalog,
)


class CompatibilityService:
    COLLECTIONS = {
        "/action": "action",
        "/api": "api",
        "/apikey": "apikey",
        "/app": "app",
        "/developer": "developer",
        "/gateway/limit/ip": "ipLimit",
        "/gateway/limit/rate": "rateLimit",
        "/gateway/routes": "route",
        "/menu": "menu",
        "/role": "role",
        "/tenant-delegation": "tenantDelegation",
        "/user": "user",
    }
    IDS = {
        "action": "actionId",
        "api": "apiId",
        "apikey": "keyId",
        "app": "appId",
        "developer": "userId",
        "ipLimit": "policyId",
        "rateLimit": "policyId",
        "route": "routeId",
        "menu": "menuId",
        "role": "roleId",
        "tenantDelegation": "id",
        "user": "userId",
    }
    PLATFORM_ONLY_RESOURCES = {
        "action",
        "api",
        "apikey",
        "developer",
        "ipLimit",
        "rateLimit",
        "route",
        "menu",
        "role",
    }

    def __init__(
        self,
        store: CrudStore,
        governance: GovernanceService,
        openapi: OpenApiCatalog,
        password_policy: Mapping[str, Any] | None = None,
    ) -> None:
        self.store = store
        self.governance = governance
        self.openapi = openapi
        self.password_policy = dict(password_policy or {})

    async def handle(
        self,
        method: str,
        path: str,
        path_params: Mapping[str, Any],
        query: Mapping[str, Any],
        body: Any,
        identity: Mapping[str, Any],
    ) -> dict[str, Any]:
        payload = dict(body) if isinstance(body, Mapping) else {}

        result = await self._special(method, path, path_params, query, payload, identity)
        if result is not None:
            return result

        for base, resource in self.COLLECTIONS.items():
            if path == base:
                return await self._collection(method, resource, query, payload, identity)
            id_name = self.IDS[resource]
            if path == f"{base}/{{{id_name}}}":
                return await self._item(method, resource, path_params[id_name], payload, identity)
        raise ValueError(f"Center 路由尚未实现: {method} {path}")

    async def _collection(
        self,
        method: str,
        resource: str,
        query: Mapping[str, Any],
        payload: Mapping[str, Any],
        identity: Mapping[str, Any],
    ) -> dict[str, Any]:
        if resource in self.PLATFORM_ONLY_RESOURCES:
            require_platform(identity)
        if resource == "user" and method == "POST" and not is_platform(identity):
            require_tenant_admin(identity)
        scoped_query = dict(query)
        scoped_payload = dict(payload)
        if resource == "tenantDelegation":
            if method == "POST" and payload.get("operatorAccount") and not scoped_payload.get("resourceTypes"):
                scoped_payload["resourceTypes"] = ["*"]
            scoped_payload = _delegation_payload(scoped_payload)
            if not is_platform(identity):
                scoped_query["ownerTenantId"] = tenant_id(identity)
                scoped_payload["ownerTenantId"] = tenant_id(identity)
                scoped_payload["appId"] = _app_id(identity)
            if method == "POST":
                operator_account = str(payload.get("operatorAccount") or "").strip()
                if operator_account:
                    operator_user = await self.store.find_user_by_username(operator_account)
                    if operator_user is None:
                        raise ValueError("运营方账号不存在")
                    scoped_payload["operatorTenantId"] = operator_user.get("companyId")
                    scoped_payload["operatorUserId"] = operator_user.get("userId")
                scoped_payload.setdefault("status", 1)
                scoped_payload.setdefault("version", 1)
                scoped_payload["createdBy"] = _user_id(identity)
                scoped_payload["approvedBy"] = _user_id(identity)
        if not is_platform(identity):
            if resource == "user":
                scoped_query["companyId"] = tenant_id(identity)
                scoped_payload["companyId"] = tenant_id(identity)
            elif resource == "app":
                scoped_query["orgId"] = tenant_id(identity)
                scoped_payload["orgId"] = tenant_id(identity)
        if method == "GET":
            page, size = _page(scoped_query)
            rows, total = await self.store.list(resource, scoped_query, page, size)
            if resource == "tenantDelegation":
                for row in rows:
                    operator = await self.store.get("user", row.get("operatorUserId")) if row.get("operatorUserId") else None
                    row["operatorAccount"] = (operator or {}).get("userName")
            return ok(page_result(rows, total, page, size))
        if method == "POST":
            if resource == "user":
                return ok(await self._create_user(scoped_payload, identity))
            if resource == "app":
                scoped_payload = _oauth_app_payload(scoped_payload)
                client_id = str(scoped_payload.get("apiKey") or "").strip()
                if not client_id:
                    code = str(scoped_payload.get("code") or "app").strip().replace(" ", "-")
                    client_id = f"{code}-{secrets.token_hex(8)}"
                client_secret = new_secret()
                public_key, private_key = _rsa_key_pair()
                saved = await self.store.save(
                    resource,
                    {
                        **scoped_payload,
                        "apiKey": client_id,
                        "secretKey": client_secret,
                        "publicKey": public_key,
                        "privateKey": private_key,
                    },
                )
                owner_tenant = saved.get("orgId")
                if owner_tenant not in (None, ""):
                    await self.store.save(
                        "tenantApp",
                        {
                            "tenantId": owner_tenant,
                            "appId": saved.get("appId"),
                            "status": 1,
                        },
                    )
                return ok(
                    {
                        "appId": saved.get("appId"),
                        "clientId": client_id,
                        "clientSecret": client_secret,
                    }
                )
            saved = await self.store.save(resource, scoped_payload)
            if resource == "menu":
                await self._sync_menu_authority(saved)
            elif resource == "action":
                await self._sync_action_authority(saved)
            return ok(saved)
        if method == "PATCH" and resource == "api":
            ids = _ids(query, payload)
            values = {key: value for key, value in payload.items() if key not in {"ids", "apiIds"}}
            for identity in ids:
                await self.store.update_where(resource, {"apiId": identity}, values)
            return ok(True)
        if method == "DELETE" and resource == "api":
            for identity in _ids(query, payload):
                await self.store.delete(resource, identity)
            return ok(True)
        raise ValueError("不支持的资源操作")

    async def _item(
        self,
        method: str,
        resource: str,
        record_id: Any,
        payload: Mapping[str, Any],
        identity: Mapping[str, Any],
    ) -> dict[str, Any]:
        if resource in self.PLATFORM_ONLY_RESOURCES:
            require_platform(identity)
        if resource == "user" and method in {"PUT", "PATCH", "DELETE"} and not is_platform(identity):
            require_tenant_admin(identity)
        existing = await self.store.get(resource, record_id)
        if resource == "user":
            await self._require_user(identity, record_id)
        elif resource == "app":
            require_tenant_record(identity, existing, "orgId")
        elif resource == "tenantDelegation":
            require_tenant_record(identity, existing, "ownerTenantId")
        scoped_payload = dict(payload)
        if resource == "user":
            for key, label in (("mobile", "手机号"), ("email", "邮箱")):
                if key not in scoped_payload:
                    continue
                if str(scoped_payload.get(key) or "").strip() != str((existing or {}).get(key) or "").strip():
                    raise ValueError("%s只能由用户本人通过验证码绑定" % label)
                scoped_payload.pop(key, None)
        if not is_platform(identity):
            if resource == "user":
                scoped_payload.pop("companyId", None)
            elif resource == "app":
                scoped_payload["orgId"] = tenant_id(identity)
            elif resource == "tenantDelegation":
                scoped_payload = _delegation_payload(scoped_payload)
                scoped_payload["ownerTenantId"] = tenant_id(identity)
                scoped_payload["appId"] = _app_id(identity)
                scoped_payload["version"] = int((existing or {}).get("version") or 0) + 1
        if method == "GET":
            return ok(existing)
        if method in {"PUT", "PATCH"}:
            if resource == "app":
                scoped_payload = _oauth_app_payload(scoped_payload, existing)
            saved = await self.store.save(resource, scoped_payload, record_id)
            if resource == "user" and saved.get("companyId") not in (None, ""):
                await self.store.ensure_link(
                    "base_user_org",
                    "user_id",
                    saved["userId"],
                    "org_id",
                    saved["companyId"],
                )
                if "roleIds" in scoped_payload:
                    await self._replace_user_roles(
                        identity, saved, scoped_payload, tenant_id(identity)
                    )
            if resource == "menu":
                await self._sync_menu_authority(saved)
            elif resource == "action":
                await self._sync_action_authority(saved)
            return ok(saved)
        if method == "DELETE":
            return ok(await self.store.delete(resource, record_id))
        raise ValueError("不支持的资源操作")

    async def _special(
        self,
        method: str,
        path: str,
        params: Mapping[str, Any],
        query: Mapping[str, Any],
        body: Mapping[str, Any],
        identity: Mapping[str, Any],
    ) -> dict[str, Any] | None:
        if path == "/captcha/pkey":
            app_key = str(query.get("appKey") or "").strip()
            rows, _ = await self.store.list("app", {"apiKey": app_key}, 1, 1)
            public_key = rows[0].get("publicKey") if rows else None
            if not public_key:
                raise ValueError("客户端不存在或未配置公钥")
            return ok(public_key)

        if path.startswith("/internal/"):
            require_internal(identity)
        if not is_platform(identity):
            platform_prefixes = (
                "/api/",
                "/api-docs",
                "/authority/",
                "/baseAccountLogs/",
                "/baseAppConfig/",
                "/customForms/",
                "/dataSourceManagement/",
                "/gateway/",
                "/apikey/",
                "/developer/",
                "/extend-field/",
            )
            if path.startswith(platform_prefixes) and path != "/baseAppConfig/getAppConfigByKey":
                require_platform(identity)
            if path in {
                "/user/registrations",
                "/user/third-party-accounts",
                "/user/third-party-account-bindings",
            }:
                require_platform(identity)

        if path == "/current/user/account":
            return ok(await self.governance.current_user(identity))
        if path == "/current/user/menu":
            requested_app_id = query.get("appId")
            return ok(
                await self.governance.current_menus(
                    identity,
                    int(requested_app_id) if requested_app_id not in (None, "") else None,
                    tree=False,
                )
            )
        if path == "/app/model":
            return ok(await self.store.get("app", body.get("appId") or body.get("id")))
        if path == "/app/list":
            filters = dict(body.get("baseApp") or body.get("app") or {})
            if not is_platform(identity):
                filters["orgId"] = tenant_id(identity)
            rows, _ = await self.store.list("app", filters, 1, 100)
            for row in rows:
                row.pop("secretKey", None)
                row.pop("privateKey", None)
            return ok(rows)
        if path == "/user/model":
            user_id = int(body.get("userId") or body.get("id") or _user_id(identity))
            return ok(await self.governance.user(user_id, identity))
        if path in {"/user/list", "/user/pageList"}:
            filters = dict(body.get("baseUser") or body.get("user") or {})
            if not is_platform(identity):
                filters["companyId"] = tenant_id(identity)
            page, size = _page(body.get("pageForm") or body)
            rows, total = await self.store.list("user", filters, page, size)
            return ok(page_result(rows, total, page, size) if path.endswith("pageList") else rows)
        if path == "/user/getUserInfoStatistics":
            filters = {} if is_platform(identity) else {"companyId": tenant_id(identity)}
            _, total = await self.store.list("user", filters, 1, 1)
            return ok({"usersTotal": total, "onlineUser": 0})
        if path == "/user/userRoles":
            user_id = int(body.get("userId") or body.get("id") or _user_id(identity))
            return ok(
                await self.governance.repository.user_roles(
                    user_id, _app_id(identity), tenant_id(identity)
                )
            )
        if path == "/baseUserConfig/model":
            return ok(None)
        if path == "/current/user" and method == "PUT":
            user_id = _user_id(identity)
            allowed = {
                key: body[key] for key in ("nickName", "realName", "userDesc", "avatar") if body.get(key) is not None
            }
            await self.store.save("user", allowed, user_id)
            return ok()
        if path == "/current/user/password" and method == "PUT":
            password = str(body.get("currentPassword") or body.get("password") or "")
            if password != str(body.get("confirmPassword") or password):
                raise ValueError("两次输入的新密码不一致")
            await self._set_password(_user_id(identity), password, str(body.get("originPassword") or ""))
            return ok()

        if path == "/user/all":
            filters = {} if is_platform(identity) else {"companyId": tenant_id(identity)}
            rows, _ = await self.store.list("user", filters, 1, 100)
            return ok(rows)
        if path == "/user/statistics":
            filters = {} if is_platform(identity) else {"companyId": tenant_id(identity)}
            rows, total = await self.store.list("user", filters, 1, 1)
            return ok({"usersTotal": total, "onlineUser": 0})
        if path == "/user/{userId}/roles" and method == "PUT":
            await self._require_user(identity, params["userId"])
            role_ids = _values(body, "roleIds")
            app_id = _app_id(identity)
            roles = [row for value in role_ids if (row := await self.store.get("role", value))]
            if len(roles) != len(set(map(str, role_ids))):
                raise ValueError("包含不存在的角色")
            app_role_ids = [
                row["roleId"] for row in roles if str(row.get("appId") or "") == str(app_id)
            ]
            if not is_platform(identity):
                forbidden = [
                    row for row in roles
                    if row.get("roleCode") in {"super_admin", "platform_operator", "iot_operator"}
                    or row.get("appId") not in (None, app_id)
                ]
                if forbidden:
                    require_platform(identity)
            await self.store.replace_scoped_links(
                "base_role_user",
                "user_id",
                params["userId"],
                "role_id",
                app_role_ids,
                "app_id",
                app_id,
                "tenant_id",
                (
                    int((await self.store.get("user", params["userId"]))["companyId"])
                    if is_platform(identity)
                    else tenant_id(identity)
                ),
            )
            return ok()
        if path == "/user/{userId}/roles" and method == "GET":
            await self._require_user(identity, params["userId"])
            user = await self.store.get("user", params["userId"])
            return ok(
                await self.governance.repository.user_roles(
                    params["userId"],
                    _app_id(identity),
                    int(user["companyId"]) if is_platform(identity) else tenant_id(identity),
                )
            )
        if path == "/user/{userId}/orgs" and method == "PUT":
            await self._require_user(identity, params["userId"])
            if not is_platform(identity) and {str(value) for value in _values(body, "orgIds")} - {
                str(tenant_id(identity))
            }:
                require_platform(identity)
            await self.store.replace_links(
                "base_user_org", "user_id", params["userId"], "org_id", _values(body, "orgIds")
            )
            return ok()
        if path == "/user/{userId}/orgs" and method == "GET":
            await self._require_user(identity, params["userId"])
            ids = await self.store.linked_ids("base_user_org", "user_id", params["userId"], "org_id")
            return ok([row for value in ids if (row := await self.store.get("org", value))])
        if path == "/user/{userId}/accounts" and method == "GET":
            await self._require_user(identity, params["userId"])
            rows, _ = await self.store.list("account", {"userId": params["userId"]}, 1, 100)
            for row in rows:
                row.pop("password", None)
            return ok(rows)
        if path == "/user/{userId}/password" and method == "PUT":
            await self._require_user(identity, params["userId"])
            await self._set_password(
                int(params["userId"]),
                str(body.get("password") or body.get("currentPassword") or ""),
            )
            return ok()
        if path == "/user/{userId}/closure":
            await self._require_user(identity, params["userId"])
            await self.store.save("user", {"status": 0}, params["userId"])
            return ok(True)
        if path == "/user/{userId}/status":
            await self._require_user(identity, params["userId"])
            return ok(await self.store.save("user", {"status": body.get("status")}, params["userId"]))
        if path in {"/user/{userId}/activations/email", "/user/{userId}/activations/mobile"}:
            return ok(True)
        if path == "/user/sessions":
            username = str(query.get("username") or body.get("username") or body.get("account") or "")
            password = str(body.get("password") or "")
            rows, _ = await self.store.list(
                "account", {"account": username}, 1, 100, include_secrets=True
            )
            exact = next((row for row in rows if row.get("account") == username), None)
            if not exact:
                raise ValueError("账号不存在")
            if exact.get("status") == 0:
                raise ValueError("账号已禁用")
            if password and not _verify(password, str(exact.get("password") or "")):
                raise ValueError("密码错误")
            return ok(await self.governance.current_user({"userId": exact["userId"]}))
        if path == "/user/registrations":
            return ok(await self._create_user({**query, **body}))
        if path in {
            "/user/third-party-accounts",
            "/user/sessions/mobile",
            "/user/third-party-account-bindings",
        }:
            return ok(await self._create_user({**query, **body}))
        if path == "/user/accounts/open-id":
            return ok(True)
        if path == "/authenticate/{loginType}/login":
            return await self._special("POST", "/user/sessions", params, query, body, identity)

        if path == "/tenant-delegation/received":
            page, size = _page(query)
            rows, total = await self.store.list_active_delegations(
                tenant_id(identity),
                _user_id(identity),
                _app_id(identity),
                page,
                size,
            )
            return ok(page_result(rows, total, page, size))

        if path == "/operator-application/current":
            filters = {"tenantId": tenant_id(identity), "appId": _app_id(identity)}
            rows, _ = await self.store.list("operatorApplication", filters, 1, 1)
            existing = rows[0] if rows else None
            if method == "GET":
                return ok(existing)
            if method == "POST":
                reason = str(body.get("reason") or "").strip()
                if existing and int(existing.get("status") or 0) in {0, 1}:
                    return ok(existing)
                values = {
                    **filters,
                    "applicantUserId": _user_id(identity),
                    "status": 0,
                    "reason": reason,
                    "reviewRemark": None,
                    "reviewedBy": None,
                    "reviewedAt": None,
                }
                return ok(
                    await self.store.save(
                        "operatorApplication", values, existing.get("id") if existing else None
                    )
                )

        if path == "/operator-application":
            require_platform(identity)
            page, size = _page(query)
            rows, total = await self.store.list(
                "operatorApplication",
                {"appId": query.get("appId"), "status": query.get("status")},
                page,
                size,
            )
            return ok(page_result(rows, total, page, size))

        if path == "/operator-application/{id}/review" and method == "PUT":
            require_platform(identity)
            status = int(body.get("status") if body.get("status") is not None else -1)
            if status not in {1, 2}:
                raise ValueError("审批状态必须为通过或驳回")
            return ok(
                await self.store.review_operator_application(
                    int(params["id"]),
                    status,
                    _user_id(identity),
                    str(body.get("reviewRemark") or "").strip(),
                )
            )

        if path == "/role/all":
            rows, _ = await self.store.list("role", {}, 1, 100)
            requested_app_id = query.get("appId") if is_platform(identity) else None
            app_id = (
                int(requested_app_id)
                if requested_app_id not in (None, "")
                else _optional_app_id(identity)
            )
            visible = [
                row for row in rows
                if row.get("appId") in (None, app_id)
                and (is_platform(identity) or row.get("roleCode") == "tenant_admin" or row.get("appId") == app_id)
            ]
            return ok(visible)
        if path == "/role/{roleId}/users" and method == "GET":
            require_platform(identity)
            ids = await self.store.linked_ids("base_role_user", "role_id", params["roleId"], "user_id")
            return ok([row for identity in ids if (row := await self.store.get("user", identity))])
        if path == "/role/{roleId}/users" and method == "PUT":
            require_platform(identity)
            role = await self.store.get("role", params["roleId"])
            if role is None or role.get("appId") in (None, ""):
                raise ValueError("应用角色不能为空")
            users = [
                row
                for user_id in _values(body, "userIds")
                if (row := await self.store.get("user", user_id)) is not None
            ]
            await self.store.replace_role_users(
                params["roleId"],
                role["appId"],
                [(row["userId"], row["companyId"]) for row in users],
            )
            return ok()

        if path == "/menu/all":
            require_platform(identity)
            rows, _ = await self.store.list("menu", query, 1, 100)
            return ok(rows)
        if path == "/menu/current":
            return ok(await self.governance.current_menus(identity))
        if path == "/menu/{menuId}/actions":
            require_platform(identity)
            rows, _ = await self.store.list("action", {"menuId": params["menuId"]}, 1, 100)
            return ok(rows)
        if path == "/menu/export":
            require_platform(identity)
            rows, _ = await self.store.list("menu", query, 1, 100)
            return ok(rows)
        if path in {"/menu/imports", "/menu/sync-from-jbm"}:
            require_platform(identity)
            return ok({"imported": 0, "updated": 0})

        if path == "/app/{appId}/secret" and method == "GET":
            app = await self.store.get("app", params["appId"])
            require_tenant_record(identity, app, "orgId")
            raise ValueError("Client Secret 只在创建或重置时显示")
        if path in {"/app/{appId}/secret", "/app/{appId}/client"} and method == "PUT":
            require_tenant_record(identity, await self.store.get("app", params["appId"]), "orgId")
            values = dict(body)
            if path.endswith("/secret"):
                secret = new_secret()
                values["secretKey"] = secret
                await self.store.save("app", values, params["appId"])
                return ok(secret)
            return ok(await self.store.save("app", values, params["appId"]))

        if path == "/apikey/{keyId}/secret":
            secret = new_secret()
            await self.store.save("apikey", {"secretKey": secret}, params["keyId"])
            return ok(secret)
        if path == "/apikey/{keyId}/status":
            return ok(await self.store.save("apikey", {"status": body.get("status")}, params["keyId"]))
        if path == "/apikey/{keyId}/authority":
            return await self._grant(method, "base_authority_apikey", "key_id", params["keyId"], body)
        if path == "/apikey/{keyId}/check":
            granted = await self.store.linked_ids("base_authority_apikey", "key_id", params["keyId"], "authority_id")
            requested = query.get("authorityId") or query.get("authority")
            return ok(requested in granted or str(requested) in {str(item) for item in granted})
        if path == "/internal/gateway/apikey":
            rows, _ = await self.store.list(
                "apikey",
                {"apiKey": query.get("apiKey")},
                1,
                10,
                include_secrets=True,
            )
            return ok(next((row for row in rows if row.get("apiKey") == query.get("apiKey")), None))
        if path == "/internal/gateway/apikey/{keyId}/check":
            return await self._special(
                "GET", "/apikey/{keyId}/check", {"keyId": params["keyId"]}, query, body, identity
            )
        if path == "/internal/gateway/api":
            rows, _ = await self.store.list(
                "api", {"serviceId": query.get("serviceId"), "path": query.get("path")}, 1, 100
            )
            requested_path = str(query.get("path") or "")
            return ok(
                next(
                    (row for row in rows if row.get("path") == requested_path),
                    rows[0] if rows else None,
                )
            )

        authority_links = {
            "/authority/roles/{roleId}": ("base_authority_role", "role_id", "roleId"),
            "/authority/users/{userId}": ("base_authority_user", "user_id", "userId"),
            "/authority/apps/{appId}": ("base_authority_app", "app_id", "appId"),
            "/authority/actions/{actionId}": ("base_authority_action", "action_id", "actionId"),
        }
        if path in authority_links:
            table, owner_column, param_name = authority_links[path]
            return await self._grant(method, table, owner_column, params[param_name], body)
        if path in {
            "/authority/resources",
            "/authority/apis",
            "/authority/apis/grantable",
            "/authority/catalog",
        }:
            rows, _ = await self.store.list("authority", query, 1, 100)
            return ok(rows)
        if path in {"/authority/menus", "/authority/menus/tree"}:
            requested = query.get("appId")
            return ok(
                await self.governance.current_menus(
                    identity, int(requested) if requested not in (None, "") else None
                )
            )

        if path == "/api/services":
            rows, _ = await self.store.list("api", {}, 1, 100)
            return ok(sorted({str(row.get("serviceId")) for row in rows if row.get("serviceId")}))

        policy_links = {
            "/gateway/limit/ip/{policyId}/apis": ("gateway_ip_limit_api", "policy_id", "policyId"),
            "/gateway/limit/rate/{policyId}/apis": (
                "gateway_rate_limit_api",
                "policy_id",
                "policyId",
            ),
        }
        if path in policy_links:
            table, owner_column, param_name = policy_links[path]
            if method == "GET":
                ids = await self.store.linked_ids(table, owner_column, params[param_name], "api_id")
                return ok([row for api_id in ids if (row := await self.store.get("api", api_id))])
            await self.store.replace_links(table, owner_column, params[param_name], "api_id", _values(body, "apiIds"))
            return ok()
        if path in {"/gateway/api/blackList", "/gateway/api/whiteList"}:
            rows, _ = await self.store.list("ipLimit", {}, 1, 100)
            policy_type = 0 if path.endswith("blackList") else 1
            return ok([row for row in rows if row.get("policyType") == policy_type])
        if path == "/gateway/api/rateLimit":
            rows, _ = await self.store.list("rateLimit", {}, 1, 100)
            return ok(rows)
        if path == "/gateway/api/route":
            rows, _ = await self.store.list("route", {}, 1, 100)
            return ok(rows)
        if path in {"/gateway/routes/micro-services", "/gateway/service/list"}:
            rows, _ = await self.store.list("route", {}, 1, 100)
            services = sorted({str(row.get("serviceId")) for row in rows if row.get("serviceId")})
            return ok([{"serviceId": item, "name": item, "instances": []} for item in services])

        if path == "/baseDic/getDicMap":
            return ok(await self.governance.dict_map())
        if path.startswith("/baseDic/"):
            return await self._master_data(method, path, "dic", "baseDic", body)
        if path.startswith("/baseOrg/"):
            operation = path.rsplit("/", 1)[-1]
            if not is_platform(identity):
                if operation == "root":
                    return ok(await self.governance.org_roots(identity))
                if operation in {"tree", "list", "pageList"}:
                    page, size = _page(body.get("pageForm") or {})
                    data = await self.governance.org_page(page, size, None, identity)
                    rows = data["contents"]
                    return ok(_tree(rows) if operation == "tree" else data if operation == "pageList" else rows)
                if operation in {"getBaseOrg", "model"}:
                    org = await self.store.get(
                        "org",
                        body.get("id") or body.get("orgId") or (body.get("baseOrg") or {}).get("id"),
                    )
                    require_tenant_record(identity, org, "id")
                    return ok(org)
                require_platform(identity)
            if path == "/baseOrg/findTopCompany":
                org = await self.store.get("org", body.get("id") or body.get("orgId"))
                while org and org.get("parentId"):
                    org = await self.store.get("org", org["parentId"])
                return ok(org)
            if path == "/baseOrg/findRelegationCompany":
                rows, _ = await self.store.list("org", {}, 1, 100)
                root = str(body.get("id") or body.get("orgId"))
                return ok(
                    [
                        row
                        for row in rows
                        if root in str(row.get("leafPath") or "").split(",") or str(row.get("id")) == root
                    ]
                )
            if path == "/baseOrg/getBaseOrg":
                return ok(await self.store.get("org", body.get("id") or body.get("orgId")))
            return await self._master_data(method, path, "org", "baseOrg", body)
        if path.startswith("/baseAccountLogs/"):
            return await self._master_data(method, path, "accountLogs", "baseAccountLogs", body)

        if path == "/baseArea/getChinaAreaList":
            rows, _ = await self.store.list("area", query, 1, 100)
            return ok(rows)
        if path == "/baseAppConfig/getAppConfigByKey":
            filters = {
                "appKey": query.get("appKey") or query.get("key"),
                "orgId": query.get("orgId"),
            }
            rows, _ = await self.store.list("appConfig", filters, 1, 1)
            return ok(rows[0] if rows else None)
        if path == "/baseReleaseInfo/findLastVersionInfo":
            filters = {"appId": body.get("appId"), "versionNumber": body.get("versionNumber")}
            rows, _ = await self.store.list("releaseInfo", filters, 1, 100)
            return ok(rows[-1] if rows else None)
        if path == "/baseUserCertification/currentUserCert":
            rows, _ = await self.store.list("certification", {"userId": _user_id(identity)}, 1, 1)
            return ok(rows[0] if rows else None)
        if path == "/baseUserCertification/updateFaceImage":
            rows, _ = await self.store.list("certification", {"userId": _user_id(identity)}, 1, 1)
            existing = rows[0] if rows else {}
            values = {**body, "userId": _user_id(identity)}
            return ok(await self.store.save("certification", values, existing.get("id")))

        if path == "/developer/all":
            rows, _ = await self.store.list("developer", {}, 1, 100)
            return ok(rows)
        if path == "/developer/pending":
            rows, _ = await self.store.list("developer", {"status": 0}, 1, 100)
            return ok(rows)
        if path == "/developer/apply":
            user = await self.store.get("user", _user_id(identity)) or {}
            return ok(await self.store.save("developer", {**user, "status": 0}, _user_id(identity)))
        if path == "/developer/{userId}/approve":
            return ok(await self.store.save("developer", {"status": 1}, params["userId"]))
        if path == "/developer/{userId}/password":
            await self._set_password(int(params["userId"]), str(body.get("password") or ""))
            return ok()
        if path in {"/developer/sessions", "/developer/third-party-accounts"}:
            return await self._special(
                "POST",
                "/user/sessions" if path.endswith("sessions") else "/user/third-party-accounts",
                params,
                query,
                body,
                identity,
            )

        if path == "/extend-field/forms":
            rows, total = await self.store.list("extendForm", query, *_page(query))
            return ok(page_result(rows, total, *_page(query)))
        if path.startswith("/extend-field/forms/{formCode}"):
            form_code = params["formCode"]
            if path.endswith("/definitions"):
                rows, _ = await self.store.list("extendForm", {"formCode": form_code}, 1, 100)
                return ok(_json_field(rows[0], "fieldsJson", []) if rows else [])
            rows, _ = await self.store.list("extendForm", {"formCode": form_code}, 1, 1)
            existing = rows[0] if rows else None
            if method == "GET":
                return ok(existing)
            saved = await self.store.save(
                "extendForm",
                {**body, "formCode": form_code, "version": (existing or {}).get("version", 0) + 1},
                (existing or {}).get("id"),
            )
            return ok(saved)

        if path == "/customForms/getDetail":
            rows, _ = await self.store.list("customForm", body, 1, 1)
            return ok(rows[0] if rows else None)
        if path == "/customForms/saveData":
            return ok(await self.store.save("customForm", body.get("customForms") or body))
        if path == "/dataSourceManagement/saveData":
            data_source_type = str(body.get("dataSourceType") or "")
            if not body.get("dataSourceCode") or not body.get("dataSourceName") or not data_source_type:
                raise ValueError("数据源编码、名称和类型不能为空")
            if data_source_type.lower() == "http" and not all(
                body.get(key) for key in ("url", "requestMethod", "requestHeader")
            ):
                raise ValueError("HTTP 数据源必须提供 URL、请求方式和请求头")
            return ok(await self.store.save("dataSource", body, body.get("id")))

        if path.startswith("/api-docs") or path.startswith("/published-docs"):
            return await self._openapi(method, path, params, query, body)
        return None

    async def _sync_menu_authority(self, menu: Mapping[str, Any]) -> None:
        menu_id = menu.get("menuId")
        menu_code = str(menu.get("menuCode") or "").strip()
        if not menu_id or not menu_code:
            return
        rows, _ = await self.store.list("authority", {"menuId": menu_id}, 1, 10)
        payload = {
            "authority": f"MENU_{menu_code}",
            "resourceType": "menu",
            "menuId": menu_id,
            "appId": menu.get("appId"),
            "status": menu.get("status", 1),
        }
        if rows:
            await self.store.save("authority", payload, rows[0]["authorityId"])
        else:
            await self.store.save("authority", payload)

    async def _sync_action_authority(self, action: Mapping[str, Any]) -> None:
        action_id = action.get("actionId")
        action_code = str(action.get("actionCode") or "").strip()
        menu_id = action.get("menuId")
        if not action_id or not action_code or not menu_id:
            return
        menu = await self.store.get("menu", menu_id)
        app_id = (menu or {}).get("appId")
        if app_id not in (None, "") and str(action.get("appId") or "") != str(app_id):
            await self.store.save("action", {"appId": app_id}, action_id)
        rows, _ = await self.store.list("authority", {"actionId": action_id}, 1, 10)
        payload = {
            "authority": f"ACTION_{action_code}",
            "resourceType": "action",
            "menuId": menu_id,
            "actionId": action_id,
            "appId": app_id,
            "status": action.get("status", 1),
        }
        if rows:
            await self.store.save("authority", payload, rows[0]["authorityId"])
        else:
            await self.store.save("authority", payload)

    async def _require_user(self, identity: Mapping[str, Any], user_id: Any) -> None:
        if is_platform(identity) or await self.store.is_user_member(user_id, tenant_id(identity)):
            return
        require_tenant_record(identity, None, "companyId")

    async def _master_data(
        self, method: str, path: str, resource: str, alias: str, body: Mapping[str, Any]
    ) -> dict[str, Any]:
        operation = path.rsplit("/", 1)[-1]
        entity = body.get(alias) if isinstance(body.get(alias), Mapping) else body.get("masterData")
        entity = dict(entity) if isinstance(entity, Mapping) else {}
        if resource == "accountLogs":
            entity = {
                {"loginIp": "ip", "accountType": "loginType", "loginStatus": "status"}.get(key, key): value
                for key, value in entity.items()
            }
        page, size = _page(body.get("pageForm") or {})
        if operation in {"list", "pageList", "root", "tree"}:
            rows, total = await self.store.list(resource, entity, page, size, operation in {"root", "tree"})
            if resource == "accountLogs":
                rows = [
                    {
                        **row,
                        "accountType": row.get("loginType"),
                        "loginIp": row.get("ip"),
                        "loginStatus": int(row.get("status") or 0) == 1,
                        "loginAgent": row.get("userAgent"),
                        "browser": row.get("userAgent"),
                    }
                    for row in rows
                ]
            if operation == "tree":
                rows = _tree(rows)
            return ok(page_result(rows, total, page, size) if operation == "pageList" else rows)
        if operation == "model":
            identity = entity.get("id") or entity.get(self.IDS.get(resource, "id"))
            return ok(await self.store.get(resource, identity))
        if operation in {"save", "mock"}:
            return ok(await self.store.save(resource, entity))
        if operation == "saveBatch":
            values = body.get(alias + "List") or body.get("list") or []
            return ok([await self.store.save(resource, item) for item in values if isinstance(item, Mapping)])
        if operation == "delete":
            return ok(await self.store.delete(resource, entity.get("id")))
        if operation == "deleteByIds":
            return ok(all([await self.store.delete(resource, value) for value in _values(body, "ids")]))
        raise ValueError("未知主数据操作")

    async def _grant(
        self, method: str, table: str, owner_column: str, owner_id: Any, body: Mapping[str, Any]
    ) -> dict[str, Any]:
        if method == "GET":
            ids = await self.store.linked_ids(table, owner_column, owner_id, "authority_id")
            return ok([row for value in ids if (row := await self.store.get("authority", value))])
        await self.store.replace_links(table, owner_column, owner_id, "authority_id", _values(body, "authorityIds"))
        return ok()

    async def _create_user(
        self,
        payload: Mapping[str, Any],
        identity: Mapping[str, Any] | None = None,
    ) -> dict[str, Any]:
        username = str(payload.get("userName") or payload.get("username") or payload.get("account") or "").strip()
        if not username:
            raise ValueError("用户名不能为空")
        if str(payload.get("mobile") or "").strip() or str(payload.get("email") or "").strip():
            raise ValueError("手机号和邮箱需由用户本人登录后通过验证码绑定")
        target_tenant_id = (
            tenant_id(identity)
            if identity is not None and not is_platform(identity)
            else int(payload.get("companyId") or 0)
        )
        existing = await self.store.find_user_by_username(username)
        if existing is not None:
            if identity is None:
                raise ValueError("用户名已存在")
            if target_tenant_id <= 0:
                raise ValueError("请选择要加入的租户")
            await self.store.ensure_link(
                "base_user_org", "user_id", existing["userId"], "org_id", target_tenant_id
            )
            if "roleIds" in payload:
                await self._replace_user_roles(identity, existing, payload, target_tenant_id)
            return {**existing, "joinedExisting": True, "membershipTenantId": str(target_tenant_id)}
        if payload.get("existingOnly"):
            raise ValueError("账号不存在，请切换为创建新账号")
        password = str(payload.get("password") or "")
        if not password:
            raise ValueError("创建新账号必须设置初始密码")
        user_payload = dict(payload)
        user_payload.pop("mobile", None)
        user_payload.pop("email", None)
        user = await self.store.save(
            "user",
            {
                **user_payload,
                "userName": username,
                "nickName": payload.get("nickName") or username,
                "realName": payload.get("realName") or payload.get("nickName") or username,
                "status": 1,
                "userType": payload.get("userType") or "normal",
            },
        )
        if user.get("companyId") not in (None, ""):
            await self.store.ensure_link(
                "base_user_org",
                "user_id",
                user["userId"],
                "org_id",
                user["companyId"],
            )
        validate_password(password, self.password_policy)
        await self.store.save(
            "account",
            {
                "userId": user["userId"],
                "account": username,
                "password": _hash(password),
                "accountType": payload.get("accountType") or "username",
                "status": 1,
                "domain": payload.get("domain") or "@admin.com",
            },
        )
        if identity is not None and "roleIds" in payload:
            await self._replace_user_roles(identity, user, payload, target_tenant_id)
        return user

    async def _replace_user_roles(
        self,
        identity: Mapping[str, Any],
        user: Mapping[str, Any],
        payload: Mapping[str, Any],
        target_tenant_id: int | None = None,
    ) -> None:
        role_ids = _values(payload, "roleIds")
        app_id = _app_id(identity)
        roles = [row for value in role_ids if (row := await self.store.get("role", value))]
        if len(roles) != len(set(map(str, role_ids))):
            raise ValueError("包含不存在的角色")
        if not is_platform(identity):
            forbidden = [
                row for row in roles
                if row.get("roleCode") in {"super_admin", "platform_operator", "iot_operator"}
                or row.get("appId") not in (None, app_id)
            ]
            if forbidden:
                require_platform(identity)
        await self.store.replace_scoped_links(
            "base_role_user",
            "user_id",
            user["userId"],
            "role_id",
            [row["roleId"] for row in roles if str(row.get("appId") or "") == str(app_id)],
            "app_id",
            app_id,
            "tenant_id",
            int(target_tenant_id or user["companyId"]),
        )

    async def _set_password(self, user_id: int, password: str, origin: str | None = None) -> None:
        validate_password(password, self.password_policy)
        rows, _ = await self.store.list(
            "account", {"userId": user_id}, 1, 100, include_secrets=True
        )
        if not rows:
            raise ValueError("用户没有可用的登录账号")
        if origin is not None and not _verify(origin, str(rows[0].get("password") or "")):
            raise ValueError("原密码错误")
        await self.store.update_where(
            "account",
            {"userId": user_id},
            {
                "password": _hash(password),
                "mustChangePassword": 0,
                "updateTime": datetime.now(),
            },
        )

    async def _openapi(
        self,
        method: str,
        path: str,
        params: Mapping[str, Any],
        query: Mapping[str, Any],
        body: Mapping[str, Any],
    ) -> dict[str, Any]:
        if path == "/api-docs/sources":
            return ok(await self.openapi.sources())
        if path == "/api-docs/operations":
            rows, total = await self.store.list("openApiOperation", query, *_page(query))
            return ok(page_result(rows, total, *_page(query)))
        if path == "/api-docs/operations/{operationId}":
            return ok(await self.store.get("openApiOperation", params["operationId"]))
        if path == "/api-docs/operations/{operationId}/use-cases":
            return ok(
                await self.store.save(
                    "openApiOperation",
                    {"examplesJson": json.dumps(body, ensure_ascii=False)},
                    params["operationId"],
                )
            )
        if path == "/api-docs/spec/{serviceId}":
            return ok(await self.openapi.spec(str(params["serviceId"])))
        if path == "/api-docs/sync":
            return ok(await self.openapi.sync(body.get("serviceIds") or body.get("serviceId")))
        if path == "/api-docs/test":
            return ok(await self.openapi.execute(body))
        if path == "/api-docs/publish":
            service_id = str(body.get("serviceId") or "").strip()
            spec = body.get("publishedSpec") or (await self.openapi.spec(service_id) if service_id else None)
            if not body.get("docKey") or not spec:
                raise ValueError("发布文档必须提供 docKey 和 serviceId/publishedSpec")
            return ok(
                await self.store.save(
                    "publishedApiDoc",
                    {
                        **body,
                        "publishedSpec": json.dumps(spec, ensure_ascii=False),
                        "publishedAt": datetime.now(),
                    },
                )
            )
        if path == "/api-docs/export":
            service_id = str(body.get("serviceId") or "").strip()
            return ok(await self.openapi.spec(service_id))
        if path in {"/published-docs/openapi", "/published-docs/openapi/{docKey}"}:
            filters = {"docKey": params.get("docKey")} if params.get("docKey") else {}
            rows, total = await self.store.list("publishedApiDoc", filters, 1, 100)
            if params.get("docKey"):
                return ok(_json_field(rows[0], "publishedSpec", {}) if rows else None)
            return ok(page_result(rows, total, 1, 100))
        raise ValueError("未知 OpenAPI 操作")


def _page(values: Mapping[str, Any]) -> tuple[int, int]:
    return (
        max(
            int(values.get("pageForm.currPage") or values.get("currPage") or values.get("page") or 1),
            1,
        ),
        min(
            max(
                int(values.get("pageForm.pageSize") or values.get("pageSize") or values.get("size") or 10),
                1,
            ),
            100,
        ),
    )


def _values(body: Mapping[str, Any], key: str) -> list[Any]:
    value = body.get(key) or []
    return list(value) if isinstance(value, (list, tuple, set)) else [value]


def _ids(query: Mapping[str, Any], body: Mapping[str, Any]) -> list[Any]:
    value = body.get("ids") or body.get("apiIds") or query.get("ids") or ""
    return [item.strip() for item in str(value).split(",") if item.strip()] if isinstance(value, str) else list(value)


def _user_id(identity: Mapping[str, Any]) -> int:
    value = identity.get("userId") or identity.get("user_id") or identity.get("sub")
    if value is None:
        raise ValueError("登录信息缺少 userId")
    return int(str(value).split("::", 1)[0])


def _app_id(identity: Mapping[str, Any]) -> int:
    value = _optional_app_id(identity)
    if value is None:
        raise ValueError("登录信息缺少 appId")
    return value


def _optional_app_id(identity: Mapping[str, Any]) -> int | None:
    value = identity.get("appId", identity.get("app_id"))
    if value is None:
        return None
    return int(str(value).split("::", 1)[0])


def _rsa_key_pair() -> tuple[str, str]:
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    public_der = private_key.public_key().public_bytes(
        serialization.Encoding.DER,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    private_der = private_key.private_bytes(
        serialization.Encoding.DER,
        serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    )
    return base64.b64encode(public_der).decode(), base64.b64encode(private_der).decode()


def _hash(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def _verify(password: str, stored: str) -> bool:
    if not stored:
        return False
    if stored.startswith(("$2a$", "$2b$", "$2y$")):
        try:
            return bcrypt.checkpw(password.encode(), stored.encode())
        except ValueError:
            return False
    return secrets.compare_digest(password, stored)


def _tree(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    nodes = {str(row.get("id")): {**row, "children": []} for row in rows}
    roots = []
    for node in nodes.values():
        parent = nodes.get(str(node.get("parentId"))) if node.get("parentId") else None
        (parent["children"] if parent else roots).append(node)
    return roots


def _json_field(row: Mapping[str, Any], key: str, default: Any) -> Any:
    value = row.get(key)
    if not isinstance(value, str):
        return value if value is not None else default
    try:
        return json.loads(value)
    except json.JSONDecodeError:
        return default


def _oauth_app_payload(
    payload: Mapping[str, Any], existing: Mapping[str, Any] | None = None
) -> dict[str, Any]:
    values = dict(payload)
    redirect_value = values.pop("redirectUris", None)
    public_value = values.pop("publicClient", None)
    registration_enabled = values.pop("registrationEnabled", None)
    registration_role = values.pop("registrationDefaultRoleCode", None)
    current = (existing or {}).get("extendData")
    if not isinstance(current, Mapping):
        try:
            current = json.loads(str(current)) if current else {}
        except (TypeError, ValueError):
            current = {}
    extend_data = dict(current)
    oauth = dict(extend_data.get("oauth") or {})
    if redirect_value is not None:
        raw = str(redirect_value).replace("\n", ",")
        oauth["redirectUris"] = [item.strip() for item in raw.split(",") if item.strip()]
    if public_value is not None:
        oauth["publicClient"] = bool(public_value)
    extend_data["oauth"] = oauth
    if registration_enabled is not None or registration_role is not None:
        registration = dict(extend_data.get("registration") or {})
        if registration_enabled is not None:
            registration["enabled"] = bool(registration_enabled)
        if registration_role is not None:
            registration["defaultRoleCode"] = str(registration_role).strip()
        registration["mode"] = "tenant"
        extend_data["registration"] = registration
    values["extendData"] = json.dumps(extend_data, ensure_ascii=False)
    return values


def _delegation_payload(payload: Mapping[str, Any]) -> dict[str, Any]:
    values = dict(payload)
    for key in ("permissionCodes", "resourceTypes", "dataScope", "fieldPolicy"):
        value = values.get(key)
        if value is not None and not isinstance(value, str):
            values[key] = json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return values
