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

- JBM 当前开发主线为 `py-7.3`；允许从主线使用 `feat/*`、`fix/*`、`hotfix/*` 开发，验证后合回，正式构建只使用已提交、已推送的主线提交。只更新相关服务，保留配置、持久化数据及其他服务。
- 原 app2docker 流水线构建多阶段 Dockerfile 时必须核对实际运行阶段，不能把服务名称当成已选择对应 stage。统一生成的 `.app2docker-release-*.yaml` 通过 `RELEASE_TARGET` 选择运行时；部署前检查 OCI revision、应用导入和所需路由，禁止仅凭“构建成功”替换线上服务。
- 发布后使用正式认证的管理员会话运行 `jbm-python-cluster/tools/check_release_contract.py`，契约为 `deploy/jbm-administration-contract.json`。令牌从本地会话文件读取，不打印或提交。HTTP 200 仍必须业务成功。
- 同时逐项检查宿主 JBM 菜单和实际加载接口，记录错误路由、功能性 404/403/500；健康检查不能代替页面验收。记录服务、镜像摘要、源码提交及验收结果。数据库运维验收只读连接与任务记录，不自动执行备份、还原或启用策略。
- 正常发布与紧急修复均遵循 `docs/operations/multi-platform-version-release-sop.md`，新正式镜像统一使用 `py-7.3-YYYYMMDD.N`，每次递增，不新建格式之外的功能或日期快照版本。当前入口见 `deploy/release-catalog.json`，发布前运行 `python tools/check_release_catalog.py --version <新版本>`；这不代替远端版本占用和镜像内容检查。主线已迁移；正式出包统一执行 tools/release.py，不直接运行旧 profile。

- 镜像主线前缀必须与 Git 主线同名，日期按 Asia/Shanghai，当天序号从 1 递增；新格式正式发布前须完成目录中登记的主线同名迁移，不在旧分支名上先出新前缀的正式包。

- 统一入口：`python tools/release.py reserve` → 提交推送 VERSION 与生成配置 → `freeze` → `build <组件>` → `status <组件>`。完整规则同时写入 app2docker release 配置并由服务端校验。

## 用户约定：时序数据统一 TDengine

- 原始采集、属性/状态变化、上下线与流量历史、控制/策略日志、用量及日月年统计、计费事实、校正值与校正历史等时序数据统一进入 TDengine。MySQL 保留业务实体、配置、当前事务状态与必要关联；Redis 负责队列、锁和缓存，不作为唯一历史存储。
- 事务 outbox 可短暂保留待归档正文；TD 写入、逐条回读及业务对账通过后，清理对应 MySQL 历史数据并移除旧读写/fallback 入口，禁止将长期双读或双份历史当作迁移完成。保留独立持久备份，不让备份接入在线查询；未归档、校验失败或并发变化的记录不得删除。
- 历史迁移按 tenant/project/resource 与原 ID/版本限定，保留合法零值、未知、真实时间粒度和业务关联。混合资源表不能整表清空；产品事件定义和映射等配置不属于时序事件历史。
- 分工、现状差距及清理验收标准统一见 IoT 仓库 `docs/operations/时序数据存储与迁移规范.md`。该规范中的待迁移项不能描述为已完成。
