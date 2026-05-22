# jbm-admin-vue

JBM7 集群管理前端：Vue 3 + Vite + TypeScript + Tailwind CSS + shadcn 风格组件，对接 **jaja7** 本地环境。

## 前置条件

本地需已启动（`spring.profiles.active=jaja7`）：

| 服务 | 端口 |
|------|------|
| Gateway | 7777 |
| Auth | 5555 |
| Center | 8888 |

## 开发

```bash
cd jbm-admin-vue
npm install --registry https://registry.npmjs.org
npm run dev
```

浏览器访问 http://localhost:5173

### 默认登录

- OAuth2 密码模式：`POST /oauth2/token`
- 默认客户端：`demo` / `demo123`（与 `scripts/user_perm_rest_modules.json` 一致）
- 用户名/密码：使用环境中已有的管理员账号（如 `admin`）

### API 代理

`vite.config.ts` 中：

- 经 Gateway（7777）：`/oauth2`、`/user`、`/current`、`/authority`、`/role`、`/menu` 等
- 直连 Center（8888）：`/app`、`/gateway`、`/baseDic`、`/developer`、`/baseOrg` 等

## 功能模块

- 登录 / Token 刷新 / 登出
- 仪表盘（用户统计）
- 系统：用户、角色、菜单、组织、权限、应用、字典
- 网关：路由、限流、IP 限制
- 审计日志、开发者列表

## 构建

```bash
npm run build
```

## 技术栈

- Vue 3 + Vue Router + Pinia
- Axios（`Authorization: Bearer` + `tenantId`）
- Tailwind CSS v4
- Lucide 图标（`@lucide/vue`）
- Reka UI（依赖预留，UI 为 shadcn 风格自研组件）
