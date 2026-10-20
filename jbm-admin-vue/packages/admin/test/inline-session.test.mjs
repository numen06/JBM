import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import test from 'node:test'

test('cross-tab logout delegates to the host inline login; default hosts still redirect', () => {
  const source = readFileSync(new URL('../../../src/stores/auth.ts', import.meta.url), 'utf8')
  const code = source.slice(source.indexOf('  function syncSessionFromStorage('), source.indexOf('  function bindSessionStorageSync('))
    .replaceAll('import.meta.env.BASE_URL', "'/'")
  for (const inline of [true, false]) {
    const calls = []
    const scope = {
      TOKEN_KEY: 'access', REFRESH_KEY: 'refresh',
      accessToken: { value: 'old' }, refreshToken: { value: 'old-refresh' },
      user: { value: { id: 'user' } }, mustChangePassword: { value: false },
      localStorage: { getItem: () => null }, useMenuStore: () => ({ clear: () => calls.push('clear') }),
      init: () => assert.fail('must not restore a removed session'),
      runtimeConfig: inline ? { onSessionExpired: () => calls.push('inline') } : {},
      window: { location: { replace: value => calls.push(value) } },
    }
    const sync = new Function('scope', `with(scope) { ${stripTypeScriptTypes(code)}; return syncSessionFromStorage }`)(scope)
    sync({ key: 'unrelated' })
    assert.deepEqual(calls, [])
    sync({ key: 'access' })
    assert.equal(scope.accessToken.value, '')
    assert.equal(scope.user.value, null)
    assert.deepEqual(calls, ['clear', inline ? 'inline' : '/login'])
  }
})
