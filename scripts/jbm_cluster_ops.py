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
  test-apikey     wait → API Key 全流程 REST（TC1–TC12，注册/审批，不直插库）
  cleanup         清理集群端口占用 + 残留 mvn/java 进程（推荐测试前执行）
  stop            按端口结束进程（含进程树）

推荐: 日常用 VS Code「jaja7: Auth + Center + Gateway」调试启动 Java；
      本脚本负责检测、造数、断言，避免为测试用户反复改种子或 mvn 重启。

示例:
  python scripts/jbm_cluster_ops.py cleanup
  python scripts/jbm_cluster_ops.py status
  python scripts/jbm_cluster_ops.py wait --timeout 60
  python scripts/jbm_cluster_ops.py workflow --password Admin@123
  python scripts/jbm_cluster_ops.py test-apikey
  python scripts/jbm_cluster_ops.py start auth center gateway --background --clean
  python scripts/jbm_cluster_ops.py start center --prepare install --background --clean
  python scripts/jbm_cluster_ops.py restart --prepare install
  python scripts/jbm_cluster_ops.py stop auth center gateway
"""
from __future__ import annotations

import argparse
import json
import re
import shlex
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"
LOGS = ROOT / "logs"
PID_FILE = LOGS / "ops-cluster.pids.json"

CLUSTER_JAVA_MARKERS = (
    "jbm-cluster-platform-auth",
    "jbm-cluster-platform-center",
    "jbm-cluster-platform-gateway",
    "jbm-cluster-platform-doc",
    "jbm-cluster-platform-push",
    "jbm-cluster-platform-logs",
    "jbm-cluster-platform-job",
    "jbm-cluster-platform-weixin",
    "jbm-cluster-platform-bigscreen",
    "JbmAuthApplication",
    "JbmCenterApplication",
    "JbmGatewayApplication",
    "JbmDocApplication",
    "JbmPushApplication",
    "JbmLogsApplication",
    "JbmJobApplication",
    "JbmWxApplication",
    "JbmBigscreenApplication",
    "spring-boot:run",
    "spring-boot.run.profiles=jaja7",
)
MVN_MARKERS = ("spring-boot:run", "jbm-cluster-platform-")
PREPARE_CHOICES = ("compile", "install", "none")

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
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth",
        "main_class": "com.jbm.cluster.auth.JbmAuthApplication",
    },
    "center": {
        "port": 8888,
        "url": DEFAULT_CENTER,
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center",
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center",
        "main_class": "com.jbm.cluster.center.JbmCenterApplication",
    },
    "gateway": {
        "port": 7777,
        "url": DEFAULT_GATEWAY,
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-gateway",
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-gateway",
        "main_class": "com.jbm.cluster.platform.gateway.JbmGatewayApplication",
    },
    "doc": {
        "port": 9999,
        "url": "http://127.0.0.1:9999",
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-doc",
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-doc",
        "main_class": "com.jbm.cluster.doc.JbmDocApplication",
    },
    "push": {
        "port": 3313,
        "url": "http://127.0.0.1:3313",
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-push",
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-push",
        "main_class": "com.jbm.cluster.push.JbmPushApplication",
    },
    "logs": {
        "port": 3312,
        "url": "http://127.0.0.1:3312",
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-logs",
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-logs",
        "main_class": "com.jbm.cluster.logs.JbmLogsApplication",
    },
    "job": {
        "port": 4444,
        "url": "http://127.0.0.1:4444",
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-job",
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-job",
        "main_class": "com.jbm.cluster.job.JbmJobApplication",
    },
    "weixin": {
        "port": 3319,
        "url": "http://127.0.0.1:3319",
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-weixin",
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-weixin",
        "main_class": "com.jbm.cluster.weixin.miniapp.JbmWxApplication",
    },
    "bigscreen": {
        "port": 3314,
        "url": "http://127.0.0.1:3314",
        "health": "/actuator/health",
        "module_dir": ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-bigscreen",
        "module_path": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-bigscreen",
        "main_class": "com.jbm.cluster.bigscreen.JbmBigscreenApplication",
    },
    "vue": {
        "port": 5173,
        "url": "http://127.0.0.1:5173",
        "health": "/",
        "module_dir": ROOT / "jbm-admin-vue",
        "mvn_cmd": "npm run dev",
    },
}


def _shell_join(parts: list[str]) -> str:
    if sys.platform == "win32":
        return subprocess.list2cmdline(parts)
    return shlex.join(parts)


def _maven_executable(preference: str = "auto") -> str:
    if preference in {"mvn", "mvnd"}:
        return preference
    return "mvnd" if shutil.which("mvnd") else "mvn"


def _resolve_prepare_mode(args) -> str:
    """Resolve Maven prepare mode. --install is kept as a backwards-compatible alias."""
    if getattr(args, "install", False):
        return "install"
    prepare = getattr(args, "prepare", "compile")
    return prepare if prepare in PREPARE_CHOICES else "compile"


def _service_jar_path(cfg: dict) -> Path:
    """Fat jar produced by spring-boot repackage (finalName = artifactId)."""
    artifact = cfg["module_dir"].name
    return cfg["module_dir"] / "target" / f"{artifact}.jar"


def _java_jar_run_command(cfg: dict) -> str:
    """Run packaged Spring Boot jar (avoids spring-boot:run / IDE stale class issues on Windows)."""
    java = shutil.which("java") or "java"
    jar = _service_jar_path(cfg)
    return _shell_join(
        [
            java,
            "-Dspring.profiles.active=jaja7",
            "-Dprofile.name=jaja7",
            "-jar",
            str(jar),
        ]
    )


def _maven_service_command(
    cfg: dict, *, prepare: str = "compile", maven: str = "auto"
) -> tuple[str, str]:
    """Return (shell command, working directory). Prepare from repo root; run via java -jar."""
    mvn = _maven_executable(maven)
    module_path = cfg["module_path"]
    root_common = [mvn, "-pl", module_path, "-DskipTests=true"]
    prepare_cmd = None
    if prepare == "install":
        prepare_cmd = root_common + ["-am", "install"]
    elif prepare == "compile":
        prepare_cmd = root_common + ["-am", "package"]
    run_cmd = _java_jar_run_command(cfg)
    if prepare_cmd:
        prepare_shell = _shell_join(prepare_cmd)
        cmd = f"{prepare_shell} && {run_cmd}"
    else:
        jar = _service_jar_path(cfg)
        if not jar.is_file():
            prepare_shell = _shell_join(root_common + ["-am", "package"])
            cmd = f"{prepare_shell} && {run_cmd}"
        else:
            cmd = run_cmd
    return cmd, str(ROOT)


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


def _kill_pid_tree(pid: int) -> bool:
    if pid <= 0:
        return False
    if sys.platform == "win32":
        proc = subprocess.run(
            ["taskkill", "/PID", str(pid), "/T", "/F"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        return proc.returncode == 0
    subprocess.run(["kill", "-TERM", str(pid)], check=False)
    return True


def _cluster_ports(services: list[str] | None = None) -> list[int]:
    names = services or list(SERVICES.keys())
    return [SERVICES[n]["port"] for n in names if n in SERVICES]


def _cim_processes(*names: str) -> list[dict]:
    """Windows: 按进程名拉取 CommandLine（json 数组）。"""
    if sys.platform == "win32":
        filter_expr = " or ".join(f"Name='{n}'" for n in names)
        ps = (
            f"Get-CimInstance Win32_Process -Filter \"{filter_expr}\" | "
            "Select-Object ProcessId, Name, CommandLine | ConvertTo-Json -Compress"
        )
        proc = subprocess.run(
            ["powershell", "-NoProfile", "-Command", ps],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        if proc.returncode != 0 or not proc.stdout.strip():
            return []
        try:
            data = json.loads(proc.stdout)
        except json.JSONDecodeError:
            return []
        return data if isinstance(data, list) else [data]
    proc = subprocess.run(
        ["sh", "-c", "ps aux"],
        capture_output=True,
        text=True,
    )
    rows = []
    for line in proc.stdout.splitlines()[1:]:
        parts = line.split(None, 10)
        if len(parts) < 11:
            continue
        pid_s, cmd = parts[1], parts[10]
        if pid_s.isdigit():
            rows.append({"ProcessId": int(pid_s), "Name": parts[10][:20], "CommandLine": cmd})
    return rows


def _find_cluster_java_pids() -> list[tuple[int, str]]:
    """查找本仓库 spring-boot / JBM 应用 Java 进程。"""
    hits: list[tuple[int, str]] = []
    for row in _cim_processes("java.exe", "javaw.exe"):
        pid = row.get("ProcessId")
        cmd = row.get("CommandLine") or ""
        if pid and any(m in cmd for m in CLUSTER_JAVA_MARKERS):
            hits.append((int(pid), cmd[:160]))
    return hits


def _find_cluster_mvn_pids() -> list[tuple[int, str]]:
    """查找残留的 mvn/cmd 包装进程（spring-boot:run 父 shell）。"""
    hits: list[tuple[int, str]] = []
    for row in _cim_processes("cmd.exe", "powershell.exe", "pwsh.exe"):
        pid = row.get("ProcessId")
        cmd = row.get("CommandLine") or ""
        if pid and any(m in cmd for m in MVN_MARKERS):
            hits.append((int(pid), cmd[:160]))
    return hits


def _stop_from_pid_file(seen: set[int], names: list[str] | None = None) -> list[str]:
    """按 ops 记录的 shell PID 结束后台 start 进程。"""
    lines: list[str] = []
    if not PID_FILE.is_file():
        return lines
    try:
        record = json.loads(PID_FILE.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return lines
    for name, info in record.items():
        if names is not None and name not in names:
            continue
        pid = int(info.get("shell_pid") or 0)
        if pid <= 0 or pid in seen:
            continue
        seen.add(pid)
        ok = _kill_pid_tree(pid)
        lines.append(f"[stop] pidfile {name} shell_pid={pid} {'OK' if ok else 'FAIL'}")
    return lines


def _save_pid_record(name: str, shell_pid: int, port: int) -> None:
    LOGS.mkdir(parents=True, exist_ok=True)
    record: dict = {}
    if PID_FILE.is_file():
        try:
            record = json.loads(PID_FILE.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            record = {}
    record[name] = {"shell_pid": shell_pid, "port": port, "ts": time.strftime("%Y-%m-%d %H:%M:%S")}
    PID_FILE.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")


def _clear_pid_record(names: list[str] | None = None) -> None:
    if not PID_FILE.is_file():
        return
    if names is None:
        PID_FILE.unlink(missing_ok=True)
        return
    try:
        record = json.loads(PID_FILE.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        PID_FILE.unlink(missing_ok=True)
        return
    for n in names:
        record.pop(n, None)
    if record:
        PID_FILE.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")
    else:
        PID_FILE.unlink(missing_ok=True)


def _kill_listeners_on_ports(ports: list[int], seen: set[int]) -> list[str]:
    """按端口结束监听进程（含 IDE 直接启动的 Java，不限 commandLine 特征）。"""
    lines: list[str] = []
    for port in ports:
        for pid in _pids_on_port(port):
            if pid in seen:
                continue
            seen.add(pid)
            ok = _kill_pid_tree(pid)
            lines.append(f"[stop] port={port} pid={pid} {'OK' if ok else 'FAIL'}")
    return lines


def _stop_services(names: list[str], *, kill_java: bool = False) -> list[str]:
    """停止指定服务，返回已清理项摘要。"""
    lines: list[str] = []
    seen_pids: set[int] = set()
    lines.extend(_stop_from_pid_file(seen_pids, names))
    ports = _cluster_ports(names)
    lines.extend(_kill_listeners_on_ports(ports, seen_pids))
    for name in names:
        if name not in SERVICES:
            continue
        port = SERVICES[name]["port"]
        pids = _pids_on_port(port)
        if not pids:
            lines.append(f"[stop] {name} port={port} (无监听)")
            continue
        for pid in pids:
            if pid in seen_pids:
                continue
            seen_pids.add(pid)
            ok = _kill_pid_tree(pid)
            lines.append(f"[stop] {name} port={port} pid={pid} {'OK' if ok else 'FAIL'}")
    if kill_java:
        for pid, cmd in _find_cluster_java_pids():
            if pid in seen_pids:
                continue
            seen_pids.add(pid)
            ok = _kill_pid_tree(pid)
            lines.append(f"[stop] java pid={pid} {'OK' if ok else 'FAIL'}  {cmd[:80]}")
        for pid, cmd in _find_cluster_mvn_pids():
            if pid in seen_pids:
                continue
            seen_pids.add(pid)
            ok = _kill_pid_tree(pid)
            lines.append(f"[stop] mvn-shell pid={pid} {'OK' if ok else 'FAIL'}  {cmd[:80]}")
    _clear_pid_record(names if names != list(SERVICES.keys()) else None)
    return lines


def cmd_cleanup(args):
    """清理集群端口 + 残留 spring-boot:run Java（测试前推荐）。"""
    names = args.services or ["auth", "center", "gateway"]
    print("清理 JBM 集群进程…")
    lines = _stop_services(names, kill_java=True)
    if not lines:
        print("  未发现需清理的进程")
    else:
        for ln in lines:
            print(f"  {ln}")
    # 二次扫描：taskkill 后端口可能短暂仍占用
    time.sleep(2.0)
    extra = _stop_services(names, kill_java=True)
    for ln in extra:
        if "无监听" not in ln:
            print(f"  {ln}")
    remain_ports = []
    for name in names:
        port = SERVICES[name]["port"]
        if _pids_on_port(port):
            remain_ports.append(f"{name}:{port}")
    remain_java = _find_cluster_java_pids()
    if remain_ports:
        print(f"WARN 端口仍占用: {', '.join(remain_ports)}", file=sys.stderr)
        return 1
    if remain_java and not args.force:
        print(f"WARN 仍有 {len(remain_java)} 个集群 Java 进程", file=sys.stderr)
        for pid, cmd in remain_java[:5]:
            print(f"  pid={pid} {cmd[:100]}", file=sys.stderr)
        return 1
    print("cleanup 完成")
    return 0


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


def _wait_service_list(names: list[str], timeout: int, interval: float) -> dict[str, bool]:
    """按服务名列表探活 actuator/health。"""
    import time

    deadline = time.time() + timeout
    targets = {n: SERVICES[n] for n in names if n in SERVICES}
    result = {n: False for n in targets}
    headers = {"tenantId": "0"}
    while time.time() < deadline:
        for name, cfg in targets.items():
            if result[name]:
                continue
            health = cfg.get("health")
            if not health:
                result[name] = bool(_pids_on_port(cfg["port"]))
                continue
            url = cfg["url"].rstrip("/") + health
            if probe_url(url, headers):
                result[name] = True
        if all(result.values()):
            return result
        time.sleep(interval)
    return result


def cmd_wait(args):
    names = getattr(args, "services", None) or []
    if names:
        print(f"等待 {', '.join(names)}（最多 {args.timeout}s）…")
        r = _wait_service_list(names, args.timeout, args.interval)
        for name in names:
            mark = "OK" if r.get(name) else "FAIL"
            print(f"  [{mark}] {name}")
        return 0 if all(r.get(n) for n in names) else 1

    need_center = getattr(args, "center", False)
    label = "Gateway + Auth + Center" if need_center else "Gateway + Auth"
    print(f"等待 {label}（最多 {args.timeout}s）…")
    r = wait_services(
        timeout=args.timeout,
        interval=args.interval,
        center=need_center,
    )
    parts = [f"gateway={r['gateway']}", f"auth={r['auth']}"]
    if need_center:
        parts.append(f"center={r['center']}")
    print(f"  {' '.join(parts)}")
    if not r["gateway"]:
        print("  Gateway 未就绪，请启动 jbm-cluster-platform-gateway", file=sys.stderr)
    if not r["auth"]:
        print("  Auth 未就绪，请启动 jbm-cluster-platform-auth", file=sys.stderr)
    if need_center and not r["center"]:
        print("  Center 未就绪，请启动 jbm-cluster-platform-center", file=sys.stderr)
    ok = r["gateway"] and r["auth"]
    if need_center:
        ok = ok and r["center"]
    return 0 if ok else 1


def cmd_login(args):
    try:
        if getattr(args, "reset_seed", False):
            from jbm_cluster_client import reset_jaja7_seed

            info = reset_jaja7_seed(via_gateway=False)
            print(f"seed reset: clientId={info.get('clientId')} ok={info.get('jbmAppCredentialsReset')}")
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


def cmd_test_apikey(args):
    """等待集群就绪后执行 API Key 全流程 REST 测试（不启动 Java）。"""
    if getattr(args, "restart_first", False):
        rc = cmd_restart(argparse.Namespace(timeout=max(args.timeout, 120)))
        if rc != 0 and not args.force:
            print("restart 未就绪，加 --force 跳过或手动排查", file=sys.stderr)
            return 1
    elif getattr(args, "cleanup_first", False):
        c = cmd_cleanup(argparse.Namespace(services=["auth", "center", "gateway"], force=False))
        if c != 0:
            print("cleanup 未完全成功，使用 --force 继续或手动处理残留进程", file=sys.stderr)
            if not args.force:
                return 1
    w = argparse.Namespace(timeout=args.timeout, interval=2.0, center=getattr(args, "wait_center", True))
    code = cmd_wait(w)
    if code != 0 and not args.force:
        print("集群未就绪；请用 VS Code「jaja7: Auth + Center + Gateway」或 ops restart", file=sys.stderr)
        return 1
    import run_api_key_flow_tests as flow

    sys.argv = ["run_api_key_flow_tests.py"]
    if args.suffix:
        sys.argv += ["--suffix", args.suffix]
    rc = flow.main()
    if rc == 0 and getattr(args, "cleanup_after", False):
        print("\n测试完成，清理集群进程…")
        cmd_cleanup(argparse.Namespace(services=["auth", "center", "gateway"], force=True))
    return rc


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
    if getattr(args, "clean", False):
        _stop_services(names, kill_java=False)
        time.sleep(1.0)
    procs = []
    prepare_mode = _resolve_prepare_mode(args)
    for i, name in enumerate(names):
        cfg = SERVICES.get(name)
        if not cfg:
            print(f"未知服务: {name}", file=sys.stderr)
            return 1
        if i > 0:
            time.sleep(15.0)
        cwd = cfg["module_dir"]
        if not cwd.is_dir():
            print(f"目录不存在: {cwd}", file=sys.stderr)
            return 1
        log = ROOT / "logs" / f"ops-start-{name}.log"
        log.parent.mkdir(parents=True, exist_ok=True)
        print(f"[start] {name} in {cwd}")
        print(f"  log: {log}")
        if "main_class" in cfg:
            cmd, work_dir = _maven_service_command(
                cfg,
                prepare=prepare_mode,
                maven=getattr(args, "maven", "auto"),
            )
            print(f"  maven: {_maven_executable(getattr(args, 'maven', 'auto'))}")
            print(f"  prepare: {prepare_mode}")
            print(f"  cwd: {work_dir}")
        else:
            cmd = cfg["mvn_cmd"]
            work_dir = str(cwd)
        with open(log, "a", encoding="utf-8") as lf:
            lf.write(f"\n--- start {time.strftime('%Y-%m-%d %H:%M:%S')} ---\n")
            lf.write(f"$ {cmd}\n")
            p = subprocess.Popen(
                cmd,
                cwd=str(work_dir if "main_class" in cfg else cwd),
                shell=True,
                stdout=lf,
                stderr=subprocess.STDOUT,
            )
        procs.append((name, p.pid, cfg["port"]))
        _save_pid_record(name, p.pid, cfg["port"])
        if not args.background:
            print("  使用 --background 避免阻塞；推荐 IDE 直接 Debug 启动")
    if args.background:
        print("后台 PID:", ", ".join(f"{n}=shell:{pid}/port:{port}" for n, pid, port in procs))
        print("下一步: python scripts/jbm_cluster_ops.py wait")
    return 0


def cmd_stop(args):
    names = args.services or list(SERVICES.keys())
    kill_java = getattr(args, "kill_java", False)
    lines = _stop_services(names, kill_java=kill_java)
    for ln in lines:
        print(ln)
    if not lines:
        print("无进程需停止")
    return 0


def cmd_restart(args):
    names = ["auth", "center", "gateway"]
    cmd_cleanup(argparse.Namespace(services=names, force=True))
    time.sleep(1.5)
    prepare_mode = _resolve_prepare_mode(args)
    cmd_start(
        argparse.Namespace(
            services=names,
            background=True,
            clean=False,
            install=False,
            prepare=prepare_mode,
            maven=getattr(args, "maven", "auto"),
        )
    )
    w = argparse.Namespace(timeout=args.timeout, interval=2.0, center=True)
    return cmd_wait(w)


def main():
    ap = argparse.ArgumentParser(description="JBM jaja7 集群运维与 RBAC 测试")
    sub = ap.add_subparsers(dest="cmd", required=True)

    parent = argparse.ArgumentParser(add_help=False)
    parent.add_argument("--password", default="Admin@123")
    parent.add_argument("--user", default="admin")

    sub.add_parser("status", help="端口与 HTTP 状态", parents=[parent])

    p_wait = sub.add_parser("wait", help="等待 Gateway+Auth(+Center) 或指定服务就绪", parents=[parent])
    p_wait.add_argument("services", nargs="*", choices=list(SERVICES.keys()),
                        help="指定服务列表；省略则默认 Gateway+Auth")
    p_wait.add_argument("--timeout", type=int, default=90)
    p_wait.add_argument("--interval", type=float, default=2.0)
    p_wait.add_argument("--center", action="store_true", help="同时等待 Center 健康")

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
    p_start.add_argument("--clean", action="store_true", help="启动前清理同端口占用")
    p_start.add_argument(
        "--prepare",
        choices=PREPARE_CHOICES,
        default="compile",
        help="启动前准备动作：compile=编译依赖模块；install=安装跨模块公共包；none=直接运行",
    )
    p_start.add_argument("--install", action="store_true", help="兼容快捷参数，等同于 --prepare install")
    p_start.add_argument("--maven", choices=["auto", "mvnd", "mvn"], default="auto",
                         help="Maven 命令选择；auto 会优先使用 mvnd，找不到再回退 mvn")

    p_stop = sub.add_parser("stop", help="按端口结束进程（含进程树）", parents=[parent])
    p_stop.add_argument("services", nargs="*", choices=list(SERVICES.keys()))
    p_stop.add_argument("--kill-java", action="store_true", help="额外结束匹配的 spring-boot Java")

    p_cleanup = sub.add_parser("cleanup", help="清理端口 + 残留集群 Java（测试前推荐）", parents=[parent])
    p_cleanup.add_argument("services", nargs="*", choices=list(SERVICES.keys()))
    p_cleanup.add_argument("--force", action="store_true", help="有残留也返回 0")

    p_apikey = sub.add_parser("test-apikey", help="API Key 全流程 REST（TC1–TC12）", parents=[parent])
    p_apikey.add_argument("--timeout", type=int, default=90)
    p_apikey.add_argument("--suffix", default="")
    p_apikey.add_argument("--force", action="store_true")
    p_apikey.add_argument("--wait-center", action="store_true", default=True,
                          help="等待 Center 就绪（默认开启，TC10 验签依赖 Center）")
    p_apikey.add_argument("--no-wait-center", action="store_false", dest="wait_center")
    p_apikey.add_argument("--cleanup-first", action="store_true", help="测试前先 cleanup（仅停服，不自动重启）")
    p_apikey.add_argument("--cleanup-after", action="store_true", help="测试通过后 cleanup 释放进程")
    p_apikey.add_argument(
        "--restart-first",
        action="store_true",
        help="测试前 cleanup → start auth+center+gateway → wait（推荐，避免残留进程）",
    )
    p_re = sub.add_parser("restart", help="cleanup → start auth+center+gateway → wait", parents=[parent])
    p_re.add_argument("--timeout", type=int, default=120)
    p_re.add_argument(
        "--prepare",
        choices=PREPARE_CHOICES,
        default="compile",
        help="重启前准备动作：compile=编译依赖模块；install=安装跨模块公共包；none=直接运行",
    )
    p_re.add_argument("--install", action="store_true", help="兼容快捷参数，等同于 --prepare install")
    p_re.add_argument("--maven", choices=["auto", "mvnd", "mvn"], default="auto",
                      help="Maven 命令选择；auto 会优先使用 mvnd，找不到再回退 mvn")

    args = ap.parse_args()
    handlers = {
        "status": cmd_status,
        "wait": cmd_wait,
        "login": cmd_login,
        "setup-rbac": cmd_setup_rbac,
        "test-rbac": cmd_test_rbac,
        "test-apikey": cmd_test_apikey,
        "workflow": cmd_workflow,
        "start": cmd_start,
        "stop": cmd_stop,
        "cleanup": cmd_cleanup,
        "restart": cmd_restart,
    }
    return handlers[args.cmd](args)


if __name__ == "__main__":
    sys.exit(main())
