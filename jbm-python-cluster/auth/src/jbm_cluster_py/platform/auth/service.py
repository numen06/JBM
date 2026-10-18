from __future__ import annotations

import hashlib
import hmac
import json
import logging
import secrets
import time
import base64
import html
import io
import random
import re
import asyncio
from contextlib import asynccontextmanager, suppress
from contextvars import ContextVar
from weakref import WeakValueDictionary
from datetime import datetime, timezone
from urllib.parse import parse_qs, urlencode, urlparse
from typing import Any, Mapping, Optional

import bcrypt
import httpx
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding

from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.common.security import validate_password
from jbm_cluster_py.platform.auth.jwt import JwtError, JwtSigner
from jbm_cluster_py.platform.auth.repository import (
    AuthRepository,
    infer_account_type,
    user_is_active,
)

logger = logging.getLogger(__name__)

LOGIN_ERROR_PREFIX = "login_error:"
APP_SECRET_ENC_PREFIX = "$ENC$"
APP_SECRET_AES_KEY = hashlib.md5(b"jbm-app-client-secret").hexdigest().encode("utf-8")


class AuthError(ValueError):
    def __init__(self, message: str, code: int = 401, oauth_error: str | None = None) -> None:
        super().__init__(message)
        self.code = code
        self.oauth_error = oauth_error


