# jbm-admin-vue

JBM7 集群管理前端：Vue 3 + Vite + TypeScript + Tailwind CSS + shadcn 风格组件，对接 **jaja7** 本地环境。

## 前置条件

须**同时**启动三个服务（`spring.profiles.active=jaja7`，Nacos 命名空间 **jbm7**）：

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 7777 | 对外统一入口，前端只连此端口 |
| Auth | 5555 | OAuth2、验证码（经 Gateway 转发） |
| Center | 8888 | 业务 API（经 Gateway 转发） |

VS Code：运行 **「jaja7: Auth + Center + Gateway」** 复合启动（`.vscode/launch.json`）。

Maven 打包/过滤资源：`mvn -Pjaja7 ...`（`jbm-cluster/pom.xml` 中 `config.namespace=jbm7`）。

## 开发

```bash
cd jbm-admin-vue
npm install --registry https://registry.npmjs.org
npm run dev
```

浏览器访问 http://localhost:5173

### 默认登录

- OAuth2 密码模式：`POST /oauth2/token`（经 Gateway）
- 图形验证码：`GET /captcha/vcode64`，登录表单传 `vcode`
- 默认客户端：`demo` / `demo123`
- 管理员示例：`admin` / `Admin@123`（以环境为准）

### API 代理

`vite.config.ts` 中**全部**前缀代理到 Gateway `http://127.0.0.1:7777`，不直连 Auth/Center。

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
