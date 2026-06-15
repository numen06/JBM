from pathlib import Path
SCRIPTS = Path(__file__).resolve().parents[2]
center = SCRIPTS / "run_center_rest_tests.py"
t = center.read_text(encoding="utf-8")
bad = "        start_center(profile)\n        service_ok = wait_health_dual(cfg, timeout=args.wait)"
good = "        start_center(profile)\n        service_ok = wait_health(cfg[\"base_url\"], cfg[\"health_path\"], th, tid, timeout=args.wait)"
if bad in t:
    center.write_text(t.replace(bad, good), encoding="utf-8")
    print("fixed center")
for name in ("run_auth_rest_tests.py", "run_feign_trust_rest_tests.py"):
    p = SCRIPTS / name
    t = p.read_text(encoding="utf-8")
    ch = False
    if "wait_health_dual" in t:
        t = t.replace("        service_ok = wait_health_dual(cfg, timeout=args.wait)", "        service_ok = wait_health(cfg[\"base_url\"], cfg[\"health_path\"], th, tid, timeout=args.wait)")
        ch = True
    if "start_auth(args.profile)" in t:
        t = t.replace("start_auth(args.profile)", "start_auth(profile)"); ch = True
    if ch:
        p.write_text(t, encoding="utf-8"); print("fixed", name)