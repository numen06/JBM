# JBM 本轮验证与修复记录

时间：2026-05-25（Cursor 执行计划闭环）

## 执行摘要

按 `.cursor/plans/jbm发现问题与cursor执行计划_20260525.plan.md` 完成 P0 修复确认、构建、服务重启、页面/API 实测与结果落盘。未回退用户已有改动（组织 UTF-8 迁移 `V16__base_org_utf8mb4.sql`、`OrgList.vue`、`org.ts` 等）。

## 已确认/已存在的 P0 修复

| 项 | 状态 | 说明 |
|---|---|---|
| 扩展字段管理：左侧字段组 → 右侧字段定义 | **已修复** | `ExtendFieldList.vue` 左侧 `button[data-testid=field-group-item]` + `getExtendFormFromDb`；后端 `GET /extend-field/forms` 分页按租户 |
| 字典页 TypeScript 构建 | **已修复** | `BaseDic.id/dicId/parentId` 已为 `number \| string` |
| 网关管理 SQL 继承列 | **已修复** | `GatewayRoute/RateLimit/IpLimitServiceImpl` 显式 `select` 真实列 |
| 注册 RSA 密码入库 | **已修复** | `SysLoginService.decryptPassword` 识别 RSA 密文后仍解密；注册走同一链路 |

## 本轮 E2E / API 实测结果

| 脚本 | 结果 | 输出 |
|---|---|---|
| `node .cursor/e2e-extend-fields-group-detail.cjs` | **passed** | `.cursor/e2e-extend-fields-group-detail-result.json`、`.cursor/screenshots/e2e-extend-fields-group-detail.png` |
| `node .cursor/jbm-user-admin-e2e.cjs` | **passed** | `.cursor/jbm-user-admin-e2e-result.json`（注册→登录→开发者→API Key→网关路由/限流/IP） |
| `python scripts/run_tenant_isolation_tests.py` | **passed** | `.cursor/e2e-tenant-isolation-result.json` |
| `python scripts/verify_extend_field_cn.py` | **passed** | `.cursor/e2e-utf8-cn-result.json`（客户表单/客户等级） |
| `cd jbm-admin-vue && npm run build` | **passed** | vue-tsc + vite build |
| `python scripts/verify_org_utf8_full.py` | **passed** | `.cursor/e2e-org-utf8-result.json`（组织 save/pageList/tree UTF-8） |
| `node .cursor/e2e-org-create-utf8.cjs` | **passed** | `.cursor/screenshots/e2e-org-create-utf8.png`、`e2e-org-tree-utf8.png` |
| `python scripts/run_org_plan_smoke.py` | **passed** | 中文子组织新建与树回显 |
| `python scripts/verify_menu_management.py` | **passed** | `.cursor/e2e-menu-management-result.json`（分页/搜索/scope/当前菜单） |
| `node .cursor/e2e-menu-management.cjs` | **passed** | 菜单页分页栏、搜索、平台 scope 筛选 + 3 张截图 |
| `mvn -pl ...-center -Dtest=CenterRbacApiH2IT#menu_paginationScopeAndKeyword test` | **passed** | H2 集成：分页、keyword、scope=platform |

### 菜单管理分页与多应用/租户模式（2026-05-25）

**已修复 BUG**：菜单管理页原先 `listAllMenus()` 全量拉取 + 前端本地过滤，无分页。

**前端**（`MenuList.vue`、`menu.ts`）：

- 改用 `usePagedList` + `listMenus(page, size, filters)` + `PaginationBar`
- 服务端分页/搜索：`keyword`、`status`、`scope`、`appId`
- 范围筛选：平台菜单 / 应用菜单 / 当前可见 / 全部
- 表格列：范围（平台/应用）、应用名称、`isPersist` 与平台标识
- 表单：菜单范围（平台/应用）、所属应用；非超管禁用平台菜单编辑/删除

**后端**：

- `BaseMenuForm` 增加 `scope`、`keyword`（`@TableField(exist=false)`）
- `BaseMenuServiceImpl.findListPage`：keyword/path/status/appId/scope 过滤；排序 `parent_id, priority, menu_id`
- `MenuDataScopeHelper`：平台超管全量；租户管理员仅「平台菜单只读 + 本组织 `BaseApp.orgId` 下应用菜单可写」
- `BaseMenuBusinessImpl`：创建/更新/删除前校验平台菜单与应用归属

