import { get, post, put, del, unwrap } from './request'
import type { BaseApp, DataPaging } from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { optionalSnowflakeIdParam, toSnowflakeIdString, type SnowflakeId } from '@/lib/snowflakeId'

export type AppListQuery = {
  keyword?: string
  orgId?: number | string
  status?: number | string
  appType?: string
}

export interface AppCredentials {
  appId?: SnowflakeId
  clientId?: string
  clientSecret?: string
}

export async function listApps(page = 1, size = DEFAULT_PAGE_SIZE, query?: AppListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = query?.keyword?.trim()
  if (kw) params.appName = kw
  const orgId = optionalSnowflakeIdParam(query?.orgId)
  if (orgId != null) params.orgId = orgId
  if (query?.status !== undefined && query.status !== '') {
    params.status = Number(query.status)
  }
  if (query?.appType) {
    params.appType = query.appType
  }
  const res = await get<DataPaging<BaseApp>>('/app', { params })
  return unwrap(res)
}

export async function createApp(data: Partial<BaseApp>) {
  const res = await post<AppCredentials>('/app', data)
  return unwrap(res)
}

export async function updateApp(appId: SnowflakeId, data: Partial<BaseApp>) {
  const res = await put<void>(`/app/${toSnowflakeIdString(appId)}`, data)
  return unwrap(res)
}

export async function resetAppSecret(appId: SnowflakeId) {
  const res = await put<string>(`/app/${toSnowflakeIdString(appId)}/secret`)
  return unwrap(res)
}

export async function getAppSecret(appId: SnowflakeId) {
  const res = await get<string>(`/app/${toSnowflakeIdString(appId)}/secret`)
  return unwrap(res)
}

export async function deleteApp(appId: SnowflakeId) {
  const res = await del<void>(`/app/${toSnowflakeIdString(appId)}`)
  return unwrap(res)
}
