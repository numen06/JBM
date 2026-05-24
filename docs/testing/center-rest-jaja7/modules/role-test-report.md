# 角色 - 业务测试报告

- 时间: 2026-05-24 22:02:37
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 7/7

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-ROLE-01 | 创建角色 | POST | 200 | PASS | 0.252 | notNull:result |  |
| TC-ROLE-01 | 查询详情校验编码 | GET | 200 | PASS | 0.226 | eq:result.roleCode:{roleCode}; eq:result.roleName:REST_ROLE |  |
| TC-ROLE-01 | 修改角色名称 | PUT | 200 | PASS | 0.230 | - |  |
| TC-ROLE-01 | 再次查询校验修改 | GET | 200 | PASS | 0.223 | eq:result.roleName:REST_ROLE_UPDATED |  |
| TC-ROLE-01 | 删除角色 | DELETE | 200 | PASS | 0.265 | - |  |
| TC-ROLE-01 | 删除后详情应为空 | GET | 200 | PASS | 0.209 | isNull:result |  |
| TC-ROLE-02 | GET /role/all | GET | 200 | PASS | 0.215 | isList:result |  |