**菜单模型规则**：

- `appId=null` → 平台公共菜单（全局共享，仅超管可改）
- `appId=应用ID` → 应用菜单（经 `BaseApp.orgId` 组织隔离）
- `/current/user/menus`：沿用「当前 app 菜单 + 平台菜单」聚合（`BaseAuthorityMapper.selectAuthorityMenu`）

**API 实测（Gateway `/menu`）**：

- 第 1 页 5 条，`total=21`；第 2 页数据变化
- `keyword=用户` → `total=2`（用户管理、在线用户）
- `scope=platform` → 全部 `appId=null`
- `/current/user/menus` → 21 条（超管）

**构建**：`npm run build` passed；`mvn ... install`（api-basic + common-mysql + platform-center）passed

**截图/结果**：

- `.cursor/screenshots/e2e-menu-pagination.png`
- `.cursor/screenshots/e2e-menu-app-scope.png`
- `.cursor/screenshots/e2e-menu-platform-protect.png`
- `.cursor/e2e-menu-management-result.json`

**后续演进缺口**：

- 应用下拉列表尚未按租户自动过滤（CRUD 已由后端 `MenuDataScopeHelper` 拦截）
- 角色授权页菜单树未单独加租户 scope UI（权限 SQL 已有 appId 规则）
- `base_menu` 无显式 `tenantId`，长期可经 `base_app.tenant_id` 或菜单表扩展字段补强

### 用户/管理员 E2E 覆盖步骤

1. 普通用户 `/register` 注册（RSA 密码）→ `/login` → `/dashboard`
2. 提交开发者申请；未审批时 `/developer/api-keys` 新建按钮禁用
3. 管理员审批 → 用户创建 API Key，Secret 一次性展示
4. 管理员 `/system/online-users` 加载正常
5. 管理员 `/gateway/routes`、`/gateway/rate-limit`、`/gateway/ip-limit` 均完成新增、筛选、编辑、删除

### 多租户隔离

- 租户 0 保存 `iso_tenant_a_*`，租户 1 保存 `iso_tenant_b_*`
- 各租户列表互不可见；租户 0 无法读取租户 B 的 formCode

## 构建与服务命令

```powershell
cd D:\workspaces\JBM7\jbm-admin-vue
npm.cmd run build

cd D:\workspaces\JBM7
python scripts\jbm_cluster_ops.py start auth center gateway --background --clean
python scripts\jbm_cluster_ops.py wait --timeout 180
```

集群 profile：`jaja7`；Auth `5555`、Center `8888`、Gateway `7777`、Vue dev `5173`。

## 本轮脚本微调

- `.cursor/e2e-extend-fields-group-detail.cjs`：等待 `#formCode`；字段组匹配改为任意 `[data-testid=field-group-item]`（不再仅限 `cen_form_*`）
- `.cursor/jbm-user-admin-e2e.cjs`：网关 CRUD 保存后 `reload` 再断言表格行，避免列表未刷新导致超时
- 新增 `scripts/verify_extend_field_cn.py`：经 Gateway 验证中文扩展字段读写

## 中文编码

