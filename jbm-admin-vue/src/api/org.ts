import { post, unwrap } from './request'
import type { BaseOrg, DataPaging } from './types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export async function pageOrgs(page = 1, size = DEFAULT_PAGE_SIZE, keyword?: string) {
  const body: Record<string, unknown> = {
    pageForm: { currPage: page, pageSize: size },
  }
  const kw = keyword?.trim()
  if (kw) {
    body.baseOrg = { orgName: kw }
  }
  const res = await post<DataPaging<BaseOrg>>('/baseOrg/pageList', body)
  return unwrap(res)
}

export async function listOrgTree() {
  const res = await post<BaseOrg[]>('/baseOrg/tree', {})
  return unwrap(res)
}

export async function listOrgRoots() {
  const res = await post<BaseOrg[]>('/baseOrg/root', {})
  return unwrap(res)
}

function toOrgModel(data: Partial<BaseOrg>) {
  const model: Record<string, unknown> = {}
  const id = data.id ?? data.orgId
  if (id != null) model.id = id
  if (data.orgName != null) model.orgName = data.orgName.trim()
  if (data.parentId != null && String(data.parentId) !== '') {
    model.parentId = Number(data.parentId)
  }
  if (data.sort != null && String(data.sort) !== '') {
    model.sort = Number(data.sort)
  } else if (data.sort === 0) {
    model.sort = 0
  }
  if (data.status != null && String(data.status) !== '') {
    model.status = Number(data.status)
  }
  if (data.orgCode != null) model.orgCode = data.orgCode
  if (data.orgType != null) model.orgType = data.orgType
  if (data.managerId != null) model.managerId = Number(data.managerId)
  return model
}

export async function saveOrg(data: Partial<BaseOrg>) {
  const res = await post<BaseOrg>('/baseOrg/save', { baseOrg: toOrgModel(data) })
  return unwrap(res)
}

export async function deleteOrg(data: Partial<BaseOrg>) {
  const res = await post<void>('/baseOrg/delete', { baseOrg: toOrgModel(data) })
  return unwrap(res)
}
