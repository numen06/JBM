"""Add Python Logs and Bigscreen owned tables.

Revision ID: 20260806_04
Revises: 20260806_03
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260806_04"
down_revision: str | None = "20260806_03"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    statements = (
        """CREATE TABLE IF NOT EXISTS gateway_logs (
            access_id VARCHAR(64) PRIMARY KEY, loglevel INTEGER, path VARCHAR(1024),
            api_path VARCHAR(1024), request_user_id VARCHAR(64), request_real_name VARCHAR(255),
            api_name VARCHAR(255), operation_type VARCHAR(64), app_id VARCHAR(64), key_id VARCHAR(64),
            app_key VARCHAR(128), api_id VARCHAR(64), app_name VARCHAR(255), method VARCHAR(16),
            ip VARCHAR(64), http_status INTEGER, request_time VARCHAR(64), response_time VARCHAR(64),
            response_body TEXT, use_time BIGINT, params TEXT, headers TEXT, user_agent TEXT,
            region VARCHAR(255), authentication TEXT, service_id VARCHAR(255), error TEXT
        )""",
        """CREATE TABLE IF NOT EXISTS gateway_log_filter_rule (
            rule_id VARCHAR(64) PRIMARY KEY, rule_name VARCHAR(255) NOT NULL, enabled INTEGER NOT NULL,
            builtin INTEGER NOT NULL, path_pattern VARCHAR(1024), method VARCHAR(16),
            service_id VARCHAR(255), status_code VARCHAR(32), remark VARCHAR(1024),
            hit_count BIGINT NOT NULL, last_hit_time VARCHAR(64), create_time VARCHAR(64),
            update_time VARCHAR(64)
        )""",
        """CREATE TABLE IF NOT EXISTS business_log (
            log_id VARCHAR(64) PRIMARY KEY, module VARCHAR(255), operation VARCHAR(255),
            user_id VARCHAR(64), username VARCHAR(255), status VARCHAR(32), request_ip VARCHAR(64),
            trace_id VARCHAR(128), remark TEXT, business_type VARCHAR(128), business_id VARCHAR(255),
            source VARCHAR(255), expire_days INTEGER, expire_date VARCHAR(64),
            total_lines INTEGER NOT NULL, create_time VARCHAR(64), update_time VARCHAR(64)
        )""",
        """CREATE TABLE IF NOT EXISTS business_log_line (
            log_id VARCHAR(64) NOT NULL, line_number INTEGER NOT NULL, content TEXT NOT NULL,
            is_append INTEGER NOT NULL, create_time VARCHAR(64), trace_id VARCHAR(128),
            business_type VARCHAR(128), business_id VARCHAR(255), source VARCHAR(255),
            stage_code VARCHAR(128), stage_name VARCHAR(255), stage_index INTEGER,
            stage_progress INTEGER, stage_status VARCHAR(32), stage_count INTEGER,
            overall_progress INTEGER, stage_event INTEGER NOT NULL,
            PRIMARY KEY (log_id, line_number)
        )""",
        """CREATE TABLE IF NOT EXISTS business_log_stage (
            log_id VARCHAR(64) PRIMARY KEY, snapshot_json TEXT NOT NULL, version BIGINT NOT NULL,
            update_time VARCHAR(64)
        )""",
        """CREATE TABLE IF NOT EXISTS bigscreen_view (
            id VARCHAR(64) PRIMARY KEY, code VARCHAR(128), app_id VARCHAR(64), parent_id VARCHAR(64),
            view_name VARCHAR(255), view_url VARCHAR(255), static_params TEXT,
            resource_path VARCHAR(1024), preview_picture VARCHAR(1024), version VARCHAR(64),
            config_data TEXT, create_time VARCHAR(64), update_time VARCHAR(64), extend_data TEXT
        )""",
    )
    for statement in statements:
        op.execute(statement)


def downgrade() -> None:
    # The database is shared by all platform services. Service rollback must not
    # destroy logs, packages, or metadata owned by another running revision.
    pass
