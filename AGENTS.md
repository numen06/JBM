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
