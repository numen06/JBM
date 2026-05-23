#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JBM jaja7 集群日常运维脚本（减少反复手启 Java / 手测 Gateway）。

子命令:
  status          检测端口与 HTTP 探活
  wait            等待 Gateway + Auth 就绪
  login           测试超管 OAuth 登录
  setup-rbac      超管 API 创建 operator/demo、editor/viewer
  test-rbac       双用户双角色 RBAC 对比断言
  workflow        status → wait → login → setup-rbac → test-rbac

推荐: 日常用 VS Code「jaja7: Auth + Center + Gateway」调试启动 Java；
      本脚本负责检测、造数、断言，避免为测试用户反复改种子或 mvn 重启。

示例:
  python scripts/jbm_cluster_ops.py status
  python scripts/jbm_cluster_ops.py wait --timeout 60
  python scripts/jbm_cluster_ops.py workflow --password Admin@123
  python scripts/jbm_cluster_ops.py start center --background
  python scripts/jbm_cluster_ops.py stop center gateway auth
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"

# 将 scripts 目录加入 path
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

from jbm_cluster_client import (  # noqa: E402
    DEFAULT_AUTH,
    DEFAULT_CENTER,
    DEFAULT_GATEWAY,
    login_password,
    probe_url,
    unwrap,
    gateway_api,
    wait_services,
)
from jbm_rest_profile import REST_PROFILE, spring_boot_profile_arg  # noqa: E402

SERVICES = {
    "auth": {
        "port": 5555,
        "url": DEFAULT_AUTH,
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth",
        "mvn_cmd": 'mvn spring-boot:run "-Dspring-boot.run.profiles=jaja7" -DskipTests=true',
    },
    "center": {
        "port": 8888,
        "url": DEFAULT_CENTER,
        "health": "/role/all",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center",
        "mvn_cmd": 'mvn spring-boot:run "-Dspring-boot.run.profiles=jaja7" -DskipTests=true',
    },
    "gateway": {
        "port": 7777,
        "url": DEFAULT_GATEWAY,
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-gateway",
        "mvn_cmd": 'mvn spring-boot:run "-Dspring-boot.run.profiles=jaja7" -DskipTests=true',
    },
    "vue": {
        "port": 5173,
        "url": "http://127.0.0.1:5173",
        "health": "/",
        "module_dir": ROOT / "jbm-admin-vue",
        "mvn_cmd": "npm run dev",
    },
}


