import { del, post, unwrap } from './request'
import type { DataPaging, OnlineUserSearchForm, SysUserOnline } from './types'

export async function listOnlineUsers(
  page = 1,
  size = 20,
  search?: Pick<OnlineUserSearchForm, 'ipaddr' | 'userName'>,
) {
  const body: OnlineUserSearchForm = {
    pageForm: { currPage: page, pageSize: size },
    ...search,
  }
  const res = await post<DataPaging<SysUserOnline>>('/online/pageList', body)
  return unwrap(res)
}

export async function kickoutUser(tokenId: string) {
  const res = await del<void>(`/online/kickout/${encodeURIComponent(tokenId)}`)
  return unwrap(res)
}

export async function logoutUser(tokenId: string) {
  const res = await del<void>(`/online/logout/${encodeURIComponent(tokenId)}`)
  return unwrap(res)
}

export async function expireToken(tokenId: string, minutes: number) {
  const res = await post<string>('/online/expire', null, {
    params: { tokenId, minutes },
  })
  return unwrap(res)
}

export async function expireTokenImmediately(tokenId: string) {
  const res = await post<string>('/online/expireImmediately', null, {
    params: { tokenId },
  })
  return unwrap(res)
}
