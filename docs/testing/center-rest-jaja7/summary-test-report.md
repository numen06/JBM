# Center 业务场景测试汇总 (profile jaja7)

- 时间: 2026-05-24 21:05:27
- 地址: http://127.0.0.1:7777
- 服务可用: 是
- 说明: 用例按**典型业务场景**编排，步骤含字段级断言（非仅 success=true）

| 模块 | 步骤数 | 通过 | 结果 |
|------|--------|------|------|
| [登录会话](modules/session-test-report.md) | 1 | 0 | FAIL |
| [用户](modules/user-test-report.md) | 4 | 1 | FAIL |
| [角色](modules/role-test-report.md) | 7 | 0 | FAIL |
| [菜单](modules/menu-test-report.md) | 1 | 0 | FAIL |
| [权限](modules/authority-test-report.md) | 2 | 0 | FAIL |
| [应用](modules/app-test-report.md) | 1 | 0 | FAIL |
| [网关配置](modules/gateway-test-report.md) | 3 | 1 | FAIL |
| [扩展表单](modules/extend-form-test-report.md) | 3 | 0 | FAIL |
| [自定义表单](modules/custom-forms-test-report.md) | 2 | 0 | FAIL |
| [当前用户](modules/current-test-report.md) | 1 | 1 | PASS |
| [其它](modules/misc-test-report.md) | 3 | 1 | FAIL |

## 总体: **部分失败**

执行: `python scripts/run_center_rest_tests.py`
带 Token: `python scripts/run_center_rest_tests.py --token <Authorization>`

