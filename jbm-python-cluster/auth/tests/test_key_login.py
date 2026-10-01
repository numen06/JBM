"""Real cryptographic registration, assertion and OAuth/PKCE regression tests."""

import asyncio
import hashlib
import json
import secrets
import struct
import subprocess
from urllib.parse import parse_qs, urlsplit

import cbor2
import pytest
from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
from fastapi.testclient import TestClient
from jbm_cluster_py.platform.auth.key_login import encode
from jbm_cluster_py.platform.auth.main import create_app
from jbm_cluster_py.platform.auth.ssh_signature import verify_ssh_signature
from sqlalchemy import text
from test_auth_compat import auth_config, seed_database

ORIGIN = "https://admin.test"
VERIFIER = "p" * 64
PKCE = encode(hashlib.sha256(VERIFIER.encode()).digest())


@pytest.fixture
def client(tmp_path):
    url = f"sqlite+aiosqlite:///{tmp_path / 'keys.db'}"
    asyncio.run(seed_database(url))
    config = auth_config(url)
    config.raw["jbm"]["auth"]["key-login"] = {
        "enabled": True,
        "rp-id": "admin.test",
        "origins": [ORIGIN],
    }
    with TestClient(create_app(config)) as client:
        response = client.post(
            "/oauth2/token",
            data={
                "grant_type": "password",
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "username": "admin",
                "password": "admin123",
            },
        )
        assert response.status_code == 200, response.text
        client.headers["Authorization"] = "Bearer " + response.json()["access_token"]
        yield client


class Authenticator:
    def __init__(self):
        self.key = ec.generate_private_key(ec.SECP256R1())
        self.id = secrets.token_bytes(32)
        self.handle = ""

    def register(self, options, origin=ORIGIN, flags=0x45):
        options = options["publicKey"]
        self.handle = options["user"]["id"]
        client = json.dumps(
            {"type": "webauthn.create", "challenge": options["challenge"], "origin": origin}
        ).encode()
        numbers = self.key.public_key().public_numbers()
        cose = cbor2.dumps(
            {
                1: 2,
                3: -7,
                -1: 1,
                -2: numbers.x.to_bytes(32, "big"),
                -3: numbers.y.to_bytes(32, "big"),
            }
        )
        auth = hashlib.sha256(b"admin.test").digest() + bytes([flags]) + struct.pack(">I", 0)
        auth += bytes(16) + struct.pack(">H", len(self.id)) + self.id + cose
        return {
            "id": encode(self.id),
            "rawId": encode(self.id),
            "type": "public-key",
            "response": {
                "clientDataJSON": encode(client),
                "attestationObject": encode(
                    cbor2.dumps(
                        {
                            "fmt": "none",
                            "attStmt": {},
                            "authData": auth,
                        }
                    )
                ),
            },
        }

    def assertion(self, options, origin=ORIGIN, flags=5, count=1, handle=None, rp="admin.test"):
        client = json.dumps(
            {
                "type": "webauthn.get",
                "challenge": options["publicKey"]["challenge"],
                "origin": origin,
            }
        ).encode()
        auth = hashlib.sha256(rp.encode()).digest() + bytes([flags]) + struct.pack(">I", count)
        signature = self.key.sign(auth + hashlib.sha256(client).digest(), ec.ECDSA(hashes.SHA256()))
        return {
            "id": encode(self.id),
            "rawId": encode(self.id),
            "type": "public-key",
            "response": {
                "clientDataJSON": encode(client),
                "authenticatorData": encode(auth),
                "signature": encode(signature),
                "userHandle": self.handle if handle is None else handle,
            },
        }


def bind(client, authenticator):
    response = client.post("/oauth2/keys/register-options", json={"name": "Laptop"})
    assert response.status_code == 200, response.text
    options = response.json()["result"]
    assert options["publicKey"]["authenticatorSelection"]["residentKey"] == "required"
    response = client.post(
        "/oauth2/keys/register",
        json={
            "challengeId": options["challengeId"],
            "credential": authenticator.register(options),
        },
    )
    assert response.status_code == 200, response.text


def options(client, kind="PASSKEY"):
    result = client.post("/oauth2/keys/login-options", json={"client_id": "JBM", "kind": kind})
    assert result.status_code == 200, result.text
    return result.json()["result"]


