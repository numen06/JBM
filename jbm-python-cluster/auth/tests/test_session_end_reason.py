"""Retain the first actual cause even when the SDK clears an expired token."""
import asyncio

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from jbm_cluster_py.platform.auth.router import build_auth_router
from jbm_cluster_py.platform.auth.service import _hash_token
from test_session_lifecycle import backends, service
from test_session_policy import clock
from test_terminal_sessions import Terminals, login


@pytest.mark.parametrize("reason", ["replaced", "timeout", "signed_out", "kicked", "revoked"])
def test_authoritative_reason_survives_access_removal_and_later_logout(reason, clock):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            token = await login(auth, "web")
            raw = token["access_token"]
            state = await cache.get_json("session_family:" + _hash_token(raw))
            if reason == "replaced":
                await login(auth, "web")
            elif reason == "signed_out":
                await auth.logout(raw)
            elif reason == "kicked":
                await auth.revoke_access_session(_hash_token(raw))
            elif reason == "timeout":
                # Expired access must still give the idle/absolute session cause.
                clock[0] += 7201
            else:
                await auth._revoke_refresh_family(state["familyId"])
            app = FastAPI()
            app.include_router(build_auth_router(auth))
            with TestClient(app) as client:
                for _ in range(2):
                    response = client.get("/oauth2/session/status", headers={"Authorization": "Bearer " + raw})
                    assert response.status_code == 401
                    assert response.json()["session_reason"] == reason
                    assert response.headers["cache-control"] == "no-store"
                    # A client's cleanup cannot overwrite the real cause.
                    await auth.logout(raw)
                invalid = client.get("/oauth2/session/status", headers={"Authorization": "Bearer invalid"})
                assert invalid.status_code == 401
                assert invalid.json()["session_reason"] is None
    asyncio.run(exercise())


def test_access_expiry_alone_is_not_a_login_timeout(clock):
    async def exercise():
        async with backends() as (cache, _):
            auth = service(cache, Terminals())
            token = await login(auth, "web")
            clock[0] += 1801
            app = FastAPI()
            app.include_router(build_auth_router(auth))
            with TestClient(app) as client:
                response = client.get("/oauth2/session/status", headers={"Authorization": "Bearer " + token["access_token"]})
                assert response.status_code == 401
                assert response.json()["session_reason"] is None
            reference = await cache.get_json("session_family:" + _hash_token(token["access_token"]))
            assert not await cache.get_json("refresh_family_revoked:" + reference["familyId"])
    asyncio.run(exercise())
