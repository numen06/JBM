"""Authentication throttling, durable attribution, own alerts and key lifecycle."""

import calendar
import json
import time
from datetime import UTC, datetime
from urllib.parse import parse_qs, urlsplit

import pytest
from jbm_cluster_py.platform.auth.key_login import six_months_after
from jbm_cluster_py.platform.auth.login_security import OfflineRegions, different_location
from sqlalchemy import text
from test_file_key import bind, proof, public, signed
from test_file_key import key as file_key_fixture
from test_key_login import PKCE, VERIFIER, login
from test_key_login import client as key_client

client = key_client
key = file_key_fixture


def password_login(client, password="admin123", ip="114.114.114.114"):
    return client.post(
        "/oauth2/doLogin",
        data={
            "response_type": "code",
            "client_id": "JBM",
            "redirect_uri": "http://admin.test/login/callback",
            "code_challenge": PKCE,
            "code_challenge_method": "S256",
            "username": "admin",
            "password": password,
            "loginType": "PASSWORD",
        },
        headers={"X-Forwarded-For": ip},
    )


def key_tokens(client, key):
    response = login(client, proof(client, key), "FILE_KEY")
    assert response.status_code == 200, response.text
    code = parse_qs(urlsplit(response.json()["result"]).query)["code"][0]
    response = client.post(
        "/oauth2/token",
        data={
            "grant_type": "authorization_code",
            "client_id": "JBM",
            "client_secret": "demo-secret",
            "code": code,
            "redirect_uri": "http://admin.test/login/callback",
            "code_verifier": VERIFIER,
        },
    )
    assert response.status_code == 200, response.text
    return response.json()


def rows(client):
    async def read():
        async with client.app.state.repository.engine.connect() as conn:
            return [
                dict(row)
                for row in (
                    await conn.execute(
                        text("SELECT * FROM base_account_logs ORDER BY account_log_id")
                    )
                ).mappings()
            ]

    return client.portal.call(read)


def test_bad_key_and_password_share_account_lock_and_have_attributed_logs(client, key):
    options = bind(client, key)
    for index in range(5):
        payload = proof(client, key)
        payload["signature"] = "invalid"
        response = login(client, payload, "FILE_KEY")
        assert response.status_code == 401, (index, response.text)
    assert login(client, proof(client, key), "FILE_KEY").status_code == 423
    assert password_login(client).status_code == 423
    failures = [row for row in rows(client) if row["login_type"] == "FILE_KEY"]
    assert len(failures) == 6
    assert all(
        row["account"] == "admin"
        and row["user_id"] is not None
        and row["credential_id"] == options["keyId"]
        and row["request_id"]
        and row["status"] == 0
        for row in failures
    )
    assert all(
        "PRIVATE" not in row["message"] and "signature" not in row["message"] for row in failures
    )
    client.portal.call(client.app.state.auth_service.cache.clear_login_error, "admin")
    assert login(client, proof(client, key), "FILE_KEY").status_code == 200


def test_password_errors_also_lock_valid_keys_and_unknown_keys_use_source_limit(client, key):
    bind(client, key)
    for _ in range(5):
        assert password_login(client, "wrong").status_code == 401
    assert login(client, proof(client, key), "FILE_KEY").status_code == 423
    client.portal.call(client.app.state.auth_service.cache.clear_login_error, "admin")
    for _ in range(5):
        assert login(client, {}, "FILE_KEY").status_code == 401
    assert login(client, {}, "FILE_KEY").status_code == 423
    assert client.portal.call(client.app.state.auth_service.cache.login_error_count, "admin") == 0
    assert login(client, proof(client, key), "FILE_KEY").status_code == 200


@pytest.mark.parametrize("invalidate", ["remove", "password", "expire"])
def test_key_login_access_and_refresh_are_invalidated_together(client, key, invalidate):
    options = bind(client, key)
    tokens = key_tokens(client, key)
    headers = {"Authorization": "Bearer " + tokens["access_token"]}
    assert client.get("/oauth2/userinfo", headers=headers).status_code == 200
    if invalidate == "remove":
        assert client.delete("/oauth2/keys/" + options["keyId"]).status_code == 200
    else:

        async def alter():
            async with client.app.state.repository.engine.begin() as conn:
                if invalidate == "password":
                    await conn.execute(
                        text(
                            "UPDATE base_account SET password='Changed-password-123!' "
                            "WHERE account='admin'"
                        )
                    )
                else:
                    row = (
                        await conn.execute(
                            text("SELECT data FROM base_auth_credential WHERE id=:id"),
                            {"id": options["keyId"]},
                        )
                    ).scalar_one()
                    data = json.loads(row)
                    data["expiresAt"] = int(time.time()) - 1
                    await conn.execute(
                        text("UPDATE base_auth_credential SET data=:data WHERE id=:id"),
                        {"data": json.dumps(data), "id": options["keyId"]},
                    )

        client.portal.call(alter)
    assert login(client, proof(client, key), "FILE_KEY").status_code == 401
    assert client.get("/oauth2/userinfo", headers=headers).status_code == 401
    response = client.post(
        "/oauth2/token",
        data={
            "grant_type": "refresh_token",
            "client_id": "JBM",
            "client_secret": "demo-secret",
            "refresh_token": tokens["refresh_token"],
        },
    )
    assert response.status_code in (400, 401)


