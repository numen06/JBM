#!/usr/bin/env python3
"""Run JBM Center H2 module tests and generate per-module docs."""
import argparse
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
CENTER = ROOT / "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center"
DOCS = ROOT / "docs/testing/modules"
SUREFIRE = CENTER / "target/surefire-reports"
CONFIG = ROOT / "scripts/center_test_modules.json"


def load_modules():
    return json.loads(CONFIG.read_text(encoding="utf-8"))


def run_maven(test_filter, timeout=900):
    cmd = (
        "mvn test -DskipTests=false -Dskip=false "
        "-pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am "
        f"-Dtest={test_filter} -DfailIfNoTests=false "
        "-Dsurefire.failIfNoSpecifiedTests=false"
    )
    print("[mvn]", cmd, flush=True)
    return subprocess.run(cmd, cwd=str(ROOT), shell=True, timeout=timeout).returncode


def find_report_xml(class_name):
    direct = SUREFIRE / f"TEST-{class_name}.xml"
    if direct.exists():
        return direct
    matches = sorted(SUREFIRE.glob(f"TEST-*.{class_name}.xml"))
    if matches:
        return matches[0]
    matches = sorted(SUREFIRE.glob(f"TEST-*{class_name}*.xml"))
    return matches[0] if matches else None


def parse_xml(path):
    if path is None or not path.exists():
        return None
    root = ET.parse(path).getroot()
    suite = root if root.tag == "testsuite" else root.find("testsuite")
    if suite is None:
        return None
    rep = {
        "tests": int(suite.attrib.get("tests", 0)),
        "failures": int(suite.attrib.get("failures", 0)),
        "errors": int(suite.attrib.get("errors", 0)),
        "skipped": int(suite.attrib.get("skipped", 0)),
        "time": float(suite.attrib.get("time", 0)),
        "cases": [],
    }
    for tc in suite.findall("testcase"):
        st, msg = "passed", ""
        if tc.find("failure") is not None:
            st, msg = "failed", (tc.find("failure").text or "")[:400]
        elif tc.find("error") is not None:
            st, msg = "error", (tc.find("error").text or "")[:400]
        elif tc.find("skipped") is not None:
            st = "skipped"
        rep["cases"].append({
            "name": tc.attrib.get("name", ""),
            "time": float(tc.attrib.get("time", 0)),
            "status": st,
            "msg": msg,
        })
    return rep


def write_cases(mod, path):
    lines = [
        f"# {mod['title']} - test cases",
        "",
        f"Test classes: `{', '.join(mod['classes'])}`",
        "",
        "| Case ID | Description |",
        "|---------|-------------|",
    ]
    for cid, name in mod["cases"]:
        lines.append(f"| {cid} | {name} |")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_report(mod, rep_list, path, run_time, exit_code):
    tests = sum((r or {}).get("tests", 0) for r in rep_list)
    fail = sum((r or {}).get("failures", 0) for r in rep_list)
    err = sum((r or {}).get("errors", 0) for r in rep_list)
    skip = sum((r or {}).get("skipped", 0) for r in rep_list)
    has_report = any(r is not None for r in rep_list)
    passed = tests - fail - err - skip
    if not has_report or (tests == 0 and skip > 0):
        ok = False
        result = "SKIPPED (no tests executed; use -DskipTests=false -Dskip=false from repo root)"
    elif exit_code != 0 or fail > 0 or err > 0:
        ok = False
        result = "FAIL"
    elif tests == 0:
        ok = False
        result = "FAIL (0 tests)"
    else:
        ok = True
        result = "PASS"
    zh = {"passed": "PASS", "failed": "FAIL", "error": "ERROR", "skipped": "SKIP"}
    lines = [
        f"# {mod['title']} - test report",
        "",
        f"- Time: {run_time}",
        f"- Maven exit: {exit_code}",
        f"- Result: **{result}**",
        f"- Total: {tests}, Pass: {passed}, Fail: {fail + err}, Skipped: {skip}",
        "",
        "| Case ID | Method | Status | Time(s) |",
        "|---------|--------|--------|---------|",
    ]
    idx = 0
    for r in rep_list:
        if not r:
            continue
        for c in r["cases"]:
            cid = mod["cases"][idx][0] if idx < len(mod["cases"]) else "-"
            lines.append(
                f"| {cid} | `{c['name']}` | {zh.get(c['status'], c['status'])} | {c['time']:.3f} |"
            )
            idx += 1
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return ok, tests, passed


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--skip-run", action="store_true", help="only regenerate docs from surefire XML")
    args = ap.parse_args()
    modules = load_modules()
    run_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    DOCS.mkdir(parents=True, exist_ok=True)
    all_classes = [c for m in modules for c in m["classes"]]
    exit_code = 0
    if not args.skip_run:
        exit_code = run_maven(",".join(all_classes))
    reports = {cls: parse_xml(find_report_xml(cls)) for cls in all_classes}
    if args.skip_run and any(reports.values()):
        exit_code = 1 if any(
            (r.get("failures", 0) + r.get("errors", 0)) > 0 for r in reports.values() if r
        ) else 0
    if not args.skip_run and exit_code == 0 and not any(reports.values()):
        print("[warn] no surefire reports found under", SUREFIRE)
    summary = []
    all_ok = exit_code == 0
    for mod in modules:
        write_cases(mod, DOCS / f"{mod['id']}-test-cases.md")
        rep_list = [reports.get(c) for c in mod["classes"]]
        ok, tests, passed = write_report(
            mod, rep_list, DOCS / f"{mod['id']}-test-report.md", run_time, exit_code
        )
        all_ok = all_ok and ok
        summary.append((mod["id"], mod["title"], mod["classes"], tests, passed, ok))
        print(f"[doc] {mod['id']}: {passed}/{tests} {'PASS' if ok else 'FAIL/SKIP'}")
    lines = [
        "# JBM Center module test summary",
        "",
        f"Time: {run_time}",
        f"Maven exit: {exit_code}",
        "",
        "| Module | Test class | Total | Pass | Result |",
        "|--------|------------|-------|------|--------|",
    ]
    for mid, title, classes, tests, passed, ok in summary:
        res = "PASS" if ok else "FAIL/SKIP"
        lines.append(
            f"| [{title}](modules/{mid}-test-report.md) | `{', '.join(classes)}` | {tests} | {passed} | {res} |"
        )
    overall = "**ALL PASS**" if all_ok and any(t > 0 for _, _, _, t, _, _ in summary) else "**HAS FAILURES OR SKIPPED**"
    lines += ["", f"## Overall: {overall}", "", "Run: `python scripts/run_center_module_tests.py`", ""]
    (ROOT / "docs/testing/summary-test-report.md").write_text("\n".join(lines), encoding="utf-8")
    print("[doc] summary-test-report.md")
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())