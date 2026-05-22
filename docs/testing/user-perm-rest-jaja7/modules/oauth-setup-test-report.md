# OAuth 登录准备 - 业务测试报告

- 时间: 2026-05-22 17:27:21
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 0/2（跳过 1）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-00 | password token | POST | 500 | FAIL | 0.107 | notEmpty:result.access_token | 请求地址发生服务器错误 |
| TC-UP-00 | userinfo | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {accessToken} |
