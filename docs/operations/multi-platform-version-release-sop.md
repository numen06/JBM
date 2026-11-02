# 三平台统一发布 SOP

正常发版、紧急修复、局部上线均遵循本文。原 App2Docker 是唯一正式构建系统；开发分支允许使用，正式制品必须来自已合回、已提交、已推送并冻结的主线。

## 主线与版本

| 平台 | 仓库 | Git 正式主线 | 镜像版本 |
| --- | --- | --- | --- |
| JBM | JBM7 | `py-7.3` | `py-7.3-YYYYMMDD.N` |
| IoT | iot-platform-jbm | `py-2.0` | `py-2.0-YYYYMMDD.N` |
| 建筑 | smart-building-platform-py | `py-2.0` | `py-2.0-YYYYMMDD.N` |

日期按 Asia/Shanghai，N 从 1 递增，按平台、主线、日期独立计数。IoT 和建筑同名主线不共用计数器。普通、紧急、仅前端、单服务修复均占用下一版本。失败、取消也占号，禁止复用、覆盖镜像或移动 Git 发布 tag。跨日的已冻结构建沿用原版本。

旧 `jbm`、`7.3-python`、`py` 保留迁移基线追溯，禁止继续写入或作为新正式构建来源；`dev`、`master`、Java 等历史正式分支保持独立，不批量合并、不覆盖。

SDK、组件包保留自身包版本，精确依赖记录在锁文件；不能用包版本代替平台版本。同平台一次计划输出的 API、Web、Worker、基础镜像共用 VERSION，镜像名区分组件。部署使用 `镜像:版本@sha256:摘要`。

## 唯一入口与规则保存

- 本文为三仓库相同内容的规范；`deploy/release-catalog.json` 定义平台、主线、原流水线 ID、组件、Dockerfile target、镜像名和服务归属。
- `tools/release.py` 为统一操作入口。VERSION 和 `.app2docker-release-<组件>.yaml` 由工具生成，禁止手改某个组件版本或另复制功能名、日期 profile。
- 根 `.app2docker.yaml` 的 `release` 保存主线、平台、VERSION、时区、不可变规则。App2Docker 服务端将它应用于所有 profile，校验 Git 来源、同名发布 tag、远端主线 SHA、完整 VERSION 和全部镜像 tag。
- App2Docker 在原持久数据库内原子占用镜像版本，失败任务不释放占用。构建参数强制写入完整 SOURCE_REVISION、JBM_COMMIT 和 RELEASE_VERSION，最终镜像必须有 OCI version/revision。
- 原流水线固定本平台主线；不使用推送分支作为任意来源，不启用自动部署。旧兼容 profile 默认禁用推送；没有冻结标签的手工构建不能发布正式版本。
- `.githooks` 在提交、推送时检查主线、生成配置和归档完整性。新环境执行 `git config core.hooksPath .githooks`。旧命令 `tools/check_jbm_mainline.py` 仅为兼容入口，实际读取目录中的主线。
- 历史一次性文件在 `deploy/release-archive/20261102/`，有原路径、来源与 SHA-256。历史发布记录保持原样，引用按当时 commit 解释，不从归档直接发版。

App2Docker 配置顶层 `version: '1.0'` 为配置结构版本，不是产品版本。

## 标准打包步骤

