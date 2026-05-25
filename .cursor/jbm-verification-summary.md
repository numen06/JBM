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

- 数据库：`V16__base_org_utf8mb4.sql` 将 `base_org` 转为 `utf8mb4`
- API：扩展字段「客户表单」「客户等级」经 Gateway JSON 读写正常（见 `e2e-utf8-cn-result.json`）
- **已知风险**：历史 E2E 种子 `ui_fields_82227436` 等存在乱码显示；`verify_org_save_cn.py` 因 Center 运行时 `NoClassDefFoundError: com/jbm/util/CollectionUtils`（`mvn spring-boot:run` 启动）暂无法验证 `/baseOrg/tree`，需 IDE 直接调试 Center 或补齐 classpath 后复测

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

## 后续 P0/P1 待办（未用「构建通过」替代）

1. **组织 API 运行时 classpath**：修复 `mvn spring-boot:run` 下 `/baseOrg/*` 的 `CollectionUtils` NoClassDefFoundError，并复跑 `verify_org_save_cn.py`
2. **清理乱码测试数据**：删除或重命名 `ui_fields_*`、乱码 `cen_form_*` 扩展字段组
3. **多租户 API Key / 网关策略隔离**：当前脚本仅覆盖扩展字段；API Key 授权范围与网关策略跨租户 REST 断言可扩展 `run_tenant_isolation_tests.py`
