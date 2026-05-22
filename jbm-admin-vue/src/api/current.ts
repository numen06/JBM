import { get, unwrap } from './request'
import type { BaseMenu, CurrentUser } from './types'

export async function getCurrentUser() {
  const res = await get<CurrentUser>('/current/user')
  return unwrap(res)
}

export async function getCurrentMenus() {
  const res = await get<BaseMenu[]>('/current/user/menus')
  return unwrap(res)
}
