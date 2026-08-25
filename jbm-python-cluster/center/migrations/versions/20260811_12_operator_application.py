"""Add tenant operator application workflow.

Revision ID: 20260811_12
Revises: 20260811_11
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260811_12"
down_revision: str | None = "20260811_11"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.execute(
        """CREATE TABLE IF NOT EXISTS base_operator_application (
            id BIGINT NOT NULL,
            app_id BIGINT NOT NULL,
            tenant_id BIGINT NOT NULL,
            applicant_user_id BIGINT NOT NULL,
            status INTEGER NOT NULL DEFAULT 0,
            reason VARCHAR(512) NULL,
            review_remark VARCHAR(512) NULL,
            reviewed_by BIGINT NULL,
            reviewed_at TIMESTAMP NULL,
            create_time TIMESTAMP NULL,
            update_time TIMESTAMP NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_operator_application_tenant_app (tenant_id, app_id),
            KEY idx_operator_application_review (app_id, status, create_time)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"""
    )


def downgrade() -> None:
    op.drop_table("base_operator_application")