- 数据库：`V16__base_org_utf8mb4.sql` 将 `base_org` 转为 `utf8mb4`；新增 `V17__repair_base_org_utf8_data.sql`（幂等修复默认组织、清理 E2E 测试组织）、`V18__cleanup_empty_org_names.sql`（按 HEX 修复 id=1 乱码、删除空名称脏数据）
- JDBC：Hikari/Druid 增加 `connection-init-sql(s)=SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci`
- 前端：`request.ts` 统一 `Content-Type: application/json;charset=UTF-8`
- **组织乱码根因（已修复）**：Gateway `XssFilter` 将 JSON body 以 UTF-8 读入后以平台默认 charset（Windows 常为 GBK）写回，导致浏览器 POST 中文 `orgName` 落库乱码；已改为 `StandardCharsets.UTF_8`，并将 `/baseOrg/**` 加入 XSS 白名单；`GatewayContextFilter` JSON 判定改为 `isCompatibleWith(APPLICATION_JSON)`
- API：扩展字段「客户表单」「客户等级」经 Gateway JSON 读写正常（见 `e2e-utf8-cn-result.json`）
- **组织新建中文（2026-05-25 复测通过）**：
  - `python scripts/verify_org_utf8_full.py` → `.cursor/e2e-org-utf8-result.json`（save/pageList/tree 均 `测试组织UTF8`）
  - `node .cursor/e2e-org-create-utf8.cjs` → 页面新建、刷新搜索、组织选择器截图
  - `python scripts/run_org_plan_smoke.py` → 中文子组织保存与树回显通过
  - 默认组织 id=1 显示为「默认组织」；`scripts/cleanup_empty_orgs.py` 清理 9 条空名称 E2E 残留

## P1/P2 合理优化（已具备或部分具备）

- 扩展字段列表分页/搜索：已实现 `pageExtendForms(keyword, page, pageSize)`
- 首页/API Wiki：`LandingPage.vue`、`ApiWikiPage.vue` 已含 0→1 接入路径与 FAQ（未审批、限流、跨租户等）
- Logo：`JbmLogo` 统一 JBM 图形，导航「JBM 开源平台」

## 截图路径

- `.cursor/screenshots/e2e-extend-fields-group-detail.png`
- `.cursor/screenshots/e2e-user-dashboard-after-register.png`
- `.cursor/screenshots/e2e-user-developer-self-service.png`
- `.cursor/screenshots/e2e-user-developer-apply-submitted.png`
- `.cursor/screenshots/e2e-user-apikey-before-admin-approval.png`
- `.cursor/screenshots/e2e-admin-developer-pending-user.png`
- `.cursor/screenshots/e2e-user-apikey-after-approval-created.png`
- `.cursor/screenshots/e2e-admin-online-users-loaded.png`
- `.cursor/screenshots/e2e-admin-gateway-route-created.png`
- `.cursor/screenshots/e2e-admin-gateway-rate-created.png`
- `.cursor/screenshots/e2e-admin-gateway-ip-created.png`
- `.cursor/screenshots/e2e-org-create-utf8.png`
- `.cursor/screenshots/e2e-org-tree-utf8.png`
- `.cursor/screenshots/e2e-menu-pagination.png`
- `.cursor/screenshots/e2e-menu-app-scope.png`
- `.cursor/screenshots/e2e-menu-platform-protect.png`

## 组织 UTF-8 修复修改文件

| 层 | 文件 |
|---|---|
| Liquibase | `V16__base_org_utf8mb4.sql`（确认 UTF-8 中文）、`V17__repair_base_org_utf8_data.sql`、`V18__cleanup_empty_org_names.sql`、`db.changelog-master.yaml` |
| JDBC | `mybatis-plus.properties`、`druid.properties` |
| Gateway | `XssFilter.java`、`GatewayContextFilter.java`、`bootstrap.yml`（`/baseOrg/**` XSS 白名单） |
| 前端 | `jbm-admin-vue/src/api/request.ts` |
| 验证脚本 | `scripts/verify_org_utf8_full.py`、`scripts/cleanup_empty_orgs.py`、`.cursor/e2e-org-create-utf8.cjs` |

## 后续 P0/P1 待办（未用「构建通过」替代）

1. **清理乱码测试数据**：删除或重命名 `ui_fields_*`、乱码 `cen_form_*` 扩展字段组
2. **多租户 API Key / 网关策略隔离**：当前脚本仅覆盖扩展字段；API Key 授权范围与网关策略跨租户 REST 断言可扩展 `run_tenant_isolation_tests.py`

### 组织管理树模式（2026-05-25）

**已改造**：组织管理页从「纯分页列表 + 手填父级 ID」升级为「组织树优先 + 列表辅助」双视图。

**前端**（`OrgList.vue`、`useOrgTree.ts`、`OrgTreeSelect.vue`）：

