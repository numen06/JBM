import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

// Exercise the actual API client with transport stubs, without browser credentials.
const requests = []
const transport = { async get(path, config) { requests.push({ path, config }); return { data: { contents: [{ userName: 'member' }], total: 1 } } }, async put(path, body) { requests.push({ path, body }); if (transport.failReset && path.endsWith('/password')) throw new Error('无权操作'); return { data: { userId: '42' } } }, unwrap: result => result.data }
globalThis.__memberApiTestTransport = transport
let source = readFileSync(new URL('../../../src/api/user.ts', import.meta.url), 'utf8')
source = source.replace(/^import .* from '\.\/request'.*$/m, 'const { get, put, unwrap } = globalThis.__memberApiTestTransport')
source = source.replace(/^import .* from '@\/constants\/pagination'.*$/m, 'const DEFAULT_PAGE_SIZE = 20')
source = source.replace(/^import .* from '@\/lib\/snowflakeId'.*$/m, 'const toSnowflakeIdString = String; const optionalSnowflakeIdParam = value => value')
const code = ts.transpile(source, { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext })
const api = await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'))

test('editing a member resets credentials through the dedicated endpoint and reports failures', async () => {
  await api.updateUser('42', { nickName: '测试成员', password: 'test-only-not-a-secret' })
  assert.deepEqual(requests.splice(0), [
    { path: '/user/42', body: { nickName: '测试成员' } },
    { path: '/user/42/password', body: { password: 'test-only-not-a-secret' } },
  ])
  await api.updateUser('42', { nickName: '资料修改', password: '' })
  assert.deepEqual(requests.splice(0), [{ path: '/user/42', body: { nickName: '资料修改' } }])
  transport.failReset = true
  await assert.rejects(api.updateUser('42', { password: 'another-test-value' }), /成员资料已保存，密码重置未完成：无权操作/)
  assert.deepEqual(requests.map(r => r.path), ['/user/42', '/user/42/password'])
  requests.splice(0)
  const result = await api.listUsers(2, 20, ' member ')
  assert.deepEqual(result, { contents: [{ userName: 'member' }], total: 1 })
  assert.deepEqual(requests, [{ path: '/user', config: { params: { 'pageForm.currPage': 2, 'pageForm.pageSize': 20, keyword: 'member' } } }])
})
