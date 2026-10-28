"""Cross-terminal isolation, mobile lifetime, and concurrent eviction regressions."""

import asyncio
import os
import time
from types import SimpleNamespace

import pytest
from jbm_cluster_py.platform.auth import service as auth_module
from jbm_cluster_py.platform.auth.service import AuthError, _hash_token
from test_session_lifecycle import Repository, backends, service


class Terminals(Repository):
    def __init__(self):
        super().__init__()
        self.clients = {
            terminal: {
                **self.client,
                "clientId": "test-" + terminal,
                "terminalType": terminal,
                "publicClient": True,
            }
            for terminal in ("web", "miniapp", "app")
        }

    async def find_client(self, client_id):
        return (
            next((dict(c) for c in self.clients.values() if c["clientId"] == client_id), None)
            if self.client_enabled
            else None
        )


async def login(auth, terminal):
    repo = auth.repository
    return await auth._issue_user_token(repo.clients[terminal], repo.account, repo.user, "all")


def form(token):
    return {"client_id": "test-" + token["terminal_type"], "refresh_token": token["refresh_token"]}


async def terminal_lifecycle(redis_url=None):
    async with backends(redis_url) as (cache1, cache2):
        auth, other = service(cache1, Terminals()), service(cache2, Terminals())
        other.signer = auth.signer
        tokens = {t: await login(auth, t) for t in ("web", "miniapp", "app")}
        for terminal, token in tokens.items():
            assert (await other.userinfo(token["access_token"]))["userId"] == 7
            assert auth.signer.verify(token["access_token"])["terminal_type"] == terminal
            assert token["expires_in"] == 1800
            assert token["refresh_expires_in"] == (604800 if terminal == "web" else 2592000)

        # Only the same terminal is evicted; its refresh cannot revive it.
        old = tokens["miniapp"]
        tokens["miniapp"] = await login(other, "miniapp")
        with pytest.raises(AuthError):
            await auth.userinfo(old["access_token"])
        with pytest.raises(AuthError):
            await auth.refresh_token(form(old))
        for token in tokens.values():
            assert (await auth.userinfo(token["access_token"]))["userId"] == 7

        # Client identity cannot be switched by a request parameter.
        with pytest.raises(AuthError):
            await auth.refresh_token(
                {**form(tokens["app"]), "client_id": "test-web", "terminalType": "app"}
            )
        current = await other.refresh_token(form(tokens["app"]))
        await auth.logout(tokens["app"]["access_token"])
        with pytest.raises(AuthError):
            await other.refresh_token(form(current))
        for terminal in ("web", "miniapp"):
            assert (await other.userinfo(tokens[terminal]["access_token"]))["userId"] == 7

        # Simultaneous new logins still leave exactly one live family.
        results = await asyncio.gather(
            *(login(auth if i % 2 else other, "web") for i in range(8)), return_exceptions=True
        )
        live = 0
        for result in results:
            if isinstance(result, AuthError):
                continue
            try:
                await auth.userinfo(result["access_token"])
                live += 1
            except AuthError:
                with pytest.raises(AuthError):
                    await other.refresh_token(form(result))
        assert live == 1

        # Pause old rotation before registration; a new login wins permanently.
        first = await login(auth, "miniapp")
        publishing, resume = asyncio.Event(), asyncio.Event()
        original = auth._record_online_session

        async def paused(*args, **kwargs):
            publishing.set()
            await resume.wait()
            await original(*args, **kwargs)

        auth._record_online_session = paused
        rotating = asyncio.create_task(auth.refresh_token(form(first)))
        await asyncio.wait_for(publishing.wait(), 3)
        winner = await login(other, "miniapp")
        resume.set()
        with pytest.raises(AuthError):
            await rotating
        assert (await other.userinfo(winner["access_token"]))["userId"] == 7
        renewed = await other.refresh_token(form(winner))
        assert renewed["refresh_token"] != winner["refresh_token"]


def test_terminal_lifecycle_in_memory():
    asyncio.run(terminal_lifecycle())


@pytest.mark.parametrize("terminal", ["web", "miniapp", "app"])
def test_same_terminal_replaces_even_a_session_with_expired_access(terminal):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            first = await login(auth, terminal)
            # The access token can expire while its refresh family stays live.
            await cache.delete("access:" + _hash_token(first["access_token"]))
            second = await login(auth, terminal)
            with pytest.raises(AuthError):
                await auth.refresh_token(form(first))
            renewed = await auth.refresh_token(form(second))
            assert (await auth.userinfo(renewed["access_token"]))["userId"] == 7
            assert renewed["terminal_type"] == terminal

    asyncio.run(exercise())


def test_terminal_lifecycle_across_real_redis_workers():
    url = os.environ.get("JBM_AUTH_TEST_REDIS_URL")
    if not url:
        pytest.skip("set JBM_AUTH_TEST_REDIS_URL")
    asyncio.run(terminal_lifecycle(url))


def test_mobile_refresh_rolls_for_active_use_and_expires_after_inactivity(monkeypatch):
    now = [int(time.time())]
    monkeypatch.setattr(auth_module, "time", SimpleNamespace(time=lambda: now[0]))

    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            token = await login(auth, "app")
            state = await cache.get_json("refresh:" + _hash_token(token["refresh_token"]))
            family = state["familyId"]
            for _ in range(3):
                now[0] += 29 * 86400
                token = await auth.refresh_token(form(token))
                state = await cache.get_json("refresh:" + _hash_token(token["refresh_token"]))
                assert state["familyId"] == family
                assert state["refreshSeconds"] == 30 * 86400
                assert token["refresh_expires_in"] == 30 * 86400
            now[0] += 30 * 86400 + 1
            with pytest.raises(AuthError):
                await auth.refresh_token(form(token))

    asyncio.run(exercise())


def test_mobile_replay_after_a_week_still_revokes_the_current_family(monkeypatch):
    now = [int(time.time())]
    monkeypatch.setattr(auth_module, "time", SimpleNamespace(time=lambda: now[0]))

    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            original = await login(auth, "miniapp")
            rotated = await auth.refresh_token(form(original))
            now[0] += 8 * 86400
            with pytest.raises(AuthError, match="已被使用"):
                await auth.refresh_token(form(original))
            with pytest.raises(AuthError):
                await auth.refresh_token(form(rotated))

    asyncio.run(exercise())
