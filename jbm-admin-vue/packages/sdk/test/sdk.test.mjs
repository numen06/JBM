import assert from 'node:assert/strict'
import { createHash, webcrypto } from 'node:crypto'
import { createServer } from 'node:http'
import test from 'node:test'
import {
  buildJbmAuthorizationUrl,
  createJbmClient,
  createJbmOAuthState,
  createJbmPkcePair,
  createJbmServiceClient,
  unwrapJbmResult,
} from '../dist/index.js'

test('client sends the standard and legacy tenant headers with the same active tenant', async () => {
  let headers
  const server = createServer((request, response) => {
    headers = request.headers
    response.setHeader('content-type', 'application/json')
    response.end('{}')
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  try {
    const address = server.address()
    const client = createJbmClient({
      baseUrl: `http://127.0.0.1:${address.port}`,
      tokenProvider: {
        getAccessToken: () => undefined,
        getRefreshToken: () => undefined,
        updateTokens: () => {},
        clearTokens: () => {},
      },
      tenantProvider: { getTenantId: () => 'tenant-active' },
    })
    await client.get('/headers')
    assert.equal(headers['x-tenant-id'], 'tenant-active')
    assert.equal(headers.tenantid, 'tenant-active')
  } finally {
    await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()))
  }
})

test('service client always prefixes an explicit service path', async () => {
  let calledPath = ''
  const base = {
    request: async (_method, path) => { calledPath = path },
    get: async (path) => { calledPath = path; return path },
    post: async (path) => { calledPath = path },
    put: async (path) => { calledPath = path },
    patch: async (path) => { calledPath = path },
    delete: async (path) => { calledPath = path },
  }
  const center = createJbmServiceClient(base, '/center/')
  assert.equal(await center.get('/current/user'), '/center/current/user')
  assert.equal(calledPath, '/center/current/user')
})

test('service path is mandatory', () => {
  assert.throws(() => createJbmServiceClient({}, ''), /service path is required/)
})

test('unwrap rejects unsuccessful result bodies', () => {
  assert.equal(unwrapJbmResult({ success: true, result: 7 }), 7)
  assert.throws(() => unwrapJbmResult({ code: 403, message: 'denied' }), /denied \[403\]/)
})

test('PKCE authorization URLs never contain a client secret', async () => {
  const pkce = await createJbmPkcePair()
  const state = createJbmOAuthState()
  const url = new URL(buildJbmAuthorizationUrl({
    authorizeUrl: 'https://auth.example.test/oauth2/authorize',
    clientId: 'browser-client',
    redirectUri: 'https://app.example.test/login/callback',
    state,
    pkce,
  }))
  assert.equal(url.searchParams.get('code_challenge_method'), 'S256')
  assert.equal(url.searchParams.get('state'), state)
  assert.equal(url.searchParams.has('client_secret'), false)
  assert.ok(pkce.verifier.length >= 43)
})

test('PKCE S256 works when Web Crypto digest is unavailable', async () => {
  const originalCrypto = globalThis.crypto
  Object.defineProperty(globalThis, 'crypto', {
    configurable: true,
    value: { getRandomValues: webcrypto.getRandomValues.bind(webcrypto) },
  })
  try {
    const pkce = await createJbmPkcePair()
    const expected = createHash('sha256').update(pkce.verifier).digest('base64url')
    assert.equal(pkce.challenge, expected)
    assert.equal(pkce.method, 'S256')
  } finally {
    Object.defineProperty(globalThis, 'crypto', { configurable: true, value: originalCrypto })
  }
})
