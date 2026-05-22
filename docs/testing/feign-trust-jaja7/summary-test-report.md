# Feign 互信测试汇总 (profile jaja7)

- 时间: 2026-05-22 18:49:24
- 地址: http://127.0.0.1:7777
- 服务可用: 是
- 说明: 用例按**典型业务场景**编排，步骤含字段级断言（非仅 success=true）

| 模块 | 步骤数 | 通过 | 结果 |
|------|--------|------|------|
| [Feign test user](modules/test-user-test-report.md) | 2 | 0 | FAIL |
| [Feign trust](modules/trust-test-report.md) | 6 | 5 | FAIL |

## 总体: **部分失败**

执行: `python scripts/run_feign_trust_rest_tests.py`
带 Token: `python scripts/run_feign_trust_rest_tests.py --token <Authorization>`