def test_default_half_year_and_explicit_permanent(client, key):
    options = bind(client, key)
    assert options["expiresAt"] == six_months_after(options["createdAt"])
    record = client.get("/oauth2/keys").json()["result"][0]
    assert record["expires_at"] == options["expiresAt"] and record["is_valid"] is True
    assert client.delete("/oauth2/keys/" + options["keyId"]).status_code == 200
    response = client.post(
        "/oauth2/keys/register-options",
        json={
            "kind": "FILE_KEY",
            "name": "长期密钥",
            "publicKey": public(key),
            "validity": "permanent",
        },
    )
    permanent = response.json()["result"]
    assert permanent["expiresAt"] is None
    assert (
        client.post(
            "/oauth2/keys/register",
            json={
                "kind": "FILE_KEY",
                "challengeId": permanent["challengeId"],
                "signature": signed(key, permanent["message"]),
            },
        ).status_code
        == 200
    )
    assert client.get("/oauth2/keys").json()["result"][0]["expires_at"] is None
    august = datetime(2026, 8, 31, 12, tzinfo=UTC)
    assert datetime.fromtimestamp(six_months_after(int(august.timestamp())), UTC) == datetime(
        2027, 2, calendar.monthrange(2027, 2)[1], 12, tzinfo=UTC
    )


def test_location_change_is_durable_visible_only_to_owner_and_acknowledged(client, monkeypatch):
    client._transport.client = ("127.0.0.1", 50000)
    locations = {
        "114.114.114.114": {
            "country": "中国",
            "province": "江苏",
            "city": "南京",
            "label": "中国 / 江苏 / 南京",
        },
        "223.5.5.5": {
            "country": "中国",
            "province": "浙江",
            "city": "杭州",
            "label": "中国 / 浙江 / 杭州",
        },
        "223.6.6.6": {
            "country": "中国",
            "province": "浙江",
            "city": "杭州",
            "label": "中国 / 浙江 / 杭州",
        },
    }
    monkeypatch.setattr(
        client.app.state.auth_service.login_security.geo, "lookup", lambda ip: locations.get(ip, {})
    )
    assert password_login(client).status_code == 200
    assert client.get("/oauth2/security/login-alerts").json()["result"] == []
    assert password_login(client, "wrong", "223.5.5.5").status_code == 401
    assert client.get("/oauth2/security/login-alerts").json()["result"] == []
    assert password_login(client, ip="223.5.5.5").status_code == 200
    alerts = client.get("/oauth2/security/login-alerts?userId=999").json()["result"]
    assert len(alerts) == 1
    assert alerts[0]["previous"]["ip"] == "114.114.114.114"
    assert alerts[0]["current"]["location"] == "中国 / 浙江 / 杭州"
    assert client.get("/oauth2/security/login-alerts").json()["result"] == alerts
    assert password_login(client, ip="223.6.6.6").status_code == 200
    assert len(client.get("/oauth2/security/login-alerts").json()["result"]) == 1
    assert (
        client.post(
            "/oauth2/security/login-alerts/acknowledge", json={"ids": [alerts[0]["id"]]}
        ).json()["result"]
        == []
    )
    assert (
        client.get("/oauth2/security/login-alerts", headers={"Authorization": ""}).status_code
        == 401
    )
    assert not different_location(
        {"country": "中国", "city": ""}, {"country": "中国", "city": "南京"}
    )


def test_untrusted_forwarded_headers_do_not_forge_location(client):
    client._transport.client = ("198.51.100.10", 50000)
    assert password_login(client, ip="223.5.5.5").status_code == 200
    assert rows(client)[-1]["ip"] == "198.51.100.10"


def test_corrupt_file_reports_without_private_material_and_locks_known_account(client, key):
    options = bind(client, key)
    payload = {"client_id": "JBM", "publicKey": public(key)}
    for _ in range(5):
        assert client.post("/oauth2/keys/login-failure", json=payload).status_code == 401
    assert client.post("/oauth2/keys/login-failure", json=payload).status_code == 423
    assert login(client, proof(client, key), "FILE_KEY").status_code == 423
    assert rows(client)[-1]["credential_id"] == options["keyId"]
    assert rows(client)[-1]["account"] == "admin"


def test_alert_ids_cannot_acknowledge_another_account(client):
    repository = client.app.state.repository

    async def seed_other_alert():
        await repository.record_login(
            user_id=999,
            account="other",
            login_type="PASSWORD",
            ip="114.114.114.114",
            user_agent="fixture",
            details={"risk_level": "warning", "risk_detail": "{}"},
        )

    client.portal.call(seed_other_alert)
    other = rows(client)[-1]
    assert client.get("/oauth2/security/login-alerts").json()["result"] == []
    assert (
        client.post(
            "/oauth2/security/login-alerts/acknowledge", json={"ids": [other["account_log_id"]]}
        ).status_code
        == 200
    )
    assert rows(client)[-1]["risk_ack_time"] is None


def test_offline_ipv4_ipv6_regions_without_external_requests():
    resolver = OfflineRegions({})
    if not (resolver.path / "ip2region_v4.xdb").exists():
        pytest.skip("Run python auth/fetch_ip_regions.py for pinned offline data")
    resolver.start()
    assert resolver.lookup("114.114.114.114")["city"] == "南京市"
    assert resolver.lookup("240e:3b7:3272:d8d0:db09:c067:8d59:539e")["country"] == "中国"
    assert resolver.lookup("127.0.0.1") == {}
    assert resolver.lookup("not-an-ip") == {}
