# 登录守卫 - 业务测试报告

- 时间: 2026-05-22 22:01:27
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 3/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-10 | no token current user | GET | 401 | PASS | 0.113 | isFalse:success | HTTP Error 401: Unauthorized |
| TC-UP-11 | bad token | GET | 401 | PASS | 0.120 | - | HTTP Error 401: Unauthorized |
| TC-UP-12 | no token menus | GET | 401 | PASS | 0.036 | - | HTTP Error 401: Unauthorized |
