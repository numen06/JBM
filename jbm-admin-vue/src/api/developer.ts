import { get, post, put, unwrap } from './request'
import type { BaseDeveloper, DataPaging } from './types'
import { pageParams } from './user'

export async function listDevelopers(page = 1, size = 20) {
  const res = await get<DataPaging<BaseDeveloper>>('/developer', {
    params: pageParams(page, size),
  })
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
