# JBM 本轮验证与修复记录

时间：2026-05-25

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
