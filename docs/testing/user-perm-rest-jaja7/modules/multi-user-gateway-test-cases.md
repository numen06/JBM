# 双用户经网关全链路 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-UP-MU-01 用户A/B注册、登录并经Gateway访问权限接口

**前置条件**：Gateway+Auth+Center 可用

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| register user A | register user A | POST | /user/registrations | 接口 success=true |
| register user B | register user B | POST | /user/registrations | 接口 success=true |
| oauth user A | oauth user A | POST | /oauth2/token | 接口 success=true |
| gateway current user A | gateway current user A | GET | /current/user | notNull:result.userId |
| gateway authority resources A | gateway authority resources A | GET | /authority/resources | isList:result |
| oauth user B | oauth user B | POST | /oauth2/token | 接口 success=true |
| gateway current user B | gateway current user B | GET | /current/user | notNull:result.userId; neq:result.userId:{userIdA} |
| gateway auth userinfo B via auth route | gateway auth userinfo B via auth route | GET | /oauth2/userinfo | notNull:result.userId |

