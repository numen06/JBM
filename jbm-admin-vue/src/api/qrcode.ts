import { get, isOk, unwrap } from './request'
import { post } from './request'
import { normalizeTokenPayload } from './auth'
import type { OAuth2TokenResult } from './types'

export interface QrLoginSession {
  image: string
  code: string
  state: string
  scanUrl?: string
}

/** GET /qrcode/login */
export async function fetchLoginQr(params: {
  clientId: string
  redirectUri: string
  width?: number
  height?: number
}): Promise<QrLoginSession> {
  const res = await get<QrLoginSession>('/qrcode/login', {
    params: {
      client_id: params.clientId,
      redirect_uri: params.redirectUri,
      width: params.width ?? 200,
      height: params.height ?? 200,
    },
  })
  return unwrap(res)
}

/** GET /qrcode/check — 轮询；confirmState=2 时 result 为 token 字段 Map */
export async function pollQrLogin(code: string): Promise<{
  done: boolean
  confirmState?: number
  token?: OAuth2TokenResult
  code?: string
  redirectUri?: string
  message?: string
}> {
  const body = await get<unknown>('/qrcode/check', { params: { code } })
  if (isOk(body)) {
    const data = body.result
    if (data && typeof data === 'object') {
      const raw = data as Record<string, unknown>
      if (typeof raw.code === 'string') {
        return {
          done: true,
          code: raw.code,
          redirectUri: typeof raw.redirectUri === 'string' ? raw.redirectUri : undefined,
        }
      }
      return { done: true, token: normalizeTokenPayload(data as Record<string, unknown>) }
    }
    return { done: false, confirmState: typeof data === 'number' ? data : undefined }
  }
  const state = typeof body.result === 'number' ? body.result : undefined
  return { done: false, confirmState: state, message: body.message ?? '等待扫码' }
}

export async function markQrScanned(code: string): Promise<number> {
  const res = await get<number>('/qrcode/scanned', { params: { code } })
  return unwrap(res)
}

export async function confirmQrLogin(code: string): Promise<{
  code: string
  redirectUri?: string
  state?: string
  location?: string
}> {
  const res = await post<Record<string, unknown>>('/qrcode/confirm', null, { params: { code } })
  const raw = unwrap(res)
  return {
    code: String(raw.code || ''),
    redirectUri: typeof raw.redirectUri === 'string' ? raw.redirectUri : undefined,
    state: typeof raw.state === 'string' ? raw.state : undefined,
    location: typeof raw.location === 'string' ? raw.location : undefined,
  }
}
