"""Opaque, platform-encrypted file credentials. No user private key is exported."""

import base64
import hashlib
import json
import re
import secrets

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.hkdf import HKDF


class EncryptedFileKey:
    AAD = b"jbm-login-key-v2"

    def __init__(self, master_key, rp_id):
        # Persist this platform key independently of JWT signing-key rotation.
        if not isinstance(master_key, bytes) or len(master_key) != 32:
            raise ValueError("文件加密密钥必须为32字节")
        key = HKDF(algorithm=hashes.SHA256(), length=32, salt=rp_id.encode(), info=self.AAD).derive(master_key)
        self.cipher = AESGCM(key)
        self.rp_id = rp_id

    def issue(self):
        secret = secrets.token_bytes(32)
        key_id = hashlib.sha256(self.AAD + secret).hexdigest()
        value = {"keyId": key_id, "secret": base64.urlsafe_b64encode(secret).decode(), "rpId": self.rp_id}
        nonce = secrets.token_bytes(12)
        encrypted = self.cipher.encrypt(nonce, json.dumps(value, separators=(",", ":")).encode(), self.AAD)
        payload = base64.urlsafe_b64encode(nonce + encrypted).decode().rstrip("=")
        return key_id, {"fileVersion": 2, "secretHash": hashlib.sha256(secret).hexdigest()}, payload

    def open(self, payload):
        if not isinstance(payload, str) or not 64 <= len(payload) <= 4096 or not re.fullmatch(r"[A-Za-z0-9_-]+", payload):
            raise ValueError("无效的加密密钥文件")
        try:
            raw = base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4))
            value = json.loads(self.cipher.decrypt(raw[:12], raw[12:], self.AAD))
            secret = base64.b64decode(value["secret"], altchars=b"-_", validate=True)
            if len(secret) != 32 or value["rpId"] != self.rp_id:
                raise ValueError("Invalid platform")
            key_id = hashlib.sha256(self.AAD + secret).hexdigest()
            if not secrets.compare_digest(key_id, value["keyId"]):
                raise ValueError("Invalid credential")
            return key_id, hashlib.sha256(secret).hexdigest()
        except Exception as exc:
            raise ValueError("加密密钥文件无效、已损坏或不属于当前平台") from exc
