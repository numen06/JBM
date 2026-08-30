"""Align legacy bigscreen metadata with the Python service schema.

Revision ID: 20260830_21
Revises: 20260829_20
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import Column, String, Text, inspect

revision: str = "20260830_21"
down_revision: str | None = "20260829_20"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    columns = {column["name"] for column in inspect(bind).get_columns("bigscreen_view")}
    additions = {
        "code": Column("code", String(128), nullable=True),
        "app_id": Column("app_id", String(64), nullable=True),
        "extend_data": Column("extend_data", Text(), nullable=True),
    }
    for name, column in additions.items():
        if name not in columns:
            op.add_column("bigscreen_view", column)

    if bind.dialect.name in {"mysql", "mariadb"}:
        op.execute(
            """ALTER TABLE bigscreen_view
            MODIFY COLUMN id VARCHAR(64) NOT NULL,
            MODIFY COLUMN parent_id VARCHAR(64) NULL,
            MODIFY COLUMN static_params TEXT NULL,
            MODIFY COLUMN resource_path VARCHAR(1024) NULL,
            MODIFY COLUMN preview_picture VARCHAR(1024) NULL,
            MODIFY COLUMN create_time VARCHAR(64) NULL,
            MODIFY COLUMN update_time VARCHAR(64) NULL"""
        )


def downgrade() -> None:
    pass
