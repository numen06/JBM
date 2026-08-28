import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type Method,
  type ResponseType,
} from 'axios'

export interface JbmTokens {
  accessToken: string
  refreshToken?: string
  expiresIn?: number
  tokenType?: string
}

export interface JbmTokenProvider {
  getAccessToken(): string | undefined
  getRefreshToken(): string | undefined
  updateTokens(tokens: JbmTokens): void | Promise<void>
  clearTokens(): void | Promise<void>
}

export interface JbmTenantProvider {
  getTenantId(): string | undefined
}

export interface JbmClientOptions {
  baseUrl: string
  tokenProvider: JbmTokenProvider
  tenantProvider?: JbmTenantProvider
  refreshTokens?: (refreshToken: string) => Promise<JbmTokens>
  onUnauthorized?: () => void | Promise<void>
  timeout?: number
}

export interface JbmRequestOptions {
  body?: unknown
  params?: Record<string, unknown>
  headers?: Record<string, string>
  responseType?: ResponseType
  signal?: AbortSignal
  skipAuthRefresh?: boolean
}

export interface JbmResultBody<T> {
  code?: number
  success?: boolean
  message?: string
  result?: T
  httpStatus?: number
  status?: number
}

type RetryConfig = AxiosRequestConfig & { _jbmRetried?: boolean; _jbmSkipAuthRefresh?: boolean }

export interface JbmClient {
  request<T>(method: Method, path: string, options?: JbmRequestOptions): Promise<T>
  get<T>(path: string, options?: JbmRequestOptions): Promise<T>
  post<T>(path: string, body?: unknown, options?: JbmRequestOptions): Promise<T>
  put<T>(path: string, body?: unknown, options?: JbmRequestOptions): Promise<T>
  patch<T>(path: string, body?: unknown, options?: JbmRequestOptions): Promise<T>
  delete<T>(path: string, options?: JbmRequestOptions): Promise<T>
}

export interface JbmServiceClient extends JbmClient {
  readonly servicePath: string
}

export interface JbmPkcePair {
  verifier: string
  challenge: string
  method: 'S256'
}

export interface JbmAuthorizationUrlOptions {
  authorizeUrl: string
  clientId: string
  redirectUri: string
  state: string
  scope?: string
  pkce: JbmPkcePair
  extraParams?: Record<string, string>
}

export function createJbmClient(options: JbmClientOptions): JbmClient {
  const http = axios.create({
    baseURL: normalizeBaseUrl(options.baseUrl),
    timeout: options.timeout ?? 30_000,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
  })
  let refreshPromise: Promise<void> | undefined

  http.interceptors.request.use((config) => {
    const accessToken = options.tokenProvider.getAccessToken()
    if (accessToken) {
      config.headers.Authorization = accessToken.startsWith('Bearer ')
        ? accessToken
        : `Bearer ${accessToken}`
    }
    const tenantId = options.tenantProvider?.getTenantId()
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId
      config.headers.tenantId = tenantId
    }
    return config
  })

  http.interceptors.response.use(
    async (response) => {
      const config = response.config as RetryConfig
      if (!config._jbmRetried && !config._jbmSkipAuthRefresh && isUnauthorizedBody(response.data)) {
        return refreshAndRetry(http, config)
      }
      return response
    },
    async (error: AxiosError) => {
      const config = error.config as RetryConfig | undefined
      if (error.response?.status === 401 && config && !config._jbmRetried && !config._jbmSkipAuthRefresh) {
        return refreshAndRetry(http, config)
      }
      return Promise.reject(toJbmError(error))
    },
  )

  async function refreshAndRetry(instance: AxiosInstance, config: RetryConfig) {
    config._jbmRetried = true
    const refreshToken = options.tokenProvider.getRefreshToken()
    if (refreshToken && options.refreshTokens) {
      try {
        refreshPromise ??= options.refreshTokens(refreshToken)
          .then((tokens) => options.tokenProvider.updateTokens(tokens))
          .then(() => undefined)
          .finally(() => {
            refreshPromise = undefined
          })
        await refreshPromise
        return instance(config)
      } catch {
        refreshPromise = undefined
      }
    }
    await options.tokenProvider.clearTokens()
    await options.onUnauthorized?.()
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }

  async function request<T>(method: Method, path: string, requestOptions: JbmRequestOptions = {}) {
    const response = await http.request<T>({
      method,
      url: normalizeRequestPath(path),
      data: requestOptions.body,
      params: requestOptions.params,
      headers: requestOptions.headers,
      responseType: requestOptions.responseType,
      signal: requestOptions.signal,
      _jbmSkipAuthRefresh: requestOptions.skipAuthRefresh,
    } as RetryConfig)
    return response.data
  }

  return {
    request,
    get: (path, requestOptions) => request('GET', path, requestOptions),
    post: (path, body, requestOptions) => request('POST', path, { ...requestOptions, body }),
    put: (path, body, requestOptions) => request('PUT', path, { ...requestOptions, body }),
    patch: (path, body, requestOptions) => request('PATCH', path, { ...requestOptions, body }),
    delete: (path, requestOptions) => request('DELETE', path, requestOptions),
  }
}

