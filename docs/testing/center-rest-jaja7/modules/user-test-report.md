# 用户 - 业务测试报告

- 时间: 2026-05-24 21:05:27
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 1/4（跳过 2）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-USER-01 | 注册 | POST | 200 | PASS | 0.162 | - |  |
| TC-USER-01 | 按 userName 查询 | GET | 400 | FAIL | 0.012 | sizeGte:result:1; eq:result.0.userName:{testUserName} | 缺少签名参数 |
| TC-USER-01 | 用户详情 | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {userId} |
| TC-USER-01 | 用户角色列表 | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {userId} |
