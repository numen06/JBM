# 专用测试用户准备 - 业务测试报告

- 时间: 2026-05-22 18:49:23
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 0/4（跳过 3）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-00 | register | POST | 401 | FAIL | 0.005 | - | 服务认证失败，无法访问系统资源 |
| TC-UP-00 | resolve userId | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {testUserName} |
| TC-UP-00 | oauth password | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {testUserName} |
| TC-UP-00 | userinfo | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {accessToken} |
