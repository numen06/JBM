"""Prevent duplicate login identities during concurrent registration.

Revision ID: 20260811_11
Revises: 20260810_10
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import inspect

revision: str = "20260811_11"
down_revision: str | None = "20260810_10"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    names = {
        item["name"] for item in inspect(bind).get_unique_constraints("base_account")
    }
    if "uk_base_account_identity" not in names:
        op.create_unique_constraint(
            "uk_base_account_identity",
            "base_account",
            ["account", "account_type", "domain"],
        )


def downgrade() -> None:
    pass
