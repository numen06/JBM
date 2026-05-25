# 经网关透传用户 Token - 业务测试报告

- 时间: 2026-05-25 03:36:03
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 2/2

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-30 | gateway current user | GET | 200 | PASS | 0.324 | eq:result.userId:{userId} |  |
| TC-UP-30 | auth userinfo same user | GET | 200 | PASS | 0.217 | eq:result.userId:{gwUserId} |  |
