import { get, post, put, del, unwrap } from './request'
import type { BaseRole, DataPaging } from './types'
import { pageParams } from './user'

export async function listRoles(page = 1, size = 20) {
  const res = await get<DataPaging<BaseRole>>('/role', { params: pageParams(page, size) })
  return unwrap(res)
}

export async function listAllRoles() {
  const res = await get<BaseRole[]>('/role/all')
  return unwrap(res)
}

export async function createRole(data: Partial<BaseRole>) {
  const res = await post<number>('/role', data)
  return unwrap(res)
}

export async function updateRole(roleId: number, data: Partial<BaseRole>) {
  const res = await put<void>(`/role/${roleId}`, data)
  return unwrap(res)
}

export async function deleteRole(roleId: number) {
  const res = await del<void>(`/role/${roleId}`)
  return unwrap(res)
}
