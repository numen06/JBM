"""Account-bound WebAuthn and OpenSSH credentials; private keys never reach Auth."""

from __future__ import annotations

import base64
import calendar
import hashlib
import json
import secrets
import time
from datetime import UTC, datetime
from typing import Any
from urllib.parse import urlsplit

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from sqlalchemy import (
    BigInteger,
    Column,
    Integer,
    MetaData,
    String,
    Table,
    Text,
    delete,
    insert,
    select,
    update,
)
from sqlalchemy.exc import IntegrityError
from webauthn import (
    generate_authentication_options,
    generate_registration_options,
    options_to_json,
    verify_authentication_response,
    verify_registration_response,
)
from webauthn.helpers.structs import (
    AuthenticatorSelectionCriteria,
    PublicKeyCredentialDescriptor,
    ResidentKeyRequirement,
    UserVerificationRequirement,
)

from .repository import user_is_active
from .service import AuthError, _account_version
from .ssh_signature import canonical_public_key, verify_ssh_signature

metadata = MetaData()
credentials = Table(
    "base_auth_credential",
    metadata,
    Column("id", String(64), primary_key=True),
    Column("user_id", BigInteger, nullable=False, index=True),
    Column("kind", String(16), nullable=False),
    Column("name", String(80), nullable=False),
    Column("data", Text, nullable=False),
    Column("sign_count", Integer, nullable=False, default=0),
    Column("created_at", BigInteger, nullable=False),
    Column("last_used_at", BigInteger, nullable=True),
)


def encode(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).decode().rstrip("=")


def decode(value: str) -> bytes:
    return base64.b64decode(value + "=" * (-len(value) % 4), altchars=b"-_", validate=True)


def file_public_key(value: str) -> str:
    if not isinstance(value, str) or len(value) > 4096 or "PRIVATE" in value:
        raise ValueError("请提供有效的 RSA 公钥")
    key = serialization.load_pem_public_key(value.encode("ascii"))
    if (
        not isinstance(key, rsa.RSAPublicKey)
        or not 2048 <= key.key_size <= 4096
        or key.public_numbers().e != 65537
    ):
        raise ValueError("密钥文件需要 2048–4096 位 RSA 公钥")
    return key.public_bytes(
        serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo
    ).decode()


def file_key_id(public_key: str) -> str:
    return hashlib.sha256(("FILE_KEY\0" + public_key).encode()).hexdigest()


def verify_file_signature(public_key: str, message: str, signature: str) -> None:
    key = serialization.load_pem_public_key(public_key.encode())
    key.verify(
        base64.b64decode(signature, validate=True),
        message.encode(),
        padding.PKCS1v15(),
        hashes.SHA256(),
    )


def six_months_after(created_at: int) -> int:
    issued = datetime.fromtimestamp(created_at, UTC)
    month = issued.month + 6
    year = issued.year + (month - 1) // 12
    month = (month - 1) % 12 + 1
    return int(
        issued.replace(
            year=year, month=month, day=min(issued.day, calendar.monthrange(year, month)[1])
        ).timestamp()
    )


def credential_expiration(row: Any) -> int | None:
    return json.loads(row["data"]).get("expiresAt", six_months_after(int(row["created_at"])))


