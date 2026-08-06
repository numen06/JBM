"""Add Doc tables and built-in gateway log rules.

Revision ID: 20260806_06
Revises: 20260806_05
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260806_06"
down_revision: str | None = "20260806_05"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    statements = (
        """CREATE TABLE IF NOT EXISTS base_doc (
            doc_id VARCHAR(64) PRIMARY KEY, doc_name VARCHAR(512), size BIGINT,
            doc_group_id VARCHAR(64), doc_group VARCHAR(512), doc_path VARCHAR(512) UNIQUE,
            state VARCHAR(64), content_type VARCHAR(256), effective_time BIGINT,
            expiration_time VARCHAR(64), version TEXT, creator VARCHAR(64),
            create_time VARCHAR(64), update_time VARCHAR(64)
        )""",
        """CREATE TABLE IF NOT EXISTS base_doc_group (
            group_id VARCHAR(64) PRIMARY KEY, group_path VARCHAR(512) UNIQUE,
            expiration_time VARCHAR(64), auto_clear TINYINT, max_quantity INTEGER,
            token_key VARCHAR(64), doc_group_name VARCHAR(256),
            create_time VARCHAR(64), update_time VARCHAR(64)
        )""",
        """CREATE TABLE IF NOT EXISTS base_doc_token (
            token_key VARCHAR(64) PRIMARY KEY, expiration_time VARCHAR(64),
            effective_time BIGINT, effective_time_type INTEGER, doc_group_id VARCHAR(64),
            doc_id VARCHAR(64), create_time VARCHAR(64), update_time VARCHAR(64)
        )""",
        """CREATE TABLE IF NOT EXISTS base_doc_version (
            id VARCHAR(96) PRIMARY KEY, doc_id VARCHAR(64), version_no INTEGER,
            object_key VARCHAR(1024), doc_name VARCHAR(512), size BIGINT,
            content_type VARCHAR(256), modifier VARCHAR(64), create_time VARCHAR(64)
        )""",
    )
    for statement in statements:
        op.execute(statement)

    rules = (
        ("builtin-logs", "日志服务自访问", "/logs/**", "防止日志查询递归采集"),
        ("builtin-health", "健康检查", "/actuator/**", "忽略健康探针"),
        ("builtin-docs", "接口文档", "/docs*", "忽略接口文档"),
        ("builtin-openapi", "OpenAPI", "/openapi.json", "忽略 OpenAPI 拉取"),
    )
    for rule_id, rule_name, path_pattern, remark in rules:
        op.execute(
            f"""INSERT INTO gateway_log_filter_rule
            (rule_id, rule_name, enabled, builtin, path_pattern, method, service_id,
             status_code, remark, hit_count, last_hit_time, create_time, update_time)
            SELECT '{rule_id}', '{rule_name}', 1, 1, '{path_pattern}', NULL, NULL, NULL,
                   '{remark}', 0, NULL,
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            WHERE NOT EXISTS (
                SELECT 1 FROM gateway_log_filter_rule WHERE rule_id = '{rule_id}'
            )"""
        )


def downgrade() -> None:
    pass
