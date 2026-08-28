"""Scope button actions and create their assignable authorities.

Revision ID: 20260810_09
Revises: 20260810_08
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import BigInteger, Column, String, inspect

revision: str = "20260810_09"
down_revision: str | None = "20260810_08"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    action_columns = {column["name"] for column in inspect(bind).get_columns("base_action")}
    if "app_id" not in action_columns:
        op.add_column("base_action", Column("app_id", BigInteger(), nullable=True))

    authority_columns = {
        column["name"] for column in inspect(bind).get_columns("base_authority")
    }
    if "app_id" not in authority_columns:
        op.add_column("base_authority", Column("app_id", BigInteger(), nullable=True))
    if "resource_type" not in authority_columns:
        op.add_column("base_authority", Column("resource_type", String(20), nullable=True))

    op.execute(
        """UPDATE base_action action
           JOIN base_menu menu ON menu.menu_id = action.menu_id
           SET action.app_id = menu.app_id
           WHERE action.app_id IS NULL"""
    )
    op.execute(
        """INSERT INTO base_authority
             (authority_id, app_id, authority, resource_type, menu_id, action_id,
              status, create_time, update_time)
           SELECT UUID_SHORT(), menu.app_id, CONCAT('ACTION_', action.action_code),
                  'action', action.menu_id, action.action_id,
                  COALESCE(action.status, 1), NOW(), NOW()
           FROM base_action action
           JOIN base_menu menu ON menu.menu_id = action.menu_id
           LEFT JOIN base_authority authority ON authority.action_id = action.action_id
           WHERE authority.authority_id IS NULL
             AND action.action_code IS NOT NULL
             AND action.action_code <> ''"""
    )


def downgrade() -> None:
    pass
