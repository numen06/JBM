# 角色 - 业务测试报告

- 时间: 2026-05-24 21:05:27
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 0/7（跳过 5）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-ROLE-01 | 创建角色 | POST | 400 | FAIL | 0.004 | notNull:result | 缺少签名参数 |
| TC-ROLE-01 | 查询详情校验编码 | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {roleId} |
| TC-ROLE-01 | 修改角色名称 | PUT | 0 | SKIP | 0.000 |  | 缺少上下文 {roleId} |
| TC-ROLE-01 | 再次查询校验修改 | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {roleId} |
| TC-ROLE-01 | 删除角色 | DELETE | 0 | SKIP | 0.000 |  | 缺少上下文 {roleId} |
| TC-ROLE-01 | 删除后详情应为空 | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {roleId} |
| TC-ROLE-02 | GET /role/all | GET | 400 | FAIL | 0.026 | isList:result | 缺少签名参数 |
