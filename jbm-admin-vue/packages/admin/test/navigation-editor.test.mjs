import assert from 'node:assert/strict'
import test from 'node:test'
import { createRenderer, h, nextTick } from 'vue'
import { JbmNavigationSettings } from '../dist/host.js'

test('page editor keeps drafts across groups/workspaces, searches, undoes, and emits isolated saves', async () => {
  const previousDocument = globalThis.Document, previousShadow = globalThis.ShadowRoot
  globalThis.Document = class {}; globalThis.ShadowRoot = class {}
  const node = (tag, text = '') => ({ tag, text, props: {}, children: [], parent: null, listeners: {}, value: '',
    addEventListener(name, handler) { this.listeners[name] = handler }, getRootNode() { return {} },
    get options() { return this.children.filter(child => child.tag === 'option') },
  })
  const renderer = createRenderer({
    createElement: tag => node(tag), createText: text => node('#text', text), createComment: text => node('#comment', text),
    setText: (el, text) => { el.text = text }, setElementText: (el, text) => { el.text = text; el.children = [] },
    patchProp: (el, key, _old, value) => { el.props[key] = value; if (key === 'value') el.value = value },
    insert: (el, parent, anchor) => { if (el.parent) el.parent.children.splice(el.parent.children.indexOf(el), 1); const at = anchor ? parent.children.indexOf(anchor) : -1; parent.children.splice(at < 0 ? parent.children.length : at, 0, el); el.parent = parent },
    remove: el => { el.parent?.children.splice(el.parent.children.indexOf(el), 1) },
    parentNode: el => el.parent, nextSibling: el => el.parent?.children[el.parent.children.indexOf(el) + 1],
  })
  const root = node('root'), saves = []
  const settings = { tenantId: 'one', revision: 1, canManage: true, preferences: { workspaces: {}, groups: {}, menus: { '/unknown': { label: '保留未加载设置' } } } }
  const catalog = [{ workspace: 'building', group: '工作台', path: '/home', label: '运行总览' }, { workspace: 'building', group: '报表', path: '/reports', label: '用量报表' }, { workspace: 'iot', group: '网关', path: '/gateways', label: '网关管理' }]
  renderer.render(h(JbmNavigationSettings, { open: true, embedded: true, catalog, settings, onSave: value => saves.push(value) }), root)
  const all = (el = root) => [el, ...el.children.flatMap(child => all(child))]
  const text = el => el.text + el.children.map(text).join('')
  const find = predicate => { const el = all().find(predicate); assert.ok(el); return el }
  const input = label => find(el => el.tag === 'input' && el.props['aria-label'] === label)
  const click = async label => { find(el => el.tag === 'button' && text(el).includes(label)).props.onClick(); await nextTick() }
  const edit = async (label, value) => { const el = input(label); el.value = value; (el.props.onInput || el.listeners.input)({ target: el }); await nextTick() }
  assert.equal(all().some(el => el.props.role === 'dialog'), false)
  await edit('运行总览名称', '租户首页')
  await click('报表')
  await edit('用量报表顺序', '7')
  await click('物连')
  await edit('网关管理名称', '现场网关')
  await click('建筑')
  assert.equal(input('运行总览名称').props.value, '租户首页')
  await edit('搜索菜单', '用量')
  assert.ok(input('用量报表名称'))
  assert.equal(all().some(el => el.props['aria-label'] === '运行总览名称'), false)
  await click('保存更改')
  assert.equal(saves[0].menus['/home'].label, '租户首页')
  assert.equal(saves[0].menus['/reports'].order, 7)
  assert.equal(saves[0].menus['/gateways'].label, '现场网关')
  assert.deepEqual(saves[0].menus['/unknown'], settings.preferences.menus['/unknown'])
  assert.equal(settings.preferences.menus['/home'], undefined)
  await click('撤销修改')
  assert.equal(input('用量报表顺序').props.value, 0)
  assert.equal(find(el => el.tag === 'button' && text(el).includes('保存更改')).props.disabled, true)
  renderer.render(h(JbmNavigationSettings, { open: true, embedded: true, catalog, settings: { ...settings, tenantId: 'two', canManage: false } }), root)
  await nextTick()
  assert.equal(find(el => el.tag === 'fieldset').props.disabled, true)
  renderer.render(null, root)
  globalThis.Document = previousDocument; globalThis.ShadowRoot = previousShadow
})
