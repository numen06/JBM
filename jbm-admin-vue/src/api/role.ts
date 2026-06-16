import { get, post, put, del, unwrap } from './request'
import type { BaseRole, DataPaging } from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { toSnowflakeIdString, type SnowflakeId } from '@/lib/snowflakeId'

export type RoleListQuery = {
  keyword?: string
  status?: number | string
}

export async function listRoles(page = 1, size = DEFAULT_PAGE_SIZE, query?: RoleListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = query?.keyword?.trim()
  if (kw) params.roleName = kw
  if (query?.status !== undefined && query.status !== '') {
    params.status = Number(query.status)
  }
  const res = await get<DataPaging<BaseRole>>('/role', { params })
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

export async function updateRole(roleId: SnowflakeId, data: Partial<BaseRole>) {
  const res = await put<void>(`/role/${toSnowflakeIdString(roleId)}`, data)
  return unwrap(res)
}

export async function deleteRole(roleId: SnowflakeId) {
  const res = await del<void>(`/role/${toSnowflakeIdString(roleId)}`)
  return unwrap(res)
}
