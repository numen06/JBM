"""Create Center-owned compatibility tables missing from the legacy baseline."""

from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect


revision = "20260806_01"
down_revision = None
branch_labels = None
depends_on = None


def _master_columns() -> list[sa.Column]:
    return [
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("code", sa.String(64)),
        sa.Column("app_id", sa.BigInteger()),
        sa.Column("parent_id", sa.BigInteger()),
        sa.Column("level", sa.Integer()),
        sa.Column("leaf_path", sa.String(512)),
        sa.Column("create_time", sa.DateTime()),
        sa.Column("update_time", sa.DateTime()),
        sa.Column("extend_data", sa.Text()),
    ]


def _create_if_missing(name: str, *columns: sa.Column, **kwargs) -> None:
    if not inspect(op.get_bind()).has_table(name):
        op.create_table(name, *columns, **kwargs)


def upgrade() -> None:
    _create_if_missing(
        "base_account_logs",
        *_master_columns(),
        sa.Column("user_id", sa.BigInteger()),
        sa.Column("account", sa.String(128)),
        sa.Column("login_type", sa.String(32)),
        sa.Column("login_time", sa.DateTime()),
        sa.Column("ip", sa.String(64)),
        sa.Column("user_agent", sa.String(512)),
        sa.Column("status", sa.Integer()),
        sa.Column("message", sa.String(1024)),
    )
    _create_if_missing(
        "base_app_config",
        *_master_columns(),
        sa.Column("app_key", sa.String(128), nullable=False),
        sa.Column("org_id", sa.BigInteger()),
        sa.Column("config_content", sa.Text()),
        sa.UniqueConstraint("app_key", "org_id", name="uk_base_app_config_key_org"),
    )
    _create_if_missing(
        "base_release_info",
        *_master_columns(),
        sa.Column("release_time", sa.DateTime()),
        sa.Column("package_time", sa.DateTime()),
        sa.Column("release_content", sa.Text()),
        sa.Column("user_name", sa.String(128)),
        sa.Column("version_number", sa.String(64)),
        sa.Column("package_url", sa.String(1024)),
    )
    _create_if_missing(
        "base_user_certification",
        *_master_columns(),
        sa.Column("user_id", sa.BigInteger(), nullable=False, unique=True),
        sa.Column("id_card", sa.String(128)),
        sa.Column("card_type", sa.String(32)),
        sa.Column("expiration_date", sa.DateTime()),
        sa.Column("effective_date", sa.DateTime()),
        sa.Column("certification_type", sa.Integer()),
        sa.Column("status", sa.Integer()),
        sa.Column("face_image", sa.Text()),
        sa.Column("fingerprint", sa.String(512)),
    )
    _create_if_missing(
        "data_source_management",
        *_master_columns(),
        sa.Column("data_source_code", sa.String(128), nullable=False, unique=True),
        sa.Column("data_source_name", sa.String(255), nullable=False),
        sa.Column("data_source_type", sa.String(32), nullable=False),
        sa.Column("customize_content", sa.Text()),
        sa.Column("url", sa.String(1024)),
        sa.Column("request_method", sa.String(16)),
        sa.Column("request_header", sa.Text()),
        sa.Column("request_body", sa.Text()),
    )


def downgrade() -> None:
    for name in (
        "data_source_management",
        "base_user_certification",
        "base_release_info",
        "base_app_config",
        "base_account_logs",
    ):
        if inspect(op.get_bind()).has_table(name):
            op.drop_table(name)
