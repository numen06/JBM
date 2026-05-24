#!/usr/bin/env python3
"""Auth OAuth2 REST 测试（典型流程 + 断言，profile jaja7）。"""
import argparse
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

from jbm_rest_profile import REST_PROFILE, apply_rest_profile, docs_dir, spring_boot_profile_arg
from jbm_cluster_client import login_password

ROOT = Path(__file__).resolve().parents[1]
SUITE_DOCS_SLUG = "auth-rest"
CONFIG = ROOT / "scripts/auth_rest_modules.json"
AUTH_MODULE = "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth"

_NO_PROXY_OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

CTX_TOKENS = (
    "userId", "roleId", "formCode", "customFormId", "roleCode",
    "testUserName", "testPassword", "lockUserName", "lockPassword",
)


def load_config():
    return json.loads(CONFIG.read_text(encoding="utf-8"))


def expand(s, ctx):
    if not s:
        return s
    if "{ts}" in s and "ts" not in ctx:
        ctx["ts"] = str(int(time.time() * 1000))
    out = s
    for _ in range(5):
        changed = False
        for k, v in ctx.items():
            ph = "{" + k + "}"
            if ph in out:
                out = out.replace(ph, str(v))
                changed = True
        if not changed:
            break
    return out


def http_request(method, url, headers, body=None, body_type=None, timeout=30):
    t0 = time.time()
    data = None
    h = dict(headers)
    if body_type == "json" and body:
        data = body.encode("utf-8")
        h.setdefault("Content-Type", "application/json")
    elif body_type == "form" and body:
        data = body.encode("utf-8")
        h.setdefault("Content-Type", "application/x-www-form-urlencoded")
    elif body_type == "query" and body:
        qs = urllib.parse.urlencode(urllib.parse.parse_qsl(body, keep_blank_values=True))
        url = url + ("&" if "?" in url else "?") + qs
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with _NO_PROXY_OPENER.open(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            return resp.status, raw, time.time() - t0, None
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace") if e.fp else ""
        return e.code, raw, time.time() - t0, str(e)
    except Exception as e:
        return 0, "", time.time() - t0, str(e)


def parse_result_body(raw):
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return None


def json_path(obj, path):
    cur = obj
    norm = re.sub(r"\[(\d+)\]", r".\1", path)
    for seg in norm.split("."):
        if not seg:
            continue
        if cur is None:
            return None
        if seg.isdigit():
            cur = cur[int(seg)] if isinstance(cur, list) else None
        elif isinstance(cur, dict):
            cur = cur.get(seg)
        else:
            return None
    return cur


def apply_extract(spec, ctx, body, path_form_code=None):
    if not spec:
        return
    items = spec.items() if isinstance(spec, dict) else _parse_kv_spec(spec)
    for key, expr in items:
        if expr.startswith("literal."):
            ctx[key] = expand(expr[8:], ctx)
        elif expr.startswith("path."):
            if path_form_code:
                ctx[key] = path_form_code
        elif body is not None:
            val = json_path(body, expr)
            if val is not None:
                ctx[key] = val


def _parse_kv_spec(spec):
    out = []
    for item in spec.split(";"):
        item = item.strip()
        if item and ":" in item:
            k, v = item.split(":", 1)
            out.append((k.strip(), v.strip()))
    return out


def _norm_val(val):
    if val is None:
        return None
    if isinstance(val, bool):
        return val
    if isinstance(val, (int, float)):
        return val
    return str(val)


def _cmp_eq(actual, expected):
    a, e = _norm_val(actual), _norm_val(expected)
    if a is None and e is None:
        return True
    if a is None or e is None:
        return False
    try:
        if str(a).isdigit() and str(e).isdigit():
            return int(a) == int(e)
    except (TypeError, ValueError):
        pass
    return str(a) == str(e)


def check_assertions(rules, ctx, jb):
    """规则示例: eq:result.roleCode:{roleCode}; sizeGte:result:1; isFalse:success"""
    if not rules:
        return True, ""
    failures = []
    for rule in rules:
        if not rule or ":" not in rule:
            continue
        op, rest = rule.split(":", 1)
        if op in ("eq", "neq", "sizeGte", "gte"):
            path, expected = rest.rsplit(":", 1)
            actual = json_path(jb, path) if path != "success" or op == "eq" else jb.get("success")
            if path == "success" and op in ("eq", "neq"):
                actual = jb.get("success") if jb else None
            else:
                actual = json_path(jb, path)
            expected = expand(expected, ctx)
            if op == "eq" and not _cmp_eq(actual, expected):
                failures.append(f"{rule} 实际={actual!r}")
            elif op == "neq" and _cmp_eq(actual, expected):
                failures.append(f"{rule} 不应等于 {expected!r}")
            elif op == "sizeGte":
                n = int(expected)
                if not isinstance(actual, list) or len(actual) < n:
                    failures.append(f"{rule} 长度={len(actual) if isinstance(actual, list) else 'N/A'}")
            elif op == "gte":
                try:
                    if actual is None or float(actual) < float(expected):
                        failures.append(f"{rule} 实际={actual!r}")
                except (TypeError, ValueError):
                    failures.append(f"{rule} 非数字 {actual!r}")
        elif op == "contains":
            path, expected = rest.rsplit(":", 1)
            actual = json_path(jb, path) if path != "message" else (jb or {}).get("message", "")
            expected = expand(expected, ctx)
            if expected not in str(actual or ""):
                failures.append(f"{rule} 实际={actual!r}")
        elif op == "notEmpty":
            v = json_path(jb, rest)
            if v is None or v == "" or v == [] or v == {}:
                failures.append(f"{rule} 为空")
        elif op == "notNull":
            if json_path(jb, rest) is None:
                failures.append(f"{rule} 为 null")
        elif op == "notEmpty":
            v = json_path(jb, rest)
            if v is None or v == "" or v == [] or v == {}:
                failures.append(f"{rule} 为空")
        elif op == "isList":
            v = json_path(jb, rest)
            if not isinstance(v, list):
                failures.append(f"{rule} 非列表")
        elif op == "isFalse":
            v = jb.get(rest) if rest == "success" and jb else json_path(jb, rest)
            if v is not False and v != 0:
                failures.append(f"{rule} 实际={v!r}")
        elif op == "isTrue":
            v = jb.get(rest) if rest == "success" and jb else json_path(jb, rest)
            if v is not True and v != 1:
                failures.append(f"{rule} 实际={v!r}")
        elif op == "isNull":
            v = jb.get(rest) if rest == "success" and jb else json_path(jb, rest)
            if v is not None:
                failures.append(f"{rule} 实际={v!r}")
        elif op == "notContains":
            path, expected = rest.rsplit(":", 1)
            actual = json_path(jb, path) if path != "message" else (jb or {}).get("message", "")
            expected = expand(expected, ctx)
            if expected in str(actual or ""):
                failures.append(f"{rule} 不应包含 {expected!r}")
        elif op == "ttlMaxDelta":
            parts = rest.split(":")
            if len(parts) >= 3:
                path_a, path_b, max_delta = parts[0], parts[1], parts[2]
                va = json_path(jb, path_a)
                vb = json_path(jb, path_b)
                try:
                    da = abs(float(va) - float(vb))
                    if da > float(max_delta):
                        failures.append(f"{rule} 差值={da} 秒 (a={va}, b={vb})")
                except (TypeError, ValueError):
                    failures.append(f"{rule} TTL 非数字 a={va!r} b={vb!r}")
    if failures:
        return False, "; ".join(failures[:3])
    return True, ""


def wait_health(base_url, health_path, tenant_header="tenantId", tenant_id="0", timeout=180):
    base = base_url.rstrip("/")
    probes = [base + health_path, base + "/role/all"]
    headers = {tenant_header: tenant_id}
    deadline = time.time() + timeout
    while time.time() < deadline:
        for url in probes:
            status, raw, _, _ = http_request("GET", url, headers)
            if status == 200:
                jb = parse_result_body(raw)
                if jb is None or jb.get("success") is True or "UP" in raw:
                    return True
        time.sleep(3)
    return False


def start_auth(profile):
    auth_dir = ROOT / "jbm-cluster" / "jbm-cluster-platform" / "jbm-cluster-platform-auth"
    cmd = f'mvn spring-boot:run "-Dspring-boot.run.profiles={spring_boot_profile_arg(profile)}" -DskipTests=true'
    print("[start]", cmd, "cwd=", auth_dir, flush=True)
    return subprocess.Popen(cmd, cwd=str(auth_dir), shell=True)


def resolve_base(cfg, step):
    if (step.get("service") or "auth").lower() == "gateway":
        return (cfg.get("gateway_base_url") or "http://127.0.0.1:7777").rstrip("/")
    return cfg["base_url"].rstrip("/")


def _missing_ctx(path, ctx):
    for key in CTX_TOKENS:
        if ("{" + key + "}") in path and not ctx.get(key):
            return key
    return None


def run_step(step, cfg, ctx, scenario_id):
    name = step.get("name", "")
    method = step["method"]
    path = step["path"]
    body_type = step.get("bodyType") or step.get("body_type") or ""
    body = step.get("body", "")
    expect = step.get("expect", "success")
    extract = step.get("extract")
    asserts = step.get("assert") or []
    step_headers = step.get("headers") or {}

    miss = _missing_ctx(path, ctx) or (_missing_ctx(body or "", ctx))
    if miss:
        return {
            "scenario": scenario_id,
            "name": name,
            "method": method,
            "path": path,
            "status": "SKIP",
            "http": 0,
            "time": 0.0,
            "msg": f"缺少上下文 {{{miss}}}",
            "assertions": "",
        }

    base = resolve_base(cfg, step)
    url = expand(path, ctx)
    if not url.startswith("http"):
        url = base + url
    headers = {k: expand(v, ctx) for k, v in step_headers.items()}
    token = ctx.get("token") or ctx.get("accessToken") or os.environ.get("AUTH_TOKEN", "")
    if token:
        headers["Authorization"] = token if str(token).startswith("Bearer ") else f"Bearer {token}"
    if not headers.get("Authorization") and (step.get("service") or "auth").lower() == "gateway":
        if path not in ("/user/registrations",) and not path.endswith("/user/registrations"):
            gateway_token = ctx.get("gatewayToken") or ""
            if gateway_token:
                headers["Authorization"] = gateway_token if str(gateway_token).startswith("Bearer ") else f"Bearer {gateway_token}"
    body_s = expand(body, ctx) if body else ""
    path_fc = None
    if "/extend-field/forms/" in url and method == "POST":
        path_fc = url.rsplit("/", 1)[-1].split("?")[0]

    status, raw, elapsed, err = http_request(method, url, headers, body_s, body_type or None)
    jb = parse_result_body(raw)
    ok = False
    msg = err or ""

    if status == 0:
        msg = err or "connection failed"
    elif expect == "optional":
        ok = True
        if jb and jb.get("success") is True:
            aok, amsg = check_assertions(asserts, ctx, jb)
            if not aok:
                ok, msg = False, amsg
        else:
            msg = (jb or {}).get("message") or "可选场景未满足（如无 Token/无组织数据）"
    elif expect == "fail":
        if jb and jb.get("success") is False:
            ok = True
        elif status >= 400:
            ok = True
        else:
            msg = (jb or {}).get("message") or "期望失败但返回成功"
        if ok and asserts:
            aok, amsg = check_assertions(asserts, ctx, jb or {})
            if not aok:
                ok, msg = False, amsg
    elif expect == "success":
        if jb and jb.get("success") is True:
            ok = True
        elif status in (200, 201) and jb is None:
            ok = True
        else:
            msg = (jb or {}).get("message") or raw[:300]
        if ok:
            apply_extract(extract, ctx, jb, path_fc)
            if asserts:
                aok, amsg = check_assertions(asserts, ctx, jb)
                if not aok:
                    ok, msg = False, amsg
    else:
        msg = f"未知 expect={expect}"

    assert_txt = "; ".join(asserts) if asserts else "-"
    return {
        "scenario": scenario_id,
        "name": name,
        "method": method,
        "path": path,
        "status": "PASS" if ok else "FAIL",
        "http": status,
        "time": elapsed,
        "msg": msg[:240] if msg else "",
        "assertions": assert_txt,
    }


def iter_steps(mod):
    for sc in mod.get("scenarios") or []:
        sid = sc.get("id", "")
        for step in sc.get("steps") or []:
            yield sc, step


def write_cases(mod, path):
    lines = [
        f"# {mod['title']} - 业务场景用例",
        "",
        "说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。",
        "",
    ]
    for sc in mod.get("scenarios") or []:
        lines.append(f"## {sc.get('id', '')} {sc.get('title', '')}")
        lines.append("")
        lines.append(f"**前置条件**：{sc.get('precondition', '-')}")
        lines.append("")
        lines.append("| 步骤 | 操作 | 方法 | 路径 | 业务断言 |")
        lines.append("|------|------|------|------|----------|")
        for st in sc.get("steps") or []:
            asserts = st.get("assert") or []
            a = "; ".join(asserts) if asserts else "接口 success=true"
            lines.append(
                f"| {st.get('name', '')} | {st.get('name', '')} | {st.get('method', '')} | "
                f"{st.get('path', '')} | {a} |"
            )
        lines.append("")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_report(mod, results, path, run_time, service_ok):
    total = len(results)
    passed = sum(1 for r in results if r["status"] == "PASS")
    skipped = sum(1 for r in results if r["status"] == "SKIP")
    active = total - skipped
    ok = passed == active and active > 0 and service_ok
    lines = [
        f"# {mod['title']} - 业务测试报告",
        "",
        f"- 时间: {run_time}",
        f"- 服务可用: {'是' if service_ok else '否'}",
        f"- 结果: **{'PASS' if ok else 'FAIL'}**",
        f"- 步骤通过: {passed}/{total}" + (f"（跳过 {skipped}）" if skipped else ""),
        "",
        "| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |",
        "|------|------|------|------|------|---------|----------|------|",
    ]
    for r in results:
        lines.append(
            f"| {r['scenario']} | {r['name']} | {r['method']} | {r['http']} | {r['status']} | "
            f"{r['time']:.3f} | {r.get('assertions', '-')} | {r['msg']} |"
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return ok, passed, total


def main():
    ap = argparse.ArgumentParser(description="Auth OAuth2 REST 测试")
    ap.add_argument("--profile", default=REST_PROFILE, help=f"Spring profile (fixed {REST_PROFILE})")
    ap.add_argument("--base-url", default="")
    ap.add_argument("--start", action="store_true")
    ap.add_argument("--wait", type=int, default=180)
    ap.add_argument("--token", default="")
    ap.add_argument("--smoke", action="store_true")
    args = ap.parse_args()
    cfg = load_config()
    profile = apply_rest_profile(cfg, args.profile)
    docs = docs_dir(ROOT, SUITE_DOCS_SLUG, profile)
    if args.base_url:
        cfg["base_url"] = args.base_url
    run_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    ts = str(int(time.time() * 1000))
    ctx = {
        "ts": ts,
        "usuffix": ts[-9:],
        "client_id": cfg.get("client_id", "demo"),
        "client_secret": cfg.get("client_secret", "demo123"),
        "redirect_uri": cfg.get("redirect_uri", ""),
    }
    if args.token:
        ctx["token"] = args.token
    else:
        try:
            gateway_base = (cfg.get("gateway_base_url") or "http://127.0.0.1:7777").rstrip("/")
            ctx["gatewayToken"] = login_password(
                "admin",
                os.environ.get("LOGIN_PASSWORD") or "Admin@123",
                gateway=gateway_base,
            )
        except Exception as e:
            print("[warn] admin login failed for auth gateway steps:", e)

    th, tid = cfg.get("tenant_header", "tenantId"), cfg.get("tenant_id", "0")
    service_ok = wait_health(cfg["base_url"], cfg["health_path"], th, tid, timeout=8)
    if not service_ok and args.start:
        start_auth(profile)
        service_ok = wait_health(cfg["base_url"], cfg["health_path"], th, tid, timeout=args.wait)

    summary = []
    all_ok = service_ok
    modules = [m for m in cfg["modules"] if (not args.smoke or m.get("id") == "oauth2-smoke")]
    for mod in modules:
        write_cases(mod, docs / "modules" / f"{mod['id']}-test-cases.md")
        results = []
        for sc, step in iter_steps(mod):
            if not service_ok:
                results.append({
                    "scenario": sc.get("id", ""),
                    "name": step.get("name", ""),
                    "method": step.get("method", ""),
                    "path": step.get("path", ""),
                    "status": "FAIL",
                    "http": 0,
                    "time": 0.0,
                    "msg": "auth 未就绪",
                    "assertions": "",
                })
            else:
                results.append(run_step(step, cfg, ctx, sc.get("id", "")))
        ok, passed, total = write_report(
            mod, results, docs / "modules" / f"{mod['id']}-test-report.md", run_time, service_ok
        )
        all_ok = all_ok and ok
        summary.append((mod["id"], mod["title"], total, passed, ok))
        print(f"[doc] {mod['id']}: {passed}/{total}")

    lines = [
        "# Auth OAuth2测试汇总 (profile jaja7)",
        "",
        f"- 时间: {run_time}",
        f"- 地址: {cfg['base_url']}",
        f"- 服务可用: {'是' if service_ok else '否'}",
        "- 说明: 用例按**典型业务场景**编排，步骤含字段级断言（非仅 success=true）",
        "",
        "| 模块 | 步骤数 | 通过 | 结果 |",
        "|------|--------|------|------|",
    ]
    for mid, title, total, passed, ok in summary:
        res = "PASS" if ok else "FAIL"
        lines.append(f"| [{title}](modules/{mid}-test-report.md) | {total} | {passed} | {res} |")
    if not service_ok:
        overall = "**FAIL / 服务不可用**"
    elif all_ok:
        overall = "**ALL PASS**"
    else:
        overall = "**部分失败**"
    lines += [
        "",
        f"## 总体: {overall}",
        "",
        "执行: `python scripts/run_auth_rest_tests.py`",
        "带 Token: `python scripts/run_auth_rest_tests.py --token <Authorization>`",
        "",
    ]
    (docs / "summary-test-report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("[doc] summary-test-report.md")
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
