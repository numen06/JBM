import { get, post, put, del, unwrap } from './request'
import type { BaseApiKey, DataPaging, OpenAuthority } from './types'
import { pageParams } from './user'

export async function listApiKeys(page = 1, size = 20, developerId?: number) {
  const res = await get<DataPaging<BaseApiKey>>('/apikey', {
    params: { ...pageParams(page, size), developerId },
  })
  return unwrap(res)
}

export async function getApiKey(keyId: number) {
  const res = await get<BaseApiKey>(`/apikey/${keyId}`)
  return unwrap(res)
}

export async function createApiKey(data: Partial<BaseApiKey> & { authorityIds?: string[] }) {
  const res = await post<BaseApiKey>('/apikey', data)
  return unwrap(res)
}

export async function updateApiKey(keyId: number, data: Partial<BaseApiKey>) {
  const res = await put<BaseApiKey>(`/apikey/${keyId}`, data)
  return unwrap(res)
}

export async function resetApiKeySecret(keyId: number) {
  const res = await put<string>(`/apikey/${keyId}/secret`)
  return unwrap(res)
}

export async function updateApiKeyStatus(keyId: number, status: number) {
  const res = await put<void>(`/apikey/${keyId}/status`, { status })
  return unwrap(res)
}

export async function deleteApiKey(keyId: number) {
  const res = await del<void>(`/apikey/${keyId}`)
  return unwrap(res)
}

export async function getApiKeyAuthorities(keyId: number) {
  const res = await get<OpenAuthority[]>(`/apikey/${keyId}/authority`)
  return unwrap(res)
}

export async function grantApiKeyAuthorities(
  keyId: number,
  payload: { authorityIds: string[]; authorityExpireTime?: string },
) {
  const res = await put<void>(`/apikey/${keyId}/authority`, payload)
  return unwrap(res)
}
