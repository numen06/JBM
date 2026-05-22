# 方法级权限 SaCheckPermission - 业务测试报告

- 时间: 2026-05-22 18:49:23
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 2/5（跳过 2）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-20 | online pageList | POST | 401 | FAIL | 0.067 | notNull:result.total; isList:result.contents | 服务认证失败，无法访问系统资源 |
| TC-UP-21 | kickout no auth | DELETE | 401 | PASS | 0.007 | - | HTTP Error 401:  |
| TC-UP-21 | kickout with admin | DELETE | 0 | SKIP | 0.000 |  | 缺少上下文 {accessToken} |
| TC-UP-22 | logout online no auth | DELETE | 401 | PASS | 0.005 | - | HTTP Error 401:  |
| TC-UP-22 | logout online admin | DELETE | 0 | SKIP | 0.000 |  | 缺少上下文 {accessToken} |
