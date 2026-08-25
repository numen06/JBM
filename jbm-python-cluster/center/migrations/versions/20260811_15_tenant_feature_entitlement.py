"""Add tenant feature entitlement and member delegation tables.

Revision ID: 20260811_15
Revises: 20260811_14
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260811_15"
down_revision: str | None = "20260811_14"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.execute(
        """CREATE TABLE IF NOT EXISTS base_app_feature (
            id BIGINT NOT NULL,
            app_id BIGINT NOT NULL,
            feature_code VARCHAR(128) NOT NULL,
            feature_name VARCHAR(128) NOT NULL,
            feature_desc VARCHAR(512) NULL,
            status INTEGER NOT NULL DEFAULT 1,
            sort_order INTEGER NOT NULL DEFAULT 0,
            create_time TIMESTAMP NULL,
            update_time TIMESTAMP NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_app_feature_code (app_id, feature_code),
            KEY idx_app_feature_status (app_id, status, sort_order)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"""
    )
    op.execute(
        """CREATE TABLE IF NOT EXISTS base_tenant_feature (
            id BIGINT NOT NULL,
            tenant_id BIGINT NOT NULL,
            app_id BIGINT NOT NULL,
            feature_code VARCHAR(128) NOT NULL,
            status INTEGER NOT NULL DEFAULT 1,
            valid_from TIMESTAMP NULL,
            valid_to TIMESTAMP NULL,
            granted_by BIGINT NULL,
            create_time TIMESTAMP NULL,
            update_time TIMESTAMP NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_tenant_app_feature (tenant_id, app_id, feature_code),
            KEY idx_tenant_feature_active (tenant_id, app_id, status, valid_to)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"""
    )
    op.execute(
        """CREATE TABLE IF NOT EXISTS base_user_feature_grant (
            id BIGINT NOT NULL,
            tenant_id BIGINT NOT NULL,
            app_id BIGINT NOT NULL,
            user_id BIGINT NOT NULL,
            feature_code VARCHAR(128) NOT NULL,
            data_scope TEXT NULL,
            status INTEGER NOT NULL DEFAULT 1,
            granted_by BIGINT NULL,
            create_time TIMESTAMP NULL,
            update_time TIMESTAMP NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_user_app_feature (tenant_id, app_id, user_id, feature_code),
            KEY idx_user_feature_active (user_id, tenant_id, app_id, status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"""
    )


def downgrade() -> None:
    pass
