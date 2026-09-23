import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import { createSSRApp, h } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { JbmWorkspaceShell } from '../dist/index.js'

test('all workspaces render the same single sidebar, header and content frame', async () => {
  for (const workspace of ['建筑能源工作台', 'IoT 数据工作台', 'JBM 管理工作台']) {
    const html = await renderToString(createSSRApp({ render: () => h(JbmWorkspaceShell,
      { brand: '党校', logo: '/images/brand/building-logo.png', title: '总览', workspace, collapsed: true, mobileOpen: true },
      { sidebar: () => h('nav', {}, '动态菜单'), context: () => h('select', { 'aria-label': '当前项目' }), actions: () => h('button', {}, '用户菜单'), default: () => h('section', {}, '业务内容'), overlays: () => h('dialog', {}, '操作弹窗') }) }))
    assert.equal((html.match(/<aside /g) || []).length, 1)
    for (const value of ['jbm-workspace-shell is-collapsed is-mobile-open', 'jbm-product-header', 'workspace-page', '动态菜单', '业务内容', '操作弹窗', '当前项目', '展开侧栏']) assert.ok(html.includes(value))
    assert.ok(html.includes('src="/images/brand/building-logo.png"'))
    assert.ok(html.includes('aria-controls="jbm-workspace-sidebar"'))
  }
})

test('shell geometry, controls and scrollbars have one responsive CSS contract', () => {
  const css = readFileSync(new URL('../design.css', import.meta.url), 'utf8')
  assert.match(css, /html \{ scrollbar-gutter: stable; \}/)
  assert.match(css, /\.jbm-shell-sidebar \.jbm-sidebar-nav \{ scrollbar-gutter: stable; \}/)
  assert.match(css, /\.jbm-workspace-shell\.is-collapsed \.jbm-shell-sidebar \{ display: none; \}/)
  assert.match(css, /\.jbm-workspace-shell\.is-mobile-open \.jbm-shell-sidebar \{ visibility: visible; transform: none; \}/)
  assert.match(css, /\.jbm-shell-toggle svg \{ width: 18px; height: 18px; \}/)
})
