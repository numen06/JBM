# 网关配置 - 业务测试报告

- 时间: 2026-05-24 21:05:27
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 1/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-GW-01 | 路由分页 | GET | 400 | PASS | 0.005 | notNull:result.total; isList:result.contents | 缺少签名参数 |
| TC-GW-01 | IP 限流分页 | GET | 400 | FAIL | 0.004 | notNull:result.total | 缺少签名参数 |
| TC-GW-01 | 速率限流分页 | GET | 400 | FAIL | 0.004 | notNull:result.total | 缺少签名参数 |
