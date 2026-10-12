from unittest.mock import AsyncMock

import pytest
from fastapi import HTTPException

from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import CompatibilityService

TENANT = {"userId": 2002, "tenantId": 2000, "appId": 3000, "roles": ["tenant_admin"]}


def service(user=None):
    store = AsyncMock()
    store.is_user_member.return_value = True
    store.get.return_value = user or {"userId": 4000, "userName": "member", "userType": "normal", "companyId": 2000}
    store.save.return_value = store.get.return_value
    return CompatibilityService(store, AsyncMock(), AsyncMock()), store


@pytest.mark.asyncio
@pytest.mark.parametrize("method,path,body", [
    ("POST", "/app/model", {"appId": 1}),
    ("POST", "/app/list", {}),
    ("GET", "/app", {}),
    ("POST", "/app", {"appName": "unauthorized"}),
])
async def test_tenant_cannot_use_application_management_aliases(method, path, body):
    api, store = service()
    with pytest.raises(HTTPException) as error:
        await api.handle(method, path, {}, {}, body, TENANT)
    assert error.value.status_code == 403
    store.get.assert_not_awaited()
    store.save.assert_not_awaited()


@pytest.mark.asyncio
@pytest.mark.parametrize("payload", [
    {"userType": "super"}, {"userName": "ADMIN"}, {"userId": 1}, {"admin": True},
    {"authorities": ["ACTION_apps_add"]},
])
async def test_tenant_cannot_mass_assign_privileged_user_fields(payload):
    api, store = service()
    with pytest.raises(HTTPException) as error:
        await api._item("PUT", "user", 4000, payload, TENANT)
    assert error.value.status_code == 403
    store.save.assert_not_awaited()


@pytest.mark.asyncio
async def test_tenant_can_update_owned_member_without_changing_identity():
    api, store = service()
    await api._item("PUT", "user", 4000, {"userId": 4000, "nickName": "member name", "userType": "normal"}, TENANT)
    payload = store.save.await_args.args[1]
    assert payload == {"nickName": "member name"}
    assert store.save.await_args.args[2] == 4000


@pytest.mark.asyncio
@pytest.mark.parametrize("path", ["/user/{userId}/password", "/user/{userId}/status", "/user/{userId}/closure"])
async def test_shared_membership_does_not_allow_global_account_mutation(path):
    api, store = service({"userId": 4000, "userName": "shared", "companyId": 9000})
    with pytest.raises(HTTPException) as error:
        await api.handle("PUT", path, {"userId": 4000}, {}, {"status": 0, "password": "Unused#2026"}, TENANT)
    assert error.value.status_code == 403
    store.save.assert_not_awaited()
    store.update_where.assert_not_awaited()


@pytest.mark.asyncio
async def test_tenant_cannot_enroll_foreign_existing_account_to_take_it_over():
    api, store = service()
    store.find_user_by_username.return_value = {"userId": 4000, "userName": "foreign", "companyId": 9000}
    store.is_user_member.return_value = False
    with pytest.raises(HTTPException) as error:
        await api._create_user({"userName": "foreign", "existingOnly": True}, TENANT)
    assert error.value.status_code == 403
    store.ensure_link.assert_not_awaited()
    store.save.assert_not_awaited()


@pytest.mark.asyncio
async def test_tenant_cannot_create_or_modify_reserved_platform_admin():
    api, store = service({"userId": 4000, "userName": "admin", "userType": "super", "companyId": 2000})
    with pytest.raises(HTTPException):
        await api._item("PUT", "user", 4000, {"status": 0}, TENANT)
    with pytest.raises(HTTPException):
        await api._create_user({"userName": "new", "userId": 1, "password": "Unused#2026"}, TENANT)
    store.save.assert_not_awaited()


@pytest.mark.asyncio
async def test_legacy_user_roles_requires_membership():
    api, store = service()
    store.is_user_member.return_value = False
    with pytest.raises(HTTPException) as error:
        await api.handle("POST", "/user/userRoles", {}, {}, {"userId": 9000}, TENANT)
    assert error.value.status_code == 403


@pytest.mark.asyncio
async def test_platform_operator_cannot_create_or_edit_super_administrator():
    operator = {"userId": 6000, "tenantId": 1, "roles": ["platform_operator"]}
    api, store = service({"userId": 1, "userName": "admin", "userType": "super", "companyId": 1})
    with pytest.raises(HTTPException):
        await api._create_user({"userName": "new", "userType": "super", "password": "Unused#2026"}, operator)
    with pytest.raises(HTTPException):
        await api._item("PUT", "user", 1, {"status": 0}, operator)
    store.save.assert_not_awaited()


@pytest.mark.asyncio
@pytest.mark.parametrize("path", ["/user/sessions/mobile", "/user/registrations", "/baseDic/save", "/baseAccountLogs/list"])
async def test_legacy_platform_operations_do_not_bypass_role_checks(path):
    api, store = service()
    with pytest.raises(HTTPException) as error:
        await api.handle("POST", path, {}, {}, {}, TENANT)
    assert error.value.status_code == 403
    store.save.assert_not_awaited()
    store.list.assert_not_awaited()


def test_human_scope_cannot_impersonate_internal_service():
    from jbm_cluster_py.platform.center.modules.governance.application.access import require_internal
    with pytest.raises(HTTPException):
        require_internal({**TENANT, "scope": "internal", "clientId": "browser"})
    require_internal({"userId": None, "scope": "internal", "clientId": "registered-service"})
