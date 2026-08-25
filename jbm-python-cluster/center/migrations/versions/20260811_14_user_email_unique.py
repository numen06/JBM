"""Keep one profile owner for each bound email.

Revision ID: 20260811_14
Revises: 20260811_13
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import inspect, text

revision: str = "20260811_14"
down_revision: str | None = "20260811_13"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    bind.execute(text("UPDATE base_user SET email=NULL WHERE TRIM(COALESCE(email, ''))=''"))
    bind.execute(text("UPDATE base_user SET email=LOWER(TRIM(email)) WHERE email IS NOT NULL"))
    duplicate = bind.execute(
        text(
            "SELECT email FROM base_user WHERE email IS NOT NULL "
            "GROUP BY email HAVING COUNT(*) > 1 LIMIT 1"
        )
    ).first()
    if duplicate:
        raise RuntimeError("base_user 存在重复邮箱，请先完成账号归并")
    names = {item["name"] for item in inspect(bind).get_unique_constraints("base_user")}
    if "uk_base_user_email" not in names:
        op.create_unique_constraint("uk_base_user_email", "base_user", ["email"])


def downgrade() -> None:
    pass
