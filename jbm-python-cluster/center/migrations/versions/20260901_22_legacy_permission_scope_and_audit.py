"""Align legacy permission scopes and login audit IDs with the Python services.

Revision ID: 20260901_22
Revises: 20260830_21
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import BigInteger, Column, inspect

revision: str = "20260901_22"
down_revision: str | None = "20260830_21"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    tables = set(inspector.get_table_names())

    for table in ("base_authority_role", "base_authority_user"):
        if table not in tables:
            continue
        columns = {column["name"] for column in inspector.get_columns(table)}
        if "app_id" not in columns:
            op.add_column(table, Column("app_id", BigInteger(), nullable=True))

    if bind.dialect.name in {"mysql", "mariadb"} and "base_account_logs" in tables:
        id_column = next(
            (
                column
                for column in inspector.get_columns("base_account_logs")
                if column["name"] == "id"
            ),
            None,
        )
        if id_column and not id_column.get("autoincrement"):
            op.execute(
                "ALTER TABLE base_account_logs "
                "MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT"
            )


def downgrade() -> None:
    # Existing grants and audit rows are intentionally preserved on rollback.
    pass
