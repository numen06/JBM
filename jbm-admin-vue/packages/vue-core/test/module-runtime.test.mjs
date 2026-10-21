import assert from 'node:assert/strict'
import test from 'node:test'
import { createSSRApp, h, effectScope } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { defineJbmModule, JbmProductHeader, JbmSidebarNavigation, JbmWorkspaceNavigation, JbmSegmentedControl, registerJbmModules } from '../dist/index.js'

test('segmented control has a labelled group, selected state and disabled choices', async () => {
  const html = await renderToString(createSSRApp({ render: () => h(JbmSegmentedControl, { label: '统计周期', modelValue: 2, options: [{value: 1, label: '日'}, {value: 2, label: '月'}, {value: 3, label: '年', disabled: true}] }) }))
  assert.match(html, /role="group" aria-label="统计周期"/)
  assert.match(html, /--segment-index:1/)
  assert.match(html, /aria-pressed="true"[^>]*>月/)
  assert.match(html, /disabled[^>]*>年/)
})

test('segmented control renders optional decorative icons without losing labels or disabled state', async () => {
  const Icon = { render: () => h('svg', { 'data-segment-icon': '' }) }
  const html = await renderToString(createSSRApp({ render: () => h(JbmSegmentedControl, {
    label: '运行模式', modelValue: 'cool', options: [
      { value: 'cool', label: '制冷', icon: Icon },
      { value: 'heat', label: '制热', icon: Icon, disabled: true },
      { value: 'auto', label: '自动' },
    ],
  }) }))
  assert.equal((html.match(/data-segment-icon/g) ?? []).length, 2)
  assert.match(html, /data-segment-icon[^>]*aria-hidden="true"/)
  assert.match(html, /aria-pressed="true"[^>]*><span[^>]*>.*<span>制冷<\/span>/)
  assert.match(html, /disabled[^>]*>.*<span>制热<\/span>/)
  assert.match(html, />自动<\/button>/)
})

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

test('shared product header keeps application context and actions in one shell', async () => {
  const app = createSSRApp({
    render: () => h(JbmProductHeader, { eyebrow: 'IoT 数据工作区', title: '设备总览' }, {
      navigation: () => h('button', { 'aria-label': '打开菜单' }, 'menu'),
      context: () => h('select', { 'aria-label': '数据租户' }, [h('option', '会昌县人民政府')]),
      actions: () => h('button', { 'aria-label': '用户菜单' }, 'admin'),
    }),
  })
  const html = await renderToString(app)
  assert.match(html, /jbm-product-header/)
  assert.match(html, /IoT 数据工作区/)
  assert.match(html, /aria-label="数据租户"/)
  assert.match(html, /aria-label="用户菜单"/)
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

test('an opted-in group opens its authorized entry and keeps collapse as a separate action', () => {
  const emitted = [], scope = effectScope()
  const groups = [
    { label: '多联机', entryPath: '/control', items: [{ path: '/control', label: '操作控制' }] },
    { label: '未授权入口', entryPath: '/hidden', items: [{ path: '/allowed', label: '合法页面' }] },
  ]
  const render = scope.run(() => JbmSidebarNavigation.setup({ groups }, { emit: (...args) => emitted.push(args) }))
  const tree = render(), group = tree.children[0], heading = group.children[0]
  heading.children[0].props.onClick()
  assert.deepEqual(emitted, [['navigate', '/control']])
  heading.children[1].props.onClick()
  assert.equal(emitted.length, 1)
  tree.children[1].children[0].props.onClick()
  assert.equal(emitted.length, 1)
  scope.stop()
})

test('empty workspaces render nothing and remaining workspace buttons fill their row', async () => {
  const render = (workspaces) => renderToString(createSSRApp({ render: () => h(JbmWorkspaceNavigation, { workspaces, currentKey: 'iot' }) }))
  assert.ok(!(await render([])).includes('应用工作区'))
  const html = await render([{ key: 'building', label: '建筑', path: '/dashboard' }, { key: 'iot', label: '物连', path: '/iot/console' }])
  assert.match(html, /grid-template-columns:repeat\(2, minmax\(0, 1fr\)\)/)
  assert.equal((html.match(/jbm-workspaces__item/g) || []).length, 2)
})
