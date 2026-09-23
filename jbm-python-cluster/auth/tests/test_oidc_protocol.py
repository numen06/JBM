from __future__ import annotations

import asyncio
import base64
import hashlib
import json
import os
import re
import secrets
import time
from urllib.parse import parse_qs, quote_plus, urlsplit

import httpx
import jwt
import pytest
from authlib.integrations.httpx_client import AsyncOAuth2Client
from authlib.oidc.core import CodeIDToken
from fastapi import FastAPI
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.auth.oidc import digest
from jbm_cluster_py.platform.auth.oidc_router import SSO_COOKIE, build_oidc_router
from jbm_cluster_py.platform.auth.repository import AuthRepository
from jbm_cluster_py.platform.auth.service import AuthError, AuthService, TokenCache
from joserfc.errors import InvalidClaimError
from sqlalchemy import text

ISSUER = "https://idp.example/oidc"
VERIFIER = "v" * 64
CHALLENGE = (
    base64.urlsafe_b64encode(hashlib.sha256(VERIFIER.encode()).digest()).decode().rstrip("=")
)


@pytest.fixture
async def oidc(tmp_path, request):
    repo = AuthRepository({"url": f"sqlite+aiosqlite:///{tmp_path / 'oidc.db'}"})
    await repo.start()
    async with repo.engine.begin() as conn:
        for i, name in enumerate(("rp-a", "rp-b", "secret-rp"), 1):
            settings = {
                "enabled": True,
                "redirectUris": [f"https://{name}.example/callback"],
                "postLogoutRedirectUris": [f"https://{name}.example/logout"],
                "scopes": ["openid", "profile", "email", "offline_access"],
                "audiences": [f"https://{name}.example/api"],
                "tokenEndpointAuthMethod": "client_secret_basic" if name == "secret-rp" else "none",
            }
            await conn.execute(
                text(
                    "INSERT INTO base_app (app_id,api_key,secret_key,status,extend_data) "
                    "VALUES (:id,:key,'client-password',1,:data)"
                ),
                {"id": i, "key": name, "data": json.dumps({"oidc": settings})},
            )
            await conn.execute(
                text("INSERT INTO base_tenant_app (tenant_id,app_id,status) VALUES (10,:app,1)"),
                {"app": i},
            )
            await conn.execute(
                text(
                    "INSERT INTO base_role_user (user_id,role_id,app_id,tenant_id) "
                    "VALUES (7,5,:app,10)"
                ),
                {"app": i},
            )
        await conn.execute(
            text(
                "INSERT INTO base_user (user_id,user_name,real_name,email,company_id,status) "
                "VALUES (7,'alice','Alice','alice@example.com',10,1)"
            )
        )
        await conn.execute(
            text(
                "INSERT INTO base_account (account_id,user_id,account,password,account_type,"
                "status,domain,must_change_password) "
                "VALUES (7,7,'alice','password','username',1,'@admin.com',0)"
            )
        )
        await conn.execute(text("INSERT INTO base_org (id,status) VALUES (10,1)"))
        await conn.execute(text("INSERT INTO base_user_org (user_id,org_id) VALUES (7,10)"))
        await conn.execute(
            text("INSERT INTO base_role (role_id,role_code,status) VALUES (5,'member',1)")
        )
    cache = TokenCache(RedisClient({"enabled": False}))
    config = {
        "allow-plaintext-secrets": True,
        "oidc": {"enabled": True, "issuer": ISSUER, **getattr(request, "param", {})},
    }
    auth = AuthService(repo, cache, config)
    router = build_oidc_router(auth, config)
    app = FastAPI()
    app.include_router(router)
    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app),
        base_url="https://idp.example",
        follow_redirects=False,
    ) as browser:
        yield browser, router.oidc_service, repo, app
    await repo.stop()


def params(client="rp-a", **overrides):
    return {
        "client_id": client,
        "redirect_uri": f"https://{client}.example/callback",
        "response_type": "code",
        "scope": "openid profile email offline_access",
        "state": "state-value",
        "nonce": "nonce-value",
        "code_challenge": CHALLENGE,
        "code_challenge_method": "S256",
        **overrides,
    }


def fields(response):
    return dict(re.findall(r'name="(transaction|csrf)" value="([^"]+)"', response.text))


