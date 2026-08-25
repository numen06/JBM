"""Keep one profile owner for each login mobile.

Revision ID: 20260811_13
Revises: 20260811_12
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import inspect, text

revision: str = "20260811_13"
down_revision: str | None = "20260811_12"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    bind.execute(text("UPDATE base_user SET mobile=NULL WHERE TRIM(COALESCE(mobile, ''))=''"))
    duplicate = bind.execute(
        text(
            "SELECT mobile FROM base_user WHERE mobile IS NOT NULL "
            "GROUP BY mobile HAVING COUNT(*) > 1 LIMIT 1"
        )
    ).first()
    if duplicate:
        raise RuntimeError("base_user 存在重复手机号，请先完成账号归并")
    names = {item["name"] for item in inspect(bind).get_unique_constraints("base_user")}
    if "uk_base_user_mobile" not in names:
        op.create_unique_constraint("uk_base_user_mobile", "base_user", ["mobile"])


def downgrade() -> None:
    pass
