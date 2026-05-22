#!/usr/bin/env python3
"""Run all REST suites with profile jaja7."""
import subprocess
import sys
from pathlib import Path

from jbm_rest_profile import REST_PROFILE, apply_rest_profile

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = [
    "run_auth_rest_tests.py",
    "run_center_rest_tests.py",
    "run_feign_trust_rest_tests.py",
    "run_user_perm_rest_tests.py",
]


def main():
    import argparse
    ap = argparse.ArgumentParser(description="All REST tests (profile jaja7)")
    ap.add_argument("--profile", default=REST_PROFILE)
    ap.add_argument("--wait", type=int, default=20)
    ap.add_argument("--base-url", default="")
    ap.add_argument("--auth-url", default="")
    args = ap.parse_args()
    profile = apply_rest_profile({}, args.profile)
    base = ["--profile", profile, "--wait", str(args.wait)]
    if args.base_url:
        base += ["--base-url", args.base_url]
    ok = True
    for script in SCRIPTS:
        extra = list(base)
        if script == "run_auth_rest_tests.py" and args.auth_url:
            extra += ["--base-url", args.auth_url]
        if script == "run_user_perm_rest_tests.py" and args.auth_url:
            extra += ["--auth-url", args.auth_url]
        cmd = [sys.executable, str(ROOT / "scripts" / script)] + extra
        print("[run]", " ".join(cmd), flush=True)
        ok = subprocess.run(cmd, cwd=str(ROOT)).returncode == 0 and ok
    print("[summary]", "PASS" if ok else "FAIL", f"profile={profile}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
