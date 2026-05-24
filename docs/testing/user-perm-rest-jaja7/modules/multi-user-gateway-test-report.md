# 双用户经网关全链路 - 业务测试报告

- 时间: 2026-05-24 22:02:50
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 8/8

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-MU-01 | register user A | POST | 200 | PASS | 0.155 | - |  |
| TC-UP-MU-01 | register user B | POST | 200 | PASS | 0.181 | - |  |
| TC-UP-MU-01 | oauth user A | POST | 200 | PASS | 0.965 | - |  |
| TC-UP-MU-01 | gateway current user A | GET | 200 | PASS | 0.314 | notNull:result.userId |  |
| TC-UP-MU-01 | gateway authority resources A | GET | 200 | PASS | 0.469 | isList:result |  |
| TC-UP-MU-01 | oauth user B | POST | 200 | PASS | 0.996 | - |  |
| TC-UP-MU-01 | gateway current user B | GET | 200 | PASS | 0.341 | notNull:result.userId; neq:result.userId:{userIdA} |  |
| TC-UP-MU-01 | gateway auth userinfo B via auth route | GET | 200 | PASS | 0.212 | notNull:result.userId |  |
