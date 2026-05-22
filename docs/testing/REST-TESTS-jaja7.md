# REST 测试（profile jaja7）

所有脚本统一使用 Spring profile **jaja7**（scripts/jbm_rest_profile.py）。

## 执行

```bash
python scripts/run_all_rest_tests.py --wait 30
python scripts/run_auth_rest_tests.py --profile jaja7
python scripts/run_center_rest_tests.py
python scripts/run_feign_trust_rest_tests.py
python scripts/run_user_perm_rest_tests.py --auth-url http://127.0.0.1:5555
python scripts/run_satoken_verify_tests.py
```

--profile 若传入其它值会被强制改回 jaja7。本地启动服务时请使用 -Dspring-boot.run.profiles=jaja7。