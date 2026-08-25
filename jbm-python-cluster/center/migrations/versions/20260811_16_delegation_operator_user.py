"""Scope tenant delegation to the operator account selected by the owner.

Revision ID: 20260811_16
Revises: 20260811_15
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260811_16"
down_revision: str | None = "20260811_15"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.execute(
        "ALTER TABLE base_tenant_delegation "
        "ADD COLUMN operator_user_id BIGINT NULL AFTER operator_tenant_id"
    )
    op.execute(
        "CREATE INDEX idx_delegation_operator_user "
        "ON base_tenant_delegation(operator_user_id, app_id, status, valid_to)"
    )


def downgrade() -> None:
    pass
