# 登录守卫 - 业务测试报告

- 时间: 2026-05-24 21:32:41
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 3/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-10 | no token current user | GET | 400 | PASS | 0.008 | isFalse:success | HTTP Error 400: Bad Request |
| TC-UP-11 | bad token | GET | 401 | PASS | 0.033 | - | HTTP Error 401: Unauthorized |
| TC-UP-12 | no token menus | GET | 400 | PASS | 0.017 | - | HTTP Error 400: Bad Request |
