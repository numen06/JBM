from __future__ import annotations

import hashlib
import hmac
import json
import logging
import secrets
import time
import base64
from typing import Any, Mapping, Optional

import bcrypt
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding

from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.auth.jwt import JwtError, JwtSigner
from jbm_cluster_py.platform.auth.repository import AuthRepository, infer_account_type, user_is_active

logger = logging.getLogger(__name__)

LOGIN_ERROR_PREFIX = "login_error:"


class AuthError(ValueError):
    def __init__(self, message: str, code: int = 401) -> None:
        super().__init__(message)
        self.code = code


class TokenCache:
    def __init__(self, redis_client: RedisClient, prefix: str = "jbm:auth") -> None:
        self.redis_client = redis_client
        self.prefix = prefix.rstrip(":")
        self._memory: dict[str, tuple[dict[str, Any], int]] = {}

    async def start(self) -> None:
        try:
            await self.redis_client.start()
        except Exception as exc:
            logger.warning("Auth Redis startup failed; use in-memory token cache: %s", exc)

    async def stop(self) -> None:
        await self.redis_client.stop()

    async def set_json(self, key: str, value: Mapping[str, Any], ttl_seconds: int) -> None:
        full_key = self._key(key)
        if self.redis_client.client is not None:
            await self.redis_client.client.set(full_key, json.dumps(value, ensure_ascii=False), ex=ttl_seconds)
            return
        self._memory[full_key] = (dict(value), int(time.time()) + ttl_seconds)

    async def get_json(self, key: str) -> Optional[dict[str, Any]]:
        full_key = self._key(key)
        if self.redis_client.client is not None:
            raw = await self.redis_client.client.get(full_key)
            return json.loads(raw) if raw else None
        item = self._memory.get(full_key)
        if not item:
            return None
        value, expires_at = item
        if expires_at <= int(time.time()):
            self._memory.pop(full_key, None)
            return None
        return dict(value)

    async def delete(self, key: str) -> None:
        full_key = self._key(key)
        if self.redis_client.client is not None:
            await self.redis_client.client.delete(full_key)
            return
        self._memory.pop(full_key, None)

    async def list_json(self, prefix: str) -> list[dict[str, Any]]:
        full_prefix = self._key(prefix)
        values: list[dict[str, Any]] = []
        if self.redis_client.client is not None:
            async for key in self.redis_client.client.scan_iter(full_prefix + "*"):
                raw = await self.redis_client.client.get(key)
                if raw:
                    values.append(json.loads(raw))
            return values
        now = int(time.time())
        expired: list[str] = []
        for key, item in self._memory.items():
            if not key.startswith(full_prefix):
                continue
            value, expires_at = item
            if expires_at <= now:
                expired.append(key)
                continue
            values.append(dict(value))
        for key in expired:
            self._memory.pop(key, None)
        return values

    async def login_error_count(self, username: str) -> int:
        key = LOGIN_ERROR_PREFIX + username
        if self.redis_client.client is not None:
            value = await self.redis_client.client.get(key)
            return int(value or 0)
        item = self._memory.get(key)
        if not item:
            return 0
        value, expires_at = item
        if expires_at <= int(time.time()):
            self._memory.pop(key, None)
            return 0
        return int(value.get("count") or 0)

    async def add_login_error(self, username: str, ttl_minutes: int) -> int:
        key = LOGIN_ERROR_PREFIX + username
        ttl_seconds = ttl_minutes * 60
        if self.redis_client.client is not None:
            count = await self.redis_client.client.incr(key)
            await self.redis_client.client.expire(key, ttl_seconds)
            return int(count)
        count = await self.login_error_count(username) + 1
        self._memory[key] = ({"count": count}, int(time.time()) + ttl_seconds)
        return count

    async def clear_login_error(self, username: str) -> None:
        key = LOGIN_ERROR_PREFIX + username
        if self.redis_client.client is not None:
            await self.redis_client.client.delete(key)
            return
        self._memory.pop(key, None)

    def _key(self, key: str) -> str:
        return "%s:%s" % (self.prefix, key)