class TokenCache:
    def __init__(self, redis_client: RedisClient, prefix: str = "jbm:auth", *, required: bool = False) -> None:
        self.redis_client = redis_client
        self.prefix = prefix.rstrip(":")
        self.required = required
        self._memory: dict[str, tuple[dict[str, Any], int]] = {}
        self._locks: WeakValueDictionary[str, asyncio.Lock] = WeakValueDictionary()
        self._indexes: dict[str, dict[str, int]] = {}
        self._held_locks: ContextVar[tuple[tuple[str, Any], ...]] = ContextVar("auth_cache_locks", default=())

    async def start(self) -> None:
        try:
            await self.redis_client.start()
            if self.required and self.redis_client.client is None:
                raise RuntimeError("Auth requires shared Redis")
        except Exception as exc:
            await self.redis_client.stop()
            if self.required:
                raise RuntimeError("Auth requires available shared Redis") from exc
            logger.warning("Auth Redis startup failed; use in-memory token cache: %s", exc)

    async def readiness(self) -> bool:
        if self.redis_client.client is None:
            return not self.required
        try:
            return bool(await self.redis_client.client.ping())
        except Exception:
            return False

    def _require_backend(self) -> None:
        if self.required and self.redis_client.client is None:
            raise AuthError("认证会话存储不可用", 503, "temporarily_unavailable")

    @asynccontextmanager
    async def lock(self, key: str):
        """Serialize bounded transactions across workers; abort on lease loss.

        The lease is renewed while awaiting IO. Every cache mutation is fenced
        against its ownership atomically in Redis: a stalled former owner cannot
        write after lease expiry, even before its renewal coroutine wakes up.
        """
        self._require_backend()
        redis = self.redis_client.client
        if redis is None:
            lock = self._locks.setdefault(key, asyncio.Lock())
            async with lock:
                yield
            return
        lock = redis.lock(self._key("lock:" + key), timeout=30, blocking_timeout=10, thread_local=False)
        if not await lock.acquire():
            raise AuthError("认证会话正在处理中", 503, "temporarily_unavailable")
        context = self._held_locks.set(self._held_locks.get() + ((lock.name, lock.local.token),))
        owner = asyncio.current_task()
        lease_lost = False

        async def renew() -> None:
            nonlocal lease_lost
            try:
                while True:
                    await asyncio.sleep(5)
                    await lock.extend(30, replace_ttl=True)
            except asyncio.CancelledError:
                raise
            except Exception:
                lease_lost = True
                owner.cancel()

        renewal = asyncio.create_task(renew())
        try:
            async with asyncio.timeout(120):
                yield
            if lease_lost or not await lock.owned():
                raise AuthError("认证会话锁已失效", 503, "temporarily_unavailable")
        except asyncio.CancelledError:
            if lease_lost:
                raise AuthError("认证会话锁已失效", 503, "temporarily_unavailable") from None
            raise
        finally:
            self._held_locks.reset(context)
            renewal.cancel()
            with suppress(asyncio.CancelledError):
                await renewal
            with suppress(Exception):
                await lock.release()

    async def _eval(self, script: str, keys: list[str], args: list[Any]):
        """Fence all writes made in distributed lock contexts in the same Lua call."""
        locks = self._held_locks.get()
        checks = "".join(
            "if redis.call('GET',KEYS[%d]) ~= ARGV[%d] then return redis.error_reply('AUTH_LOCK_LOST') end; "
            % (len(keys) + index + 1, len(args) + index + 1)
            for index in range(len(locks))
        )
        try:
            return await self.redis_client.client.eval(
                checks + script, len(keys) + len(locks),
                *keys, *(key for key, _ in locks), *args, *(token for _, token in locks),
            )
        except Exception as exc:
            if "AUTH_LOCK_LOST" in str(exc):
                raise AuthError("认证会话锁已失效", 503, "temporarily_unavailable") from None
            raise

    async def consume_refresh(self, token_hash: str, ttl: int) -> tuple[Optional[dict[str, Any]], bool]:
        """Publish the replay marker in the same atomic step as consumption."""
        self._require_backend()
        key, used = "refresh:" + token_hash, "refresh_used:" + token_hash
        if self.redis_client.client is not None:
            result = await self._eval(
                "local v=redis.call('GET',KEYS[1]); "
                "if v then redis.call('SET',KEYS[2],v,'EX',ARGV[1]); "
                "redis.call('DEL',KEYS[1]); return {v,0}; end; "
                "return {redis.call('GET',KEYS[2]) or '',1}",
                [self._key(key), self._key(used)], [ttl],
            )
            return (json.loads(result[0]) if result[0] else None, bool(result[1]))
        async with self.lock("consume:" + token_hash):
            state = await self.pop_json(key)
            if state:
                await self.set_json(used, state, ttl)
                return state, False
            return await self.get_json(used), True

    async def index_add(self, key: str, member: str, expires_at: int) -> None:
        self._require_backend()
        if self.redis_client.client is not None:
            # Score-based expiry avoids shortening a live member's lifetime when
            # another access token has a shorter TTL.
            await self._eval(
                "redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',ARGV[3]); "
                "redis.call('ZADD',KEYS[1],ARGV[1],ARGV[2]); "
                "local top=redis.call('ZREVRANGE',KEYS[1],0,0,'WITHSCORES'); "
                "redis.call('EXPIREAT',KEYS[1],math.ceil(tonumber(top[2]))); return 1",
                [self._key(key)], [expires_at, member, int(time.time())],
            )
            return
        self._indexes.setdefault(key, {})[member] = expires_at

    async def index_members(self, key: str) -> list[str]:
        self._require_backend()
        now = int(time.time())
        if self.redis_client.client is not None:
            result = await self._eval(
                "redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',ARGV[1]); "
                "return redis.call('ZRANGE',KEYS[1],0,-1)", [self._key(key)], [now],
            )
            return [item.decode() if isinstance(item, bytes) else str(item) for item in result]
        self._indexes[key] = {member: expiry for member, expiry in self._indexes.get(key, {}).items() if expiry > now}
        return list(self._indexes[key])

    async def stop(self) -> None:
        await self.redis_client.stop()

    async def set_json(self, key: str, value: Mapping[str, Any], ttl_seconds: int) -> None:
        self._require_backend()
        full_key = self._key(key)
        if self.redis_client.client is not None:
            await self._eval("return redis.call('SET',KEYS[1],ARGV[1],'EX',ARGV[2])", [full_key], [json.dumps(value, ensure_ascii=False), ttl_seconds])
            return
        self._memory[full_key] = (dict(value), int(time.time()) + ttl_seconds)

    async def get_json(self, key: str) -> Optional[dict[str, Any]]:
        self._require_backend()
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

    async def pop_json(self, key: str) -> Optional[dict[str, Any]]:
        self._require_backend()
        full_key = self._key(key)
        if self.redis_client.client is not None:
            raw = await self._eval("local v=redis.call('GET',KEYS[1]); redis.call('DEL',KEYS[1]); return v", [full_key], [])
            return json.loads(raw) if raw else None
        item = self._memory.pop(full_key, None)
        if not item:
            return None
        value, expires_at = item
        return dict(value) if expires_at > int(time.time()) else None

    async def delete(self, key: str) -> None:
        self._require_backend()
        full_key = self._key(key)
        if self.redis_client.client is not None:
            await self._eval("return redis.call('DEL',KEYS[1])", [full_key], [])
            return
        self._memory.pop(full_key, None)

    async def expire(self, key: str, ttl_seconds: int) -> bool:
        self._require_backend()
        full_key = self._key(key)
        ttl = max(int(ttl_seconds), 1)
        if self.redis_client.client is not None:
            return bool(await self._eval("return redis.call('EXPIRE',KEYS[1],ARGV[1])", [full_key], [ttl]))
        item = self._memory.get(full_key)
        if not item:
            return False
        value, _ = item
        self._memory[full_key] = (value, int(time.time()) + ttl)
        return True

    async def list_json(self, prefix: str) -> list[dict[str, Any]]:
        self._require_backend()
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
        self._require_backend()
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
        self._require_backend()
        key = LOGIN_ERROR_PREFIX + username
        ttl_seconds = ttl_minutes * 60
        if self.redis_client.client is not None:
            count = await self._eval("local n=redis.call('INCR',KEYS[1]); redis.call('EXPIRE',KEYS[1],ARGV[1]); return n", [key], [ttl_seconds])
            return int(count)
        count = await self.login_error_count(username) + 1
        self._memory[key] = ({"count": count}, int(time.time()) + ttl_seconds)
        return count

    async def clear_login_error(self, username: str) -> None:
        self._require_backend()
        key = LOGIN_ERROR_PREFIX + username
        if self.redis_client.client is not None:
            await self._eval("return redis.call('DEL',KEYS[1])", [key], [])
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
        discovery: Any = None,
        http_client: Optional[httpx.AsyncClient] = None,
    ) -> None:
        self.repository = repository
        self.cache = cache
        self.config = dict(config)
        self.discovery = discovery
        self.http_client = http_client
        jwt_config = dict(self.config.get("jwt") or {})
        issuer = str(jwt_config.get("issuer") or "http://localhost:5555")
        audience = str(jwt_config.get("audience") or "jbm-api")
        self.signer = JwtSigner(
            issuer=issuer,
            audience=audience,
            kid=str(jwt_config.get("key-id") or jwt_config.get("kid") or "jbm-auth-rs256"),
            private_key_pem=jwt_config.get("private-key"),
            verification_keys=jwt_config.get("verification-keys"),
        )
        self.issuer = issuer.rstrip("/")
        self.audience = audience
        self.access_seconds = int(jwt_config.get("access-token-seconds") or 7200)
        self.refresh_seconds = int(jwt_config.get("refresh-token-seconds") or 604800)
        self.account_domain = str(self.config.get("account-domain") or "@admin.com")
        self.max_errors = int(self.config.get("login-error-number") or 5)
        self.lock_minutes = int(self.config.get("login-error-limit-minutes") or 10)
        self.login_captcha_required = _truthy(self.config.get("login-captcha-required", False))
        self.max_sessions_per_user = max(int(self.config.get("max-sessions-per-user") or 5), 1)
        self.require_pkce = _truthy(self.config.get("require-pkce", True))
        self.require_https_redirects = _truthy(self.config.get("require-https-redirects", True))
        self.legacy_password_grant_enabled = _truthy(
            self.config.get("legacy-password-grant-enabled", False)
        )
        self.dev_bypass_enabled = _truthy(self.config.get("dev-bypass-enabled", False))
        self.fixed_captcha_code = str(self.config.get("fixed-captcha-code") or "").strip()
        self.allow_plaintext_secrets = _truthy(self.config.get("allow-plaintext-secrets", False))
        self.login_providers = dict(self.config.get("login-providers") or {})
        self.password_policy = dict(self.config.get("password-policy") or {})
        sms_config = dict(self.config.get("sms") or {})
        self.sms_registration_required = _truthy(sms_config.get("registration-required", False))
        self.sms_push_service = str(sms_config.get("push-service") or "jbm-cluster-platform-push")
        self.sms_push_base_url = str(sms_config.get("push-base-url") or "").rstrip("/")
        self.sms_valid_time = max(int(sms_config.get("valid-time") or 300), 60)
        self.sms_interval = max(int(sms_config.get("interval") or 60), 1)
        from .key_login import KeyLoginService
        self.key_login = KeyLoginService(self)

    async def password_token(self, form: Mapping[str, Any]) -> dict[str, Any]:
        if not self.legacy_password_grant_enabled:
            raise AuthError(
                "password grant已禁用，请使用authorization_code + PKCE",
                400,
                "unsupported_grant_type",
            )
        client = await self._resolve_user_flow_client(form)
        try:
            token = await self._password_login_for_client(client, form)
        except AuthError as exc:
            await self._record_login(form, status=0, message=str(exc))
            raise
        await self._record_login(form)
        return token

    async def validate_authorization_request(self, form: Mapping[str, Any]) -> None:
        if str(form.get("response_type") or "") != "code":
            raise AuthError("仅支持response_type=code", 400, "unsupported_response_type")
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        redirect_uri = str(form.get("redirect_uri") or form.get("redirectUri") or "").strip()
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端不存在", 401, "invalid_request")
        self._validate_redirect_uri(client, redirect_uri)
        self._validate_pkce_request(form)

    async def authorize_code_login(self, form: Mapping[str, Any]) -> str:
        if str(form.get("response_type") or "code") != "code":
            raise AuthError("仅支持response_type=code", 400)
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        redirect_uri = str(form.get("redirect_uri") or form.get("redirectUri") or "").strip()
        if not client_id:
            raise AuthError("client_id不能为空", 400)
        if not redirect_uri:
            raise AuthError("redirect_uri不能为空", 400)
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端不存在", 401)
        self._validate_redirect_uri(client, redirect_uri)
        code_challenge, code_challenge_method = self._validate_pkce_request(form)
        try:
            account, user, scope = await self._authenticate_user_for_client(client, form)
        except AuthError as exc:
            await self._record_login(form, status=0, message=str(exc))
            raise
        audit = dict(form)
        audit["username"] = account.get("account") or user.get("user_name")
        audit["account_type"] = account.get("account_type")
        await self._record_login(audit)
        return await self._authorization_redirect(
            client,
            redirect_uri,
            {
                "userId": int(user["user_id"]),
                "username": account.get("account") or user.get("user_name"),
                "account": account.get("account"),
                "accountType": account.get("account_type"),
                "accountVersion": _account_version(account),
                "mustChangePassword": bool(account.get("must_change_password")),
                "scope": scope,
                "codeChallenge": code_challenge,
                "codeChallengeMethod": code_challenge_method,
            },
            str(form.get("state") or "").strip(),
        )

    async def thirdparty_code_login(self, provider: str, form: Mapping[str, Any]) -> str:
        provider_name = str(provider or "").strip().lower()
        if not re.fullmatch(r"[a-z0-9_-]{1,32}", provider_name):
            raise AuthError("第三方提供商无效", 400)
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        redirect_uri = str(form.get("redirect_uri") or form.get("redirectUri") or "").strip()
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端不存在", 401)
        self._validate_redirect_uri(client, redirect_uri)
        code_challenge, code_challenge_method = self._validate_pkce_request(form)
        try:
            account, user, scope = await self._authenticate_external_login(
                "THIRD_PARTY",
                str(form.get("username") or ""),
                str(form.get("code") or ""),
                form,
                provider_key="thirdparty-" + provider_name,
            )
        except AuthError as exc:
            await self._record_login(form, status=0, message=str(exc))
            raise
        audit = dict(form)
        audit["username"] = account.get("account") or user.get("user_name")
        audit["loginType"] = "THIRD_PARTY"
        audit["account_type"] = account.get("account_type")
        await self._record_login(audit)
        return await self._authorization_redirect(
            client,
            redirect_uri,
            {
                "userId": int(user["user_id"]),
                "username": account.get("account") or user.get("user_name"),
                "account": account.get("account"),
                "accountType": account.get("account_type"),
                "accountVersion": _account_version(account),
                "mustChangePassword": bool(account.get("must_change_password")),
                "scope": scope,
                "codeChallenge": code_challenge,
                "codeChallengeMethod": code_challenge_method,
            },
            str(form.get("state") or "").strip(),
        )

    async def _record_login(
        self,
        form: Mapping[str, Any],
        *,
        status: int = 1,
        message: str = "登录成功",
    ) -> None:
        username = str(form.get("username") or "").strip()
        try:
            account = await self.repository.find_account(
                username,
                str(form.get("account_type") or infer_account_type(username)),
                self.account_domain,
            )
            await self.repository.record_login(
                user_id=int(account["user_id"]) if account and account.get("user_id") is not None else None,
                account=username,
                login_type=str(form.get("loginType") or form.get("login_type") or "PASSWORD").upper(),
                ip=str(form.get("login_ip") or ""),
                user_agent=str(form.get("user_agent") or ""),
                status=status,
                message=message,
            )
        except Exception as exc:
            logger.warning("Login audit persistence failed: %s", exc)

    async def _authorization_redirect(
        self,
        client: Mapping[str, Any],
        redirect_uri: str,
        grant: Mapping[str, Any],
        state: str = "",
    ) -> str:
        code = secrets.token_urlsafe(32)
        await self.cache.set_json(
            "auth_code:" + _hash_token(code),
            {
                "clientId": client.get("clientId"),
                "redirectUri": redirect_uri,
                **dict(grant),
            },
            300,
        )
        params = {"code": code}
        if state:
            params["state"] = state
        separator = "&" if "?" in redirect_uri else "?"
        return redirect_uri + separator + urlencode(params)

    async def authorization_code_token(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        if not client_id:
            raise AuthError("client_id不能为空", 400, "invalid_request")
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端无效", 401, "invalid_client")
        self._validate_token_client(client, form)
        code = str(form.get("code") or "").strip()
        if not code:
            raise AuthError("code不能为空", 400, "invalid_request")
        key = "auth_code:" + _hash_token(code)
        cached = await self.cache.pop_json(key)
        if not cached:
            raise AuthError("授权码无效或已过期", 400, "invalid_grant")
        if str(cached.get("clientId") or "") != str(client.get("clientId") or ""):
            raise AuthError("授权码客户端不匹配", 400, "invalid_grant")
        redirect_uri = str(form.get("redirect_uri") or form.get("redirectUri") or "").strip()
        if not redirect_uri or redirect_uri != str(cached.get("redirectUri") or ""):
            raise AuthError("redirect_uri不匹配", 400, "invalid_grant")
        self._verify_pkce(cached, form)
        user_id = int(cached.get("userId") or 0)
        user = await self.repository.find_user(user_id)
        if not user or not user_is_active(user):
            raise AuthError("用户已被禁用", 403)
        account = {
            "account": cached.get("account") or cached.get("username") or user.get("user_name"),
            "account_type": cached.get("accountType"),
            "credential_version": cached.get("accountVersion"),
            "user_id": user_id,
            "must_change_password": cached.get("mustChangePassword"),
        }
        return await self._issue_user_token(client, account, user, str(cached.get("scope") or "all"))

    async def _password_login_for_client(
        self,
        client: Mapping[str, Any],
        form: Mapping[str, Any],
    ) -> dict[str, Any]:
        account, user, scope = await self._authenticate_user_for_client(client, form)
        return await self._issue_user_token(client, account, user, scope)

    async def _authenticate_user_for_client(
        self,
        client: Mapping[str, Any],
        form: Mapping[str, Any],
    ) -> tuple[dict[str, Any], dict[str, Any], str]:
        username = str(form.get("username") or "").strip()
        password = str(form.get("password") or "")
        login_type = str(form.get("loginType") or form.get("login_type") or "PASSWORD").upper()
        if login_type in {"PASSKEY", "SSH_KEY"}:
            return await self.key_login.authenticate(client, form)
        if login_type in {"PASSWORD", "SMS"} and (
            self.login_captcha_required or form.get("vcode")
        ):
            await self.verify_captcha(str(form.get("vcode") or ""))
        if login_type == "PASSWORD" and (
            _truthy(form.get("password_encrypted")) or _looks_like_ciphertext(password)
        ):
            password = _decrypt_password(password, str(client.get("privateKey") or ""))
        if not username or not password:
            raise AuthError("用户名或密码不能为空", 400)
        if login_type in {"PASSWORD", "SMS"}:
            count = await self.cache.login_error_count(username)
            if count >= self.max_errors:
                raise AuthError("登录失败次数过多，帐户锁定%s分钟" % self.lock_minutes, 423)
        if login_type == "SMS":
            try:
                await self.verify_phone_code(username, password)
            except AuthError:
                await self.cache.add_login_error(username, self.lock_minutes)
                raise
            account = await self.repository.find_account(username, "mobile", self.account_domain)
            if not account:
                users = await self.repository.find_users_by_mobile(username)
                if len(users) > 1:
                    raise AuthError("手机号资料存在冲突，请联系管理员处理", 409)
                if not users:
                    await self._auto_register_sms_account(client, username)
                else:
                    try:
                        await self.repository.bind_mobile_account(
                            int(users[0]["user_id"]), username, self.account_domain
                        )
                    except ValueError as exc:
                        raise AuthError(str(exc), 409) from exc
                account = await self.repository.find_account(username, "mobile", self.account_domain)
            if not account:
                raise AuthError("手机号登录初始化失败", 503)
            user = await self.repository.find_user(int(account["user_id"]))
            if not user or not user_is_active(user):
                raise AuthError("用户已被禁用", 403)
            await self.cache.clear_login_error(username)
            return dict(account), dict(user), str(form.get("scope") or "all")
        if login_type in {"FACE", "WECHAT", "MINIAPP"}:
            return await self._authenticate_external_login(login_type, username, password, form)
        if login_type != "PASSWORD":
            raise AuthError("不支持的登录模式: %s" % login_type, 400)
        account_type = str(form.get("account_type") or infer_account_type(username))
        account = await self.repository.find_account(username, account_type, self.account_domain)
        if not account or not _secret_matches(
            password,
            str(account.get("password") or ""),
            self.allow_plaintext_secrets,
        ):
            await self.cache.add_login_error(username, self.lock_minutes)
            raise AuthError("用户名或密码错误", 401)
        if int(account.get("status") or 0) != 1:
            raise AuthError("帐号已被禁用", 403)
        user = await self.repository.find_user(int(account["user_id"]))
        if not user or not user_is_active(user):
            raise AuthError("用户已被禁用", 403)
        await self.cache.clear_login_error(username)
        return dict(account), dict(user), str(form.get("scope") or "all")

    async def _auto_register_sms_account(self, client: Mapping[str, Any], mobile: str) -> None:
        registration = dict(client.get("registration") or {})
        if not registration.get("enabled") or registration.get("mode") != "tenant":
            raise AuthError("该应用未开放短信自动注册", 403)
        app_id = int(client.get("appId") or 0)
        default_role_code = str(registration.get("defaultRoleCode") or "").strip()
        if not app_id or not default_role_code:
            raise AuthError("应用注册配置不完整", 503)
        display_name = "手机用户%s" % mobile[-4:]
        password_hash = bcrypt.hashpw(
            secrets.token_urlsafe(32).encode("utf-8"), bcrypt.gensalt()
        ).decode("utf-8")
        try:
            await self.repository.create_tenant_account(
                app_id=app_id,
                default_role_code=default_role_code,
                tenant_name="%s的账号空间" % display_name,
                org_type="account",
                username=mobile,
                password_hash=password_hash,
                nick_name=display_name,
                mobile=mobile,
                domain=self.account_domain,
            )
        except ValueError as exc:
            if not await self.repository.find_account(mobile, "mobile", self.account_domain):
                raise AuthError(str(exc), 409) from exc

    async def _authenticate_external_login(
        self,
        login_type: str,
        username: str,
        credential: str,
        form: Mapping[str, Any],
        provider_key: str | None = None,
    ) -> tuple[dict[str, Any], dict[str, Any], str]:
        key = provider_key or login_type.lower()
        settings = dict(self.login_providers.get(key) or {})
        if not _truthy(settings.get("enabled")):
            raise AuthError("%s登录通道未启用" % key, 503)
        if not credential:
            raise AuthError("登录凭证不能为空", 400)
        if len(credential) > int(settings.get("max-credential-length") or 2_800_000):
            raise AuthError("登录凭证过大", 413)

        if self.dev_bypass_enabled and _truthy(settings.get("dev-mock-enabled")):
            expected = str(settings.get("dev-code") or "")
            if login_type == "FACE":
                if not credential.startswith("data:image/") or len(credential) < 32:
                    raise AuthError("人脸照片无效", 401)
            elif not expected or not hmac.compare_digest(credential, expected):
                raise AuthError("第三方登录凭证无效", 401)
            subject = str(settings.get("dev-subject") or username).strip()
        else:
            verify_url = str(settings.get("verify-url") or "").strip()
            if not verify_url.startswith(("http://", "https://")):
                raise AuthError("%s登录通道配置不完整" % key, 503)
            headers = {"Content-Type": "application/json"}
            auth_token = str(settings.get("auth-token") or "").strip()
            if auth_token:
                headers["Authorization"] = auth_token
            client = self.http_client
            close_client = False
            if client is None:
                client = httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=3.0), trust_env=False)
                close_client = True
            try:
                response = await client.post(
                    verify_url,
                    headers=headers,
                    json={
                        "provider": key,
                        "loginType": login_type,
                        "subjectHint": username,
                        "credential": credential,
                    },
                )
            except httpx.HTTPError as exc:
                raise AuthError("%s登录通道调用失败" % key, 503) from exc
            finally:
                if close_client:
                    await client.aclose()
            if response.status_code >= 400:
                raise AuthError("%s登录凭证校验失败" % key, 401)
            try:
                body = response.json()
            except ValueError as exc:
                raise AuthError("%s登录通道响应异常" % key, 503) from exc
            if not isinstance(body, Mapping) or body.get("success") is False:
                raise AuthError("%s登录凭证校验失败" % key, 401)
            result = body.get("result") if isinstance(body.get("result"), Mapping) else body
            verified = result.get("verified", result.get("active", result.get("success", True)))
            if not _truthy(verified):
                raise AuthError("%s登录凭证校验失败" % key, 401)
            subject = str(
                result.get("subject")
                or result.get("openid")
                or result.get("phone")
                or result.get("account")
                or ""
            ).strip()

        if not subject:
            raise AuthError("%s登录通道未返回用户标识" % key, 401)
        if username and _truthy(settings.get("require-subject-match", True)) and subject != username:
            raise AuthError("第三方用户标识不匹配", 401)
        account_type = str(settings.get("account-type") or login_type.lower())
        account = await self.repository.find_account(subject, account_type, self.account_domain)
        if not account:
            raise AuthError("第三方账号未绑定", 401)
        if int(account.get("status") or 0) != 1:
            raise AuthError("帐号已被禁用", 403)
        user = await self.repository.find_user(int(account["user_id"]))
        if not user or not user_is_active(user):
            raise AuthError("用户已被禁用", 403)
        return dict(account), dict(user), str(form.get("scope") or "all")

    async def client_credentials_token(self, form: Mapping[str, Any]) -> dict[str, Any]:
        if not await self.cache.readiness():
            raise AuthError("认证会话存储不可用", 503, "temporarily_unavailable")
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
        refresh_token = str(form.get("refresh_token") or "").strip()
        if not refresh_token:
            raise AuthError("refresh_token不能为空", 400, "invalid_request")
        token_hash = _hash_token(refresh_token)
        state = await self.cache.get_json("refresh:" + token_hash)
        if not state:
            state = await self.cache.get_json("refresh_used:" + token_hash)
        if not state:
            raise AuthError("refresh_token无效或已过期", 400, "invalid_grant")
        state = await self._resolve_refresh_owner(state)
        requested_client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        if requested_client_id and requested_client_id != str(state.get("clientId") or ""):
            raise AuthError("refresh_token客户端不匹配", 400, "invalid_grant")
        client = await self.repository.find_client(str(state.get("clientId") or ""))
        if not client:
            raise AuthError("客户端无效", 401)
        self._validate_token_client(client, form)
        # Authentication must precede every mutation, including replay revocation.
        state, reused = await self.cache.consume_refresh(token_hash, self.refresh_seconds)
        if not state:
            raise AuthError("refresh_token无效或已过期", 400, "invalid_grant")
        family_id = str(state.get("familyId") or secrets.token_urlsafe(24))
        if reused:
            await self._revoke_refresh_family(family_id)
            raise AuthError("refresh_token已被使用", 400, "invalid_grant")
        if await self.cache.get_json("refresh_family_revoked:" + family_id):
            raise AuthError("登录会话已失效", 400, "invalid_grant")
        user_id = int(state["userId"])
        user = await self.repository.find_user(user_id)
        if not user or not user_is_active(user):
            await self._revoke_refresh_family(family_id)
            raise AuthError("用户已被禁用", 403)
        await self._require_account_active(state)
        await self._revoke_access_state(state)
        account = {
            "user_id": user_id,
            "must_change_password": state.get("mustChangePassword"),
            "account": state.get("account") or state.get("username") or user.get("user_name"),
            "account_type": state.get("accountType"),
            "credential_version": state.get("accountVersion"),
        }
        return await self._issue_user_token(
            client,
            account,
            user,
            str(state.get("scope") or "all"),
            family_id=family_id,
        )

    async def logout(self, token: str | None = None, refresh_token: str | None = None) -> dict[str, bool]:
        if refresh_token:
            token_hash = _hash_token(refresh_token)
            state = (await self.cache.get_json("refresh:" + token_hash)
                     or await self.cache.get_json("refresh_used:" + token_hash))
            if state and state.get("familyId"):
                await self._revoke_refresh_family(str(state["familyId"]))
        if token:
            # Keep the session-to-family reference beyond access expiry, so logout
            # after expiry or a racing refresh still ends the refresh capability.
            state = (await self.cache.get_json("access:" + _hash_token(token))
                     or await self.cache.get_json("session_family:" + _hash_token(token)))
            if state and state.get("familyId"):
                await self._revoke_refresh_family(str(state["familyId"]))
            await self.revoke_access_token(token)
        return {"logout": True}

    async def _resolve_refresh_owner(self, state: Mapping[str, Any]) -> dict[str, Any]:
        """Recover client ownership for replay markers issued before the upgrade."""
        if state.get("clientId"):
            return dict(state)
        family_id = str(state.get("familyId") or "")
        family = await self.cache.get_json("refresh_family:" + family_id) if family_id else None
        current = await self.cache.get_json("refresh:" + str(family.get("refreshHash") or "")) if family else None
        return {**(current or {}), **dict(state)}

    async def revoke_token(self, form: Mapping[str, Any]) -> None:
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端无效", 401, "invalid_client")
        self._validate_token_client(client, form)
        token = str(form.get("token") or "").strip()
        if not token:
            raise AuthError("token不能为空", 400, "invalid_request")
        token_hash = _hash_token(token)
        hint = str(form.get("token_type_hint") or "").strip()
        if hint in {"", "refresh_token"}:
            state = (await self.cache.get_json("refresh:" + token_hash)
                     or await self.cache.get_json("refresh_used:" + token_hash))
            if state:
                state = await self._resolve_refresh_owner(state)
            if state and str(state.get("clientId") or "") == client_id:
                if state.get("familyId"):
                    await self._revoke_refresh_family(str(state["familyId"]))
                return
        if hint in {"", "access_token"}:
            try:
                claims = self.signer.verify(token)
            except JwtError:
                return
            if str(claims.get("client_id") or "") == client_id:
                await self.revoke_access_token(token)

    async def introspect_token(self, form: Mapping[str, Any]) -> dict[str, Any]:
        caller = await self._require_client(form)
        token = str(form.get("token") or "").strip()
        if not token:
            raise AuthError("token不能为空", 400, "invalid_request")
        hint = str(form.get("token_type_hint") or "").strip()
        if hint in {"", "access_token"}:
            try:
                claims = self.signer.verify(token)
                await self._require_active_access(token, claims)
            except (JwtError, AuthError):
                claims = None
            if claims and str(claims.get("client_id") or "") == str(caller.get("clientId") or ""):
                return {
                    "active": True,
                    "scope": claims.get("scope"),
                    "client_id": claims.get("client_id"),
                    "username": claims.get("username"),
                    "token_type": "Bearer",
                    "sub": claims.get("sub"),
                    "aud": claims.get("aud"),
                    "iss": claims.get("iss"),
                    "iat": claims.get("iat"),
                    "nbf": claims.get("nbf"),
                    "exp": claims.get("exp"),
                    "jti": claims.get("jti"),
                }
        if hint in {"", "refresh_token"}:
            state = await self.cache.get_json("refresh:" + _hash_token(token))
            active = bool(state and str(state.get("clientId") or "") == str(caller.get("clientId") or ""))
            if active:
                family_id = str(state.get("familyId") or "")
                user = await self.repository.find_user(int(state.get("userId") or 0))
                active = bool(user and user_is_active(user) and not await self.cache.get_json("refresh_family_revoked:" + family_id))
                if active:
                    try:
                        await self._require_account_active(state)
                    except AuthError:
                        active = False
                if active and str(user.get("user_name") or "") != "admin":
                    active = await self.repository.tenant_app_enabled(int(user.get("company_id") or 0), int(caller.get("appId") or 0))
            if active:
                return {
                    "active": True,
                    "client_id": state.get("clientId"),
                    "username": state.get("username"),
                    "scope": state.get("scope"),
                    "token_type": "refresh_token",
                }
        return {"active": False}

    async def userinfo(self, token: str) -> dict[str, Any]:
        claims = self.signer.verify(token)
        await self._require_active_access(token, claims)
        role_context = await self._current_role_context(claims)
        permissions = await self._permissions_for_claims(claims, role_context)
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
            "tenantId": claims.get("tenant_id"),
            "departmentId": claims.get("department_id"),
            "userType": claims.get("user_type"),
            "roles": role_context[0],
            "permissions": permissions,
            "scope": claims.get("scope"),
            "mustChangePassword": bool(claims.get("must_change_password")),
        }

    async def online_users(
        self,
        search: Mapping[str, Any],
        current_token: str = "",
    ) -> dict[str, Any]:
        page_form = dict(search.get("pageForm") or search.get("page_form") or {})
        curr_page = max(int(page_form.get("currPage") or page_form.get("curr_page") or 1), 1)
        page_size = max(int(page_form.get("pageSize") or page_form.get("page_size") or 10), 1)
        rows = await self.cache.list_json("access:")
        rows = [row for row in rows if not await self._is_revoked(row)]
        rows = self._filter_online_rows(rows, search)
        rows.sort(key=lambda row: str(row.get("loginTime") or ""), reverse=True)
        current_session_id = _hash_token(current_token) if current_token else ""
        for row in rows:
            row["currentSession"] = bool(
                current_session_id and str(row.get("tokenId") or "") == current_session_id
            )
            row.pop("jti", None)
            row.pop("accessExpiresAt", None)
            row.pop("familyId", None)
            row.pop("accountVersion", None)
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

    async def revoke_access_session(self, session_id: str) -> None:
        session_key = str(session_id or "").strip().lower()
        if not re.fullmatch(r"[0-9a-f]{64}", session_key):
            raise AuthError("会话ID无效", 400)
        state = (await self.cache.get_json("access:" + session_key)
                 or await self.cache.get_json("session_family:" + session_key))
        if not state:
            return
        if state.get("familyId"):
            await self._revoke_refresh_family(str(state["familyId"]))
        expires_at = int(state.get("accessExpiresAt") or 0)
        ttl = expires_at - int(time.time())
        jti = str(state.get("jti") or "")
        if jti and ttl > 0:
            await self.cache.set_json("revoked:" + jti, {"revoked": True}, ttl)
        await self.cache.delete("access:" + session_key)

    async def expire_access_session(self, session_id: str, seconds: int) -> None:
        session_key = str(session_id or "").strip().lower()
        if not re.fullmatch(r"[0-9a-f]{64}", session_key):
            raise AuthError("会话ID无效", 400)
        state = await self.cache.get_json("access:" + session_key)
        if not state:
            raise AuthError("会话不存在或已过期", 404)
        ttl = max(min(int(seconds), self.access_seconds), 1)
        expires_at = int(time.time()) + ttl
        state["accessExpiresAt"] = expires_at
        state["expiredTime"] = _timestamp(expires_at)
        await self.cache.set_json("access:" + session_key, state, ttl)

    async def _revoke_access_state(self, state: Mapping[str, Any]) -> None:
        jti = str(state.get("accessJti") or "")
        expires_at = int(state.get("accessExpiresAt") or 0)
        ttl = expires_at - int(time.time())
        if jti and ttl > 0:
            await self.cache.set_json("revoked:" + jti, {"revoked": True}, ttl)
        access_key = str(state.get("accessKey") or "")
        if access_key:
            await self.cache.delete("access:" + access_key)

    async def _revoke_refresh_family(self, family_id: str) -> None:
        await self.cache.set_json(
            "refresh_family_revoked:" + family_id,
            {"revoked": True},
            max(self.refresh_seconds, self.access_seconds) + 120,
        )
        family = await self.cache.pop_json("refresh_family:" + family_id)
        await self.cache.delete("family_session:" + family_id)
        if not family:
            return
        refresh_hash = str(family.get("refreshHash") or "")
        if refresh_hash:
            await self.cache.delete("refresh:" + refresh_hash)
        await self._revoke_access_state(family)

    async def verify_permissions(self, token: str, *permissions: str) -> None:
        if not token:
            raise AuthError("未登录", 401)
        claims = self.signer.verify(token)
        await self._require_active_access(token, claims)
        token_permissions = set(await self._permissions_for_claims(claims))
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

    async def _current_role_context(self, claims: Mapping[str, Any]) -> tuple[list[str], bool]:
        if claims.get("user_id") is None:
            return [], False
        rows = await self.repository.user_roles(
            int(claims["user_id"]), int(claims.get("app_id") or 0), int(claims.get("tenant_id") or 0),
        )
        codes = sorted({str(row["role_code"]) for row in rows if row.get("role_code")})
        # Keep the legacy admin rule, but never reuse a historical role-1 grant.
        root = str(claims.get("username") or "").lower() == "admin" or any(
            int(row.get("role_id") or 0) == 1 for row in rows
        )
        return codes, root

    async def _permissions_for_claims(
        self, claims: Mapping[str, Any], role_context: tuple[list[str], bool] | None = None,
    ) -> list[str]:
        raw_user_id = claims.get("user_id")
        if raw_user_id is None:
            embedded = claims.get("permissions")
            return [str(item) for item in embedded if item] if isinstance(embedded, list) else []
        user_id = int(raw_user_id)
        _, root = role_context if role_context is not None else await self._current_role_context(claims)
        rows = await self.repository.user_authorities(
            user_id,
            root,
            int(claims.get("app_id") or 0),
            int(claims.get("tenant_id") or 0),
        )
        return [str(item) for item in rows if item]

    async def _require_active_access(self, token: str, claims: Mapping[str, Any]) -> None:
        jti = str(claims.get("jti") or "")
        if jti and await self.cache.get_json("revoked:" + jti):
            raise AuthError("token已失效", 401)
        client = await self.repository.find_client(str(claims.get("client_id") or ""))
        if not client:
            raise AuthError("客户端已失效", 401)
        if claims.get("user_id") is not None:
            state = await self.cache.get_json("access:" + _hash_token(token))
            if not state or str(state.get("jti") or "") != jti:
                raise AuthError("登录会话已失效", 401)
            if state.get("familyId") and await self.cache.get_json("refresh_family_revoked:" + str(state["familyId"])):
                raise AuthError("登录会话已失效", 401)
            user = await self.repository.find_user(int(claims["user_id"]))
            if not user or not user_is_active(user):
                raise AuthError("用户已被禁用", 403)
            await self._require_account_active(state)
            if str(user.get("user_name") or "") != "admin":
                tenant_id = int(user.get("company_id") or 0)
                if tenant_id != int(claims.get("tenant_id") or 0) or not await self.repository.tenant_app_enabled(tenant_id, int(claims.get("app_id") or 0)):
                    raise AuthError("租户应用授权已失效", 403)

    async def _require_account_active(self, state: Mapping[str, Any]) -> Optional[dict[str, Any]]:
        # Old sessions without an account reference remain compatible. New
        # sessions keep the actual login account (mobile/email may differ from username).
        account_name = str(state.get("account") or "")
        if not account_name:
            return
        account = await self.repository.find_account(
            account_name, str(state.get("accountType") or infer_account_type(account_name)), self.account_domain,
        )
        if not account or int(account.get("status") or 0) != 1 or int(account.get("user_id") or 0) != int(state.get("userId") or 0):
            raise AuthError("帐号已被禁用", 403)
        if state.get("accountVersion") and not hmac.compare_digest(str(state["accountVersion"]), _account_version(account)):
            raise AuthError("帐号凭证已变更，请重新登录", 401, "invalid_grant")
        return dict(account)

    def openid_configuration(self) -> dict[str, Any]:
        issuer = self.issuer
        grants = [
            "authorization_code",
            "client_credentials",
            "refresh_token",
        ]
        if self.legacy_password_grant_enabled:
            grants.append("password")
        return {
            "issuer": issuer,
            "authorization_endpoint": issuer + "/oauth2/authorize",
            "jwks_uri": issuer + "/jwks.json",
            "token_endpoint": issuer + "/oauth2/token",
            "userinfo_endpoint": issuer + "/oauth2/userinfo",
            "end_session_endpoint": issuer + "/oauth2/logout",
            "revocation_endpoint": issuer + "/oauth2/revoke",
            "introspection_endpoint": issuer + "/oauth2/introspect",
            "grant_types_supported": grants,
            "token_endpoint_auth_methods_supported": ["none", "client_secret_post", "client_secret_basic"],
            "revocation_endpoint_auth_methods_supported": ["none", "client_secret_post", "client_secret_basic"],
            "introspection_endpoint_auth_methods_supported": ["client_secret_post", "client_secret_basic"],
            "response_types_supported": ["code"],
            "code_challenge_methods_supported": ["S256"],
            "scopes_supported": ["all"],
        }

    def jwks(self) -> dict[str, Any]:
        return self.signer.jwks()

    async def public_key(self, client_id: str) -> Optional[str]:
        client = await self.repository.find_client(client_id)
        return str(client.get("publicKey") or "") if client else None

    async def public_clients(self) -> list[dict[str, Any]]:
        return await self.repository.list_clients()

    async def captcha_base64(self, width: int = 120, height: int = 40, scope: str = "system") -> str:
        width = max(int(width or 120), 80)
        height = max(int(height or 40), 32)
        code = "".join(random.choice("23456789ABCDEFGHJKLMNPQRSTUVWXYZ") for _ in range(5))
        await self.cache.set_json("captcha:%s:%s" % (scope or "system", code.lower()), {"code": code}, 60)
        return _captcha_image_base64(code, width, height)

    async def verify_captcha(self, code: str, scope: str = "system") -> bool:
        value = str(code or "").strip()
        if value and (
            value == self.fixed_captcha_code or self.dev_bypass_enabled and value == "9999"
        ):
            return True
        if not value:
            raise AuthError("验证码不能为空", 400)
        cached = await self.cache.pop_json("captcha:%s:%s" % (scope or "system", value.lower()))
        if not cached:
            raise AuthError("验证码错误", 400)
        return True

    async def send_phone_code(self, phone: str, image_code: str) -> bool:
        phone_value = str(phone or "").strip()
        if not re.fullmatch(r"1\d{10}", phone_value):
            raise AuthError("非法手机号", 400)
        await self.verify_captcha(image_code)
        result = await self._send_sms_via_push(phone_value)
        legacy_pin = str(result.get("pin") or "").strip()
        if legacy_pin:
            await self.cache.set_json(
                "phone:%s" % phone_value,
                {"code": legacy_pin},
                self.sms_valid_time,
            )
        return True

    async def verify_phone_code(self, phone: str, code: str) -> bool:
        phone_value = str(phone or "").strip()
        code_value = str(code or "").strip()
        if not re.fullmatch(r"1\d{10}", phone_value):
            raise AuthError("非法手机号", 400)
        if not code_value:
            raise AuthError("短信验证码不能为空", 400)
        if self.dev_bypass_enabled and code_value == "99999":
            return True
        push_result = await self._verify_sms_via_push(phone_value, code_value)
        if push_result is True:
            return True
        cached = await self.cache.pop_json("phone:%s" % phone_value)
        if not cached or str(cached.get("code") or "") != code_value:
            raise AuthError("验证码错误", 400)
        return True

    def phone_code_config(self) -> dict[str, Any]:
        return {
            "registrationRequired": False,
            "interval": self.sms_interval,
            "validTime": self.sms_valid_time,
            "debugBypass": self.dev_bypass_enabled,
        }

    async def send_mobile_bind_code(self, token: str, mobile: str) -> bool:
        mobile_value = str(mobile or "").strip()
        if not re.fullmatch(r"1\d{10}", mobile_value):
            raise AuthError("非法手机号", 400)
        user_id = await self._current_user_id(token)
        account = await self.repository.find_account(mobile_value, "mobile", self.account_domain)
        if account and int(account.get("user_id") or 0) != user_id:
            raise AuthError("手机号已绑定其他用户", 409)
        users = await self.repository.find_users_by_mobile(mobile_value)
        if any(int(row.get("user_id") or 0) != user_id for row in users):
            raise AuthError("手机号已绑定其他用户", 409)
        if self.dev_bypass_enabled:
            return True
        result = await self._send_sms_via_push(mobile_value)
        legacy_pin = str(result.get("pin") or "").strip()
        if legacy_pin:
            await self.cache.set_json(
                "phone:%s" % mobile_value,
                {"code": legacy_pin},
                self.sms_valid_time,
            )
        return True

    async def bind_mobile(self, token: str, mobile: str, code: str) -> dict[str, str]:
        mobile_value = str(mobile or "").strip()
        user_id = await self._current_user_id(token)
        await self.verify_phone_code(mobile_value, code)
        password_hash = await self._identity_password_hash(user_id)
        try:
            await self.repository.bind_mobile_account(
                user_id, mobile_value, self.account_domain, password_hash
            )
        except ValueError as exc:
            raise AuthError(str(exc), 409) from exc
        return {"mobile": mobile_value}

    async def send_email_bind_code(self, token: str, email: str) -> bool:
        email_value = _email_value(email)
        user_id = await self._current_user_id(token)
        account = await self.repository.find_account(email_value, "email", self.account_domain)
        if account and int(account.get("user_id") or 0) != user_id:
            raise AuthError("邮箱已绑定其他用户", 409)
        users = await self.repository.find_users_by_email(email_value)
        if any(int(row.get("user_id") or 0) != user_id for row in users):
            raise AuthError("邮箱已绑定其他用户", 409)
        code = "99999" if self.dev_bypass_enabled else "%06d" % secrets.randbelow(1_000_000)
        if not self.dev_bypass_enabled:
            await self._send_email_via_push(token, user_id, email_value, code)
        await self.cache.set_json(
            self._email_bind_cache_key(user_id, email_value),
            {"code": code},
            self.sms_valid_time,
        )
        return True

    async def bind_email(self, token: str, email: str, code: str) -> dict[str, str]:
        email_value = _email_value(email)
        code_value = str(code or "").strip()
        if not code_value:
            raise AuthError("邮箱验证码不能为空", 400)
        user_id = await self._current_user_id(token)
        if not (self.dev_bypass_enabled and code_value == "99999"):
            cached = await self.cache.pop_json(self._email_bind_cache_key(user_id, email_value))
            if not cached or not secrets.compare_digest(str(cached.get("code") or ""), code_value):
                raise AuthError("邮箱验证码错误或已过期", 400)
        password_hash = await self._identity_password_hash(user_id)
        try:
            await self.repository.bind_email_account(
                user_id, email_value, self.account_domain, password_hash
            )
        except ValueError as exc:
            raise AuthError(str(exc), 409) from exc
        return {"email": email_value}

    async def _current_user_id(self, token: str) -> int:
        identity = await self.userinfo(token)
        user_id = int(identity.get("userId") or 0)
        if not user_id:
            raise AuthError("当前登录用户无效", 401)
        return user_id

    async def _identity_password_hash(self, user_id: int) -> str:
        accounts = await self.repository.password_accounts_for_user(user_id)
        password_hash = str(accounts[0].get("password") or "") if accounts else ""
        if not password_hash:
            raise AuthError("当前账号缺少可复用的密码凭证", 409)
        return password_hash

    @staticmethod
    def _email_bind_cache_key(user_id: int, email: str) -> str:
        return "email_bind:%s:%s" % (user_id, hashlib.sha256(email.encode()).hexdigest())

    async def _send_sms_via_push(self, phone: str) -> dict[str, Any]:
        base_url = await self._push_base_url()
        if not base_url:
            raise AuthError("短信通知通道未启用", 503)
        params = {"phoneNumber": phone}
        client = self.http_client
        close_client = False
        if client is None:
            client = httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=3.0), trust_env=False)
            close_client = True
        try:
            response = await client.post(base_url + "/pin/send", params=params)
        except httpx.HTTPError as exc:
            raise AuthError("短信通知通道调用失败: %s" % exc, 503) from exc
        finally:
            if close_client:
                await client.aclose()
        if response.status_code >= 400:
            raise AuthError("短信通知通道调用失败: HTTP %s" % response.status_code, 503)
        try:
            body = response.json()
        except ValueError as exc:
            raise AuthError("短信通知通道响应异常", 503) from exc
        if body.get("success") is False:
            raise AuthError(str(body.get("message") or "短信通知通道调用失败"), int(body.get("code") or 503))
        result = body.get("result") if isinstance(body.get("result"), Mapping) else {}
        sms_code = str(result.get("Code") or result.get("code") or "").upper()
        if sms_code and sms_code != "OK":
            message = str(result.get("Message") or result.get("message") or sms_code)
            raise AuthError("短信发送失败: %s" % message, 503)
        return dict(result)

    async def _verify_sms_via_push(self, phone: str, code: str) -> bool | None:
        base_url = await self._push_base_url()
        if not base_url:
            return None
        client = self.http_client
        close_client = False
        if client is None:
            client = httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=3.0), trust_env=False)
            close_client = True
        try:
            response = await client.post(
                base_url + "/pin/check",
                params={"phoneNumber": phone, "code": code},
            )
        except httpx.HTTPError as exc:
            raise AuthError("短信核验通道调用失败: %s" % exc, 503) from exc
        finally:
            if close_client:
                await client.aclose()
        if response.status_code in {404, 501}:
            return None
        if response.status_code >= 400:
            raise AuthError("短信核验通道调用失败: HTTP %s" % response.status_code, 503)
        try:
            body = response.json()
        except ValueError as exc:
            raise AuthError("短信核验通道响应异常", 503) from exc
        if body.get("success") is False:
            raise AuthError(str(body.get("message") or "短信核验通道调用失败"), 503)
        result = body.get("result") if isinstance(body.get("result"), Mapping) else {}
        verified = str(result.get("VerifyResult") or result.get("verifyResult") or "").upper()
        if verified == "PASS":
            return True
        if verified:
            raise AuthError("短信验证码错误或已过期", 400)
        return None

    async def _send_email_via_push(self, token: str, user_id: int, email: str, code: str) -> None:
        base_url = await self._push_base_url()
        if not base_url:
            raise AuthError("邮件通知通道未启用", 503)
        client = self.http_client
        close_client = False
        if client is None:
            client = httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=3.0), trust_env=False)
            close_client = True
        try:
            response = await client.post(
                base_url + "/notification/send/email",
                headers={"Authorization": "Bearer " + token},
                json={
                    "recUserId": user_id,
                    "receiver": email,
                    "title": "JBM 邮箱绑定验证码",
                    "content": "您的邮箱绑定验证码是 %s，%s 秒内有效。" % (code, self.sms_valid_time),
                    "pushWay": "email",
                    "syncDelivery": True,
                    "showInMessageCenter": False,
                },
            )
        except httpx.HTTPError as exc:
            raise AuthError("邮件通知通道调用失败: %s" % exc, 503) from exc
        finally:
            if close_client:
                await client.aclose()
        if response.status_code >= 400:
            raise AuthError("邮件通知通道调用失败: HTTP %s" % response.status_code, 503)
        try:
            body = response.json()
        except ValueError as exc:
            raise AuthError("邮件通知通道响应异常", 503) from exc
        if body.get("success") is False:
            raise AuthError(str(body.get("message") or "邮件通知通道调用失败"), 503)

    async def _push_base_url(self) -> str:
        if self.sms_push_base_url:
            return self.sms_push_base_url
        if self.discovery is not None:
            try:
                instance = await self.discovery.choose_instance(self.sms_push_service)
            except Exception as exc:
                logger.warning("Push service discovery failed: %s", exc)
                instance = None
            if instance:
                host = instance.get("ip") or instance.get("host")
                port = instance.get("port")
                if host and port:
                    return "http://%s:%s" % (host, port)
        return ""

    async def register(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client = await self._resolve_user_flow_client(form)
        username = str(form.get("userName") or form.get("username") or "").strip()
        password = str(form.get("password") or "")
        vcode = str(form.get("vcode") or "")
        display_name = str(form.get("nickName") or form.get("nick_name") or username).strip()
        tenant_name = f"{display_name}的账号空间"
        organization_type = "account"
        if not username:
            raise AuthError("用户名不能为空", 400)
        if str(form.get("mobile") or "").strip() or str(form.get("email") or "").strip():
            raise AuthError("手机号和邮箱请登录后在个人中心验证绑定", 400)
        registration = client.get("registration") or {}
        if not registration.get("enabled"):
            raise AuthError("该应用未开放用户注册", 403)
        if registration.get("mode") != "tenant":
            raise AuthError("该应用注册模式暂不支持", 400)
        if len(tenant_name) > 128:
            raise AuthError("组织名称不能超过128个字符", 400)
        default_role_code = str(registration.get("defaultRoleCode") or "").strip()
        app_id = int(client.get("appId") or 0)
        if not app_id or not default_role_code:
            raise AuthError("应用注册配置不完整", 503)
        await self.verify_captcha(vcode)
        if _truthy(form.get("password_encrypted")) or _looks_like_ciphertext(password):
            password = _decrypt_password(password, str(client.get("privateKey") or ""))
        try:
            validate_password(password, self.password_policy)
        except ValueError as exc:
            raise AuthError(str(exc), 400) from exc
        password_hash = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
        try:
            return await self.repository.create_tenant_account(
                app_id=app_id,
                default_role_code=default_role_code,
                tenant_name=tenant_name,
                org_type=organization_type,
                username=username,
                password_hash=password_hash,
                nick_name=str(form.get("nickName") or form.get("nick_name") or "") or None,
                email=None,
                mobile=None,
                domain=self.account_domain,
            )
        except ValueError as exc:
            raise AuthError(str(exc), 409) from exc

    async def create_qr_login(
        self,
        client_id: str,
        redirect_uri: str,
        width: int = 200,
        height: int = 200,
        code_challenge: str = "",
        code_challenge_method: str = "",
    ) -> dict[str, str]:
        client = await self.repository.find_client(str(client_id or "").strip())
        if not client:
            raise AuthError("客户端不存在", 401)
        self._validate_redirect_uri(client, str(redirect_uri or "").strip())
        challenge, method = self._validate_pkce_request(
            {"code_challenge": code_challenge, "code_challenge_method": code_challenge_method}
        )
        code = secrets.token_urlsafe(24)
        state = secrets.token_urlsafe(12)
        target = _qr_scan_url(redirect_uri, code, state)
        await self.cache.set_json(
            "qr:%s" % code,
            {
                "clientId": client_id,
                "redirectUri": redirect_uri,
                "state": state,
                "confirmState": 0,
                "codeChallenge": challenge,
                "codeChallengeMethod": method,
            },
            300,
        )
        return {
            "image": _qr_svg_base64(target, max(int(width or 200), 120), max(int(height or 200), 120)),
            "code": code,
            "state": state,
            "scanUrl": target,
        }

    async def qr_state(self, code: str) -> int | dict[str, Any]:
        cached = await self.cache.get_json("qr:%s" % str(code or ""))
        if not cached:
            raise AuthError("二维码不存在或已过期", 404)
        if int(cached.get("confirmState") or 0) == 2:
            code_response = cached.get("codeResponse")
            if isinstance(code_response, Mapping):
                return dict(code_response)
            token_response = cached.get("tokenResponse")
            if isinstance(token_response, Mapping):
                return dict(token_response)
        return int(cached.get("confirmState") or 0)

    async def mark_qr_scanned(self, code: str) -> int:
        cached = await self.cache.get_json("qr:%s" % str(code or ""))
        if not cached:
            raise AuthError("二维码不存在或已过期", 404)
        cached["confirmState"] = 1
        await self.cache.set_json("qr:%s" % str(code or ""), cached, 300)
        return 1

    async def confirm_qr_login(self, code: str, bearer_token: str) -> dict[str, Any]:
        token = str(bearer_token or "").strip()
        if token.lower().startswith("bearer "):
            token = token.split(None, 1)[1].strip()
        if not token:
            raise AuthError("未提供access_token", 401)
        cached = await self.cache.get_json("qr:%s" % str(code or ""))
        if not cached:
            raise AuthError("二维码不存在或已过期", 404)
        claims = self.signer.verify(token)
        await self._require_active_access(token, claims)
        client = await self.repository.find_client(str(cached.get("clientId") or ""))
        if not client:
            raise AuthError("客户端无效", 401)
        user_id = int(claims.get("user_id") or 0)
        user = await self.repository.find_user(user_id)
        if not user or not user_is_active(user):
            raise AuthError("用户已被禁用", 403)
        redirect_uri = str(cached.get("redirectUri") or "")
        if not redirect_uri:
            raise AuthError("redirect_uri不能为空", 400)
        source_session = await self.cache.get_json("access:" + _hash_token(token)) or {}
        redirect_url = await self._authorization_redirect(
            client,
            redirect_uri,
            {
                "userId": user_id,
                "username": claims.get("username") or user.get("user_name"),
                "account": source_session.get("account"),
                "accountType": source_session.get("accountType"),
                "accountVersion": source_session.get("accountVersion"),
                "mustChangePassword": bool(claims.get("must_change_password")),
                "scope": str(claims.get("scope") or "all"),
                "codeChallenge": cached.get("codeChallenge"),
                "codeChallengeMethod": cached.get("codeChallengeMethod"),
            },
            str(cached.get("state") or ""),
        )
        parsed_code = parse_qs(urlparse(redirect_url).query).get("code", [""])[0]
        cached["confirmState"] = 2
        cached["codeResponse"] = {
            "code": parsed_code,
            "redirectUri": redirect_uri,
            "state": cached.get("state"),
            "location": redirect_url,
        }
        await self.cache.set_json("qr:%s" % str(code or ""), cached, 300)
        return dict(cached["codeResponse"])

    async def _require_client(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        client_secret = str(form.get("client_secret") or form.get("clientSecret") or "")
        client = await self.repository.find_client(client_id)
        if not client or not _secret_matches(
            client_secret,
            str(client.get("clientSecret") or ""),
            self.allow_plaintext_secrets,
        ):
            raise AuthError("客户端认证失败", 401, "invalid_client")
        return client

    def _validate_redirect_uri(self, client: Mapping[str, Any], redirect_uri: str) -> None:
        registered = {str(item) for item in client.get("redirectUris") or [] if str(item)}
        parsed = urlparse(redirect_uri)
        same_origin_path = (
            not parsed.scheme
            and not parsed.netloc
            and redirect_uri.startswith("/")
            and not redirect_uri.startswith("//")
        )
        registered_same_origin_paths = {
            urlparse(item).path
            for item in registered
            if item.startswith("/") and not item.startswith("//")
        }
        if redirect_uri not in registered and not (
            same_origin_path and parsed.path in registered_same_origin_paths
        ):
            raise AuthError("redirect_uri未登记", 400, "invalid_request")
        if same_origin_path:
            return
        if self.require_https_redirects and urlparse(redirect_uri).scheme.lower() != "https":
            raise AuthError("redirect_uri必须使用HTTPS", 400, "invalid_request")

    def _validate_pkce_request(self, form: Mapping[str, Any]) -> tuple[str, str]:
        challenge = str(form.get("code_challenge") or form.get("codeChallenge") or "").strip()
        method = str(form.get("code_challenge_method") or form.get("codeChallengeMethod") or "").strip()
        if not challenge and not self.require_pkce:
            return "", ""
        if method != "S256" or not re.fullmatch(r"[A-Za-z0-9._~-]{43,128}", challenge):
            raise AuthError("必须使用有效的PKCE S256 code_challenge", 400, "invalid_request")
        return challenge, method

    def _verify_pkce(self, grant: Mapping[str, Any], form: Mapping[str, Any]) -> None:
        challenge = str(grant.get("codeChallenge") or "")
        method = str(grant.get("codeChallengeMethod") or "")
        if not challenge and not self.require_pkce:
            return
        verifier = str(form.get("code_verifier") or form.get("codeVerifier") or "").strip()
        if method != "S256" or not re.fullmatch(r"[A-Za-z0-9._~-]{43,128}", verifier):
            raise AuthError("code_verifier无效", 400, "invalid_grant")
        actual = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode("ascii")).digest()).decode("ascii").rstrip("=")
        if not hmac.compare_digest(actual, challenge):
            raise AuthError("PKCE校验失败", 400, "invalid_grant")

    def _validate_token_client(self, client: Mapping[str, Any], form: Mapping[str, Any]) -> None:
        if client.get("publicClient"):
            return
        supplied = str(form.get("client_secret") or form.get("clientSecret") or "")
        if not supplied or not _secret_matches(
            supplied,
            str(client.get("clientSecret") or ""),
            self.allow_plaintext_secrets,
        ):
            raise AuthError("客户端认证失败", 401, "invalid_client")

    async def _resolve_user_flow_client(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        if not client_id:
            raise AuthError("client_id不能为空", 400, "invalid_request")
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端不存在", 401, "invalid_client")
        supplied_secret = str(form.get("client_secret") or form.get("clientSecret") or "")
        if supplied_secret and not _secret_matches(
            supplied_secret,
            str(client.get("clientSecret") or ""),
            self.allow_plaintext_secrets,
        ):
            raise AuthError("客户端认证失败", 401, "invalid_client")
        return client

    async def _issue_user_token(
        self,
        client: Mapping[str, Any],
        account: Mapping[str, Any],
        user: Mapping[str, Any],
        scope: str,
        family_id: str | None = None,
    ) -> dict[str, Any]:
        user_id = int(user["user_id"])
        app_id = int(client.get("appId") or 0)
        username = str(user.get("user_name") or account.get("account") or user_id)
        tenant_id = int(user.get("company_id") or 0)
        if username != "admin" and not await self.repository.tenant_app_enabled(tenant_id, app_id):
            raise AuthError("当前租户未开通该应用", 403, "access_denied")
        roles = await self.repository.user_roles(user_id, app_id, tenant_id)
        role_codes = sorted({str(row["role_code"]) for row in roles if row.get("role_code")})
        role_ids = {int(row["role_id"]) for row in roles if row.get("role_id") is not None}
        root = username == "admin" or 1 in role_ids
        permissions = await self.repository.user_authorities(user_id, root, app_id, tenant_id)
        now = int(time.time())
        refresh_token = secrets.token_urlsafe(36)
        refresh_hash = _hash_token(refresh_token)
        family_id = family_id or secrets.token_urlsafe(24)
        if await self.cache.get_json("refresh_family_revoked:" + family_id):
            raise AuthError("登录会话已失效，请重新登录", 401)
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
            "root": root,
            "scope": scope,
            "must_change_password": bool(account.get("must_change_password")),
            "iat": now,
            "nbf": now,
            "exp": now + self.access_seconds,
            "jti": jti,
        }
        access_token = self.signer.sign(claims)
        access_key = _hash_token(access_token)
        refresh_state = {
            "userId": user_id,
            "username": username,
            "account": account.get("account") if account.get("account_type") else None,
            "accountType": account.get("account_type"),
            "accountVersion": account.get("credential_version") or (
                _account_version(account) if "password" in account or "update_time" in account else None
            ),
            "clientId": client.get("clientId"),
            "scope": scope,
            "mustChangePassword": bool(account.get("must_change_password")),
            "familyId": family_id,
            "accessKey": access_key,
            "accessJti": jti,
            "accessExpiresAt": now + self.access_seconds,
        }
        current_account = await self._require_account_active(refresh_state)
        if current_account:
            refresh_state["accountVersion"] = _account_version(current_account)
        await self.cache.set_json(
            "refresh:" + refresh_hash,
            refresh_state,
            self.refresh_seconds,
        )
        await self.cache.set_json(
            "refresh_family:" + family_id,
            {
                "refreshHash": refresh_hash,
                "accessKey": access_key,
                "accessJti": jti,
                "accessExpiresAt": now + self.access_seconds,
            },
            self.refresh_seconds,
        )
        await self._record_online_session(access_token, claims, user, client, family_id, refresh_state)
        if await self.cache.get_json("refresh_family_revoked:" + family_id):
            await self._revoke_refresh_family(family_id)
            raise AuthError("登录会话已失效", 400, "invalid_grant")
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
        family_id: str,
        refresh_state: Mapping[str, Any] | None = None,
    ) -> None:
        index_key = "user_sessions:%s:%s" % (claims.get("user_id"), claims.get("app_id"))
        async with self.cache.lock(index_key):
            await self._record_indexed_session(access_token, claims, user, client, family_id, index_key, refresh_state)

    async def _record_indexed_session(
        self, access_token: str, claims: Mapping[str, Any], user: Mapping[str, Any],
        client: Mapping[str, Any], family_id: str, index_key: str,
        refresh_state: Mapping[str, Any] | None = None,
    ) -> None:
        now = int(time.time())
        expires_at = int(claims.get("exp") or now)
        ttl = expires_at - now
        if ttl <= 0:
            return
        existing = []
        for member in await self.cache.index_members(index_key):
            row = await self.cache.get_json("family_session:" + member)
            if row and member != family_id and not await self.cache.get_json("refresh_family_revoked:" + member):
                existing.append(row)
        existing.sort(key=lambda row: str(row.get("loginTime") or ""), reverse=True)
        for stale in existing[self.max_sessions_per_user - 1 :]:
            stale_family = str(stale.get("familyId") or "")
            if stale_family:
                await self._revoke_refresh_family(stale_family)
            elif stale.get("tokenId"):
                await self.revoke_access_session(str(stale["tokenId"]))
        row = {
            "tokenId": _hash_token(access_token),
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
            "accessExpiresAt": expires_at,
            "familyId": family_id,
            "account": (refresh_state or {}).get("account"),
            "accountType": (refresh_state or {}).get("accountType"),
            "accountVersion": (refresh_state or {}).get("accountVersion"),
        }
        token_hash = _hash_token(access_token)
        await self.cache.set_json("access:" + token_hash, row, ttl)
        await self.cache.set_json("session_family:" + token_hash, {"familyId": family_id}, max(ttl, self.refresh_seconds))
        session_ttl = max(ttl, self.refresh_seconds)
        await self.cache.set_json("family_session:" + family_id, row, session_ttl)
        await self.cache.index_add(index_key, family_id, now + session_ttl)
        await self.cache.index_add("client_sessions:" + str(client.get("clientId") or ""), family_id, now + session_ttl)

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


