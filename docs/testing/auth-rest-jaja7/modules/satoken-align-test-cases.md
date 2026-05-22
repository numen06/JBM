# Sa-Token OAuth2 align - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-AUTH-14 config unified

**前置条件**：Auth 已启动

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| diagnose config | diagnose config | GET | /token/diagnose/config | 接口 success=true |

## TC-AUTH-15 renewal then ttl aligned

**前置条件**：TC-AUTH-01 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| password token | password token | POST | /oauth2/token | 接口 success=true |
| renewal | renewal | POST | /oauth2/renewal | 接口 success=true |
| diagnose after renewal | diagnose after renewal | GET | /token/diagnose/check | contains:result.诊断结论:双层Token均有效; ttlMaxDelta:result.1_Sa-Token层.token_TTL(秒):result.2_OAuth2层.access_token_TTL(秒):120 |

