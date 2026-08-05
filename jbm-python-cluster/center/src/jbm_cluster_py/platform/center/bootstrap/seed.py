from __future__ import annotations

import asyncio
import base64
from pathlib import Path

import bcrypt
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.integrations.database import configured_database_url


async def seed_local_baseline(config: AppConfig) -> None:
    settings = dict(config.get("jbm.center.seed", {}) or {})
    if not settings.get("enabled", False):
        return
    url = configured_database_url(config.database)
    if not url:
        raise RuntimeError("Center database is not configured")
    password = str(settings.get("admin-password") or "Admin@123")
    password_hash = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()
    seed_file = Path(__file__).parents[5] / "migrations" / "sql" / "local_seed.sql"
    sql = seed_file.read_text(encoding="utf-8").replace("__ADMIN_PASSWORD_HASH__", password_hash)
    engine = create_async_engine(url, pool_pre_ping=True)
    try:
        async with engine.begin() as connection:
            for statement in sql.split(";"):
                if statement.strip():
                    await connection.execute(text(statement))
            if settings.get("force-reset-password", False):
                await connection.execute(
                    text("UPDATE base_account SET password=:password WHERE account_id=1"),
                    {"password": password_hash},
                )
            row = (
                await connection.execute(
                    text("SELECT public_key, private_key FROM base_app WHERE app_id=1000")
                )
            ).mappings().first()
            if row and (not row.get("public_key") or not row.get("private_key")):
                public_key, private_key = _rsa_key_pair()
                await connection.execute(
                    text(
                        "UPDATE base_app SET public_key=:public_key, private_key=:private_key "
                        "WHERE app_id=1000"
                    ),
                    {"public_key": public_key, "private_key": private_key},
                )
    finally:
        await engine.dispose()


def _rsa_key_pair() -> tuple[str, str]:
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    public_der = private_key.public_key().public_bytes(
        serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo
    )
    private_der = private_key.private_bytes(
        serialization.Encoding.DER,
        serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    )
    return base64.b64encode(public_der).decode(), base64.b64encode(private_der).decode()


def run() -> None:
    asyncio.run(seed_local_baseline(AppConfig.load(app="center")))


if __name__ == "__main__":
    run()
