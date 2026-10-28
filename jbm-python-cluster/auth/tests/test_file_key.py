"""Portable file keys use signed challenges and the existing OAuth session path."""

import base64
import hashlib
import json
import time
from urllib.parse import parse_qs, urlsplit

import pytest
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, padding, rsa
from sqlalchemy import text
from test_key_login import VERIFIER, login
from test_key_login import client as key_client

client = key_client


@pytest.fixture
def key():
    return rsa.generate_private_key(public_exponent=65537, key_size=2048)


def public(key):
    return (
        key.public_key()
        .public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo)
        .decode()
    )


def signed(key, message):
    return base64.b64encode(
        key.sign(message.encode(), padding.PKCS1v15(), hashes.SHA256())
    ).decode()


def encrypted_password(client, password="admin123"):
    encoded = client.get("/oauth2/publicKey", params={"app_id": "JBM"}).json()["result"]
    public_key = serialization.load_der_public_key(base64.b64decode(encoded))
    return base64.b64encode(public_key.encrypt(password.encode(), padding.PKCS1v15())).decode()


def bind(client, key):
    verified = client.post(
        "/oauth2/keys/verify-password", json={"password": encrypted_password(client)}
    )
    assert verified.status_code == 200, verified.text
    response = client.post(
        "/oauth2/keys/register-options",
        json={
            "kind": "FILE_KEY",
            "name": "测试文件",
            "publicKey": public(key),
        },
    )
    assert response.status_code == 200, response.text
    options = response.json()["result"]
    assert options["account"] == "admin"
    assert options["rpId"] == "admin.test"
    response = client.post(
        "/oauth2/keys/register",
        json={
            "kind": "FILE_KEY",
            "challengeId": options["challengeId"],
            "signature": signed(key, options["message"]),
        },
    )
    assert response.status_code == 200, response.text
    return options


def proof(client, key):
    response = client.post(
        "/oauth2/keys/login-options", json={"kind": "FILE_KEY", "client_id": "JBM"}
    )
    assert response.status_code == 200, response.text
    options = response.json()["result"]
    return {
        "challengeId": options["challengeId"],
        "publicKey": public(key),
        "signature": signed(key, options["message"]),
    }


def test_file_key_oauth_identity_replay_and_revoke(client, key):
    options = bind(client, key)
    rows = client.get("/oauth2/keys").json()["result"]
    assert rows[0]["kind"] == "FILE_KEY"
    assert "privateKey" not in json.dumps(rows)
    assert rows[0]["id"] == options["keyId"]
    payload = proof(client, key)
    response = login(client, payload, "FILE_KEY")
    assert response.status_code == 200, response.text
    code = parse_qs(urlsplit(response.json()["result"]).query)["code"][0]
    tokens = client.post(
        "/oauth2/token",
        data={
            "grant_type": "authorization_code",
            "client_id": "JBM",
            "code": code,
            "redirect_uri": "http://admin.test/login/callback",
            "code_verifier": VERIFIER,
        },
    )
    assert tokens.status_code == 200, tokens.text
    identity = client.get(
        "/oauth2/userinfo", headers={"Authorization": "Bearer " + tokens.json()["access_token"]}
    )
    assert identity.json()["result"]["username"] == "admin"
    # The key login superseded the fixture's password-based Web session.
    client.headers["Authorization"] = "Bearer " + tokens.json()["access_token"]
    assert login(client, payload, "FILE_KEY").status_code == 401
    assert client.get("/oauth2/keys").json()["result"][0]["last_used_at"] is not None
    assert client.delete("/oauth2/keys/" + options["keyId"]).status_code == 200
    assert login(client, proof(client, key), "FILE_KEY").status_code == 401


def test_wrong_signature_and_challenge_purpose(client, key):
    options = bind(client, key)
    payload = proof(client, key)
    other = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    payload["signature"] = signed(other, "wrong message")
    assert login(client, payload, "FILE_KEY").status_code == 401
    assert (
        login(
            client,
            {
                "challengeId": options["challengeId"],
                "publicKey": public(key),
                "signature": signed(key, options["message"]),
            },
            "FILE_KEY",
        ).status_code
        == 401
    )
    assert (
        client.post(
            "/oauth2/keys/register",
            json={
                "kind": "FILE_KEY",
                "challengeId": proof(client, key)["challengeId"],
                "signature": "invalid",
            },
        ).status_code
        == 401
    )


