# Center 业务场景测试汇总 (profile jaja7)

- 时间: 2026-05-24 22:02:37
- 地址: http://127.0.0.1:7777
- 服务可用: 是
- 说明: 用例按**典型业务场景**编排，步骤含字段级断言（非仅 success=true）

| 模块 | 步骤数 | 通过 | 结果 |
|------|--------|------|------|
| [登录会话](modules/session-test-report.md) | 1 | 1 | PASS |
| [用户](modules/user-test-report.md) | 4 | 4 | PASS |
| [角色](modules/role-test-report.md) | 7 | 7 | PASS |
| [菜单](modules/menu-test-report.md) | 1 | 1 | PASS |
| [权限](modules/authority-test-report.md) | 2 | 2 | PASS |
| [应用](modules/app-test-report.md) | 1 | 1 | PASS |
| [网关配置](modules/gateway-test-report.md) | 3 | 3 | PASS |
| [扩展表单](modules/extend-form-test-report.md) | 3 | 3 | PASS |
| [自定义表单](modules/custom-forms-test-report.md) | 2 | 2 | PASS |
| [当前用户](modules/current-test-report.md) | 1 | 1 | PASS |
| [其它](modules/misc-test-report.md) | 3 | 3 | PASS |

## 总体: **ALL PASS**

执行: `python scripts/run_center_rest_tests.py`
带 Token: `python scripts/run_center_rest_tests.py --token <Authorization>`

