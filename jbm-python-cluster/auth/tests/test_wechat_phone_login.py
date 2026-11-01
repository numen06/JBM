import asyncio
import json
from urllib.parse import parse_qs, urlparse

import httpx
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import text

from jbm_cluster_py.platform.auth.main import create_app
from test_auth_compat import auth_config, seed_database, PKCE_CHALLENGE, PKCE_VERIFIER

APP_ID = "wx-test-phone"
MOBILE = "13800000000"


@pytest.fixture
def phone_login(tmp_path):
    url = f"sqlite+aiosqlite:///{tmp_path / 'wechat.db'}"
    asyncio.run(seed_database(url))
    config = auth_config(url)
    config.raw["jbm"]["auth"]["login-providers"]["miniapp"] = {
        "enabled": True, "mode": "wechat-phone", "app-id": APP_ID,
        "app-secret": "server-only-fixture-secret", "client-ids": ["JBM"],
    }
    counts = {"token": 0, "phone": 0}
    response = {"errcode": 0, "phone_info": {
        "purePhoneNumber": MOBILE, "countryCode": "86", "watermark": {"appid": APP_ID},
    }}

    def upstream(request):
        assert request.url.host == "api.weixin.qq.com"
        data = json.loads(request.content)
        if request.url.path == "/cgi-bin/stable_token":
            counts["token"] += 1
            assert data["appid"] == APP_ID and data["secret"] == "server-only-fixture-secret"
            return httpx.Response(200, json={"access_token": "server-fixture-token", "expires_in": 7200})
        assert request.url.path == "/wxa/business/getuserphonenumber"
        counts["phone"] += 1
        assert request.url.params["access_token"] == "server-fixture-token"
        assert data == {"code": "one-use-phone-code"}
        return httpx.Response(200, json=response)

    with TestClient(create_app(config)) as browser:
        service = browser.app.state.auth_service
        original_http = service.http_client
        service.http_client = httpx.AsyncClient(transport=httpx.MockTransport(upstream))
        yield browser, service, response, counts
        asyncio.run(service.http_client.aclose())
        service.http_client = original_http


def submit(browser, **overrides):
    return browser.post("/oauth2/doLogin", data={
        "response_type": "code", "client_id": "JBM", "redirect_uri": "http://admin.test/login/callback",
        "scope": "all", "state": "fresh-state", "code_challenge": PKCE_CHALLENGE,
        "code_challenge_method": "S256", "loginType": "MINIAPP",
        "wx_app_id": APP_ID, "wx_phone_code": "one-use-phone-code", **overrides,
    })


def test_wechat_phone_enters_normal_pkce_session_without_exposing_secrets(phone_login):
    browser, service, _, counts = phone_login
    options = browser.get("/oauth2/miniapp/options", params={"client_id": "JBM", "app_id": APP_ID})
    assert options.json()["result"] == {"enabled": True, "message": ""}
    login = submit(browser)
    assert login.status_code == 200, login.text
    params = parse_qs(urlparse(login.json()["result"]).query)
    assert params["state"] == ["fresh-state"]
    exchange = browser.post("/oauth2/token", data={
        "grant_type": "authorization_code", "client_id": "JBM", "code": params["code"][0],
        "redirect_uri": "http://admin.test/login/callback", "code_verifier": PKCE_VERIFIER,
    })
    assert exchange.status_code == 200, exchange.text
    token = exchange.json()["access_token"]
    user = browser.get("/oauth2/userinfo", headers={"Authorization": "Bearer " + token}).json()["result"]
    assert user["username"] == "admin"
    assert user["userId"] == 2057849052900044802
    assert int(user["authTime"]) > 0
    assert "one-use-phone-code" not in login.text + exchange.text + options.text
    assert "server-fixture-token" not in login.text + exchange.text + options.text
    assert submit(browser).status_code == 200
    assert counts == {"token": 1, "phone": 2}


@pytest.mark.parametrize("kind,status", [("unknown", 403), ("duplicate", 409), ("disabled", 403),
                                        ("source", 401), ("replay", 401), ("app", 503)])
def test_wechat_phone_denies_unsafe_account_or_grant(phone_login, kind, status):
    browser, service, response, counts = phone_login

    async def alter(sql):
        async with service.repository.engine.begin() as conn:
            await conn.execute(text(sql))

    if kind == "unknown":
        response["phone_info"]["purePhoneNumber"] = "13900000001"
    elif kind == "duplicate":
        original = service.repository.find_users_by_mobile
        async def duplicate_users(mobile):
            users = await original(mobile)
            return users + [{**users[0], "user_id": 99}]
        service.repository.find_users_by_mobile = duplicate_users
    elif kind == "disabled":
        asyncio.run(alter("UPDATE base_account SET status=0 WHERE account_type='mobile'"))
    elif kind == "source":
        response["phone_info"]["watermark"]["appid"] = "wrong-app"
    elif kind == "replay":
        response.clear()
        response.update({"errcode": 40029, "errmsg": "invalid code"})
    result = submit(browser, **({"wx_app_id": "wrong-app"} if kind == "app" else {}))
    assert result.status_code == status, result.text
    assert "code=" not in result.text
    if kind == "app":
        assert counts == {"token": 0, "phone": 0}


def test_wechat_phone_binds_existing_profile_only_and_never_uses_client_phone(phone_login):
    browser, service, _, _ = phone_login

    async def remove_mobile_account():
        async with service.repository.engine.begin() as conn:
            await conn.execute(text("DELETE FROM base_account WHERE account_type='mobile'"))
    asyncio.run(remove_mobile_account())
    result = submit(browser, username="attacker", mobile="13900000001", phone="13900000001")
    assert result.status_code == 200, result.text
    account = asyncio.run(service.repository.find_account(MOBILE, "mobile", service.account_domain))
    assert int(account["user_id"]) == 2057849052900044802
    assert asyncio.run(service.repository.find_users_by_mobile("13900000001")) == []


def test_wechat_phone_readiness_never_exposes_server_config(phone_login):
    browser, service, _, _ = phone_login
    service.login_providers["miniapp"]["app-secret"] = ""
    result = browser.get("/oauth2/miniapp/options", params={"client_id": "JBM", "app_id": APP_ID})
    assert result.json()["result"]["enabled"] is False
    assert submit(browser).status_code == 503
    assert "app-secret" not in result.text


def test_wechat_phone_reads_mounted_secret_without_exposing_it(phone_login, tmp_path):
    browser, service, _, _ = phone_login
    settings = service.login_providers["miniapp"]
    settings.pop("app-secret")
    secret_file = tmp_path / "docker-secret"
    secret_file.write_text("server-only-fixture-secret\n", encoding="utf-8")
    settings["app-secret-file"] = str(secret_file)
    assert submit(browser).status_code == 200
    secret_file.unlink()
    assert submit(browser).status_code == 503
