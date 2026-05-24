# 用户与权限 REST 测试汇总 (profile jaja7)

- 时间: 2026-05-25 00:11:41
- Gateway: http://127.0.0.1:7777
- Auth: http://127.0.0.1:5555
- 服务可用: 是
- 说明: 用例按**典型业务场景**编排，步骤含字段级断言（非仅 success=true）

| 模块 | 步骤数 | 通过 | 结果 |
|------|--------|------|------|
| [专用测试用户准备](modules/test-user-bootstrap-test-report.md) | 3 | 3 | PASS |
| [用户常规操作](modules/user-routine-test-report.md) | 7 | 7 | PASS |
| [登录守卫](modules/perm-login-test-report.md) | 3 | 3 | PASS |
| [权限数据（菜单/资源）](modules/perm-data-test-report.md) | 4 | 4 | PASS |
| [方法级权限 SaCheckPermission](modules/perm-action-test-report.md) | 5 | 5 | PASS |
| [经网关透传用户 Token](modules/perm-gateway-test-report.md) | 2 | 2 | PASS |
| [双用户经网关全链路](modules/multi-user-gateway-test-report.md) | 8 | 8 | PASS |

## 总体: **ALL PASS**

执行: `python scripts/run_user_perm_rest_tests.py`
带 Token: `python scripts/run_user_perm_rest_tests.py --token <Authorization>`