export function createJbmServiceClient(client: JbmClient, servicePath: string): JbmServiceClient {
  const prefix = normalizeServicePath(servicePath)
  const pathFor = (path: string) => `${prefix}/${normalizeRequestPath(path)}`
  return {
    servicePath: prefix,
    request: (method, path, options) => client.request(method, pathFor(path), options),
    get: (path, options) => client.get(pathFor(path), options),
    post: (path, body, options) => client.post(pathFor(path), body, options),
    put: (path, body, options) => client.put(pathFor(path), body, options),
    patch: (path, body, options) => client.patch(pathFor(path), body, options),
    delete: (path, options) => client.delete(pathFor(path), options),
  }
}

export async function createJbmPkcePair(): Promise<JbmPkcePair> {
  const verifier = randomBase64Url(32)
  const digest = await sha256(new TextEncoder().encode(verifier))
  return { verifier, challenge: bytesToBase64Url(digest), method: 'S256' }
}

export function createJbmOAuthState() {
  return randomBase64Url(24)
}

export function buildJbmAuthorizationUrl(options: JbmAuthorizationUrlOptions) {
  const url = new URL(options.authorizeUrl)
  const params: Record<string, string> = {
    response_type: 'code',
    client_id: options.clientId,
    redirect_uri: options.redirectUri,
    scope: options.scope ?? 'all',
    state: options.state,
    code_challenge: options.pkce.challenge,
    code_challenge_method: options.pkce.method,
    ...options.extraParams,
  }
  for (const [key, value] of Object.entries(params)) url.searchParams.set(key, value)
  return url.toString()
}

export function isJbmResultOk<T>(body: JbmResultBody<T> | undefined): boolean {
  return !!body && (body.success === true || body.code === 200)
}

export function unwrapJbmResult<T>(body: JbmResultBody<T>): T {
  if (!isJbmResultOk(body)) {
    const code = body.code != null ? ` [${body.code}]` : ''
    throw new Error(`${body.message?.trim() || '请求失败'}${code}`)
  }
  return body.result as T
}

function normalizeBaseUrl(value: string) {
  const trimmed = value.trim()
  if (!trimmed) return ''
  return trimmed.endsWith('/') ? trimmed : `${trimmed}/`
}

