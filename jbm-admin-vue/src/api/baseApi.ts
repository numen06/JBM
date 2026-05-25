import { get, post, put, del, patch, unwrap } from './request'
import type { BaseApi, DataPaging } from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export type BaseApiListQuery = {
  keyword?: string
  serviceId?: string
  requestMethod?: string
  status?: number | string
  isOpen?: number | string
  isAuth?: number | string | boolean
  accessLog?: boolean | string
  path?: string
  apiName?: string
  apiCode?: string
}

function buildListParams(page: number, size: number, query?: BaseApiListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = query?.keyword?.trim()
  if (query?.serviceId) params.serviceId = query.serviceId
  if (query?.requestMethod) params.requestMethod = query.requestMethod
  if (query?.status !== undefined && query.status !== '') params.status = Number(query.status)
  if (query?.isOpen !== undefined && query.isOpen !== '') params.isOpen = Number(query.isOpen)
  if (query?.isAuth !== undefined && query.isAuth !== '') {
    params.isAuth =
      query.isAuth === true || query.isAuth === 1 || query.isAuth === '1' ? 1 : 0
  }
  if (query?.accessLog !== undefined && query.accessLog !== '') {
    params.accessLog = query.accessLog === true || query.accessLog === 'true' ? 1 : 0
  }
  if (query?.path) params.path = query.path
  if (query?.apiName) params.apiName = query.apiName
  if (query?.apiCode) params.apiCode = query.apiCode
  if (kw) params.keyword = kw
  return params
}

function filterApis(list: BaseApi[], query?: BaseApiListQuery) {
  const kw = query?.keyword?.trim().toLowerCase()
  return list.filter((row) => {
    if (query?.serviceId && row.serviceId !== query.serviceId) return false
    if (query?.requestMethod && row.requestMethod !== query.requestMethod) return false
    if (query?.status !== undefined && query.status !== '' && row.status !== Number(query.status)) {
      return false
    }
    if (query?.isOpen !== undefined && query.isOpen !== '' && row.isOpen !== Number(query.isOpen)) {
      return false
    }
    if (query?.isAuth !== undefined && query.isAuth !== '') {
      const auth = row.isAuth === true || row.isAuth === 1
      const want = query.isAuth === true || query.isAuth === 1 || query.isAuth === '1'
      if (auth !== want) return false
    }
    if (query?.accessLog !== undefined && query.accessLog !== '') {
      const want = query.accessLog === true || query.accessLog === 'true'
      if (!!row.accessLog !== want) return false
    }
    if (!kw) return true
    return (
      row.path?.toLowerCase().includes(kw) ||
      row.apiName?.toLowerCase().includes(kw) ||
      row.apiCode?.toLowerCase().includes(kw) ||
      row.serviceId?.toLowerCase().includes(kw)
    )
  })
}

function paginateList<T>(list: T[], page: number, size: number): DataPaging<T> {
  const start = (page - 1) * size
  return {
    contents: list.slice(start, start + size),
    total: list.length,
    pageForm: { currPage: page, pageSize: size },
  }
}

export async function listBaseApis(
  page = 1,
  size = DEFAULT_PAGE_SIZE,
  query?: BaseApiListQuery,
) {
  const res = await get<DataPaging<BaseApi> | BaseApi[]>('/api', {
    params: buildListParams(page, size, query),
  })
  const raw = unwrap(res)
  if (Array.isArray(raw)) {
    return paginateList(filterApis(raw, query), page, size)
  }
  if (raw && Array.isArray(raw.contents)) {
    return raw
  }
  return paginateList([], page, size)
}

export async function getBaseApi(apiId: number) {
  const res = await get<BaseApi>(`/api/${apiId}`)
  return unwrap(res)
}

export async function createBaseApi(data: Partial<BaseApi>) {
  const res = await post<BaseApi>('/api', data)
  return unwrap(res)
}

export async function updateBaseApi(apiId: number, data: Partial<BaseApi>) {
  const res = await put<void>(`/api/${apiId}`, data)
  return unwrap(res)
}

export async function deleteBaseApi(apiId: number) {
  const res = await del<void>(`/api/${apiId}`)
  return unwrap(res)
}

export async function batchDeleteBaseApis(ids: Array<number | string>) {
  const res = await del<void>('/api', { params: { ids: ids.join(',') } })
  return unwrap(res)
}

export async function batchPatchApiOpen(ids: Array<number | string>, open: boolean) {
  const res = await patch<number>('/api', undefined, {
    params: { ids: ids.join(','), open },
  })
  return unwrap(res)
}

export async function batchPatchApiAuth(ids: Array<number | string>, auth: number) {
  const res = await patch<void>('/api', undefined, {
    params: { ids: ids.join(','), auth },
  })
  return unwrap(res)
}

export async function batchPatchApiStatus(ids: Array<number | string>, status: number) {
  const res = await patch<void>('/api', undefined, {
    params: { ids: ids.join(','), status },
  })
  return unwrap(res)
}

export async function batchPatchApiAccessLog(ids: Array<number | string>, accessLog: boolean) {
  const res = await patch<number>('/api', undefined, {
    params: { ids: ids.join(','), accessLog },
  })
  return unwrap(res)
}

export async function listBaseApiServices() {
  try {
    const res = await get<string[]>('/api/services')
    const fromApi = unwrap(res) ?? []
    if (fromApi.length) return fromApi.sort((a, b) => a.localeCompare(b))
  } catch {
    // fallback below
  }
  const data = await listBaseApis(1, 500)
  const set = new Set<string>()
  for (const row of data.contents ?? []) {
    if (row.serviceId) set.add(row.serviceId)
  }
  return [...set].sort((a, b) => a.localeCompare(b))
}

export { listBaseApiServices as listApiServices }
