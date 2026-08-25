from __future__ import annotations

from typing import Any, Mapping, Protocol


class GovernanceRepository(Protocol):
    async def start(self) -> None: ...

    async def stop(self) -> None: ...

    async def health(self) -> dict[str, Any]: ...

    async def list_users(
        self, page: int, size: int, keyword: str | None, filters: Mapping[str, Any]
    ) -> tuple[list[dict[str, Any]], int]: ...

    async def get_user(self, user_id: int) -> dict[str, Any] | None: ...

    async def is_user_member(self, user_id: int, tenant_id: int) -> bool: ...

    async def user_roles(
        self, user_id: int, app_id: int | None = None, tenant_id: int | None = None
    ) -> list[dict[str, Any]]: ...

    async def user_orgs(self, user_id: int) -> list[dict[str, Any]]: ...

    async def user_accounts(self, user_id: int) -> list[dict[str, Any]]: ...

    async def user_authorities(
        self,
        user_id: int,
        is_admin: bool,
        app_id: int | None = None,
        tenant_id: int | None = None,
    ) -> list[dict[str, Any]]: ...

    async def user_menus(
        self, user_id: int, app_id: int | None, is_admin: bool, tenant_id: int | None = None
    ) -> list[dict[str, Any]]: ...

    async def list_orgs(self, keyword: str | None = None) -> list[dict[str, Any]]: ...

    async def list_dicts(self, parent_id: int | None = None) -> list[dict[str, Any]]: ...

    async def list_apps(self, page: int, size: int, filters: Mapping[str, Any]) -> tuple[list[dict[str, Any]], int]: ...

    async def find_tenant_delegation(
        self,
        owner_tenant_id: int,
        operator_tenant_id: int,
        operator_user_id: int,
        app_id: int,
        permission: str,
        resource_type: str | None = None,
    ) -> dict[str, Any] | None: ...

    async def list_roles(
        self, page: int, size: int, filters: Mapping[str, Any]
    ) -> tuple[list[dict[str, Any]], int]: ...

    async def list_routes(
        self, page: int, size: int, filters: Mapping[str, Any]
    ) -> tuple[list[dict[str, Any]], int]: ...

    async def dashboard_counts(self, tenant_id: int | None = None) -> dict[str, int]: ...

    async def list_app_features(self, app_id: int) -> list[dict[str, Any]]: ...

    async def create_app_feature(
        self, app_id: int, feature_code: str, feature_name: str, feature_desc: str | None
    ) -> dict[str, Any]: ...

    async def disable_app_feature(self, app_id: int, feature_code: str) -> None: ...

    async def list_tenant_features(self, tenant_id: int, app_id: int) -> list[dict[str, Any]]: ...

    async def effective_user_features(
        self, user_id: int, tenant_id: int, app_id: int, tenant_admin: bool
    ) -> list[str]: ...

    async def list_feature_tenants(self, app_id: int) -> list[dict[str, Any]]: ...

    async def replace_tenant_features(
        self,
        tenant_id: int,
        app_id: int,
        feature_codes: list[str],
        granted_by: int,
    ) -> list[str]: ...

    async def list_tenant_members(self, tenant_id: int, app_id: int) -> list[dict[str, Any]]: ...

    async def replace_member_features(
        self,
        tenant_id: int,
        app_id: int,
        user_id: int,
        feature_codes: list[str],
        granted_by: int,
    ) -> list[str]: ...
