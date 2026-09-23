import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { chartPalette, chartPresentation, chartSeriesColor, formatChartNumber } from '../dist/design.js'

test('chart numbers retain full units and distinguish missing values from zero', () => {
  for (const value of [null, undefined, '', ' ', NaN, Infinity, true, {}]) assert.equal(formatChartNumber(value), '—')
  assert.equal(formatChartNumber(0), '0')
  assert.equal(formatChartNumber(82134.67), '82,134.67')
  assert.equal(formatChartNumber(-125000), '-125,000')
  assert.equal(formatChartNumber(0.00015), '0.00015')
})
test('palette is stable by identity and reduced motion disables chart animation', () => {
  const before = ['a', 'b', 'c'].map(key => [key, chartSeriesColor(key)])
  for (const [key, color] of before.reverse()) assert.equal(chartSeriesColor(key), color)
  assert.equal(chartPresentation(true).animation, false)
  assert.equal(chartPresentation(false).animation, true)
  assert.notEqual(chartPresentation().color, chartPalette)
})
test('foundation exposes one contract for shadcn, hosts and chart renderers', () => {
  const css = readFileSync(new URL('../design.css', import.meta.url), 'utf8')
  for (const color of chartPalette) assert.ok(css.includes(color))
  for (const token of ['--color-card:', '--color-popover:', '--surface-card:', '--jbm-normal:', '--workspace-sidebar-width:']) assert.ok(css.includes(token))
  assert.match(css, /prefers-reduced-motion/)
  assert.match(css, /focus-visible/)
  assert.doesNotMatch(css, /https?:\/\//)
})

test('energy bars follow the brand while general chart ink and surfaces stay restrained', () => {
  const css = readFileSync(new URL('../design.css', import.meta.url), 'utf8')
  assert.match(css, /--primary: 152 100% 50%/)
  assert.match(css, /--ring: 152 100% 50%/)
  assert.match(css, /--primary-foreground: 0 0% 0%/)
  assert.equal(chartPalette[0], '#68b894')
  assert.match(css, /--jbm-energy-bar: hsl\(var\(--primary\) \/ \.7\)/)
  assert.match(css, /\.jbm-energy-bar \{ --jbm-bar-color: var\(--jbm-energy-bar\); background: var\(--jbm-energy-bar\); \}/)
  assert.match(css, /--background: 0 0% 4%/)
  assert.match(css, /--surface-card: 0 0% 9%/)
  assert.match(css, /\.jbm-metric::after \{ content: none; \}/)
  assert.doesNotMatch(css, /radial-gradient|background: linear-gradient\((?:0|90|120|135|145)deg/)
  assert.doesNotMatch(css, /164 73% 58%|--surface-hover\) \/ \.6/)
})

test('accent roles use a shared ladder and only data bars receive a directional gradient', () => {
  const css = readFileSync(new URL('../design.css', import.meta.url), 'utf8')
  for (const token of ['soft', 'bg-hover', 'selected', 'border', 'border-strong', 'ink', 'solid', 'solid-hover', 'solid-pressed']) {
    assert.ok(css.includes(`--jbm-accent-${token}:`))
    assert.ok(css.includes(`var(--jbm-accent-${token})`))
  }
  assert.match(css, /\.jbm-chart-bar\.jbm-energy-bar \{ background: linear-gradient\(180deg, var\(--jbm-energy-bar-top\), var\(--jbm-energy-bar-bottom\)\); \}/)
  const luminance = rgb => rgb.map(v => v / 255).map(v => v <= .04045 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4).reduce((sum, v, i) => sum + v * [.2126, .7152, .0722][i], 0)
  const contrast = (a, b) => (Math.max(luminance(a), luminance(b)) + .05) / (Math.min(luminance(a), luminance(b)) + .05)
  const primary = [0, 255, 136], panel = [23, 23, 23]
  assert.ok(contrast(primary, [0, 0, 0]) >= 4.5)
  assert.ok(contrast([166, 166, 166], panel) >= 4.5)
  for (const token of ['bar', 'bar-top', 'bar-bottom']) {
    const alpha = Number(css.match(new RegExp(`--jbm-energy-${token}: hsl\\(var\\(--primary\\) / ([.\\d]+)\\)`))[1])
    assert.ok(contrast(primary.map((v, i) => v * alpha + panel[i] * (1 - alpha)), panel) >= 3)
  }
})
