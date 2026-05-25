import { get, post, put, del, unwrap } from './request'
import type { BaseMenu, DataPaging } from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export type MenuScope = 'platform' | 'app' | 'visible' | 'all'

export type MenuListQuery = {
  keyword?: string
  menuCode?: string
  menuName?: string
  path?: string
  status?: number | string
  appId?: number | string
  scope?: MenuScope
}

function buildMenuQueryParams(query?: MenuListQuery): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  const kw = query?.keyword?.trim()
  if (kw) params.keyword = kw
  if (query?.menuCode?.trim()) params.menuCode = query.menuCode.trim()
  if (query?.menuName?.trim()) params.menuName = query.menuName.trim()
  if (query?.path?.trim()) params.path = query.path.trim()
  if (query?.status !== undefined && query.status !== '') {
    params.status = Number(query.status)
  }
  if (query?.appId !== undefined && query.appId !== '') {
    params.appId = Number(query.appId)
  }
  if (query?.scope) params.scope = query.scope
  return params
}

export async function listMenus(
  page = 1,
  size = DEFAULT_PAGE_SIZE,
  query?: MenuListQuery,
) {
  const params = { ...pageParams(page, size), ...buildMenuQueryParams(query) }
  const res = await get<DataPaging<BaseMenu>>('/menu', { params })
  return unwrap(res)
}

export async function listAllMenus(appId?: number) {
  const res = await get<BaseMenu[]>('/menu/all', { params: appId ? { appId } : {} })
  return unwrap(res)
}

export async function createMenu(data: Partial<BaseMenu>) {
  const res = await post<number>('/menu', data)
  return unwrap(res)
}

export async function updateMenu(menuId: number, data: Partial<BaseMenu>) {
  const res = await put<void>(`/menu/${menuId}`, data)
  return unwrap(res)
}

export async function deleteMenu(menuId: number) {
  const res = await del<void>(`/menu/${menuId}`)
  return unwrap(res)
}
