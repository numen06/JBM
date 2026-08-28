"""Add per-application authentication policy storage.

Revision ID: 20260829_19
Revises: 20260826_18
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import Column, Text, inspect

revision: str = "20260829_19"
down_revision: str | None = "20260826_18"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    columns = {column["name"] for column in inspect(op.get_bind()).get_columns("base_app")}
    if "extend_data" not in columns:
        op.add_column("base_app", Column("extend_data", Text(), nullable=True))


def downgrade() -> None:
    # Authentication policy data is retained on rollback.
    pass
