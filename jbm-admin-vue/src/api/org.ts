import { post, unwrap } from './request'
import type { BaseOrg } from './types'

export async function listOrgTree() {
  const res = await post<BaseOrg[]>('/baseOrg/tree', {})
  return unwrap(res)
}

export async function listOrgRoots() {
  const res = await post<BaseOrg[]>('/baseOrg/root', {})
  return unwrap(res)
}

export async function saveOrg(data: Partial<BaseOrg>) {
  const res = await post<BaseOrg>('/baseOrg/save', { model: data })
  return unwrap(res)
}

export async function deleteOrg(data: Partial<BaseOrg>) {
  const res = await post<void>('/baseOrg/delete', { model: data })
  return unwrap(res)
}
