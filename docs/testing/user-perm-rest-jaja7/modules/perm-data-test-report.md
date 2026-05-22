# 权限数据（菜单/资源） - 业务测试报告

- 时间: 2026-05-22 18:49:23
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 0/4（跳过 2）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-15 | authority resources | GET | 401 | FAIL | 0.031 | isList:result | 服务认证失败，无法访问系统资源 |
| TC-UP-15 | authority menu tree | GET | 401 | FAIL | 0.004 | isList:result | 服务认证失败，无法访问系统资源 |
| TC-UP-15 | user authorities | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {userId} |
| TC-UP-16 | userinfo permissions | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {accessToken} |
