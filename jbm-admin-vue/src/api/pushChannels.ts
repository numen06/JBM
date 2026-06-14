import { post, unwrap } from './request'
import type { DataPaging, EmailPushConfig, PushConfigInfo } from './types'

const CONFIG_BASE = '/push/pushConfigInfo'
const EMAIL_BASE = '/push/emailPushConfig'

export async function listPushConfigs(page = 1, size = 20, entity: Partial<PushConfigInfo> = {}) {
  const res = await post<DataPaging<PushConfigInfo>>(`${CONFIG_BASE}/pageList`, {
    ...entity,
    pageForm: {
      currPage: page,
      pageSize: size,
      sortRule: 'updateTime:desc',
    },
  })
  return unwrap(res)
}

export async function savePushConfig(entity: PushConfigInfo) {
  const res = await post<PushConfigInfo>(`${CONFIG_BASE}/save`, entity)
  return unwrap(res)
}

export async function deletePushConfigs(ids: number[]) {
  if (!ids.length) return false
  const res = await post<boolean>(`${CONFIG_BASE}/deleteByIds`, { ids })
  return unwrap(res)
}

export async function listEmailConfigs(page = 1, size = 20, entity: Partial<EmailPushConfig> = {}) {
  const res = await post<DataPaging<EmailPushConfig>>(`${EMAIL_BASE}/pageList`, {
    entity,
    pageForm: {
      currPage: page,
      pageSize: size,
      sortRule: 'updateTime:desc',
    },
  })
  return unwrap(res)
}

export async function saveEmailConfig(entity: EmailPushConfig) {
  const res = await post<EmailPushConfig>(`${EMAIL_BASE}/save`, { entity })
  return unwrap(res)
}

export async function deleteEmailConfigs(ids: number[]) {
  if (!ids.length) return false
  const res = await post<boolean>(`${EMAIL_BASE}/deleteByIds`, { ids })
  return unwrap(res)
}