function normalizeRequestPath(path: string) {
  if (!path) throw new Error('JBM request path is required')
  if (/^https?:\/\//i.test(path)) return path
  return path.replace(/^\/+/, '')
}

function normalizeServicePath(path: string) {
  if (!path?.trim()) throw new Error('JBM service path is required')
  return `/${path.replace(/^\/+|\/+$/g, '')}`
}

function isUnauthorizedBody(data: unknown) {
  if (!data || typeof data !== 'object') return false
  const body = data as Record<string, unknown>
  return Number(body.httpStatus ?? body.status ?? body.code) === 401
}

function toJbmError(error: AxiosError) {
  const body = error.response?.data
  if (body && typeof body === 'object') {
    const message = (body as Record<string, unknown>).message
    if (typeof message === 'string' && message.trim()) return new Error(message.trim())
  }
  return new Error(error.message || '请求失败')
}

function randomBase64Url(byteLength: number) {
  const bytes = new Uint8Array(byteLength)
  globalThis.crypto.getRandomValues(bytes)
  return bytesToBase64Url(bytes)
}

function bytesToBase64Url(bytes: Uint8Array) {
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return globalThis.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

async function sha256(message: Uint8Array) {
  const subtle = globalThis.crypto?.subtle
  if (subtle) {
    const input = new Uint8Array(message.length)
    input.set(message)
    return new Uint8Array(await subtle.digest('SHA-256', input.buffer))
  }

  const constants = new Uint32Array([
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
  ])
  const rotateRight = (value: number, bits: number) => (value >>> bits) | (value << (32 - bits))
  const bitLength = message.length * 8
  const paddedLength = Math.ceil((message.length + 9) / 64) * 64
  const padded = new Uint8Array(paddedLength)
  padded.set(message)
  padded[message.length] = 0x80
  const paddedView = new DataView(padded.buffer)
  paddedView.setUint32(paddedLength - 8, Math.floor(bitLength / 0x100000000))
  paddedView.setUint32(paddedLength - 4, bitLength)

  const hash = new Uint32Array([
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
  ])
  const words = new Uint32Array(64)
  for (let offset = 0; offset < paddedLength; offset += 64) {
    for (let i = 0; i < 16; i += 1) words[i] = paddedView.getUint32(offset + i * 4)
    for (let i = 16; i < 64; i += 1) {
      const s0 = rotateRight(words[i - 15]!, 7) ^ rotateRight(words[i - 15]!, 18) ^ (words[i - 15]! >>> 3)
      const s1 = rotateRight(words[i - 2]!, 17) ^ rotateRight(words[i - 2]!, 19) ^ (words[i - 2]! >>> 10)
      words[i] = (words[i - 16]! + s0 + words[i - 7]! + s1) >>> 0
    }

    let a = hash[0]!
    let b = hash[1]!
    let c = hash[2]!
    let d = hash[3]!
    let e = hash[4]!
    let f = hash[5]!
    let g = hash[6]!
    let h = hash[7]!
    for (let i = 0; i < 64; i += 1) {
      const sum1 = rotateRight(e, 6) ^ rotateRight(e, 11) ^ rotateRight(e, 25)
      const choice = (e & f) ^ (~e & g)
      const temp1 = (h + sum1 + choice + constants[i]! + words[i]!) >>> 0
      const sum0 = rotateRight(a, 2) ^ rotateRight(a, 13) ^ rotateRight(a, 22)
      const majority = (a & b) ^ (a & c) ^ (b & c)
      const temp2 = (sum0 + majority) >>> 0
      h = g
      g = f
      f = e
      e = (d + temp1) >>> 0
      d = c
      c = b
      b = a
      a = (temp1 + temp2) >>> 0
    }
    hash[0] = (hash[0]! + a) >>> 0
    hash[1] = (hash[1]! + b) >>> 0
    hash[2] = (hash[2]! + c) >>> 0
    hash[3] = (hash[3]! + d) >>> 0
    hash[4] = (hash[4]! + e) >>> 0
    hash[5] = (hash[5]! + f) >>> 0
    hash[6] = (hash[6]! + g) >>> 0
    hash[7] = (hash[7]! + h) >>> 0
  }

  const digest = new Uint8Array(32)
  const digestView = new DataView(digest.buffer)
  hash.forEach((word, index) => digestView.setUint32(index * 4, word))
  return digest
}
