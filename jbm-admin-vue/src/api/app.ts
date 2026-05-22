import { get, post, put, del, unwrap } from './request'
import type { BaseApp, DataPaging } from './types'
import { pageParams } from './user'

export async function listApps(page = 1, size = 20) {
  const res = await get<DataPaging<BaseApp>>('/app', { params: pageParams(page, size) })
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
