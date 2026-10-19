"""Self-service password changes use only the authenticated account identity."""
from unittest.mock import AsyncMock

import bcrypt
import pytest

from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import CompatibilityService, _hash


@pytest.mark.asyncio
@pytest.mark.parametrize('origin', ['', 'Wrong!234'])
async def test_wrong_or_missing_current_password_does_not_write(origin):
    store = AsyncMock()
    store.list.return_value = ([{'userId': 7, 'password': _hash('Original!234')}], 1)
    api = CompatibilityService(store, AsyncMock(), AsyncMock())
    with pytest.raises(ValueError, match='原密码错误'):
        await api.handle('PUT', '/current/user/password', {}, {}, {
            'originPassword': origin, 'currentPassword': 'Replacement!234',
            'confirmPassword': 'Replacement!234',
        }, {'userId': 7, 'tenantId': 20, 'appId': 30, 'roles': []})
    store.update_where.assert_not_awaited()


@pytest.mark.asyncio
async def test_password_change_ignores_other_user_id_and_updates_only_self():
    store = AsyncMock()
    store.list.return_value = ([{'userId': 7, 'password': _hash('Original!234')}], 1)
    api = CompatibilityService(store, AsyncMock(), AsyncMock())
    await api.handle('PUT', '/current/user/password', {}, {}, {
        'userId': 999, 'originPassword': 'Original!234', 'currentPassword': 'Replacement!234',
        'confirmPassword': 'Replacement!234',
    }, {'userId': 7, 'tenantId': 20, 'appId': 30, 'roles': []})
    store.list.assert_awaited_once_with('account', {'userId': 7}, 1, 100, include_secrets=True)
    resource, query, changes = store.update_where.await_args.args
    assert resource == 'account' and query == {'userId': 7}
    assert changes['mustChangePassword'] == 0
    assert bcrypt.checkpw(b'Replacement!234', changes['password'].encode())


@pytest.mark.asyncio
@pytest.mark.parametrize('new,confirm,error', [
    ('short', 'short', '密码'),
    ('Replacement!234', 'Different!234', '两次输入'),
])
async def test_invalid_new_password_does_not_write(new, confirm, error):
    store = AsyncMock()
    api = CompatibilityService(store, AsyncMock(), AsyncMock())
    with pytest.raises(ValueError, match=error):
        await api.handle('PUT', '/current/user/password', {}, {}, {
            'originPassword': 'Original!234', 'currentPassword': new, 'confirmPassword': confirm,
        }, {'userId': 7, 'tenantId': 20, 'appId': 30, 'roles': []})
    store.update_where.assert_not_awaited()
