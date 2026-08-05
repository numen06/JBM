import {
  JBM_DEFAULT_CLIENT_ID,
  JBM_DEFAULT_OAUTH_SCOPE,
} from '@/constants/loginModes'
import { createJbmPkcePair } from '@jbm7/sdk'
import { encryptPasswordForClient } from '@/lib/rsaEncrypt'
import { apiBaseUrl, runtimeConfig } from '@/runtimeConfig'
import { get, post, postForm, unwrap } from './request'
import type { ResultBody } from './types'
import type { OAuth2TokenResult } from './types'

export const PASSWORD_ENCRYPTED_HEADER = 'X-Password-Encrypted'

export interface LoginParams {
  username: string
  password: string
  /** 图形验证码，对应 OAuth2 表单字段 vcode（PASSWORD/SMS 可选） */
  vcode?: string
  /** 对应 OAuth2 表单 loginType，默认 PASSWORD */
  loginType?: string
  clientId?: string
  scope?: string
  redirectUri?: string
}

const DEFAULT_CLIENT_ID = JBM_DEFAULT_CLIENT_ID

const loginPlaintext =
  import.meta.env.VITE_LOGIN_PLAINTEXT === 'true' ||
  import.meta.env.VITE_LOGIN_PLAINTEXT === '1'

function shouldEncryptPassword(loginType?: string): boolean {
  if (loginPlaintext) return false
  const t = (loginType ?? 'PASSWORD').toUpperCase()
  return t === 'PASSWORD'
}

export async function login(params: LoginParams): Promise<OAuth2TokenResult> {
  const clientId = params.clientId ?? DEFAULT_CLIENT_ID
  const loginType = (params.loginType ?? 'PASSWORD').toUpperCase()
  const password = shouldEncryptPassword(loginType)
    ? await encryptPasswordForClient(params.password, clientId)
    : params.password
  const pkce = await createJbmPkcePair()
  const body = new URLSearchParams({
    response_type: 'code',
    client_id: clientId,
    redirect_uri: params.redirectUri ?? defaultLoginRedirectUri(),
    state: crypto.randomUUID?.() ?? `state_${Date.now()}`,
    username: params.username,
    password,
    scope: params.scope ?? JBM_DEFAULT_OAUTH_SCOPE,
    loginType,
    code_challenge: pkce.challenge,
    code_challenge_method: pkce.method,
  })
  if (params.vcode?.trim()) {
    body.set('vcode', params.vcode.trim())
  }
  const headers: Record<string, string> = {}
  if (shouldEncryptPassword(loginType)) {
    headers[PASSWORD_ENCRYPTED_HEADER] = 'true'
  }
  const loginResult = await postAuthCenterForm<string>('/oauth2/doLogin', body, headers)
  const callbackUrl = unwrap(loginResult)
  const parsed = new URL(callbackUrl, window.location.origin)
  const returnedState = parsed.searchParams.get('state')
  if (returnedState && returnedState !== body.get('state')) {
    throw new Error('state 校验失败，请重新登录')
  }
  const code = parsed.searchParams.get('code')
  if (!code) {
    throw new Error('授权码缺失，请重新登录')
  }
  return exchangeAuthorizationCode({
    code,
    redirectUri: body.get('redirect_uri') || defaultLoginRedirectUri(),
    clientId,
    codeVerifier: pkce.verifier,
  })
}

function defaultLoginRedirectUri(): string {
  if (typeof window === 'undefined') return '/login/callback'
  return `${window.location.origin}/login/callback`
}

function authCenterUrl(pathWithQuery: string) {
  const configuredAuthorizeBase = runtimeConfig.oauthAuthorizeBaseUrl?.trim()
  if (configuredAuthorizeBase) {
    return `${configuredAuthorizeBase.replace(/\/+$/, '')}${pathWithQuery}`
  }
  const base = apiBaseUrl.replace(/\/+$/, '')
  if (base) return `${base}/auth${pathWithQuery}`
  return `/auth${pathWithQuery}`
}

async function postAuthCenterForm<T>(
  path: string,
  body: URLSearchParams,
  headers: Record<string, string>,
): Promise<ResultBody<T>> {
  const res = await fetch(authCenterUrl(path), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      ...headers,
    },
    body,
  })
  return (await res.json()) as ResultBody<T>
}

