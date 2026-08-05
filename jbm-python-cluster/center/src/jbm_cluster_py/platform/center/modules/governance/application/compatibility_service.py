from __future__ import annotations

import json
import secrets
from datetime import datetime
from typing import Any, Mapping

import bcrypt

from jbm_cluster_py.common.result import ok, page_result
from jbm_cluster_py.platform.center.modules.governance.application.service import GovernanceService
from jbm_cluster_py.platform.center.modules.governance.infrastructure.crud_store import CrudStore, new_secret
from jbm_cluster_py.platform.center.modules.governance.infrastructure.openapi_catalog import OpenApiCatalog


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
        "user": "userId",
    }

    def __init__(self, store: CrudStore, governance: GovernanceService, openapi: OpenApiCatalog) -> None:
        self.store = store
        self.governance = governance
        self.openapi = openapi

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
                return await self._collection(method, resource, query, payload)
            id_name = self.IDS[resource]
            if path == f"{base}/{{{id_name}}}":
                return await self._item(method, resource, path_params[id_name], payload)
        raise ValueError(f"Center 路由尚未实现: {method} {path}")

    async def _collection(
        self, method: str, resource: str, query: Mapping[str, Any], payload: Mapping[str, Any]
    ) -> dict[str, Any]:
        if method == "GET":
            page, size = _page(query)
            rows, total = await self.store.list(resource, query, page, size)
            return ok(page_result(rows, total, page, size))
        if method == "POST":
            if resource == "user":
                return ok(await self._create_user(payload))
            return ok(await self.store.save(resource, payload))
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
        self, method: str, resource: str, identity: Any, payload: Mapping[str, Any]
    ) -> dict[str, Any]:
        if method == "GET":
            return ok(await self.store.get(resource, identity))
        if method in {"PUT", "PATCH"}:
            return ok(await self.store.save(resource, payload, identity))
        if method == "DELETE":
            return ok(await self.store.delete(resource, identity))
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
        if path == "/current/user" and method == "PUT":
            user_id = _user_id(identity)
            allowed = {key: body[key] for key in ("nickName", "realName", "userDesc", "avatar") if body.get(key) is not None}
            await self.store.save("user", allowed, user_id)
            return ok()
        if path == "/current/user/password" and method == "PUT":
            password = str(body.get("currentPassword") or body.get("password") or "")
            if password != str(body.get("confirmPassword") or password):
                raise ValueError("两次输入的新密码不一致")
            await self._set_password(
                _user_id(identity), password, str(body.get("originPassword") or "")
            )
            return ok()

        if path == "/user/all":
            rows, _ = await self.store.list("user", {}, 1, 100)
            return ok(rows)
        if path == "/user/statistics":
            rows, total = await self.store.list("user", {}, 1, 1)
            return ok({"usersTotal": total, "onlineUser": 0})
        if path == "/user/{userId}/roles" and method == "PUT":
            await self.store.replace_links("base_role_user", "user_id", params["userId"], "role_id", _values(body, "roleIds"))
            return ok()
        if path == "/user/{userId}/roles" and method == "GET":
            ids = await self.store.linked_ids(
                "base_role_user", "user_id", params["userId"], "role_id"
            )
            return ok([row for value in ids if (row := await self.store.get("role", value))])
        if path == "/user/{userId}/orgs" and method == "PUT":
            await self.store.replace_links("base_user_org", "user_id", params["userId"], "org_id", _values(body, "orgIds"))
            return ok()
        if path == "/user/{userId}/orgs" and method == "GET":
            ids = await self.store.linked_ids(
                "base_user_org", "user_id", params["userId"], "org_id"
            )
            return ok([row for value in ids if (row := await self.store.get("org", value))])
        if path == "/user/{userId}/accounts" and method == "GET":
            rows, _ = await self.store.list("account", {"userId": params["userId"]}, 1, 100)
            for row in rows:
                row.pop("password", None)
            return ok(rows)
        if path == "/user/{userId}/password" and method == "PUT":
            await self._set_password(int(params["userId"]), str(body.get("password") or body.get("currentPassword") or ""))
            return ok()
        if path == "/user/{userId}/closure":
            await self.store.save("user", {"status": 0}, params["userId"])
            return ok(True)
        if path == "/user/{userId}/status":
            return ok(await self.store.save("user", {"status": body.get("status")}, params["userId"]))
        if path in {"/user/{userId}/activations/email", "/user/{userId}/activations/mobile"}:
            return ok(True)
        if path == "/user/sessions":
            username = str(query.get("username") or body.get("username") or body.get("account") or "")
            password = str(query.get("password") or body.get("password") or "")
            rows, _ = await self.store.list("account", {"account": username}, 1, 100)
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
        if path in {"/user/third-party-accounts", "/user/sessions/mobile", "/user/third-party-account-bindings"}:
            return ok(await self._create_user({**query, **body}))
        if path == "/user/accounts/open-id":
            return ok(True)
        if path == "/authenticate/{loginType}/login":
            return await self._special("POST", "/user/sessions", params, query, body, identity)

        if path == "/role/all":
            rows, _ = await self.store.list("role", {}, 1, 100)
            return ok(rows)
        if path == "/role/{roleId}/users" and method == "GET":
            ids = await self.store.linked_ids("base_role_user", "role_id", params["roleId"], "user_id")
            return ok([row for identity in ids if (row := await self.store.get("user", identity))])
        if path == "/role/{roleId}/users" and method == "PUT":
            await self.store.replace_links("base_role_user", "role_id", params["roleId"], "user_id", _values(body, "userIds"))
            return ok()

        if path == "/menu/all":
            rows, _ = await self.store.list("menu", query, 1, 100)
            return ok(rows)
        if path == "/menu/current":
            return ok(await self.governance.current_menus(identity))
        if path == "/menu/{menuId}/actions":
            rows, _ = await self.store.list("action", {"menuId": params["menuId"]}, 1, 100)
            return ok(rows)
        if path == "/menu/export":
            rows, _ = await self.store.list("menu", query, 1, 100)
            return ok(rows)
        if path in {"/menu/imports", "/menu/sync-from-jbm"}:
            return ok({"imported": 0, "updated": 0})

        if path == "/app/{appId}/secret" and method == "GET":
            app = await self.store.get("app", params["appId"])
            return ok({"secretKey": app.get("secretKey") if app else None})
        if path in {"/app/{appId}/secret", "/app/{appId}/client"} and method == "PUT":
            values = dict(body)
            if path.endswith("/secret"):
                values["secretKey"] = new_secret()
            return ok(await self.store.save("app", values, params["appId"]))

        if path == "/apikey/{keyId}/secret":
            return ok(await self.store.save("apikey", {"secretKey": new_secret()}, params["keyId"]))
        if path == "/apikey/{keyId}/status":
            return ok(await self.store.save("apikey", {"status": body.get("status")}, params["keyId"]))
        if path == "/apikey/{keyId}/authority":
            return await self._grant(method, "base_authority_apikey", "key_id", params["keyId"], body)
        if path == "/apikey/{keyId}/check":
            granted = await self.store.linked_ids("base_authority_apikey", "key_id", params["keyId"], "authority_id")
            requested = query.get("authorityId") or query.get("authority")
            return ok(requested in granted or str(requested) in {str(item) for item in granted})
        if path == "/internal/gateway/apikey":
            rows, _ = await self.store.list("apikey", {"apiKey": query.get("apiKey")}, 1, 10)
            return ok(next((row for row in rows if row.get("apiKey") == query.get("apiKey")), None))
        if path == "/internal/gateway/apikey/{keyId}/check":
            return await self._special("GET", "/apikey/{keyId}/check", {"keyId": params["keyId"]}, query, body, identity)
        if path == "/internal/gateway/api":
            rows, _ = await self.store.list(
                "api", {"serviceId": query.get("serviceId"), "path": query.get("path")}, 1, 100
            )
            requested_path = str(query.get("path") or "")
            return ok(next((row for row in rows if row.get("path") == requested_path), rows[0] if rows else None))

        authority_links = {
            "/authority/roles/{roleId}": ("base_authority_role", "role_id", "roleId"),
            "/authority/users/{userId}": ("base_authority_user", "user_id", "userId"),
            "/authority/apps/{appId}": ("base_authority_app", "app_id", "appId"),
            "/authority/actions/{actionId}": ("base_authority_action", "action_id", "actionId"),
        }
        if path in authority_links:
            table, owner_column, param_name = authority_links[path]
            return await self._grant(method, table, owner_column, params[param_name], body)
        if path in {"/authority/resources", "/authority/apis", "/authority/apis/grantable", "/authority/catalog"}:
            rows, _ = await self.store.list("authority", query, 1, 100)
            return ok(rows)
        if path in {"/authority/menus", "/authority/menus/tree"}:
            return ok(await self.governance.current_menus(identity))

        if path == "/api/services":
            rows, _ = await self.store.list("api", {}, 1, 100)
            return ok(sorted({str(row.get("serviceId")) for row in rows if row.get("serviceId")}))

        policy_links = {
            "/gateway/limit/ip/{policyId}/apis": ("gateway_ip_limit_api", "policy_id", "policyId"),
            "/gateway/limit/rate/{policyId}/apis": ("gateway_rate_limit_api", "policy_id", "policyId"),
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
            if path == "/baseOrg/findTopCompany":
                org = await self.store.get("org", body.get("id") or body.get("orgId"))
                while org and org.get("parentId"):
                    org = await self.store.get("org", org["parentId"])
                return ok(org)
            if path == "/baseOrg/findRelegationCompany":
                rows, _ = await self.store.list("org", {}, 1, 100)
                root = str(body.get("id") or body.get("orgId"))
                return ok([row for row in rows if root in str(row.get("leafPath") or "").split(",") or str(row.get("id")) == root])
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
            return await self._special("POST", "/user/sessions" if path.endswith("sessions") else "/user/third-party-accounts", params, query, body, identity)

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
            saved = await self.store.save("extendForm", {**body, "formCode": form_code, "version": (existing or {}).get("version", 0) + 1}, (existing or {}).get("id"))
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
            if data_source_type.lower() == "http" and not all(body.get(key) for key in ("url", "requestMethod", "requestHeader")):
                raise ValueError("HTTP 数据源必须提供 URL、请求方式和请求头")
            return ok(await self.store.save("dataSource", body, body.get("id")))

        if path.startswith("/api-docs") or path.startswith("/published-docs"):
            return await self._openapi(method, path, params, query, body)
        return None

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

    async def _create_user(self, payload: Mapping[str, Any]) -> dict[str, Any]:
        username = str(payload.get("userName") or payload.get("username") or payload.get("account") or "").strip()
        if not username:
            raise ValueError("用户名不能为空")
        user = await self.store.save(
            "user",
            {
                **payload,
                "userName": username,
                "nickName": payload.get("nickName") or username,
                "realName": payload.get("realName") or payload.get("nickName") or username,
                "status": 1,
                "userType": payload.get("userType") or "normal",
            },
        )
        password = str(payload.get("password") or "")
        if password:
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
        return user

    async def _set_password(self, user_id: int, password: str, origin: str | None = None) -> None:
        if len(password) < 8:
            raise ValueError("密码长度不能少于 8 位")
        rows, _ = await self.store.list("account", {"userId": user_id}, 1, 100)
        if not rows:
            raise ValueError("用户没有可用的登录账号")
        if origin is not None and not _verify(origin, str(rows[0].get("password") or "")):
            raise ValueError("原密码错误")
        await self.store.update_where("account", {"userId": user_id}, {"password": _hash(password), "mustChangePassword": 0})

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
            return ok(await self.store.save("openApiOperation", {"examplesJson": json.dumps(body, ensure_ascii=False)}, params["operationId"]))
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
                    {**body, "publishedSpec": json.dumps(spec, ensure_ascii=False), "publishedAt": datetime.now()},
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
        max(int(values.get("pageForm.currPage") or values.get("currPage") or values.get("page") or 1), 1),
        min(max(int(values.get("pageForm.pageSize") or values.get("pageSize") or values.get("size") or 10), 1), 100),
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
