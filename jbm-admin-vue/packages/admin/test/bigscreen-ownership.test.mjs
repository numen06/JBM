import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('JBM owns the bigscreen management route and mutations', () => {
  const modules = readFileSync(new URL('../src/modules.ts', import.meta.url), 'utf8')
  const api = readFileSync(new URL('../../../src/api/bigscreen.ts', import.meta.url), 'utf8')
  assert.match(modules, /path: 'bigscreen\/views'/)
  assert.match(api, /bigscreenView\/package/)
  assert.match(api, /bigscreenView\/delete/)
})
