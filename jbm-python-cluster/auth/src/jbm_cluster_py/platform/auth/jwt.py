from __future__ import annotations

import base64
import json
import time
from typing import Any, Mapping

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa


def b64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")


def b64url_decode(data: str) -> bytes:
    return base64.urlsafe_b64decode(data + "=" * (-len(data) % 4))


def _json_b64(data: Mapping[str, Any]) -> str:
    return b64url_encode(json.dumps(data, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))


class JwtError(ValueError):
    pass


class JwtSigner:
    def __init__(
        self,
        issuer: str,
        audience: str,
        kid: str = "jbm-auth-rs256",
        private_key_pem: str | None = None,
    ) -> None:
        self.issuer = issuer
        self.audience = audience
        self.kid = kid
        self._private_key = self._load_private_key(private_key_pem)
        self._public_key = self._private_key.public_key()

    @staticmethod
    def _load_private_key(private_key_pem: str | None) -> rsa.RSAPrivateKey:
        if private_key_pem:
            normalized = private_key_pem.replace("\\n", "\n").encode("utf-8")
            loaded = serialization.load_pem_private_key(normalized, password=None)
            if not isinstance(loaded, rsa.RSAPrivateKey):
                raise ValueError("jbm.auth.jwt.private-key must be an RSA private key")
            return loaded
        return rsa.generate_private_key(public_exponent=65537, key_size=2048)

    def sign(self, claims: Mapping[str, Any]) -> str:
        header = {"alg": "RS256", "typ": "JWT", "kid": self.kid}
        signing_input = "%s.%s" % (_json_b64(header), _json_b64(claims))
        signature = self._private_key.sign(
            signing_input.encode("ascii"),
            padding.PKCS1v15(),
            hashes.SHA256(),
        )
        return "%s.%s" % (signing_input, b64url_encode(signature))

    def verify(self, token: str) -> dict[str, Any]:
        try:
            header_part, payload_part, signature_part = token.split(".", 2)
            header = json.loads(b64url_decode(header_part))
            claims = json.loads(b64url_decode(payload_part))
        except Exception as exc:
            raise JwtError("invalid_token") from exc
        if header.get("alg") != "RS256":
            raise JwtError("unsupported_alg")
        signing_input = "%s.%s" % (header_part, payload_part)
        try:
            self._public_key.verify(
                b64url_decode(signature_part),
                signing_input.encode("ascii"),
                padding.PKCS1v15(),
                hashes.SHA256(),
            )
        except InvalidSignature as exc:
            raise JwtError("invalid_signature") from exc
        now = int(time.time())
        if int(claims.get("nbf") or 0) > now:
            raise JwtError("token_not_active")
        if int(claims.get("exp") or 0) <= now:
            raise JwtError("token_expired")
        if claims.get("iss") != self.issuer:
            raise JwtError("invalid_issuer")
        aud = claims.get("aud")
        if isinstance(aud, list):
            valid_aud = self.audience in aud
        else:
            valid_aud = aud == self.audience
        if not valid_aud:
            raise JwtError("invalid_audience")
        return dict(claims)

    def jwk(self) -> dict[str, str]:
        numbers = self._public_key.public_numbers()
        return {
            "kty": "RSA",
            "use": "sig",
            "kid": self.kid,
            "alg": "RS256",
            "n": b64url_encode(numbers.n.to_bytes((numbers.n.bit_length() + 7) // 8, "big")),
            "e": b64url_encode(numbers.e.to_bytes((numbers.e.bit_length() + 7) // 8, "big")),
        }
