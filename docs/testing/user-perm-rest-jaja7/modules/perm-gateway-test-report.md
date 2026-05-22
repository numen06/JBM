# 经网关透传用户 Token - 业务测试报告

- 时间: 2026-05-22 18:49:23
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 0/2（跳过 1）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-30 | gateway current user | GET | 401 | FAIL | 0.006 | eq:result.userId:{userId} | 服务认证失败，无法访问系统资源 |
| TC-UP-30 | auth userinfo same user | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {accessToken} |
