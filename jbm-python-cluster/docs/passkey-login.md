# Passkey 与 SSH key 登录

JBM 统一管理网页登录密钥，并由 JBM Auth 签发 OAuth access/refresh token。JBM 登录页和个人中心提供 Passkey、SSH key 登录及凭据管理；接入 JBM Auth 的应用可以复用这些能力。

首次通过已有方式登录并绑定密钥。之后 Passkey 直接选择账号，通过指纹、面容或设备 PIN 验证，不需要输入用户名、密码或图形验证码。一个账号支持多个凭据。

SSH key 通过 `ssh-keygen -Y sign` 对下载的一次性文件签名，JBM 根据账号下的公钥识别用户。支持 Ed25519、RSA（至少 2048 位）、ECDSA。已有 SSH 私钥可以复用；将其现有公钥关联到 JBM 账号后，无需增加另一把私钥。浏览器不能直接把普通 SSH 私钥作为 WebAuthn Passkey；这里提供的是使用同一把 SSH key 的独立登录方式。

私钥无需上传，服务器保存公钥。SSH 登录使用专用签名命名空间 `jbm-key-login`，验证文件包含站点、用途、客户端和随机挑战，120 秒过期且只能使用一次。Passkey 强制用户验证，验证 RP ID、Origin、签名、账号绑定和签名计数。登录沿用 JBM 的账号状态、OAuth PKCE、租户权限、刷新和会话撤销机制。

## 线上状态（2026-09-29）

JBM 管理后台已发布在 <https://feige.hz-aitech.com/jbm/>。登录页选择“使用已绑定的 SSH key”，下载一次性挑战文件，在本机运行页面给出的 `ssh-keygen -Y sign` 命令，用已有私钥签名，再把签名粘贴回页面，即可登录。这里的 SSH key 登录是网页身份验证，不是服务器终端登录。普通 SSH key 不能直接成为 WebAuthn Passkey，因此页面分别提供 SSH key 和 Passkey 两种方式；它们由同一个 JBM 账号管理。

生产超级管理员 `admin / user_id=0` 登记的是本机已有 `C:\Users\admin\.ssh\id_ed25519` 的公钥，指纹 `SHA256:/1Tmh8iZHG2Vzq90bpfJCperDl6s769WG0NREY9mrfU`。服务器没有该私钥。线上账号目前只有这一个 `SSH_KEY` 登录凭据；端到端测试时临时创建的虚拟 Passkey 已删除。

生产 JBM 数据库已迁移至 `20260928_24`。Auth 服务使用镜像 `registry.cn-shanghai.aliyuncs.com/okc/jbm-python-cluster:py-7.3.23-passkey-20260929`，JBM Admin 服务使用镜像 `registry.cn-shanghai.aliyuncs.com/okc/jbm-admin:py-7.3.23-passkey-admin-20260929-2`，均为单副本运行。JBM Admin 的 OAuth 客户端为 `jbm_admin_hz_20260929`，回调地址为 `/jbm/login/callback`。Auth 已启用 `feige.hz-aitech.com` 的 RP ID 和 `https://feige.hz-aitech.com` Origin；生产配置中的固定验证码覆盖值已清空。

公开域名的 `/jbm/`、`/jbm/v3/api/` 与 `/jbm/auth-center/` 由 Nginx 转发，原站点 `/` 保持原用途。当前规则写在服务器 Nginx-webui 生成的配置文件中；以后若在 Nginx-webui 重新生成站点配置，需要检查并保留这三条路由。发布前的 Auth 服务规格与 Nginx 站点配置备份分别在服务器 `/home/opt/backups/passkey-20260929/auth-service-before.json` 和 `/home/opt/backups/passkey-20260929/feige-nginx-before.conf`，仅供受控回滚使用。

## 部署与配置说明

后端改动位于 `jbm-python-cluster`，JBM 前端改动位于 `jbm-admin-vue`。在其他环境部署时，需要同时发布 Auth 与 JBM Admin，才能在 JBM 登录页和个人中心使用新入口。其他应用可随后接入同一套 JBM Auth 接口。

1. 安装更新后的 Auth 依赖（`webauthn`，已更新 `uv.lock` 和生产镜像使用的 `requirements.txt`）。
2. 通过 Center 的标准 Alembic 流程执行迁移 `20260928_24`，创建 `base_auth_credential`。生产 Auth 不自动创建表。
3. 在 Auth 的配置中填写实际访问域名并启用功能，例如：

```yaml
jbm:
  auth:
    key-login:
      enabled: true
      rp-id: feige.hz-aitech.com
      rp-name: 飞鸽智控平台
      origins:
        - https://feige.hz-aitech.com
```

RP ID 和 Origin 必须明确配置，不从 Host 或转发头推导。Origin 是浏览器地址栏的来源，不是内部 Auth 服务地址。浏览器要求 HTTPS（本机 `http://localhost` 可用于开发）。跨子域共用凭据时，RP ID 需为合法的共同父域并明确列出可信 Origin；不同主域需要分别绑定，不要随意扩大到其他应用域名。

复用原有 SSH key 时，仅把现有公钥登记到 JBM 凭据表，私钥保持原位置。JBM 独立管理网页登录密钥的查询、使用和撤销，不访问 IoT 数据库。若同一把公钥还用于其他系统，撤销 JBM 登录权限不会自动撤销那些系统的权限。

4. 使用既有生产 Redis 作为挑战和会话存储。重新构建 Auth 与 JBM Admin。
5. 登录一次，在“登录密钥”中添加 Passkey 或已有 SSH 公钥，退出后验证免密码登录；保留原密码/短信作为恢复方式。

代码中功能默认关闭，必须由目标环境明确启用。`jbm.auth.fixed-captcha-code` 在生产配置中必须为空；已发布的生产 Auth 服务使用空值。

## 验证

JBM 新增 `auth/tests/test_key_login.py`：真实 WebAuthn 加密签名、账号识别、OAuth/PKCE 换票、签名重放、错误 Origin/RP、用户验证、错误 userHandle、签名计数、停用/撤销、重复绑定、跨会话绑定、过期与跨客户端挑战，以及 OpenSSH 三种密钥互通。

`auth/tests/test_key_login_browser.py` 为可选 Chrome 测试，使用虚拟认证器执行实际 `navigator.credentials.create/get` 和生产 TypeScript 编解码函数；设置 `JBM_AUTH_BROWSER_TEST=1`，可用 `JBM_AUTH_CHROMIUM_EXECUTABLE` 指定 Chrome。

发布验证结果：Python 测试 `294 passed, 4 skipped`，Chrome WebAuthn 测试 `1 passed`，JBM Admin 构建通过。生产公网完成现有 SSH 私钥签名登录、OAuth 授权码换票、`userinfo` 身份校验、签名重放拒绝和注销；实际 Chrome 页面已登录至 `/jbm/dashboard`。虚拟认证器还在生产页面完成 Passkey 注册、退出、Passkey 再登录、删除和再次退出。最终检查 `/jbm/`、`/jbm/env.js`、登录接口均正常，Auth 与 Admin 服务各有一个运行副本，数据库没有残留测试 Passkey。

参考：[WebAuthn 标准](https://www.w3.org/TR/webauthn-3/)、[py_webauthn](https://duo-labs.github.io/py_webauthn/)、[OpenSSH 签名协议](https://github.com/openssh/openssh-portable/blob/master/PROTOCOL.sshsig)。
