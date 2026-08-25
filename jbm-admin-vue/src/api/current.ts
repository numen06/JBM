import { get, post, put, unwrap } from './request'
import type { BaseMenu, CurrentUser } from './types'

export async function getCurrentUser() {
  const res = await get<CurrentUser>('/current/user')
  return unwrap(res)
}

export async function getCurrentMenus() {
  const res = await get<BaseMenu[]>('/current/user/menus')
  return unwrap(res)
}

export interface UpdatePasswordParams {
  originPassword: string
  currentPassword: string
  confirmPassword: string
}

export interface UpdateCurrentUserParams {
  nickName?: string
  realName?: string
  userDesc?: string
  avatar?: string
}

export async function updateCurrentUser(params: UpdateCurrentUserParams): Promise<void> {
  const res = await put<void>('/current/user', params)
  unwrap(res)
}

export async function updatePassword(params: UpdatePasswordParams): Promise<void> {
  const res = await put<void>('/current/user/password', params)
  unwrap(res)
}

export async function sendMobileBindCode(mobile: string): Promise<void> {
  const res = await post<boolean>('/oauth2/mobile/code', { mobile })
  unwrap(res)
}

export async function bindMobile(mobile: string, code: string): Promise<void> {
  const res = await post<{ mobile: string }>('/oauth2/mobile/bind', { mobile, code })
  unwrap(res)
}

export async function sendEmailBindCode(email: string): Promise<void> {
  const res = await post<boolean>('/oauth2/email/code', { email })
  unwrap(res)
}

export async function bindEmail(email: string, code: string): Promise<void> {
  const res = await post<{ email: string }>('/oauth2/email/bind', { email, code })
  unwrap(res)
}
