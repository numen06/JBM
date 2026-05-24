# login lock - 业务测试报告

- 时间: 2026-05-24 20:26:07
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 0/11（跳过 10）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-07-prep | register lock user | POST | 0 | FAIL | 2.032 | - | <urlopen error [WinError 10061] 由于目标计算机积极拒绝，无法连接。> |
| TC-AUTH-07 | wrong 1 | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-07 | wrong 2 | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-07 | wrong 3 | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-07 | wrong 4 | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-07 | wrong 5 | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-07 | locked | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-11 | wrong 1 | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-11 | wrong 2 | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-11 | wrong 3 | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
| TC-AUTH-11 | correct login | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {lockUserName} |
