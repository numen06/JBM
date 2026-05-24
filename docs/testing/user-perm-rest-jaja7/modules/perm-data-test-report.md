# 权限数据（菜单/资源） - 业务测试报告

- 时间: 2026-05-25 00:11:41
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 4/4

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-15 | authority resources | GET | 200 | PASS | 0.334 | isList:result |  |
| TC-UP-15 | authority menu tree | GET | 200 | PASS | 0.401 | isList:result |  |
| TC-UP-15 | user authorities | GET | 200 | PASS | 0.188 | isList:result |  |
| TC-UP-16 | userinfo permissions | GET | 200 | PASS | 0.198 | sizeGte:result.menuPermission:0; sizeGte:result.roles:0 |  |
