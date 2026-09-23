"""Session lifecycle regression tests, optionally against an isolated real Redis.

Set JBM_AUTH_TEST_REDIS_URL to opt into two-worker Redis tests. Each test uses a
random prefix and deletes only its own keys; never flushes a shared database.
"""

from __future__ import annotations

import asyncio
import os
import secrets
from contextlib import asynccontextmanager

import pytest
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.auth.service import AuthError, AuthService, TokenCache, _hash_token


class Repository:
    def __init__(self):
        self.client_enabled = True
        self.tenant_enabled = True
        self.user = {
            "user_id": 7,
            "user_name": "alice",
            "company_id": 10,
            "status": 1,
            "user_type": "normal",
        }
        self.account = {"user_id": 7, "account": "alice", "account_type": "username", "status": 1}
        self.client = {"clientId": "test-rp", "clientSecret": "test-secret", "appId": 20}

    async def find_client(self, client_id):
        return (
            dict(self.client)
            if self.client_enabled and client_id == self.client["clientId"]
            else None
        )

    async def find_user(self, user_id):
        return dict(self.user) if user_id == self.user["user_id"] else None

    async def find_account(self, *args):
        return dict(self.account)

    async def tenant_app_enabled(self, *args):
        return self.tenant_enabled

    async def user_roles(self, *args):
        return [{"role_id": 10, "role_code": "reader"}]

    async def user_authorities(self, *args):
        return ["READ"]


def service(cache, repository=None):
    return AuthService(repository or Repository(), cache, {"allow-plaintext-secrets": True})


async def issue(auth):
    repo = auth.repository
    return await auth._issue_user_token(repo.client, repo.account, repo.user, "all")


def refresh_form(token, **extra):
    return {
        "refresh_token": token["refresh_token"],
        "client_id": "test-rp",
        "client_secret": "test-secret",
        **extra,
    }


@asynccontextmanager
async def backends(redis_url=None):
    prefix = "test:jbm:auth:" + secrets.token_hex(12)
    first = TokenCache(
        RedisClient({"enabled": bool(redis_url), "url": redis_url}),
        prefix,
        required=bool(redis_url),
    )
    second = (
        TokenCache(
            RedisClient({"enabled": bool(redis_url), "url": redis_url}),
            prefix,
            required=bool(redis_url),
        )
        if redis_url
        else first
    )
    await first.start()
    if second is not first:
        await second.start()
    try:
        yield first, second
    finally:
        if first.redis_client.client is not None:
            keys = [key async for key in first.redis_client.client.scan_iter(prefix + ":*")]
            if keys:
                await first.redis_client.client.delete(*keys)
        await first.stop()
        if second is not first:
            await second.stop()


def test_required_redis_never_falls_back_to_memory():
    async def exercise():
        cache = TokenCache(RedisClient({"enabled": False}), required=True)
        assert not await cache.readiness()
        with pytest.raises(RuntimeError, match="shared Redis"):
            await cache.start()
        with pytest.raises(AuthError):
            await cache.set_json("session", {"active": True}, 60)
        assert cache._memory == {}

    asyncio.run(exercise())


def test_bad_client_authentication_does_not_consume_or_revoke_refresh():
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache)
            initial = await issue(auth)
            for extra in ({"client_secret": "wrong"}, {"client_id": "other-rp"}):
                with pytest.raises(AuthError):
                    await auth.refresh_token(refresh_form(initial, **extra))
            rotated = await auth.refresh_token(refresh_form(initial))
            with pytest.raises(AuthError):
                await auth.refresh_token(refresh_form(initial, client_secret="wrong"))
            assert (await auth.userinfo(rotated["access_token"]))["userId"] == 7

    asyncio.run(exercise())


@pytest.mark.parametrize(
    "operation", ["logout", "kickout", "logout_old_access", "logout_old_refresh"]
)
def test_ending_session_also_ends_refresh_family(operation):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache)
            initial = await issue(auth)
            current = await auth.refresh_token(refresh_form(initial))
            if operation == "kickout":
                await auth.revoke_access_session(_hash_token(initial["access_token"]))
            elif operation == "logout_old_refresh":
                await auth.logout(refresh_token=initial["refresh_token"])
            else:
                await auth.logout(
                    initial["access_token"]
                    if operation == "logout_old_access"
                    else current["access_token"]
                )
            with pytest.raises(AuthError):
                await auth.userinfo(current["access_token"])
            with pytest.raises(AuthError):
                await auth.refresh_token(refresh_form(current))

    asyncio.run(exercise())


@pytest.mark.parametrize("disable", ["user", "account", "client", "tenant", "tenant_changed"])
def test_current_identity_state_is_checked_online(disable):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache)
            token = await issue(auth)
            repo = auth.repository
            if disable in {"user", "account"}:
                getattr(repo, disable)["status"] = 0
            elif disable == "tenant_changed":
                repo.user["company_id"] = 11
            else:
                setattr(repo, disable + "_enabled", False)
            with pytest.raises(AuthError):
                await auth.userinfo(token["access_token"])
            if disable != "tenant_changed":
                with pytest.raises(AuthError):
                    await auth.refresh_token(refresh_form(token))

    asyncio.run(exercise())


