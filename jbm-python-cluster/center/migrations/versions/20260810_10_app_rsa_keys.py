"""Backfill client password-encryption keys for registered applications.

Revision ID: 20260810_10
Revises: 20260810_09
"""

import base64
from collections.abc import Sequence

from alembic import op
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from sqlalchemy import text

revision: str = "20260810_10"
down_revision: str | None = "20260810_09"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    connection = op.get_bind()
    rows = connection.execute(
        text(
            "SELECT app_id FROM base_app "
            "WHERE public_key IS NULL OR public_key = '' "
            "OR private_key IS NULL OR private_key = ''"
        )
    ).mappings()
    for row in rows:
        public_key, private_key = _rsa_key_pair()
        connection.execute(
            text(
                "UPDATE base_app SET public_key=:public_key, private_key=:private_key "
                "WHERE app_id=:app_id"
            ),
            {
                "app_id": row["app_id"],
                "public_key": public_key,
                "private_key": private_key,
            },
        )


def downgrade() -> None:
    pass


def _rsa_key_pair() -> tuple[str, str]:
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    public_der = private_key.public_key().public_bytes(
        serialization.Encoding.DER,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    private_der = private_key.private_bytes(
        serialization.Encoding.DER,
        serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    )
    return base64.b64encode(public_der).decode(), base64.b64encode(private_der).decode()
