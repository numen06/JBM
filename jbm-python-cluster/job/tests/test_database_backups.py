import gzip
import hashlib
import json
import tempfile
import subprocess
import shutil
import unittest
from datetime import datetime, timezone
from unittest.mock import Mock

from fastapi import FastAPI
import httpx

from jbm_cluster_py.platform.job.database_backups import DatabaseBackups, database_name, next_run
from jbm_cluster_py.platform.job.database_backup_router import build_database_backup_router


CONFIG = {"url": "mysql+asyncmy://operator:private-password@127.0.0.1:3306/jbm_test"}


class BackupTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.service = DatabaseBackups(self.tmp.name, CONFIG)

    def tearDown(self):
        self.service.worker_lock.close()
        self.tmp.cleanup()

    def test_default_source_is_idempotent_and_secrets_stay_private(self):
        policy = self.service.save_policy("jbm-default", {"enabled": True, "time": "03:45", "retentionDays": 30})
        self.service.worker_lock.close()
        self.service = DatabaseBackups(self.tmp.name, CONFIG)
        self.assertEqual(len(self.service.sources()), 1)
        self.assertEqual(self.service.policy("jbm-default"), policy)
        self.assertNotIn("private-password", json.dumps(self.service.sources()))
        self.assertNotIn("credential", self.service.sources()[0])
        self.assertEqual(self.service.sources()[0]["databaseType"], "MySQL")

    @unittest.skipUnless(shutil.which("mysqldump"), "native MySQL client required")
    def test_shared_credentials_file_is_supported_by_both_native_clients(self):
        with self.service.client_file(self.service.source("jbm-default")) as config:
            for client in ("mysql", "mysqldump"):
                result = subprocess.run([client, "--defaults-extra-file=" + config, "--help"],
                                        stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
                self.assertEqual(result.returncode, 0, result.stderr.decode())

    def test_scheduler_runs_once_and_rejects_concurrent_work(self):
        self.service.save_policy("jbm-default", {"enabled": True, "time": "02:00"})
        policy = self.service.policy("jbm-default")
        policy["nextRunAt"] = "2020-01-01T00:00:00+00:00"
        with self.service.connect() as db:
            db.execute("UPDATE policies SET document=?", (json.dumps(policy),))
        self.service.schedule()
        self.service.schedule()
        self.assertEqual(self.service.tasks("backup")["total"], 1)
        self.assertEqual(self.service.tasks("backup")["items"][0]["origin"], "scheduled")
        with self.assertRaises(ValueError):
            self.service.enqueue("backup", "jbm-default", "test")

    def test_policy_time_is_beijing_and_validated(self):
        now = datetime(2026, 10, 22, 17, 0, tzinfo=timezone.utc)
        self.assertEqual(next_run({"frequency": "daily", "time": "02:00"}, now), "2026-10-22T18:00:00+00:00")
        with self.assertRaises(ValueError):
            self.service.save_policy("jbm-default", {"time": "25:00"})
        for name in ("mysql", "bad`name", "x; DROP DATABASE y", "../escape"):
            with self.assertRaises(ValueError):
                database_name(name)

    def test_same_server_discovery_reuses_credentials_without_exposing_them(self):
        self.service.query = Mock(side_effect=lambda source, sql, database=None:
                                  "jbm_test\t63\nother_db\t4" if "SCHEMA_NAME,COUNT" in sql else "8.4.10")
        self.service.metadata = Mock(return_value={"tables": [["example", "BASE TABLE", "InnoDB"]]})
        available = self.service.discover_databases("jbm-default")
        self.assertEqual(available, [{"database": "jbm_test", "tableCount": 63, "connected": True},
                                    {"database": "other_db", "tableCount": 4, "connected": False}])
        result = self.service.attach_database("jbm-default", "other_db")
        self.assertEqual(result["status"], "online")
        self.assertNotIn("credential", result)
        original = self.service.source("jbm-default")
        attached = self.service.source(result["id"])
        self.assertEqual(attached["credential"], original["credential"])
        self.assertFalse(self.service.policy(result["id"])["enabled"])
        with self.assertRaises(ValueError):
            self.service.attach_database("jbm-default", "other_db")
        with self.assertRaises(ValueError):
            self.service.attach_database("jbm-default", "missing_db")

    def fake_backup(self):
        task = self.service.enqueue("backup", "jbm-default", "test")
        path = self.service.archives / (task["id"] + ".sql.gz")
        with gzip.open(path, "wb") as file:
            file.write(b"CREATE TABLE example(id INT);\n")
        task.update(status="success", sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                    filename="test.sql.gz", manifest={"tables": [["example", "BASE TABLE", "InnoDB"]],
                    "routines": 0, "events": 0, "triggers": 0})
        with self.service.connect() as db:
            self.service.write_task(db, task)
        return task, path

    def test_task_filters_apply_to_counts_pagination_and_restore_records(self):
        other = self.service.save_source({"name": "Other", "host": "127.0.0.1", "database": "other_db", "username": "operator"})
        with self.service.connect() as db:
            for source_id in ("jbm-default", other):
                for kind in ("backup", "restore"):
                    for index in range(3):
                        task = {"id": f"{source_id}-{kind}-{index}", "kind": kind, "sourceId": source_id,
                                "status": "success", "createdAt": f"2026-10-22T00:0{index}:00+00:00"}
                        self.service.write_task(db, task)
        for kind in ("backup", "restore"):
            result = self.service.tasks(kind, page=2, size=2, source_id=other)
            self.assertEqual(result["total"], 3)
            self.assertEqual(len(result["items"]), 1)
            self.assertEqual(result["items"][0]["sourceId"], other)
            self.assertEqual(result["items"][0]["id"], f"{other}-{kind}-0")
            self.assertEqual(self.service.tasks(kind)["total"], 6)
        with self.assertRaises(ValueError):
            self.service.tasks("backup", source_id="missing")

    def test_replace_requires_confirmation_and_integrity_failure_cannot_reach_mysql(self):
        task, path = self.fake_backup()
        with self.assertRaises(ValueError):
            self.service.restore_options(task["id"], "replace", "jbm_test", "wrong", True)
        with self.assertRaises(ValueError):
            self.service.restore_options(task["id"], "replace", "jbm_test", "jbm_test", False)
        options = self.service.restore_options(task["id"], "new", "restored_test", "", False)
        restore = self.service.enqueue("restore", "jbm-default", "test", options)
        path.write_bytes(b"corrupt backup")
        self.service.query = Mock(side_effect=AssertionError("must not execute SQL"))
        self.service.run_next()
        self.assertEqual(self.service.task(restore["id"])["status"], "failed")
        self.service.query.assert_not_called()

    def test_source_change_blocks_old_backup_restore(self):
        task, _ = self.fake_backup()
        source = self.service.source("jbm-default")
        source["host"] = "another-server"
        with self.service.connect() as db:
            db.execute("UPDATE sources SET document=?", (json.dumps(source),))
        with self.assertRaises(ValueError):
            self.service.restore_options(task["id"], "new", "restored_test", "", False)


class AuthorizationTests(unittest.IsolatedAsyncioTestCase):
    async def test_tenant_cannot_operate_database_even_directly(self):
        app = FastAPI()
        app.include_router(build_database_backup_router())

        @app.middleware("http")
        async def identity(request, call_next):
            request.state.identity = {"userId": 55, "roles": ["tenant_admin"]}
            return await call_next(request)

        async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
            for method, path, body in [("GET", "/sources", None), ("POST", "/backups", {"sourceId": "jbm-default"}),
                                       ("POST", "/restores", {}), ("GET", "/backups/test/file", None)]:
                result = await client.request(method, "/sysJob/database-backups" + path, json=body)
                self.assertEqual(result.status_code, 403)


if __name__ == "__main__":
    unittest.main()