async def authorize(browser, client="rp-a", **overrides):
    response = await browser.get("/oidc/authorize", params=params(client, **overrides))
    if 'name="username"' in response.text:
        response = await browser.post(
            "/oidc/continue",
            data={
                **fields(response),
                "decision": "approve",
                "username": "alice",
                "password": "password",
            },
        )
    if response.status_code == 200:
        response = await browser.post(
            "/oidc/continue", data={**fields(response), "decision": "approve"}
        )
    assert response.status_code == 303, response.text
    return parse_qs(urlsplit(response.headers["location"]).query)["code"][0]


async def exchange(browser, code, client="rp-a", **overrides):
    return await browser.post(
        "/oidc/token",
        data={
            "grant_type": "authorization_code",
            "client_id": client,
            "code": code,
            "redirect_uri": f"https://{client}.example/callback",
            "code_verifier": VERIFIER,
            **overrides,
        },
    )


async def test_two_independent_authlib_rps_discover_login_and_reuse_sso(oidc):
    browser, service, _, app = oidc
    discovery = (await browser.get("/oidc/.well-known/openid-configuration")).json()
    jwk = (await browser.get(discovery["jwks_uri"])).json()["keys"][0]
    subjects = []
    for client_id in ("rp-a", "rp-b"):
        async with AsyncOAuth2Client(
            client_id,
            redirect_uri=f"https://{client_id}.example/callback",
            scope="openid profile email",
            token_endpoint_auth_method="none",
            code_challenge_method="S256",
            transport=httpx.ASGITransport(app=app),
        ) as rp:
            url, state = rp.create_authorization_url(
                discovery["authorization_endpoint"], code_verifier=VERIFIER, nonce=client_id
            )
            response = await browser.get(url)
            if client_id == "rp-a":
                assert 'name="username"' in response.text
                response = await browser.post(
                    "/oidc/continue",
                    data={
                        **fields(response),
                        "decision": "approve",
                        "username": "alice",
                        "password": "password",
                    },
                )
                session = browser.cookies.get(SSO_COOKIE)
            else:
                assert 'name="username"' not in response.text
                assert browser.cookies.get(SSO_COOKIE) == session
            assert "允许应用访问" in response.text
            response = await browser.post(
                "/oidc/continue", data={**fields(response), "decision": "approve"}
            )
            callback = response.headers["location"]
            assert parse_qs(urlsplit(callback).query)["state"] == [state]
            tokens = await rp.fetch_token(
                discovery["token_endpoint"], authorization_response=callback, code_verifier=VERIFIER
            )
            claims = jwt.decode(
                tokens["id_token"],
                jwt.PyJWK.from_dict(jwk).key,
                algorithms=["RS256"],
                audience=client_id,
                issuer=ISSUER,
            )
            assert claims["nonce"] == client_id
            CodeIDToken(claims, {}, params={"nonce": client_id}).validate_nonce()
            with pytest.raises(InvalidClaimError):
                CodeIDToken(claims, {}, params={"nonce": "wrong-nonce"}).validate_nonce()
            assert claims["auth_time"] <= claims["iat"]
            assert "roles" not in claims and "email" not in claims
            with pytest.raises(jwt.InvalidAudienceError):
                jwt.decode(
                    tokens["id_token"],
                    jwt.PyJWK.from_dict(jwk).key,
                    algorithms=["RS256"],
                    audience="wrong-rp",
                    issuer=ISSUER,
                )
            info = (await rp.get(discovery["userinfo_endpoint"])).json()
            assert info == {
                "sub": claims["sub"],
                "name": "Alice",
                "preferred_username": "alice",
                "email": "alice@example.com",
            }
            assert "refresh_token" not in tokens
            subjects.append(claims["sub"])
    assert subjects[0] == subjects[1] and subjects[0] != "7"
    assert len(subjects[0]) == 43
    assert service.metadata()["grant_types_supported"] == ["authorization_code", "refresh_token"]


