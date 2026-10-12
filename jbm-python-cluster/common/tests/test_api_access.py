import hashlib

import httpx
import pytest
from fastapi import HTTPException, Request

from jbm_cluster_py.common.api_access import authorize_api, signature_payload
from jbm_cluster_py.common.auth import UserInfoAuthClient


def person(*permissions):
    return {"userId": 42, "tenantId": 7, "clientId": "browser", "permissions": list(permissions)}


@pytest.mark.parametrize(
    "service,method,path",
    [
        ("center", "POST", "/app/model"),
        ("center", "GET", "/role"),
        ("job", "GET", "/sysJob/run"),
        ("logs", "POST", "/GatewayLogs/findLogs"),
        ("gateway", "GET", "/gateway/discovery/services"),
        ("center", "GET", "/not-registered"),
    ],
)
def test_valid_user_token_does_not_grant_platform_or_unknown_api(service, method, path):
    with pytest.raises(HTTPException) as exc:
        authorize_api(service, method, path, {**person("MENU_apps"), "roles": ["tenant_admin"]})
    assert exc.value.status_code == 403


def test_read_permission_does_not_grant_write_or_sibling_menu():
    user = person("MENU_iot_resources", "iot.platform.read")
    authorize_api("iot-platform", "GET", "/devices/123", user)
    for method, path in [("POST", "/devices"), ("GET", "/observability"), ("GET", "/streetlights")]:
        with pytest.raises(HTTPException):
            authorize_api("iot-platform", method, path, user)


def test_edit_does_not_authorize_delete_or_get_remove_alias():
    user = person("MENU_users", "ACTION_users_edit")
    authorize_api("center", "PUT", "/user/123", user)
    with pytest.raises(HTTPException):
        authorize_api("center", "DELETE", "/user/123", user)
    with pytest.raises(HTTPException):
        authorize_api("doc", "GET", "/remove/file.txt", person("MENU_doc_files"))
    with pytest.raises(HTTPException):
        authorize_api("doc", "GET", "/remove/file.txt", person("MENU_doc_files", "ACTION_doc_files_add"))
    authorize_api("doc", "GET", "/remove/file.txt", person("MENU_doc_files", "ACTION_doc_files_delete"))


def test_service_terminal_keeps_explicit_bearer_operation_grants():
    terminal = {"clientId": "collector", "tenantId": 7, "permissions": ["iot.platform.read"]}
    authorize_api("iot-platform", "GET", "/devices", terminal)
    with pytest.raises(HTTPException):
        authorize_api("iot-platform", "POST", "/devices", terminal)


def test_human_internal_scope_cannot_impersonate_service():
    with pytest.raises(HTTPException):
        authorize_api("center", "GET", "/internal/gateway/api", person("internal"))
    authorize_api(
        "center", "GET", "/internal/gateway/api", {"clientId": "gateway", "scope": "internal"}
    )


def test_platform_statistics_are_available_to_platform_only():
    authorize_api('logs', 'POST', '/clusterAccess/getClusterAccessInfo', {**person(), 'admin': True})
    with pytest.raises(HTTPException) as exc:
        authorize_api('logs', 'POST', '/clusterAccess/getClusterAccessInfo', person('MENU_onlineUsers'))
    assert exc.value.status_code == 403


def test_owned_project_scope_check_preserves_native_iot_bearer_permissions():
    terminal = {'clientId': 'iot-reader', 'tenantId': 7, 'permissions': ['iot.platform.read']}
    authorize_api('building', 'GET', '/api/v1/building/project-scope', terminal)
    authorize_api('building', 'GET', '/api/v1/building/project-scope', person('all'))
    for method, path in [('POST', '/api/v1/building/project-scope'), ('GET', '/api/v1/building/master-data/projects')]:
        with pytest.raises(HTTPException):
            authorize_api('building', method, path, terminal)


def test_signing_binds_target_body_and_data_scope():
    args = [
        "iot-platform",
        "POST",
        "/devices",
        "x=1",
        hashlib.sha256(b"{}").hexdigest(),
        "key",
        "1",
        "nonce",
        "7",
        "project",
    ]
    original = signature_payload(*args)
    for index, value in [
        (0, "center"),
        (1, "PUT"),
        (2, "/devices/1"),
        (4, "0" * 64),
        (8, "8"),
        (9, "other"),
    ]:
        changed = args.copy()
        changed[index] = value
        assert signature_payload(*changed) != original


@pytest.mark.asyncio
async def test_bearer_terminal_needs_no_signature_headers_and_backend_failure_denies():
    calls = []

    def handler(request):
        calls.append(request)
        return httpx.Response(
            200,
            json={"success": True, "result": {"allowed": True, "identity": person("MENU_users")}},
        )

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        auth = UserInfoAuthClient({}, client)
        request = Request(
            {
                "type": "http",
                "method": "GET",
                "path": "/user",
                "query_string": b"",
                "headers": [
                    (b"authorization", b"Bearer existing-terminal"),
                    (b"x-app-id", b"legacy-app-header"),
                ],
                "scheme": "http",
                "server": ("test", 80),
            }
        )
        assert (await auth.authenticate_request(request, "center"))["userId"] == 42
        assert calls[0].headers["Authorization"] == "Bearer existing-terminal"
        assert b'"key"' not in calls[0].content
    async with httpx.AsyncClient(
        transport=httpx.MockTransport(lambda _: httpx.Response(503, json={}))
    ) as client:
        auth = UserInfoAuthClient({}, client)
        request = Request({**request.scope, "state": {}})
        with pytest.raises(HTTPException) as exc:
            await auth.authenticate_request(request, "center")
        assert exc.value.status_code == 503
