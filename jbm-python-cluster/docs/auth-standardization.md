# JBM Python Auth：OIDC 与会话升级

本次交付为 P0 会话基础与 P1 的首个可测试 OIDC profile。默认保留旧服务行为，新 OIDC 必须显式启用。没有进行生产部署，也没有取得 OpenID 认证。

## 已实现的边界

- 旧 `/oauth2/*` 保持 issuer、令牌格式及 JBM UserInfo 包装；修复刷新竞争、刷新重放、退出/踢人后仍能刷新、会话数量限制和身份状态校验。
- 新 `/oidc/*` 是独立 issuer，支持授权码 + S256 PKCE、ID Token、顶层 UserInfo、SSO、按 scope 同意、离线刷新与重放撤销、Access/Refresh Token 内省和 RP 发起退出。
- 新 profile 只提供 `openid profile email offline_access`；只使用本地密码认证。新 profile 不提供机器授权、SAML、LDAP、MFA、动态客户端注册、claims/request/request_uri 参数、其他 response_mode 或完整后通道退出。
- 新 profile 的 JWT Access Token 使用 `typ=at+jwt` 和资源 audience；ID Token 使用客户端 audience。旧 issuer、ID Token、其他 audience 的令牌不能替代该 Access Token。
- 新 profile 严格要求有效用户、账号、组织成员、租户应用开通及该应用的角色关联，没有 `admin` 用户名绕过。
- subject 为数据库保存的随机 public subject，不包含个人信息，在客户端之间保持一致。它不是 pairwise subject；`base_user.user_id` 必须永久唯一，不能删除后回收给另一个人。迁移降级保留已签发身份映射。
- 旧在线用户列表/踢人界面暂不枚举或终止新 OIDC SSO；新 profile 的退出通过 `/oidc/logout`。不能据此宣称现有后台已具备跨协议统一会话管理。

## 配置与数据库

生产先通过 Center 的现有 Alembic 链执行新迁移 `20260922_23`，增加 `base_auth_subject`。在原有发布环境、数据库配置和工作目录中执行：

```sh
alembic -c center/alembic.ini upgrade head
```

不要对未核对的环境执行迁移。Auth 在 OIDC 启用时检查映射表存在；生产不由 Auth 自动建表。SQLite 仅供开发/测试，依照现有模式建表。

Nacos 中的 `jbm.auth` 示例（真实密钥通过既有密钥配置流程注入）：

```yaml
jbm:
  auth:
    require-pkce: true
    require-https-redirects: true
    legacy-password-grant-enabled: false
    allow-plaintext-secrets: false
    dev-bypass-enabled: false
    fixed-captcha-code: ""
    jwt:
      issuer: https://auth.example.com
      audience: jbm-api
      key-id: auth-key-2026-09
      private-key: "<PEM private key supplied securely>"
      verification-keys: {}
    oidc:
      enabled: true
      issuer: https://auth.example.com/oidc
      cookie-secure: true
      access-token-seconds: 600
      refresh-token-seconds: 604800
      sso-session-seconds: 28800
```

新 issuer 固定以 `/oidc` 结尾，配置必须为 HTTPS 且没有用户信息、query 或 fragment。反向代理按原路径转发，不能基于任意请求 Host 推导 issuer。浏览器页面使用 host-only、Secure、HttpOnly、SameSite=Lax Cookie，并绑定 CSRF 和短期服务端事务。

`dev/test/local/default` 可使用内存状态和临时签名密钥；其他 profile（含 staging）启动时强制持久化签名密钥、共享 Redis 和生产安全配置。开发环境也可设 `require-redis: true`。运行中 Redis 故障不降级为独立内存状态。

在 `base_app.extend_data` 中增加 `oidc` 配置。不要覆盖既有 oauth、注册及其他业务配置：