async def test_prompt_none_pkce_replay_and_token_type(oidc):
    browser, _, _, _ = oidc
    response = await browser.get("/oidc/authorize", params=params(prompt="none"))
    assert "error=login_required" in response.headers["location"]
    code = await authorize(browser)
    bad = await exchange(browser, code, code_verifier="wrong" * 12)
    assert bad.json()["error"] == "invalid_grant"
    response = await exchange(browser, code)
    assert response.status_code == 200, response.text
    tokens = response.json()
    info = await browser.get(
        "/oidc/userinfo", headers={"Authorization": "Bearer " + tokens["id_token"]}
    )
    assert info.status_code == 401
    assert (await exchange(browser, code)).json()["error"] == "invalid_grant"
    assert (
        await browser.get(
            "/oidc/userinfo", headers={"Authorization": "Bearer " + tokens["access_token"]}
        )
    ).status_code == 401
    assert (
        await browser.post(
            "/oidc/token",
            data={
                "grant_type": "refresh_token",
                "client_id": "rp-a",
                "refresh_token": tokens["refresh_token"],
            },
        )
    ).json()["error"] == "invalid_grant"


async def test_refresh_rotation_wrong_client_and_concurrent_replay(oidc):
    browser, _, _, _ = oidc
    token = (await exchange(browser, await authorize(browser))).json()
    bad = await browser.post(
        "/oidc/token",
        data={
            "grant_type": "refresh_token",
            "client_id": "rp-b",
            "refresh_token": token["refresh_token"],
        },
    )
    assert bad.json()["error"] == "invalid_grant"
    form = {
        "grant_type": "refresh_token",
        "client_id": "rp-a",
        "refresh_token": token["refresh_token"],
    }
    results = await asyncio.gather(
        browser.post("/oidc/token", data=form), browser.post("/oidc/token", data=form)
    )
    assert sorted(item.status_code for item in results) == [200, 400]
    rotated = next(item.json() for item in results if item.status_code == 200)
    assert (
        await browser.get(
            "/oidc/userinfo", headers={"Authorization": "Bearer " + rotated["access_token"]}
        )
    ).status_code == 401


@pytest.mark.parametrize(
    "changes,error",
    [
        ({"scope": "openid admin"}, "invalid_scope"),
        ({"prompt": "none login"}, "invalid_request"),
        ({"max_age": "-1"}, "invalid_request"),
        ({"response_type": "token"}, "unsupported_response_type"),
        ({"request_uri": "https://evil.example"}, "invalid_request"),
    ],
)
async def test_unsupported_authorization_profile_rejected(oidc, changes, error):
    browser, _, _, _ = oidc
    response = await browser.get("/oidc/authorize", params=params(**changes))
    assert f"error={error}" in response.headers["location"]


async def test_redirect_duplicates_and_browser_csrf(oidc):
    browser, _, _, app = oidc
    response = await browser.get(
        "/oidc/authorize", params=params(redirect_uri="https://evil.example")
    )
    assert response.status_code == 400 and "location" not in response.headers
    response = await browser.get(
        "/oidc/authorize", params=[*params().items(), ("client_id", "rp-b")]
    )
    assert response.status_code == 400
    page = await browser.get("/oidc/authorize", params=params())
    form = {**fields(page), "decision": "approve", "username": "alice", "password": "password"}
    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app), base_url="https://idp.example"
    ) as attacker:
        assert (await attacker.post("/oidc/continue", data=form)).status_code == 400
    assert (
        await browser.post("/oidc/continue", data=form, headers={"Origin": "https://evil.example"})
    ).status_code == 400
    response = await browser.post("/oidc/continue", data=form)
    assert response.status_code == 200
    assert (await browser.post("/oidc/continue", data=form)).status_code == 400
    for value in page.headers.get_list("set-cookie"):
        assert (
            "Secure" in value
            and "HttpOnly" in value
            and "SameSite=lax" in value
            and "Domain=" not in value
        )


@pytest.mark.parametrize(
    "sql",
    [
        "UPDATE base_account SET status=0",
        "UPDATE base_account SET password='changed'",
        "UPDATE base_user SET status=0",
        "UPDATE base_app SET status=0",
        "DELETE FROM base_user_org",
        "UPDATE base_tenant_app SET status=0",
        "DELETE FROM base_role_user",
    ],
)
async def test_live_changes_invalidate_access_and_refresh(oidc, sql):
    browser, _, repo, _ = oidc
    token = (await exchange(browser, await authorize(browser))).json()
    async with repo.engine.begin() as conn:
        await conn.execute(text(sql))
    assert (
        await browser.get(
            "/oidc/userinfo", headers={"Authorization": "Bearer " + token["access_token"]}
        )
    ).status_code in {400, 401}
    assert (
        await browser.post(
            "/oidc/token",
            data={
                "grant_type": "refresh_token",
                "client_id": "rp-a",
                "refresh_token": token["refresh_token"],
            },
        )
    ).status_code in {400, 401}


