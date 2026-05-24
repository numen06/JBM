# 双用户经网关全链路 - 业务测试报告

- 时间: 2026-05-24 21:32:41
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 8/8

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-MU-01 | register user A | POST | 200 | PASS | 0.175 | - |  |
| TC-UP-MU-01 | register user B | POST | 200 | PASS | 0.177 | - |  |
| TC-UP-MU-01 | oauth user A | POST | 200 | PASS | 0.926 | - |  |
| TC-UP-MU-01 | gateway current user A | GET | 200 | PASS | 0.333 | notNull:result.userId |  |
| TC-UP-MU-01 | gateway authority resources A | GET | 200 | PASS | 0.413 | isList:result |  |
| TC-UP-MU-01 | oauth user B | POST | 200 | PASS | 0.960 | - |  |
| TC-UP-MU-01 | gateway current user B | GET | 200 | PASS | 0.327 | notNull:result.userId; neq:result.userId:{userIdA} |  |
| TC-UP-MU-01 | gateway auth userinfo B via auth route | GET | 200 | PASS | 0.203 | notNull:result.userId |  |
