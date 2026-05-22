# Auth OAuth2测试汇总 (profile jaja7)

- 时间: 2026-05-22 11:56:35
- 地址: http://127.0.0.1:5555
- 服务可用: 是
- 说明: 用例按**典型业务场景**编排，步骤含字段级断言（非仅 success=true）

| 模块 | 步骤数 | 通过 | 结果 |
|------|--------|------|------|
| [OAuth2 smoke](modules/oauth2-smoke-test-report.md) | 2 | 2 | PASS |
| [OAuth2 core](modules/oauth2-core-test-report.md) | 7 | 3 | FAIL |
| [login lock](modules/oauth2-lock-test-report.md) | 10 | 8 | FAIL |

## 总体: **部分失败**

执行: `python scripts/run_auth_rest_tests.py`
带 Token: `python scripts/run_auth_rest_tests.py --token <Authorization>`

