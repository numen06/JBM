#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verify Chinese extend-field form save/read via Gateway (UTF-8)."""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from jbm_cluster_client import DEFAULT_GATEWAY, http_request, login_password, parse_json, unwrap  # noqa: E402

FORM_NAME = "客户表单"
FIELD_LABEL = "客户等级"


def main() -> int:
    token = login_password("admin", "Admin@123")
    headers = {
        "Authorization": f"Bearer {token}",
        "tenantId": "0",
        "Content-Type": "application/json",
    }
    code = f"cn_form_{int(time.time()) % 1000000}"
    body = {
        "formName": FORM_NAME,
        "fields": [
            {
                "fieldName": "customerLevel",
                "fieldType": "string",
                "fieldLabel": FIELD_LABEL,
                "required": False,
            }
        ],
        "autoPublish": False,
    }
    status, raw = http_request(
        "POST",
        f"{DEFAULT_GATEWAY}/extend-field/forms/{quote(code, safe='')}",
        headers,
        body,
    )
    jb = parse_json(raw)
    if status >= 400 or not jb or jb.get("success") is False:
        print(f"FAIL save: {jb}", file=sys.stderr)
        return 1
    saved = unwrap(jb) or {}

    status2, raw2 = http_request(
        "GET",
        f"{DEFAULT_GATEWAY}/extend-field/forms/{quote(code, safe='')}",
        headers,
    )
    jb2 = parse_json(raw2)
    if status2 >= 400 or not jb2 or jb2.get("success") is False:
        print(f"FAIL read: {jb2}", file=sys.stderr)
        return 1
    loaded = unwrap(jb2) or {}
    label = (loaded.get("fields") or [{}])[0].get("fieldLabel")

    if loaded.get("formName") != FORM_NAME or label != FIELD_LABEL:
        print(
            f"FAIL mismatch formName={loaded.get('formName')!r} label={label!r}",
            file=sys.stderr,
        )
        return 1

    result = {
        "status": "passed",
        "formCode": code,
        "formName": loaded.get("formName"),
        "fieldLabel": label,
        "gateway": DEFAULT_GATEWAY,
    }
    out = ROOT / ".cursor" / "e2e-utf8-cn-result.json"
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print("PASS", out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