async def test_logout_confirmation_revokes_all_sso_grants_and_refresh(oidc):
    browser, _, _, _ = oidc
    first = (await exchange(browser, await authorize(browser))).json()
    second = (await exchange(browser, await authorize(browser, "rp-b"), "rp-b")).json()
    response = await browser.get(
        "/oidc/logout",
        params={
            "id_token_hint": first["id_token"],
            "post_logout_redirect_uri": "https://evil.example",
        },
    )
    assert response.status_code == 400
    response = await browser.get(
        "/oidc/logout",
        params={
            "id_token_hint": first["id_token"],
            "post_logout_redirect_uri": "https://rp-a.example/logout",
            "state": "bye",
        },
    )
    assert response.status_code == 200
    # Merely following a cross-site logout link cannot mutate the cookie/session.
    assert (
        await browser.get(
            "/oidc/userinfo", headers={"Authorization": "Bearer " + second["access_token"]}
        )
    ).status_code == 200
    response = await browser.post("/oidc/logout", data={**fields(response), "decision": "approve"})
    assert response.headers["location"] == "https://rp-a.example/logout?state=bye"
    assert not browser.cookies.get(SSO_COOKIE)
    for client, token in (("rp-a", first), ("rp-b", second)):
        assert (
            await browser.get(
                "/oidc/userinfo", headers={"Authorization": "Bearer " + token["access_token"]}
            )
        ).status_code == 401
        assert (
            await browser.post(
                "/oidc/token",
                data={
                    "grant_type": "refresh_token",
                    "client_id": client,
                    "refresh_token": token["refresh_token"],
                },
            )
        ).status_code == 400


async def test_openid_only_discloses_no_personal_claims_and_prompt_behaviors(oidc):
    browser, service, _, _ = oidc
    token = (await exchange(browser, await authorize(browser, scope="openid"))).json()
    response = await browser.get(
        "/oidc/userinfo", headers={"Authorization": "Bearer " + token["access_token"]}
    )
    assert set(response.json()) == {"sub"}
    assert "refresh_token" not in token
    response = await browser.get("/oidc/authorize", params=params(scope="openid", prompt="none"))
    assert "code=" in response.headers["location"]
    response = await browser.get("/oidc/authorize", params=params(prompt="none"))
    assert "consent_required" in response.headers["location"]
    for update in ({"prompt": "login"}, {"max_age": "0"}):
        response = await browser.get("/oidc/authorize", params=params(**update))
        assert 'name="username"' in response.text
    response = await browser.get("/oidc/authorize", params=params(scope="openid", prompt="consent"))
    assert "允许应用访问" in response.text
    sid = browser.cookies.get(SSO_COOKIE)
    session = await service.session(sid)
    session["auth_time"] -= 300
    await service.cache.set_json("oidc:sso:" + digest(sid), session, 600)
    response = await browser.get(
        "/oidc/authorize", params=params(scope="openid", max_age="60", prompt="none")
    )
    assert "login_required" in response.headers["location"]


async def test_authorize_post_and_policy_changes_reject_code_exchange(oidc):
    browser, _, repo, _ = oidc
    response = await browser.post("/oidc/authorize", data=params())
    assert 'name="username"' in response.text
    assert "form-action 'self' https://rp-a.example;" in response.headers["content-security-policy"]
    assert response.headers["referrer-policy"] == "same-origin"
    assert (await browser.post("/oidc/authorize?client_id=rp-b", data=params())).status_code == 400
    code = await authorize(browser)
    registration = (await repo.find_client("rp-a"))["oidc"]
    registration["grantTypes"] = ["refresh_token"]
    async with repo.engine.begin() as conn:
        await conn.execute(
            text("UPDATE base_app SET extend_data=:data WHERE api_key='rp-a'"),
            {"data": json.dumps({"oidc": registration})},
        )
    assert (await exchange(browser, code)).status_code in {400, 401}


@pytest.mark.parametrize("oidc", [{"refresh-token-seconds": 60}], indirect=True)
async def test_revocation_marker_outlives_short_refresh_tokens(oidc, monkeypatch):
    browser, _, _, _ = oidc
    tokens = (await exchange(browser, await authorize(browser))).json()
    response = await browser.post(
        "/oidc/revoke", data={"client_id": "rp-a", "token": tokens["refresh_token"]}
    )
    assert response.status_code == 200
    before = time.time()
    monkeypatch.setattr("time.time", lambda: before + 61)
    response = await browser.get(
        "/oidc/userinfo", headers={"Authorization": "Bearer " + tokens["access_token"]}
    )
    assert response.status_code == 401


