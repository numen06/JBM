# 网关配置 - 业务测试报告

- 时间: 2026-05-22 18:21:01
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 3/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-GW-01 | 路由分页 | GET | 200 | PASS | 0.004 | notNull:result.total; isList:result.contents | 接口异常! |
| TC-GW-01 | IP 限流分页 | GET | 200 | PASS | 0.033 | notNull:result.total |  |
| TC-GW-01 | 速率限流分页 | GET | 200 | PASS | 0.036 | notNull:result.total |  |
