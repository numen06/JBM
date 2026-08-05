# @jbm7/admin

JBM 7.3 基础技术平台管理页面。可使用 `@jbm7/admin/shell` 启动标准管理端，也可按子路径把认证、系统、权限、网关、日志、消息、任务、文档和 OpenAPI 模块注册到自有 Vue 3 宿主。

```bash
npm install @jbm7/admin @jbm7/sdk @jbm7/vue-core vue vue-router pinia
```

```ts
import { createJbmAdminApp } from '@jbm7/admin/shell'
import '@jbm7/admin/style.css'

await createJbmAdminApp({ mount: '#app', base: '/' })
```

下游行业应用通过 `modules` 注册自己的路由与导航；IoT、建筑等行业模型和页面不进入本包。
