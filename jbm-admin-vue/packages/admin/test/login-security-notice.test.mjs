import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { computed, ref } from 'vue'
import test from 'node:test'

test('security notice shows and acknowledges at most three records without acknowledging unseen logins', async () => {
  const source = readFileSync(new URL('../../../src/components/LoginSecurityNotice.vue', import.meta.url), 'utf8')
  const selection = source.match(/const visibleAlerts = computed\([^\n]+/)[0]
  const request = source.slice(source.indexOf('async function request('), source.indexOf('async function load('))
  const alerts = ref(Array.from({ length: 8 }, (_, index) => ({ id: String(index) })))
  let submitted
  const scope = { alerts, computed, props: { authBase: '/auth' }, fetch: async (_, options) => {
    submitted = JSON.parse(options.body).ids
    return { ok: true, json: async () => ({ success: true, result: alerts.value.filter(row => !submitted.includes(row.id)) }) }
  } }
  const api = new Function('scope', `with(scope) { ${stripTypeScriptTypes(selection + '\n' + request)}; return { visibleAlerts, request } }`)(scope)
  assert.deepEqual(api.visibleAlerts.value.map(row => row.id), ['0', '1', '2'])
  alerts.value = await api.request('existing-session', true)
  assert.deepEqual(submitted, ['0', '1', '2'])
  assert.equal(alerts.value.length, 5)
  assert.deepEqual(api.visibleAlerts.value.map(row => row.id), ['3', '4', '5'])
})
