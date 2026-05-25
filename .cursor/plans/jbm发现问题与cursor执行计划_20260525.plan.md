---
name: JBM 发现问题与 Cursor 执行计划
overview: 整理 Codex 本轮代码审查、编译、页面实测发现的问题，交给 Cursor 按修复、构建、启动、页面实测、结果落盘的闭环执行。重点判断字段管理、字典类型、网关 SQL、注册登录、多租户隔离、API Key 授权、首页与 API Wiki 的合理性和 BUG。
todos:
  - id: review-current-diff
    content: 先读取当前 git diff，确认 Cursor 已做改动、Codex 已做改动和用户改动，不要回退无关变更。
    status: completed
  - id: fix-extend-field-group-detail
    content: 修复扩展字段管理页：左侧字段组列表点击后右侧必须展示字段定义，字段组项使用 button，保存后刷新列表并保持选中。
    status: completed
  - id: verify-extend-field-e2e
    content: 执行字段管理专项页面实测：管理员登录、进入 /system/extend-fields、点击字段组、断言右侧字段定义出现，截图和 JSON 结果落盘。
    status: completed
  - id: fix-dict-id-type-contract
    content: 修复字典页 TypeScript 构建失败：统一 BaseDic 的 id/dicId/parentId 类型或做 API normalize，确保 npm run build 通过。
    status: completed
  - id: verify-gateway-management
    content: 回归管理员网关管理：路由、限流、IP 限制页面均完成新增、筛选、编辑、删除，避免 MyBatis-Plus 继承列导致 SQL 错误。
    status: completed
  - id: verify-register-login-flow
    content: 从普通用户视角实测 /register 到 /login 到 /dashboard，确认 RSA 加密密码注册后能正常登录。
    status: completed
  - id: verify-user-admin-access-boundary
    content: 回归普通用户、开发者、管理员菜单与按钮权限边界：未审批不能创建 API Key，管理员才能审批和管理网关。
    status: completed
  - id: verify-tenant-isolation
    content: 补测多租户隔离：不同租户字段组、API Key、网关策略互不可见，API Key 只能访问授权范围。
    status: completed
  - id: check-utf8-data-encoding
    content: 检查并修复中文乱码风险：数据库字符集、连接、响应头、自动化测试写入方式，清理乱码测试数据。
    status: completed
  - id: improve-home-docs-openapi
    content: 按开源社区平台风格检查首页、Logo、登录注册入口和 API Wiki，从用户接入 JBM 的 0 到 1 路径补齐文档。
    status: completed
  - id: build-restart-full-e2e-loop
    content: 执行前端构建、后端目标模块构建、重启本地 Auth/Center/Gateway，然后循环跑字段管理和用户/管理员 E2E，失败则继续修复。
    status: completed
  - id: update-verification-summary
    content: 更新 .cursor/jbm-verification-summary.md，记录修复项、未修复风险、命令结果、截图路径和 JSON 结果路径。
    status: completed
isProject: false
---

# JBM 发现问题与 Cursor 执行计划

日期：2026-05-25  
根目录：`D:\workspaces\JBM7`

## 执行原则

Cursor 执行时不要只跑构建。每个 P0 问题都必须按下面闭环推进：

1. 读代码和当前 diff。
2. 修复问题。
3. 前端/后端构建。
4. 重启本地服务。
5. 用浏览器或 Playwright 从页面真实操作。
6. 截图和 JSON 结果落盘。
7. 失败则继续修复并重复。

不要回退用户已有改动；如果遇到不相关 dirty 文件，只记录并跳过。

## P0 确认 BUG

### 1. 扩展字段管理点击字段组右侧不显示

判断：确认 BUG。

现象：`/system/extend-fields` 左侧字段组点击后，右侧没有可靠展示字段定义。原页面更像“输入 formCode 加载”，不是“字段组列表 -> 字段详情”的管理界面。

建议修复：

- 后端补齐 `GET /extend-field/forms`，按当前租户查询字段组列表。
- 前端 `src/api/extendField.ts` 增加 `listExtendForms()`。
- 前端 `src/views/system/ExtendFieldList.vue` 改成左侧字段组列表 + 右侧字段详情。
- 字段组项使用 `<button type="button">`，不要只在 `li` 上绑定 click。
- 点击字段组时调用 `GET /extend-field/forms/{formCode}` 并填充右侧 `formCode/formName/customFormId/version/tenantId/fields`。
- 保存后刷新字段组列表，并保持当前选中项。

验收：

- 管理员登录后进入 `/system/extend-fields`。
- 左侧能看到字段组列表。
- 点击 `cen_form_*` 或新建字段组，右侧显示字段定义表格，能看到字段名和标签。
- 截图：`.cursor/screenshots/e2e-extend-fields-group-detail.png`
- 结果：`.cursor/e2e-extend-fields-group-detail-result.json`，`status` 必须是 `passed`。

参考已跑过的结果：

- `.cursor/e2e-extend-fields-group-detail-result.json`
- `.cursor/screenshots/e2e-extend-fields-group-detail.png`

### 2. 字典页 ID 类型导致 TypeScript 构建失败

判断：确认 BUG。

现象：`npm run build` 曾在 `src/views/system/DictList.vue` 报 `number` 与 `string | number` 不匹配。后端实际可能返回字符串 ID，前端类型仍按纯 `number` 使用。

建议修复：

- 统一 `BaseDic.id/dicId/parentId` 为 `number | string`，或在 `src/api/dict.ts` 做 normalize。
- 检查 `pageDictItems/saveDict/deleteDict` 调用，避免编辑、删除、筛选失效。

