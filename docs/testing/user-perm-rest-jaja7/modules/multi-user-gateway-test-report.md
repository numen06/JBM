# 双用户经网关全链路 - 业务测试报告

- 时间: 2026-05-24 21:05:29
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 8/8

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-MU-01 | register user A | POST | 200 | PASS | 0.182 | - |  |
| TC-UP-MU-01 | register user B | POST | 200 | PASS | 0.148 | - |  |
| TC-UP-MU-01 | oauth user A | POST | 200 | PASS | 0.922 | - |  |
| TC-UP-MU-01 | gateway current user A | GET | 200 | PASS | 0.344 | notNull:result.userId |  |
| TC-UP-MU-01 | gateway authority resources A | GET | 200 | PASS | 0.442 | isList:result |  |
| TC-UP-MU-01 | oauth user B | POST | 200 | PASS | 0.950 | - |  |
| TC-UP-MU-01 | gateway current user B | GET | 200 | PASS | 0.330 | notNull:result.userId; neq:result.userId:{userIdA} |  |
| TC-UP-MU-01 | gateway auth userinfo B via auth route | GET | 200 | PASS | 0.637 | notNull:result.userId |  |
