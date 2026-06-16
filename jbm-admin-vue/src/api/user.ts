import { get, post, put, unwrap } from './request'
import type { BaseAccount, BaseRole, BaseUser, BaseUserOrg, DataPaging, UserInfoStatistics } from './types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { optionalSnowflakeIdParam, toSnowflakeIdString, type SnowflakeId } from '@/lib/snowflakeId'

export function pageParams(page = 1, size = DEFAULT_PAGE_SIZE) {
  return { 'pageForm.currPage': page, 'pageForm.pageSize': size }
}

export interface UserListFilters {
  keyword?: string
  companyId?: number | string | null
  departmentId?: number | string | null
  status?: number
}

/** GET /user — 分页；带 keyword 时走 params=keyword 检索 */
export async function listUsers(page = 1, size = DEFAULT_PAGE_SIZE, keyword?: string) {
  if (keyword?.trim()) {
    const res = await get<BaseUser[]>('/user', { params: { keyword: keyword.trim() } })
    const list = unwrap(res) ?? []
    return {
      contents: list,
      total: list.length,
      pageForm: { currPage: 1, pageSize: size },
    } satisfies DataPaging<BaseUser>
  }
  const res = await get<DataPaging<BaseUser>>('/user', {
    params: pageParams(page, size),
  })
  return unwrap(res)
}

export async function listUsersByFilter(page = 1, size = DEFAULT_PAGE_SIZE, filters: UserListFilters = {}) {
  const params: Record<string, string | number> = {
    ...pageParams(page, size),
  }
  const keyword = filters.keyword?.trim()
  if (keyword) params.userName = keyword
  const companyId = optionalSnowflakeIdParam(filters.companyId)
  if (companyId != null) params.companyId = companyId
  const departmentId = optionalSnowflakeIdParam(filters.departmentId)
  if (departmentId != null) params.departmentId = departmentId
  if (filters.status != null) params.status = filters.status
  const res = await get<DataPaging<BaseUser>>('/user', { params })
  return unwrap(res)
}

export async function getUser(userId: SnowflakeId) {
  const res = await get<BaseUser>(`/user/${toSnowflakeIdString(userId)}`)
  return unwrap(res)
}

/** POST /user — Body 为 BaseUserForm，须含 password */
export async function createUser(data: Partial<BaseUser> & { orgIds?: string[] }) {
  const res = await post<void>('/user', {
    ...data,
    userType: data.userType ?? 'normal',
    password: data.password,
  })
  return unwrap(res)
}

/**
 * PUT /user/{id} — 保存用户并可带 roleIds（与角色授权一并提交）
 */
export async function getUserOrgs(userId: SnowflakeId) {
  const res = await get<BaseUserOrg[]>(`/user/${toSnowflakeIdString(userId)}/orgs`)
  return unwrap(res) ?? []
}

export async function updateUser(
  userId: SnowflakeId,
  data: Partial<BaseUser> & { roleIds?: string[]; orgIds?: string[] },
) {
  const res = await put<BaseUser>(`/user/${toSnowflakeIdString(userId)}`, data)
  return unwrap(res)
}

export async function closeUser(userId: SnowflakeId) {
  const res = await post<boolean>(`/user/${toSnowflakeIdString(userId)}/closure`, {})
  return unwrap(res)
}

export async function getUserStatistics() {
  const res = await get<UserInfoStatistics>('/user/statistics')
  return unwrap(res)
}

export async function getUserRoles(userId: SnowflakeId) {
  const res = await get<BaseRole[]>(`/user/${toSnowflakeIdString(userId)}/roles`)
  return unwrap(res)
}

export async function putUserRoles(userId: SnowflakeId, roleIds: string[]) {
  const res = await put<void>(`/user/${toSnowflakeIdString(userId)}/roles`, { roleIds })
  return unwrap(res)
}

export async function getUserAccounts(userId: SnowflakeId) {
  const res = await get<BaseAccount[]>(`/user/${toSnowflakeIdString(userId)}/accounts`)
  return unwrap(res) ?? []
}

export async function activateUserEmail(userId: SnowflakeId) {
  const res = await put<void>(`/user/${toSnowflakeIdString(userId)}/activations/email`, {})
  return unwrap(res)
}

export async function activateUserMobile(userId: SnowflakeId) {
  const res = await put<void>(`/user/${toSnowflakeIdString(userId)}/activations/mobile`, {})
  return unwrap(res)
}
