# 方法级权限 SaCheckPermission - 业务测试报告

- 时间: 2026-05-25 03:36:03
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 5/5

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-20 | online pageList | POST | 200 | PASS | 1.641 | notNull:result.total; isList:result.contents |  |
| TC-UP-21 | kickout no auth | DELETE | 401 | PASS | 0.006 | - | HTTP Error 401:  |
| TC-UP-21 | kickout with admin | DELETE | 500 | PASS | 0.310 | - | 无此权限：ACTION_monitor:online:forceLogout |
| TC-UP-22 | logout online no auth | DELETE | 401 | PASS | 0.001 | - | HTTP Error 401:  |
| TC-UP-22 | logout online admin | DELETE | 500 | PASS | 0.288 | - | 无此权限：ACTION_monitor:online:logout |
