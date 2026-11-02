# Repository agent instructions

## CodeGraph

This branch is CodeGraph-enabled. The local index is intentionally not committed.

- Run `codegraph init .` after cloning, or `codegraph index .` after large changes.
- When `.codegraph/` exists, use `codegraph explore "<symbol or question>"` before text search or manual file traversal.
- Do not commit `.codegraph/codegraph.db`, daemon state, sockets, or logs.

## 用户约定：优先密钥登录

- 后续访问 JBM 及复用 JBM 认证的业务系统，优先使用本地已有的有效 `.jbmkey` 文件；按平台域名与账号匹配，不跨环境复用。
- 没有有效密钥时，在用户授权范围内使用已有账号密码，通过正式认证、密码验证和密钥绑定接口生成并保存到当前用户的 `Documents/JBM登录密钥`。不得为此重设密码、跳过认证或直接写入凭证表。
- 私钥仅保存在用户本地，禁止打印、上传服务器或提交到 Git；留存不含秘密的文件位置及登录验收结果。生成后重新读取本地文件验证账号与权限一致。

## JBM 统一版发布与验收

- JBM 开发主线为 `7.3-python`；构建使用已提交、已推送的同一主线提交。只更新本次相关服务，保留现有配置、持久化数据及其他服务。
- 原 app2docker 流水线构建多阶段 Dockerfile 时必须核对实际运行阶段，不能把服务名称当成已选择对应 stage。已验证的 `.app2docker-database-job.yaml`、`.app2docker-menu-center.yaml`、`.app2docker-menu-gateway.yaml` 通过 `JBM_BUILD_TARGET` 选择运行时；部署前检查 OCI revision、应用导入和所需路由，禁止仅凭“构建成功”替换线上服务。
- 发布后使用正式认证的管理员会话运行 `jbm-python-cluster/tools/check_release_contract.py`，契约为 `deploy/jbm-administration-contract.json`。令牌从本地会话文件读取，不打印或提交。HTTP 200 仍必须业务成功。
- 同时逐项检查宿主 JBM 菜单和实际加载接口，记录错误路由、功能性 404/403/500；健康检查不能代替页面验收。记录服务、镜像摘要、源码提交及验收结果。数据库运维验收只读连接与任务记录，不自动执行备份、还原或启用策略。
