# Auth OAuth2测试汇总 (profile jaja7)

- 时间: 2026-05-22 18:20:55
- 地址: http://127.0.0.1:5555
- 服务可用: 是
- 说明: 用例按**典型业务场景**编排，步骤含字段级断言（非仅 success=true）

| 模块 | 步骤数 | 通过 | 结果 |
|------|--------|------|------|
| [OAuth2 smoke](modules/oauth2-smoke-test-report.md) | 2 | 2 | PASS |
| [专用测试用户](modules/test-user-test-report.md) | 2 | 2 | PASS |
| [OAuth2 core](modules/oauth2-core-test-report.md) | 5 | 5 | PASS |
| [Sa-Token OAuth2 align](modules/satoken-align-test-report.md) | 4 | 4 | PASS |
| [login lock](modules/oauth2-lock-test-report.md) | 11 | 11 | PASS |
| [logout](modules/oauth2-logout-test-report.md) | 2 | 2 | PASS |

## 总体: **ALL PASS**

执行: `python scripts/run_auth_rest_tests.py`
带 Token: `python scripts/run_auth_rest_tests.py --token <Authorization>`