def _pids_on_port(port: int) -> list[int]:
    if sys.platform != "win32":
        proc = subprocess.run(
            ["sh", "-c", f"lsof -ti :{port}"],
            capture_output=True,
            text=True,
        )
        if proc.returncode != 0:
            return []
        return [int(x) for x in proc.stdout.split() if x.strip().isdigit()]
    out = subprocess.run(
        ["netstat", "-ano"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    pids = []
    pat = re.compile(rf":{port}\s+.*LISTENING\s+(\d+)", re.I)
    for line in out.stdout.splitlines():
        m = pat.search(line.replace(":", ":"))
        if m:
            pids.append(int(m.group(1)))
    return list(dict.fromkeys(pids))


def cmd_status(_args):
    print(f"profile: {REST_PROFILE}\n")
    all_ok = True
    for name, cfg in SERVICES.items():
        port = cfg["port"]
        pids = _pids_on_port(port)
        listen = "LISTEN" if pids else "DOWN"
        http_ok = False
        if pids and cfg.get("health"):
            http_ok = probe_url(cfg["url"].rstrip("/") + cfg["health"])
        mark = "OK" if pids and (name == "vue" or http_ok or name == "center") else (
            "OK" if pids and not cfg.get("health") else "WARN" if pids else "FAIL"
        )
        if mark == "FAIL":
            all_ok = False
        print(
            f"  [{mark:4}] {name:8} port={port} pid={pids or '-'} "
            f"http={'yes' if http_ok else 'no' if pids else '-'}"
        )
    print(f"\nGateway: {DEFAULT_GATEWAY}")
    print("提示: Java 服务建议用 VS Code 复合启动「jaja7: Auth + Center + Gateway」")
    return 0 if all_ok else 1


def cmd_wait(args):
    print(f"等待 Gateway + Auth（最多 {args.timeout}s）…")
    r = wait_services(timeout=args.timeout, interval=args.interval)
    print(f"  gateway={r['gateway']} auth={r['auth']}")
    if not r["gateway"]:
        print("  Gateway 未就绪，请启动 jbm-cluster-platform-gateway", file=sys.stderr)
    if not r["auth"]:
        print("  Auth 未就绪，请启动 jbm-cluster-platform-auth", file=sys.stderr)
    return 0 if r["gateway"] and r["auth"] else 1


def cmd_login(args):
    try:
        tok = login_password(args.user, args.password)
        print(f"OK login user={args.user!r} token_len={len(tok)}")
        if args.verbose:
            _, jb, _ = gateway_api("GET", "/current/user", tok)
            u = unwrap(jb)
            print(f"  userId={u.get('userId')} userName={u.get('userName')}")
            auths = [a.get("authority") for a in (u.get("authorities") or []) if a.get("authority")]
            print(f"  authorities({len(auths)}): {', '.join(auths[:8])}…")
        return 0
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        return 1


def cmd_setup_rbac(args):
    import setup_test_users_via_admin as setup

    sys.argv = ["setup", "--password", args.password]
    return setup.main()


def cmd_test_rbac(args):
    import run_rbac_compare_test as compare

    sys.argv = ["compare"]
    return compare.main()


def cmd_workflow(args):
    steps = []
    code = 0
    if not args.skip_status:
        code = cmd_status(argparse.Namespace())
        steps.append(("status", code))
    if code == 0 or args.force:
        w = argparse.Namespace(timeout=args.wait_timeout, interval=2.0)
        code = cmd_wait(w)
        steps.append(("wait", code))
    if code == 0 or args.force:
        l = argparse.Namespace(user=args.user, password=args.password, verbose=True)
        code = cmd_login(l)
        steps.append(("login", code))
    if code == 0 or args.force:
        s = argparse.Namespace(password=args.password)
        code = cmd_setup_rbac(s)
        steps.append(("setup-rbac", code))
    if code == 0 or args.force:
        code = cmd_test_rbac(argparse.Namespace())
        steps.append(("test-rbac", code))
    print("\n=== workflow 汇总 ===")
    for name, c in steps:
        print(f"  {name}: {'PASS' if c == 0 else 'FAIL'}")
    return 0 if all(c == 0 for _, c in steps) else 1


def cmd_start(args):
    names = args.services or ["center"]
    procs = []
    for name in names:
        cfg = SERVICES.get(name)
        if not cfg:
            print(f"未知服务: {name}", file=sys.stderr)
            return 1
        cwd = cfg["module_dir"]
        if not cwd.is_dir():
            print(f"目录不存在: {cwd}", file=sys.stderr)
            return 1
        log = ROOT / "logs" / f"ops-start-{name}.log"
        log.parent.mkdir(parents=True, exist_ok=True)
        print(f"[start] {name} in {cwd}")
        print(f"  log: {log}")
        with open(log, "a", encoding="utf-8") as lf:
            lf.write(f"\n--- start {time.strftime('%Y-%m-%d %H:%M:%S')} ---\n")
            p = subprocess.Popen(
                cfg["mvn_cmd"],
                cwd=str(cwd),
                shell=True,
                stdout=lf,
                stderr=subprocess.STDOUT,
            )
        procs.append((name, p.pid))
        if not args.background:
            print("  使用 --background 避免阻塞；推荐 IDE 直接 Debug 启动")
    if args.background:
        print("后台 PID:", ", ".join(f"{n}={pid}" for n, pid in procs))
        print("下一步: python scripts/jbm_cluster_ops.py wait")
    return 0


def cmd_stop(args):
    names = args.services or list(SERVICES.keys())
    for name in names:
        if name not in SERVICES:
            continue
        port = SERVICES[name]["port"]
        pids = _pids_on_port(port)
        for pid in pids:
            print(f"[stop] {name} kill pid={pid} port={port}")
            if sys.platform == "win32":
                subprocess.run(["taskkill", "/PID", str(pid), "/F"], check=False)
            else:
                subprocess.run(["kill", str(pid)], check=False)
    return 0


def main():
    ap = argparse.ArgumentParser(description="JBM jaja7 集群运维与 RBAC 测试")
    sub = ap.add_subparsers(dest="cmd", required=True)

    parent = argparse.ArgumentParser(add_help=False)
    parent.add_argument("--password", default="Admin@123")
    parent.add_argument("--user", default="admin")

    sub.add_parser("status", help="端口与 HTTP 状态", parents=[parent])

    p_wait = sub.add_parser("wait", help="等待 Gateway+Auth 就绪", parents=[parent])
    p_wait.add_argument("--timeout", type=int, default=90)
    p_wait.add_argument("--interval", type=float, default=2.0)

    p_login = sub.add_parser("login", help="OAuth 登录探测", parents=[parent])
    p_login.add_argument("-v", "--verbose", action="store_true")

    sub.add_parser("setup-rbac", help="超管创建测试角色与用户", parents=[parent])
    sub.add_parser("test-rbac", help="RBAC 对比断言并写报告", parents=[parent])

    p_wf = sub.add_parser("workflow", help="完整流程：检测→等待→登录→造数→断言", parents=[parent])
    p_wf.add_argument("--wait-timeout", type=int, default=90)
    p_wf.add_argument("--skip-status", action="store_true")
    p_wf.add_argument("--force", action="store_true", help="某步失败后仍继续")

    p_start = sub.add_parser("start", help="后台 mvn/npm 启动（可选，推荐 IDE）", parents=[parent])
    p_start.add_argument("services", nargs="*", choices=list(SERVICES.keys()))
    p_start.add_argument("--background", action="store_true", default=True)

    p_stop = sub.add_parser("stop", help="按端口结束进程", parents=[parent])
    p_stop.add_argument("services", nargs="*", choices=list(SERVICES.keys()))

    args = ap.parse_args()
    handlers = {
        "status": cmd_status,
        "wait": cmd_wait,
        "login": cmd_login,
        "setup-rbac": cmd_setup_rbac,
        "test-rbac": cmd_test_rbac,
        "workflow": cmd_workflow,
        "start": cmd_start,
        "stop": cmd_stop,
    }
    return handlers[args.cmd](args)


if __name__ == "__main__":
    sys.exit(main())
