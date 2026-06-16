import { get, post, put, del, unwrap } from './request'
import type { BaseAction, DataPaging } from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { optionalSnowflakeIdParam, toSnowflakeIdString, type SnowflakeId } from '@/lib/snowflakeId'

export async function listActions(menuId?: SnowflakeId, page = 1, size = DEFAULT_PAGE_SIZE) {
  const res = await get<DataPaging<BaseAction>>('/action', {
    params: {
      ...pageParams(page, size),
      ...(menuId != null ? { menuId: optionalSnowflakeIdParam(menuId) } : {}),
    },
  })
  return unwrap(res)
}

export async function createAction(data: Partial<BaseAction>) {
  const res = await post<number>('/action', data)
  return unwrap(res)
}

export async function updateAction(actionId: SnowflakeId, data: Partial<BaseAction>) {
  const res = await put<void>(`/action/${toSnowflakeIdString(actionId)}`, data)
  return unwrap(res)
}

export async function deleteAction(actionId: SnowflakeId) {
  const res = await del<void>(`/action/${toSnowflakeIdString(actionId)}`)
  return unwrap(res)
}
