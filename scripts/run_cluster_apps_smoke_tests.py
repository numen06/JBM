#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""非核心集群应用 jaja7 REST 冒烟（经 Gateway + 直连 health）。"""
from __future__ import annotations

import argparse
import json
import sys
import time
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "scripts/cluster_apps_smoke_modules.json"
OUT_JSON = ROOT / ".cursor/cluster-apps-smoke-result.json"
OUT_MD = ROOT / "docs/testing/cluster-apps-jaja7/summary-test-report.md"

if str(ROOT / "scripts") not in sys.path:
    sys.path.insert(0, str(ROOT / "scripts"))

from jbm_cluster_client import DEFAULT_AUTH, DEFAULT_GATEWAY, http_request, login_password, parse_json, probe_url
from jbm_rest_profile import apply_rest_profile


def load_config():
    return json.loads(CONFIG.read_text(encoding="utf-8"))


def run_case(case: dict, token: str, base_url: str, service_down: set[str]) -> dict:
    svc = case.get("service", "")
    if svc in service_down and case.get("allowSkipIfDown"):
        return {"id": case["id"], "status": "skipped", "reason": f"service {svc} not reachable"}

    via = case.get("via", "gateway")
    path = case["path"]
    url = (base_url if via == "gateway" else f"http://127.0.0.1:{case.get('port', 0)}") + path
    headers = {"tenantId": "0"}
    if case.get("headers"):
        headers.update(case["headers"])
    if case.get("expectNoAuth"):
        pass
    elif token:
        headers["Authorization"] = f"Bearer {token}"

    body = case.get("body")
    status, raw = http_request(case.get("method", "GET"), url, headers, body)
    jb = parse_json(raw)
    ok = status in case.get("expectStatus", [200])
    if not case.get("expectStatus"):
        ok = status == 200 and (jb is None or jb.get("success") is not False)
    if case.get("expectNoAuth"):
        ok = status in case.get("expectStatus", [401, 403])

    result = {
        "id": case["id"],
        "status": "passed" if ok else "failed",
        "httpStatus": status,
        "url": url,
    }
    if not ok:
        result["reason"] = (jb or {}).get("message") or raw[:300]
    return result


def probe_services(cfg: dict) -> dict[str, bool]:
    alive: dict[str, bool] = {}
    for name, sc in cfg.get("services", {}).items():
        url = f"http://127.0.0.1:{sc['port']}{sc.get('health', '/actuator/health')}"
        alive[name] = probe_url(url)
    return alive


def main() -> int:
    ap = argparse.ArgumentParser(description="Cluster non-core apps smoke tests")
    ap.add_argument("--profile", default="jaja7")
    ap.add_argument("--base-url", default=DEFAULT_GATEWAY)
    ap.add_argument("--auth-url", default=DEFAULT_AUTH)
    ap.add_argument("--user", default="admin")
    ap.add_argument("--password", default="Admin@123")
    ap.add_argument(
        "--services",
        default="",
        help="逗号分隔服务名，仅跑这些服务的用例（如 doc,push）；weixin 默认排除",
    )
    args = ap.parse_args()

    cfg = load_config()
    apply_rest_profile(cfg, args.profile)
    base_url = args.base_url.rstrip("/")

    service_filter = {s.strip() for s in args.services.split(",") if s.strip()} if args.services else None
    if service_filter:
        cfg["services"] = {k: v for k, v in cfg.get("services", {}).items() if k in service_filter}

    alive = probe_services(cfg)
    service_down = {n for n, ok in alive.items() if not ok}

    token = ""
    try:
        token = login_password(args.user, args.password, gateway=base_url)
    except Exception as e:
        print(f"WARN login failed: {e}", file=sys.stderr)

    results = []
    for case in cfg.get("cases", []):
        if service_filter and case.get("service") not in service_filter:
            continue
        port = cfg.get("services", {}).get(case.get("service", ""), {}).get("port")
        case = {**case, "port": port}
        r = run_case(case, token, base_url, service_down)
        results.append(r)
        mark = r["status"].upper()
        print(f"[{mark}] {r['id']}" + (f" — {r.get('reason', '')}" if r["status"] == "failed" else ""))

    summary = {
        "profile": args.profile,
        "timestamp": datetime.now().isoformat(timespec="seconds"),
        "baseUrl": base_url,
        "servicesFilter": sorted(service_filter) if service_filter else None,
        "weixin": "excluded",
        "servicesAlive": alive,
        "passed": sum(1 for r in results if r["status"] == "passed"),
        "failed": sum(1 for r in results if r["status"] == "failed"),
        "skipped": sum(1 for r in results if r["status"] == "skipped"),
        "results": results,
    }

    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

    OUT_MD.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "# 集群非核心应用 jaja7 冒烟报告",
        "",
        f"时间：{summary['timestamp']}",
        f"Profile：{args.profile}",
        "",
        "## 服务探活",
        "",
    ]
    for name, ok in alive.items():
        lines.append(f"- {name}: {'UP' if ok else 'DOWN'}")
    lines.extend(["", "## 用例结果", ""])
    for r in results:
        lines.append(f"- **{r['id']}**: {r['status']}" + (f" — {r.get('reason', '')}" if r.get("reason") else ""))
    lines.extend([
        "",
        f"汇总：passed={summary['passed']} failed={summary['failed']} skipped={summary['skipped']}",
        "",
        f"机器可读结果：`.cursor/cluster-apps-smoke-result.json`",
    ])
    OUT_MD.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"\n结果已写入 {OUT_JSON} 与 {OUT_MD}")
    return 0 if summary["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
