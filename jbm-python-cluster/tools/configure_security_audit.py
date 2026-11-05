"""Provision audit settings inside the existing logs service; default is read-only.

Prepare and verify persistent Loki and the logs backup volume before --apply.
Secrets are generated server-side, backed up privately and never printed.
"""
import argparse
import asyncio
import hashlib
import json
import logging
import os
from pathlib import Path
import secrets

import yaml
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient


async def configure(apply=False):
    config = AppConfig.load(app="logs")
    backup = Path("/app/data/security-audit-backup/config-before-security-audit")
    nacos = NacosDiscoveryClient({**config.nacos_discovery, **config.nacos_config})
    await nacos.start()
    result = []
    try:
        for app in ("common", "logs", "auth"):
            data_id = f"{app}-{config.profile}.yml"
            old = await nacos.get_config(data_id)
            if not old.strip():
                raise ValueError("Missing configuration: " + data_id)
            data = yaml.safe_load(old)
            jbm = data.setdefault("jbm", {})
            if app == "common":
                jbm.setdefault("security-audit", {})["enabled"] = True
                jbm.setdefault("admin-access", {}).setdefault("mode", "monitor")
            elif app == "logs":
                audit = jbm.setdefault("logs", {}).setdefault("security-audit", {})
                if apply:
                    audit.setdefault("integrity-key", secrets.token_hex(32))
                audit.setdefault("integrity-key-id", "v1")
                audit["keep-days"] = max(365, int(audit.get("keep-days", 365)))
                audit["backup-directory"] = "/app/data/security-audit-backup"
            else:
                jbm.setdefault("auth", {}).setdefault("password-policy", {}).setdefault("max-age-days", 90)
            row = {"config": data_id, "beforeSha256": hashlib.sha256(old.encode()).hexdigest(), "applied": apply}
            if apply:
                backup.mkdir(parents=True, exist_ok=True)
                os.chmod(backup, 0o700)
                path = backup / data_id
                if not path.exists():
                    with path.open("x", encoding="utf8") as stream:
                        stream.write(old)
                        stream.flush()
                        os.fsync(stream.fileno())
                    os.chmod(path, 0o600)
                if await nacos.get_config(data_id) != old:
                    raise RuntimeError("Configuration changed concurrently: " + data_id)
                new = yaml.safe_dump(data, allow_unicode=True, sort_keys=False)
                await nacos.publish_config(data_id, new)
                if yaml.safe_load(await nacos.get_config(data_id)) != data:
                    raise RuntimeError("Configuration verification failed: " + data_id)
                row["afterSha256"] = hashlib.sha256(new.encode()).hexdigest()
            result.append(row)
    finally:
        await nacos.stop()
    return {"configs": result, "existingCredentialsChanged": False, "sourceRestrictionEnabledByTool": False}


if __name__ == "__main__":
    logging.disable(logging.CRITICAL)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    options = parser.parse_args()
    print(json.dumps(asyncio.run(configure(options.apply))))
