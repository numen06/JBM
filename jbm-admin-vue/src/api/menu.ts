import http, { get, post, put, del, unwrap, withServicePrefix } from './request'
import type { BaseMenu, DataPaging, ResultBody } from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { isBlankSnowflakeId, optionalSnowflakeIdParam, toSnowflakeIdParam, toSnowflakeIdString, type SnowflakeId } from '@/lib/snowflakeId'

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
  const appId = optionalSnowflakeIdParam(query?.appId)
  if (appId != null) params.appId = appId
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

export async function listAllMenus(appId?: SnowflakeId) {
  const res = await get<BaseMenu[]>('/menu/all', {
    params: appId != null ? { appId: toSnowflakeIdParam(appId) } : {},
  })
  return unwrap(res)
}

export async function createMenu(data: Partial<BaseMenu>) {
  const res = await post<number>('/menu', data)
  return unwrap(res)
}

export async function updateMenu(menuId: SnowflakeId, data: Partial<BaseMenu>) {
  const res = await put<void>(`/menu/${toSnowflakeIdString(menuId)}`, data)
  return unwrap(res)
}

export async function deleteMenu(menuId: SnowflakeId) {
  const res = await del<void>(`/menu/${toSnowflakeIdString(menuId)}`)
  return unwrap(res)
}

export async function exportMenus(appId?: SnowflakeId) {
  const { data } = await http.get<Blob>(withServicePrefix('/menu/export'), {
    params:
      appId != null && !isBlankSnowflakeId(appId)
        ? {
            appId: toSnowflakeIdParam(appId),
          }
        : undefined,
    responseType: 'blob',
  })
  return data
}

export async function importMenus(file: File, appId?: SnowflakeId) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<ResultBody<string>>(withServicePrefix('/menu/imports'), form, {
    params:
      appId != null && !isBlankSnowflakeId(appId)
        ? {
            appId: toSnowflakeIdParam(appId),
          }
        : undefined,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(data)
}

export const JBM_TEMPLATE_APP_ID = 1000

export type MenuSyncMode = 'merge' | 'replace'

export async function syncMenusFromJbm(
  targetAppId: SnowflakeId,
  options?: { sourceAppId?: SnowflakeId; mode?: MenuSyncMode },
) {
  const params: Record<string, unknown> = {
    targetAppId: toSnowflakeIdParam(targetAppId),
    mode: options?.mode ?? 'merge',
  }
  if (options?.sourceAppId != null && !isBlankSnowflakeId(options.sourceAppId)) {
    params.sourceAppId = toSnowflakeIdParam(options.sourceAppId)
  }
  const res = await post<string>('/menu/sync-from-jbm', undefined, { params })
  return unwrap(res)
}