def login(client, proof, kind="PASSKEY", **overrides):
    return client.post(
        "/oauth2/doLogin",
        data={
            "response_type": "code",
            "client_id": "JBM",
            "redirect_uri": "http://admin.test/login/callback",
            "state": "test-state",
            "scope": "all",
            "code_challenge": PKCE,
            "code_challenge_method": "S256",
            "username": "",
            "password": json.dumps(proof),
            "loginType": kind,
            **overrides,
        },
    )


def test_passkey_full_oauth_session_and_replay(client):
    authenticator = Authenticator()
    bind(client, authenticator)
    challenge = options(client)
    assert not challenge["publicKey"].get("allowCredentials")
    proof = {
        "challengeId": challenge["challengeId"],
        "credential": authenticator.assertion(challenge),
    }
    client.headers.pop("Authorization")
    response = login(client, proof)
    assert response.status_code == 200, response.text
    query = parse_qs(urlsplit(response.json()["result"]).query)
    assert query["state"] == ["test-state"]
    token = client.post(
        "/oauth2/token",
        data={
            "grant_type": "authorization_code",
            "client_id": "JBM",
            "code": query["code"][0],
            "redirect_uri": "http://admin.test/login/callback",
            "code_verifier": VERIFIER,
        },
    )
    assert token.status_code == 200, token.text
    assert token.json()["refresh_token"]
    user = client.get(
        "/oauth2/userinfo", headers={"Authorization": "Bearer " + token.json()["access_token"]}
    )
    assert user.json()["result"]["username"] == "admin"
    assert "ACTION_SAVE" in user.json()["result"]["permissions"]
    assert login(client, proof).status_code == 401


def test_jbm_admin_user_id_zero_can_bind_existing_key(client):
    async def migrate_test_identity():
        async with client.app.state.repository.engine.begin() as conn:
            for table in ("base_user", "base_account", "base_role_user"):
                await conn.execute(
                    text(f"UPDATE {table} SET user_id=0 WHERE user_id=2057849052900044802")
                )

    client.portal.call(migrate_test_identity)
    login_response = client.post(
        "/oauth2/token",
        data={
            "grant_type": "password",
            "client_id": "JBM",
            "client_secret": "demo-secret",
            "username": "admin",
            "password": "admin123",
        },
    )
    assert login_response.status_code == 200, login_response.text
    client.headers["Authorization"] = "Bearer " + login_response.json()["access_token"]
    authenticator = Authenticator()
    bind(client, authenticator)
    challenge = options(client)
    proof = {
        "challengeId": challenge["challengeId"],
        "credential": authenticator.assertion(challenge),
    }
    response = login(client, proof)
    assert response.status_code == 200, response.text
    code = parse_qs(urlsplit(response.json()["result"]).query)["code"][0]
    token = client.post(
        "/oauth2/token",
        data={
            "grant_type": "authorization_code",
            "client_id": "JBM",
            "code": code,
            "redirect_uri": "http://admin.test/login/callback",
            "code_verifier": VERIFIER,
        },
    )
    assert token.status_code == 200, token.text
    identity = client.get(
        "/oauth2/userinfo",
        headers={"Authorization": "Bearer " + token.json()["access_token"]},
    )
    assert identity.json()["result"]["userId"] == 0


@pytest.mark.parametrize(
    "change",
    [
        {"origin": "https://evil.test"},
        {"flags": 1},
        {"handle": encode(b"another-user")},
        {"rp": "evil.test"},
        {"count": 0},
    ],
)
def test_reject_invalid_assertions(client, change):
    authenticator = Authenticator()
    bind(client, authenticator)
    challenge = options(client)
    assert (
        login(
            client,
            {
                "challengeId": challenge["challengeId"],
                "credential": authenticator.assertion(challenge),
            },
        ).status_code
        == 200
    )
    challenge = options(client)
    proof = {
        "challengeId": challenge["challengeId"],
        "credential": authenticator.assertion(challenge, **change),
    }
    assert login(client, proof).status_code == 401


def test_revocation_and_disabled_user(client):
    authenticator = Authenticator()
    bind(client, authenticator)
    challenge = options(client)
    proof = {
        "challengeId": challenge["challengeId"],
        "credential": authenticator.assertion(challenge),
    }
    key_id = client.get("/oauth2/keys").json()["result"][0]["id"]
    assert client.delete("/oauth2/keys/" + key_id).status_code == 200
    assert login(client, proof).status_code == 401
    bind(client, authenticator)
    challenge = options(client)

    async def disable():
        async with client.app.state.repository.engine.begin() as conn:
            await conn.execute(text("UPDATE base_user SET status=0"))

    client.portal.call(disable)
    assert (
        login(
            client,
            {
                "challengeId": challenge["challengeId"],
                "credential": authenticator.assertion(challenge),
            },
        ).status_code
        == 403
    )


