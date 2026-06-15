import http, { get, post, put, del, unwrap, withServicePrefix } from './request'
import type { BaseMenu, DataPaging, ResultBody } from './types'
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

export async function exportMenus(appId?: number | string) {
  const { data } = await http.get<Blob>(withServicePrefix('/menu/export'), {
    params:
      appId !== undefined && appId !== ''
        ? {
            appId: Number(appId),
          }
        : undefined,
    responseType: 'blob',
  })
  return data
}

export async function importMenus(file: File, appId?: number | string) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<ResultBody<string>>(withServicePrefix('/menu/imports'), form, {
    params:
      appId !== undefined && appId !== ''
        ? {
            appId: Number(appId),
          }
        : undefined,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(data)
}