验收：

- `cd jbm-admin-vue && npm run build` 通过。
- 字典页能选择左侧分组，右侧能加载字典项。

### 3. 网关管理 SQL 查询继承列不匹配

判断：确认 BUG。

现象：`gateway_route/gateway_rate_limit/gateway_ip_limit` 表不具备完整 `MasterDataEntity` 继承列，默认 MyBatis-Plus 查询可能带出不存在字段，触发 `Unknown column 'id' in 'field list'`。

建议修复：

- `GatewayRouteServiceImpl.findListPage/getRoute` 显式 select 路由表真实字段。
- `GatewayRateLimitServiceImpl.findListPage/getRateLimitPolicy` 显式 select 限流表真实字段。
- `GatewayIpLimitServiceImpl.findListPage/getIpLimitPolicy` 显式 select IP 限制表真实字段。
- 全局审查继承了基类但物理表缺列的实体。

验收：

- `/gateway/routes` 完成新增、筛选、编辑、删除。
- `/gateway/rate-limit` 完成新增、筛选、编辑、删除。
- `/gateway/ip-limit` 完成新增、筛选、编辑、删除。
- `.cursor/jbm-user-admin-e2e-result.json` 状态为 `passed`。

### 4. 注册后登录密码链路风险

判断：确认 BUG，需要回归。

现象：注册页发送 RSA 加密密码，但 jaja7 明文登录白名单场景下，后端可能把 RSA 密文当密码保存，导致注册后无法登录。

建议修复：

- 注册接口保存密码前识别 RSA 密文并解密。
- 不要让登录白名单或明文登录兼容逻辑影响注册入库。

验收：

- 新用户从 `/register` 注册。
- 注册成功跳转登录页。
- 使用刚注册的账号密码登录 `/dashboard` 成功。

## P1 合理优化和平台能力缺口

### 5. 扩展字段列表需要分页和搜索

判断：合理优化。

当前全量 `GET /extend-field/forms` 短期可用，但字段组多时不适合。建议增加 `keyword/page/pageSize`，并确保查询带租户过滤。

### 6. 中文数据编码污染

判断：确认存在数据污染，根因待验证。

现象：页面/API 曾出现 `CEN��`、`UI���`、`�ͻ��ȼ�` 一类乱码。

建议：

- 检查数据库、表、列字符集是否为 `utf8mb4`。
- 检查 JDBC 连接、响应头、前端请求是否 UTF-8。
- 自动化测试不要用 Windows 非 UTF-8 shell 直接写中文；中文专项测试用 UTF-8 文件执行。
- 清理测试污染数据，例如 `ui_fields_82227436` 和乱码 `cen_form_*` 记录。

验收：

- 页面新建中文字段组“客户表单”、字段“客户等级”，刷新后仍正常显示。
- API JSON 中文不乱码。

### 7. 多租户与数据隔离专项回归

判断：平台能力必要验证。

建议补测：

- 创建租户 A、租户 B。
- 分别创建字段组、API Key、网关路由/限流策略。
- 租户 A 不能看到或访问租户 B 的字段组、API Key、网关策略。
- API Key 只能访问授权应用和授权范围。

验收：

- 增加多租户 E2E 脚本。
- JSON 结果明确断言“跨租户不可见/不可访问”。

### 8. 普通用户、开发者、管理员权限边界

判断：确认曾有 BUG，需要回归。

验收：

- 普通注册用户只看到仪表盘、开放平台、API Wiki、自助开发者/API Key 入口。
- 未审批开发者不能创建 API Key。
- 管理员才能审批开发者、管理网关、管理系统基础数据。

## P2 体验和文档

### 9. JBM 首页与 Logo

判断：合理优化。

要求：

- 首页第一屏明确 JBM 是开源开放平台。
- Logo 使用统一 JBM 图形资产，不突出展示“JB”。
- 登录、注册、文档、GitHub/OpenAPI 入口清晰。
- 首屏能进入实际接入流程，不做空营销页。

### 10. API Wiki/OpenAPI 文档

判断：合理优化。

文档要从用户接入角度组织：

- 注册账号。
- 登录并申请成为开发者。
- 等待管理员审批。
- 创建 API Key。
- 查看一次性 Secret。
- 使用 API Key 调用开放接口。
- 管理员配置网关路由、限流、IP 限制。
- 常见错误：未审批、API Key 禁用、IP 不在白名单、限流触发、跨租户访问。

## Cursor 推荐执行命令

```powershell
cd D:\workspaces\JBM7
git status --short
```

```powershell
cd D:\workspaces\JBM7\jbm-admin-vue
npm.cmd run build
```

```powershell
cd D:\workspaces\JBM7
mvn -pl jbm-cluster/jbm-cluster-api/jbm-cluster-api-basic,jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am -DskipTests install
```

```powershell
cd D:\workspaces\JBM7
python scripts\jbm_cluster_ops.py restart
```

```powershell
cd D:\workspaces\JBM7
node .cursor\e2e-extend-fields-group-detail.cjs
node .cursor\jbm-user-admin-e2e.cjs
```

## 最终交付要求

- 明确列出修复了哪些 BUG。
- 明确列出哪些只是合理优化或待验证风险。
- 提供构建命令和实测命令结果。
- 提供截图路径和 JSON 结果路径。
- 如果多租户、加密传输、API Key 授权仍有缺口，必须单独列为后续 P0/P1，不能用“构建通过”替代功能通过。
