"""Scope tenant delegation to the operator account selected by the owner.

Revision ID: 20260811_16
Revises: 20260811_15
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import BigInteger, Column, inspect

revision: str = "20260811_16"
down_revision: str | None = "20260811_15"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    columns = {
        column["name"]
        for column in inspect(bind).get_columns("base_tenant_delegation")
    }
    if "operator_user_id" not in columns:
        op.add_column(
            "base_tenant_delegation",
            Column("operator_user_id", BigInteger(), nullable=True),
        )

    indexes = {
        index["name"]
        for index in inspect(bind).get_indexes("base_tenant_delegation")
    }
    if "idx_delegation_operator_user" not in indexes:
        op.create_index(
            "idx_delegation_operator_user",
            "base_tenant_delegation",
            ["operator_user_id", "app_id", "status", "valid_to"],
        )


def downgrade() -> None:
    pass
