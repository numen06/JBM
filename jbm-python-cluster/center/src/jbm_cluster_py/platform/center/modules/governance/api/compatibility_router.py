from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Request

from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import (
    CompatibilityService,
)


ROUTES = """
DELETE /action/{actionId}
DELETE /api
DELETE /api/{apiId}
DELETE /apikey/{keyId}
DELETE /app/{appId}
DELETE /gateway/limit/ip/{policyId}
DELETE /gateway/limit/rate/{policyId}
DELETE /gateway/routes/{routeId}
DELETE /menu/{menuId}
DELETE /role/{roleId}
GET /action
GET /action/{actionId}
GET /api
GET /api-docs/operations
GET /api-docs/operations/{operationId}
GET /api-docs/sources
GET /api-docs/spec/{serviceId}
GET /api/{apiId}
GET /api/services
GET /apikey
GET /apikey/{keyId}
GET /apikey/{keyId}/authority
GET /apikey/{keyId}/check
GET /app
GET /app/{appId}
GET /app/{appId}/secret
GET /authority/actions/{actionId}
GET /authority/apis
GET /authority/apis/grantable
GET /authority/apps/{appId}
GET /authority/catalog
GET /authority/menus
GET /authority/menus/tree
GET /authority/resources
GET /authority/roles/{roleId}
GET /authority/users/{userId}
GET /baseAppConfig/getAppConfigByKey
GET /baseArea/getChinaAreaList
GET /baseDic/getDicMap
GET /baseUserCertification/currentUserCert
GET /current/dashboard
GET /current/user
GET /current/user/menus
GET /developer
GET /developer/{userId}
GET /developer/all
GET /developer/pending
GET /extend-field/forms
GET /extend-field/forms/{formCode}
GET /extend-field/forms/{formCode}/definitions
GET /gateway/api/blackList
GET /gateway/api/rateLimit
GET /gateway/api/route
GET /gateway/api/whiteList
GET /gateway/limit/ip
GET /gateway/limit/ip/{policyId}
GET /gateway/limit/ip/{policyId}/apis
GET /gateway/limit/rate
GET /gateway/limit/rate/{policyId}
GET /gateway/limit/rate/{policyId}/apis
GET /gateway/routes
GET /gateway/routes/{routeId}
GET /gateway/routes/micro-services
GET /gateway/service/list
GET /menu
GET /menu/{menuId}
GET /menu/{menuId}/actions
GET /menu/all
GET /menu/current
GET /menu/export
GET /published-docs/openapi
GET /published-docs/openapi/{docKey}
GET /role
GET /role/{roleId}
GET /role/{roleId}/users
GET /role/all
GET /user
GET /user/{userId}
GET /user/{userId}/accounts
GET /user/{userId}/orgs
GET /user/{userId}/roles
GET /user/all
GET /user/statistics
PATCH /api
PATCH /user/{userId}
PATCH /user/{userId}/status
POST /action
POST /api
POST /api-docs/export
POST /api-docs/operations/{operationId}/use-cases
POST /api-docs/publish
POST /api-docs/sync
POST /api-docs/test
POST /apikey
POST /app
POST /baseAccountLogs/delete
POST /baseAccountLogs/deleteByIds
POST /baseAccountLogs/list
POST /baseAccountLogs/mock
POST /baseAccountLogs/model
POST /baseAccountLogs/pageList
POST /baseAccountLogs/save
POST /baseAccountLogs/saveBatch
POST /baseDic/delete
POST /baseDic/deleteByIds
POST /baseDic/items/pageList
POST /baseDic/list
POST /baseDic/mock
POST /baseDic/model
POST /baseDic/pageList
POST /baseDic/root
POST /baseDic/root/pageList
POST /baseDic/save
POST /baseDic/saveBatch
POST /baseDic/tree
POST /baseOrg/delete
POST /baseOrg/deleteByIds
POST /baseOrg/findRelegationCompany
POST /baseOrg/findTopCompany
POST /baseOrg/getBaseOrg
POST /baseOrg/list
POST /baseOrg/mock
POST /baseOrg/model
POST /baseOrg/pageList
POST /baseOrg/root
POST /baseOrg/save
POST /baseOrg/saveBatch
POST /baseOrg/tree
POST /baseReleaseInfo/findLastVersionInfo
POST /baseUserCertification/updateFaceImage
POST /customForms/getDetail
POST /customForms/saveData
POST /dataSourceManagement/saveData
POST /developer
POST /developer/apply
POST /developer/sessions
POST /developer/third-party-accounts
POST /extend-field/forms/{formCode}
POST /extend-field/forms/{formCode}/publish
POST /gateway/limit/ip
POST /gateway/limit/rate
POST /gateway/routes
POST /menu
POST /menu/imports
POST /menu/sync-from-jbm
POST /role
POST /user
POST /user/{userId}/closure
POST /user/registrations
POST /user/sessions
POST /user/sessions/mobile
POST /user/third-party-account-bindings
POST /user/third-party-accounts
PUT /action/{actionId}
PUT /api/{apiId}
PUT /apikey/{keyId}
PUT /apikey/{keyId}/authority
PUT /apikey/{keyId}/secret
PUT /apikey/{keyId}/status
PUT /app/{appId}
PUT /app/{appId}/client
PUT /app/{appId}/secret
PUT /authority/actions/{actionId}
PUT /authority/apps/{appId}
PUT /authority/roles/{roleId}
PUT /authority/users/{userId}
PUT /current/user
PUT /current/user/password
PUT /developer/{userId}
PUT /developer/{userId}/approve
PUT /developer/{userId}/password
PUT /extend-field/forms/{formCode}
PUT /gateway/limit/ip/{policyId}
PUT /gateway/limit/ip/{policyId}/apis
PUT /gateway/limit/rate/{policyId}
PUT /gateway/limit/rate/{policyId}/apis
PUT /gateway/routes/{routeId}
PUT /menu/{menuId}
PUT /role/{roleId}
PUT /role/{roleId}/users
PUT /user/{userId}
PUT /user/{userId}/activations/email
PUT /user/{userId}/activations/mobile
PUT /user/{userId}/orgs
PUT /user/{userId}/password
PUT /user/{userId}/roles
PUT /user/accounts/open-id
GET /internal/gateway/api
GET /internal/gateway/apikey
GET /internal/gateway/apikey/{keyId}/check
POST /authenticate/{loginType}/login
"""


def route_set() -> set[tuple[str, str]]:
    return {tuple(line.split(" ", 1)) for line in ROUTES.splitlines() if line.strip()}


def build_compatibility_router(service: CompatibilityService) -> APIRouter:
    router = APIRouter()
    routes = sorted(route_set(), key=lambda item: ("{" in item[1], -len(item[1]), item[0]))
    for method, path in routes:
        router.add_api_route(
            path,
            _handler(service, method, path),
            methods=[method],
            name="center_" + method.lower() + "_" + path.strip("/").replace("/", "_").replace("{", "").replace("}", ""),
        )
    return router


def _handler(service: CompatibilityService, method: str, template: str):
    async def endpoint(request: Request) -> Any:
        body: Any = {}
        if method in {"POST", "PUT", "PATCH", "DELETE"}:
            content_type = request.headers.get("content-type", "")
            try:
                if "application/json" in content_type:
                    body = await request.json()
                elif "form" in content_type or "multipart" in content_type:
                    body = dict(await request.form())
            except Exception:
                body = {}
        return await service.handle(
            method,
            template,
            request.path_params,
            dict(request.query_params),
            body,
            request.state.identity,
        )

    return endpoint
