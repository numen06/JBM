"""Persist permanent opaque subjects for the opt-in OIDC issuer.

Revision ID: 20260922_23
Revises: 20260901_22
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import BigInteger, Column, DateTime, String, UniqueConstraint, inspect

revision: str = "20260922_23"
down_revision: str | None = "20260901_22"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    if inspect(op.get_bind()).has_table("base_auth_subject"):
        return
    # Intentionally no cascading FK: deleted accounts must not free issued subjects.
    # User IDs are permanent identifiers and must never be recycled.
    op.create_table(
        "base_auth_subject",
        Column("user_id", BigInteger(), primary_key=True, autoincrement=False),
        Column("subject", String(64), nullable=False),
        Column("create_time", DateTime(), nullable=False),
        UniqueConstraint("subject", name="uq_base_auth_subject_subject"),
    )


def downgrade() -> None:
    # Preserve issued identity mappings even after disabling the OIDC endpoints.
    pass