```json
{
  "oidc": {
    "enabled": true,
    "redirectUris": ["https://portal.example.com/login/callback"],
    "postLogoutRedirectUris": ["https://portal.example.com/logged-out"],
    "scopes": ["openid", "profile", "email", "offline_access"],
    "audiences": ["https://api.example.com"],
    "tokenEndpointAuthMethod": "none",
    "grantTypes": ["authorization_code", "refresh_token"],
    "trusted": false
  }
}
```

`api_key` 是客户端 ID。公开客户端使用 `none` 并强制 PKCE；有后端保存凭证的客户端可注册 `client_secret_basic` 或 `client_secret_post`，凭证沿用已有安全存储。配置必须显式 opt-in；不继承旧应用的宽松回跳规则。首版 HTTPS 回调精确匹配，不支持原生应用的自定义 scheme 或 HTTP loopback 例外。

`trusted: true` 只允许跳过普通范围的首次同意，离线访问仍需要用户同意；不要把外部应用配置为 trusted。没有 `offline_access` 的请求不取得 Refresh Token。

## 标准端点

| 路径 | 方法 / 说明 |
| --- | --- |
| `/oidc/.well-known/openid-configuration` | GET；只声明已实现的 profile |
| `/oidc/authorize` | GET/POST；授权码，S256，prompt none/login/consent，max_age |
| `/oidc/token` | POST form-urlencoded；授权码交换与刷新 |
| `/oidc/userinfo` | GET/POST；Authorization Bearer；顶层 claims，按 scope 输出 |
| `/oidc/jwks` | GET；当前与 verify-only 公钥 |
| `/oidc/revoke` | POST；客户端只能撤销自己的授权令牌 |
| `/oidc/introspect` | POST；只向所属客户端返回有效 Access/Refresh Token 状态 |
| `/oidc/logout` | GET 展示确认；POST 校验浏览器事务后结束 SSO |

post_logout_redirect_uri 必须注册并配合属于当前会话的有效签名 ID Token hint。结束 SSO 后关联 Access/Refresh Token 在线校验失败；没有向外部 RP 发送后通道登出通知，各 RP 自己的 Cookie 仍需由其退出流程处理。

新客户端必须校验 ID Token 的签名、issuer、audience、有效期及请求中的 nonce，并核对 UserInfo 的 sub。发现文档不是通过认证的证明。

当前资源服务器不能仅修改旧 UserInfo URL 来使用新 profile。现有 common 客户端读取 JBM 包装结构；IoT 的应用归属、权限、租户/项目及委托检查仍由原路径执行。P2 将增加统一 Principal、标准验证适配和分批客户端迁移。

## 会话、撤销和密钥

旧用户会话以刷新令牌族为单位限制数量；退出和管理员踢人会终止相关刷新能力。单独撤销 Access Token 保持单令牌语义。刷新之前先验证客户端，再原子消费并发布重放标记；并发重放可以使获胜方刚得到的令牌也失效，这是检测泄漏后的保护行为，客户端不能无限重试同一刷新令牌。

新建会话绑定账号密码和更新时间的版本，后续访问、刷新和授权码兑换重新核对。旧协议的 UserInfo 和权限判断重新读取当前角色，撤销 root 角色后不继续信任旧 JWT 内的 root 标记；既有旧协议 `admin` 用户名兼容规则仍保留，新 OIDC 不采用此规则。

新旧 profile 使用不同缓存命名空间。标准 SSO 的浏览器认证复用期限由 sso-session-seconds 控制；服务端保留相关身份状态以支持批准的离线刷新，但保留时间有限，不能假设刷新会无限延续。

旧版本已经发出的会话没有新增账号版本字段或索引；需重新登录/刷新后逐步进入新机制。高要求环境在升级窗口主动终止旧会话，不能声称升级前所有令牌都已有新增保护。

使用 PyJWT 进行 RS256 编解码和声明验证。新公钥使用新 kid；旧公钥放在 `jwt.verification-keys` 的 `kid: PEM-public-key` 映射中，在最长相关 token 有效期结束后移除。签名私钥不出现在 JWKS。发生私钥泄漏时立即移除受影响 key 并撤销授权，而非等待正常轮换周期。

