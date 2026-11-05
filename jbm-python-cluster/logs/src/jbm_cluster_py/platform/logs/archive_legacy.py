"""Verified migration of legacy access/login history into the existing logs service.

Run inside the logs environment after provisioning its persistent backup mount.
Raw backups are private; online Loki records redact credentials. No business data
or accounts are altered. --delete-verified compares the original row in a locked
transaction after backup and Loki readback, never truncates a table.
"""
from __future__ import annotations

import argparse
import asyncio
import hashlib
import json
import logging
import os
import re
from datetime import UTC, datetime
from pathlib import Path

from sqlalchemy import inspect, text
from jbm_cluster_py.common.security_audit import login_succeeded
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.logs.loki import LokiSink
from jbm_cluster_py.platform.logs.repository import LogsRepository
from jbm_cluster_py.platform.logs.security_audit import SecurityAuditStore


def canonical(value):
    return json.dumps(value, ensure_ascii=True, sort_keys=True, separators=(",", ":"), default=str)


def redact(value):
    if isinstance(value, dict):
        return {k: "***" if any(word in k.lower().replace("_", "").replace("-", "") for word in ("authorization", "password", "secret", "token", "cookie", "signature", "credential", "apikey")) else redact(v) for k, v in value.items()}
    if isinstance(value, list):
        return [redact(item) for item in value]
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
            if isinstance(parsed, (dict, list)):
                return canonical(redact(parsed))
        except ValueError:
            pass
        return re.sub(r"(?i)Bearer\s+\S+|eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+", "***", value)
    return value


def event_for(table, pk, row):
    raw = canonical(row)
    digest = hashlib.sha256(raw.encode()).hexdigest()
    safe = redact(json.loads(raw))
    stamp = row.get("login_time") if table == "base_account_logs" else row.get("request_time")
    parsed = datetime.fromisoformat(str(stamp).replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=UTC)
    event = {"eventId": hashlib.sha256((table + ":" + str(row[pk])).encode()).hexdigest()[:32], "timestamp": int(parsed.timestamp()*1000),
             "source": "legacy-login" if table == "base_account_logs" else "legacy-gateway", "legacySha256": digest, "legacy": safe,
             "originalId": str(row[pk]), "phase": "RESULT"}
    if table == "base_account_logs":
        event.update(category="login", userId=row.get("user_id"), account=row.get("account"), operation=row.get("login_type") or row.get("account_type"),
            loginType=row.get("login_type") or row.get("account_type"), ip=row.get("login_ip") or row.get("ip"),
            success=login_succeeded(row.get("status", row.get("login_status"))), userAgent=row.get("user_agent") or row.get("login_agent"),
            loginLocation=row.get("login_location"), riskLevel=row.get("risk_level"), requestId=row.get("request_id"), clientId=row.get("client_id"), message=row.get("message"))
    else:
        from jbm_cluster_py.platform.logs.repository import LogsRepository
        from jbm_cluster_py.common.api_access import read_operation
        gateway = LogsRepository._gateway_from_db(safe)
        status = row.get("http_status")
        event.update(category="access", userId=row.get("request_user_id"), account=row.get("request_real_name"),
            operation=row.get("operation_type") or ("READ" if read_operation(str(row.get("method") or "GET"), str(row.get("path") or "/")) else row.get("method")), method=row.get("method"), serviceId=row.get("service_id"),
            target=row.get("path"), ip=row.get("ip"), success=int(status)<400 if status is not None else None,
            httpStatus=row.get("http_status"), gateway=gateway)
    return event, raw


def backup_source(store, table, event, raw):
    source_backup = store.backup / "legacy-source" / table / (event["eventId"] + ".json")
    source_backup.parent.mkdir(parents=True, exist_ok=True)
    if source_backup.exists():
        if source_backup.read_text(encoding="utf8") != raw:
            raise ValueError("原日志已变化，保留SQL并停止迁移")
    else:
        with source_backup.open("x", encoding="utf8") as stream:
            stream.write(raw);stream.flush();os.fsync(stream.fileno())
        os.chmod(source_backup, 0o400)
        if os.name != "nt":
            directory = os.open(source_backup.parent, os.O_RDONLY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
    if hashlib.sha256(source_backup.read_bytes()).hexdigest() != event["legacySha256"]:
        raise ValueError("原日志备份校验失败")


async def migrate(config, *, delete=False, limit=None):
    repository = LogsRepository(config.database)
    settings = dict(config.get("jbm.logs", {}) or {})
    sink = LokiSink(settings.get("loki", {}) or {})
    store = SecurityAuditStore({**dict(config.get("jbm.security-audit", {}) or {}), **dict(settings.get("security-audit", {}) or {})}, loki=sink)
    await store.start()
    if not store.enabled:
        raise ValueError("先配置并验证日志服务归档，禁止静默回退SQL历史")
    results = []
    try:
        for table in ("base_account_logs", "gateway_logs"):
            async with repository.engine.connect() as conn:
                meta = await conn.run_sync(lambda c: inspect(c).get_pk_constraint(table))
            keys = meta.get("constrained_columns") or []
            if len(keys) != 1 or not re.fullmatch(r"[a-z_]+", keys[0]):
                raise ValueError("日志主键不符合迁移约束")
            pk = keys[0]
            last = None
            last_time = None
            time_column = "login_time" if table == "base_account_logs" else "request_time"
            count = removed = 0
            while limit is None or count < limit:
                async with repository.engine.connect() as conn:
                    rows = (await conn.execute(text(f"SELECT * FROM {table}" + (f" WHERE {time_column}>:time OR ({time_column}=:time AND {pk}>:last)" if last is not None else "") + f" ORDER BY {time_column},{pk} LIMIT 100"), {"last": last, "time": last_time} if last is not None else {})).mappings().all()
                if not rows:
                    break
                batch = []
                for row in rows:
                    if limit is not None and count + len(batch) >= limit:
                        break
                    row = dict(row)
                    event, raw = event_for(table, pk, row)
                    batch.append((row, event, raw))
                await asyncio.gather(*(asyncio.to_thread(backup_source, store, table, event, raw) for _, event, raw in batch))
                await store.handle_many([event for _, event, _ in batch])
                async with repository.engine.begin() as conn:
                    for row, event, raw in batch:
                        if delete:
                            lock = "" if repository._sqlite else " FOR UPDATE"
                            current = (await conn.execute(text(f"SELECT * FROM {table} WHERE {pk}=:id" + lock), {"id": row[pk]})).mappings().first()
                            if current is not None:
                                if canonical(dict(current)) != raw:
                                    raise ValueError("原日志并发变化，禁止删除")
                                await conn.execute(text(f"DELETE FROM {table} WHERE {pk}=:id"), {"id": row[pk]})
                                removed += 1
                        last = row[pk]
                        last_time = row[time_column]
                        count += 1
                print(json.dumps({"table": table, "verified": count, "deleted": removed}), flush=True)
            async with repository.engine.connect() as conn:
                remaining = (await conn.execute(text(f"SELECT COUNT(*) FROM {table}"))).scalar()
            results.append({"table": table, "verified": count, "deleted": removed, "remainingSqlRows": remaining})
        return results
    finally:
        await sink.stop()
        await repository.stop()


if __name__ == "__main__":
    logging.getLogger("httpx").setLevel(logging.WARNING)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--delete-verified", action="store_true")
    parser.add_argument("--limit", type=int)
    options = parser.parse_args()
    print(json.dumps(asyncio.run(migrate(AppConfig.load(app="logs"), delete=options.delete_verified, limit=options.limit))))