export async function thirdPartyCallback(params: {
  provider: string
  code: string
  clientId?: string
  redirectUri?: string
  state?: string
}): Promise<OAuth2TokenResult> {
  const query: Record<string, string | undefined> = {
    code: params.code,
    client_id: params.clientId ?? DEFAULT_CLIENT_ID,
    redirect_uri: params.redirectUri,
    state: params.state,
  }
  const res = await get<Record<string, unknown>>(`/oauth2/thirdparty/${params.provider}/callback`, { params: query })
  const raw = unwrap(res)
  return normalizeTokenPayload(raw)
}

/** GET /oauth2/callback?code=（服务端直接换 Token，简化回调） */
export async function exchangeAuthCode(code: string): Promise<OAuth2TokenResult> {
  const res = await get<Record<string, unknown>>('/oauth2/callback', { params: { code } })
  const raw = unwrap(res)
  return normalizeTokenPayload(raw)
}

/** POST /oauth2/token — 标准 OAuth2 授权码模式 */
export async function exchangeAuthorizationCode(params: {
  code: string
  redirectUri: string
  clientId?: string
  codeVerifier?: string
}): Promise<OAuth2TokenResult> {
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code: params.code,
    client_id: params.clientId ?? DEFAULT_CLIENT_ID,
    redirect_uri: params.redirectUri,
  })
  if (params.codeVerifier?.trim()) body.set('code_verifier', params.codeVerifier.trim())
  const res = await postForm<OAuth2TokenResult | Record<string, unknown>>('/oauth2/token', body)
  const raw = unwrap(res)
  if (raw && typeof raw === 'object' && 'access_token' in (raw as object)) {
    return normalizeTokenPayload(raw as Record<string, unknown>)
  }
  return raw as OAuth2TokenResult
}

export function normalizeTokenPayload(raw: Record<string, unknown>): OAuth2TokenResult {
  const access =
    (raw.access_token as string) ||
    (raw.accessToken as string) ||
    (raw.token as string) ||
    ''
  const refresh = (raw.refresh_token as string) || (raw.refreshToken as string)
  return {
    access_token: access,
    refresh_token: refresh,
    expires_in: raw.expires_in as number | undefined,
    token_type: (raw.token_type as string) || (raw.tokenType as string),
    scope: raw.scope as string | undefined,
    must_change_password: !!(raw.must_change_password ?? raw.mustChangePassword),
  }
}

export async function refreshToken(
  refreshToken: string,
  clientId = DEFAULT_CLIENT_ID,
): Promise<OAuth2TokenResult> {
  const body = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: clientId,
    refresh_token: refreshToken,
  })
  const res = await postForm<OAuth2TokenResult>('/oauth2/refresh', body)
  return unwrap(res)
}

export async function logout(): Promise<void> {
  try {
    await import('./request').then((m) => m.del('/oauth2/logout'))
  } catch {
    // ignore logout errors
  }
}

/** jaja7：恢复 admin 密码与 JBM/demo 应用 OAuth 凭证（需 Auth 已部署 DevJaja7SeedController） */
export async function resetJaja7Seed(): Promise<Record<string, unknown>> {
  const res = await post<Record<string, unknown>>('/internal/dev/reset-jaja7-seed')
  return unwrap(res)
}

export interface RegisterParams {
  userName: string
  password: string
  confirmPassword?: string
  nickName?: string
  email?: string
  mobile?: string
  vcode: string
  clientId?: string
}

export async function register(params: RegisterParams): Promise<void> {
  const clientId = params.clientId ?? DEFAULT_CLIENT_ID
  const password = await encryptPasswordForClient(params.password, clientId)
  const body = new URLSearchParams({
    userName: params.userName.trim(),
    password,
    vcode: params.vcode.trim(),
    client_id: clientId,
  })
  if (params.nickName?.trim()) body.set('nickName', params.nickName.trim())
  if (params.email?.trim()) body.set('email', params.email.trim())
  if (params.mobile?.trim()) body.set('mobile', params.mobile.trim())
  const headers: Record<string, string> = {
    [PASSWORD_ENCRYPTED_HEADER]: 'true',
  }
  const res = await postForm<void>('/oauth2/register', body, { headers })
  unwrap(res)
}
