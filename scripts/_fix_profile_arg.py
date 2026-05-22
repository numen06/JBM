from pathlib import Path
ROOT = Path(r"d:/workspaces/JBM7/scripts")
for name in ["run_center_rest_tests.py", "run_auth_rest_tests.py", "run_feign_trust_rest_tests.py", "run_user_perm_rest_tests.py"]:
    p = ROOT / name
    t = p.read_text(encoding="utf-8")
    t = t.replace('ap.add_argument("--profile", default="jaja7")', 'ap.add_argument("--profile", default=REST_PROFILE, help=f"Spring profile (fixed {REST_PROFILE})")')
    p.write_text(t, encoding="utf-8")
    print("ok", name)