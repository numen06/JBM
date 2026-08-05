import assert from 'node:assert/strict'
import test from 'node:test'
import { defineJbmModule, registerJbmModules } from '../dist/index.js'

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
