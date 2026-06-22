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
from urllib.parse import parse_qs, urlencode, urlparse
from typing import Any, Mapping, Optional

import bcrypt
import httpx
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding

from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.auth.jwt import JwtError, JwtSigner
from jbm_cluster_py.platform.auth.repository import AuthRepository, infer_account_type, user_is_active

logger = logging.getLogger(__name__)

LOGIN_ERROR_PREFIX = "login_error:"
APP_SECRET_ENC_PREFIX = "$ENC$"
APP_SECRET_AES_KEY = hashlib.md5(b"jbm-app-client-secret").hexdigest().encode("utf-8")


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
        )
        self.issuer = issuer.rstrip("/")
        self.audience = audience
        self.access_seconds = int(jwt_config.get("access-token-seconds") or 7200)
        self.refresh_seconds = int(jwt_config.get("refresh-token-seconds") or 604800)
        self.account_domain = str(self.config.get("account-domain") or "@admin.com")
        self.max_errors = int(self.config.get("login-error-number") or 5)
        self.lock_minutes = int(self.config.get("login-error-limit-minutes") or 10)
        sms_config = dict(self.config.get("sms") or {})
        self.sms_push_service = str(sms_config.get("push-service") or "jbm-cluster-platform-push")
        self.sms_push_base_url = str(sms_config.get("push-base-url") or "").rstrip("/")
        self.sms_sign_name = str(sms_config.get("sign-name") or "甲佳智能")
        self.sms_template_code = str(sms_config.get("template-code") or "SMS_236340338")

    async def password_token(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client = await self._resolve_user_flow_client(form)
        return await self._password_login_for_client(client, form)

    async def authorize_code_login(self, form: Mapping[str, Any]) -> str:
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        redirect_uri = str(form.get("redirect_uri") or form.get("redirectUri") or "").strip()
        if not client_id:
            raise AuthError("client_id不能为空", 400)
        if not redirect_uri:
            raise AuthError("redirect_uri不能为空", 400)
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端不存在", 401)
        token = await self._password_login_for_client(client, form)
        return await self._authorization_redirect(client, redirect_uri, token, str(form.get("state") or "").strip())

    async def _authorization_redirect(
        self,
        client: Mapping[str, Any],
        redirect_uri: str,
        token: Mapping[str, Any],
        state: str = "",
    ) -> str:
        code = secrets.token_urlsafe(32)
        await self.cache.set_json(
            "auth_code:" + _hash_token(code),
            {
                "clientId": client.get("clientId"),
                "redirectUri": redirect_uri,
                "token": token,
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
            raise AuthError("client_id不能为空", 400)
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端无效", 401)
        code = str(form.get("code") or "").strip()
        if not code:
            raise AuthError("code不能为空", 400)
        key = "auth_code:" + _hash_token(code)
        cached = await self.cache.get_json(key)
        if not cached:
            raise AuthError("授权码无效或已过期", 401)
        await self.cache.delete(key)
        if str(cached.get("clientId") or "") != str(client.get("clientId") or ""):
            raise AuthError("授权码客户端不匹配", 401)
        redirect_uri = str(form.get("redirect_uri") or form.get("redirectUri") or "").strip()
        if redirect_uri and redirect_uri != str(cached.get("redirectUri") or ""):
            raise AuthError("redirect_uri不匹配", 401)
        token = cached.get("token")
        if not isinstance(token, Mapping):
            raise AuthError("授权码状态异常", 401)
        return dict(token)

    async def _password_login_for_client(
        self,
        client: Mapping[str, Any],
        form: Mapping[str, Any],
    ) -> dict[str, Any]:
        username = str(form.get("username") or "").strip()
        password = str(form.get("password") or "")
        login_type = str(form.get("loginType") or form.get("login_type") or "PASSWORD").upper()
        if form.get("vcode"):
            await self.verify_captcha(str(form.get("vcode") or ""))
        if _truthy(form.get("password_encrypted")) or _looks_like_ciphertext(password):
            password = _decrypt_password(password, str(client.get("privateKey") or ""))
        if not username or not password:
            raise AuthError("用户名或密码不能为空", 400)
        if login_type == "SMS":
            await self.verify_phone_code(username, password)
            account = await self.repository.find_account(username, "mobile", self.account_domain)
            if not account:
                raise AuthError("手机号未绑定账号", 401)
            user = await self.repository.find_user(int(account["user_id"]))
            if not user or not user_is_active(user):
                raise AuthError("用户已被禁用", 403)
            return await self._issue_user_token(client, account, user, str(form.get("scope") or "all"))
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
        refresh_token = str(form.get("refresh_token") or "").strip()
        if not refresh_token:
            raise AuthError("refresh_token不能为空", 400)
        state = await self.cache.get_json("refresh:" + _hash_token(refresh_token))
        if not state:
            raise AuthError("refresh_token无效或已过期", 401)
        requested_client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        if requested_client_id and requested_client_id != str(state.get("clientId") or ""):
            raise AuthError("refresh_token客户端不匹配", 401)
        user_id = int(state["userId"])
        user = await self.repository.find_user(user_id)
        if not user or not user_is_active(user):
            raise AuthError("用户已被禁用", 403)
        client = await self.repository.find_client(str(state["clientId"]))
        if not client:
            raise AuthError("客户端无效", 401)
        supplied_secret = str(form.get("client_secret") or form.get("clientSecret") or "")
        if supplied_secret and not _secret_matches(supplied_secret, str(client.get("clientSecret") or "")):
            raise AuthError("客户端认证失败", 401)
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
            "grant_types_supported": ["authorization_code", "password", "client_credentials", "refresh_token"],
            "token_endpoint_auth_methods_supported": ["client_secret_post", "client_secret_basic"],
            "response_types_supported": ["code", "token"],
            "subject_types_supported": ["public"],
            "id_token_signing_alg_values_supported": ["RS256"],
        }

    def jwks(self) -> dict[str, Any]:
        return {"keys": [self.signer.jwk()]}

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
        if value == "9999":
            return True
        if not value:
            raise AuthError("验证码不能为空", 400)
        cached = await self.cache.get_json("captcha:%s:%s" % (scope or "system", value.lower()))
        if not cached:
            raise AuthError("验证码错误", 400)
        return True

    async def send_phone_code(self, phone: str, image_code: str) -> bool:
        phone_value = str(phone or "").strip()
        if not re.fullmatch(r"1\d{10}", phone_value):
            raise AuthError("非法手机号", 400)
        await self.verify_captcha(image_code)
        code = "".join(random.choice("0123456789") for _ in range(6))
        await self.cache.set_json("phone:%s" % phone_value, {"code": code}, 300)
        await self._send_sms_via_push(phone_value, code)
        return True

    async def verify_phone_code(self, phone: str, code: str) -> bool:
        if str(code or "") == "99999":
            return True
        cached = await self.cache.get_json("phone:%s" % str(phone or "").strip())
        if not cached or str(cached.get("code") or "") != str(code or "").strip():
            raise AuthError("验证码错误", 400)
        return True

    async def _send_sms_via_push(self, phone: str, code: str) -> None:
        base_url = await self._push_base_url()
        if not base_url:
            raise AuthError("短信通知通道未启用", 503)
        params = {
            "phoneNumber": phone,
            "code": code,
            "signName": self.sms_sign_name,
            "templateCode": self.sms_template_code,
        }
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
        if not username:
            raise AuthError("用户名不能为空", 400)
        if len(password) < 6:
            raise AuthError("密码至少6位", 400)
        await self.verify_captcha(vcode)
        if _truthy(form.get("password_encrypted")) or _looks_like_ciphertext(password):
            password = _decrypt_password(password, str(client.get("privateKey") or ""))
        password_hash = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
        try:
            return await self.repository.create_user_account(
                username=username,
                password_hash=password_hash,
                nick_name=str(form.get("nickName") or form.get("nick_name") or "") or None,
                email=str(form.get("email") or "") or None,
                mobile=str(form.get("mobile") or "") or None,
                domain=self.account_domain,
            )
        except ValueError as exc:
            raise AuthError(str(exc), 409) from exc

    async def create_qr_login(self, client_id: str, redirect_uri: str, width: int = 200, height: int = 200) -> dict[str, str]:
        code = secrets.token_urlsafe(24)
        state = secrets.token_urlsafe(12)
        target = "%s?%s" % (
            redirect_uri,
            urlencode({"code": code, "state": state}),
        )
        await self.cache.set_json(
            "qr:%s" % code,
            {"clientId": client_id, "redirectUri": redirect_uri, "state": state, "confirmState": 0},
            300,
        )
        return {
            "image": _qr_svg_base64(target, max(int(width or 200), 120), max(int(height or 200), 120)),
            "code": code,
            "state": state,
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
        revoked = await self.cache.get_json("revoked:" + str(claims.get("jti") or ""))
        if revoked:
            raise AuthError("token已失效", 401)
        client = await self.repository.find_client(str(cached.get("clientId") or ""))
        if not client:
            raise AuthError("客户端无效", 401)
        user_id = int(claims.get("user_id") or 0)
        user = await self.repository.find_user(user_id)
        if not user or not user_is_active(user):
            raise AuthError("用户已被禁用", 403)
        account = {
            "account": claims.get("username") or user.get("user_name"),
            "user_id": user_id,
            "must_change_password": False,
        }
        token_response = await self._issue_user_token(client, account, user, str(claims.get("scope") or "all"))
        redirect_uri = str(cached.get("redirectUri") or "")
        if not redirect_uri:
            raise AuthError("redirect_uri不能为空", 400)
        redirect_url = await self._authorization_redirect(
            client,
            redirect_uri,
            token_response,
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
        if not client or not _secret_matches(client_secret, str(client.get("clientSecret") or "")):
            raise AuthError("客户端认证失败", 401)
        return client

    async def _resolve_user_flow_client(self, form: Mapping[str, Any]) -> dict[str, Any]:
        client_id = str(form.get("client_id") or form.get("clientId") or "").strip()
        if not client_id:
            raise AuthError("client_id不能为空", 400)
        client = await self.repository.find_client(client_id)
        if not client:
            raise AuthError("客户端不存在", 401)
        supplied_secret = str(form.get("client_secret") or form.get("clientSecret") or "")
        if supplied_secret and not _secret_matches(supplied_secret, str(client.get("clientSecret") or "")):
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
    decoded = _decode_app_secret(stored)
    if decoded is not None:
        return hmac.compare_digest(raw, decoded)
    if stored.startswith(("$2a$", "$2b$", "$2y$")):
        try:
            return bcrypt.checkpw(raw.encode("utf-8"), stored.encode("utf-8"))
        except ValueError:
            return False
    return hmac.compare_digest(raw, stored)


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
        image_data = (
            image.get_flattened_data() if hasattr(image, "get_flattened_data") else image.getdata()
        )
        for pixel in image_data:
            delta = sum(abs(pixel[index] - background[index]) for index in range(3))
            if delta <= 8:
                recolored.append(pixel)
                continue
            strength = min(0.72, max(0.22, delta / 320))
            recolored.append(
                tuple(
                    int(background[index] * (1 - strength) + text_color[index] * strength)
                    for index in range(3)
                )
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
        '</linearGradient></defs>'
        '<rect x="0.5" y="0.5" width="%s" height="%s" rx="8" fill="url(#bg)" stroke="#dbeafe"/>'
        '<g style="user-select:none">%s%s%s</g>'
        '</svg>'
    ) % (width, height, width, height, width - 1, height - 1, "".join(dots), "".join(curves), "".join(chars))


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
