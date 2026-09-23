from __future__ import annotations

import base64
from collections.abc import Mapping
from typing import Any

import jwt as pyjwt
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa


def b64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")


def b64url_decode(data: str) -> bytes:
    return base64.urlsafe_b64decode(data + "=" * (-len(data) % 4))


class JwtError(ValueError):
    pass


class JwtSigner:
    """One active RS256 signing key and explicitly configured verification-only keys.

    Retire old public keys after their tokens expire, or immediately for a
    compromised key. Algorithms and key locations never come from token headers.
    Production startup requires a private key; generation is for development.
    """

    def __init__(
        self,
        issuer: str,
        audience: str,
        kid: str = "jbm-auth-rs256",
        private_key_pem: str | None = None,
        verification_keys: Mapping[str, str] | None = None,
    ) -> None:
        self.issuer = issuer
        self.audience = audience
        self.kid = str(kid).strip()
        if not self.kid:
            raise ValueError("JWT signing key ID cannot be empty")
        self._private_key = self._load_private_key(private_key_pem)
        self._public_key = self._private_key.public_key()
        self._verification_keys: dict[str, rsa.RSAPublicKey] = {self.kid: self._public_key}
        for key_id, pem in (verification_keys or {}).items():
            if not isinstance(key_id, str) or not key_id.strip() or key_id != key_id.strip():
                raise ValueError("JWT verification key ID must be a nonempty string")
            if key_id == self.kid:
                raise ValueError("Verification-only key ID conflicts with active signing key")
            key = serialization.load_pem_public_key(pem.replace("\\n", "\n").encode())
            if not isinstance(key, rsa.RSAPublicKey) or key.key_size < 2048:
                raise ValueError(
                    "JWT verification keys must be RSA public keys of at least 2048 bits"
                )
            self._verification_keys[key_id] = key

    @staticmethod
    def _load_private_key(private_key_pem: str | None) -> rsa.RSAPrivateKey:
        if private_key_pem:
            normalized = private_key_pem.replace("\\n", "\n").encode("utf-8")
            loaded = serialization.load_pem_private_key(normalized, password=None)
            if not isinstance(loaded, rsa.RSAPrivateKey) or loaded.key_size < 2048:
                raise ValueError(
                    "jbm.auth.jwt.private-key must be an RSA key of at least 2048 bits"
                )
            return loaded
        return rsa.generate_private_key(public_exponent=65537, key_size=2048)

    def for_issuer(self, issuer: str, audience: str) -> JwtSigner:
        """Share the trusted key ring across distinct protocol profiles."""
        signer = object.__new__(JwtSigner)
        signer.issuer = issuer
        signer.audience = audience
        signer.kid = self.kid
        signer._private_key = self._private_key
        signer._public_key = self._public_key
        signer._verification_keys = dict(self._verification_keys)
        return signer

    def sign(self, claims: Mapping[str, Any], *, typ: str = "JWT") -> str:
        return pyjwt.encode(
            dict(claims),
            self._private_key,
            algorithm="RS256",
            headers={"typ": typ, "kid": self.kid},
        )

    def verify(
        self,
        token: str,
        *,
        audience: str | list[str] | None = None,
        typ: str | None = None,
        verify_exp: bool = True,
    ) -> dict[str, Any]:
        try:
            header = pyjwt.get_unverified_header(token)
            if header.get("alg") != "RS256":
                raise JwtError("unsupported_alg")
            if header.get("crit") or header.get("b64") is False:
                raise JwtError("unsupported_header")
            if typ is not None and header.get("typ") != typ:
                raise JwtError("invalid_token_type")
            key_id = header.get("kid")
            if not isinstance(key_id, str) or key_id not in self._verification_keys:
                raise JwtError("unknown_signing_key")
            return dict(
                pyjwt.decode(
                    token,
                    self._verification_keys[key_id],
                    algorithms=["RS256"],
                    issuer=self.issuer,
                    audience=self.audience if audience is None else audience,
                    options={"require": ["iss", "aud", "exp"], "verify_exp": verify_exp},
                )
            )
        except JwtError:
            raise
        except pyjwt.ExpiredSignatureError as exc:
            raise JwtError("token_expired") from exc
        except pyjwt.ImmatureSignatureError as exc:
            raise JwtError("token_not_active") from exc
        except pyjwt.InvalidIssuerError as exc:
            raise JwtError("invalid_issuer") from exc
        except pyjwt.InvalidAudienceError as exc:
            raise JwtError("invalid_audience") from exc
        except (pyjwt.PyJWTError, ValueError, TypeError, OverflowError) as exc:
            raise JwtError("invalid_token") from exc

    @staticmethod
    def _jwk(key_id: str, key: rsa.RSAPublicKey) -> dict[str, str]:
        numbers = key.public_numbers()
        return {
            "kty": "RSA",
            "use": "sig",
            "kid": key_id,
            "alg": "RS256",
            "n": b64url_encode(numbers.n.to_bytes((numbers.n.bit_length() + 7) // 8, "big")),
            "e": b64url_encode(numbers.e.to_bytes((numbers.e.bit_length() + 7) // 8, "big")),
        }

    def jwk(self) -> dict[str, str]:
        return self._jwk(self.kid, self._public_key)

    def jwks(self) -> dict[str, Any]:
        return {"keys": [self._jwk(kid, key) for kid, key in self._verification_keys.items()]}
