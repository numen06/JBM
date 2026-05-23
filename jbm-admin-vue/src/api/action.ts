import { get, post, put, del, unwrap } from './request'
import type { BaseAction, DataPaging } from './types'
import { pageParams } from './user'

export async function listActions(menuId?: number, page = 1, size = 100) {
  const res = await get<DataPaging<BaseAction>>('/action', {
    params: {
      ...pageParams(page, size),
      ...(menuId != null ? { menuId } : {}),
    },
  })
  return unwrap(res)
}

export async function createAction(data: Partial<BaseAction>) {
  const res = await post<number>('/action', data)
  return unwrap(res)
}

export async function updateAction(actionId: number, data: Partial<BaseAction>) {
  const res = await put<void>(`/action/${actionId}`, data)
  return unwrap(res)
}

export async function deleteAction(actionId: number) {
  const res = await del<void>(`/action/${actionId}`)
  return unwrap(res)
}
