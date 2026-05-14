# JBM Token 认证全链路文档

## 一、技术架构概览

### 1.1 技术选型

| 组件 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 认证框架 | Sa-Token | 1.32.0 | 统一认证、鉴权、会话管理 |
| JWT 模式 | StpLogicJwtForSimple (自定义扩展) | - | 自定义实现 `StpLogicJwtForCustom` |
| OAuth2 | Sa-Token-OAuth2 | 1.32.0 | 标准 OAuth2.0 协议支持 |
| Token 存储 | Redis (RedisSaTokenDao) | - | 自定义 DAO，替代默认内存存储 |
| 在线用户 | Redis (online_tokens:{token}) | - | 自维护的在线用户表 |
| Token 传递 | Feign Interceptor | - | 服务间自动透传 Authorization / Id-Token |

### 1.2 JWT 模式说明

项目使用 **Simple 简单模式** (自定义 `StpLogicJwtForCustom`):

- Token 风格: JWT
- 登录数据存储: **Redis 中** (非 JWT 内)
- Session 存储: **Redis 中**
- 支持踢人下线、timeout 有效期、activity-timeout 有效期、账号封禁
- JWT 中仅携带 loginId 和过期时间，验证时需回查 Redis

> 注意: 虽然使用了 JWT 格式，但本质是 **JWT + Redis 双验证**，Redis 数据过期则 JWT 也无效。

### 1.3 支持的登录方式

| 登录类型 | 枚举值 | 说明 |
|---|---|---|
| 密码登录 | `PASSWORD` | 账号+密码，支持验证码 |
| 短信登录 | `SMS` | 手机号+短信验证码 |
| 扫码登录 | `SCAN` | 二维码扫码 |
| 人脸登录 | `FACE` | 人脸识别 |
| 第三方登录 | `THIRD_PARTY` | 第三方平台授权 |
| 微信登录 | `WECHAT` | 微信授权 |
| 小程序登录 | `MINIAPP` | 微信小程序 |

### 1.4 支持的设备类型

| 设备类型 | 枚举值 | device 值 |
|---|---|---|
| PC 端 | `PC` | `"pc"` |
| APP 端 | `APP` | `"app"` |
| 小程序端 | `XCX` | `"xcx"` |

---

## 二、Token 配置详解

### 2.1 配置加载优先级

```
yml / 环境变量配置  (最高优先级)
    ↓ 覆盖
sa-token.properties  (公共模块默认值)
    ↓ 被覆盖
Sa-Token 框架内置默认值  (最低优先级)
```

### 2.2 配置文件位置

| 文件 | 作用范围 | 说明 |
|---|---|---|
| `jbm-cluster-common-satoken/.../sa-token.properties` | **所有服务** | 通过 `SaTokenEnvProcessor` 加载 |
| `jbm-cluster-platform-gateway/bootstrap.yml` | **仅 Gateway** | Gateway 独立配置，会覆盖 properties |

### 2.3 当前配置值

配置来源: `sa-token.properties` (公共默认值)

| 配置项 | 值 | 换算 | 说明 |
|---|---|---|---|
| `sa-token.token-name` | `Authorization` | - | Header 名称 |
| `sa-token.timeout` | `86400` | 24 小时 | Token 总有效期，到期必过期 |
| `sa-token.activity-timeout` | `7200` | 2 小时 | 无操作过期，有操作续签 |
| `sa-token.is-concurrent` | `false` | - | 不允许并发登录，新登录挤掉旧登录 |
| `sa-token.is-share` | `false` | - | 每次登录新建 token |
| `sa-token.is-read-head` | `true` | - | 从 Header 读取 token |
| `sa-token.is-read-cookie` | `false` | - | 不从 Cookie 读取 |
| `sa-token.token-prefix` | `Bearer` | - | Token 前缀 |
| `sa-token.jwt-secret-key` | `abcdefghijklmnopqrstuvwxyz` | - | JWT 签名密钥 |
| `sa-token.check-id-token` | `true` | - | 开启内网服务调用鉴权 |
| `sa-token.id-token-timeout` | `604800` | 7 天 | Id-Token 有效期 (服务间互信) |
| `sa-token.oauth2.access-token-timeout` | `86400` | 24 小时 | OAuth2 访问令牌有效期 |
| `sa-token.oauth2.client-token-timeout` | `86400` | 24 小时 | OAuth2 客户端令牌有效期 |
| `sa-token.oauth2.is-code` | `true` | - | 启用授权码模式 |
| `sa-token.oauth2.is-password` | `true` | - | 启用密码模式 |
| `sa-token.oauth2.is-client` | `true` | - | 启用客户端模式 |
| `sa-token.oauth2.is-implicit` | `false` | - | 禁用隐式模式 |

