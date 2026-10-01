"""Verify OpenSSH ssh-keygen -Y sign output in a dedicated login namespace."""

import base64
import hashlib
import struct

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, ed25519, padding, rsa, utils


def canonical_public_key(value: str) -> str:
    if len(value) > 16384:
        raise ValueError("Public key too large")
    key = serialization.load_ssh_public_key(value.strip().encode())
    if not isinstance(key, (ed25519.Ed25519PublicKey, rsa.RSAPublicKey, ec.EllipticCurvePublicKey)):
        raise ValueError("Unsupported SSH key")
    if isinstance(key, rsa.RSAPublicKey) and key.key_size < 2048:
        raise ValueError("RSA key must be at least 2048 bits")
    return key.public_bytes(
        serialization.Encoding.OpenSSH, serialization.PublicFormat.OpenSSH
    ).decode()


class Reader:
    def __init__(self, data: bytes):
        self.data = data

    def take(self, size: int) -> bytes:
        if size > len(self.data):
            raise ValueError("Truncated SSH signature")
        value, self.data = self.data[:size], self.data[size:]
        return value

    def string(self) -> bytes:
        return self.take(struct.unpack(">I", self.take(4))[0])

    def end(self) -> None:
        if self.data:
            raise ValueError("Trailing SSH data")


def packed(value: bytes) -> bytes:
    return struct.pack(">I", len(value)) + value


def verify_ssh_signature(public_key: str, message: str, signature: str) -> None:
    if len(signature) > 16384:
        raise ValueError("Signature too large")
    lines = signature.strip().splitlines()
    if lines[0] != "-----BEGIN SSH SIGNATURE-----" or lines[-1] != "-----END SSH SIGNATURE-----":
        raise ValueError("Expected OpenSSH signature")
    reader = Reader(base64.b64decode("".join(lines[1:-1]), validate=True))
    if reader.take(10) != b"SSHSIG\x00\x00\x00\x01":
        raise ValueError("Invalid SSH signature version")
    key_blob, namespace, reserved, algorithm = (reader.string() for _ in range(4))
    signature_reader = Reader(reader.string())
    reader.end()
    if namespace != b"jbm-key-login" or algorithm not in {b"sha256", b"sha512"}:
        raise ValueError("Invalid signature context")
    expected = canonical_public_key(public_key)
    if key_blob != base64.b64decode(expected.split()[1], validate=True):
        raise ValueError("Wrong public key")
    signed = b"SSHSIG" + b"".join(
        packed(value)
        for value in (
            namespace,
            reserved,
            algorithm,
            hashlib.new(algorithm.decode(), message.encode()).digest(),
        )
    )
    signature_algorithm, signature_bytes = signature_reader.string(), signature_reader.string()
    signature_reader.end()
    key = serialization.load_ssh_public_key(expected.encode())
    if isinstance(key, ed25519.Ed25519PublicKey) and signature_algorithm == b"ssh-ed25519":
        key.verify(signature_bytes, signed)
    elif isinstance(key, rsa.RSAPublicKey) and signature_algorithm in {
        b"rsa-sha2-256",
        b"rsa-sha2-512",
    }:
        digest = hashes.SHA256() if signature_algorithm == b"rsa-sha2-256" else hashes.SHA512()
        key.verify(signature_bytes, signed, padding.PKCS1v15(), digest)
    elif (
        isinstance(key, ec.EllipticCurvePublicKey)
        and signature_algorithm == expected.split()[0].encode()
    ):
        components = Reader(signature_bytes)
        r, s = (
            int.from_bytes(components.string(), "big"),
            int.from_bytes(components.string(), "big"),
        )
        components.end()
        digest = {256: hashes.SHA256, 384: hashes.SHA384, 521: hashes.SHA512}[key.key_size]()
        key.verify(utils.encode_dss_signature(r, s), signed, ec.ECDSA(digest))
    else:
        raise ValueError("Unsupported signature algorithm")
