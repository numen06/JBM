import { post, unwrap } from './request'
import type { DataPaging, PushMessage, PushMessageQuery } from './types'

const BASE = '/jbm-cluster-platform-push/pushMessage'

export async function listCurrentMessages(
  page = 1,
  size = 10,
  query: Omit<PushMessageQuery, 'pageForm'> = {},
) {
  const res = await post<DataPaging<PushMessage>>(`${BASE}/findCurrMessagePage`, {
    ...query,
    pageForm: {
      currPage: page,
      pageSize: size,
      sortRule: 'createTime:desc',
    },
  })
  return unwrap(res)
}

export async function getUnreadMessageCount() {
  const res = await post<number>(`${BASE}/unreadCount`, {})
  return unwrap(res) ?? 0
}

export async function markMessagesRead(ids: string[]) {
  if (!ids.length) return
  const res = await post<string>(`${BASE}/read`, { ids })
  return unwrap(res)
}

export async function markMessagesUnread(ids: string[]) {
  if (!ids.length) return
  const res = await post<string>(`${BASE}/unread`, { ids })
  return unwrap(res)
}

export async function deleteMessages(ids: string[]) {
  if (!ids.length) return
  const res = await post<string>(`${BASE}/deleteByIds`, { ids })
  return unwrap(res)
}