> **注意**: Gateway 的 `bootstrap.yml` 中 `sa-token.is-concurrent=true`，与公共默认值 `false` 不一致。

### 2.4 auto-renew 续签机制

当前配置中 **未显式配置** `sa-token.auto-renew`，Sa-Token 默认 `auto-renew=true`。

在 `StpLogicJwtForCustom.getLoginId()` 中:
```java
if (getConfig().getAutoRenew()) {
    updateLastActivityToNow(tokenValue);  // 自动续签 activity-timeout
}
```

**仅续签 Sa-Token 层的 `last-activity` 时间，不影响 OAuth2 层的 TTL。**

---

## 三、登录全链路

### 3.1 路由入口

所有请求先经过 Gateway，Gateway 路由到 Auth 服务。

```
客户端 → Gateway(6060) → Auth 服务(5555) → OAuth2 处理
```

Gateway 白名单 (`bootstrap.yml` / `IgnoreWhiteProperties`):
- `/*/login/**`, `/*/logout/**`, `/*/oauth/**` — 放行登录相关
- `/actuator/health`, `/actuator/info` — 放行监控
- `/*/authority/granted/me` 等 — 放行权限查询

### 3.2 登录流程 (OAuth2 密码模式)

这是最常用的登录方式，前端发起 OAuth2 密码模式授权。

```
┌────────┐      ┌──────────┐      ┌───────────────────────────┐
│ 前端    │      │ Gateway  │      │ Auth 服务                 │
└───┬────┘      └────┬─────┘      └──────────┬────────────────┘
    │                │                        │
    │  POST /auth/oauth2/token               │
    │  {username, password, grant_type,      │
    │   client_id, response_type, scope}     │
    │ ────────────────>│────────────────────>│
    │                │                        │
    │                │   [SaOAuth2Handle.serverRequest()]
    │                │                        │
    │                │         ┌──────────────┤
    │                │         │ 1. preLogin  │ 解析 clientId, loginType
    │                │         │    提取设备信息│
    │                │         ├──────────────┤
    │                │         │ 2. preCheck  │ 校验验证码(vcode)
    │                │         │    校验客户端 │ SaOAuth2Util.checkClientModel
    │                │         ├──────────────┤
    │                │         │ 3. doDecrypt │ RSA 解密密码
    │                │         │   Password   │ (demo客户端跳过)
    │                │         ├──────────────┤
    │                │         │ 4. doCheck   │ 校验用户身份
    │                │         │   (login)    │ → ILoginAuthenticate.login()
    │                │         │             │ → baseUserServiceClient.userLogin()
    │                │         ├──────────────┤
    │                │         │ 5. LoginHelper.login()  ← Sa-Token 登录
    │                │         │   StpUtil.login(loginId, device)
    │                │         │   Redis 写入: satoken:login:token:{token}
    │                │         │   Redis 写入: satoken:login:session:{token}
    │                │         │   Redis 写入: satoken:login:last-activity:{token}
    │                │         ├──────────────┤
    │                │         │ 6. SaOAuth2Util.generateCode()
    │                │         │   生成授权码 code (存 Redis)
    │                │         ├──────────────┤
    │                │         │ 7. SaOAuth2Util.generateAccessToken(code)
    │                │         │   Redis 写入: satoken:oauth2:access-token:{token}
    │                │         │   Redis 写入: satoken:oauth2:refresh-token:{refreshToken}
    │                │         ├──────────────┤
    │                │         │ 8. UserActionListener.doLogin()
    │                │         │   Redis 写入: online_tokens:{token}
    │                │         │   记录登录日志
    │                │         └──────────────┘
    │                │                        │
    │  {access_token, refresh_token,          │
    │   expires_in, token_type, scope}        │
    │ <─────────────────────────────────────── │
    │                │                        │
```

### 3.3 登录流程 (直接 doLogin)

