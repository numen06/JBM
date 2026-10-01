"""Account-bound passkeys and OpenSSH public keys.

Revision ID: 20260928_24
Revises: 20260922_23
"""

from alembic import op
import sqlalchemy as sa

revision = "20260928_24"
down_revision = "20260922_23"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "base_auth_credential",
        sa.Column("id", sa.String(64), primary_key=True),
        sa.Column("user_id", sa.BigInteger(), nullable=False),
        sa.Column("kind", sa.String(16), nullable=False),
        sa.Column("name", sa.String(80), nullable=False),
        sa.Column("data", sa.Text(), nullable=False),
        sa.Column("sign_count", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.BigInteger(), nullable=False),
        sa.Column("last_used_at", sa.BigInteger()),
    )
    op.create_index("ix_base_auth_credential_user_id", "base_auth_credential", ["user_id"])


def downgrade():
    op.drop_table("base_auth_credential")
