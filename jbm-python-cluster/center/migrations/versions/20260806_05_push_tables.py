"""Add Python Push owned tables.

Revision ID: 20260806_05
Revises: 20260806_04
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260806_05"
down_revision: str | None = "20260806_04"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    statements = (
        """CREATE TABLE IF NOT EXISTS push_message_body (
            id BIGINT PRIMARY KEY AUTO_INCREMENT, send_user_id BIGINT, title VARCHAR(512),
            tags VARCHAR(512), content TEXT, template_code VARCHAR(256), type VARCHAR(64),
            level INTEGER, url VARCHAR(1024), extend_data TEXT, create_time VARCHAR(64),
            update_time VARCHAR(64)
        )""",
        """CREATE TABLE IF NOT EXISTS push_message_item (
            msg_id VARCHAR(64) PRIMARY KEY, msg_body_id BIGINT NOT NULL, rec_user_id BIGINT,
            send_user_id BIGINT, push_status VARCHAR(32), push_way VARCHAR(32), read_flag INTEGER,
            create_time VARCHAR(64), update_time VARCHAR(64), INDEX idx_push_message_body (msg_body_id),
            INDEX idx_push_message_rec_user (rec_user_id, read_flag)
        )""",
        """CREATE TABLE IF NOT EXISTS push_config_info (
            id BIGINT PRIMARY KEY, enable INTEGER, type INTEGER, release_content TEXT,
            create_time VARCHAR(64), update_time VARCHAR(64)
        )""",
        """CREATE TABLE IF NOT EXISTS email_push_config (
            id BIGINT PRIMARY KEY, host VARCHAR(255), username VARCHAR(255), password VARCHAR(512),
            port INTEGER, create_time VARCHAR(64), update_time VARCHAR(64)
        )""",
    )
    for statement in statements:
        op.execute(statement)


def downgrade() -> None:
    pass
