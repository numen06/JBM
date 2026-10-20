"""Tenant navigation preferences, shared by all product shells."""
from alembic import op

revision = "20261020_25"
down_revision = "20260928_24"
branch_labels = None
depends_on = None

CREATE_TABLE = """CREATE TABLE IF NOT EXISTS base_tenant_navigation (
    tenant_id BIGINT NOT NULL PRIMARY KEY,
    preferences JSON NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1,
    updated_by BIGINT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"""


def upgrade():
    op.execute(CREATE_TABLE)


def downgrade():
    op.drop_table("base_tenant_navigation")
