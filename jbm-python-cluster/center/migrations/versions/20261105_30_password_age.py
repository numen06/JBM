"""Current password lifecycle metadata; existing passwords remain unchanged."""
import sqlalchemy as sa
import json
from alembic import op

revision = "20261105_30"
down_revision = "20261104_29"
branch_labels = None
depends_on = None


def upgrade():
    if not sa.inspect(op.get_bind()).has_table("security_audit_outbox"):
        op.create_table("security_audit_outbox",
            sa.Column("event_id", sa.String(64), primary_key=True),
            sa.Column("payload", sa.Text(), nullable=False),
            sa.Column("published_at", sa.BigInteger(), nullable=True))
    if not sa.inspect(op.get_bind()).has_table("base_account_password_age"):
        op.create_table("base_account_password_age",
            sa.Column("account_id", sa.BigInteger(), primary_key=True),
            sa.Column("fingerprint", sa.String(64), nullable=False),
            sa.Column("changed_at", sa.BigInteger(), nullable=False))
    if not sa.inspect(op.get_bind()).has_table("base_login_security_state"):
        op.create_table("base_login_security_state",
            sa.Column("user_id", sa.BigInteger(), primary_key=True),
            sa.Column("ip", sa.String(64)), sa.Column("login_time", sa.String(64)))
        op.create_table("base_login_security_alert",
            sa.Column("alert_id", sa.BigInteger(), primary_key=True),
            sa.Column("user_id", sa.BigInteger(), nullable=False, index=True),
            sa.Column("payload", sa.Text(), nullable=False))
        conn = op.get_bind()
        if sa.inspect(conn).has_table("base_account_logs"):
            columns = {c["name"] for c in sa.inspect(conn).get_columns("base_account_logs")}
            primary = next((k for k in ("id", "account_log_id", "log_id") if k in columns), None)
            if primary:
                rows = conn.execute(sa.text(f"SELECT * FROM base_account_logs ORDER BY login_time,{primary}")).mappings().all()
                latest, alerts = {}, {}
                for row in rows:
                    uid = row.get("user_id")
                    if uid is None:
                        continue
                    kind = row.get("login_type") or row.get("account_type")
                    if int(row.get("status", row.get("login_status")) or 0) == 1 and kind in {"PASSWORD", "SMS", "FILE_KEY", "SSH_KEY", "PASSKEY", "WECHAT", "MINIAPP", "FACE", "THIRD_PARTY"}:
                        latest[uid] = {"id": uid, "ip": row.get("login_ip") or row.get("ip") or "", "time": str(row.get("login_time") or "")}
                    if row.get("risk_level") == "warning" and row.get("risk_ack_time") is None:
                        items = alerts.setdefault(uid, [])
                        items.append({"id": row[primary], "uid": uid, "payload": json.dumps({"id": str(row[primary]), "time": str(row.get("login_time") or ""), "clientId": row.get("client_id") or "", **json.loads(row.get("risk_detail") or "{}")}, ensure_ascii=False)})
                for current in latest.values():
                    conn.execute(sa.text("INSERT INTO base_login_security_state VALUES (:id,:ip,:time)"), current)
                for pending in alerts.values():
                    for current in pending:
                        conn.execute(sa.text("INSERT INTO base_login_security_alert VALUES (:id,:uid,:payload)"), current)


def downgrade():
    # Keep metadata on application rollback; never alter existing credentials.
    pass