async def test_expired_authorization_code_and_backend_unavailable(oidc, monkeypatch):
    browser, service, _, _ = oidc
    code = await authorize(browser)
    before = time.time()
    with monkeypatch.context() as changed:
        changed.setattr("time.time", lambda: before + 121)
        assert (await exchange(browser, code)).json()["error"] == "invalid_grant"

    async def unavailable(_):
        raise AuthError("internal Redis connection address", 503)

    monkeypatch.setattr(service.cache, "get_json", unavailable)
    response = await browser.get("/oidc/authorize", params=params())
    assert response.status_code == 503
    assert response.json() == {
        "error": "temporarily_unavailable",
        "error_description": "temporarily_unavailable",
    }


async def test_captcha_required_and_no_login_mode_override(oidc, monkeypatch):
    browser, service, _, _ = oidc
    service.auth.login_captcha_required = True

    async def challenge():
        await service.cache.set_json("captcha:system:abc12", {"code": "ABC12"}, 60)
        return "data:image/png;base64,aW1hZ2U="

    monkeypatch.setattr(service.auth, "captcha_base64", challenge)
    response = await browser.get("/oidc/authorize", params=params())
    assert 'name="vcode"' in response.text
    form = {
        **fields(response),
        "decision": "approve",
        "username": "alice",
        "password": "password",
        "loginType": "SMS",
    }
    assert (await browser.post("/oidc/continue", data=form)).status_code == 400
    response = await browser.post("/oidc/continue", data={**form, "vcode": "ABC12"})
    assert response.status_code == 200 and "允许应用访问" in response.text


async def test_reauthentication_rotates_cookie_and_rejects_previous_logout_hint(oidc):
    browser, _, _, _ = oidc
    first = (await exchange(browser, await authorize(browser))).json()
    old_cookie = browser.cookies.get(SSO_COOKIE)
    await authorize(browser, prompt="login")
    assert browser.cookies.get(SSO_COOKIE) != old_cookie
    response = await browser.get("/oidc/logout", params={"id_token_hint": first["id_token"]})
    assert response.status_code == 400
    assert (
        await browser.get(
            "/oidc/userinfo", headers={"Authorization": "Bearer " + first["access_token"]}
        )
    ).status_code == 401


async def test_refresh_introspection_and_revocation(oidc):
    browser, _, _, _ = oidc
    tokens = (await exchange(browser, await authorize(browser))).json()
    form = {"client_id": "rp-a", "token": tokens["refresh_token"]}
    assert (await browser.post("/oidc/introspect", data=form)).json()["active"] is True
    await browser.post("/oidc/revoke", data=form)
    assert (await browser.post("/oidc/introspect", data=form)).json() == {"active": False}


async def test_refresh_and_logout_race_cannot_restore_session(oidc):
    browser, _, _, _ = oidc
    tokens = (await exchange(browser, await authorize(browser))).json()
    response = await browser.get("/oidc/logout", params={"id_token_hint": tokens["id_token"]})
    refresh, logout = await asyncio.gather(
        browser.post(
            "/oidc/token",
            data={
                "grant_type": "refresh_token",
                "client_id": "rp-a",
                "refresh_token": tokens["refresh_token"],
            },
        ),
        browser.post("/oidc/logout", data={**fields(response), "decision": "approve"}),
    )
    assert logout.status_code == 200
    assert refresh.status_code in {200, 400}
    if refresh.status_code == 200:
        access = refresh.json()["access_token"]
        assert (
            await browser.get("/oidc/userinfo", headers={"Authorization": "Bearer " + access})
        ).status_code == 401