def _account_version(account: Mapping[str, Any]) -> str:
    """Opaque server-side credential version; never embed hashes in JWTs or logs."""
    encoded = json.dumps(
        [str(account.get("password") or ""), str(account.get("update_time") or "")],
        ensure_ascii=False, separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _secret_matches(raw: str, stored: str, allow_plaintext: bool = False) -> bool:
    if not stored:
        return bool(allow_plaintext and hmac.compare_digest(raw, stored))
    decoded = _decode_app_secret(stored)
    if decoded is not None:
        return hmac.compare_digest(raw, decoded)
    if stored.startswith(("$2a$", "$2b$", "$2y$")):
        try:
            return bcrypt.checkpw(raw.encode("utf-8"), stored.encode("utf-8"))
        except ValueError:
            return False
    return bool(allow_plaintext and hmac.compare_digest(raw, stored))


def _decode_app_secret(stored: str) -> Optional[str]:
    if not stored.startswith(APP_SECRET_ENC_PREFIX):
        return None
    try:
        ciphertext = base64.b64decode(stored[len(APP_SECRET_ENC_PREFIX) :])
        decryptor = Cipher(algorithms.AES(APP_SECRET_AES_KEY), modes.ECB()).decryptor()
        padded = decryptor.update(ciphertext) + decryptor.finalize()
        pad_len = padded[-1]
        if pad_len < 1 or pad_len > 16:
            return None
        return padded[:-pad_len].decode("utf-8")
    except Exception:
        return None


def _hash_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def _timestamp(seconds: int) -> str:
    return datetime.fromtimestamp(seconds, tz=timezone.utc).isoformat().replace("+00:00", "Z")


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    return str(value or "").strip().lower() in {"1", "true", "yes", "y"}


def _email_value(value: str) -> str:
    email = str(value or "").strip().lower()
    if len(email) > 128 or not re.fullmatch(r"[^@\s]+@[^@\s]+\.[^@\s]+", email):
        raise AuthError("邮箱格式不正确", 400)
    return email


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


def _captcha_image_base64(code: str, width: int, height: int) -> str:
    try:
        from captcha.image import ImageCaptcha
        from PIL import ImageDraw

        width = max(int(width or 120), 80)
        height = max(int(height or 40), 32)
        font_sizes = tuple(range(max(20, int(height * 0.56)), max(21, int(height * 0.64)) + 1))
        captcha = ImageCaptcha(width=width, height=height, font_sizes=font_sizes)
        captcha.character_rotate = (-24, 24)
        captcha.character_warp_dx = (0.08, 0.18)
        captcha.character_warp_dy = (0.12, 0.22)
        captcha.word_offset_dx = 0.12
        captcha.word_space_probability = 1.0
        background = (248, 251, 255)
        text_color = (56, 80, 118)
        image = captcha.create_captcha_image(
            code,
            color=(96, 176, 255),
            background=background,
        )
        recolored = []
        image_data = image.get_flattened_data() if hasattr(image, "get_flattened_data") else image.getdata()
        for pixel in image_data:
            delta = sum(abs(pixel[index] - background[index]) for index in range(3))
            if delta <= 8:
                recolored.append(pixel)
                continue
            strength = min(0.72, max(0.22, delta / 320))
            recolored.append(
                tuple(int(background[index] * (1 - strength) + text_color[index] * strength) for index in range(3))
            )
        image.putdata(recolored)
        draw = ImageDraw.Draw(image)
        for _ in range(2):
            draw.line(
                (
                    random.randint(0, width // 3),
                    random.randint(0, height),
                    random.randint(width // 2, width),
                    random.randint(0, height),
                ),
                fill=(
                    random.randint(125, 175),
                    random.randint(155, 205),
                    random.randint(190, 230),
                ),
                width=1,
            )
        for _ in range(max(18, width * height // 260)):
            x = random.randint(0, width - 1)
            y = random.randint(0, height - 1)
            draw.point(
                (x, y),
                fill=(
                    random.randint(125, 205),
                    random.randint(145, 220),
                    random.randint(165, 235),
                ),
            )
        output = io.BytesIO()
        image.save(output, format="PNG")
        return "data:image/png;base64," + base64.b64encode(output.getvalue()).decode("ascii")
    except Exception:
        svg = _captcha_svg(code, width, height)
        return "data:image/svg+xml;base64," + base64.b64encode(svg.encode("utf-8")).decode("ascii")


def _captcha_svg(code: str, width: int, height: int) -> str:
    width = max(int(width or 120), 80)
    height = max(int(height or 40), 32)
    seed_text = "%s:%s:%s" % (code, width, height)
    seed = int(hashlib.sha1(seed_text.encode("utf-8")).hexdigest()[:8], 16)
    rng = random.Random(seed)
    palette = ["#1d4ed8", "#0f766e", "#7c3aed", "#be123c", "#0369a1"]
    accents = ["#93c5fd", "#99f6e4", "#c4b5fd", "#fecdd3", "#bae6fd"]
    dots = []
    for index in range(18):
        dots.append(
            '<circle cx="%s" cy="%s" r="%s" fill="%s" opacity="0.34"/>'
            % (
                rng.randint(4, width - 4),
                rng.randint(4, height - 4),
                rng.choice((0.8, 1.0, 1.2)),
                accents[index % len(accents)],
            )
        )
    curves = []
    for index in range(3):
        y1 = rng.randint(max(6, height // 5), max(7, height - height // 5))
        y2 = rng.randint(max(6, height // 5), max(7, height - height // 5))
        c1 = rng.randint(width // 5, width // 2)
        c2 = rng.randint(width // 2, max(width // 2 + 1, width - width // 6))
        curves.append(
            '<path d="M 6 %s C %s %s, %s %s, %s %s" fill="none" stroke="%s" '
            'stroke-width="1.2" stroke-linecap="round" opacity="0.28"/>'
            % (y1, c1, y2, c2, y1, width - 6, y2, accents[index % len(accents)])
        )
    chars = []
    count = max(len(code), 1)
    step = width / (count + 0.7)
    font_size = min(max(int(height * 0.62), 21), max(22, int(step * 0.92)))
    for index, char in enumerate(code):
        x = int(step * (index + 0.85))
        y = int(height * 0.6 + rng.randint(-2, 2))
        rotate = rng.randint(-9, 9)
        chars.append(
            '<text x="%s" y="%s" text-anchor="middle" dominant-baseline="middle" '
            'transform="rotate(%s %s %s)" font-family="Inter, ui-monospace, SFMono-Regular, Menlo, Consolas, monospace" '
            'font-size="%s" font-weight="800" fill="%s">%s</text>'
            % (x, y, rotate, x, y, font_size, palette[index % len(palette)], html.escape(char))
        )
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="%s" height="%s" viewBox="0 0 %s %s">'
        '<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">'
        '<stop offset="0" stop-color="#f8fbff"/><stop offset="1" stop-color="#eef7ff"/>'
        "</linearGradient></defs>"
        '<rect x="0.5" y="0.5" width="%s" height="%s" rx="8" fill="url(#bg)" stroke="#dbeafe"/>'
        '<g style="user-select:none">%s%s%s</g>'
        "</svg>"
    ) % (
        width,
        height,
        width,
        height,
        width - 1,
        height - 1,
        "".join(dots),
        "".join(curves),
        "".join(chars),
    )


def _qr_scan_url(redirect_uri: str, code: str, state: str) -> str:
    params = urlencode({"code": code, "state": state})
    parsed = urlparse(str(redirect_uri or ""))
    if parsed.scheme and parsed.netloc:
        return "%s://%s/qr-login?%s" % (parsed.scheme, parsed.netloc, params)
    return "/qr-login?%s" % params


def _qr_svg_base64(data: str, width: int, height: int) -> str:
    try:
        import qrcode

        image = qrcode.make(data)
        image = image.resize((width, height))
        output = io.BytesIO()
        image.save(output, format="PNG")
        return "data:image/png;base64," + base64.b64encode(output.getvalue()).decode("ascii")
    except Exception:
        escaped = html.escape(data)
        svg = (
            '<svg xmlns="http://www.w3.org/2000/svg" width="%s" height="%s">'
            '<rect width="100%%" height="100%%" fill="#fff"/>'
            '<text x="50%%" y="50%%" text-anchor="middle" dominant-baseline="middle" '
            'font-family="monospace" font-size="10">%s</text></svg>'
        ) % (width, height, escaped)
        return "data:image/svg+xml;base64," + base64.b64encode(svg.encode("utf-8")).decode("ascii")
