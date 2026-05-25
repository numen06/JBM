#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""菜单管理 API 验证（Gateway OAuth + Center 菜单分页/scope）。"""
from __future__ import annotations

import json
import sys
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / ".cursor" / "e2e-menu-management-result.json"
SCRIPTS = ROOT / "scripts"
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

from jbm_cluster_client import DEFAULT_GATEWAY, gateway_api, login_password, unwrap  # noqa: E402


def main() -> int:
    token = login_password("admin", "Admin@123")
    result = {
        "gateway": DEFAULT_GATEWAY,
        "tokenObtained": bool(token),
        "steps": [],
    }

    def call(path: str) -> dict:
        status, jb, raw = gateway_api("GET", path, token)
        if status >= 400 or not jb:
            raise RuntimeError(f"HTTP {status}: {raw[:300]}")
        return jb

    page1 = call("/menu?pageForm.currPage=1&pageForm.pageSize=5")
    paging = unwrap(page1)
    result["api"] = {
        "page1": {"total": paging.get("total"), "count": len(paging.get("contents") or [])},
    }
    if not paging.get("total") or len(paging.get("contents") or []) > 5:
        raise SystemExit("分页 API 未返回预期 pageSize")

    search = call(f"/menu?pageForm.currPage=1&pageForm.pageSize=5&keyword={quote('用户')}")
    search_paging = unwrap(search)
    result["api"]["searchUser"] = {
        "total": search_paging.get("total"),
        "sample": [m.get("menuName") for m in (search_paging.get("contents") or [])[:3]],
    }

    platform = call("/menu?pageForm.currPage=1&pageForm.pageSize=20&scope=platform")
    platform_paging = unwrap(platform)
    platform_menus = platform_paging.get("contents") or []
    result["api"]["platformScope"] = {
        "total": platform_paging.get("total"),
        "allNullAppId": all(m.get("appId") is None for m in platform_menus),
    }
    if platform_menus and not result["api"]["platformScope"]["allNullAppId"]:
        raise SystemExit("scope=platform 返回了非平台菜单")

    current = call("/current/user/menus")
    menus = unwrap(current)
    result["api"]["currentMenus"] = {"count": len(menus) if isinstance(menus, list) else 0}

    page2 = call("/menu?pageForm.currPage=2&pageForm.pageSize=5")
    page2_paging = unwrap(page2)
    page1_ids = {m.get("menuId") for m in (paging.get("contents") or [])}
    page2_ids = {m.get("menuId") for m in (page2_paging.get("contents") or [])}
    result["api"]["page2Changed"] = bool(page2_ids - page1_ids)

    result["status"] = "passed"
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"OK -> {OUT}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        fail = {"status": "failed", "error": str(exc)}
        OUT.write_text(json.dumps(fail, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"FAIL -> {exc}", file=sys.stderr)
        raise SystemExit(1)
