# JBM 本轮验证与修复记录

时间：2026-05-25

## 2026-05-25 继续验证：Cursor 新增功能 + 用户/管理员实测

### 代码审查范围

- 前端新增/增强：开发者、API Key、应用、角色、网关路由、限流、IP 限制、组织、菜单筛选；新增在线用户页。
- 后端新增/增强：应用/开发者/角色/网关相关列表筛选与排序。
- 重点风险：网关管理表 `gateway_route`、`gateway_rate_limit`、`gateway_ip_limit` 并不完整具备 `MasterDataEntity` 的继承列，使用默认 MyBatis-Plus 查询会把 `id/code/app_id/parent_id/...` 带入 SQL。

### 编译验证

- 前端：`npm run build` PASS。
- 后端：`mvn -pl jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am -DskipTests install` PASS。
- 集群：Auth `5555`、Center `8888`、Gateway `7777` 重启后健康。

### 页面实测结果

本轮通过本地 Chrome + Playwright 从页面真实操作完成，结果文件：

- `.cursor/jbm-user-admin-e2e-result.json`

通过步骤：

1. 普通用户从 `/register` 注册：`jbm_ui_81168364`。
2. 使用注册账号登录进入 `/dashboard`。
3. 普通用户进入 `/developer` 提交开发者申请。
4. 未审批前进入 `/developer/api-keys`，验证“新建 API Key”禁用。
5. 管理员 `admin / Admin@123` 登录并在开发者页面审批该用户。
6. 用户再次登录后创建 API Key，Secret 一次性展示正常。
7. 管理员进入 `/system/online-users`，在线用户页面加载正常。
8. 管理员进入 `/gateway/routes`，完成路由创建、筛选、编辑、删除。
9. 管理员进入 `/gateway/rate-limit`，完成限流策略创建、筛选、编辑、删除。
10. 管理员进入 `/gateway/ip-limit`，完成 IP 限制策略创建、筛选、编辑、删除。

### 本轮修复

- 修复 `GatewayRouteServiceImpl.findListPage/getRoute`：显式选择 `gateway_route` 真实存在字段，避免查询继承列导致 `Unknown column 'id' in 'field list'`。
- 修复 `GatewayRateLimitServiceImpl.findListPage/getRateLimitPolicy`：显式选择 `gateway_rate_limit` 真实字段。
- 修复 `GatewayIpLimitServiceImpl.findListPage/getIpLimitPolicy`：显式选择 `gateway_ip_limit` 真实字段。
- 增加 `.cursor/jbm-user-admin-e2e.cjs`，用于可重复执行从用户注册到管理员网关管理的页面级 E2E 验证。

### 本轮截图

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

## 本轮页面实测路径

从匿名用户视角完整走了一遍：

1. 打开 `/register`，注册账号 `jbm_ui_70664079`。
2. 注册成功跳转 `/login?username=jbm_ui_70664079`。
3. 使用注册密码 `UiTest@123456` 登录成功进入 `/dashboard`。
4. 普通用户进入控制台，仅展示“仪表盘 / 开放平台 / API Wiki”自助入口。
5. 在 `/developer` 提交“申请成为开发者”，页面提示“申请已提交，请等待管理员审批”。
6. 未审批时进入 `/developer/api-keys`，页面提示待审批，`新建 API Key` 禁用。
7. 使用管理员 `admin / Admin@123` 登录，在开发者页审批该用户。
8. 回到 `jbm_ui_70664079`，`新建 API Key` 启用，创建 `ui-key-468389` 成功，并展示一次性 Secret。

## 发现并修复的问题

- 注册后无法登录：前端注册发送 RSA 加密密码，但 jaja7 明文登录白名单导致后端直接把 RSA 密文当密码保存。已修复为“看起来是 RSA 密文时仍执行解密”。
- 普通注册用户菜单过宽：菜单加载失败/为空时前端回退全量后台菜单，且 `rawMenus >= 10` 被误判为全权限。已改为普通用户只显示自助接入入口。
- 开发者页越权展示：普通用户能看到全部开发者、待审批列表和管理操作。已改为只有后台授权用户显示管理列表，普通用户只显示申请入口。
- API Key 页误导：待审批用户能看到可点击的“新建 API Key”。已加开发者状态检查，只有审批通过后才能创建。
- Vite 深链冲突：直接访问 `/developer` 被代理到后端 API，返回“缺少签名参数”。已加 HTML 导航 bypass，让 SPA 路由优先返回 `index.html`。
- 审批按钮原生确认框会卡住自动化。已去掉原生 `confirm`，改为直接审批并刷新列表。

## 回归结果

- 前端构建：`npm run build` PASS。
- 后端 API Key 全链路：`python scripts/run_api_key_flow_tests.py` PASS。
- 集群服务：Auth `5555`、Center `8888`、Gateway `7777` 均已启动并参与测试。

## 截图

- `.cursor/screenshots/e2e-admin-approve.png`
- `.cursor/screenshots/e2e-user-apikey-enabled.png`
- `.cursor/screenshots/e2e-user-apikey-created.png`
- `.cursor/screenshots/e2e-apikey-dialog-debug.png`
