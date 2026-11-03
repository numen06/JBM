import assert from 'node:assert/strict'
import test from 'node:test'
import { registerHooks, createRequire } from 'node:module'
import { pathToFileURL } from 'node:url'

// JSEncrypt's Node ESM entry contains extensionless imports. Its supported
// CommonJS entry runs the same legacy RSA implementation in these tests.
const require = createRequire(import.meta.url)
registerHooks({ resolve(name, context, next) { return next(name === 'jsencrypt' ? pathToFileURL(require.resolve(name)).href : name, context) } })
globalThis.self = globalThis
globalThis.window = { location: { origin: 'https://admin.test' }, crypto: globalThis.crypto }
const { readLoginKey, createKeyClient, downloadLoginKey } = await import('../../../src/lib/keyLogin.ts')

test('the portable file contains only the encrypted credential', async () => {
  const file = { format: 'jbm-login-key', version: 2, payload: 'A'.repeat(128) }
  assert.deepEqual(await readLoginKey(new File([JSON.stringify(file)], 'key.jbmkey')), file)
  await assert.rejects(readLoginKey(new File([JSON.stringify({ ...file, payload: 'broken' })], 'key.jbmkey')), /加密密钥文件/)
})

test('encrypted login submits the opaque credential without a private key or password prompt', async () => {
  const requests = []
  globalThis.fetch = async (url, options) => {
    requests.push(JSON.parse(options.body))
    return { ok: true, json: async () => ({ success: true, result: { challengeId: 'single-use-challenge' } }) }
  }
  const file = new File([JSON.stringify({ format: 'jbm-login-key', version: 2, payload: 'B'.repeat(128) })], 'key.jbmkey')
  const proof = JSON.parse(await createKeyClient('/auth', () => '').fileKeyProof(file, 'app'))
  assert.deepEqual(proof, { challengeId: 'single-use-challenge', fileKey: 'B'.repeat(128) })
  assert.deepEqual(requests, [{ kind: 'FILE_KEY', client_id: 'app' }])
})

test('new exports omit account metadata and refuse plaintext private-key exports', async () => {
  let blob
  globalThis.URL.createObjectURL = value => { blob = value; return 'blob:test' }
  globalThis.URL.revokeObjectURL = () => {}
  globalThis.document = { createElement: () => ({ click() {}, remove() {} }), body: { appendChild() {} } }
  downloadLoginKey({ format: 'jbm-login-key', version: 2, payload: 'C'.repeat(128), origin: 'https://admin.test', account: 'user', name: 'test', keyId: 'a'.repeat(64) })
  assert.deepEqual(Object.keys(JSON.parse(await blob.text())).sort(), ['format', 'payload', 'version'])
  assert.throws(() => downloadLoginKey({ version: 1, privateKey: 'secret' }), /不再支持明文导出/)
})
