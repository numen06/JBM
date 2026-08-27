from unittest.mock import AsyncMock, call

import pytest
from fastapi import HTTPException

from jbm_cluster_py.platform.center.modules.governance.application.service import GovernanceService
from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import (
    CompatibilityService,
)


TENANT = {"userId": 2002, "tenantId": 2000, "roles": ["tenant_admin"]}
OPERATOR = {"userId": 2001, "tenantId": 1, "roles": ["platform_operator"]}
IOT_OPERATOR = {**OPERATOR, "appId": 3000}
SUPER_ADMIN = {"userId": 1, "tenantId": 1, "roles": ["super_admin"]}


@pytest.mark.asyncio
async def test_tenant_filters_override_client_values() -> None:
    repository = AsyncMock()
    repository.list_users.return_value = ([{"userId": 2002, "companyId": 2000}], 1)
    repository.list_apps.return_value = ([{"appId": 2000, "orgId": 2000}], 1)
    service = GovernanceService(repository)

    await service.users(1, 10, None, {"companyId": 1}, TENANT)
    await service.apps(1, 10, {"orgId": 1}, TENANT)

    assert repository.list_users.await_args.args[3]["tenantId"] == 2000
    assert "companyId" not in repository.list_users.await_args.args[3]
    assert repository.list_apps.await_args.args[2]["orgId"] == 2000


@pytest.mark.asyncio
async def test_tenant_cannot_read_cross_tenant_user() -> None:
    repository = AsyncMock()
    repository.get_user.return_value = {"userId": 1, "companyId": 1}
    repository.is_user_member.return_value = False
    service = GovernanceService(repository)

    with pytest.raises(HTTPException) as error:
        await service.user(1, TENANT)
    assert error.value.status_code == 403

    assert await service.user(1, OPERATOR) == {"userId": 1, "companyId": 1}


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("method", "path"),
    [
        ("POST", "/baseAccountLogs/list"),
        ("POST", "/dataSourceManagement/saveData"),
        ("POST", "/api-docs/test"),
        ("GET", "/internal/gateway/apikey"),
    ],
)
async def test_tenant_cannot_use_platform_or_internal_compatibility_paths(
    method: str, path: str
) -> None:
    service = CompatibilityService(AsyncMock(), AsyncMock(), AsyncMock())

    with pytest.raises(HTTPException) as error:
        await service.handle(method, path, {}, {}, {}, TENANT)

    assert error.value.status_code == 403


@pytest.mark.asyncio
@pytest.mark.parametrize("identity", [TENANT, OPERATOR])
async def test_only_super_admin_can_manage_app_login_branding(identity: dict) -> None:
    service = CompatibilityService(AsyncMock(), AsyncMock(), AsyncMock())

    with pytest.raises(HTTPException) as error:
        await service.handle("GET", "/baseAppConfig/{appId}", {"appId": 3000}, {}, {}, identity)

    assert error.value.status_code == 403


@pytest.mark.asyncio
async def test_super_admin_updates_app_login_branding_without_losing_other_config() -> None:
    store = AsyncMock()
    store.list.return_value = (
        [
            {
                "id": 7000,
                "appId": 3000,
                "appKey": "smart-building-client",
                "orgId": None,
                "configContent": '{"desc":"保留说明","sysLogo":"old.png"}',
            }
        ],
        1,
    )
    store.get.return_value = {"appId": 3000, "code": "smart-building"}
    store.save.side_effect = [
        {"id": 7000, "appId": 3000, "appKey": "smart-building-client"},
        {"appId": 3000, "appName": "中共江西省委党校智慧建筑平台"},
    ]
    service = CompatibilityService(store, AsyncMock(), AsyncMock())

    result = await service.handle(
        "PUT",
        "/baseAppConfig/{appId}",
        {"appId": 3000},
        {},
        {
            "title": "中共江西省委党校智慧建筑平台",
            "sysBg": "party-school.png",
            "sysLogo": "",
        },
        SUPER_ADMIN,
    )

    saved_config = store.save.await_args_list[0].args[1]["configContent"]
    assert '"desc": "保留说明"' in saved_config
    assert '"sysBg": "party-school.png"' in saved_config
    assert result["result"]["configContent"]["sysLogo"] == ""
    assert call("app", {"appName": "中共江西省委党校智慧建筑平台"}, 3000) in store.save.await_args_list


@pytest.mark.asyncio
async def test_new_user_roles_are_scoped_to_target_tenant_and_app() -> None:
    store = AsyncMock()
    store.find_user_by_username.return_value = None
    store.save.return_value = {
        "userId": 4000,
        "userName": "iot_viewer",
        "companyId": 5000,
    }
    store.get.return_value = {
        "roleId": 6000,
        "roleCode": "iot_viewer",
        "appId": 3000,
    }
    service = CompatibilityService(store, AsyncMock(), AsyncMock())

    await service._create_user(
        {
            "userName": "iot_viewer",
            "companyId": 5000,
            "roleIds": [6000],
            "password": "Iot#2026Pass",
        },
        IOT_OPERATOR,
    )

    store.replace_scoped_links.assert_awaited_once_with(
        "base_role_user",
        "user_id",
        4000,
        "role_id",
        [6000],
        "app_id",
        3000,
        "tenant_id",
        5000,
    )