适用于前端直接调用 Auth 服务的简化登录接口。

```
POST /auth/oauth2/doLogin
{username, password, client_id, response_type, redirect_uri, scope, loginType}
```

流程:
1. `SysLoginService.login(username, password, LoginType.PASSWORD)` — 通过 Feign 调用用户服务校验
2. 获取 `BaseApp` 信息 (通过 clientId)
3. 设置 `JbmLoginUser.appId`, `clientId`, `device`
4. `LoginHelper.login(jbmLoginUser)` — Sa-Token 登录
5. `SaOAuth2Util.generateCode(ra)` — 生成授权码
6. 返回回调 URL (含 code)

### 3.4 登录流程 (第三方登录)

```
GET /auth/oauth2/thirdparty/{provider}/callback?code=xxx&client_id=xxx&redirect_uri=xxx
```

流程:
1. `ThirdPartyAuthService.getUserInfoByCode()` — 用 code 换取第三方用户信息
2. `SysLoginService.thirdPartyLogin()` — 映射/注册系统用户
3. 设置 `JbmLoginUser` 信息
4. `LoginHelper.login(myUser)` — Sa-Token 登录
5. `SaOAuth2Util.generateAccessToken(requestAuthModel, true)` — 直接生成 Access Token (跳过 code)
6. 返回 `AccessTokenModel` (JSON) 或重定向

### 3.5 核心: LoginHelper.login() 做了什么

```java
// LoginHelper.java
public static void loginByDevice(JbmLoginUser loginUser, String device) {
    LOGIN_CACHE.set(loginUser);                    // 1. 写入 ThreadLocal 一级缓存
    StpUtil.login(loginUser.getLoginId(), device); // 2. Sa-Token 登录 (核心)
    loginUser.setToken(StpUtil.getTokenValue());   // 3. 获取生成的 token
    setLoginUser(loginUser);                       // 4. 写入 TokenSession
}

public static void setLoginUser(JbmLoginUser loginUser) {
    StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser); // Redis: satoken:login:session:{token}
}
```

`StpUtil.login(loginId, device)` 内部 (Sa-Token):
1. 创建 JWT (Simple 模式，payload 中写入 loginId + 创建时间 + 过期时间)
2. Redis SET `satoken:login:token:{tokenValue}` → `loginId` , TTL = `sa-token.timeout`
3. Redis SET `satoken:login:session:{tokenValue}` → Session 对象, TTL = `sa-token.timeout`
4. Redis SET `satoken:login:last-activity:{tokenValue}` → 当前时间戳, TTL = `sa-token.activity-timeout`
5. 触发 `SaTokenListener.doLogin()` 事件

### 3.6 核心: SaOAuth2Util.generateAccessToken() 做了什么

`JbmNodeOAuth2TemplateImpl.randomAccessToken()` 被重写:
```java
public String randomAccessToken(String clientId, Object loginId, String scope) {
    String tmp = StpUtil.getTokenValueByLoginId(loginId);
    if (StrUtil.isNotEmpty(tmp)) {
        token = tmp;  // 直接复用 Sa-Token 的 token
    } else {
        token = StpUtil.createLoginSession(loginId);  // 没有则创建
    }
    return token;
}
```

**关键**: OAuth2 的 access_token 和 Sa-Token 的 token 是同一个值。

但 OAuth2 框架在 `generateAccessToken` 时还会额外写入:
- Redis SET `satoken:oauth2:access-token:{tokenValue}` → `AccessTokenModel`, TTL = `sa-token.oauth2.access-token-timeout`
- Redis SET `satoken:oauth2:refresh-token:{refreshToken}` → `AccessTokenModel`, TTL = OAuth2 refresh timeout

### 3.7 核心: UserActionListener.doLogin() 做了什么

```java
public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
    // 构建在线用户对象
    SysUserOnline userOnline = new SysUserOnline();
    userOnline.setIpaddr(ip);
    userOnline.setLoginTime(DateTime.now());
    userOnline.setTokenId(tokenValue);
    userOnline.setExpiredTime(DateUtil.offsetSecond(loginTime, loginModel.getTimeout()));
    userOnline.setUserName(user.getUsername());

    // 写入 Redis，TTL = sa-token.timeout
    redisService.setCacheObject("online_tokens:" + tokenValue, userOnline,
                                tokenConfig.getTimeout(), TimeUnit.SECONDS);
}
```

