"""Add password state required by the Python authentication service.

Revision ID: 20260829_20
Revises: 20260829_19
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import Column, Integer, inspect

revision: str = "20260829_20"
down_revision: str | None = "20260829_19"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    columns = {column["name"] for column in inspect(op.get_bind()).get_columns("base_account")}
    if "must_change_password" not in columns:
        op.add_column(
            "base_account",
            Column("must_change_password", Integer(), nullable=False, server_default="0"),
        )


def downgrade() -> None:
    # Password-state data is retained on rollback.
    pass