def test_login_limit_uses_user_index_and_ends_evicted_family():
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache)
            auth.max_sessions_per_user = 1

            async def forbid_scan(*args):
                raise AssertionError("login must not scan global sessions")

            cache.list_json = forbid_scan
            first = await issue(auth)
            second = await issue(auth)
            with pytest.raises(AuthError):
                await auth.refresh_token(refresh_form(first))
            assert (await auth.userinfo(second["access_token"]))["userId"] == 7
            second_state = await cache.get_json("refresh:" + _hash_token(second["refresh_token"]))
            assert second_state["familyId"] in await cache.index_members("client_sessions:test-rp")
            # Access expiry must not hide the still-live refresh family from limits.
            await cache.delete("access:" + _hash_token(second["access_token"]))
            await issue(auth)
            with pytest.raises(AuthError):
                await auth.refresh_token(refresh_form(second))

    asyncio.run(exercise())


async def concurrent_replay(redis_url=None):
    async with backends(redis_url) as (cache1, cache2):
        auth1, auth2 = service(cache1), service(cache2)
        auth2.signer = auth1.signer
        first = await issue(auth1)
        # Pause winner AFTER atomic consumption. The other worker must see the
        # replay marker now, not a transient missing token with no family link.
        consumed = asyncio.Event()
        resume = asyncio.Event()
        original = cache1.consume_refresh

        async def paused_consume(*args):
            result = await original(*args)
            if not result[1]:
                consumed.set()
                await resume.wait()
            return result

        cache1.consume_refresh = paused_consume
        winner = asyncio.create_task(auth1.refresh_token(refresh_form(first)))
        await asyncio.wait_for(consumed.wait(), 3)
        with pytest.raises(AuthError):
            await auth2.refresh_token(refresh_form(first))
        resume.set()
        with pytest.raises(AuthError):
            await winner
        with pytest.raises(AuthError):
            await auth2.userinfo(first["access_token"])


def test_replay_during_rotation_cannot_resurrect_family():
    asyncio.run(concurrent_replay())


async def revoke_during_publish(operation, redis_url=None):
    async with backends(redis_url) as (cache1, cache2):
        auth1, auth2 = service(cache1), service(cache2)
        auth2.signer = auth1.signer
        initial = await issue(auth1)
        publishing, resume = asyncio.Event(), asyncio.Event()
        original = cache1.set_json

        async def pause_publish(key, value, ttl):
            if key.startswith("refresh:"):
                publishing.set()
                await resume.wait()
            await original(key, value, ttl)

        cache1.set_json = pause_publish
        rotating = asyncio.create_task(auth1.refresh_token(refresh_form(initial)))
        await asyncio.wait_for(publishing.wait(), 3)
        if operation == "logout":
            await auth2.logout(initial["access_token"])
        elif operation == "revoke":
            await auth2.revoke_token(
                {
                    **refresh_form(initial),
                    "token": initial["refresh_token"],
                    "token_type_hint": "refresh_token",
                }
            )
        else:
            with pytest.raises(AuthError):
                await auth2.refresh_token(refresh_form(initial))
        resume.set()
        with pytest.raises(AuthError):
            await rotating
        assert await cache1.index_members("user_sessions:7:20")
        for family_id in await cache1.index_members("user_sessions:7:20"):
            assert await cache2.get_json("refresh_family_revoked:" + family_id)


@pytest.mark.parametrize("operation", ["logout", "revoke", "replay"])
def test_revocation_wins_even_after_rotation_started_publishing(operation):
    asyncio.run(revoke_during_publish(operation))


def test_legacy_replay_marker_and_revocation_of_rotated_refresh():
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache)
            for operation in ("replay", "revoke"):
                first = await issue(auth)
                rotated = await auth.refresh_token(refresh_form(first))
                marker_key = "refresh_used:" + _hash_token(first["refresh_token"])
                marker = await cache.get_json(marker_key)
                # Upgrade compatibility with the former minimal replay marker.
                await cache.set_json(marker_key, {"familyId": marker["familyId"]}, 300)
                if operation == "replay":
                    with pytest.raises(AuthError):
                        await auth.refresh_token(refresh_form(first))
                else:
                    await auth.revoke_token(
                        {
                            **refresh_form(first),
                            "token": first["refresh_token"],
                            "token_type_hint": "refresh_token",
                        }
                    )
                with pytest.raises(AuthError):
                    await auth.userinfo(rotated["access_token"])
                with pytest.raises(AuthError):
                    await auth.refresh_token(refresh_form(rotated))
            assert len(cache._locks) == 0

    asyncio.run(exercise())


