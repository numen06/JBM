"""The public Auth routes must enforce current permissions before touching sessions."""
import asyncio

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.auth.router import build_auth_router
from jbm_cluster_py.platform.auth.service import TokenCache, _hash_token
from test_session_lifecycle import Repository, issue, service
from test_terminal_sessions import Terminals, login


@pytest.mark.parametrize('path', ['/online/pageList', '/online/expire', '/online/expireImmediately', '/online/kickout/', '/online/logout/'])
def test_unprivileged_user_cannot_enumerate_or_modify_sessions(path):
    auth = service(TokenCache(RedisClient({'enabled': False})), Terminals())
    caller = asyncio.run(login(auth, 'web'))
    target = asyncio.run(login(auth, 'app'))
    target_id = _hash_token(target['access_token'])
    app = FastAPI()
    app.include_router(build_auth_router(auth))
    with TestClient(app) as client:
        headers = {'Authorization': 'Bearer ' + caller['access_token']}
        if path.endswith('/'):
            response = client.delete(path + target_id, headers=headers)
        else:
            response = client.post(path, json={'tokenId': target_id, 'minutes': 1}, headers=headers)
        assert response.status_code == 403
        assert response.json()['result'] is None
        for token in [caller, target]:
            assert client.get('/oauth2/userinfo', headers={'Authorization': 'Bearer ' + token['access_token']}).status_code == 200


def test_permission_revocation_denies_existing_token_and_menu_does_not_grant_mutation():
    class MutablePermissions(Repository):
        grants = ['MENU_onlineUsers']

        async def user_roles(self, *args):
            return [{'role_id': 2, 'role_code': 'platform_operator'}]

        async def user_authorities(self, *args):
            return self.grants

    repo = MutablePermissions()
    auth = service(TokenCache(RedisClient({'enabled': False})), repo)
    token = asyncio.run(issue(auth))
    app = FastAPI()
    app.include_router(build_auth_router(auth))
    with TestClient(app) as client:
        client.headers['Authorization'] = 'Bearer ' + token['access_token']
        assert client.post('/online/pageList', json={}).status_code == 200
        session_id = _hash_token(token['access_token'])
        assert client.post('/online/expire', json={'tokenId': session_id}).status_code == 403
        repo.grants = ['ACTION_monitor:online:forceLogout']
        assert client.post('/online/expire', json={'tokenId': session_id, 'minutes': 1}).status_code == 200
        repo.grants = []
        assert client.post('/online/expireImmediately', json={'tokenId': session_id}).status_code == 403
        assert client.post('/online/pageList', json={}).status_code == 403
        assert client.get('/oauth2/userinfo').status_code == 200


def test_session_menu_and_actions_do_not_elevate_tenant_to_platform_monitor():
    class TenantWithActions(Repository):
        async def user_authorities(self, *args):
            return ['MENU_onlineUsers', 'ACTION_monitor:online:forceLogout', 'ACTION_monitor:online:logout']

    auth = service(TokenCache(RedisClient({'enabled': False})), TenantWithActions())
    token = asyncio.run(issue(auth))
    app = FastAPI()
    app.include_router(build_auth_router(auth))
    with TestClient(app) as client:
        client.headers['Authorization'] = 'Bearer ' + token['access_token']
        session_id = _hash_token(token['access_token'])
        for path in ['/online/pageList', '/online/expire', '/online/expireImmediately']:
            assert client.post(path, json={'tokenId': session_id}).status_code == 403
        for path in ['/online/kickout/', '/online/logout/']:
            assert client.delete(path + session_id).status_code == 403
        assert client.get('/oauth2/userinfo').status_code == 200