class AuthService:
    def __init__(
        self,
        repository: AuthRepository,
        cache: TokenCache,
        config: Mapping[str, Any],
    ) -> None:
        self.repository = repository
        self.cache = cache
        self.config = dict(config)
        jwt_config = dict(self.config.get("jwt") or {})
        issuer = str(jwt_config.get("issuer") or "http://localhost:5555")
        audience = str(jwt_config.get("audience") or "jbm-api")
        self.signer = JwtSigner(
            issuer=issuer,
            audience=audience,
            kid=str(jwt_config.get("key-id") or jwt_config.get("kid") or "jbm-auth-rs256"),
            private_key_pem=jwt_config.get("private-key"),
        )
        self.issuer = issuer.rstrip("/")
        self.audience = audience
        self.access_seconds = int(jwt_config.get("access-token-seconds") or 7200)
        self.refresh_seconds = int(jwt_config.get("refresh-token-seconds") or 604800)
        self.account_domain = str(self.config.get("account-domain") or "@admin.com")
        self.max_errors = int(self.config.get("login-error-number") or 5)
        self.lock_minutes = int(self.config.get("login-error-limit-minutes") or 10)

    async def password_token(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client = await self._require_client(form)
        username = str(form.get("username") or "").strip()
        password = str(form.get("password") or "")
        if _truthy(form.get("password_encrypted")) or _looks_like_ciphertext(password):
            password = _decrypt_password(password, str(client.get("privateKey") or ""))
        if not username or not password:
            raise AuthError("用户名或密码不能为空", 400)
        count = await self.cache.login_error_count(username)
        if count >= self.max_errors:
            raise AuthError("密码错误次数过多，帐户锁定%s分钟" % self.lock_minutes, 423)
        account_type = str(form.get("account_type") or infer_account_type(username))
        account = await self.repository.find_account(username, account_type, self.account_domain)
        if not account or not _secret_matches(password, str(account.get("password") or "")):
            await self.cache.add_login_error(username, self.lock_minutes)
            raise AuthError("用户名或密码错误", 401)
        if int(account.get("status") or 0) != 1:
            raise AuthError("帐号已被禁用", 403)
        user = await self.repository.find_user(int(account["user_id"]))
        if not user or not user_is_active(user):
            raise AuthError("用户已被禁用", 403)
        await self.cache.clear_login_error(username)
        return await self._issue_user_token(client, account, user, str(form.get("scope") or "all"))

    async def client_credentials_token(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client = await self._require_client(form)
        scope = str(form.get("scope") or client.get("scopeModules") or "all")
        subject = "client:%s" % client["clientId"]
        now = int(time.time())
        jti = secrets.token_urlsafe(24)
        claims = {
            "iss": self.issuer,
            "aud": self.audience,
            "sub": subject,
            "loginId": subject,
            "client_id": client["clientId"],
            "app_id": client.get("appId"),
            "scope": scope,
            "roles": [],
            "permissions": [],
            "iat": now,
            "nbf": now,
            "exp": now + self.access_seconds,
            "jti": jti,
        }
        return self._token_response(self.signer.sign(claims), None, scope)

    async def refresh_token(self, form: Mapping[str, Any]) -> dict[str, Any]:
        await self._require_client(form)
        refresh_token = str(form.get("refresh_token") or "").strip()
        if not refresh_token:
            raise AuthError("refresh_token不能为空", 400)
        state = await self.cache.get_json("refresh:" + _hash_token(refresh_token))
        if not state:
            raise AuthError("refresh_token无效或已过期", 401)
        user_id = int(state["userId"])
        user = await self.repository.find_user(user_id)
        if not user or not user_is_active(user):
            raise AuthError("用户已被禁用", 403)
        client = await self.repository.find_client(str(state["clientId"]))
        if not client:
            raise AuthError("客户端无效", 401)
        account = {
            "account": state.get("username") or user.get("user_name"),
            "user_id": user_id,
            "must_change_password": state.get("mustChangePassword"),
        }
        return await self._issue_user_token(client, account, user, str(state.get("scope") or "all"))

    async def logout(self, token: str | None = None, refresh_token: str | None = None) -> dict[str, bool]:
        if refresh_token:
            await self.cache.delete("refresh:" + _hash_token(refresh_token))
        if token:
            await self.revoke_access_token(token)
        return {"logout": True}

    async def userinfo(self, token: str) -> dict[str, Any]:
        claims = self.signer.verify(token)
        revoked = await self.cache.get_json("revoked:" + str(claims.get("jti") or ""))
        if revoked:
            raise AuthError("token已失效", 401)
        return {
            "sub": claims.get("sub"),
            "userId": claims.get("user_id"),
            "username": claims.get("username"),
            "name": claims.get("name"),
            "nickname": claims.get("nickname"),
            "avatar": claims.get("avatar"),
            "email": claims.get("email"),
            "mobile": claims.get("mobile"),
            "clientId": claims.get("client_id"),
            "appId": claims.get("app_id"),
            "roles": claims.get("roles") or [],
            "permissions": claims.get("permissions") or [],
            "scope": claims.get("scope"),
        }

    async def online_users(self, search: Mapping[str, Any]) -> dict[str, Any]:
        page_form = dict(search.get("pageForm") or search.get("page_form") or {})
        curr_page = max(int(page_form.get("currPage") or page_form.get("curr_page") or 1), 1)
        page_size = max(int(page_form.get("pageSize") or page_form.get("page_size") or 10), 1)
        rows = await self.cache.list_json("access:")
        rows = [row for row in rows if not await self._is_revoked(row)]
        rows = self._filter_online_rows(rows, search)
        rows.sort(key=lambda row: str(row.get("loginTime") or ""), reverse=True)
        total = len(rows)
        start = (curr_page - 1) * page_size
        return {
            "contents": rows[start : start + page_size],
            "total": total,
            "pageForm": {"currPage": curr_page, "pageSize": page_size},
            "currPage": curr_page,
            "pageSize": page_size,
        }

    async def revoke_access_token(self, token: str) -> None:
        try:
            claims = self.signer.verify(token)
        except JwtError:
            await self.cache.delete("access:" + _hash_token(token))
            return
        ttl = int(claims.get("exp") or 0) - int(time.time())
        if ttl > 0 and claims.get("jti"):
            await self.cache.set_json("revoked:" + str(claims["jti"]), {"revoked": True}, ttl)
        await self.cache.delete("access:" + _hash_token(token))

    def verify_permissions(self, token: str, *permissions: str) -> None:
        if not token:
            raise AuthError("未登录", 401)
        claims = self.signer.verify(token)
        token_permissions = {str(item) for item in claims.get("permissions") or [] if item}
        if not permissions:
            return
        expanded = set(token_permissions)
        for permission in list(token_permissions):
            if permission.startswith("ACTION_"):
                expanded.add(permission.replace("ACTION_", "", 1))
            else:
                expanded.add("ACTION_" + permission)
        if "admin" == str(claims.get("username") or "").lower() or expanded.intersection(permissions):
            return
        raise AuthError("无权限访问", 403)

    def openid_configuration(self) -> dict[str, Any]:
        issuer = self.issuer
        return {
            "issuer": issuer,
            "jwks_uri": issuer + "/jwks.json",
            "token_endpoint": issuer + "/oauth2/token",
            "userinfo_endpoint": issuer + "/oauth2/userinfo",
            "end_session_endpoint": issuer + "/oauth2/logout",
            "grant_types_supported": ["password", "client_credentials", "refresh_token"],
            "token_endpoint_auth_methods_supported": ["client_secret_post", "client_secret_basic"],
            "response_types_supported": ["token"],
            "subject_types_supported": ["public"],
            "id_token_signing_alg_values_supported": ["RS256"],
        }

    def jwks(self) -> dict[str, Any]:
        return {"keys": [self.signer.jwk()]}

    async def public_key(self, client_id: str) -> Optional[str]:
        client = await self.repository.find_client(client_id)
        return str(client.get("publicKey") or "") if client else None

    async def _require_client(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        client_secret = str(form.get("client_secret") or form.get("clientSecret") or "")
        client = await self.repository.find_client(client_id)
        if not client or not _secret_matches(client_secret, str(client.get("clientSecret") or "")):
            raise AuthError("客户端认证失败", 401)
        return client

    async def _issue_user_token(
        self,
        client: Mapping[str, Any],
        account: Mapping[str, Any],
        user: Mapping[str, Any],
        scope: str,
    ) -> dict[str, Any]:
        user_id = int(user["user_id"])
        app_id = int(client.get("appId") or 0)
        username = str(account.get("account") or user.get("user_name") or user_id)
        roles = await self.repository.user_roles(user_id)
        role_codes = sorted({str(row["role_code"]) for row in roles if row.get("role_code")})
        role_ids = {int(row["role_id"]) for row in roles if row.get("role_id") is not None}
        root = username == "admin" or 1 in role_ids
        permissions = await self.repository.user_authorities(user_id, root)
        now = int(time.time())
        refresh_token = secrets.token_urlsafe(36)
        jti = secrets.token_urlsafe(24)
        login_id = "%s:%s:%s" % (user.get("user_type") or "normal", app_id, user_id)
        claims = {
            "iss": self.issuer,
            "aud": self.audience,
            "sub": login_id,
            "loginId": login_id,
            "user_id": user_id,
            "username": username,
            "name": user.get("real_name") or user.get("nick_name") or username,
            "nickname": user.get("nick_name"),
            "avatar": user.get("avatar"),
            "email": user.get("email"),
            "mobile": user.get("mobile"),
            "tenant_id": user.get("company_id"),
            "department_id": user.get("department_id"),
            "client_id": client.get("clientId"),
            "app_id": app_id,
            "user_type": user.get("user_type"),
            "roles": role_codes,
            "permissions": permissions,
            "scope": scope,
            "iat": now,
            "nbf": now,
            "exp": now + self.access_seconds,
            "jti": jti,
        }
        await self.cache.set_json(
            "refresh:" + _hash_token(refresh_token),
            {
                "userId": user_id,
                "username": username,
                "clientId": client.get("clientId"),
                "scope": scope,
                "mustChangePassword": bool(account.get("must_change_password")),
            },
            self.refresh_seconds,
        )
        access_token = self.signer.sign(claims)
        await self._record_online_session(access_token, claims, user, client)
        return self._token_response(
            access_token,
            refresh_token,
            scope,
            {
                "must_change_password": bool(account.get("must_change_password")),
                "login_id": login_id,
                "user_id": user_id,
                "roles": role_codes,
                "permissions": permissions,
            },
        )

    def _token_response(
        self,
        access_token: str,
        refresh_token: str | None,
        scope: str,
        extra: Mapping[str, Any] | None = None,
    ) -> dict[str, Any]:
        body: dict[str, Any] = {
            "access_token": access_token,
            "token_type": "Bearer",
            "expires_in": self.access_seconds,
            "scope": scope,
        }
        if refresh_token:
            body["refresh_token"] = refresh_token
        body.update(dict(extra or {}))
        return body

    async def _record_online_session(
        self,
        access_token: str,
        claims: Mapping[str, Any],
        user: Mapping[str, Any],
        client: Mapping[str, Any],
    ) -> None:
        now = int(time.time())
        expires_at = int(claims.get("exp") or now)
        ttl = expires_at - now
        if ttl <= 0:
            return
        row = {
            "tokenId": access_token,
            "userId": claims.get("user_id"),
            "deptId": claims.get("department_id"),
            "departmentId": claims.get("department_id"),
            "companyId": claims.get("tenant_id"),
            "appId": claims.get("app_id"),
            "appName": client.get("clientId"),
            "userName": claims.get("username"),
            "loginLocation": "",
            "browser": "",
            "os": "",
            "loginTime": _timestamp(now),
            "expiredTime": _timestamp(expires_at),
            "activityTime": _timestamp(now),
            "jti": claims.get("jti"),
        }
        await self.cache.set_json("access:" + _hash_token(access_token), row, ttl)

    async def _is_revoked(self, row: Mapping[str, Any]) -> bool:
        jti = str(row.get("jti") or "")
        return bool(jti and await self.cache.get_json("revoked:" + jti))

    @staticmethod
    def _filter_online_rows(rows: list[dict[str, Any]], search: Mapping[str, Any]) -> list[dict[str, Any]]:
        user_name = str(search.get("userName") or search.get("user_name") or "").strip().lower()
        ipaddr = str(search.get("ipaddr") or "").strip()
        app_id = str(search.get("appId") or search.get("app_id") or "").strip()
        company_id = str(search.get("companyId") or search.get("company_id") or "").strip()
        filtered: list[dict[str, Any]] = []
        for row in rows:
            if user_name and user_name not in str(row.get("userName") or "").lower():
                continue
            if ipaddr and ipaddr not in str(row.get("ipaddr") or ""):
                continue
            if app_id and app_id != str(row.get("appId") or ""):
                continue
            if company_id and company_id != str(row.get("companyId") or ""):
                continue
            filtered.append(row)
        return filtered


def _secret_matches(raw: str, stored: str) -> bool:
    if not stored:
        return raw == stored
    if stored.startswith(("$2a$", "$2b$", "$2y$")):
        try:
            return bcrypt.checkpw(raw.encode("utf-8"), stored.encode("utf-8"))
        except ValueError:
            return False
    return hmac.compare_digest(raw, stored)


def _hash_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def _timestamp(seconds: int) -> str:
    return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(seconds))


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    return str(value or "").strip().lower() in {"1", "true", "yes", "y"}


def _looks_like_ciphertext(value: str) -> bool:
    text_value = str(value or "").strip()
    if len(text_value) < 128:
        return False
    try:
        base64.b64decode(text_value, validate=True)
        return True
    except Exception:
        return False


def _decrypt_password(ciphertext: str, private_key_base64: str) -> str:
    if not private_key_base64:
        raise AuthError("客户端未配置登录私钥", 400)
    try:
        private_key = serialization.load_der_private_key(
            base64.b64decode(private_key_base64),
            password=None,
        )
        decrypted = private_key.decrypt(
            base64.b64decode(ciphertext),
            padding.PKCS1v15(),
        )
        return decrypted.decode("utf-8")
    except Exception as exc:
        raise AuthError("处理登录信息异常", 400) from exc