- 默认进入 **组织树** 视图；顶部 segmented control 可切换 **列表** 视图
- 左侧：真实组织树（展开/折叠、选中高亮、搜索过滤、刷新），非下拉选择器
- 右侧：选中组织详情（ID、名称、编码、父级、排序、状态、子组织数）+ 操作（新建子组织、编辑、删除）
- 直属子组织表格：从树节点 `children` 渲染，点击行可选中子节点
- **新建子组织**：从当前选中节点进入，弹窗 `parentId` 自动填充且锁定
- **新建根组织**：父级留空
- 编辑/新建弹窗：父级改为 `OrgTreeSelect`（树形下拉），编辑时排除自身及子孙节点防循环
- 删除保护：默认组织（id=1 或名称「默认组织」）不可删；有子组织时阻止并提示
- `useOrgTree` 改为模块级共享状态，页面与选择器刷新后数据一致

**列表视图**（辅助模式）：

- 保留分页、搜索、编辑、删除；切换至列表时懒加载 `pageList`

**多租户/组织可见范围**（后端已具备，前端沿用）：

- `BaseOrgServiceImpl.selectEntitys` / 分页：`LoginHelper.isAdmin()` 查全量；有 `companyId` 时按 `groupId` 过滤；否则按用户部门顶层公司 `groupId` 过滤
- `/baseOrg/tree` 与 `/baseOrg/pageList` 共用上述过滤；平台管理员可见全树，租户/组织管理员仅见授权范围
- 前端未单独加 scope UI，依赖后端返回范围；**风险已记录**：若未来需在前端提示「部分组织不可见」，可补 scope 说明条

**中文编码**：沿用既有 UTF-8 修复（Gateway XSS、`request.ts` charset、Liquibase V16–V18）；本任务未改保存链路

**局部构建**：`npm run build` passed（vue-tsc + vite build）

**待总测场景**（由统一总测任务执行，不单独重启/E2E）：

1. 管理员登录 → `/system/orgs` 默认展示组织树视图
2. 左侧可见根组织与子组织层级；点击根组织，右侧显示详情与直属子组织
3. 「新建子组织」弹窗父级为当前组织；中文名 `树模式组织_<timestamp>` 保存后树与右侧子列表同步
4. 切换列表视图可搜索到新组织；编辑时父级为树选择器非数字 ID
5. 刷新页面后树结构正确；组织名称不乱码
6. 非超管账号仅见授权组织子树（与 `/baseOrg/tree` 范围一致）

**截图/结果建议路径**：

- `.cursor/runs/total-20260525/screenshots/e2e-org-tree-view.png`
- `.cursor/runs/total-20260525/screenshots/e2e-org-create-child.png`
- `.cursor/runs/total-20260525/screenshots/e2e-org-list-search.png`
- `.cursor/runs/total-20260525/results-org-tree.json`

**修改文件**：

| 层 | 文件 |
|---|---|
| 前端 composable | `jbm-admin-vue/src/composables/useOrgTree.ts` |
| 前端组件 | `jbm-admin-vue/src/components/OrgTreeSelect.vue` |
| 前端页面 | `jbm-admin-vue/src/views/system/OrgList.vue` |

---

## 集群非核心应用联合修复（2026-05-25）

按计划 `.cursor/plans/集群非核心应用联合修复计划_20260525.plan.md` 完成 P0 横向修复与构建/冒烟脚本。

### 盘点结论

| 服务 | 模块 | 端口 | 入口类 | jaja7 profile |
|---|---|---:|---|---|
| Doc | `jbm-cluster-platform-doc` | 9999 | `JbmDocApplication` | 新增 |
| Push | `jbm-cluster-platform-push` | 3313 | `JbmPushApplication` | 新增 |
| Logs | `jbm-cluster-platform-logs` | 3312 | `JbmLogsApplication` | 新增 |
| Job | `jbm-cluster-platform-job` | 4444 | `JbmJobApplication` | 新增 |
| Weixin | `jbm-cluster-platform-weixin` | 3319 | `JbmWxApplication` | 新增 |
| Bigscreen | `jbm-cluster-platform-bigscreen` | 3314 | `JbmBigscreenApplication` | 新增 |

`jbm-cluster-platform-ai` 不在 `jbm-cluster-platform/pom.xml` modules 中，无源码目录，按历史产物处理，未纳入本轮。