@pytest.mark.asyncio
async def test_existing_user_joins_current_tenant_without_duplicate_account() -> None:
    store = AsyncMock()
    store.find_user_by_username.return_value = {
        "userId": 4100,
        "userName": "shared_user",
        "companyId": 9000,
    }
    store.get.return_value = {
        "roleId": 6000,
        "roleCode": "iot_viewer",
        "appId": 3000,
    }
    service = CompatibilityService(store, AsyncMock(), AsyncMock())

    result = await service._create_user(
        {"userName": "shared_user", "existingOnly": True, "roleIds": [6000]},
        {"userId": 5001, "tenantId": 5000, "appId": 3000, "roles": ["iot_admin"]},
    )

    store.ensure_link.assert_awaited_once_with(
        "base_user_org", "user_id", 4100, "org_id", 5000
    )
    store.save.assert_not_awaited()
    store.replace_scoped_links.assert_awaited_once()
    assert result["joinedExisting"] is True
    assert result["companyId"] == 9000


@pytest.mark.asyncio
async def test_owner_delegates_to_existing_operator_account() -> None:
    store = AsyncMock()
    store.find_user_by_username.return_value = {
        "userId": 7100,
        "userName": "operator_a",
        "companyId": 7000,
    }
    store.save.return_value = {
        "id": 7200,
        "ownerTenantId": 5000,
        "operatorTenantId": 7000,
        "operatorUserId": 7100,
    }
    service = CompatibilityService(store, AsyncMock(), AsyncMock())

    await service.handle(
        "POST",
        "/tenant-delegation",
        {},
        {},
        {
            "operatorAccount": "operator_a",
            "permissionCodes": ["iot.platform.read"],
            "dataScope": {"projectIds": [501]},
        },
        {"userId": 5001, "tenantId": 5000, "appId": 3000, "roles": ["iot_admin"]},
    )

    saved = store.save.await_args.args[1]
    assert saved["ownerTenantId"] == 5000
    assert saved["operatorTenantId"] == 7000
    assert saved["operatorUserId"] == 7100
    assert '"*"' in saved["resourceTypes"]
    assert "501" in saved["dataScope"]


@pytest.mark.asyncio
async def test_tenant_admin_can_only_delegate_granted_features() -> None:
    repository = AsyncMock()
    repository.replace_member_features.return_value = ["energy.solar"]
    service = GovernanceService(repository)
    identity = {"userId": 5001, "tenantId": 5000, "appId": 3000, "roles": ["iot_admin"]}

    result = await service.replace_member_features(identity, 5100, ["energy.solar"])

    assert result == ["energy.solar"]
    repository.replace_member_features.assert_awaited_once_with(
        5000, 3000, 5100, ["energy.solar"], 5001
    )


@pytest.mark.asyncio
async def test_only_platform_admin_can_set_tenant_feature_ceiling() -> None:
    repository = AsyncMock()
    service = GovernanceService(repository)

    with pytest.raises(HTTPException) as error:
        await service.replace_tenant_features(
            {"userId": 5001, "tenantId": 5000, "appId": 3000, "roles": ["iot_admin"]},
            5000,
            ["energy.storage"],
        )

    assert error.value.status_code == 403


@pytest.mark.asyncio
async def test_action_authority_inherits_menu_app_scope() -> None:
    store = AsyncMock()
    store.get.return_value = {"menuId": 10, "appId": 3000}
    store.list.return_value = ([], 0)
    service = CompatibilityService(store, AsyncMock(), AsyncMock())

    await service._sync_action_authority(
        {
            "actionId": 20,
            "actionCode": "iot.platform.operate",
            "menuId": 10,
            "status": 1,
        }
    )

    assert call("action", {"appId": 3000}, 20) in store.save.await_args_list
    assert call(
        "authority",
        {
            "authority": "ACTION_iot.platform.operate",
            "resourceType": "action",
            "menuId": 10,
            "actionId": 20,
            "appId": 3000,
            "status": 1,
        },
    ) in store.save.await_args_list


@pytest.mark.asyncio
async def test_received_delegations_use_operator_app_and_active_window() -> None:
    store = AsyncMock()
    store.list_active_delegations.return_value = ([{"id": 7000}], 1)
    service = CompatibilityService(store, AsyncMock(), AsyncMock())

    result = await service.handle(
        "GET",
        "/tenant-delegation/received",
        {},
        {"page": 2, "pageSize": 20},
        {},
        {"userId": 2003, "tenantId": 5000, "appId": 3000, "roles": ["iot_operator"]},
    )

    store.list_active_delegations.assert_awaited_once_with(5000, 2003, 3000, 2, 20)
    assert result["result"]["total"] == 1
    assert result["result"]["contents"] == [{"id": 7000}]


@pytest.mark.asyncio
async def test_operator_application_uses_authenticated_tenant_and_app() -> None:
    store = AsyncMock()
    store.list.return_value = ([], 0)
    store.save.return_value = {"id": 8000, "tenantId": 5000, "appId": 3000, "status": 0}
    service = CompatibilityService(store, AsyncMock(), AsyncMock())

    result = await service.handle(
        "POST",
        "/operator-application/current",
        {},
        {},
        {"tenantId": 9999, "appId": 9999, "reason": "园区设备运维能力"},
        {"userId": 2003, "tenantId": 5000, "appId": 3000, "roles": ["iot_admin"]},
    )

    saved = store.save.await_args.args[1]
    assert saved["tenantId"] == 5000
    assert saved["appId"] == 3000
    assert saved["applicantUserId"] == 2003
    assert result["result"]["status"] == 0


@pytest.mark.asyncio
async def test_operator_application_review_is_platform_only() -> None:
    service = CompatibilityService(AsyncMock(), AsyncMock(), AsyncMock())
    with pytest.raises(HTTPException) as error:
        await service.handle(
            "PUT",
            "/operator-application/{id}/review",
            {"id": 8000},
            {},
            {"status": 1},
            TENANT,
        )
    assert error.value.status_code == 403
