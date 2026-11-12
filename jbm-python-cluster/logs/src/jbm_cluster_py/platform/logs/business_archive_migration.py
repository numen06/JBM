"""Archive legacy business-log lines and retire only the exact verified SQL rows."""

import argparse
import asyncio
import hashlib
import json
import os
import tempfile
from pathlib import Path

from sqlalchemy import inspect, text

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.logs.business_archive import BusinessLineArchive
from jbm_cluster_py.platform.logs.loki import LokiSink
from jbm_cluster_py.platform.logs.repository import LogsRepository
from jbm_cluster_py.platform.logs.security_audit import SecurityAuditStore


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), default=str)


def preserve(directory, key, value):
    data = canonical(value).encode()
    digest = hashlib.sha256(data).hexdigest()
    target = directory / f"{key}-{digest}.json"
    with tempfile.NamedTemporaryFile(dir=directory) as temporary:
        temporary.write(data)
        temporary.flush()
        os.fsync(temporary.fileno())
        try:
            os.link(temporary.name, target)
        except FileExistsError:
            pass
    if target.read_bytes() != data:
        raise ValueError("Legacy business-log backup read-back differs")
    fd = os.open(directory, os.O_RDONLY | os.O_DIRECTORY)
    try:
        os.fsync(fd)
    finally:
        os.close(fd)


async def migrate(repository, archive, directory, *, retire=False):
    if not directory.is_absolute():
        raise ValueError("Specify an absolute, independent backup directory")
    directory.mkdir(parents=True, exist_ok=True, mode=0o700)
    async with repository.engine.connect() as conn:
        if not await conn.run_sync(lambda c: inspect(c).has_table("business_log_line")):
            return {"missing": True}
        schema = (await conn.execute(text("SHOW CREATE TABLE business_log_line"))).one()[1]
    preserve(directory, "schema", schema)
    cursor = {"log": "", "line": 0}
    verified = removed = 0
    while True:
        async with repository.engine.connect() as conn:
            result = await conn.execute(
                text("SELECT * FROM business_log_line WHERE log_id>:log OR "
                     "(log_id=:log AND line_number>:line) ORDER BY log_id,line_number LIMIT 100"), cursor
            )
            rows = [dict(row) for row in result.mappings()]
        if not rows:
            break
        for row in rows:
            log_id, number = row["log_id"], int(row["line_number"])
            summary = await repository.business_summary(log_id)
            if not summary:
                raise ValueError("Legacy business log has no matching summary")
            identity = hashlib.sha256(canonical([log_id, number]).encode()).hexdigest()
            preserve(directory, identity, {"row": row, "logCreatedAt": summary["createTime"]})
            await archive.write_verified(summary, [json.loads(canonical(row))], "legacy-" + identity)
            verified += 1
            if retire:
                args = {"log": log_id, "line": number}
                where = " WHERE log_id=:log AND line_number=:line"
                async with repository.engine.begin() as conn:
                    stored = (await conn.execute(
                        text("SELECT * FROM business_log_line" + where + " FOR UPDATE"), args
                    )).mappings().first()
                    if stored is None or dict(stored) != row:
                        raise ValueError("Legacy business-log row changed; original retained")
                    await conn.execute(text("DELETE FROM business_log_line" + where), args)
                removed += 1
            cursor = {"log": log_id, "line": number}
    async with repository.engine.connect() as conn:
        remaining = (await conn.execute(text("SELECT COUNT(*) FROM business_log_line"))).scalar_one()
    return {"verified": verified, "removed": removed, "remaining": remaining}


async def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backup", type=Path, required=True)
    parser.add_argument("--retire", action="store_true", help="Enable only after the legacy SQL writers have stopped")
    args = parser.parse_args()
    config = AppConfig.load(app="logs")
    logs = dict(config.get("jbm.logs", {}) or {})
    loki = LokiSink(logs.get("loki", {}) or {})
    audit = SecurityAuditStore({**dict(config.get("jbm.security-audit", {}) or {}),
                                **dict(logs.get("security-audit", {}) or {})}, loki=loki)
    repository = LogsRepository(config.database)
    try:
        result = await migrate(repository, BusinessLineArchive(loki, audit.tenant), args.backup, retire=args.retire)
        print(json.dumps(result), flush=True)
    finally:
        await loki.stop()
        await repository.stop()


if __name__ == "__main__":
    asyncio.run(main())
