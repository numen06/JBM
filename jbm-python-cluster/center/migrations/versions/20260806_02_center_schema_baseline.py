"""Create the complete Center compatibility schema on an empty database."""

from pathlib import Path

from alembic import op
import sqlalchemy as sa


revision = "20260806_02"
down_revision = "20260806_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    schema = Path(__file__).parents[1] / "sql" / "center_schema.sql"
    for statement in schema.read_text(encoding="utf-8").split(";"):
        if statement.strip():
            op.execute(sa.text(statement))


def downgrade() -> None:
    # This revision is a non-destructive baseline for shared legacy databases.
    pass
