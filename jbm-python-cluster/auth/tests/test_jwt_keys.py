from __future__ import annotations

import json
import time

import jwt
import pytest
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from jbm_cluster_py.platform.auth.jwt import JwtError, JwtSigner


def pem_pair() -> tuple[str, str]:
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    return (
        key.private_bytes(
            serialization.Encoding.PEM,
            serialization.PrivateFormat.PKCS8,
            serialization.NoEncryption(),
        ).decode(),
        key.public_key()
        .public_bytes(
            serialization.Encoding.PEM,
            serialization.PublicFormat.SubjectPublicKeyInfo,
        )
        .decode(),
    )


def claims(**changes):
    now = int(time.time())
    return {
        "iss": "https://auth.example",
        "aud": "jbm-api",
        "sub": "user:1",
        "iat": now,
        "nbf": now,
        "exp": now + 300,
        "jti": "token-1",
        **changes,
    }


def test_rotation_verifies_old_tokens_until_key_retired():
    old_private, old_public = pem_pair()
    new_private, _ = pem_pair()
    old = JwtSigner("https://auth.example", "jbm-api", "old", old_private)
    token = old.sign(claims())
    rotated = JwtSigner(
        "https://auth.example",
        "jbm-api",
        "new",
        new_private,
        verification_keys={"old": old_public},
    )
    assert rotated.verify(token)["sub"] == "user:1"
    assert jwt.get_unverified_header(rotated.sign(claims()))["kid"] == "new"
    keys = rotated.jwks()["keys"]
    assert {key["kid"] for key in keys} == {"old", "new"}
    assert all("d" not in key and "p" not in key and "q" not in key for key in keys)
    retired = JwtSigner("https://auth.example", "jbm-api", "new", new_private)
    with pytest.raises(JwtError, match="unknown_signing_key"):
        retired.verify(token)


def test_distinct_issuer_audience_and_type_are_enforced():
    legacy = JwtSigner("https://auth.example", "jbm-api")
    standard = legacy.for_issuer("https://auth.example/oidc", "iot-api")
    token = standard.sign(
        claims(iss=standard.issuer, aud="iot-api"),
        typ="at+jwt",
    )
    assert standard.verify(token, typ="at+jwt")["aud"] == "iot-api"
    assert standard.jwks() == legacy.jwks()
    with pytest.raises(JwtError, match="invalid_issuer"):
        legacy.verify(token)
    with pytest.raises(JwtError, match="invalid_audience"):
        standard.verify(token, audience="building-api", typ="at+jwt")
    identity = standard.sign(claims(iss=standard.issuer, aud="iot-api"))
    with pytest.raises(JwtError, match="invalid_token_type"):
        standard.verify(identity, typ="at+jwt")


@pytest.mark.parametrize(
    "changes",
    [
        {"exp": 1},
        {"nbf": 9999999999},
        {"iat": 9999999999},
        {"iss": "https://evil.example"},
        {"aud": "other"},
        {"exp": "bad"},
        {"aud": None},
        {"iss": None},
        {"exp": None},
        {"sub": 123},
    ],
)
def test_invalid_claims_fail_closed(changes):
    signer = JwtSigner("https://auth.example", "jbm-api")
    token = jwt.api_jws.encode(
        json.dumps(claims(**changes)).encode(),
        signer._private_key,
        algorithm="RS256",
        headers={"kid": signer.kid},
    )
    with pytest.raises(JwtError):
        signer.verify(token)


def test_expired_logout_hint_still_requires_authentic_signature_and_issuer():
    signer = JwtSigner("https://auth.example", "jbm-api")
    token = signer.sign(claims(exp=1))
    assert signer.verify(token, verify_exp=False)["sub"] == "user:1"
    rogue = JwtSigner("https://auth.example", "jbm-api")
    with pytest.raises(JwtError):
        signer.verify(rogue.sign(claims(exp=1)), verify_exp=False)


@pytest.mark.parametrize("token", ["", "not-a-token", "e30.e30.AA"])
def test_malformed_tokens_are_domain_errors(token):
    with pytest.raises(JwtError):
        JwtSigner("https://auth.example", "jbm-api").verify(token)


def test_symmetric_algorithm_confusion_is_rejected():
    signer = JwtSigner("https://auth.example", "jbm-api")
    token = jwt.encode(claims(), "a" * 32, algorithm="HS256", headers={"kid": signer.kid})
    with pytest.raises(JwtError, match="unsupported_alg"):
        signer.verify(token)


def test_verification_key_cannot_replace_active_key():
    private, public = pem_pair()
    with pytest.raises(ValueError, match="conflicts"):
        JwtSigner("https://auth.example", "jbm-api", "same", private, {"same": public})