---

## 四、Redis Key 一览

### 4.1 Sa-Token 层

| Redis Key | 值 | TTL | 说明 |
|---|---|---|---|
| `satoken:login:token:{tokenValue}` | `loginId` | `sa-token.timeout` (24h) | token → loginId 映射 |
| `satoken:login:session:{tokenValue}` | `Session` (含 JbmLoginUser) | `sa-token.timeout` (24h) | Token 会话数据 |
| `satoken:login:last-activity:{tokenValue}` | `时间戳` | `sa-token.activity-timeout` (2h) | 最后活动时间，用于活动超时判断 |
| `satoken:login:token-session:{loginId}` | `tokenValue` | `sa-token.timeout` (24h) | loginId → token 映射 (支持 id 反查) |

### 4.2 OAuth2 层

| Redis Key | 值 | TTL | 说明 |
|---|---|---|---|
| `satoken:oauth2:access-token:{tokenValue}` | `AccessTokenModel` | `sa-token.oauth2.access-token-timeout` (24h) | OAuth2 访问令牌 |
| `satoken:oauth2:refresh-token:{refreshToken}` | `AccessTokenModel` | OAuth2 refresh timeout | OAuth2 刷新令牌 |
| `satoken:oauth2:code:{code}` | `CodeModel` | 短期 (通常5分钟) | OAuth2 授权码 |

### 4.3 在线用户表

| Redis Key | 值 | TTL | 说明 |
|---|---|---|---|
| `online_tokens:{tokenValue}` | `SysUserOnline` | `sa-token.timeout` (24h) | 在线用户信息 (IP、浏览器、OS等) |

### 4.4 服务间互信 (Id-Token)

| Redis Key | 值 | TTL | 说明 |
|---|---|---|---|
| `satoken:id-token:{serviceId}` | `idTokenValue` | `sa-token.id-token-timeout` (7天) | 服务身份标识 |

### 4.5 登录错误锁定

| Redis Key | 值 | TTL | 说明 |
|---|---|---|---|
| `login_error:{username}` | `错误次数` | `LOGIN_ERROR_LIMIT_TIME` | 登录失败锁定 |

---

## 五、请求认证链路

### 5.1 Gateway 层认证

**SaAuthFilter** (`jbm-cluster-platform-gateway`):

```
请求进入 Gateway
    │
    ├─ 匹配白名单 (/*/login/**, /*/oauth/**, /actuator/** 等)
    │   └── 直接放行
    │
    └─ SaReactorFilter.setAuth()
        │
        └── StpUtil.checkLogin()   ← ⚠️ 当前已被注释掉!
            │
            └── 不执行登录校验，所有请求直接透传到下游
```

> **重要**: Gateway 的 `StpUtil.checkLogin()` 被注释掉了 (SaAuthFilter.java 第51行)，
> 当前 Gateway 不做强制登录校验，仅做路由转发。认证完全依赖下游服务。

### 5.2 下游服务认证 (Servlet 服务)

**JbmSecurityConfiguration** 注册的 `SaServletSuperFilter`:

```
请求进入下游服务
    │
    ├─ 匹配白名单 (permitAll 注解、actuator、v2/api-docs 等)
    │   └── 直接放行
    │
    └─ SaOAuthFilterAuthStrategy.run()   ← 核心认证逻辑
        │
        ├─ Step 1: 本地 IP (127.x.x.x / ::1) → 直接放行
        │
        ├─ Step 2: StpUtil.getTokenValue() 获取 token
        │   └─ token 为空 → 抛出 "无效Token"
        │
        ├─ Step 3: StpUtil.getTokenInfo() 检查 Sa-Token 状态
        │   ├─ isLogin=true && tokenTimeout>0 → 放行 ✓
        │   ├─ isLogin=true && tokenTimeout<=0 → 抛出 "Token已失效"
        │   └─ isLogin=false:
        │       ├─ 有 Satoken-Id-Token header → SaIdUtil.checkCurrentRequestToken() → 放行 ✓
        │       └─ 无 Id-Token → 继续 Step 4
        │
        └─ Step 4: OAuth2 校验
            ├─ SaOAuth2Util.getAccessToken(tokenValue) 找到
            │   └─ SaOAuth2Util.checkAccessToken() → 放行 ✓
            ├─ SaOAuth2Util.getClientToken(tokenValue) 找到 (ClientToken)
            │   └─ 放行 ✓
            └─ 都找不到 → 抛出 "无效的访问客户端"
```