@pytest.mark.parametrize("bad_key", ["invalid", "private", "weak", "ec"])
def test_reject_invalid_public_keys(client, key, bad_key):
    assert (
        client.post(
            "/oauth2/keys/verify-password", json={"password": encrypted_password(client)}
        ).status_code
        == 200
    )
    values = {
        "invalid": "not a PEM key",
        "private": key.private_bytes(
            serialization.Encoding.PEM,
            serialization.PrivateFormat.TraditionalOpenSSL,
            serialization.NoEncryption(),
        ).decode(),
        "weak": public(rsa.generate_private_key(public_exponent=65537, key_size=1024)),
        "ec": public(ec.generate_private_key(ec.SECP256R1())),
    }
    response = client.post(
        "/oauth2/keys/register-options",
        json={
            "kind": "FILE_KEY",
            "publicKey": values[bad_key],
        },
    )
    assert response.status_code == 400, response.text
    assert client.get("/oauth2/keys").json()["result"] == []


def test_registration_is_bound_to_session_and_duplicate_key_rejected(client, key):
    options = bind(client, key)
    duplicate = client.post(
        "/oauth2/keys/register-options",
        json={"kind": "FILE_KEY", "publicKey": public(key)},
    ).json()["result"]
    assert (
        client.post(
            "/oauth2/keys/register",
            json={
                "kind": "FILE_KEY",
                "challengeId": duplicate["challengeId"],
                "signature": signed(key, duplicate["message"]),
            },
        ).status_code
        == 409
    )
    challenge = client.post(
        "/oauth2/keys/register-options",
        json={"kind": "FILE_KEY", "publicKey": public(key)},
    ).json()["result"]
    tokens = client.post(
        "/oauth2/token",
        data={
            "grant_type": "password",
            "client_id": "JBM",
            "client_secret": "demo-secret",
            "username": "admin",
            "password": "admin123",
        },
    )
    assert tokens.status_code == 200
    assert (
        client.post(
            "/oauth2/keys/verify-password",
            json={},
            headers={"Authorization": "Bearer " + tokens.json()["access_token"]},
        ).status_code
        == 403
    )
    response = client.post(
        "/oauth2/keys/register",
        headers={"Authorization": "Bearer " + tokens.json()["access_token"]},
        json={
            "kind": "FILE_KEY",
            "challengeId": challenge["challengeId"],
            "signature": signed(key, challenge["message"]),
        },
    )
    assert response.status_code == 401
    assert client.get("/oauth2/keys", headers={"Authorization": "Bearer " + tokens.json()["access_token"]}).json()["result"][0]["id"] == options["keyId"]


def test_file_key_requires_current_password_and_limits_retries(client, key):
    payload = {"kind": "FILE_KEY", "publicKey": public(key)}
    assert client.post("/oauth2/keys/register-options", json=payload).status_code == 403
    assert client.post("/oauth2/keys/verify-password", json={"password": ""}).status_code == 400
    payload = {"password": "admin123"}  # Plaintext is never accepted by this endpoint.
    assert client.post("/oauth2/keys/verify-password", json=payload).status_code == 403
    payload["password"] = encrypted_password(client, "wrong-password")
    for _ in range(client.app.state.auth_service.max_errors - 1):
        response = client.post("/oauth2/keys/verify-password", json=payload)
        assert response.status_code == 403
        assert response.json()["message"] == "当前账号密码错误"
    payload["password"] = encrypted_password(client)
    assert client.post("/oauth2/keys/verify-password", json=payload).status_code == 423
    assert client.get("/oauth2/keys").json()["result"] == []


