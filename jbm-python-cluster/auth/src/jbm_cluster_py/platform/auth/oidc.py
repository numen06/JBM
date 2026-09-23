"""Opt-in, deliberately bounded OIDC authorization-code provider.

Only registered clients, S256 PKCE, password authentication and normal JSON
claims are supported. This is not an OpenID certification claim. Storage uses
the same fail-closed distributed cache as legacy auth, with separate keys.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import secrets
import time
from collections.abc import Mapping
from typing import Any
from urllib.parse import unquote_plus, urlencode, urlsplit

from jbm_cluster_py.platform.auth.jwt import JwtError
from jbm_cluster_py.platform.auth.service import _secret_matches

STANDARD_SCOPES = {"openid", "profile", "email", "offline_access"}


class OidcError(ValueError):
    def __init__(self, error: str, description: str = "", status: int = 400):
        super().__init__(description or error)
        self.error, self.description, self.status = error, description or error, status


def digest(value: str) -> str:
    return hashlib.sha256(value.encode()).hexdigest()


def account_version(account: Mapping[str, Any]) -> str:
    return digest(str(account.get("password")) + ":" + str(account.get("update_time")))


class OidcService:
    def __init__(self, auth: Any, config: Mapping[str, Any]):
        self.auth, self.repo, self.cache = auth, auth.repository, auth.cache
        self.config = dict(config.get("oidc") or {})
        self.issuer = str(self.config.get("issuer") or "").rstrip("/")
        parsed = urlsplit(self.issuer)
        if (
            parsed.scheme != "https"
            or not parsed.netloc
            or parsed.path != "/oidc"
            or parsed.query
            or parsed.fragment
            or parsed.username
        ):
            raise ValueError("jbm.auth.oidc.issuer must be an explicit https://host/oidc URL")
        # Reuse configured signing and rotation keys but preserve a separate issuer.
        self.signer = auth.signer.for_issuer(self.issuer, "oidc-api")
        self.access_seconds = max(60, min(int(self.config.get("access-token-seconds", 600)), 3600))
        self.refresh_seconds = max(60, int(self.config.get("refresh-token-seconds", 604800)))
        self.sso_seconds = max(60, int(self.config.get("sso-session-seconds", 28800)))
        self.revocation_seconds = max(self.access_seconds, self.refresh_seconds)

    def metadata(self) -> dict[str, Any]:
        return {
            "issuer": self.issuer,
            "authorization_endpoint": self.issuer + "/authorize",
            "token_endpoint": self.issuer + "/token",
            "userinfo_endpoint": self.issuer + "/userinfo",
            "jwks_uri": self.issuer + "/jwks",
            "revocation_endpoint": self.issuer + "/revoke",
            "introspection_endpoint": self.issuer + "/introspect",
            "end_session_endpoint": self.issuer + "/logout",
            "response_types_supported": ["code"],
            "response_modes_supported": ["query"],
            "authorization_response_iss_parameter_supported": True,
            "grant_types_supported": ["authorization_code", "refresh_token"],
            "subject_types_supported": ["public"],
            "id_token_signing_alg_values_supported": ["RS256"],
            "token_endpoint_auth_methods_supported": [
                "none",
                "client_secret_basic",
                "client_secret_post",
            ],
            "code_challenge_methods_supported": ["S256"],
            "scopes_supported": sorted(STANDARD_SCOPES),
            "claims_supported": [
                "sub",
                "iss",
                "aud",
                "exp",
                "iat",
                "nonce",
                "auth_time",
                "amr",
                "sid",
                "at_hash",
                "name",
                "preferred_username",
                "picture",
                "email",
            ],
            "claims_parameter_supported": False,
            "request_parameter_supported": False,
            "request_uri_parameter_supported": False,
        }

    async def client(self, client_id: str) -> dict[str, Any]:
        client = await self.repo.find_client(client_id)
        if not client or not (client.get("oidc") or {}).get("enabled"):
            raise OidcError("invalid_client", status=401)
        return client

    async def authenticate_client(self, form: dict[str, str], authorization: str) -> dict[str, Any]:
        client_id, secret = form.get("client_id", ""), form.get("client_secret", "")
        method = "client_secret_post" if "client_secret" in form else "none"
        if authorization:
            if not authorization.lower().startswith("basic ") or "client_secret" in form:
                raise OidcError("invalid_client", status=401)
            try:
                basic_id, basic_secret = (
                    base64.b64decode(authorization[6:], validate=True).decode().split(":", 1)
                )
                basic_id, secret = unquote_plus(basic_id), unquote_plus(basic_secret)
            except (ValueError, UnicodeError) as exc:
                raise OidcError("invalid_client", status=401) from exc
            if client_id and client_id != basic_id:
                raise OidcError("invalid_client", status=401)
            client_id, method = basic_id, "client_secret_basic"
        client = await self.client(client_id)
        if client["oidc"]["tokenEndpointAuthMethod"] != method:
            raise OidcError("invalid_client", status=401)
        if method != "none" and (
            not secret
            or not _secret_matches(
                secret, str(client.get("clientSecret") or ""), self.auth.allow_plaintext_secrets
            )
        ):
            raise OidcError("invalid_client", status=401)
        return client

    def redirect(self, request: Mapping[str, Any], **values: str) -> str:
        fields = dict(values)
        if "state" in request:
            fields["state"] = request["state"]
        fields["iss"] = self.issuer
        uri = request["redirect_uri"]
        return uri + ("&" if "?" in uri else "?") + urlencode(fields)

    def validate_request(self, p: dict[str, str], client: dict[str, Any]) -> dict[str, Any]:
        import re

        allowed = {
            "client_id",
            "redirect_uri",
            "response_type",
            "response_mode",
            "scope",
            "state",
            "nonce",
            "code_challenge",
            "code_challenge_method",
            "prompt",
            "max_age",
        }
        if set(p) - allowed:
            raise OidcError("invalid_request", "Unsupported authorization parameter")
        if p.get("response_type") != "code":
            raise OidcError("unsupported_response_type")
        if p.get("response_mode", "query") != "query":
            raise OidcError("invalid_request", "Only query response mode is supported")
        if "authorization_code" not in client["oidc"]["grantTypes"]:
            raise OidcError("unauthorized_client")
        scopes = set(p.get("scope", "").split())
        if (
            "openid" not in scopes
            or scopes - STANDARD_SCOPES
            or scopes - set(client["oidc"]["scopes"])
        ):
            raise OidcError("invalid_scope")
        if "offline_access" in scopes and "refresh_token" not in client["oidc"]["grantTypes"]:
            raise OidcError("invalid_scope", "Offline access is disabled for this client")
        if p.get("code_challenge_method") != "S256" or not re.fullmatch(
            r"[A-Za-z0-9_-]{43}", p.get("code_challenge", "")
        ):
            raise OidcError("invalid_request", "S256 PKCE is required")
        prompts = set(p.get("prompt", "").split())
        if prompts - {"none", "login", "consent"} or ("none" in prompts and len(prompts) > 1):
            raise OidcError("invalid_request", "Unsupported prompt")
        max_age = None
        if "max_age" in p:
            if not re.fullmatch(r"[0-9]{1,10}", p["max_age"]):
                raise OidcError("invalid_request", "Invalid max_age")
            max_age = int(p["max_age"])
        return {**p, "scopes": sorted(scopes), "prompts": sorted(prompts), "max_age_value": max_age}

    async def identity(self, state: Mapping[str, Any], client: dict[str, Any]) -> dict[str, Any]:
        account = await self.repo.find_account(
            state["account"], state["account_type"], state["domain"]
        )
        if (
            not account
            or int(account.get("status") or 0) != 1
            or int(account["user_id"]) != state["user_id"]
            or account_version(account) != state["account_version"]
            or account.get("must_change_password")
        ):
            raise OidcError("invalid_grant", "Account is no longer eligible")
        context = await self.repo.oidc_user_context(state["user_id"], client)
        if not context:
            raise OidcError("access_denied", "Application membership is no longer eligible")
        return context

    async def session(self, sid: str) -> dict[str, Any] | None:
        return await self.cache.get_json("oidc:sso:" + digest(sid)) if sid else None

    async def create_session(
        self, account: Mapping[str, Any], user: Mapping[str, Any]
    ) -> tuple[str, dict[str, Any]]:
        sid = secrets.token_urlsafe(32)
        state = {
            "user_id": int(user["user_id"]),
            "account": account["account"],
            "account_type": account["account_type"],
            "domain": account["domain"],
            "account_version": account_version(account),
            "auth_time": int(time.time()),
            "sub": await self.repo.oidc_subject(int(user["user_id"])),
        }
        await self.cache.set_json(
            "oidc:sso:" + digest(sid), state, max(self.sso_seconds, self.refresh_seconds)
        )
        return sid, state

    async def consent_needed(self, sid: str, p: Mapping[str, Any], client: dict[str, Any]) -> bool:
        prior = await self.cache.get_json("oidc:consent:" + digest(sid + ":" + client["clientId"]))
        approved = set((prior or {}).get("scopes", []))
        if "consent" in p["prompts"]:
            return True
        missing = set(p["scopes"]) - approved
        return bool(missing and (not client["oidc"].get("trusted") or "offline_access" in missing))

    async def approve(self, sid: str, p: Mapping[str, Any]) -> None:
        key = "oidc:consent:" + digest(sid + ":" + p["client_id"])
        async with self.cache.lock(key):
            prior = await self.cache.get_json(key) or {}
            await self.cache.set_json(
                key,
                {"scopes": sorted(set(prior.get("scopes", [])) | set(p["scopes"]))},
                self.refresh_seconds,
            )

    async def code(self, sid: str, p: Mapping[str, Any], client: dict[str, Any]) -> str:
        state = await self.session(sid)
        if not state:
            raise OidcError("login_required")
        await self.identity(state, client)
        code = secrets.token_urlsafe(32)
        await self.cache.set_json(
            "oidc:code:" + digest(code), {"sid": sid, "request": dict(p), "used": False}, 120
        )
        return self.redirect(p, code=code)

    async def tokens(
        self,
        client: dict[str, Any],
        sid: str,
        p: Mapping[str, Any],
        family: str,
    ) -> dict[str, Any]:
        state = await self.session(sid)
        if not state:
            raise OidcError("invalid_grant")
        context = await self.identity(state, client)
        if set(p["scopes"]) - set(client["oidc"]["scopes"]):
            raise OidcError("invalid_scope")
        now = int(time.time())
        common = {
            "iss": self.issuer,
            "sub": state["sub"],
            "iat": now,
            "exp": now + self.access_seconds,
            "sid": digest(sid),
        }
        claims = {
            **common,
            "aud": list(client["oidc"]["audiences"]),
            "jti": secrets.token_urlsafe(24),
            "client_id": client["clientId"],
            "scope": " ".join(p["scopes"]),
            "subject_type": "user",
            "user_id": state["user_id"],
            "app_id": context["appId"],
            "tenant_id": context["tenantId"],
        }
        access = self.signer.sign(claims, typ="at+jwt")
        await self.cache.set_json(
            "oidc:access:" + digest(access),
            {"claims": claims, "sid": sid, "family": family},
            self.access_seconds,
        )
        identity_claims = {
            **common,
            "aud": client["clientId"],
            "auth_time": state["auth_time"],
            "amr": ["pwd"],
            "at_hash": base64.urlsafe_b64encode(hashlib.sha256(access.encode()).digest()[:16])
            .decode()
            .rstrip("="),
        }
        if "nonce" in p:
            identity_claims["nonce"] = p["nonce"]
        result = {
            "access_token": access,
            "token_type": "Bearer",
            "expires_in": self.access_seconds,
            "scope": " ".join(p["scopes"]),
            "id_token": self.signer.sign(identity_claims),
        }
        if "offline_access" in p["scopes"]:
            token = secrets.token_urlsafe(48)
            await self.cache.set_json(
                "oidc:refresh:" + digest(token),
                {
                    "sid": sid,
                    "family": family,
                    "client_id": client["clientId"],
                    "request": dict(p),
                    "used": False,
                    "expires_at": now + self.refresh_seconds,
                },
                self.refresh_seconds,
            )
            result["refresh_token"] = token
        return result

    async def exchange(self, form: dict[str, str], client: dict[str, Any]) -> dict[str, Any]:
        import re

        if form.get("grant_type") == "authorization_code":
            if "authorization_code" not in client["oidc"]["grantTypes"]:
                raise OidcError("unauthorized_client")
            key = "oidc:code:" + digest(form.get("code", ""))
            family = digest("code:" + form.get("code", ""))
            async with self.cache.lock("oidc:family:" + family):
                grant = await self.cache.get_json(key)
                if not grant or grant["request"]["client_id"] != client["clientId"]:
                    raise OidcError("invalid_grant")
                p = grant["request"]
                if form.get("redirect_uri") != p["redirect_uri"]:
                    raise OidcError("invalid_grant")
                verifier = form.get("code_verifier", "")
                actual = (
                    base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest())
                    .decode()
                    .rstrip("=")
                )
                if not re.fullmatch(
                    r"[A-Za-z0-9._~-]{43,128}", verifier
                ) or not hmac.compare_digest(actual, p["code_challenge"]):
                    raise OidcError("invalid_grant")
                if grant["used"]:
                    await self.cache.set_json(
                        "oidc:revoked:" + family, {"revoked": True}, self.revocation_seconds
                    )
                    raise OidcError("invalid_grant")
                if p["redirect_uri"] not in client["oidc"]["redirectUris"]:
                    raise OidcError("invalid_grant")
                grant["used"] = True
                await self.cache.set_json(key, grant, self.revocation_seconds)
                return await self.tokens(client, grant["sid"], p, family)
        if form.get("grant_type") == "refresh_token":
            if "refresh_token" not in client["oidc"]["grantTypes"]:
                raise OidcError("unauthorized_client")
            key = "oidc:refresh:" + digest(form.get("refresh_token", ""))
            grant = await self.cache.get_json(key)
            if not grant or grant["client_id"] != client["clientId"]:
                raise OidcError("invalid_grant")
            family = grant["family"]
            async with self.cache.lock("oidc:family:" + family):
                grant = await self.cache.get_json(key)
                if not grant or await self.cache.get_json("oidc:revoked:" + family):
                    raise OidcError("invalid_grant")
                if grant["used"]:
                    await self.cache.set_json(
                        "oidc:revoked:" + family, {"revoked": True}, self.revocation_seconds
                    )
                    raise OidcError("invalid_grant")
                p = dict(grant["request"])
                if "scope" in form:
                    scopes = set(form["scope"].split())
                    if "openid" not in scopes or scopes - set(p["scopes"]):
                        raise OidcError("invalid_scope")
                    p["scopes"] = sorted(scopes)
                # Validate before consumption; invalid client auth never reaches this method.
                state = await self.session(grant["sid"])
                if not state:
                    raise OidcError("invalid_grant")
                await self.identity(state, client)
                grant["used"] = True
                await self.cache.set_json(key, grant, self.refresh_seconds)
                return await self.tokens(client, grant["sid"], p, family)
        raise OidcError("unsupported_grant_type")

    async def active_access(self, token: str) -> tuple[dict[str, Any], dict[str, Any]]:
        record = await self.cache.get_json("oidc:access:" + digest(token))
        if not record or await self.cache.get_json("oidc:revoked:" + record["family"]):
            raise OidcError("invalid_token", status=401)
        try:
            client = await self.client(record["claims"]["client_id"])
        except OidcError as exc:
            raise OidcError("invalid_token", status=401) from exc
        try:
            claims = self.signer.verify(token, audience=client["oidc"]["audiences"], typ="at+jwt")
        except JwtError as exc:
            raise OidcError("invalid_token", status=401) from exc
        state = await self.session(record["sid"])
        if not state:
            raise OidcError("invalid_token", status=401)
        try:
            context = await self.identity(state, client)
        except OidcError as exc:
            raise OidcError("invalid_token", status=401) from exc
        if (
            claims["tenant_id"] != context["tenantId"]
            or claims["app_id"] != context["appId"]
            or set(claims["scope"].split()) - set(client["oidc"]["scopes"])
            or set(claims["aud"]) - set(client["oidc"]["audiences"])
        ):
            raise OidcError("invalid_token", status=401)
        return claims, context

    async def userinfo(self, token: str) -> dict[str, Any]:
        claims, context = await self.active_access(token)
        result = {"sub": claims["sub"]}
        user, scopes = context["user"], set(claims["scope"].split())
        if "profile" in scopes:
            for claim, field in (
                ("name", "real_name"),
                ("preferred_username", "user_name"),
                ("picture", "avatar"),
            ):
                if user.get(field):
                    result[claim] = user[field]
        if "email" in scopes and user.get("email"):
            result["email"] = user["email"]
        return result

    async def revoke(self, token: str, client: dict[str, Any]) -> None:
        grant = await self.cache.get_json("oidc:refresh:" + digest(token))
        if grant and grant["client_id"] == client["clientId"]:
            async with self.cache.lock("oidc:family:" + grant["family"]):
                await self.cache.set_json(
                    "oidc:revoked:" + grant["family"], {"revoked": True}, self.revocation_seconds
                )
        record = await self.cache.get_json("oidc:access:" + digest(token))
        if record and record["claims"]["client_id"] == client["clientId"]:
            await self.cache.delete("oidc:access:" + digest(token))

    async def introspect(self, token: str, client: dict[str, Any]) -> dict[str, Any]:
        try:
            grant = await self.cache.get_json("oidc:refresh:" + digest(token))
            if grant:
                if (
                    grant["client_id"] != client["clientId"]
                    or grant["used"]
                    or "refresh_token" not in client["oidc"]["grantTypes"]
                    or await self.cache.get_json("oidc:revoked:" + grant["family"])
                ):
                    return {"active": False}
                state = await self.session(grant["sid"])
                if not state:
                    return {"active": False}
                await self.identity(state, client)
                if set(grant["request"]["scopes"]) - set(client["oidc"]["scopes"]):
                    return {"active": False}
                return {
                    "active": True,
                    "client_id": client["clientId"],
                    "sub": state["sub"],
                    "scope": " ".join(grant["request"]["scopes"]),
                    "exp": grant["expires_at"],
                }
            claims, _ = await self.active_access(token)
            if claims["client_id"] != client["clientId"]:
                return {"active": False}
            return {**claims, "active": True, "token_type": "Bearer"}
        except OidcError:
            return {"active": False}
