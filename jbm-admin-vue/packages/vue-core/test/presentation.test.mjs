import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createSSRApp, h } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { JbmValue, JbmMetricCard, JbmPageHeading } from '../dist/presentation.js'
import { readFileSync } from 'node:fs'

test('metric transitions preserve the exact supplied reading including units and missing data', async () => {
  for (const value of [0, -12.75, '82,134.67 kWh', '—', '无基准']) {
    const html = await renderToString(createSSRApp({ render: () => h(JbmValue, { value }) }))
    assert.ok(html.includes(`>${value}</span>`))
  }
})
test('V3 motion is scoped, bounded and supports reduced motion', () => {
  const css = readFileSync(new URL('../design.css', import.meta.url), 'utf8')
  for (const name of ['jbm-value__reading', 'jbm-dialog-panel', 'jbm-switch', 'jbm-table', 'jbm-skeleton']) assert.ok(css.includes(`.${name}`))
  const reduced = css.slice(css.indexOf('@media (prefers-reduced-motion: reduce)'))
  assert.ok(reduced.includes('.jbm-value__reading'))
  assert.ok(reduced.includes('.jbm-skeleton'))
  assert.ok(reduced.includes('animation: none'))
  assert.doesNotMatch(css, /transition:\s*all|backdrop-filter/)
  assert.doesNotMatch(css, /\.jbm-page-heading \{[^}]*margin-bottom: 0/)
})

test('all hosts share metric anatomy, unavailable states and page headings', async () => {
  const render = component => renderToString(createSSRApp({ render: () => component }))
  for (const value of [0, '—', '82,134.67']) {
    const html = await render(h(JbmMetricCard, { label: '用电', value, unit: 'kWh', hint: '本月', tone: 'warning' }))
    assert.ok(html.includes('jbm-metric__header'))
    assert.ok(html.includes('jbm-metric__value'))
    assert.ok(html.includes(`>${value}</span>`))
    assert.equal(html.includes('jbm-metric__unit'), value !== '—')
    assert.ok(html.includes('data-tone="warning"'))
  }
  const busy = await render(h(JbmMetricCard, { label: '设备', value: 99, loading: true }))
  assert.ok(busy.includes('aria-busy="true"'))
  assert.ok(busy.includes('加载中'))
  assert.ok(!busy.includes('>99<'))
  const heading = await render(h(JbmPageHeading, { title: '运行总览', description: '状态概览' }, { default: () => h('button', {}, '刷新') }))
  assert.match(heading, /<h1>运行总览<\/h1>/)
  assert.ok(heading.includes('jbm-page-heading__actions'))
})
