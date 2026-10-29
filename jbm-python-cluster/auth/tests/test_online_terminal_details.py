"""Admin session list reflects refresh authorization, not just access-token TTL."""

import asyncio
import time
from datetime import datetime
from types import SimpleNamespace

from jbm_cluster_py.platform.auth import service as auth_module
from jbm_cluster_py.platform.auth.service import _hash_token
from test_session_lifecycle import backends, service
from test_terminal_sessions import Terminals, form, login


def epoch(value):
    return int(datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp())


def test_admin_sees_all_terminals_and_real_rolling_login_deadlines(monkeypatch):
    now = [int(time.time())]
    monkeypatch.setattr(auth_module, "time", SimpleNamespace(time=lambda: now[0]))

    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            tokens = {t: await login(auth, t) for t in ("web", "miniapp", "app")}
            rows = (await auth.online_users({}, tokens["web"]["access_token"]))["contents"]
            assert len(rows) == 3
            initial = {row["terminalType"]: row for row in rows}
            for terminal, row in initial.items():
                assert epoch(row["accessTokenExpiresAt"]) == now[0] + 1800
                lifetime = 7200 if terminal == "web" else 15 * 86400
                assert epoch(row["reauthRequiredAt"]) == now[0] + lifetime
                assert row["lastRefreshTime"] is None
                assert row["sessionStatus"] == "active"
                assert row["currentSession"] == (terminal == "web")
                assert (
                    not {
                        "familyId",
                        "jti",
                        "accountVersion",
                        "refreshExpiresAt",
                        "refreshHash",
                        "refreshToken",
                    }
                    & row.keys()
                )
            now[0] += 2000
            # All access tokens expired; mobile login authorizations remain visible.
            rows = (await auth.online_users({}))["contents"]
            assert len(rows) == 3
            assert {row["sessionStatus"] for row in rows} == {"refreshable"}
            rotated = await auth.refresh_token(form(tokens["miniapp"]))
            rows = (await auth.online_users({}, rotated["access_token"]))["contents"]
            mini = next(row for row in rows if row["terminalType"] == "miniapp")
            assert mini["loginTime"] == initial["miniapp"]["loginTime"]
            assert epoch(mini["lastRefreshTime"]) == now[0]
            assert epoch(mini["reauthRequiredAt"]) == now[0] + 15 * 86400
            assert mini["currentSession"] is True
            assert mini["sessionStatus"] == "active"
            await auth.expire_access_session(_hash_token(rotated["access_token"]), 10)
            mini = next(
                row
                for row in (await auth.online_users({}))["contents"]
                if row["terminalType"] == "miniapp"
            )
            assert epoch(mini["accessTokenExpiresAt"]) == now[0] + 10
            assert epoch(mini["reauthRequiredAt"]) == now[0] + 15 * 86400
            await auth.logout(rotated["access_token"])
            assert {row["terminalType"] for row in (await auth.online_users({}))["contents"]} == {
                "web",
                "app",
            }
            now[0] += 15 * 86400 + 1
            assert (await auth.online_users({}))["total"] == 0

    asyncio.run(exercise())


def test_legacy_session_deadline_uses_remaining_ttl_without_extending_it(monkeypatch):
    now = [int(time.time())]
    monkeypatch.setattr(auth_module, "time", SimpleNamespace(time=lambda: now[0]))

    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            token = await login(auth, "app")
            state = await cache.get_json("refresh:" + _hash_token(token["refresh_token"]))
            key = "family_session:" + state["familyId"]
            old = await cache.get_json(key)
            old.pop("refreshExpiresAt")
            old.pop("lastRefreshTime")
            await cache.set_json(key, old, 15 * 86400)
            now[0] += 5000
            deadline = epoch((await auth.online_users({}))["contents"][0]["reauthRequiredAt"])
            now[0] += 5000
            assert (
                epoch((await auth.online_users({}))["contents"][0]["reauthRequiredAt"]) == deadline
            )

    asyncio.run(exercise())
