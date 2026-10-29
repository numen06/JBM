"""Idle, absolute and rolling deadlines are independent server-side boundaries."""
import asyncio
import time
from datetime import datetime
from types import SimpleNamespace

import jwt.api_jwt
import pytest

from jbm_cluster_py.platform.auth import service as module
from jbm_cluster_py.platform.auth.service import AuthError, _hash_token
from test_session_lifecycle import backends, service
from test_terminal_sessions import Terminals, form, login
from test_auth_compat import PKCE_CHALLENGE, PKCE_VERIFIER


@pytest.fixture
def clock(monkeypatch):
    now = [int(time.time())]
    monkeypatch.setattr(module, 'time', SimpleNamespace(time=lambda: now[0]))

    class Clock(datetime):
        @classmethod
        def now(cls, tz=None):
            return datetime.fromtimestamp(now[0], tz=tz)

    monkeypatch.setattr(jwt.api_jwt, 'datetime', Clock)
    return now


@pytest.mark.parametrize('root,limit', [(False, 7200), (True, 3600)])
def test_polling_and_refresh_do_not_extend_human_idle_deadline(clock, root, limit):
    async def exercise():
        async with backends() as (cache, _):
            repo = Terminals()
            if root:
                repo.user['user_name'] = 'admin'
            auth = service(cache, repo)
            original = await login(auth, 'web')
            clock[0] += limit - 30
            token = await auth.refresh_token(form(original))
            status = await auth.session_status(token['access_token'])
            assert status['idleTimeoutSeconds'] == limit
            assert status['authTime'] == clock[0] - limit + 30
            assert (await auth.userinfo(token['access_token']))['authTime'] == status['authTime']
            clock[0] += 30
            with pytest.raises(AuthError, match='超时'):
                await auth.userinfo(token['access_token'])
            with pytest.raises(AuthError):
                await auth.refresh_token(form(token))
    asyncio.run(exercise())


def test_human_activity_extends_idle_but_neither_refresh_nor_activity_extends_twelve_hours(clock):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            token = await login(auth, 'web')
            initial = await auth.session_status(token['access_token'])
            for _ in range(11):
                clock[0] += 3600
                token = await auth.refresh_token(form(token))
                status = await auth.session_status(token['access_token'], activity=True)
                assert status['absoluteExpiresAt'] == initial['absoluteExpiresAt']
                assert status['authTime'] == initial['authTime']
            clock[0] += 3600
            with pytest.raises(AuthError):
                await auth.refresh_token(form(token))
    asyncio.run(exercise())


def test_mobile_fifteen_days_inactivity_and_ninety_days_absolute(clock):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            token = await login(auth, 'miniapp')
            initial = await auth.session_status(token['access_token'])
            assert initial['idleTimeoutSeconds'] is None
            assert token['refresh_expires_in'] == 15 * 86400
            for _ in range(6):
                clock[0] += 14 * 86400
                token = await auth.refresh_token(form(token))
                current = await auth.session_status(token['access_token'])
                assert current['absoluteExpiresAt'] == initial['absoluteExpiresAt']
                assert current['authTime'] == initial['authTime']
            clock[0] += 6 * 86400
            with pytest.raises(AuthError):
                await auth.refresh_token(form(token))
            token = await login(auth, 'app')
            clock[0] += 15 * 86400
            with pytest.raises(AuthError):
                await auth.refresh_token(form(token))
    asyncio.run(exercise())


def test_existing_thirty_day_token_cannot_keep_old_expiration_after_configuration_change(clock):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            auth.mobile_refresh_seconds = 30 * 86400
            token = await login(auth, 'app')
            auth.mobile_refresh_seconds = 15 * 86400
            clock[0] += 16 * 86400
            with pytest.raises(AuthError, match='超时'):
                await auth.refresh_token(form(token))
    asyncio.run(exercise())


def test_configurable_limits_and_legacy_bootstrap_preserve_original_authentication(clock):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            auth.session_policy_config['web-idle-seconds'] = 5
            token = await login(auth, 'web')
            state = await cache.get_json('refresh:' + _hash_token(token['refresh_token']))
            await cache.delete('session_policy:' + state['familyId'])
            clock[0] += 4
            status = await auth.session_status(token['access_token'])
            assert status['idleTimeoutSeconds'] == 5
            assert status['authTime'] == clock[0] - 4
            clock[0] += 1
            with pytest.raises(AuthError):
                await auth.session_status(token['access_token'], activity=True)
    asyncio.run(exercise())


def test_delayed_authorization_code_exchange_does_not_reset_recent_authentication(clock):
    from urllib.parse import parse_qs, urlparse
    async def exercise():
        async with backends() as (cache, _):
            repo = Terminals()
            auth = service(cache, repo)
            authenticated = clock[0]
            redirect = await auth._authorization_redirect(repo.clients['web'], '/callback', {
                'userId': 7, 'account': 'alice', 'accountType': 'username',
                'codeChallenge': PKCE_CHALLENGE, 'codeChallengeMethod': 'S256',
            })
            clock[0] += 120
            token = await auth.authorization_code_token({
                'client_id': 'test-web', 'redirect_uri': '/callback',
                'code': parse_qs(urlparse(redirect).query)['code'][0], 'code_verifier': PKCE_VERIFIER,
            })
            assert (await auth.userinfo(token['access_token']))['authTime'] == authenticated
    asyncio.run(exercise())


@pytest.mark.parametrize('operation', ['userinfo', 'activity'])
def test_promoted_super_admin_immediately_uses_shorter_idle_limit(clock, operation):
    async def exercise():
        async with backends() as (cache, _):
            repo = Terminals()
            auth = service(cache, repo)
            token = await login(auth, 'web')
            clock[0] += 3500
            token = await auth.refresh_token(form(token))
            async def promoted(*_):
                return [{'role_id': 1, 'role_code': 'super_admin'}]
            repo.user_roles = promoted
            clock[0] += 101
            with pytest.raises(AuthError, match='超时'):
                if operation == 'activity':
                    await auth.session_status(token['access_token'], activity=True)
                else:
                    await auth.userinfo(token['access_token'])
    asyncio.run(exercise())
