"""Scope bigscreen packages by tenant and project.

Revision ID: 20260826_17
Revises: 20260811_16
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import Column, String, inspect

revision: str = "20260826_17"
down_revision: str | None = "20260811_16"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    columns = {column["name"] for column in inspect(bind).get_columns("bigscreen_view")}
    for name in ("tenant_id", "project_id", "created_by"):
        if name not in columns:
            op.add_column("bigscreen_view", Column(name, String(64), nullable=True))

    indexes = {index["name"] for index in inspect(bind).get_indexes("bigscreen_view")}
    if "idx_bigscreen_tenant_project" not in indexes:
        op.create_index(
            "idx_bigscreen_tenant_project",
            "bigscreen_view",
            ["tenant_id", "project_id", "update_time"],
        )


def downgrade() -> None:
    pass
