"""Add granular cross-tenant delegation grants.

Revision ID: 20260810_08
Revises: 20260810_07
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260810_08"
down_revision: str | None = "20260810_07"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.execute(
        """CREATE TABLE IF NOT EXISTS base_tenant_delegation (
            id BIGINT NOT NULL,
            app_id BIGINT NOT NULL,
            owner_tenant_id BIGINT NOT NULL,
            operator_tenant_id BIGINT NOT NULL,
            status INTEGER NOT NULL DEFAULT 1,
            permission_codes TEXT NOT NULL,
            resource_types TEXT NULL,
            data_scope TEXT NULL,
            field_policy TEXT NULL,
            valid_from TIMESTAMP NULL,
            valid_to TIMESTAMP NULL,
            purpose VARCHAR(512) NULL,
            version BIGINT NOT NULL DEFAULT 1,
            created_by BIGINT NULL,
            approved_by BIGINT NULL,
            revoked_by BIGINT NULL,
            create_time TIMESTAMP NULL,
            update_time TIMESTAMP NULL,
            extend_data TEXT NULL,
            PRIMARY KEY (id),
            KEY idx_delegation_operator (operator_tenant_id, app_id, status, valid_to),
            KEY idx_delegation_owner (owner_tenant_id, app_id, status, valid_to)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"""
    )


def downgrade() -> None:
    pass
