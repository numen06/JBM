# JBM 认证与访问体系（用户态 + 内部服务互信）

> 本文描述 **当前代码实现**（jaja7 本地联调已验证）。  
> 登录流程、Redis Key、续签与诊断见：[`token-auth-full-chain.md`](../../jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth/docs/token-auth-full-chain.md)。

---

## 1. 设计目标

| 场景 | 目标 |
|------|------|
| **用户访问** | 客户端经 Gateway 携带 `Authorization: Bearer {access_token}` 访问 Center 等，`@SaCheckLogin` 可识别用户 |
| **内部服务访问** | 无用户 Token 时，用 **Id-Token** 在共享 Redis 上完成服务互信 |
| **混合转发** | Gateway **保留** 用户 Authorization 并 **追加** Id-Token；下游 **先校验用户 Token**，Bearer 无效时 **禁止** 仅用 Id-Token 冒充登录 |

---

## 2. 总体架构

```mermaid
flowchart LR
    U[用户] -->|OAuth| A[Auth :5555]
    A --> R[(Redis)]
    U -->|Bearer| G[Gateway :6060]
    G -->|Auth + Id-Token| C[Center :7777]
    C --> R
```

| 组件 | 职责 |
|------|------|
| **Auth** | 登录、`/oauth2/token` 签发；`OAuth2ResponseHelper` 使 `access_token` 与 Sa-Token JWT 一致 |
| **Gateway** | 路由；`ForwardAuthFilter` 追加内部 Header；**不**统一 `StpUtil.checkLogin()` |
| **业务节点** | `SaOAuthFilterAuthStrategy` 真实认证；需 **`sa-token-oauth2`** + 与 Auth **同一 Redis** |

---

## 3. Token 类型

| 类型 | Header | 用途 | Redis 示例（`token-name=Authorization`） |
|------|--------|------|------------------------------------------|
| 用户 AccessToken | `Authorization: Bearer {jwt}` | 用户 API | `Authorization:oauth2:access-token:{jwt}`、`Authorization:login:token:{jwt}` |
| Id-Token | `Satoken-Id-Token` | 无用户态的服务互信 | Sa-Token Id 模块管理 |
| ClientToken | `Authorization`（可选） | 客户端凭证 | `checkClientToken`；Feign **默认不**自动写入 |
| RefreshToken | 响应体 | 刷新 | `Authorization:oauth2:refresh-token:*` |

> 虽为 JWT 外形，仍依赖 Redis 映射与 OAuth2 记录，**不是**纯无状态 JWT。

---

## 4. 用户访问体系

### 4.1 登录与持票

