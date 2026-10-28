"""Register Feige mobile OAuth clients under the existing IoT application.

Revision ID: 20261029_27
Revises: 20261023_26
"""

import json

import sqlalchemy as sa
from alembic import op

revision = "20261029_27"
down_revision = "20261023_26"
branch_labels = None
depends_on = None

CLIENT_ID = "iot_platform-98dc2cbb2a528b0e"


def upgrade():
    connection = op.get_bind()
    row = (
        connection.execute(
            sa.text("SELECT app_id, extend_data FROM base_app WHERE api_key=:client_id"),
            {"client_id": CLIENT_ID},
        )
        .mappings()
        .first()
    )
    if not row:
        return
    raw = row["extend_data"]
    data = json.loads(raw) if isinstance(raw, str) and raw else dict(raw or {})
    oauth = data.setdefault("oauth", {})
    clients = oauth.setdefault("terminalClients", {})
    # Preserve an administrator's explicit registration, including disabled ones.
    clients.setdefault("miniapp", {"enabled": True, "redirectUris": ["/login/callback"]})
    clients.setdefault("app", {"enabled": True, "redirectUris": ["/login/callback"]})
    connection.execute(
        sa.text("UPDATE base_app SET extend_data=:data WHERE app_id=:app_id"),
        {"data": json.dumps(data, ensure_ascii=False), "app_id": row["app_id"]},
    )


def downgrade():
    # Client registrations may have been edited after deployment. Retain them;
    # the old auth service simply does not resolve the registered suffixes.
    pass