def test_registration_requires_session_and_uv(client):
    authenticator = Authenticator()
    challenge = client.post("/oauth2/keys/register-options", json={}).json()["result"]
    assert (
        client.post(
            "/oauth2/keys/register",
            json={
                "challengeId": challenge["challengeId"],
                "credential": authenticator.register(challenge, flags=0x41),
            },
        ).status_code
        == 401
    )
    client.headers.pop("Authorization")
    assert client.post("/oauth2/keys/register-options", json={}).status_code in (400, 401)
    assert client.get("/oauth2/keys").status_code in (400, 401)


@pytest.mark.parametrize("invalidate", ["expire", "client", "signature", "account"])
def test_challenge_binding_and_account_state(client, invalidate):
    authenticator = Authenticator()
    bind(client, authenticator)
    challenge = options(client)
    proof = {
        "challengeId": challenge["challengeId"],
        "credential": authenticator.assertion(challenge),
    }

    async def alter():
        service = client.app.state.auth_service
        cache_key = "key-challenge:" + challenge["challengeId"]
        if invalidate == "expire":
            await service.cache.delete(cache_key)
        elif invalidate == "client":
            state = await service.cache.get_json(cache_key)
            state["clientId"] = "different-client"
            await service.cache.set_json(cache_key, state, 120)
        elif invalidate == "account":
            async with service.repository.engine.begin() as conn:
                await conn.execute(text("UPDATE base_account SET status=0"))

    client.portal.call(alter)
    if invalidate == "signature":
        proof["credential"]["response"]["signature"] = encode(bytes(64))
    assert login(client, proof).status_code in (401, 403)


def test_duplicate_registration_and_wrong_session(client):
    authenticator = Authenticator()
    bind(client, authenticator)
    challenge = client.post("/oauth2/keys/register-options", json={}).json()["result"]
    assert challenge["publicKey"]["excludeCredentials"]
    payload = {
        "challengeId": challenge["challengeId"],
        "credential": authenticator.register(challenge),
    }
    assert client.post("/oauth2/keys/register", json=payload).status_code == 409
    challenge = client.post("/oauth2/keys/register-options", json={}).json()["result"]
    payload = {
        "challengeId": challenge["challengeId"],
        "credential": authenticator.register(challenge),
    }
    token = client.post(
        "/oauth2/token",
        data={
            "grant_type": "password",
            "client_id": "JBM",
            "client_secret": "demo-secret",
            "username": "admin",
            "password": "admin123",
        },
    ).json()["access_token"]
    client.headers["Authorization"] = "Bearer " + token
    assert client.post("/oauth2/keys/register", json=payload).status_code == 401


@pytest.mark.parametrize("key", ["ed25519", "rsa", "ecdsa"])
def test_ssh_keygen_interoperability_and_login(client, tmp_path, key):
    private_path = tmp_path / "key"
    subprocess.run(
        ["ssh-keygen", "-q", "-t", key, "-N", "", "-f", str(private_path)],
        check=True,
        capture_output=True,
        timeout=30,
    )
    public = private_path.with_suffix(".pub").read_text().strip()

    def sign(challenge):
        message = tmp_path / secrets.token_hex(8)
        message.write_bytes(challenge["message"].encode())
        subprocess.run(
            [
                "ssh-keygen",
                "-Y",
                "sign",
                "-f",
                str(private_path),
                "-n",
                "jbm-key-login",
                str(message),
            ],
            check=True,
            capture_output=True,
            timeout=10,
        )
        signature = message.with_suffix(".sig").read_text()
        verify_ssh_signature(public, challenge["message"], signature)
        with pytest.raises(InvalidSignature):
            verify_ssh_signature(public, challenge["message"] + "tampered", signature)
        return signature

    challenge = client.post(
        "/oauth2/keys/register-options", json={"kind": "SSH_KEY", "publicKey": public}
    ).json()["result"]
    assert (
        client.post(
            "/oauth2/keys/register",
            json={
                "kind": "SSH_KEY",
                "challengeId": challenge["challengeId"],
                "signature": sign(challenge),
            },
        ).status_code
        == 200
    )
    challenge = options(client, "SSH_KEY")
    response = login(
        client,
        {
            "challengeId": challenge["challengeId"],
            "publicKey": public,
            "signature": sign(challenge),
        },
        "SSH_KEY",
    )
    assert response.status_code == 200, response.text
