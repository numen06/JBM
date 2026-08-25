"""Add tenant/app subscription and tenant-scoped role assignments.

Revision ID: 20260810_07
Revises: 20260806_06
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import BigInteger, Column, inspect

revision: str = "20260810_07"
down_revision: str | None = "20260806_06"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    role_user_columns = {column["name"] for column in inspector.get_columns("base_role_user")}
    if "tenant_id" not in role_user_columns:
        op.add_column("base_role_user", Column("tenant_id", BigInteger(), nullable=True))

    op.execute(
        """UPDATE base_role_user ru
           JOIN base_user u ON u.user_id = ru.user_id
           SET ru.tenant_id = u.company_id
           WHERE ru.tenant_id IS NULL"""
    )

    indexes = {index["name"] for index in inspect(bind).get_indexes("base_role_user")}
    if "uk_role_user_tenant_app" not in indexes:
        op.create_unique_constraint(
            "uk_role_user_tenant_app",
            "base_role_user",
            ["tenant_id", "app_id", "user_id", "role_id"],
        )

    op.execute(
        """DELETE newer FROM base_user_org newer
           JOIN base_user_org older
             ON older.user_id = newer.user_id AND older.org_id = newer.org_id
            AND older.id < newer.id"""
    )
    user_org_indexes = {index["name"] for index in inspect(bind).get_indexes("base_user_org")}
    if "uk_base_user_org_user_org" not in user_org_indexes:
        op.create_unique_constraint(
            "uk_base_user_org_user_org", "base_user_org", ["user_id", "org_id"]
        )

    op.execute(
        """CREATE TABLE IF NOT EXISTS base_tenant_app (
            id BIGINT NOT NULL,
            tenant_id BIGINT NOT NULL,
            app_id BIGINT NOT NULL,
            status INTEGER NOT NULL DEFAULT 1,
            config_content TEXT NULL,
            create_time TIMESTAMP NULL,
            update_time TIMESTAMP NULL,
            extend_data TEXT NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_tenant_app (tenant_id, app_id),
            KEY idx_tenant_app_app (app_id, status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"""
    )
    op.execute(
        """INSERT INTO base_tenant_app
           (id, tenant_id, app_id, status, create_time, update_time)
           SELECT a.app_id, a.org_id, a.app_id, 1,
                  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
           FROM base_app a
           WHERE a.org_id IS NOT NULL
             AND NOT EXISTS (
                 SELECT 1 FROM base_tenant_app ta
                 WHERE ta.tenant_id = a.org_id AND ta.app_id = a.app_id
             )"""
    )


def downgrade() -> None:
    # Shared production data is intentionally preserved on service rollback.
    pass