### 5.3 SaTokenInfo.tokenTimeout 的计算

`StpLogicJwtForCustom.getLoginId()` 是核心校验方法:

```java
public Object getLoginId() {
    String tokenValue = getTokenValue();
    if (tokenValue == null) throw NOT_TOKEN;

    String loginId = getLoginIdNotHandle(tokenValue);  // 解析 JWT
    if (loginId == null) throw INVALID_TOKEN;

    // JWT 中标记为已过期 → 总有效期到期
    if (loginId.equals(NotLoginException.TOKEN_TIMEOUT))
        throw NotLoginException.newInstance(TOKEN_TIMEOUT);

    // 被顶替下线
    if (loginId.equals(NotLoginException.BE_REPLACED))
        throw NotLoginException.newInstance(BE_REPLACED);

    // 被踢下线
    if (loginId.equals(NotLoginException.KICK_OUT))
        throw NotLoginException.newInstance(KICK_OUT);

    // 检查活动超时 (读取 Redis: satoken:login:last-activity:{token})
    checkActivityTimeout(tokenValue);

    // 自动续签 (更新 last-activity 时间)
    if (getConfig().getAutoRenew()) {
        updateLastActivityToNow(tokenValue);
    }

    return loginId;
}
```

`tokenTimeout` 的值由 `StpUtil.getTokenInfo()` 计算:
- **总有效期剩余** = `satoken:login:token:{token}` 的 Redis TTL
- **活动超时剩余** = `satoken:login:last-activity:{token}` 的 Redis TTL
- `SaTokenInfo.tokenTimeout` = min(总有效期剩余, 活动超时剩余)

---

## 六、服务间调用 (Feign)

### 6.1 请求拦截器

**AppPreRequestInterceptor** (`jbm-cluster-common-fegin`):

```
Feign 调用发起
    │
    ├─ 当前请求有 Authorization header
    │   └── 直接透传用户的 token
    │
    └─ 当前请求无 Authorization header (服务间调用)
        ├── 生成 ClientToken: saOAuth2Template.generateClientToken(serviceName, "*")
        ├── 生成 Id-Token: SaIdUtil.getToken()
        └── 设置 header:
            ├── Authorization: Bearer {clientToken}
            └── Satoken-Id-Token: {idToken}
```

### 6.2 ClientToken 生成

`JbmNodeOAuth2TemplateImpl`:
- 使用 **Caffeine 本地缓存**，过期时间 = `tokenConfig.getClientTokenCacheHours()` (默认24h)
- 首次生成时调用 `SaOAuth2Template.generateClientToken()` 写入 Redis
- 后续从本地缓存获取，不重复生成

### 6.3 Id-Token

- `SaIdUtil.getToken()` 获取当前服务的 Id-Token
- Id-Token 存储在 Redis: `satoken:id-token:{serviceId}`，TTL = `sa-token.id-token-timeout` (7天)
- 服务启动时自动生成

---

## 七、登出链路

### 7.1 用户主动登出

```
DELETE /auth/oauth2/logout
    │
    ├─ LoginHelper.getLoginUser()  → 获取当前登录用户
    ├─ sysLoginService.logout(null)
    │   └─ LoginHelper.loginout()
    │       ├─ SaOAuth2Util.getLoginIdByAccessToken(tokenValue)
    │       ├─ StpUtil.logout(loginId)         → 清除 Sa-Token 层所有数据
    │       └─ LoginHelper.clearCache()        → 清除 ThreadLocal
    │
    └─ UserActionListener.doLogout()  → 清除 online_tokens:{token}
```

`StpUtil.logout(loginId)` 清除:
- `satoken:login:token:{tokenValue}`
- `satoken:login:session:{tokenValue}`
- `satoken:login:last-activity:{tokenValue}`
- `satoken:login:token-session:{loginId}`

### 7.2 管理员踢出

