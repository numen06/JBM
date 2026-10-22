"""Persistent, single-worker MySQL backup and restore operations."""
from __future__ import annotations

import asyncio
import gzip
import hashlib
import json
import os
import re
import shutil
import sqlite3
import subprocess
import tempfile
import threading
import uuid
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

from cryptography.fernet import Fernet
from sqlalchemy.engine import make_url

from jbm_cluster_py.integrations.database import configured_database_url


def utcnow():
    return datetime.now(timezone.utc)


def next_run(policy, now=None):
    now = now or utcnow()
    local = now.astimezone(ZoneInfo("Asia/Shanghai"))
    hour, minute = map(int, policy["time"].split(":"))
    result = local.replace(hour=hour, minute=minute, second=0, microsecond=0)
    if policy["frequency"] == "weekly":
        result += timedelta(days=(policy["weekday"] - result.weekday()) % 7)
        if result <= local:
            result += timedelta(days=7)
    elif result <= local:
        result += timedelta(days=1)
    return result.astimezone(timezone.utc).isoformat()


def database_name(value):
    if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}", value):
        raise ValueError("数据库名仅支持字母、数字、下划线、点和连字符，最多 64 字符")
    if value.lower() in {"mysql", "sys", "information_schema", "performance_schema"}:
        raise ValueError("不能操作 MySQL 系统数据库")
    return value