class KeyLoginService:
    def __init__(self, auth: Any) -> None:
        self.auth = auth
        self.cache = auth.cache
        self.config = dict(auth.config.get("key-login") or {})
        self.enabled = self.config.get("enabled") in (True, "true")
        self.rp_id = str(self.config.get("rp-id") or "")
        self.origins = self.config.get("origins") or []
        if self.enabled:
            if not self.rp_id or not isinstance(self.origins, list) or not self.origins:
                raise ValueError("key-login requires rp-id and explicit origins")
            for origin in self.origins:
                parsed = urlsplit(origin)
                host = parsed.hostname or ""
                if (
                    (
                        parsed.scheme != "https"
                        and not (parsed.scheme == "http" and host == "localhost")
                    )
                    or (host != self.rp_id and not host.endswith("." + self.rp_id))
                    or parsed.path
                    or parsed.query
                    or parsed.fragment
                    or parsed.username
                    or parsed.password
                ):
                    raise ValueError("Invalid key-login origin / rp-id")

    @property
    def engine(self):
        return self.auth.repository.engine

    def require_enabled(self) -> None:
        if not self.enabled:
            raise AuthError("密钥登录尚未配置，请联系管理员", 503)

    async def start(self) -> None:
        if not self.enabled:
            return
        if self.auth.repository._sqlite:
            async with self.engine.begin() as conn:
                await conn.run_sync(metadata.create_all)
        elif not await self.auth.repository.has_table(credentials.name):
            raise RuntimeError("Apply Center migration 20260928_24 before enabling key-login")

    async def owner(self, token: str, *, allow_password_update: bool = False) -> tuple[dict, dict]:
        identity = await self.auth.userinfo(token)
        if identity.get("userId") is None or (
            identity.get("mustChangePassword") and not allow_password_update
        ):
            raise AuthError("请先完成账号验证和密码更新", 403)
        return await self.account(int(identity["userId"]))

    async def account(self, user_id: int) -> tuple[dict, dict]:
        user = await self.auth.repository.find_user(user_id)
        accounts = await self.auth.repository.password_accounts_for_user(user_id)
        account = next(
            (
                row
                for row in accounts
                if row.get("domain") == self.auth.account_domain
                and int(row.get("status") or 0) == 1
            ),
            None,
        )
        if not user or not user_is_active(user) or not account:
            raise AuthError("账号不可用", 403)
        account = await self.auth.repository.find_account(
            account["account"], account["account_type"], self.auth.account_domain
        )
        if not account or int(account.get("status") or 0) != 1:
            raise AuthError("账号不可用", 403)
        return account, user

    async def challenge(self, data: dict) -> dict:
        challenge_id = secrets.token_urlsafe(32)
        data["challenge"] = encode(secrets.token_bytes(32))
        await self.cache.set_json("key-challenge:" + challenge_id, data, 120)
        return {"challengeId": challenge_id, **data}

    async def consume(self, payload: dict, purpose: str, kind: str) -> dict:
        challenge_id = str(payload.get("challengeId") or "")
        if len(challenge_id) > 100:
            raise AuthError("无效的密钥挑战", 400)
        state = await self.cache.pop_json("key-challenge:" + challenge_id)
        if not state or state.get("purpose") != purpose or state.get("kind") != kind:
            raise AuthError("密钥验证已过期或已使用，请重试", 401)
        return state

    async def list(self, token: str) -> list[dict]:
        self.require_enabled()
        account, user = await self.owner(token, allow_password_update=True)
        async with self.engine.connect() as conn:
            rows = (
                (
                    await conn.execute(
                        select(credentials).where(credentials.c.user_id == user["user_id"])
                    )
                )
                .mappings()
                .all()
            )
        result = []
        for row in rows:
            item = {
                **{key: row[key] for key in ("id", "kind", "name", "created_at", "last_used_at")},
                "expires_at": credential_expiration(row),
                "is_valid": True,
                "invalid_reason": "",
            }
            try:
                self.require_current_key(row, account)
            except (AuthError, ValueError) as exc:
                item.update(is_valid=False, invalid_reason=str(exc))
            result.append(item)
        return result

    async def remove(self, token: str, key_id: str) -> None:
        self.require_enabled()
        _, user = await self.owner(token, allow_password_update=True)
        async with self.cache.lock("credential:" + key_id):
            async with self.engine.begin() as conn:
                await conn.execute(
                    delete(credentials).where(
                        credentials.c.id == key_id, credentials.c.user_id == user["user_id"]
                    )
                )

    async def password_verification(self, token: str) -> dict:
        state = await self.cache.get_json(
            "key-password:" + hashlib.sha256(token.encode()).hexdigest()
        )
        if not state or state.get("expiresAt", 0) <= time.time():
            raise AuthError("密码验证已过期，请重新验证当前账号密码", 403)
        return {"expiresIn": max(0, int(state["expiresAt"] - time.time()))}

    async def verify_password(self, token: str, payload: dict) -> dict:
        self.require_enabled()
        account, _ = await self.owner(token, allow_password_update=True)
        if "password" not in payload:
            return await self.password_verification(token)
        await self.auth.verify_key_password(token, account, payload.get("password"))
        await self.cache.set_json(
            "key-password:" + hashlib.sha256(token.encode()).hexdigest(),
            {"expiresAt": time.time() + 60},
            60,
        )
        return {"expiresIn": 60}

    async def registration_options(self, token: str, payload: dict) -> dict:
        self.require_enabled()
        kind = str(payload.get("kind") or "PASSKEY")
        if kind not in {"PASSKEY", "SSH_KEY", "FILE_KEY"}:
            raise AuthError("不支持的密钥类型", 400)
        account, user = await self.owner(token, allow_password_update=kind == "FILE_KEY")
        if kind == "FILE_KEY":
            await self.password_verification(token)
        existing = await self.list(token)
        name = str(payload.get("name") or "我的密钥").strip() or "我的密钥"
        if len(name) > 80:
            raise AuthError("密钥名称不能超过 80 个字符", 400)
        if any(str(row["name"]).strip().casefold() == name.casefold() for row in existing):
            raise AuthError("密钥名称已存在，请下载已有密钥或使用其他名称", 409)
        if len(existing) >= 20:
            raise AuthError("最多绑定 20 个密钥，请先移除旧密钥", 400)
        validity = payload.get("validity", "six_months")
        if validity not in ("six_months", "permanent"):
            raise AuthError("请选择半年或长期有效", 400)
        created_at = int(time.time())
        data = {
            "purpose": "register",
            "kind": kind,
            "userId": int(user["user_id"]),
            "session": hashlib.sha256(token.encode()).hexdigest(),
            "name": name,
            "handle": encode(secrets.token_bytes(32)),
            "createdAt": created_at,
            "expiresAt": six_months_after(created_at) if validity == "six_months" else None,
        }
        if kind == "SSH_KEY":
            data["publicKey"] = canonical_public_key(str(payload.get("publicKey") or ""))
        elif kind == "FILE_KEY":
            data["publicKey"] = file_public_key(payload.get("publicKey"))
        state = await self.challenge(data)
        if kind == "FILE_KEY":
            return {
                **self.file_options(state),
                "account": str(account["account"]),
                "keyId": file_key_id(state["publicKey"]),
                "createdAt": state["createdAt"],
                "expiresAt": state["expiresAt"],
            }
        if kind == "SSH_KEY":
            return self.ssh_options(state)
        async with self.engine.connect() as conn:
            rows = (
                (
                    await conn.execute(
                        select(credentials.c.data).where(
                            credentials.c.user_id == user["user_id"],
                            credentials.c.kind == "PASSKEY",
                        )
                    )
                )
                .scalars()
                .all()
            )
        options = generate_registration_options(
            rp_id=self.rp_id,
            rp_name=str(self.config.get("rp-name") or "JBM 平台"),
            user_id=decode(state["handle"]),
            user_name=str(account["account"]),
            challenge=decode(state["challenge"]),
            timeout=120000,
            authenticator_selection=AuthenticatorSelectionCriteria(
                resident_key=ResidentKeyRequirement.REQUIRED,
                user_verification=UserVerificationRequirement.REQUIRED,
            ),
            exclude_credentials=[
                PublicKeyCredentialDescriptor(id=decode(json.loads(row)["credentialId"]))
                for row in rows
            ],
        )
        return {
            "challengeId": state["challengeId"],
            "publicKey": json.loads(options_to_json(options)),
        }

    def ssh_options(self, state: dict) -> dict:
        # Domain, purpose, client and nonce are all covered by the OpenSSH signature.
        message = (
            json.dumps(
                {key: state.get(key) for key in ("purpose", "challenge", "clientId", "userId")},
                sort_keys=True,
            )
            + "\n"
            + self.rp_id
        )
        return {
            "challengeId": state.get("challengeId"),
            "message": message,
            "namespace": "jbm-key-login",
            "expiresIn": 120,
        }

    def file_options(self, state: dict) -> dict:
        message = (
            "jbm-file-key-v1\n"
            + json.dumps(
                {
                    key: state.get(key)
                    for key in ("purpose", "kind", "challenge", "clientId", "userId", "publicKey")
                },
                sort_keys=True,
                separators=(",", ":"),
            )
            + "\n"
            + self.rp_id
        )
        return {
            "challengeId": state.get("challengeId"),
            "message": message,
            "rpId": self.rp_id,
            "expiresIn": 120,
        }

    async def register(self, token: str, payload: dict) -> None:
        self.require_enabled()
        kind = str(payload.get("kind") or "PASSKEY")
        account, user = await self.owner(token, allow_password_update=kind == "FILE_KEY")
        state = await self.consume(payload, "register", kind)
        if (
            state["userId"] != user["user_id"]
            or state["session"] != hashlib.sha256(token.encode()).hexdigest()
        ):
            raise AuthError("密钥绑定会话不匹配", 401)
        if kind == "FILE_KEY":
            await self.password_verification(token)
        try:
            if kind == "PASSKEY":
                verified = verify_registration_response(
                    credential=payload["credential"],
                    expected_challenge=decode(state["challenge"]),
                    expected_rp_id=self.rp_id,
                    expected_origin=self.origins,
                    require_user_verification=True,
                )
                data = {
                    "credentialId": encode(verified.credential_id),
                    "publicKey": encode(verified.credential_public_key),
                    "handle": state["handle"],
                }
                key_id = hashlib.sha256(verified.credential_id).hexdigest()
                count = verified.sign_count
            elif kind == "SSH_KEY":
                verify_ssh_signature(
                    state["publicKey"], self.ssh_options(state)["message"], payload["signature"]
                )
                data = {"publicKey": state["publicKey"]}
                key_id = hashlib.sha256(state["publicKey"].encode()).hexdigest()
                count = 0
            elif kind == "FILE_KEY":
                verify_file_signature(
                    state["publicKey"], self.file_options(state)["message"], payload["signature"]
                )
                data = {"publicKey": state["publicKey"]}
                key_id = file_key_id(state["publicKey"])
                count = 0
            else:
                raise ValueError("Unsupported key")
        except Exception as exc:
            raise AuthError("密钥验证失败", 401) from exc
        data["accountVersion"] = _account_version(account)
        data["expiresAt"] = state["expiresAt"]
        try:
            name_lock = (
                str(user["user_id"])
                + ":"
                + hashlib.sha256(state["name"].casefold().encode()).hexdigest()
            )
            async with self.cache.lock("credential-name:" + name_lock):
                async with self.engine.begin() as conn:
                    names = (
                        (
                            await conn.execute(
                                select(credentials.c.name).where(
                                    credentials.c.user_id == user["user_id"]
                                )
                            )
                        )
                        .scalars()
                        .all()
                    )
                    if any(
                        str(name).strip().casefold() == state["name"].casefold() for name in names
                    ):
                        raise AuthError("密钥名称已存在，请下载已有密钥或使用其他名称", 409)
                    await conn.execute(
                        insert(credentials).values(
                            id=key_id,
                            user_id=user["user_id"],
                            kind=kind,
                            name=state["name"],
                            data=json.dumps(data),
                            sign_count=count,
                            created_at=state["createdAt"],
                        )
                    )
        except IntegrityError as exc:
            raise AuthError("该密钥已绑定账号", 409) from exc

    async def login_options(self, payload: dict) -> dict:
        self.require_enabled()
        kind = str(payload.get("kind") or "PASSKEY")
        client_id = str(payload.get("client_id") or "")
        if kind not in {
            "PASSKEY",
            "SSH_KEY",
            "FILE_KEY",
        } or not await self.auth.repository.find_client(client_id):
            raise AuthError("无效的登录请求", 400)
        state = await self.challenge({"purpose": "login", "kind": kind, "clientId": client_id})
        if kind == "FILE_KEY":
            return self.file_options(state)
        if kind == "SSH_KEY":
            return self.ssh_options(state)
        options = generate_authentication_options(
            rp_id=self.rp_id,
            challenge=decode(state["challenge"]),
            timeout=120000,
            user_verification=UserVerificationRequirement.REQUIRED,
        )
        return {
            "challengeId": state["challengeId"],
            "publicKey": json.loads(options_to_json(options)),
        }

    def credential_id(self, form: dict) -> str:
        try:
            raw = str(form.get("password") or "")
            if len(raw) > 32768:
                return ""
            payload = json.loads(raw)
            kind = str(form.get("loginType") or form.get("login_type")).upper()
            if kind == "PASSKEY":
                return hashlib.sha256(decode(payload["credential"]["id"])).hexdigest()
            public_key = (
                file_public_key(payload.get("publicKey"))
                if kind == "FILE_KEY"
                else canonical_public_key(payload["publicKey"])
            )
            return (
                file_key_id(public_key)
                if kind == "FILE_KEY"
                else hashlib.sha256(public_key.encode()).hexdigest()
            )
        except (ValueError, TypeError, KeyError, AttributeError):
            return ""

    @staticmethod
    def require_current_key(row: Any, account: dict) -> None:
        expires_at = credential_expiration(row)
        if expires_at is not None and expires_at <= time.time():
            raise AuthError("登录密钥已到期，请使用账号密码登录后重新生成", 401)
        data = json.loads(row["data"])
        version = data.get("accountVersion")
        if version:
            valid = version == _account_version(account)
        else:
            # Legacy keys predate password-version binding. A later account update
            # still invalidates them, without revoking unchanged existing keys.
            changed = account.get("update_time")
            valid = not changed or datetime.fromisoformat(str(changed)).timestamp() <= int(
                row["created_at"]
            )
        if not valid:
            raise AuthError("密钥已失效，请使用账号密码登录后重新生成", 401)

    async def require_active_session(self, state: dict, account: dict) -> None:
        key_id = state.get("credentialId")
        if not key_id:
            return
        async with self.engine.connect() as conn:
            row = (
                (await conn.execute(select(credentials).where(credentials.c.id == key_id)))
                .mappings()
                .first()
            )
        if not row or int(row["user_id"]) != int(state["userId"]):
            raise AuthError("登录密钥已移除，请重新登录", 401, "invalid_grant")
        self.require_current_key(row, account)

    async def attempt_identity(self, key_id: str, ip: str) -> tuple[str, dict]:
        audit = {"credential_id": key_id, "username": "", "account_type": "username"}
        subject = "key-source:" + hashlib.sha256(str(ip or "unknown").encode()).hexdigest()
        if key_id:
            async with self.engine.connect() as conn:
                row = (
                    (await conn.execute(select(credentials).where(credentials.c.id == key_id)))
                    .mappings()
                    .first()
                )
            if row:
                accounts = await self.auth.repository.password_accounts_for_user(
                    int(row["user_id"])
                )
                account = next(
                    (a for a in accounts if a.get("domain") == self.auth.account_domain), None
                )
                if account:
                    subject = str(account["account"])
                    audit.update(username=subject, account_type=account["account_type"])
        return subject, audit

    async def report_file_failure(self, payload: dict, context: dict) -> None:
        self.require_enabled()
        client_id = str(payload.get("client_id") or "")
        if not await self.auth.repository.find_client(client_id):
            raise AuthError("无效的登录应用", 400)
        try:
            key_id = file_key_id(file_public_key(payload.get("publicKey")))
        except (ValueError, TypeError):
            key_id = ""
        subject, audit = await self.attempt_identity(key_id, context.get("login_ip", ""))
        async with self.cache.lock("login-attempt:" + hashlib.sha256(subject.encode()).hexdigest()):
            if await self.cache.login_error_count(subject) >= self.auth.max_errors:
                error = AuthError(f"登录失败次数过多，帐户锁定{self.auth.lock_minutes}分钟", 423)
            else:
                await self.cache.add_login_error(subject, self.auth.lock_minutes)
                error = AuthError("密钥文件验证失败", 401)
            await self.auth._record_login(
                {**context, **audit, "loginType": "FILE_KEY", "client_id": client_id},
                status=0,
                message=str(error),
            )
        raise error

    async def authenticate(self, client: dict, form: dict) -> tuple[dict, dict, str]:
        self.require_enabled()
        key_id = self.credential_id(form)
        subject, audit = await self.attempt_identity(key_id, form.get("login_ip", ""))
        async with self.cache.lock("login-attempt:" + hashlib.sha256(subject.encode()).hexdigest()):
            try:
                if await self.cache.login_error_count(subject) >= self.auth.max_errors:
                    raise AuthError(f"登录失败次数过多，帐户锁定{self.auth.lock_minutes}分钟", 423)
                result = await self._authenticate(client, form)
            except AuthError as exc:
                if exc.code in (400, 401, 403):
                    await self.cache.add_login_error(subject, self.auth.lock_minutes)
                exc.login_audit = audit
                raise
            await self.cache.clear_login_error(subject)
            account, user, scope = result
            return {**account, "credential_id": key_id}, user, scope

    async def _authenticate(self, client: dict, form: dict) -> tuple[dict, dict, str]:
        self.require_enabled()
        try:
            raw = str(form.get("password") or "")
            if len(raw) > 32768:
                raise ValueError("Oversize response")
            payload = json.loads(raw)
            kind = str(form.get("loginType") or form.get("login_type")).upper()
            state = await self.consume(payload, "login", kind)
            if state["clientId"] != client["clientId"]:
                raise ValueError("Client mismatch")
            if kind == "PASSKEY":
                key_id = hashlib.sha256(decode(payload["credential"]["id"])).hexdigest()
            elif kind == "FILE_KEY":
                key_id = file_key_id(file_public_key(payload.get("publicKey")))
            else:
                public_key = canonical_public_key(payload["publicKey"])
                key_id = hashlib.sha256(public_key.encode()).hexdigest()
            async with self.cache.lock("credential:" + key_id):
                async with self.engine.begin() as conn:
                    row = (
                        (await conn.execute(select(credentials).where(credentials.c.id == key_id)))
                        .mappings()
                        .first()
                    )
                    if not row or row["kind"] != kind:
                        raise ValueError("Unknown credential")
                    data = json.loads(row["data"])
                    count = row["sign_count"]
                    if kind == "PASSKEY":
                        credential = payload["credential"]
                        if decode(credential["response"]["userHandle"]) != decode(data["handle"]):
                            raise ValueError("User handle mismatch")
                        verified = verify_authentication_response(
                            credential=credential,
                            expected_challenge=decode(state["challenge"]),
                            expected_rp_id=self.rp_id,
                            expected_origin=self.origins,
                            credential_public_key=decode(data["publicKey"]),
                            credential_current_sign_count=count,
                            require_user_verification=True,
                        )
                        count = verified.new_sign_count
                    elif kind == "FILE_KEY":
                        verify_file_signature(
                            data["publicKey"],
                            self.file_options(state)["message"],
                            payload["signature"],
                        )
                    else:
                        verify_ssh_signature(
                            data["publicKey"],
                            self.ssh_options(state)["message"],
                            payload["signature"],
                        )
                    account, user = await self.account(int(row["user_id"]))
                    self.require_current_key(row, account)
                    await conn.execute(
                        update(credentials)
                        .where(credentials.c.id == key_id)
                        .values(sign_count=count, last_used_at=int(time.time()))
                    )
            return account, user, str(form.get("scope") or "all")
        except AuthError:
            raise
        except Exception as exc:
            raise AuthError("密钥登录验证失败", 401) from exc
