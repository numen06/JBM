# RBAC 对比测试报告

- 时间: 2026-05-23T11:55:12
- Gateway: `http://127.0.0.1:7777`
- 结果: **33/33** 通过

## 断言明细

| 领域 | 用户 | 检查项 | 期望 | 实际 | 结果 |
|------|------|--------|------|------|------|
| API | admin | GET /user 分页 | success | total=118 | PASS |
| 多凭证 | admin | 登录 admin | 成功 | 2057849052900044802 | PASS |
| 多凭证 | admin | userId 唯一 | 1 | 1 | PASS |
| 菜单 | admin | 含 /dashboard | true | True | PASS |
| 菜单 | admin | 含 /system/users | true | True | PASS |
| 菜单 | admin | 含 /system/roles | true | True | PASS |
| 菜单 | admin | 菜单数量 | >=8 | 18 | PASS |
| 按钮 | admin | 含 ACTION_dict_delete | true | True | PASS |
| 按钮 | admin | 含 ACTION_users_add | true | True | PASS |
| 多凭证 | demo | 登录 demo | 成功 | 2058024575231987713 | PASS |
| 多凭证 | demo | 登录 13800138000 | 成功 | 2058024575231987713 | PASS |
| 多凭证 | demo | 登录 demo@jbm.local | 成功 | 2058024575231987713 | PASS |
| 多凭证 | demo | userId 唯一 | 1 | 1 | PASS |
| 菜单 | demo | 含 /system/dicts | true | True | PASS |
| 菜单 | demo | 含 /dashboard | true | True | PASS |
| 菜单 | demo | 菜单数量 | >=2 | 3 | PASS |
| 按钮 | demo | 含 ACTION_dict_view | true | True | PASS |
| 按钮 | demo | 含 ACTION_dict_add | true | True | PASS |
| 按钮 | demo | 不含 ACTION_dict_delete | false | False | PASS |
| 按钮 | demo | 不含 ACTION_users_add | false | False | PASS |
| 逻辑 | demo | 无用户管理菜单 | false | False | PASS |
| 多凭证 | viewer | 登录 viewer | 成功 | 2058026838692335617 | PASS |
| 多凭证 | viewer | 登录 13900139000 | 成功 | 2058026838692335617 | PASS |
| 多凭证 | viewer | 登录 viewer@jbm.local | 成功 | 2058026838692335617 | PASS |
| 多凭证 | viewer | userId 唯一 | 1 | 1 | PASS |
| 菜单 | viewer | 含 /system/users | true | True | PASS |
| 菜单 | viewer | 菜单数量 | >=1 | 1 | PASS |
| 按钮 | viewer | 含 ACTION_users_view | true | True | PASS |
| 按钮 | viewer | 含 ACTION_users_edit | true | True | PASS |
| 按钮 | viewer | 不含 ACTION_users_add | false | False | PASS |
| 按钮 | viewer | 不含 ACTION_users_delete | false | False | PASS |
| 逻辑 | viewer | 无字典管理菜单 | false | False | PASS |
| 造数 | admin | 角色 operator/editor | 存在 | {'editor', 'operator_test', 'operator'} | PASS |
