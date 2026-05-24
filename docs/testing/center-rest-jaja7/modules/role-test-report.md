# 角色 - 业务测试报告

- 时间: 2026-05-24 21:32:31
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 7/7

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-ROLE-01 | 创建角色 | POST | 200 | PASS | 0.236 | notNull:result |  |
| TC-ROLE-01 | 查询详情校验编码 | GET | 200 | PASS | 0.190 | eq:result.roleCode:{roleCode}; eq:result.roleName:REST_ROLE |  |
| TC-ROLE-01 | 修改角色名称 | PUT | 200 | PASS | 0.230 | - |  |
| TC-ROLE-01 | 再次查询校验修改 | GET | 200 | PASS | 0.203 | eq:result.roleName:REST_ROLE_UPDATED |  |
| TC-ROLE-01 | 删除角色 | DELETE | 200 | PASS | 0.248 | - |  |
| TC-ROLE-01 | 删除后详情应为空 | GET | 200 | PASS | 0.207 | isNull:result |  |
| TC-ROLE-02 | GET /role/all | GET | 200 | PASS | 0.214 | isList:result |  |
