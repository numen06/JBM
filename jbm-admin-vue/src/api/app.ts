import { get, post, put, del, unwrap } from './request'
import type { BaseApp, DataPaging } from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export type AppListQuery = {
  keyword?: string
  orgId?: number | string
  status?: number | string
}

export async function listApps(page = 1, size = DEFAULT_PAGE_SIZE, query?: AppListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = query?.keyword?.trim()
  if (kw) params.appName = kw
  if (query?.orgId !== undefined && query.orgId !== '') {
    params.orgId = Number(query.orgId)
  }
  if (query?.status !== undefined && query.status !== '') {
    params.status = Number(query.status)
  }
  const res = await get<DataPaging<BaseApp>>('/app', { params })
  return unwrap(res)
}

export async function createApp(data: Partial<BaseApp>) {
  const res = await post<number>('/app', data)
  return unwrap(res)
}

export async function updateApp(appId: number, data: Partial<BaseApp>) {
  const res = await put<void>(`/app/${appId}`, data)
  return unwrap(res)
}

export async function deleteApp(appId: number) {
  const res = await del<void>(`/app/${appId}`)
  return unwrap(res)
}
