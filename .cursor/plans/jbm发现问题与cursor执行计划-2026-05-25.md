# JBM 发现问题与 Cursor 执行计划

日期：2026-05-25  
目标：把本轮代码审查、编译、页面实测发现的问题整理给 Cursor 执行。Cursor 执行时不要只跑构建，必须按“修复 -> 启动服务 -> 页面真实操作 -> 结果落盘”的循环推进。

## 总体判断

本轮升级方向整体合理：前端从纯后台管理补齐到“开源平台 + 用户自助接入 + 管理员治理”，后端也在补齐多租户、开发者、API Key、网关治理、扩展字段等平台能力。

但当前主要问题不是单点 UI，而是前后端契约、数据类型、租户隔离和页面实测闭环不够稳。下面按优先级执行。

## P0：确认 BUG，优先修复

### 1. 扩展字段管理：点击字段组右侧不显示

现象：`/system/extend-fields` 左侧字段组点击后，右侧没有可靠展示字段定义。  
判断：确认 BUG。原页面偏“输入 formCode 加载”，缺少完整的“字段组列表 -> 右侧详情”交互模型。

建议修复：

- 后端补齐 `GET /extend-field/forms`，按当前租户查询字段组列表，按 `update_time desc, id desc` 排序。
- 前端 `src/api/extendField.ts` 增加 `listExtendForms()`。
- 前端 `src/views/system/ExtendFieldList.vue` 改成左侧字段组列表 + 右侧字段详情。
- 点击字段组时调用 `GET /extend-field/forms/{formCode}`，右侧填充 `formCode/formName/customFormId/version/tenantId/fields`。
- 字段组项必须使用真实 `<button type="button">`，不要只给 `li` 绑定 click，保证可访问性和自动化测试稳定。
- 保存后刷新字段组列表，并保持当前选中项。

验收：

- 管理员登录后进入 `/system/extend-fields`。
- 左侧能看到字段组列表。
- 点击 `cen_form_*` 或新建字段组，右侧显示字段定义表格，能看到字段名和标签。
- 截图保存到 `.cursor/screenshots/e2e-extend-fields-group-detail.png`。
- 结果保存到 `.cursor/e2e-extend-fields-group-detail-result.json`，状态必须是 `passed`。

参考已跑过的实测结果：

- `.cursor/e2e-extend-fields-group-detail-result.json`
- `.cursor/screenshots/e2e-extend-fields-group-detail.png`

### 2. 字典页 TypeScript 构建失败：ID 类型契约不一致

现象：`npm run build` 曾在 `src/views/system/DictList.vue` 报错，核心是 `number` 与 `string | number` 不匹配。  
判断：确认 BUG。后端实际可能返回字符串 ID，前端类型仍按纯 number 使用，升级后契约不一致。

建议修复：

- 在 `src/api/types.ts` 中将 `BaseDic.id/dicId/parentId` 明确统一为 `number | string`，或在 `src/api/dict.ts` 统一 normalize 成 number。
- 二选一即可，但全项目要一致。更稳妥是保持 `number | string` 并通过 `dicId(row)` helper 传递。
- 检查所有调用 `pageDictItems/saveDict/deleteDict` 的地方，避免强转导致筛选、删除、编辑失效。

验收：

- `cd jbm-admin-vue && npm run build` 必须通过。
- 字典页能选中左侧分组，右侧能加载字典项。

### 3. 网关管理 SQL 查询继承列不匹配

现象：网关表 `gateway_route/gateway_rate_limit/gateway_ip_limit` 不具备完整 `MasterDataEntity` 继承列，默认 MyBatis-Plus 查询会带出不存在字段，出现 `Unknown column 'id' in 'field list'` 一类错误。  
判断：确认 BUG，之前页面实测网关管理时触发过。

建议修复：

- `GatewayRouteServiceImpl.findListPage/getRoute` 显式 select 网关路由真实字段。
- `GatewayRateLimitServiceImpl.findListPage/getRateLimitPolicy` 显式 select 限流表真实字段。
- `GatewayIpLimitServiceImpl.findListPage/getIpLimitPolicy` 显式 select IP 限制表真实字段。
- 顺手全局审查：凡是继承了基类但物理表缺列的实体，都要显式列选择或调整实体映射。

验收：

- 管理员页面进入 `/gateway/routes`，完成新增、筛选、编辑、删除。
- 管理员页面进入 `/gateway/rate-limit`，完成新增、筛选、编辑、删除。
- 管理员页面进入 `/gateway/ip-limit`，完成新增、筛选、编辑、删除。
- `.cursor/jbm-user-admin-e2e-result.json` 状态必须是 `passed`。

### 4. 注册后登录密码链路风险

现象：注册页发送 RSA 加密密码，但 jaja7 明文登录白名单场景下，后端可能把 RSA 密文当作密码保存，导致注册后无法登录。  
判断：确认 BUG，属于用户从 0 到 1 接入的阻断点。

建议修复：

- 注册接口保存密码前识别 RSA 密文并解密。
- 不要让“登录白名单/明文登录兼容”影响注册密码入库逻辑。

验收：

- 新用户从 `/register` 注册。
- 注册成功跳转登录页。
- 使用刚注册的账号密码登录 `/dashboard` 成功。

## P1：合理优化，影响平台能力完整性

### 5. 扩展字段列表需要分页/搜索，不要长期全量拉取

现状：字段组列表如果直接 `GET /extend-field/forms` 全量返回，短期可用，但长期租户字段组多时不适合。  
判断：合理优化。

建议：

- 增加分页接口或支持 `keyword/page/pageSize`。
- 前端左侧字段组搜索应优先走服务端搜索；数据少时可先本地过滤，但接口设计要预留。
- 查询必须带租户过滤，不能跨租户返回。

