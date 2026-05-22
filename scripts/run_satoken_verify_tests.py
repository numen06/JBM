#!/usr/bin/env python3
"""Sa-Token verify: JUnit + REST (profile jaja7)."""
import argparse
import subprocess
import sys
from pathlib import Path

from jbm_rest_profile import REST_PROFILE, apply_rest_profile

ROOT = Path(__file__).resolve().parents[1]
AUTH_MODULE = "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth"


def run_mvn_unit_tests():
    cmd = [
        "mvn", "-pl", AUTH_MODULE, "-am", "test", "-DskipTests=false",
        "-Dtest=TokenConfigTest,AccessTokenExpiryAlignerTest",
        "-Dsurefire.failIfNoSpecifiedTests=false",
    ]
    print("[mvn]", " ".join(cmd), flush=True)
    return subprocess.run(cmd, cwd=str(ROOT)).returncode == 0


def run_script(name, extra_args=None):
    script = ROOT / "scripts" / name
    cmd = [sys.executable, str(script)] + (extra_args or [])
    print("[rest]", " ".join(cmd), flush=True)
    return subprocess.run(cmd, cwd=str(ROOT)).returncode == 0


def main():
    ap = argparse.ArgumentParser(description="Sa-Token verify (profile jaja7)")
    ap.add_argument("--profile", default=REST_PROFILE, help=f"fixed to {REST_PROFILE}")
    ap.add_argument("--skip-mvn", action="store_true")
    ap.add_argument("--skip-auth", action="store_true")
    ap.add_argument("--skip-feign", action="store_true")
    ap.add_argument("--skip-user-perm", action="store_true")
    ap.add_argument("--wait", type=int, default=15)
    ap.add_argument("--base-url", default="")
    ap.add_argument("--auth-url", default="")
    args = ap.parse_args()
    cfg = {"profile": args.profile}
    profile = apply_rest_profile(cfg, args.profile)
    ok = True
    rest = ["--profile", profile, "--wait", str(args.wait)]
    if args.base_url:
        rest += ["--base-url", args.base_url]
    if not args.skip_mvn:
        ok = run_mvn_unit_tests() and ok
    if not args.skip_auth:
        auth = list(rest)
        if args.auth_url:
            auth += ["--base-url", args.auth_url]
        ok = run_script("run_auth_rest_tests.py", auth) and ok
    if not args.skip_feign:
        ok = run_script("run_feign_trust_rest_tests.py", rest) and ok
    if not args.skip_user_perm:
        up = list(rest)
        if args.auth_url:
            up += ["--auth-url", args.auth_url]
        ok = run_script("run_user_perm_rest_tests.py", up) and ok
    print("\n[summary]", "PASS" if ok else "FAIL", f"profile={profile}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
