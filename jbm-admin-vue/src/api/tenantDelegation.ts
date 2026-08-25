import { get, post, put, unwrap } from './request'
import type { DataPaging } from './types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export interface TenantDelegation {
  id?: string | number
  appId?: string | number
  ownerTenantId?: string | number
  operatorTenantId?: string | number
  operatorUserId?: string | number
  operatorAccount?: string
  status?: number
  permissionCodes?: string | string[]
  resourceTypes?: string | string[]
  dataScope?: string | { projectIds?: Array<string | number> }
  fieldPolicy?: string | Record<string, unknown>
  validFrom?: string
  validTo?: string
  purpose?: string
  version?: number
}

export async function listTenantDelegations(page = 1, size = DEFAULT_PAGE_SIZE) {
  const res = await get<DataPaging<TenantDelegation>>('/tenant-delegation', {
    params: { 'pageForm.currPage': page, 'pageForm.pageSize': size },
  })
  return unwrap(res)
}

export async function createTenantDelegation(data: Partial<TenantDelegation>) {
  const res = await post<TenantDelegation>('/tenant-delegation', data)
  return unwrap(res)
}

export async function updateTenantDelegation(id: string | number, data: Partial<TenantDelegation>) {
  const res = await put<TenantDelegation>(`/tenant-delegation/${id}`, data)
  return unwrap(res)
}