async def test_basic_form_encoding_and_client_secret_post(oidc):
    browser, _, repo, _ = oidc
    password = "spaces and+plus"
    async with repo.engine.begin() as conn:
        await conn.execute(
            text("UPDATE base_app SET secret_key=:secret WHERE api_key='secret-rp'"),
            {"secret": password},
        )
    code = await authorize(browser, "secret-rp")
    credentials = base64.b64encode(("secret-rp:" + quote_plus(password)).encode()).decode()
    response = await browser.post(
        "/oidc/token",
        headers={"Authorization": "basic " + credentials},
        data={
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": "https://secret-rp.example/callback",
            "code_verifier": VERIFIER,
        },
    )
    assert response.status_code == 200
    registration = (await repo.find_client("secret-rp"))["oidc"]
    registration["tokenEndpointAuthMethod"] = "client_secret_post"
    async with repo.engine.begin() as conn:
        await conn.execute(
            text("UPDATE base_app SET extend_data=:data WHERE api_key='secret-rp'"),
            {"data": json.dumps({"oidc": registration})},
        )
    code = await authorize(browser, "secret-rp")
    response = await exchange(browser, code, "secret-rp", client_secret=password)
    assert response.status_code == 200


@pytest.mark.parametrize("oidc", [{"enabled": " on "}], indirect=True)
async def test_enabled_flag_matches_app_configuration(oidc):
    browser, _, _, _ = oidc
    assert (await browser.get("/oidc/.well-known/openid-configuration")).status_code == 200


async def test_oidc_two_redis_instances_share_state_and_fence_refresh_replay(oidc):
    url = os.environ.get("JBM_AUTH_TEST_REDIS_URL")
    if not url:
        pytest.skip("Set JBM_AUTH_TEST_REDIS_URL for a real Redis integration test")
    browser, service, repo, _ = oidc
    prefix = "oidc-test:" + secrets.token_hex(12)
    first = TokenCache(RedisClient({"enabled": True, "url": url}), prefix=prefix, required=True)
    second = TokenCache(RedisClient({"enabled": True, "url": url}), prefix=prefix, required=True)
    await first.start()
    await second.start()
    service.cache = service.auth.cache = first
    auth = AuthService(repo, second, service.auth.config)
    auth.signer = service.auth.signer
    app = FastAPI()
    app.include_router(build_oidc_router(auth, service.auth.config))
    try:
        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app), base_url="https://idp.example"
        ) as other:
            code = await authorize(browser)
            other.cookies.update(browser.cookies)
            token = (await exchange(other, code)).json()
            await second.stop()
            await second.start()
            assert (
                await other.get(
                    "/oidc/userinfo", headers={"Authorization": "Bearer " + token["access_token"]}
                )
            ).status_code == 200
            form = {
                "grant_type": "refresh_token",
                "client_id": "rp-a",
                "refresh_token": token["refresh_token"],
            }
            responses = await asyncio.gather(
                browser.post("/oidc/token", data=form), other.post("/oidc/token", data=form)
            )
            assert sorted(response.status_code for response in responses) == [200, 400]
            winner = next(response.json() for response in responses if response.status_code == 200)
            assert (
                await browser.get(
                    "/oidc/userinfo", headers={"Authorization": "Bearer " + winner["access_token"]}
                )
            ).status_code == 401
    finally:
        keys = [key async for key in first.redis_client.client.scan_iter(prefix + ":*")]
        if keys:
            await first.redis_client.client.delete(*keys)
        await first.stop()
        await second.stop()


async def test_client_basic_auth_is_required_without_consuming_code(oidc):
    browser, _, _, _ = oidc
    code = await authorize(browser, "secret-rp")
    assert (await exchange(browser, code, "secret-rp")).status_code == 401
    form = {
        "grant_type": "authorization_code",
        "code": code,
        "redirect_uri": "https://secret-rp.example/callback",
        "code_verifier": VERIFIER,
    }
    response = await browser.post("/oidc/token", data=form, auth=("secret-rp", "wrong"))
    assert response.status_code == 401
    response = await browser.post("/oidc/token", data=form, auth=("secret-rp", "client-password"))
    assert response.status_code == 200
    token = response.json()["access_token"]
    assert (
        await browser.post("/oidc/introspect", data={"client_id": "rp-b", "token": token})
    ).json() == {"active": False}
    assert (
        await browser.post(
            "/oidc/introspect", data={"token": token}, auth=("secret-rp", "client-password")
        )
    ).json()["active"] is True
    assert (
        await browser.post(
            "/oidc/revoke", data={"token": token}, auth=("secret-rp", "client-password")
        )
    ).status_code == 200
    assert (
        await browser.post(
            "/oidc/introspect", data={"token": token}, auth=("secret-rp", "client-password")
        )
    ).json() == {"active": False}
