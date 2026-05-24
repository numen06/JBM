# 用户常规操作 - 业务测试报告

- 时间: 2026-05-24 21:32:41
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 7/7

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-01 | current user | GET | 200 | PASS | 0.328 | notNull:result.userId |  |
| TC-UP-02 | current menus | GET | 200 | PASS | 0.311 | isList:result |  |
| TC-UP-03 | POST sessions | POST | 200 | PASS | 0.096 | notNull:result.userId |  |
| TC-UP-04 | list by userName | GET | 200 | PASS | 0.224 | sizeGte:result:1 |  |
| TC-UP-05 | statistics | GET | 200 | PASS | 0.224 | notNull:result.usersTotal; gte:result.usersTotal:0 |  |
| TC-UP-06 | patch nick | PUT | 200 | PASS | 0.307 | - |  |
| TC-UP-06 | verify nick | GET | 200 | PASS | 0.360 | contains:result.nickName:REST_perm_ |  |
