# 用户 - 业务测试报告

- 时间: 2026-05-25 03:17:35
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 4/4

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-USER-01 | 注册 | POST | 200 | PASS | 0.289 | - |  |
| TC-USER-01 | 按 userName 查询 | GET | 200 | PASS | 0.144 | sizeGte:result:1; eq:result.0.userName:{testUserName} |  |
| TC-USER-01 | 用户详情 | GET | 200 | PASS | 0.150 | eq:result.userName:{testUserName}; notNull:result.userId |  |
| TC-USER-01 | 用户角色列表 | GET | 200 | PASS | 0.157 | isList:result |  |