1. `POST /oauth2/token`（password）或授权码 / `doLogin`（经 Gateway `/oauth2/**` 或直连 Auth）。
2. `LoginHelper.login` → `SaOAuth2Util.generateAccessToken` → `OAuth2ResponseHelper.unifyAccessToken`。
3. 业务请求：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
tenantId: 0
```

### 4.2 Gateway：`ForwardAuthFilter`

| 行为 | 说明 |
|------|------|
| 保留 | 客户端 `Authorization`（不覆盖、不删除） |
| 追加 | `Satoken-Id-Token`、`X-Internal-Service`、`X-Internal-Instance`、`X-Original-Path` |

本地静态路由：`jbm-cluster-platform-gateway/src/main/resources/bootstrap-jaja7.yml`（Center `7777`、Auth `5555`）。

### 4.3 下游：`SaOAuthFilterAuthStrategy`

注册于 `JbmSecurityConfiguration` → `SaServletSuperFilter`（白名单：`@PermitAll`、`permitAll` 配置、`/actuator/**` 等）。

**判定顺序：**

```
提取 Bearer
  → setTokenValue(bearer)
  → StpUtil.checkLogin() 成功 → 通过
  → OAuth2 getAccessToken / checkAccessToken + 绑定用户态 → 通过
  → getLoginIdByToken + LoginHelper 缓存 → 通过
  → 仍有 Bearer 仍失败 → 401（禁止 Id-Token 顶替）
  → 无 Bearer → SaIdUtil.checkCurrentRequestToken() → 通过 / 401
```

**跨节点要点：** Center 必须通过 `jbm-cluster-node-basic` 引入 `sa-token-oauth2`，否则无法读取 Auth 写入的 `Authorization:oauth2:access-token:*`。

**JWT 增强：** `StpLogicJwtForCustom` 在 JWT 解析失败时回退 `SaOAuth2Util.checkAccessToken` / `getLoginIdByAccessToken`。

**用户缓存：** 无 Redis session 时可用 `LoginHelper.setLoginUserCache` 填充请求级 `JbmLoginUser`。

### 4.4 注解与上下文

- `@SaCheckLogin`：依赖过滤器已建立登录态。
- `SaRouteInterceptor`：`LoginHelper.initCache()` / `clearCache()`。
- `HeaderContextFilter`：`user_id`、`username`、`X-Context-*`、`X-Internal-*` → `SecurityContextHolder`。

---

## 5. 内部服务访问体系

### 5.1 适用场景

定时任务、MQ 消费、无 Servlet 用户上下文的 Feign/`feign://` 调用。

### 5.2 调用方

| 路径 | 行为 |
|------|------|
| **FeignRequestInterceptor** | 有入站 `Authorization` 则 **原样透传**；并透传 `user_id`、`username`、`X-Forwarded-For`、`X-Context-*` |
| **AppPreRequestInterceptor** | **无** `Authorization` 时注入 `Satoken-Id-Token` + `X-Internal-*` |
| **JbmFeignRequest** | `feign://service/path` 经 LoadBalancer 解析；`buildRequest` 追加 Id-Token 与内部身份 |
| **Gateway** | 对所有入站请求追加 Gateway 自身 Id-Token（与用户 Bearer 并存） |

### 5.3 被调方

无 Bearer 时：`SaIdUtil.checkCurrentRequestToken()`，并 `recordInternalCaller` 写入 `fromService` / `fromInstance`。

### 5.4 测试与诊断

| 接口 | 服务 | 说明 |
|------|------|------|
| `POST /internal/trust/id-token` | Center | 签发 Id-Token 供互信脚本 |
| `GET /token/diagnose/check?tokenValue=` | Auth | 双层 TTL 诊断 |
| `GET /token/diagnose/config` | Auth | 生效配置 |

自动化：`scripts/run_feign_trust_rest_tests.py`、`scripts/run_user_perm_rest_tests.py`。

### 5.5 与旧文档差异

[`服务间互信认证机制.md`](../../jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-push/docs/服务间互信认证机制.md) 中「Feign 自动把 ClientToken 写入 Authorization」**已变更**；当前默认以 **Id-Token** 为主，ClientToken 仅在显式携带时由 `checkClientToken` 识别。

---

## 6. HTTP Header 约定

| Header | 常量 | 方向 |
|--------|------|------|
| `Authorization` | `AUTHORIZATION_HEADER` | 用户 Bearer |
| `Satoken-Id-Token` | `SaIdUtil.ID_TOKEN` | 内部互信 |
| `X-Internal-Service` | `INTERNAL_SERVICE` | 调用方服务名 |
| `X-Internal-Instance` | `INTERNAL_INSTANCE` | `name:port` |
| `user_id` | `DETAILS_USER_ID` | Feign 可选 |
| `username` | `DETAILS_USERNAME` | Feign 可选 |
| `X-Context-{key}` | `CONTEXT_HEADER_PREFIX` | 业务上下文 |
| `X-Original-Path` | — | Gateway 原始路径 |

---

## 7. 模块与 Maven 依赖

| 模块 | 说明 |
|------|------|
| `jbm-cluster-common-satoken` | 过滤器、JWT、`SaOAuth2AutoConfiguration`（需 classpath 有 oauth2） |
| `jbm-cluster-common-security` | `SaServletSuperFilter` |
| `jbm-cluster-common-fegin` | Feign 拦截器、`HeaderContextFilter` |
| `jbm-cluster-node-basic` | **显式** `sa-token-oauth2`（业务节点校验 OAuth token） |
| `jbm-cluster-platform-auth` | 签发、`TokenDiagnoseController` |
| `jbm-cluster-platform-gateway` | `ForwardAuthFilter`、`SaAuthFilter` |

`common-satoken` 内 `sa-token-oauth2` 为 **optional**，不会自动传递到所有节点。

---

## 8. 本地 jaja7 联调

| 服务 | 端口 | 配置 |
|------|------|------|
| Auth | 5555 | `spring.profiles.active=jaja7` |
| Center | 7777 | 同上 |
| Gateway | 6060 | `jaja7` + `bootstrap-jaja7.yml` |

```bash
set NO_PROXY=*
python scripts/run_user_perm_rest_tests.py --wait 30
python scripts/run_feign_trust_rest_tests.py --wait 20
```

报告：`docs/testing/user-perm-rest-jaja7/`、`docs/testing/feign-trust-jaja7/`。

---

## 9. 常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| Auth userinfo 200，`/current/user` 401 | Center 无 oauth2 或 Redis 不一致 | 确认 `node-basic` 依赖、重启 Center |
| 带 Bearer 仍 401 | 旧过滤器 Id-Token 优先 | 使用当前 `SaOAuthFilterAuthStrategy` |
| Feign 401 | 未带 Id-Token 或 Redis 无记录 | `check-id-token`、调用方 Header |
| Python 502 | 系统代理 | `NO_PROXY=*` |

---

## 10. 源码索引

| 文件 | 包路径 |
|------|--------|
| `ForwardAuthFilter.java` | `...platform.gateway.filter` |
| `SaAuthFilter.java` | `...platform.gateway.filter` |
| `SaOAuthFilterAuthStrategy.java` | `...satoken.core.filter` |
| `JbmSecurityConfiguration.java` | `...security.configuration` |
| `StpLogicJwtForCustom.java` | `...satoken.core` |
| `OAuth2ResponseHelper.java` | `...satoken.oauth` |
| `FeignRequestInterceptor.java` | `...common.feign` |
| `AppPreRequestInterceptor.java` | `...common.feign` |
| `JbmFeignRequest.java` | `...feign.request` |
| `HeaderContextFilter.java` | `...common.feign` |
| `InternalTrustTokenController.java` | `...center.controller` |
| `sa-token.properties` | `common-satoken/resources/configs` |

---

## 11. 相关文档

| 文档 | 内容 |
|------|------|
| **本文** | 用户 + 内部双通道、Header、过滤器、联调 |
| [token-auth-full-chain.md](../../jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth/docs/token-auth-full-chain.md) | 登录细节、Redis 大全、续签 |
| [服务间互信认证机制.md](../../jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-push/docs/服务间互信认证机制.md) | Push 场景（部分过时） |