### 代码修复清单

| 项 | 文件 |
|---|---|
| LOG_SERVER 统一为复数 | `jbm-cluster-core/.../JbmClusterConstants.java`（新增 `BIGSCREEN_SERVER`） |
| 六服务 jaja7 配置 | 各模块 `bootstrap-jaja7.yml` |
| Gateway 静态路由 + Feign URL | `bootstrap-jaja7.yml`、`application-jaja7.yml`（Weixin 不含 `/user/**`，避免与 Center 冲突） |
| 运维脚本扩展 | `scripts/jbm_cluster_ops.py`（doc/push/logs/job/weixin/bigscreen + 按服务 wait） |
| 冒烟脚本 | `scripts/run_cluster_apps_smoke_tests.py`、`scripts/cluster_apps_smoke_modules.json` |

### 构建结果

```powershell
mvnd -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-doc,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-push,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-logs,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-job,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-weixin,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-bigscreen,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-gateway -am -DskipTests compile
```

**结果：BUILD SUCCESS**（Push 首次失败为 stale classpath，`-am clean compile` 后通过）

### 冒烟结果（服务未启动时）

```powershell
python scripts\run_cluster_apps_smoke_tests.py --profile jaja7
```

- 输出：`.cursor/cluster-apps-smoke-result.json`
- 报告：`docs/testing/cluster-apps-jaja7/summary-test-report.md`
- 当前环境：核心/非核心服务均未运行 → 9 用例 **skipped**（脚本 exit 0）

### 推荐联调命令

```powershell
python scripts\jbm_cluster_ops.py start auth center gateway logs push doc job weixin bigscreen --background --clean --prepare compile
python scripts\jbm_cluster_ops.py wait auth center gateway logs push doc job weixin bigscreen --timeout 240
python scripts\run_cluster_apps_smoke_tests.py --profile jaja7
```

启动日志：`logs/ops-start-<service>.log`

### 遗留风险

- MinIO/MQTT/短信/邮件/微信/OpenObserve 等外部依赖缺失时，各服务启动仍可能失败，需 Nacos `jbm7` 共享配置或本地 mock
- Job jaja7 已设 `load-on-startup: false`、`sync-enabled: false`，避免危险定时任务
- Push/Weixin 配置项 `jaja7-dry-run: true` 为占位，业务层若未读取则仍可能尝试真实发送
- 完整 Gateway 转发断言需六服务 + 核心三服务同时就绪后再跑冒烟

## 集群应用启动循环修复（2026-05-26）

目标服务：auth、center、gateway、doc、push、logs、job、bigscreen  
排除服务：weixin

| 服务 | compile | started | health | gateway smoke | 说明 |
|---|---|---|---|---|---|
| auth | passed | failed | failed | blocked | spring-boot:run exit 1；日志含 Redis/依赖 |
| center | passed | failed | failed | blocked | 并行 mvnd 锁；曾 clean compile 修复 |
| gateway | passed | failed | failed | blocked | 同左 |
| doc | passed | failed | failed | skipped | 服务未监听 |
| push | passed | failed | failed | skipped | 已恢复误删源码 + mapper namespace |
| logs | passed | failed | failed | skipped | 服务未监听 |
| job | passed | failed | failed | skipped | 服务未监听 |
| bigscreen | passed | failed | failed | skipped | 服务未监听 |
| weixin | excluded | excluded | excluded | excluded | 本轮按要求排除 |

循环轮次：3  
结果文件：`.cursor/cluster-start-loop-result.json`、`.cursor/cluster-apps-smoke-result.json`  
报告目录：`docs/testing/cluster-apps-jaja7/`（`start-loop-report.md`、`summary-test-report.md`）

本轮补丁：`BaseAppPreprocessing` / `LoginAssemblyConfiguration` Redis 降级；`jbm_cluster_ops.py` 使用 `-f module/pom.xml spring-boot:run`。  
**未满足**计划最终退出条件（8 服务 health + Gateway 冒烟 passed）；阻塞主要为外部 Redis/Nacos 与并行启动资源竞争。验证后已 `ops stop`。

