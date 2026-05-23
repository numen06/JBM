import { get, put, unwrap } from './request'
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

export async function updatePassword(params: UpdatePasswordParams): Promise<void> {
  const res = await put<void>('/current/user/password', params)
  unwrap(res)
}