class DatabaseBackups:
    def __init__(self, root, default_database):
        self.root = Path(root).resolve()
        self.archives = self.root / "archives"
        self.archives.mkdir(parents=True, exist_ok=True)
        self.worker_lock = (self.root / ".worker.lock").open("a")
        if os.name != "nt":
            import fcntl
            fcntl.flock(self.worker_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        self.state = self.root / "operations.sqlite3"
        key = self.root / "credentials.key"
        if not key.exists():
            with key.open("xb") as out:
                out.write(Fernet.generate_key())
            key.chmod(0o600)
        self.cipher = Fernet(key.read_bytes())
        with self.connect() as db:
            db.executescript("""
                PRAGMA journal_mode=WAL;
                CREATE TABLE IF NOT EXISTS sources(id TEXT PRIMARY KEY, document TEXT NOT NULL);
                CREATE TABLE IF NOT EXISTS policies(source_id TEXT PRIMARY KEY, document TEXT NOT NULL);
                CREATE TABLE IF NOT EXISTS tasks(id TEXT PRIMARY KEY, kind TEXT NOT NULL,
                    source_id TEXT NOT NULL, status TEXT NOT NULL, created_at TEXT NOT NULL,
                    document TEXT NOT NULL);
                CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY, at TEXT, actor TEXT,
                    action TEXT, target TEXT);
            """)
            # A killed dump/import must never become a successful backup or be replayed.
            for row in db.execute("SELECT id,document FROM tasks WHERE status='running'").fetchall():
                task = json.loads(row["document"])
                task.update(status="failed", error="服务中断，请核对目标数据库后重新操作", finishedAt=utcnow().isoformat())
                self.write_task(db, task)
        for partial in self.archives.glob("*.partial"):
            if re.fullmatch(r"[a-f0-9]{32}\.partial", partial.name):
                partial.unlink()
        raw = configured_database_url(default_database)
        if not raw:
            raise ValueError("JBM 默认数据库配置缺失")
        url = make_url(raw)
        if not url.drivername.startswith("mysql"):
            raise ValueError("默认数据库必须为 MySQL")
        source = {"id": "jbm-default", "name": "JBM 默认数据库", "host": os.getenv("JBM_DB_BACKUP_DEFAULT_HOST") or url.host,
                  "port": url.port or 3306, "database": database_name(url.database),
                  "username": url.username, "password": url.password or "", "sslMode": "PREFERRED"}
        old = self.source("jbm-default", required=False)
        self.save_source(source, default=True)
        if old and self.identity(old) != self.identity(self.source("jbm-default")):
            policy = self.policy("jbm-default")
            if policy["enabled"]:
                self.save_policy("jbm-default", {**policy, "enabled": False})

    @contextmanager
    def connect(self):
        db = sqlite3.connect(self.state, timeout=15)
        db.row_factory = sqlite3.Row
        try:
            with db:
                yield db
        finally:
            db.close()

    def audit(self, actor, action, target):
        with self.connect() as db:
            db.execute("INSERT INTO audit(at,actor,action,target) VALUES(?,?,?,?)",
                       (utcnow().isoformat(), str(actor), action, target))

    @staticmethod
    def identity(source):
        return {key: source[key] for key in ("host", "port", "database")}

    def source(self, source_id, required=True):
        with self.connect() as db:
            row = db.execute("SELECT document FROM sources WHERE id=?", (source_id,)).fetchone()
        if not row:
            if required:
                raise ValueError("数据库接入不存在")
            return None
        return json.loads(row[0])

    def sources(self):
        with self.connect() as db:
            rows = db.execute("SELECT document FROM sources ORDER BY id='jbm-default' DESC,id").fetchall()
        result = []
        for row in rows:
            source = json.loads(row[0])
            source.pop("credential", None)
            source.setdefault("databaseType", "MySQL")
            source["policy"] = self.policy(source["id"])
            with self.connect() as db:
                last = db.execute("SELECT document FROM tasks WHERE source_id=? AND kind='backup' "
                                  "AND status='success' ORDER BY created_at DESC LIMIT 1", (source["id"],)).fetchone()
            source["lastBackupAt"] = json.loads(last[0])["finishedAt"] if last else None
            result.append(source)
        return result

    def save_source(self, values, default=False):
        source_id = values.get("id") or uuid.uuid4().hex
        if source_id != "jbm-default" and not re.fullmatch(r"[a-f0-9]{32}", source_id):
            raise ValueError("无效的数据源标识")
        old = self.source(source_id, required=False)
        if source_id == "jbm-default" and not default:
            raise ValueError("JBM 默认连接由系统配置维护")
        host = str(values.get("host") or "").strip()
        if not re.fullmatch(r"[A-Za-z0-9_.:-]{1,253}", host):
            raise ValueError("请输入有效的数据库主机名或 IP")
        port = int(values.get("port", 3306))
        name = str(values.get("name") or "").strip()
        username = str(values.get("username") or "").strip()
        if not 1 <= port <= 65535 or not name or len(name) > 80 or not username or len(username) > 128:
            raise ValueError("请检查连接名称、端口和用户名")
        mode = values.get("sslMode", "PREFERRED")
        if mode not in {"PREFERRED", "REQUIRED"}:
            raise ValueError("不支持的 TLS 模式")
        password = values.get("password")
        credential = old.get("credential") if old else None
        if password is not None and (password or not credential or default):
            credential = self.cipher.encrypt(str(password).encode()).decode()
        if not credential:
            credential = self.cipher.encrypt(b"").decode()
        source = {"id": source_id, "name": name, "host": host, "port": port,
                  "database": database_name(values.get("database")), "databaseType": "MySQL", "username": username,
                  "credential": credential, "sslMode": mode, "isDefault": default,
                  "status": "unknown", "updatedAt": utcnow().isoformat()}
        if old and all(old.get(key) == source.get(key) for key in ("host", "port", "database", "username", "sslMode")):
            source.update({key: old[key] for key in ("status", "version", "tableCount", "checkedAt", "error") if key in old})
        with self.connect() as db:
            if db.execute("SELECT 1 FROM tasks WHERE source_id=? AND status IN ('queued','running')", (source_id,)).fetchone():
                if not default:
                    raise ValueError("数据库有执行中的任务，暂不能修改接入")
            db.execute("INSERT OR REPLACE INTO sources VALUES(?,?)", (source_id, json.dumps(source)))
        return source_id

    def discover_databases(self, source_id):
        source = self.source(source_id)
        rows = self.query(source, "SELECT s.SCHEMA_NAME,COUNT(t.TABLE_NAME) FROM information_schema.SCHEMATA s "
                          "LEFT JOIN information_schema.TABLES t ON t.TABLE_SCHEMA=s.SCHEMA_NAME "
                          "WHERE s.SCHEMA_NAME NOT IN ('mysql','sys','information_schema','performance_schema') "
                          "GROUP BY s.SCHEMA_NAME ORDER BY s.SCHEMA_NAME;")
        existing = {(item["host"], item["port"], item["database"]) for item in self.sources()}
        return [{"database": name, "tableCount": int(count),
                 "connected": (source["host"], source["port"], name) in existing}
                for name, count in (row.split("\t") for row in rows.splitlines() if row)]

    def attach_database(self, source_id, name):
        name = database_name(name)
        available = self.discover_databases(source_id)
        item = next((item for item in available if item["database"] == name), None)
        if not item:
            raise ValueError("该连接无法访问所选数据库")
        if item["connected"]:
            raise ValueError("该数据库已经接入")
        source = self.source(source_id)
        identity = {"host": source["host"], "port": source["port"], "database": name}
        key = hashlib.sha256(json.dumps(identity, sort_keys=True).encode()).hexdigest()[:32]
        source.update(id=key, name=name, database=name, isDefault=False, status="unknown", error="")
        for field in ("version", "tableCount", "checkedAt"):
            source.pop(field, None)
        with self.connect() as db:
            db.execute("INSERT OR IGNORE INTO sources VALUES(?,?)", (key, json.dumps(source)))
        return self.test_source(key)

    @contextmanager
    def client_file(self, source):
        password = self.cipher.decrypt(source["credential"].encode()).decode()
        def escaped(value):
            return str(value).replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r")
        with tempfile.TemporaryDirectory(prefix="jbm-db-") as tmp:
            config = Path(tmp) / "client.cnf"
            lines = {"host": source["host"], "port": source["port"], "user": source["username"],
                     "password": password, "ssl-mode": source["sslMode"], "protocol": "TCP",
                     "default-character-set": "utf8mb4"}
            config.write_text("[client]\n" + "".join(f'{key}="{escaped(value)}"\n' for key, value in lines.items())
                              + "[mysql]\nconnect-timeout=10\n", encoding="utf-8")
            config.chmod(0o600)
            yield str(config)

    def query(self, source, sql, database=None):
        with self.client_file(source) as config:
            args = ["mysql", "--defaults-extra-file=" + config, "--batch", "--skip-column-names", "--raw"]
            if database:
                args += [database_name(database)]
            result = subprocess.run(args, input=sql.encode(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=60)
        if result.returncode:
            raise RuntimeError(self.safe_error(result.stderr.decode(errors="replace"), source))
        return result.stdout.decode(errors="replace").strip()

    def safe_error(self, error, source):
        password = self.cipher.decrypt(source["credential"].encode()).decode()
        if password:
            error = error.replace(password, "***")
        return error[-1800:]

    def metadata(self, source, database=None):
        name = database_name(database or source["database"])
        # Identifiers use a deliberately restricted alphabet; SQL values are never commands.
        rows = self.query(source, "SELECT TABLE_NAME,TABLE_TYPE,COALESCE(ENGINE,'') FROM information_schema.TABLES "
                          f"WHERE TABLE_SCHEMA='{name}' ORDER BY TABLE_NAME;")
        tables = [row.split("\t") for row in rows.splitlines() if row]
        objects = self.query(source, f"SELECT (SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='{name}'),"
                             f"(SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA='{name}'),"
                             f"(SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='{name}');")
        counts = list(map(int, objects.split("\t")))
        encoding = self.query(source, "SELECT DEFAULT_CHARACTER_SET_NAME,DEFAULT_COLLATION_NAME "
                              f"FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='{name}';").split("\t")
        if len(encoding) != 2:
            raise ValueError("目标数据库不存在")
        return {"tables": tables, "routines": counts[0], "events": counts[1], "triggers": counts[2],
                "charset": encoding[0], "collation": encoding[1]}

    def test_source(self, source_id):
        source = self.source(source_id)
        source.setdefault("databaseType", "MySQL")
        try:
            version = self.query(source, "SELECT VERSION();", source["database"])
            meta = self.metadata(source)
            source.update(status="online", version=version, tableCount=len(meta["tables"]), error="")
        except Exception as exc:
            source.update(status="error", error=self.safe_error(str(exc), source))
        source["checkedAt"] = utcnow().isoformat()
        with self.connect() as db:
            db.execute("UPDATE sources SET document=? WHERE id=?", (json.dumps(source), source_id))
        source.pop("credential", None)
        return source

    def policy(self, source_id):
        with self.connect() as db:
            row = db.execute("SELECT document FROM policies WHERE source_id=?", (source_id,)).fetchone()
        return json.loads(row[0]) if row else {"enabled": False, "frequency": "daily", "time": "02:00",
                                              "weekday": 0, "retentionDays": 14, "nextRunAt": None}

    def save_policy(self, source_id, values):
        self.source(source_id)
        policy = {"enabled": values.get("enabled") is True, "frequency": values.get("frequency", "daily"),
                  "time": values.get("time", "02:00"), "weekday": int(values.get("weekday", 0)),
                  "retentionDays": int(values.get("retentionDays", 14))}
        if (policy["frequency"] not in {"daily", "weekly"} or not 0 <= policy["weekday"] <= 6
                or not 1 <= policy["retentionDays"] <= 365
                or not re.fullmatch(r"(?:[01]\d|2[0-3]):[0-5]\d", str(policy["time"]))):
            raise ValueError("请检查执行周期、时间和保留天数")
        policy["nextRunAt"] = next_run(policy) if policy["enabled"] else None
        with self.connect() as db:
            db.execute("INSERT OR REPLACE INTO policies VALUES(?,?)", (source_id, json.dumps(policy)))
        return policy

    @staticmethod
    def write_task(db, task):
        db.execute("INSERT OR REPLACE INTO tasks VALUES(?,?,?,?,?,?)", (task["id"], task["kind"],
                   task["sourceId"], task["status"], task["createdAt"], json.dumps(task)))

    def task(self, task_id):
        with self.connect() as db:
            row = db.execute("SELECT document FROM tasks WHERE id=?", (task_id,)).fetchone()
        if not row:
            raise ValueError("任务不存在")
        return json.loads(row[0])

    def tasks(self, kind, page=1, size=20, source_id=None):
        where, params = "kind=?", [kind]
        if source_id is not None:
            self.source(source_id)
            where += " AND source_id=?"
            params.append(source_id)
        with self.connect() as db:
            total = db.execute("SELECT COUNT(*) FROM tasks WHERE " + where, params).fetchone()[0]
            rows = db.execute("SELECT document FROM tasks WHERE " + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                              (*params, size, (page - 1) * size)).fetchall()
        return {"items": [json.loads(row[0]) for row in rows], "total": total}

    def enqueue(self, kind, source_id, actor, options=None):
        source = self.source(source_id)
        task = {"id": uuid.uuid4().hex, "kind": kind, "sourceId": source_id, "sourceName": source["name"],
                "database": source["database"], "sourceIdentity": self.identity(source), "actor": str(actor),
                "status": "queued", "createdAt": utcnow().isoformat(), "error": "", **(options or {})}
        with self.connect() as db:
            db.execute("BEGIN IMMEDIATE")
            if db.execute("SELECT 1 FROM tasks WHERE source_id=? AND status IN ('queued','running')", (source_id,)).fetchone():
                raise ValueError("该数据库已有执行中的任务")
            self.write_task(db, task)
        self.audit(actor, kind, task["id"])
        return task

    def backup(self, task, source):
        if shutil.disk_usage(self.root).free < 256 * 1024 * 1024:
            raise ValueError("备份目录可用空间不足 256 MB")
        meta = self.metadata(source)
        if not meta["tables"]:
            raise ValueError("数据库没有可备份的表")
        if any(row[1] == "BASE TABLE" and row[2] != "InnoDB" for row in meta["tables"]):
            raise ValueError("存在非 InnoDB 表，请先安排一致性备份维护窗口")
        temporary = self.archives / (task["id"] + ".partial")
        path = self.archives / (task["id"] + ".sql.gz")
        with self.client_file(source) as config, tempfile.TemporaryFile() as errors:
            args = ["mysqldump", "--defaults-extra-file=" + config, "--single-transaction", "--quick",
                    "--routines", "--events", "--triggers", "--hex-blob", "--no-tablespaces",
                    "--set-gtid-purged=OFF", "--column-statistics=0", source["database"]]
            process = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=errors)
            deadline = threading.Timer(3600, process.kill)
            deadline.start()
            try:
                with gzip.open(temporary, "wb", compresslevel=6) as out:
                    shutil.copyfileobj(process.stdout, out)
                code = process.wait(timeout=3600)
            finally:
                deadline.cancel()
                if process.poll() is None:
                    process.kill()
                    process.wait()
                process.stdout.close()
            if code:
                errors.seek(0)
                temporary.unlink(missing_ok=True)
                raise RuntimeError(self.safe_error(errors.read().decode(errors="replace"), source))
        if temporary.stat().st_size < 100:
            temporary.unlink(missing_ok=True)
            raise ValueError("备份文件为空")
        temporary.replace(path)
        path.chmod(0o600)
        with path.open("rb") as file:
            digest = hashlib.file_digest(file, "sha256").hexdigest()
        task.update(size=path.stat().st_size, sha256=digest,
                    manifest={**meta, "version": self.query(source, "SELECT VERSION();"), "source": self.identity(source)},
                    filename=source["database"] + "-" + task["createdAt"][:19].replace(":", "-") + ".sql.gz")
        (self.archives / (task["id"] + ".manifest.json")).write_text(json.dumps(task, ensure_ascii=False), encoding="utf-8")

    def backup_file(self, task):
        if task["kind"] != "backup" or task["status"] != "success":
            raise ValueError("备份尚未完成")
        path = self.archives / (task["id"] + ".sql.gz")
        if not path.is_file():
            raise ValueError("备份文件不存在")
        return path

    def restore_options(self, backup_id, mode, target, confirmation, maintenance):
        backup = self.task(backup_id)
        self.backup_file(backup)
        source = self.source(backup["sourceId"])
        if self.identity(source) != backup["sourceIdentity"]:
            raise ValueError("数据库接入地址已改变，请核对备份来源")
        if mode not in {"new", "replace"}:
            raise ValueError("请选择还原方式")
        target = database_name(target)
        if mode == "replace":
            if target != source["database"] or confirmation != target or maintenance is not True:
                raise ValueError("覆盖原库须输入准确库名，并确认已停止所有业务写入")
        elif target == source["database"]:
            raise ValueError("新库名称不能与原库相同")
        return {"backupId": backup_id, "mode": mode, "targetDatabase": target,
                "maintenanceConfirmed": maintenance is True}

    def restore(self, task, source):
        backup = self.task(task["backupId"])
        path = self.backup_file(backup)
        with path.open("rb") as file:
            digest = hashlib.file_digest(file, "sha256").hexdigest()
        if digest != backup["sha256"]:
            raise ValueError("备份文件校验失败，已停止还原")
        with gzip.open(path, "rb") as file:
            while file.read(1024 * 1024):
                pass
        target = database_name(task["targetDatabase"])
        meta = backup["manifest"]
        charset, collation = meta["charset"], meta["collation"]
        if not all(re.fullmatch(r"[A-Za-z0-9_]+", value) for value in (charset, collation)):
            raise ValueError("备份字符集或排序规则无效")
        version = self.query(source, "SELECT VERSION();")
        if version.split(".")[0] != meta["version"].split(".")[0]:
            raise ValueError("备份与目标 MySQL 主版本不同，请先验证版本兼容性")
        if task["mode"] == "new" and (meta["routines"] or meta["events"] or meta["triggers"]
                                      or any(row[1] == "VIEW" for row in meta["tables"])):
            raise ValueError("备份含视图或存储对象，需要在隔离实例中按原库名还原，不能直接改库名")
        exists = self.query(source, f"SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='{target}';") == "1"
        if task["mode"] == "new" and exists:
            raise ValueError("目标数据库已存在，请换一个新库名")
        if task["mode"] == "replace":
            grants = self.query(source, "SHOW GRANTS FOR CURRENT_USER;").upper()
            if not any(" ON *.* " in line and ("ALL PRIVILEGES" in line or "PROCESS" in line)
                       for line in grants.splitlines()):
                raise ValueError("覆盖原库需要 PROCESS 权限，以核查所有业务连接")
            connections = self.query(source, f"SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB='{target}' AND ID<>CONNECTION_ID();")
            if int(connections):
                raise ValueError("目标库仍有业务连接，请完成停写维护后重试")
            if meta["events"] and self.query(source, "SELECT @@event_scheduler;") != "OFF":
                raise ValueError("数据库含定时事件，请先在维护窗口关闭事件调度")
            safety = {"id": uuid.uuid4().hex, "kind": "backup", "sourceId": source["id"], "sourceName": source["name"],
                      "database": source["database"], "sourceIdentity": self.identity(source), "actor": task["actor"],
                      "createdAt": utcnow().isoformat(), "status": "running", "origin": "before_restore", "error": ""}
            with self.connect() as db:
                self.write_task(db, safety)
            try:
                self.backup(safety, source)
                safety.update(status="success", finishedAt=utcnow().isoformat())
            except Exception:
                safety.update(status="failed", error="还原前备份失败")
                raise
            finally:
                with self.connect() as db:
                    self.write_task(db, safety)
            task["safetyBackupId"] = safety["id"]
            with self.connect() as db:
                self.write_task(db, task)
            if int(self.query(source, f"SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB='{target}' AND ID<>CONNECTION_ID();")):
                raise ValueError("还原前备份期间检测到业务重新连接，已停止覆盖")
            self.query(source, f"DROP DATABASE `{target}`;")
        self.query(source, f"CREATE DATABASE `{target}` CHARACTER SET {charset} COLLATE {collation};")
        with self.client_file(source) as config, tempfile.TemporaryFile() as errors:
            process = subprocess.Popen(["mysql", "--defaults-extra-file=" + config, "--binary-mode", target],
                                       stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=errors)
            deadline = threading.Timer(3600, process.kill)
            deadline.start()
            try:
                with gzip.open(path, "rb") as file:
                    shutil.copyfileobj(file, process.stdin)
                process.stdin.close()
                code = process.wait(timeout=3600)
            finally:
                deadline.cancel()
                if process.poll() is None:
                    process.kill()
                    process.wait()
            if code:
                errors.seek(0)
                raise RuntimeError(self.safe_error(errors.read().decode(errors="replace"), source))
        actual = self.metadata(source, target)
        if actual != {key: meta[key] for key in ("tables", "routines", "events", "triggers", "charset", "collation")}:
            raise ValueError("还原后的数据库对象清单不匹配，请核对目标库")
        task["verifiedTables"] = len(actual["tables"])

    def delete_backup(self, task_id, actor):
        task = self.task(task_id)
        path = self.backup_file(task)
        if task.get("origin") == "before_restore":
            raise ValueError("还原前保护备份不能在页面删除")
        with self.connect() as db:
            for row in db.execute("SELECT document FROM tasks WHERE kind='restore'").fetchall():
                if json.loads(row[0]).get("backupId") == task_id:
                    raise ValueError("该备份有关联还原记录，不能删除")
            path.unlink()
            (self.archives / (task_id + ".manifest.json")).unlink(missing_ok=True)
            task.update(status="deleted")
            self.write_task(db, task)
        self.audit(actor, "delete_backup", task_id)

    def schedule(self):
        with self.connect() as db:
            rows = db.execute("SELECT source_id,document FROM policies").fetchall()
        now = utcnow()
        for row in rows:
            policy = json.loads(row[1])
            if not policy["enabled"] or not policy["nextRunAt"] or datetime.fromisoformat(policy["nextRunAt"]) > now:
                continue
            try:
                self.enqueue("backup", row[0], "scheduler", {"origin": "scheduled"})
            except ValueError:
                continue
            policy["nextRunAt"] = next_run(policy, now)
            with self.connect() as db:
                db.execute("UPDATE policies SET document=? WHERE source_id=?", (json.dumps(policy), row[0]))

    def cleanup(self, source_id):
        cutoff = (utcnow() - timedelta(days=self.policy(source_id)["retentionDays"])).isoformat()
        with self.connect() as db:
            rows = db.execute("SELECT id,document FROM tasks WHERE source_id=? AND kind='backup' AND status='success' "
                              "ORDER BY created_at DESC", (source_id,)).fetchall()
        for row in rows[1:]:
            task = json.loads(row[1])
            if task["createdAt"] < cutoff:
                try:
                    self.delete_backup(task["id"], "retention")
                except ValueError:
                    pass

    def run_next(self):
        with self.connect() as db:
            db.execute("BEGIN IMMEDIATE")
            row = db.execute("SELECT document FROM tasks WHERE status='queued' ORDER BY created_at LIMIT 1").fetchone()
            if not row:
                return
            task = json.loads(row[0])
            task.update(status="running", startedAt=utcnow().isoformat())
            self.write_task(db, task)
        source = self.source(task["sourceId"])
        try:
            if self.identity(source) != task["sourceIdentity"]:
                raise ValueError("执行前检测到数据库连接配置改变")
            (self.backup if task["kind"] == "backup" else self.restore)(task, source)
            task["status"] = "success"
        except Exception as exc:
            task.update(status="failed", error=self.safe_error(str(exc), source))
        task["finishedAt"] = utcnow().isoformat()
        with self.connect() as db:
            self.write_task(db, task)
        if task["kind"] == "backup" and task["status"] == "success":
            self.cleanup(task["sourceId"])

    async def worker(self):
        # ponytail: one worker per deployment; use a shared queue before scaling replicas.
        while True:
            try:
                await asyncio.to_thread(self.schedule)
                await asyncio.to_thread(self.run_next)
            except Exception:
                import logging
                logging.getLogger(__name__).exception("Database backup worker iteration failed")
            await asyncio.sleep(2)
