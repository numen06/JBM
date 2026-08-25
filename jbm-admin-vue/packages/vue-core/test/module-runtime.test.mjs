import assert from 'node:assert/strict'
import test from 'node:test'
import { createSSRApp, h } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { defineJbmModule, JbmSidebarNavigation, JbmWorkspaceNavigation, registerJbmModules } from '../dist/index.js'

test('module id must be namespaced', () => {
  assert.throws(() => defineJbmModule({ id: 'gateway', version: '1.0.0', routes: [] }), /namespaced id/)
})

test('duplicate route names fail fast', () => {
  const router = {
    getRoutes: () => [],
    addRoute: () => undefined,
  }
  const modules = [
    defineJbmModule({ id: 'test.one', version: '1.0.0', routes: [{ path: '/a', name: 'same', component: {} }] }),
    defineJbmModule({ id: 'test.two', version: '1.0.0', routes: [{ path: '/b', name: 'same', component: {} }] }),
  ]
  assert.throws(() => registerJbmModules({ router, modules }), /Duplicate JBM route name/)
})

test('navigation is the intersection of installed and authorized menus', () => {
  const router = { getRoutes: () => [], addRoute: () => undefined }
  const module = defineJbmModule({
    id: 'test.nav',
    version: '1.0.0',
    routes: [],
    navigation: [{ label: 'Test', items: [
      { name: 'allowed', title: 'Allowed', to: '/allowed', menuCodes: ['allowed'] },
      { name: 'hidden', title: 'Hidden', to: '/hidden', menuCodes: ['hidden'] },
    ] }],
  })
  const registry = registerJbmModules({ router, modules: [module] })
  assert.deepEqual(registry.navigation(new Set(['allowed']))[0].items.map((item) => item.name), ['allowed'])
})

test('shared shell navigation renders workspaces, icons and active menu state', async () => {
  const Icon = { render: () => h('svg', { 'data-test-icon': '' }) }
  const app = createSSRApp({
    render: () => h('aside', [
      h(JbmWorkspaceNavigation, {
        currentKey: 'iot',
        workspaces: [{ key: 'iot', label: 'IoT 数据工作区', shortLabel: 'IoT', path: '/iot/console', icon: Icon }],
      }),
      h(JbmSidebarNavigation, {
        groups: [{ label: '工作台', items: [{ path: '/iot/console', label: '总览', icon: Icon, active: true }] }],
      }),
    ]),
  })
  const html = await renderToString(app)
  assert.match(html, /jbm-workspaces__item is-active/)
  assert.match(html, /data-test-icon/)
  assert.match(html, /jbm-sidebar-nav__item is-active/)
  assert.match(html, /aria-current="page"/)
})

test('shared shell navigation keeps only the most specific active menu item', async () => {
  const app = createSSRApp({
    render: () => h(JbmSidebarNavigation, {
      groups: [{ label: '消息管理', items: [
        { path: '/jbm/messages', label: '消息记录', active: true },
        { path: '/jbm/messages/webhook-configs', label: '事件订阅配置', active: true },
      ] }],
    }),
  })
  const html = await renderToString(app)
  assert.equal((html.match(/aria-current="page"/g) ?? []).length, 1)
  assert.match(html, /href="\/jbm\/messages\/webhook-configs" class="jbm-sidebar-nav__item is-active"/)
})
