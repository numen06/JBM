# OAuth2 core - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-AUTH-02 password grant

**前置条件**：TC-AUTH-01 已注册

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| password token | password token | POST | /oauth2/token | notEmpty:result.access_token |

## TC-AUTH-05 userinfo

**前置条件**：TC-AUTH-01 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| userinfo | userinfo | GET | /oauth2/userinfo | notNull:result.userId; isList:result.roles; isList:result.menuPermission |

## TC-AUTH-04 refresh

**前置条件**：TC-AUTH-01 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| refresh | refresh | POST | /oauth2/refresh | 接口 success=true |

## TC-AUTH-12 renewal

**前置条件**：TC-AUTH-01 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| renewal | renewal | POST | /oauth2/renewal | 接口 success=true |

## TC-AUTH-13 diagnose

**前置条件**：TC-AUTH-01 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| diagnose | diagnose | GET | /token/diagnose/check | contains:result.诊断结论:双层Token均有效; ttlMaxDelta:result.1_Sa-Token层.token_TTL(秒):result.2_OAuth2层.access_token_TTL(秒):120 |

