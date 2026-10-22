# 党校密钥登录配置

`party-school-key-login.json` 是党校认证服务的环境配置增量，合入党校 Nacos 的 `jbm-py / DEFAULT_GROUP / auth-prod.yml`，只调整 `jbm.auth.key-login` 下列出的四个字段，保留其他配置。变更前备份原文，比较当前版本后提交，再回读核对并重启认证服务。

认证镜像升级与环境配置独立：本地 `auth-dev.yml` 中开启密钥登录，不会启用党校生产配置。发布验收须同时核对运行代码、有效配置，以及公网 `/jbm-api/auth/oauth2/keys/login-options` 的文件密钥挑战响应；仅前端入口可见或容器健康不足以证明功能已启用。

党校只接受 `jxdx.project.hz-aitech.com` 及对应 HTTPS 来源，不复用本地调试域名。保留现有密码校验、PKCE、权限、密钥撤销和挑战一次性校验。首次使用须由账号本人在用户中心验证当前密码并生成密钥文件；公网挑战成功与实际文件登录成功分别记录。