1. 从本平台主线开发，可用 `feat/*`、`fix/*`、`hotfix/*`。验证后合回主线，不从开发分支、脏工作区、旧发行副本或独立发布仓库出正式包。
2. 核对变更、相关服务、配置、依赖及迁移。完成测试；功能/UI 改动更新原本实际运行的本地服务，从原 debug 地址验收，不能只编译或另开预览。
3. 工作区干净且已推送，执行 `python tools/release.py reserve`。工具读取远端已占用编号，原子创建 `release-reservations/<完整版本>`，生成 VERSION 和全部组件配置。抢占冲突应停止核对，不强推。
4. 检查生成差异，提交并推送主线；执行 `python tools/check_release_catalog.py`、`python tools/check_mainline.py --release`、`python tools/release.py freeze`。同名 Git tag 固定本次完整 SHA。
5. 按计划逐组件执行 `python tools/release.py build <组件>`，使用原流水线和已冻结 Git tag；执行 `status <组件>` 查看任务。启动记录在忽略目录 `.release-output/<版本>/`，不存密钥。
6. 冻结后改源码、失败或需要重建时重新 reserve 新版本；不覆盖旧标签，不删除占号记录。若提交请求中断且不确定是否受理，先查询任务，禁止盲目重复推送。
7. 核对所有计划组件构建完成、任务 config_commit 与冻结 SHA 相同、profile/target 正确、AMD64/ARM64 清单、OCI version/revision、镜像 digest。隔离导入应用、核对关键路由、文件和原配置；不能凭阶段名或构建成功认定内容正确。

## 上线与验收

1. 保存生产旧服务规格、摘要、配置与迁移 revision，准备逐服务回滚基线。需要变更业务数据时按授权备份，不因普通镜像发版自动恢复或清空数据。
2. 使用候选制品更新本地实际服务并验证，再分批上线，通常 JBM → IoT → 建筑。保留环境变量、密钥、网络、卷、副本、启动命令和消费语义，不顺带更新外部服务或网关固件。
3. 同一制品按摘要晋级，不在生产重建同名标签。每步检查健康、日志、实际接口与原业务；异常暂停后续服务，按回滚基线恢复并检查。
4. JBM 执行 `jbm-python-cluster/tools/check_release_contract.py` 和 `deploy/jbm-administration-contract.json`，逐项走实际菜单及加载接口。HTTP 200 仍须业务成功，功能性 404/403/500、跳转错误和卡住均不能通过。
5. IoT 执行 `tools/check_streetlight_integrity.py` 和 `deploy/integrity/party-school-streetlights.json` 的生产只读核对，复查定位、配置、采集与控制回执链。建筑复查项目/空间、API、批处理、报表及数据新鲜度。
6. 现场控制必须已有明确授权和恢复措施，默认只读真实状态，不自动下发控制或伪造遥测。数据库运维验收不自动备份、还原或启用策略。
7. 所有计划服务的实际版本、完整提交、digest 和清单一致才记已上线；部分完成明确记录部署中/部分完成，不能靠改显示版本或重新打标掩盖旧内容。

## 发布记录

正式记录放 `deploy/releases/` 或已有 `docs/releases/`，包含：平台/仓库/主线/版本/发布类型/状态，冻结 SHA 与 Git tag，原 App2Docker 流水线和任务，每个组件 Dockerfile/target/基础摘要/架构/OCI 元数据/最终 digest，部署前后服务规格引用，迁移和兼容性，测试、本地实际服务、线上菜单/API/完整性结果，回滚基线与最终三平台组合。秘密只在忽略的私有记录中，不提交 Git。

## 紧急修复与回滚

紧急修复同样合回主线、递增版本、冻结标签、原流水线构建并核对内容；可缩小相关服务和测试范围，但须记录缩小项与后续补验。局部上线如实保留其他服务旧版本；目标整套统一时必须补齐全部计划组件。

回滚到上一个已验收的原摘要和兼容配置，记录失败版本及原因，不覆盖旧 tag。应用回滚与数据回滚分开，不能自动删表、还原数据库。回滚后复查认证、消息、设备连接和数据新鲜度；下一修复继续从当天已占用最大号递增。

## 主线迁移维护

迁移须备份 refs、未提交文件、工作树及在途任务；只从现有融合主线 SHA 建立目标主线并比较 tree。同步远端默认分支/保护、upstream、钩子、流水线和文档。目标同名分支已有不同历史时停止核对，不能强推。删除历史临时分支前核实已合入且无工作树占用；不以批量合并代替审查。
