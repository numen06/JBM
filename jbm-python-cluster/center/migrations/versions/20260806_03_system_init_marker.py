"""Add the shared initialization marker required by cluster services.

Revision ID: 20260806_03
Revises: 20260806_02
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260806_03"
down_revision: str | None = "20260806_02"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.execute(
        """
        CREATE TABLE IF NOT EXISTS jbm_system_init_marker (
            marker_key VARCHAR(64) NOT NULL PRIMARY KEY,
            initialized_at TIMESTAMP NOT NULL
        )
        """
    )


def downgrade() -> None:
    # The database is shared by multiple platform services. Never drop a shared
    # coordination table during a Center rollback.
    pass
