import { get, post, put, unwrap } from './request'
import type { BaseDeveloper, DataPaging } from './types'
import { pageParams } from './user'

export type DeveloperListQuery = {
  keyword?: string
  status?: number | string
  userType?: string
}

export async function listDevelopers(page = 1, size = 20, query?: DeveloperListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = query?.keyword?.trim()
  if (kw) params.userName = kw
  if (query?.userType) params.userType = query.userType
  if (query?.status !== undefined && query.status !== '') {
    params.status = Number(query.status)
  }
  const res = await get<DataPaging<BaseDeveloper>>('/developer', { params })
  return unwrap(res)
}

export async function createDeveloper(data: Partial<BaseDeveloper>) {
  const res = await post<void>('/developer', data)
  return unwrap(res)
}

export async function updateDeveloper(userId: number, data: Partial<BaseDeveloper>) {
  const res = await put<void>(`/developer/${userId}`, data)
  return unwrap(res)
}

export async function getDeveloper(userId: number) {
  const res = await get<BaseDeveloper>(`/developer/${userId}`)
  return unwrap(res)
}

export async function applyDeveloper(userType = 'dev') {
  const res = await post<void>('/developer/apply', { userType })
  return unwrap(res)
}

export async function listPendingDevelopers() {
  const res = await get<BaseDeveloper[]>('/developer/pending')
  return unwrap(res)
}

export async function approveDeveloper(userId: number) {
  const res = await put<void>(`/developer/${userId}/approve`)
  return unwrap(res)
}
