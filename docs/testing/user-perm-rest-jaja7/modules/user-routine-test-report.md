# 用户常规操作 - 业务测试报告

- 时间: 2026-05-22 18:49:23
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 1/7（跳过 2）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-01 | current user | GET | 401 | FAIL | 0.008 | notNull:result.userId | 服务认证失败，无法访问系统资源 |
| TC-UP-02 | current menus | GET | 401 | PASS | 0.005 | isList:result | 服务认证失败，无法访问系统资源 |
| TC-UP-03 | POST sessions | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {testUserName} |
| TC-UP-04 | list by userName | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {testUserName} |
| TC-UP-05 | statistics | GET | 401 | FAIL | 0.010 | notNull:result.usersTotal; gte:result.usersTotal:0 | 服务认证失败，无法访问系统资源 |
| TC-UP-06 | patch nick | PUT | 401 | FAIL | 0.005 | - | 服务认证失败，无法访问系统资源 |
| TC-UP-06 | verify nick | GET | 401 | FAIL | 0.006 | contains:result.nickName:REST_perm_ | 服务认证失败，无法访问系统资源 |
