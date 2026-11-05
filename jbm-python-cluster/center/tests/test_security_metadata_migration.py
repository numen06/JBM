import importlib.util
from pathlib import Path

from alembic.migration import MigrationContext
from alembic.operations import Operations
from sqlalchemy import create_engine, text


def test_bit_history_and_interrupted_ddl_can_be_retried_without_changing_newer_state():
    path = Path(__file__).resolve().parents[1] / "migrations/versions/20261105_30_password_age.py"
    spec = importlib.util.spec_from_file_location("audit_metadata_migration", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    engine = create_engine("sqlite://")
    with engine.begin() as conn:
        conn.execute(text("CREATE TABLE base_account_logs(id INTEGER PRIMARY KEY,user_id INTEGER,login_time TEXT,login_type TEXT,status BLOB,ip TEXT)"))
        for row in ({"id": 1, "time": "2026-11-01", "status": b"\x01", "ip": "198.51.100.1"}, {"id": 2, "time": "2026-11-02", "status": b"\x00", "ip": "198.51.100.2"}):
            conn.execute(text("INSERT INTO base_account_logs VALUES (:id,1,:time,'PASSWORD',:status,:ip)"), row)
        # Model MySQL's auto-committed DDL left by a failed first attempt.
        conn.execute(text("CREATE TABLE base_login_security_state(user_id BIGINT PRIMARY KEY,ip VARCHAR(64),login_time VARCHAR(64))"))
        with Operations.context(MigrationContext.configure(conn)):
            module.upgrade()
        assert conn.execute(text("SELECT ip FROM base_login_security_state")).scalar() == "198.51.100.1"
        conn.execute(text("UPDATE base_login_security_state SET ip='203.0.113.1',login_time='2026-11-03'"))
        with Operations.context(MigrationContext.configure(conn)):
            module.upgrade()
        assert conn.execute(text("SELECT ip FROM base_login_security_state")).scalar() == "203.0.113.1"
        assert conn.execute(text("SELECT COUNT(*) FROM base_account_logs")).scalar() == 2
    engine.dispose()
