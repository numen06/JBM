import { test } from 'node:test'
import assert from 'node:assert/strict'
import { applyNavigation, availableNavigationWorkspaces, emptyNavigation, navigationCatalog } from '../src/navigationPreferences.ts'

test('only authorized business workspaces survive, preferences cannot grant a menu', () => {
  const definitions = ['building', 'iot', 'jbm'].map(key => ({ key, label: key, path: '/' }))
  const menus = [{ workspace: 'building', group: '能源', path: '/dashboard', label: '总览' }, { workspace: 'jbm', group: '概览', path: '/jbm/dashboard', label: '仪表盘' }]
  const preferences = emptyNavigation()
  preferences.menus['/jbm/system/users'] = { label: '不应提权' }
  assert.deepEqual(availableNavigationWorkspaces(definitions, menus, preferences).map(item => item.label), ['建筑'])
  preferences.workspaces.building = { label: '能源', order: 3 }
  assert.equal(availableNavigationWorkspaces(definitions, menus, preferences)[0].label, '能源')
  preferences.menus['/dashboard'] = { hidden: true }
  assert.deepEqual(availableNavigationWorkspaces(definitions, menus, preferences), [])
})

test('menu and group presentation overrides preserve paths and remove empty groups', () => {
  const source = [{ label: '设备', items: [{ path: '/iot/a', label: '甲' }, { path: '/iot/b', label: '乙' }] }]
  const prefs = emptyNavigation()
  prefs.groups['iot:设备'] = { label: '物联设备' }
  prefs.menus['/iot/a'] = { order: 8, label: '一号' }
  assert.deepEqual(applyNavigation(source, prefs, 'iot')[0].items.map(item => item.path), ['/iot/b', '/iot/a'])
  assert.equal(applyNavigation(source, prefs, 'iot')[0].label, '物联设备')
  prefs.groups['iot:设备'].hidden = true
  assert.deepEqual(applyNavigation(source, prefs, 'iot'), [])
  assert.equal(source[0].label, '设备')
})

test('remote menu catalog includes only authorized product leaves and keeps host labels', () => {
  const local = [{ workspace: 'iot', group: '路灯', path: '/iot/streetlights/list', label: '路灯设备' }]
  const raw = [{ menuName: '物连', children: [{ menuCode: 'iot_streetlights', path: '/streetlights/list', menuName: '旧名称' }] }, { menuCode: 'building_dashboard', path: '/dashboard', menuName: '能耗总览' }, { menuCode: 'users', path: '/system/users' }]
  const catalog = navigationCatalog(local, raw)
  assert.equal(catalog.length, 2)
  assert.equal(catalog[0].label, '路灯设备')
  assert.equal(catalog[1].workspace, 'building')
})

test('product parents remain navigable and legacy building VRF menus belong to IoT', () => {
  const catalog = navigationCatalog([], [{ menuCode: 'building_group_iot', menuName: 'IoT 与采集', path: '/iot/dashboard', children: [{ menuCode: 'iot_air_conditioning', path: '/iot/air-conditioning/control', children: [{ menuCode: 'building_vrf_energy', path: '/iot/air-conditioning/energy' }] }] }])
  assert.equal(catalog.length, 2)
  assert.ok(catalog.every(item => item.workspace === 'iot'))
  assert.ok(catalog.some(item => item.path === '/iot/air-conditioning/control'))
  const prefs = emptyNavigation()
  prefs.groups['iot:多联机组'] = { hidden: true }
  assert.deepEqual(availableNavigationWorkspaces([{ key: 'iot', label: '物连', path: '/iot/console' }], catalog, prefs), [])
})

test('new standard menus inherit authorization and group visibility without resetting tenant overrides', () => {
  const preferences = emptyNavigation()
  preferences.menus['/iot/air-conditioning/control'] = { label: '空调遥控', order: 1 }
  const original = [{ label: '多联机组', items: [{ path: '/iot/air-conditioning/control', label: '空调控制' }] }]
  const upgraded = [{ ...original[0], items: [...original[0].items, { path: '/iot/air-conditioning/energy', label: '新能效分析' }] }]
  assert.deepEqual(applyNavigation(upgraded, preferences, 'iot')[0].items.map(item => item.label), ['新能效分析', '空调遥控'])
  preferences.groups['iot:多联机组'] = { hidden: true }
  assert.deepEqual(applyNavigation(upgraded, preferences, 'iot'), [])
  assert.equal(preferences.menus['/iot/air-conditioning/control'].label, '空调遥控')
})

test('legacy building IoT entries keep the host canonical route mapping', () => {
  const catalog = navigationCatalog([], [{ menuCode: 'building_iot', path: '/iot/dashboard' }, { menuCode: 'building_device_bindings', path: '/iot/device-bindings' }])
  assert.deepEqual(catalog.map(item => [item.workspace, item.path]), [['iot', '/iot/console'], ['building', '/device-bindings']])
})

test('host catalog keeps contextual and unavailable pages out of its own menu settings', () => {
  const local = [{ workspace: 'building', group: '工作台', path: '/dashboard', label: '总览' }]
  assert.equal(navigationCatalog(local, [{ menuCode: 'building_transformer_monitor', path: '/transformer-monitoring' }]).length, 1)
})
