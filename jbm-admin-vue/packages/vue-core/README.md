# @jbm7/vue-core

JBM 7.3 的 Vue 3 插件、权限守卫和前端模块注册契约。业务扩展只依赖这里的公共类型，不引用 JBM 管理端内部源码。

```ts
import { defineJbmModule } from '@jbm7/vue-core'

export const industryModule = defineJbmModule({
  id: 'example.industry',
  version: '1.0.0',
  routes: [{ path: 'industry', name: 'example-industry', component: IndustryPage }],
})
```

模块 ID、路由名和菜单码应使用稳定命名空间；平台与行业包独立版本、独立发布。
