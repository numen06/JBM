"""Durable login attribution and account-owned location warnings.

Revision ID: 20261023_26
Revises: 20261020_25
"""

import sqlalchemy as sa
from alembic import op

revision = "20261023_26"
down_revision = "20261020_25"
branch_labels = None
depends_on = None

FIELDS = {
    "login_location": sa.String(256),
    "client_id": sa.String(128),
    "credential_id": sa.String(64),
    "request_id": sa.String(64),
    "risk_level": sa.String(16),
    "risk_detail": sa.Text(),
    "risk_ack_time": sa.DateTime(),
}


def upgrade():
    columns = {row["name"] for row in sa.inspect(op.get_bind()).get_columns("base_account_logs")}
    for name, field in FIELDS.items():
        if name not in columns:
            op.add_column("base_account_logs", sa.Column(name, field, nullable=True))
    indexes = {row["name"] for row in sa.inspect(op.get_bind()).get_indexes("base_account_logs")}
    if "ix_account_logs_user_security" not in indexes:
        op.create_index(
            "ix_account_logs_user_security",
            "base_account_logs",
            ["user_id", "risk_level", "risk_ack_time"],
        )
    if "ix_account_logs_user_login_time" not in indexes:
        op.create_index(
            "ix_account_logs_user_login_time", "base_account_logs", ["user_id", "login_time"]
        )


def downgrade():
    op.drop_index("ix_account_logs_user_security", "base_account_logs")
    op.drop_index("ix_account_logs_user_login_time", "base_account_logs")
    for name in reversed(list(FIELDS)):
        # login_location may be part of the original Java schema; retain it.
        if name != "login_location":
            op.drop_column("base_account_logs", name)
