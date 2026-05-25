#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""多租户隔离冒烟：扩展字段组按 tenantId 头隔离（经 Gateway）。"""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

from jbm_cluster_client import (  # noqa: E402
    DEFAULT_GATEWAY,
    http_request,
    login_password,
    parse_json,
    unwrap,
)

RESULT_FILE = ROOT / ".cursor" / "e2e-tenant-isolation-result.json"
SUFFIX = str(int(time.time()))[-6]
FORM_A = f"iso_tenant_a_{SUFFIX}"
FORM_B = f"iso_tenant_b_{SUFFIX}"


def gateway_with_tenant(
    method: str,
    path: str,
    token: str,
    tenant_id: str,
    body=None,
):
    url = DEFAULT_GATEWAY + (path if path.startswith("/") else "/" + path)
    headers = {"tenantId": tenant_id}
    if token:
        headers["Authorization"] = f"Bearer {token}" if not token.startswith("Bearer ") else token
    status, raw = http_request(method, url, headers, body)
    jb = parse_json(raw)
    if status >= 400 and not jb:
        raise RuntimeError(f"HTTP {status}: {raw[:400]}")
    return status, jb, raw


def save_form(token: str, tenant_id: str, form_code: str, form_name: str):
    path = f"/extend-field/forms/{quote(form_code, safe='')}"
    body = {
        "formName": form_name,
        "fields": [
            {
                "fieldName": "note",
                "fieldType": "string",
                "fieldLabel": "备注",
                "required": False,
            }
        ],
        "autoPublish": False,
    }
    status, jb, _ = gateway_with_tenant("POST", path, token, tenant_id, body)
    if status >= 400 or (jb and jb.get("success") is False):
        raise RuntimeError(jb.get("message") if jb else f"save failed HTTP {status}")
    return unwrap(jb)


def list_forms(token: str, tenant_id: str):
    path = "/extend-field/forms?pageForm.currPage=1&pageForm.pageSize=50"
    status, jb, _ = gateway_with_tenant("GET", path, token, tenant_id)
    if status >= 400 or (jb and jb.get("success") is False):
        raise RuntimeError(jb.get("message") if jb else f"list failed HTTP {status}")
    page = unwrap(jb) or {}
    return page.get("contents") or []


def get_form(token: str, tenant_id: str, form_code: str):
    path = f"/extend-field/forms/{quote(form_code, safe='')}"
    status, jb, _ = gateway_with_tenant("GET", path, token, tenant_id)
    return status, jb


def codes(forms):
    return {f.get("formCode") for f in forms if f.get("formCode")}


def main() -> int:
    result = {
        "status": "pending",
        "gateway": DEFAULT_GATEWAY,
        "formA": FORM_A,
        "formB": FORM_B,
        "steps": [],
    }

    def step(name: str, status: str, **extra):
        result["steps"].append({"name": name, "status": status, **extra})

    print("=== 多租户隔离 API 冒烟 ===\n")
    token = login_password("admin", "Admin@123")
    step("admin login", "passed")

    save_form(token, "0", FORM_A, f"租户A表单_{SUFFIX}")
    save_form(token, "1", FORM_B, f"租户B表单_{SUFFIX}")
    step("save forms under tenant 0 and 1", "passed")

    list0 = list_forms(token, "0")
    list1 = list_forms(token, "1")
    c0, c1 = codes(list0), codes(list1)

    if FORM_A not in c0:
        step("tenant 0 sees form A", "failed", codes=list(c0))
        raise SystemExit(1)
    if FORM_B in c0:
        step("tenant 0 must not see form B", "failed", codes=list(c0))
        raise SystemExit(1)
    step("tenant 0 sees A not B", "passed")

    if FORM_B not in c1:
        step("tenant 1 sees form B", "failed", codes=list(c1))
        raise SystemExit(1)
    if FORM_A in c1:
        step("tenant 1 must not see form A", "failed", codes=list(c1))
        raise SystemExit(1)
    step("tenant 1 sees B not A", "passed")

    status_cross, jb_cross = get_form(token, "0", FORM_B)
    cross_ok = status_cross == 404 or (
        jb_cross and jb_cross.get("success") is False
    )
    if not cross_ok:
        step("tenant 0 cannot read form B", "failed", httpStatus=status_cross)
        raise SystemExit(1)
    step("tenant 0 cannot read form B", "passed", httpStatus=status_cross)

    result["status"] = "passed"
    RESULT_FILE.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print("\n=== 多租户隔离全部通过 ===")
    print(f"结果: {RESULT_FILE}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except SystemExit:
        raise
    except Exception as e:
        out = {
            "status": "failed",
            "error": str(e),
            "formA": FORM_A,
            "formB": FORM_B,
        }
        RESULT_FILE.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"\n[FATAL] {e}", file=sys.stderr)
        raise SystemExit(1) from e
