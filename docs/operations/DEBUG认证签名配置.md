# DEBUG 认证签名配置

2026-11-18，DEBUG Auth 使用 `py-7.3-20261118.2`，冻结提交 `e08be7d85afb8bc774e7f1700239df9ba80b063b`。此版本支持 `JBM_AUTH_JWT_PRIVATE_KEY_FILE`；显式配置 `JBM_AUTH_JWT_PRIVATE_KEY` 时仍以该值优先。密钥文件不存在或为空时拒绝启动。

DEBUG 当前服务使用服务端生成的 RSA 3072 位密钥，保存在外部 Docker 卷 `jbm-debug-auth-signing`，以只读方式挂载到 `/run/secrets/jbm-auth-signing`，文件 `signing.pem` 的属主为 1000、权限为 0600。公钥 SPKI SHA256：`6f3329f4c8c2842126563847ee1264ca36ec64280a74b0d46d90d96071801300`。这不是管理员登录文件的私钥。

后续按原 DEBUG 配置重建 Auth 时，把 `deploy/debug-auth-signing.compose.yml` 作为最后一个 Compose 覆盖文件；保留已有环境、其他卷和网络。卷必须已存在，禁止自动重新生成或清空。该文件只适用于 DEBUG，其他环境保留各自有效签名密钥，不能共用 DEBUG 的密钥或卷。

本轮先前的 `.1` 更新暴露了原服务使用进程临时签名密钥的问题，导致旧访问令牌验签失败。`.2` 已修复并补齐持久配置；真实管理员文件登录后，使用同一令牌在 Auth 重启前后验证 `/auth/oauth2/userinfo`，两次均 HTTP 200、业务成功。后续签名轮换必须另行安排兼容与会话策略，不能把重新登录当成重启会话保持验收。

原服务规格、配置与回滚证据留在 IoT 仓库忽略目录 `output/debug-schema-followup-20261118`，公共结果见该仓库 `deploy/releases/20261118-debug-schema-followup.json`。此部署说明不改写已冻结镜像的源码提交。
