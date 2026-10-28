# Web、小程序、APP 会话与续期

同一用户在同一应用下可同时保留 Web、小程序、APP 三个会话。每种终端最多一个会话，新登录撤销同端旧会话的整个刷新凭证族，旧 Access Token 和 Refresh Token 都不能继续使用。Android/iOS 统一算 APP。自动刷新延续原凭证族，不新增会话；刷新与新登录竞争时，已被撤销的凭证族不能顶掉新会话。退出、踢出、改密和禁用继续使凭证失效。

终端由服务端注册信息决定，不使用 `terminalType`、User-Agent 或设备请求头决定名额。现有根客户端算 Web；移动客户端使用 `<根 client ID>:miniapp` 和 `<根 client ID>:app`。两个移动客户端保留原应用 appId、租户开通和角色权限范围，采用公共客户端的授权码 + PKCE S256，不分发 client secret，也不能使用 client credentials 获取服务权限。

在 `base_app.extend_data.oauth` 中显式注册移动客户端：

```json
{
  "terminalClients": {
    "miniapp": {"enabled": true, "redirectUris": ["/login/callback"]},
    "app": {"enabled": true, "redirectUris": ["/login/callback"]}
  }
}
```

保留该应用原有的其他 OAuth 配置。未注册、禁用、错误注册或根应用被禁用时，移动客户端不可用。回调地址仍经服务端校验。`20261029_27_terminal_sessions.py` 迁移为 `iot_platform-98dc2cbb2a528b0e` 注册上述两个客户端；不会覆盖已有注册，也不会创建另一套应用、用户或角色。其他应用按需登记相同结构。

有效期配置位于 `jbm.auth.jwt`：

| 配置 | 默认值 | 行为 |
| --- | --- | --- |
| `access-token-seconds` | 1800 | 所有终端访问凭证的有效期 |
| `refresh-token-seconds` | 604800 | Web 刷新凭证的滚动有效期 |
| `mobile-refresh-token-seconds` | 2592000 | 小程序、APP 刷新凭证的滚动有效期 |

每次刷新返回新的 Access Token / Refresh Token，客户端必须原子保存新凭证，并合并并发刷新。响应中的 `refresh_expires_in` 表示本次刷新凭证有效秒数，`terminal_type` 表示注册终端。活跃移动用户可持续滚动续期；连续 30 天未刷新后需重登。旧刷新凭证重放仍会撤销整个凭证族，重放标记和撤销记录覆盖移动凭证有效期。原 `max-sessions-per-user` 多会话配置不再生效。

发布顺序：Center 迁移 → 统一认证服务 → 新小程序。若使用 Nacos 等外部配置，确认 `jbm.auth.jwt.access-token-seconds=1800`、`mobile-refresh-token-seconds=2592000`，外部值会覆盖仓库默认值。移动客户端不得先于认证服务发布。旧小程序 Web 客户端凭证需要升级后重新登录一次，保留账号、租户、项目上下文。回滚旧服务时移动客户端不可用，数据库登记保留以免删除管理员后续修改。

APP 接入使用 `iot_platform-98dc2cbb2a528b0e:app`，使用登记回调和 PKCE S256，刷新时发送同一个 client ID；刷新凭证保存到系统安全存储，恢复前台或业务请求时续期，无需后台常驻。退出调用 `POST /oauth2/logout` 撤销当前访问/刷新凭证。离线退出只能保证清除本机凭证。

本地验证（在 `jbm-python-cluster` 目录运行）：

```powershell
# 指向独立测试 Redis；测试使用随机命名空间并只清理自己的数据。
$env:JBM_AUTH_TEST_REDIS_URL = 'redis://127.0.0.1:<测试端口>/0'
.\.venv\Scripts\python.exe -m pytest auth/tests -q
```

测试覆盖 HTTP 授权码 + PKCE 的三端登录、共享应用权限、同端顶号、单端退出、移动端 30 天滚动续期与不活跃过期、超过一周的刷新重放、双实例 Redis 竞态与锁失效。小程序在其仓库运行 `node --test tests/*.test.js`，覆盖冷启动续期、连续轮换、并发合并、顶号后的重登、旧客户端迁移、退出撤销和迟到响应隔离。