滚动轮换先让所有实例信任新旧公钥，再逐步切新签名 key；不能直接让一半实例只认识旧 key、另一半只认识新 key。两个 issuer 在此版本共享受控密钥环，但验证的 issuer/audience/token 类型分别限定。

## 发布和回滚

1. 先发布 Center 迁移、安装新增依赖；普通 Dockerfile 与增量 Dockerfile.release 都需包含新依赖。
2. 发布 Auth，保持 oidc.enabled=false；检查旧登录、刷新、退出和 readiness。
3. 登记测试客户端与真实 HTTPS 地址，启用独立标准 issuer，验证两个客户端和敏感负面用例。
4. 在完成 P2 前，不切换当前 IoT、楼宇及 JBM Admin 的登录入口。客户端接入需要独立的 issuer/profile 配置，验证失败不能自动降级到旧协议。
5. 灰度回滚优先恢复经测试的上一兼容版本；若停用 OIDC，要明确终止新会话及其依赖应用。保留身份映射和旧签名公钥。撤销和刷新均依赖共享会话存储，不回滚或恢复陈旧 Redis 快照来“恢复登录”。

## 验证方式

在 `jbm-python-cluster` 目录中按锁文件准备开发环境，执行：

```powershell
uv sync --extra dev
$env:JBM_AUTH_TEST_REDIS_URL = 'redis://127.0.0.1:<isolated-port>/15'
uv run --extra dev pytest auth/tests common/tests -q
```

Redis 必须是测试允许访问的实例；测试使用随机 key 前缀并只清理自己创建的 key，不清空数据库。未设置测试 Redis URL 时相应用例跳过，不能将该运行描述成已验证多实例行为。

`test_oidc_protocol.py` 使用两个独立 Authlib RP 测试端点互操作与签名/nonce 校验；`test_session_lifecycle.py` 验证并发刷新、撤销、重放、锁失效和真实 Redis；`test_oidc_repository.py` 验证数据库身份映射和业务资格；`test_jwt_keys.py` 验证密钥轮换及令牌混淆拒绝。

真实 Chromium 的跨域登录、同意、第二客户端 SSO、Cookie 隔离和退出验证：

```powershell
uv sync --extra dev --extra browser
uv run --extra dev --extra browser playwright install chromium
$env:JBM_AUTH_BROWSER_TEST = '1'
uv run --extra dev --extra browser pytest auth/tests/test_oidc_browser.py -q
```

如使用已有 Chromium，可用 `JBM_AUTH_CHROMIUM_EXECUTABLE` 指定浏览器可执行文件。测试仅在本地临时 HTTPS 服务使用自签名证书并关闭该测试浏览器上下文的证书校验，不修改生产 TLS 配置。

正式启用前仍需针对部署地址、代理配置、目标客户端运行相应 OpenID 一致性套件和容量测试。此次内部测试结果不替代完整协议认证、安全审计或生产容量结论。

2026-09-22 本地验证：`auth/tests` 与 `common/tests` 合计 **174 passed**，启用了真实 Redis 和 Chromium 测试，无跳过；IoT `tests/test_jbm_runtime.py` **11 passed**。Alembic 唯一 head 为 `20260922_23`。新增文件与本次重构的 JWT/启动代码通过完整 Ruff 检查，认证目录通过 E9/F 检查。现有依赖仍有弃用警告；没有构建或发布生产镜像、执行生产迁移或运行外部一致性套件。

## 后续阶段

P2：标准资源服务适配、统一会话管理、前端/BFF 迁移、后通道退出和打包升级。
P3：TOTP、恢复码、WebAuthn、分阶段认证及敏感操作重新认证。
P4：外部身份源、客户端/策略/因素管理界面。

这些阶段尚未实现，不能在当前产品或 discovery 中宣称支持。
