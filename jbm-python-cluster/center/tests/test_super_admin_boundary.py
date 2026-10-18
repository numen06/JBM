from unittest.mock import AsyncMock
from types import SimpleNamespace
import pytest
from fastapi import HTTPException
from jbm_cluster_py.common.api_access import require_api_boundary, authorize_api, is_super_admin
from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import CompatibilityService


@pytest.mark.parametrize("roles", [["tenant_admin"], ["emc_admin"], ["platform_operator"], []])
@pytest.mark.parametrize("path", ["/emqx/clients/foreign", "/workers/rebalance", "/workers/flow", "/admin/users/1", "/observability", "/tdengine/overview", "/air-conditioning/connections"])
def test_explicit_grants_cannot_cross_infrastructure_boundary(roles, path):
    identity = {"userId": 2, "roles": roles, "permissions": ["iot.platform.root", "iot.platform.admin", "all"]}
    with pytest.raises(HTTPException) as error:
        require_api_boundary("iot-platform", path, identity)
    assert error.value.status_code == 403
    require_api_boundary("iot-platform", path, {"userId": 1, "admin": True})


def test_heartbeat_service_exception_is_narrow():
    service = {"clientId": "worker", "scope": "internal"}
    authorize_api("iot-platform", "POST", "/workers/heartbeat", service)
    with pytest.raises(HTTPException):
        authorize_api("iot-platform", "POST", "/workers/rebalance", service)
    assert not is_super_admin({"admin": "false", "userType": "super", "permissions": ["iot.platform.root"]})


@pytest.mark.asyncio
async def test_tenant_cannot_remove_global_role_from_ordinary_type_account():
    store, governance = AsyncMock(), AsyncMock()
    store.is_user_member.return_value = True
    store.get.return_value = {"userId": 3, "companyId": 20, "userType": "normal"}
    governance.repository.user_roles.return_value = [{"role_id": 1, "role_code": "admin"}]
    service = CompatibilityService(store, governance, AsyncMock())
    with pytest.raises(HTTPException):
        await service._require_editable_user({"userId": 2, "tenantId": 20, "appId": 30, "roles": ["tenant_admin"]}, 3)
    governance.repository.user_roles.assert_awaited_once_with(3, None, None)


@pytest.mark.asyncio
async def test_global_business_role_is_assigned_with_tenant_and_app_scope():
    store, governance = AsyncMock(), AsyncMock()
    store.is_user_member.return_value = True
    store.get.side_effect = lambda kind, value: ({"userId": 3, "companyId": 20} if kind == "user" else {"roleId": 4, "roleCode": "emc_admin", "appId": None})
    governance.repository.user_roles.return_value = []
    service = CompatibilityService(store, governance, AsyncMock())
    await service.handle("PUT", "/user/{userId}/roles", {"userId": 3}, {}, {"roleIds": [4]}, {"userId": 2, "tenantId": 20, "appId": 30, "roles": ["tenant_admin"]})
    assert store.replace_scoped_links.await_args.args == ("base_role_user", "user_id", 3, "role_id", [4], "app_id", 30, "tenant_id", 20)


@pytest.mark.asyncio
async def test_userinfo_uses_current_roles_instead_of_embedded_admin_claim():
    from jbm_cluster_py.platform.auth.service import AuthService
    service = object.__new__(AuthService)
    service.signer = SimpleNamespace(verify=lambda _: {'user_id': 3, 'admin': True, 'username': 'former_admin'})
    service._require_active_access = AsyncMock()
    service.repository = SimpleNamespace(user_roles=AsyncMock(return_value=[{'role_id': 4, 'role_code': 'emc_admin'}]))
    service._permissions_for_claims = AsyncMock(return_value=[])
    identity = await service.userinfo('previously-issued-token')
    assert identity['admin'] is False and identity['roles'] == ['emc_admin']


@pytest.mark.asyncio
@pytest.mark.parametrize('operation', ['create', 'rename', 'delete', 'grant'])
async def test_platform_operator_cannot_create_or_modify_reserved_roles(operation):
    store = AsyncMock()
    store.get.return_value = {'roleId': 1, 'roleCode': 'admin'} if operation != 'rename' else {'roleId': 4, 'roleCode': 'business'}
    service = CompatibilityService(store, AsyncMock(), AsyncMock())
    actor = {'userId': 2, 'tenantId': 20, 'appId': 30, 'roles': ['platform_operator']}
    with pytest.raises(HTTPException):
        if operation == 'create':
            await service._collection('POST', 'role', {}, {'roleCode': 'super_admin'}, actor)
        elif operation == 'rename':
            await service._item('PUT', 'role', 4, {'roleCode': 'super_admin'}, actor)
        elif operation == 'delete':
            await service._item('DELETE', 'role', 1, {}, actor)
        else:
            await service.handle('PUT', '/authority/roles/{roleId}', {'roleId': 1}, {}, {'authorityIds': []}, actor)
    store.save.assert_not_awaited()
    store.delete.assert_not_awaited()
    store.replace_links.assert_not_awaited()