```
DELETE /auth/online/kickout/{tokenId}
    │
    ├─ SaOAuth2Util.revokeAccessToken(tokenId)  → 清除 OAuth2 access-token
    └─ StpUtil.kickoutByTokenValue(tokenId)     → 踢出 Sa-Token 会话
```

### 7.3 并发登录互踢 (is-concurrent=false)

当 `sa-token.is-concurrent=false` 时，新登录会踢掉旧登录:
- `StpUtil.login()` 内部检测到该 loginId 已有其他 token
- 调用 `StpUtil.kickout(oldLoginId)`
- 触发 `UserActionListener.doReplaced()` → 清除旧 `online_tokens`

---

## 八、Token 续签机制

### 8.1 自动续签

Sa-Token 默认 `auto-renew=true`，每次调用 `getLoginId()` 时:
1. 检查 `satoken:login:last-activity:{token}` 是否过期
2. 如果未过期，更新 `last-activity` 为当前时间
3. Redis TTL 重新设为 `sa-token.activity-timeout`

### 8.2 手动续签

```
DELETE /auth/online/refresh
    └─ StpUtil.updateLastActivityToNow()

POST /auth/oauth2/renewal
    ├─ StpUtil.isLogin() → 已登录 → StpUtil.updateLastActivityToNow()
    └─ 未登录 → 通过 access_token 参数查找并续签
```

### 8.3 OAuth2 Token 刷新

```
/oauth2/refresh (Sa-Token 标准 OAuth2 接口)
    │
    └─ JbmNodeOAuth2TemplateImpl.refreshAccessToken()
        ├─ super.refreshAccessToken()  → 验证 refreshToken
        ├─ StpUtil.logoutByTokenValue(oldToken)  → 踢掉旧 token
        ├─ LoginHelper.login(loginUser)  → 重新登录 (生成新 token)
        └─ accessTokenModel.accessToken = newToken
```

---

## 九、已知问题与风险点

### 9.1 双层 TTL 不一致问题 (核心问题)

OAuth2 和 Sa-Token 使用同一个 token 值，但各自维护独立的 Redis key:

| 层 | Redis Key | TTL 配置 | 续签 |
|---|---|---|---|
| Sa-Token | `satoken:login:token:{token}` | `sa-token.timeout` (24h) | 不续签 (固定) |
| Sa-Token | `satoken:login:last-activity:{token}` | `sa-token.activity-timeout` (2h) | 自动续签 |
| OAuth2 | `satoken:oauth2:access-token:{token}` | `sa-token.oauth2.access-token-timeout` (24h) | **不续签** |

**问题场景**:
1. 如果 `sa-token.timeout` != `sa-token.oauth2.access-token-timeout`，较短的一方先过期
2. Sa-Token 层续签了 `activity-timeout`，但 OAuth2 层的 `access-token` 不会跟随续签
3. 长时间在线用户: `last-activity` 一直续签，但 `oauth2:access-token` 按原 TTL 到期

### 9.2 Gateway 认证被旁路

Gateway 的 `SaAuthFilter` 中 `StpUtil.checkLogin()` 被注释，Gateway 不校验 token。
所有认证依赖下游服务的 `SaOAuthFilterAuthStrategy`。

### 9.3 is-concurrent 配置不一致

- 公共 `sa-token.properties`: `is-concurrent=false` (互踢)
- Gateway `bootstrap.yml`: `is-concurrent=true` (允许并发)

### 9.4 online_tokens 与 Sa-Token 不同步

`online_tokens:{token}` 的 TTL 由 `UserActionListener.doLogin()` 设置为 `sa-token.timeout`，
但 Sa-Token 层的 `activity-timeout` 过期时，`online_tokens` 不会同步删除。
导致在线用户列表可能显示已实际过期的用户。

### 9.5 JWT 密钥硬编码

`sa-token.jwt-secret-key=abcdefghijklmnopqrstuvwxyz` 是弱密钥，生产环境需更换。

---

## 十、诊断接口

部署 `TokenDiagnoseController` 后可使用:

| 接口 | 方法 | 说明 |
|---|---|---|
| `/token/diagnose/config` | GET | 查看当前生效的 Sa-Token 配置 |
| `/token/diagnose/check?tokenValue=xxx` | GET | 检查指定 token 的双层 TTL 状态 |
| `/token/diagnose/scan?prefix=satoken:&limit=100` | GET | 扫描 Redis 中所有 token key |
