# -*- coding: utf-8 -*-
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[2]
IMPORT_BLOCK = """
from jbm_rest_profile import REST_PROFILE, apply_rest_profile, docs_dir, spring_boot_profile_arg
"""

RUNNERS = [
    ("run_center_rest_tests.py", "center-rest", "Center"),
    ("run_auth_rest_tests.py", "auth-rest", "Auth"),
    ("run_feign_trust_rest_tests.py", "feign-trust", "Feign"),
    ("run_user_perm_rest_tests.py", "user-perm-rest", "User perm"),
]

for name, slug, _ in RUNNERS:
    path = SCRIPTS / name
    if path.read_bytes()[:4] != b"#!/u":
        raise SystemExit(f"bad encoding: {name}")
    t = path.read_text(encoding="utf-8")
    if "jbm_rest_profile" not in t:
        t = t.replace("from pathlib import Path\n", "from pathlib import Path\n" + IMPORT_BLOCK)
    t = t.replace(f'DOCS = ROOT / "docs/testing/{slug}-jaja7"\n', f'SUITE_DOCS_SLUG = "{slug}"\n')
    if "profile = apply_rest_profile" not in t:
        t = t.replace(
            "    cfg = load_config()\n",
            "    cfg = load_config()\n    profile = apply_rest_profile(cfg, args.profile)\n    docs = docs_dir(ROOT, SUITE_DOCS_SLUG, profile)\n",
            1,
        )
        t = t.replace("    cfg[\"profile\"] = args.profile\n", "")
    t = t.replace("    DOCS.mkdir(parents=True, exist_ok=True)\n    (DOCS / \"modules\").mkdir(exist_ok=True)\n", "")
    t = t.replace("DOCS /", "docs /")
    t = t.replace("(DOCS /", "(docs /")
    t = t.replace("-Dspring-boot.run.profiles={profile}", "-Dspring-boot.run.profiles={spring_boot_profile_arg(profile)}")
    # fix user_perm broken start block
    t = t.replace(
        "        start_center(args.profile)\n        service_ok = wait_health(cfg[\"base_url\"], cfg[\"health_path\"], th, tid, timeout=args.wait)",
        "        start_center(profile)\n        service_ok = wait_health_dual(cfg, timeout=args.wait)",
    )
    path.write_text(t, encoding="utf-8")
    print("patched", name)

# satoken verify
sv = SCRIPTS / "run_satoken_verify_tests.py"
content = '''#!/usr/bin/env python3
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
    print("\\n[summary]", "PASS" if ok else "FAIL", f"profile={profile}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
'''
sv.write_text(content, encoding="utf-8")
print("patched run_satoken_verify_tests.py")

# run all
all_py = SCRIPTS / "run_all_rest_tests.py"
all_py.write_text('''#!/usr/bin/env python3
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
''', encoding="utf-8")
print("created run_all_rest_tests.py")