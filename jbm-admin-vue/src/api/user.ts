import { get, post, put, unwrap } from './request'
import type { BaseAccount, BaseRole, BaseUser, DataPaging, UserInfoStatistics } from './types'

export function pageParams(page = 1, size = 20) {
  return { 'pageForm.currPage': page, 'pageForm.pageSize': size }
}

/** GET /user — 分页；带 keyword 时走 params=keyword 检索 */
export async function listUsers(page = 1, size = 20, keyword?: string) {
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

export async function getUser(userId: number) {
  const res = await get<BaseUser>(`/user/${userId}`)
  return unwrap(res)
}

/** POST /user — Body 为 BaseUserForm，须含 password */
export async function createUser(data: Partial<BaseUser>) {
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
export async function updateUser(
  userId: number,
  data: Partial<BaseUser> & { roleIds?: string[] },
) {
  const res = await put<BaseUser>(`/user/${userId}`, data)
  return unwrap(res)
}

export async function closeUser(userId: number) {
  const res = await post<boolean>(`/user/${userId}/closure`, {})
  return unwrap(res)
}

export async function getUserStatistics() {
  const res = await get<UserInfoStatistics>('/user/statistics')
  return unwrap(res)
}

export async function getUserRoles(userId: number) {
  const res = await get<BaseRole[]>(`/user/${userId}/roles`)
  return unwrap(res)
}

export async function putUserRoles(userId: number, roleIds: string[]) {
  const res = await put<void>(`/user/${userId}/roles`, { roleIds })
  return unwrap(res)
}

export async function getUserAccounts(userId: number) {
  const res = await get<BaseAccount[]>(`/user/${userId}/accounts`)
  return unwrap(res) ?? []
}

export async function activateUserEmail(userId: number) {
  const res = await put<void>(`/user/${userId}/activations/email`, {})
  return unwrap(res)
}

export async function activateUserMobile(userId: number) {
  const res = await put<void>(`/user/${userId}/activations/mobile`, {})
  return unwrap(res)
}
