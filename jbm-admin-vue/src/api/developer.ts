import { get, del, unwrap } from './request'
import type { BaseDeveloper, DataPaging } from './types'
import { pageParams } from './user'

export async function listDevelopers(page = 1, size = 20) {
  const res = await get<DataPaging<BaseDeveloper>>('/developer', {
    params: pageParams(page, size),
  })
  return unwrap(res)
}

export async function deleteDeveloper(developerId: number) {
  const res = await del<void>(`/developer/${developerId}`)
  return unwrap(res)
}
