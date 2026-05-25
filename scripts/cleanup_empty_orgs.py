#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Delete base_org rows with empty org_name via Gateway (E2E residue cleanup)."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from jbm_cluster_client import gateway_api, login_password, unwrap  # noqa: E402


def main() -> int:
    token = login_password("admin", "Admin@123")
    deleted = 0
    for page in range(1, 20):
        _, jb, _ = gateway_api(
            "POST",
            "/baseOrg/pageList",
            token,
            {"pageForm": {"currPage": page, "pageSize": 50}},
        )
        data = unwrap(jb) or {}
        rows = data.get("contents") or []
        if not rows:
            break
        for row in rows:
            oid = row.get("id") or row.get("orgId")
            name = (row.get("orgName") or "").strip()
            if str(oid) == "1" or oid == 1:
                continue
            if name:
                continue
            gateway_api("POST", "/baseOrg/delete", token, {"baseOrg": {"id": oid}})
            deleted += 1
    print(f"deleted_empty_orgs={deleted}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