### 6. 中文数据存在编码污染，需要清理并验证 UTF-8

现象：页面和接口里出现过 `CEN��`、`UI���`、`�ͻ��ȼ�` 这类乱码。部分可能来自 PowerShell/测试脚本编码，部分可能已经写入数据库。  
判断：确认存在数据污染，根因待 Cursor 验证。

建议：

- 检查数据库连接、表字符集、列字符集是否为 `utf8mb4`。
- 检查前端请求头、后端响应头是否 UTF-8。
- 不要用 Windows 非 UTF-8 shell 直接写中文测试数据；自动化测试优先用 ASCII，中文专项测试用 UTF-8 文件执行。
- 清理测试污染数据，例如 `ui_fields_82227436` 和乱码 `cen_form_*` 测试记录。

验收：

- 页面新建中文字段组“客户表单”、字段“客户等级”，刷新后仍正常显示。
- API 返回 JSON 中中文不乱码。

### 7. 多租户与数据隔离需要专项回归

现状：平台目标包含多租户、子应用接入、客户端授权访问、数据隔离、加密传输，但页面测试主要覆盖了默认租户/管理员路径。  
判断：合理性要求，属于平台能力必要验证。

建议补测：

- 创建租户 A、租户 B。
- 分别创建字段组、API Key、网关路由/限流策略。
- 用租户 A 登录不可看到租户 B 的字段组、API Key、网关策略。
- API Key 访问只能访问授权应用/授权范围。
- 管理员页面的列表接口都必须带租户过滤。

验收：

- 补充 `.cursor/plans` 或 `.cursor` 下的多租户 E2E 脚本。
- 结果 JSON 里明确列出“跨租户不可见/不可访问”断言。

### 8. 普通用户与管理员菜单权限边界

现象：之前普通用户曾看到过过多后台菜单，开发者管理页也曾暴露审批/列表能力。  
判断：确认 BUG 已修过一轮，但需要回归。

建议：

- 普通注册用户只看到仪表盘、开放平台、API Wiki、自助开发者入口/API Key 入口。
- 未审批开发者不能创建 API Key。
- 管理员才能审批开发者、管理网关、管理系统基础数据。

验收：

- 普通用户登录后截图保存。
- 管理员登录后网关/系统菜单可见。
- 未审批用户 API Key 新建按钮禁用。
- 审批后 API Key 新建按钮启用。

## P2：体验与文档完善

### 9. JBM 首页与开源社区风格要保持一致

现状：登录/首页已有重设计诉求，但仍需用用户视角走一遍。  
判断：合理优化。

建议：

- 首页第一屏明确 JBM 是开源开放平台。
- Logo 使用统一的 JBM 图形资产，不要使用“JB”突出展示。
- 登录、注册、文档、GitHub/OpenAPI 入口清晰。
- 不要做营销式空页面，首屏要能进入实际接入流程。

验收：

- 打开 `/` 能进入社区平台首页。
- `/login`、`/register`、`/docs` 都可达。
- Logo 在侧栏、登录页、首页尺寸和形态统一。

### 10. API Wiki/OpenAPI 文档需要从用户接入角度补齐

现状：需要围绕 JBM OpenAPI 支持“从注册登录开始接入 JBM”。  
判断：合理优化。

建议文档结构：

- 注册账号。
- 登录并申请成为开发者。
- 等待管理员审批。
- 创建 API Key。
- 查看 API Key/Secret。
- 使用 API Key 调用开放接口。
- 管理员配置网关路由、限流、IP 限制。
- 常见错误：未审批、API Key 禁用、IP 不在白名单、限流触发、跨租户访问。

验收：

- `/docs` 页面按用户接入路径组织。
- 提供 OpenAPI 概念和示例请求。
- 不暴露真实 Secret，只展示一次性 Secret 的安全提示。

## Cursor 执行顺序

1. 先读当前 diff，不要覆盖用户和 Codex 已做改动：
   - `git status --short`
   - `git diff -- jbm-admin-vue/src/views/system/ExtendFieldList.vue`
   - `git diff -- jbm-admin-vue/src/views/system/DictList.vue jbm-admin-vue/src/api/types.ts`
   - `git diff -- jbm-cluster/**/Gateway*ServiceImpl.java`

2. 编译验证：
   - `cd jbm-admin-vue && npm run build`
   - `mvn -pl jbm-cluster/jbm-cluster-api/jbm-cluster-api-basic,jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am -DskipTests install`

3. 启动本地服务：
   - `python scripts/jbm_cluster_ops.py restart`
   - 确认 Auth `5555`、Center `8888`、Gateway `7777` 都可用。

4. 执行字段管理专项实测：
   - `node .cursor/e2e-extend-fields-group-detail.cjs`
   - 如果脚本不存在，按本计划的 P0-1 验收步骤补一个 Playwright 脚本。

5. 执行用户从 0 到 1 接入与管理员网关管理回归：
   - `node .cursor/jbm-user-admin-e2e.cjs`

6. 发现失败就修复后重复第 2-5 步，直到页面真实流程通过。

7. 更新结果文件：
   - `.cursor/e2e-extend-fields-group-detail-result.json`
   - `.cursor/jbm-user-admin-e2e-result.json`
   - `.cursor/jbm-verification-summary.md`

## 最终交付要求

- 明确列出修复了哪些 BUG。
- 明确列出哪些只是合理优化，没有立刻做。
- 提供编译命令和实测命令的结果。
- 提供截图路径和 JSON 结果路径。
- 如果发现多租户、加密传输、API Key 授权仍有缺口，必须单独列为后续 P0/P1，不要用“构建通过”替代功能通过。
