import { get, post, put, unwrap } from './request'
import type { BaseUser, DataPaging, UserInfoStatistics } from './types'

export function pageParams(page = 1, size = 20) {
  return { 'pageForm.currPage': page, 'pageForm.pageSize': size }
}

export async function listUsers(page = 1, size = 20, keyword?: string) {
  const res = await get<DataPaging<BaseUser>>('/user', {
    params: { ...pageParams(page, size), keyword },
  })
  return unwrap(res)
}

export async function getUser(userId: number) {
  const res = await get<BaseUser>(`/user/${userId}`)
  return unwrap(res)
}

export async function createUser(data: Partial<BaseUser>) {
  const res = await post<number>('/user', data)
  return unwrap(res)
}

export async function updateUser(userId: number, data: Partial<BaseUser>) {
  const res = await put<void>(`/user/${userId}`, data)
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