@pytest.mark.parametrize("disable", ["user", "account", "tenant", "family"])
def test_refresh_introspection_obeys_current_validity(disable):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache)
            token = await issue(auth)
            form = {
                **refresh_form(token),
                "token": token["refresh_token"],
                "token_type_hint": "refresh_token",
            }
            assert (await auth.introspect_token(form))["active"]
            if disable in {"user", "account"}:
                getattr(auth.repository, disable)["status"] = 0
            elif disable == "tenant":
                auth.repository.tenant_enabled = False
            else:
                state = await cache.get_json("refresh:" + _hash_token(token["refresh_token"]))
                await cache.set_json(
                    "refresh_family_revoked:" + state["familyId"], {"revoked": True}, 300
                )
            assert await auth.introspect_token(form) == {"active": False}

    asyncio.run(exercise())


@pytest.mark.parametrize("field", ["password", "update_time"])
def test_account_credential_version_change_invalidates_access_and_refresh(field):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache)
            auth.repository.account.update(
                password="old-password-hash", update_time="2026-09-01 10:00:00"
            )
            token = await issue(auth)
            state = await cache.get_json("refresh:" + _hash_token(token["refresh_token"]))
            assert state["accountVersion"]
            assert "old-password-hash" not in str(state)
            auth.repository.account[field] = "changed"
            with pytest.raises(AuthError, match="凭证已变更"):
                await auth.userinfo(token["access_token"])
            assert await auth.introspect_token(
                {
                    **refresh_form(token),
                    "token": token["refresh_token"],
                    "token_type_hint": "refresh_token",
                }
            ) == {"active": False}
            with pytest.raises(AuthError, match="凭证已变更"):
                await auth.refresh_token(refresh_form(token))

    asyncio.run(exercise())


def test_credential_change_during_rotation_cannot_recapture_new_version():
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache)
            auth.repository.account["password"] = "old-hash"
            token = await issue(auth)
            original = auth.repository.find_account

            async def change_after_check(*args):
                result = await original(*args)
                auth.repository.account["password"] = "new-hash"
                return result

            auth.repository.find_account = change_after_check
            with pytest.raises(AuthError, match="凭证已变更"):
                await auth.refresh_token(refresh_form(token))

    asyncio.run(exercise())


def test_role_revocation_removes_stale_root_and_updates_userinfo_roles():
    class MutableRolesRepository(Repository):
        roles = [{"role_id": 1, "role_code": "superuser"}]

        async def user_roles(self, *args):
            return list(self.roles)

        async def user_authorities(self, user_id, root, *args):
            return ["ALL"] if root else ["READ"]

    async def exercise():
        async with backends() as (cache, _):
            repo = MutableRolesRepository()
            auth = service(cache, repo)
            token = await issue(auth)
            assert auth.signer.verify(token["access_token"])["root"]
            await auth.verify_permissions(token["access_token"], "ALL")
            repo.roles = [{"role_id": 10, "role_code": "reader"}]
            identity = await auth.userinfo(token["access_token"])
            assert identity["roles"] == ["reader"]
            assert identity["permissions"] == ["READ"]
            with pytest.raises(AuthError, match="无权限"):
                await auth.verify_permissions(token["access_token"], "ALL")
            rotated = await auth.refresh_token(refresh_form(token))
            assert not auth.signer.verify(rotated["access_token"])["root"]

    asyncio.run(exercise())


def test_real_redis_two_workers_replay_and_restart():
    url = os.environ.get("JBM_AUTH_TEST_REDIS_URL")
    if not url:
        pytest.skip("set JBM_AUTH_TEST_REDIS_URL for a real Redis integration test")

    async def exercise():
        await concurrent_replay(url)
        for operation in ("logout", "revoke", "replay"):
            await revoke_during_publish(operation, url)
        async with backends(url) as (cache1, cache2):
            auth1, auth2 = service(cache1), service(cache2)
            auth2.signer = auth1.signer
            token = await issue(auth1)
            assert (await auth2.userinfo(token["access_token"]))["userId"] == 7
            await cache1.stop()
            await cache1.start()
            rotated = await auth1.refresh_token(refresh_form(token))
            assert (await auth2.userinfo(rotated["access_token"]))["userId"] == 7
            await auth2.revoke_access_session(_hash_token(rotated["access_token"]))
            with pytest.raises(AuthError):
                await auth1.refresh_token(refresh_form(rotated))
            assert await cache1.readiness()
            # Same cache API lock serializes operations on separate connections.
            entered = asyncio.Event()
            async with cache1.lock("probe"):

                async def other():
                    async with cache2.lock("probe"):
                        entered.set()

                pending = asyncio.create_task(other())
                await asyncio.sleep(0.1)
                assert not entered.is_set()
            await asyncio.wait_for(pending, 3)
            # Simulate a process paused beyond its lease. A new owner writes;
            # the former owner must not overwrite it even before renewal runs.
            with pytest.raises(AuthError, match="锁已失效"):
                async with cache1.lock("fenced"):
                    await cache1.redis_client.client.pexpire(cache1._key("lock:fenced"), 1)
                    await asyncio.sleep(0.02)
                    async with cache2.lock("fenced"):
                        await cache2.set_json("fenced-result", {"owner": "new"}, 60)
                        with pytest.raises(AuthError, match="锁已失效"):
                            await cache1.set_json("fenced-result", {"owner": "stale"}, 60)
            assert await cache2.get_json("fenced-result") == {"owner": "new"}

    asyncio.run(exercise())
