# API Key 全流程测试报告

- 时间: 2026-05-25 03:26:58
- Gateway: http://127.0.0.1:7777

| 用例 | 步骤 | 结果 | 备注 |
|------|------|------|------|
| TC1 | 用户注册 | PASS |  |
| TC2 | 申请开发者 | PASS |  |
| TC3 | 管理员审批 | PASS |  |
| TC3b | 管理员分配 API 权限 | PASS |  |
| TC4 | 创建业务应用 | PASS |  |
| TC6 | 创建个人 API Key | PASS |  |
| TC7 | 创建应用 API Key | PASS |  |
| TC8 | API Key 授权 | PASS |  |
| TC9 | 第三方 client_token | PASS |  |
| TC10 | 签名调用已授权 API | PASS |  |
| TC12 | 越权拒绝 | PASS |  |
