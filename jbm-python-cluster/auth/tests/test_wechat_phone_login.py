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


def password_session(browser):
    login = browser.post('/oauth2/doLogin', data={
        'response_type': 'code', 'client_id': 'JBM', 'redirect_uri': 'http://admin.test/login/callback',
        'scope': 'all', 'state': 'binding-session', 'code_challenge': PKCE_CHALLENGE,
        'code_challenge_method': 'S256', 'username': 'admin', 'password': 'admin123',
    })
    assert login.status_code == 200, login.text
    code = parse_qs(urlparse(login.json()['result']).query)['code'][0]
    result = browser.post('/oauth2/token', data={'grant_type': 'authorization_code', 'client_id': 'JBM',
        'redirect_uri': 'http://admin.test/login/callback', 'code': code, 'code_verifier': PKCE_VERIFIER})
    assert result.status_code == 200, result.text
    return {'Authorization': 'Bearer ' + result.json()['access_token']}


def clear_mobile(service):
    async def clear():
        async with service.repository.engine.begin() as conn:
            await conn.execute(text("DELETE FROM base_account WHERE account_type='mobile'"))
            await conn.execute(text("UPDATE base_user SET mobile=NULL WHERE user_name='admin'"))
    asyncio.run(clear())


def bind_phone(browser, headers, **extra):
    return browser.post('/oauth2/miniapp/mobile/bind', headers=headers,
        json={'wx_app_id': APP_ID, 'wx_phone_code': 'one-use-phone-code', **extra})


def test_authenticated_user_can_bind_verified_wechat_phone_to_self_and_login(phone_login):
    browser, service, _, _ = phone_login
    clear_mobile(service)
    headers = password_session(browser)
    before = browser.get('/oauth2/userinfo', headers=headers).json()['result']
    result = bind_phone(browser, headers, user_id=99, userId=99, mobile='13900000001')
    assert result.status_code == 200, result.text
    assert result.json()['result'] == {'mobileBound': True, 'mobileMasked': '138****0000'}
    user = asyncio.run(service.repository.find_user(before['userId']))
    assert user['mobile'] == MOBILE
    assert asyncio.run(service.repository.find_user(99)) is None
    after = browser.get('/oauth2/userinfo', headers=headers).json()['result']
    for key in ('userId', 'tenantId', 'roles', 'permissions'):
        assert after.get(key) == before.get(key)
    assert submit(browser).status_code == 200


@pytest.mark.parametrize('kind,status', [('anonymous',401),('recent',401),('conflict',409),('invalid',400),('existing',409)])
def test_wechat_phone_binding_requires_identity_fresh_auth_and_free_number(phone_login, kind, status):
    browser, service, response, counts = phone_login
    clear_mobile(service)
    headers = {} if kind == 'anonymous' else password_session(browser)
    if kind == 'recent':
        original = service.userinfo
        async def stale(token):
            return {**await original(token), 'authTime': 1}
        service.userinfo = stale
    elif kind in {'conflict','existing'}:
        async def alter():
            async with service.repository.engine.begin() as conn:
                if kind == 'conflict':
                    await conn.execute(text("INSERT INTO base_user (user_id,user_name,mobile,status) VALUES (99,'other','13800000000',1)"))
                else:
                    await conn.execute(text("UPDATE base_user SET mobile='13900000001' WHERE user_name='admin'"))
        asyncio.run(alter())
    elif kind == 'invalid':
        response.clear(); response.update({'errcode':40029})
    result = bind_phone(browser, headers)
    assert result.status_code == status, result.text
    if kind in {'anonymous','recent'}:
        assert counts == {'token':0,'phone':0}
    if kind == 'recent':
        assert '需要重新验证身份' in result.text
    if kind == 'invalid':
        assert browser.get('/oauth2/userinfo', headers=headers).status_code == 200
    user = asyncio.run(service.repository.find_user(2057849052900044802))
    assert user['mobile'] == ('13900000001' if kind == 'existing' else None)


def test_initial_binding_transaction_never_replaces_a_recorded_phone_or_reenables_disabled_credentials(phone_login):
    _, service, _, _ = phone_login
    async def check():
        with pytest.raises(ValueError, match='已有手机号'):
            await service.repository.bind_mobile_account(2057849052900044802, '13900000001', service.account_domain, initial_only=True)
        async with service.repository.engine.begin() as conn:
            await conn.execute(text("UPDATE base_account SET status=0 WHERE account_type='mobile'"))
        with pytest.raises(ValueError, match='禁用'):
            await service.repository.bind_mobile_account(2057849052900044802, MOBILE, service.account_domain, initial_only=True)
        user = await service.repository.find_user(2057849052900044802)
        account = await service.repository.find_account(MOBILE, 'mobile', service.account_domain)
        assert user['mobile'] == MOBILE
        assert account['status'] == 0
    asyncio.run(check())


def test_zero_id_administrator_can_bind_phone_and_keep_the_same_account(phone_login):
    browser, service, _, _ = phone_login
    clear_mobile(service)
    async def zero_admin():
        async with service.repository.engine.begin() as conn:
            await conn.execute(text("UPDATE base_user SET user_id=0 WHERE user_name='admin'"))
            await conn.execute(text("UPDATE base_account SET user_id=0"))
            await conn.execute(text("UPDATE base_role_user SET user_id=0"))
    asyncio.run(zero_admin())
    headers = password_session(browser)
    identity = browser.get('/oauth2/userinfo', headers=headers).json()['result']
    assert identity['userId'] == 0
    assert asyncio.run(service._current_user_id(headers['Authorization'][7:])) == 0
    result = bind_phone(browser, headers)
    assert result.status_code == 200, result.text
    assert result.json()['result']['mobileBound'] is True
    user = asyncio.run(service.repository.find_user(0))
    assert user['mobile'] == MOBILE
    assert submit(browser).status_code == 200
