import { get, post, put, del, unwrap } from './request'
import type { BaseApiKey, DataPaging, OpenAuthority } from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { toSnowflakeIdString, type SnowflakeId } from '@/lib/snowflakeId'

export type ApiKeyListQuery = {
  developerId?: number
  keyword?: string
  status?: number | string
}

export async function listApiKeys(page = 1, size = DEFAULT_PAGE_SIZE, query?: ApiKeyListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  if (query?.developerId != null) params.developerId = query.developerId
  const kw = query?.keyword?.trim()
  if (kw) params.keyName = kw
  if (query?.status !== undefined && query.status !== '') {
    params.status = Number(query.status)
  }
  const res = await get<DataPaging<BaseApiKey>>('/apikey', { params })
  return unwrap(res)
}

export async function getApiKey(keyId: SnowflakeId) {
  const res = await get<BaseApiKey>(`/apikey/${toSnowflakeIdString(keyId)}`)
  return unwrap(res)
}

export async function createApiKey(data: Partial<BaseApiKey> & { authorityIds?: string[] }) {
  const res = await post<BaseApiKey>('/apikey', data)
  return unwrap(res)
}

export async function updateApiKey(keyId: SnowflakeId, data: Partial<BaseApiKey>) {
  const res = await put<BaseApiKey>(`/apikey/${toSnowflakeIdString(keyId)}`, data)
  return unwrap(res)
}

export async function resetApiKeySecret(keyId: SnowflakeId) {
  const res = await put<string>(`/apikey/${toSnowflakeIdString(keyId)}/secret`)
  return unwrap(res)
}

export async function updateApiKeyStatus(keyId: SnowflakeId, status: number) {
  const res = await put<void>(`/apikey/${toSnowflakeIdString(keyId)}/status`, { status })
  return unwrap(res)
}

export async function deleteApiKey(keyId: SnowflakeId) {
  const res = await del<void>(`/apikey/${toSnowflakeIdString(keyId)}`)
  return unwrap(res)
}

export async function getApiKeyAuthorities(keyId: SnowflakeId) {
  const res = await get<OpenAuthority[]>(`/apikey/${toSnowflakeIdString(keyId)}/authority`)
  return unwrap(res)
}

export async function grantApiKeyAuthorities(
  keyId: SnowflakeId,
  payload: { authorityIds: string[]; authorityExpireTime?: string },
) {
  const res = await put<void>(`/apikey/${toSnowflakeIdString(keyId)}/authority`, payload)
  return unwrap(res)
}
