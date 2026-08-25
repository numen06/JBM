import { createJbmAdminApp } from '@jbm7/admin/shell'
import '@jbm7/admin/style.css'
import { defineJbmModule } from '@jbm7/vue-core'
import { defineComponent, h } from 'vue'
import './style.css'

const DemoPage = defineComponent({
  name: 'DownstreamDemoPage',
  setup: () => () => h('main', { class: 'downstream-demo' }, [
    h('p', { class: 'downstream-demo__tag' }, 'npm 集成验证'),
    h('h1', '下游模块已接入 JBM'),
    h('p', '该页面由下游系统注册，登录、权限、导航和基础平台页面由 @jbm7/admin 提供。'),
    h('dl', [
      h('div', [h('dt', 'SDK'), h('dd', '@jbm7/sdk')]),
      h('div', [h('dt', 'Vue 运行时'), h('dd', '@jbm7/vue-core')]),
      h('div', [h('dt', '管理平台'), h('dd', '@jbm7/admin')]),
    ]),
  ]),
})

const downstreamModule = defineJbmModule({
  id: 'demo.downstream',
  version: '1.0.0',
  routes: [
    {
      path: 'downstream-demo',
      name: 'downstream-demo',
      component: DemoPage,
      meta: { title: '下游集成 Demo', public: true },
    },
  ],
  navigation: [
    {
      label: '下游示例',
      items: [{ name: '集成验证', title: '下游集成 Demo', to: '/downstream-demo' }],
    },
  ],
})

await createJbmAdminApp({
  mount: '#app',
  runtimeConfig: { apiBaseUrl: '/' },
  modules: [downstreamModule],
})
