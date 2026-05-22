# 双用户经网关全链路 - 业务测试报告

- 时间: 2026-05-22 18:49:23
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 0/8（跳过 3）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-MU-01 | register user A | POST | 401 | FAIL | 0.004 | - | 服务认证失败，无法访问系统资源 |
| TC-UP-MU-01 | register user B | POST | 401 | FAIL | 0.003 | - | 服务认证失败，无法访问系统资源 |
| TC-UP-MU-01 | oauth user A | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {testUserNameA} |
| TC-UP-MU-01 | gateway current user A | GET | 401 | FAIL | 0.003 | notNull:result.userId | 服务认证失败，无法访问系统资源 |
| TC-UP-MU-01 | gateway authority resources A | GET | 401 | FAIL | 0.005 | isList:result | 服务认证失败，无法访问系统资源 |
| TC-UP-MU-01 | oauth user B | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {testUserNameB} |
| TC-UP-MU-01 | gateway current user B | GET | 401 | FAIL | 0.003 | notNull:result.userId; neq:result.userId:{userIdA} | 服务认证失败，无法访问系统资源 |
| TC-UP-MU-01 | gateway auth userinfo B via auth route | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {accessTokenB} |
