import { post, unwrap } from './request'
import type { DataPaging, PushMessage, PushMessageQuery } from './types'

const BASE = '/push/pushMessage'

export async function listCurrentMessages(
  page = 1,
  size = 10,
  query: Omit<PushMessageQuery, 'pageForm'> = {},
) {
  const keyword = query.keyword?.trim()
  const res = await post<DataPaging<PushMessage>>(`${BASE}/findCurrMessagePage`, {
    ...query,
    pageForm: {
      currPage: page,
      pageSize: size,
      sortRule: 'createTime:desc',
      ...(keyword ? { keyword } : {}),
    },
  })
  return unwrap(res)
}

export async function listMessageRecords(
  page = 1,
  size = 10,
  query: Omit<PushMessageQuery, 'pageForm'> = {},
) {
  const keyword = query.keyword?.trim()
  const res = await post<DataPaging<PushMessage>>(`${BASE}/pageList`, {
    ...query,
    pageForm: {
      currPage: page,
      pageSize: size,
      sortRule: 'createTime:desc',
      ...(keyword ? { keyword } : {}),
    },
  })
  return unwrap(res)
}

async function countMessageRecords(query: Omit<PushMessageQuery, 'pageForm'> = {}) {
  const data = await listMessageRecords(1, 1, query)
  return data?.total ?? 0
}

export async function getMessageRecordStats() {
  const [total, unread, read, system, user, failed] = await Promise.all([
    countMessageRecords(),
    countMessageRecords({ readFlag: false }),
    countMessageRecords({ readFlag: true }),
    countMessageRecords({ sourceType: 'system' }),
    countMessageRecords({ sourceType: 'user' }),
    countMessageRecords({ pushStatus: 'fail' }),
  ])
  return { total, unread, read, system, user, failed }
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

export async function markCurrentMessagesAllRead() {
  const res = await post<string>(`${BASE}/readAllCurr`, {})
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