def test_initial_password_account_can_generate_without_clearing_password_flag(client, key):
    async def require_password_update():
        async with client.app.state.repository.engine.begin() as conn:
            await conn.execute(
                text("UPDATE base_account SET must_change_password=1 WHERE account='admin'")
            )

    client.portal.call(require_password_update)
    tokens = client.post(
        "/oauth2/token",
        data={
            "grant_type": "password",
            "client_id": "JBM",
            "client_secret": "demo-secret",
            "username": "admin",
            "password": "admin123",
        },
    )
    assert tokens.status_code == 200, tokens.text
    client.headers["Authorization"] = "Bearer " + tokens.json()["access_token"]
    assert client.get("/oauth2/keys").status_code == 200
    options = bind(client, key)
    assert client.get("/oauth2/userinfo").json()["result"]["mustChangePassword"] is True
    assert login(client, proof(client, key), "FILE_KEY").status_code == 200
    assert client.delete("/oauth2/keys/" + options["keyId"]).status_code == 200
    # Existing Passkey/SSH policy is unaffected.
    assert client.post("/oauth2/keys/register-options", json={"kind": "PASSKEY"}).status_code == 403


def test_verification_reused_for_60_seconds_without_extending_and_checked_at_commit(client, key):
    verified = client.post(
        "/oauth2/keys/verify-password", json={"password": encrypted_password(client)}
    )
    assert verified.json()["result"]["expiresIn"] == 60
    cache_key = (
        "key-password:" + hashlib.sha256(client.headers["Authorization"][7:].encode()).hexdigest()
    )
    cache = client.app.state.auth_service.cache
    before = client.portal.call(cache.get_json, cache_key)
    for _ in range(2):
        checked = client.post("/oauth2/keys/verify-password", json={})
        assert checked.status_code == 200
        assert 0 <= checked.json()["result"]["expiresIn"] <= 60
    assert client.portal.call(cache.get_json, cache_key) == before
    options = client.post(
        "/oauth2/keys/register-options", json={"kind": "FILE_KEY", "publicKey": public(key)}
    ).json()["result"]
    client.portal.call(cache.set_json, cache_key, {"expiresAt": time.time() - 1}, 60)
    assert client.post("/oauth2/keys/verify-password", json={}).status_code == 403
    assert (
        client.post(
            "/oauth2/keys/register-options", json={"kind": "FILE_KEY", "publicKey": public(key)}
        ).status_code
        == 403
    )
    response = client.post(
        "/oauth2/keys/register",
        json={
            "kind": "FILE_KEY",
            "challengeId": options["challengeId"],
            "signature": signed(key, options["message"]),
        },
    )
    assert response.status_code == 403
    assert client.get("/oauth2/keys").json()["result"] == []
    assert (
        client.post(
            "/oauth2/keys/verify-password", json={"password": encrypted_password(client)}
        ).status_code
        == 200
    )
    assert client.post("/oauth2/keys/verify-password", json={}).status_code == 200


def test_names_are_unique_after_trimming_casefold_and_competing_challenges(client, key):
    assert (
        client.post(
            "/oauth2/keys/verify-password", json={"password": encrypted_password(client)}
        ).status_code
        == 200
    )
    second = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    challenges = []
    for name, signing_key in [(" Test Key ", key), ("test key", second)]:
        response = client.post(
            "/oauth2/keys/register-options",
            json={"kind": "FILE_KEY", "name": name, "publicKey": public(signing_key)},
        )
        assert response.status_code == 200
        challenges.append(response.json()["result"])
    for index, signing_key in enumerate([key, second]):
        challenge = challenges[index]
        response = client.post(
            "/oauth2/keys/register",
            json={
                "kind": "FILE_KEY",
                "challengeId": challenge["challengeId"],
                "signature": signed(signing_key, challenge["message"]),
            },
        )
        assert response.status_code == (200 if index == 0 else 409)
    rows = client.get("/oauth2/keys").json()["result"]
    assert len(rows) == 1 and rows[0]["name"] == "Test Key"
    assert (
        client.post(
            "/oauth2/keys/register-options",
            json={"kind": "FILE_KEY", "name": "TEST KEY", "publicKey": public(second)},
        ).status_code
        == 409
    )
    assert (
        client.post(
            "/oauth2/keys/register-options", json={"kind": "PASSKEY", "name": "test key"}
        ).status_code
        == 409
    )
    assert (
        client.post(
            "/oauth2/keys/register-options",
            json={"kind": "FILE_KEY", "name": "x" * 81, "publicKey": public(second)},
        ).status_code
        == 400
    )
