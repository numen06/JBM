import http, { get, post, unwrap } from './request'
import type {
  DataPaging,
  OpenApiExportRequest,
  OpenApiOperationDetail,
  OpenApiOperationQuery,
  OpenApiOperationView,
  OpenApiPublishRequest,
  OpenApiSource,
  OpenApiSyncResult,
  OpenApiTestRequest,
  OpenApiTestResult,
  OpenApiUseCaseSaveRequest,
  PublishedDocSummary,
} from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

function buildOperationParams(page: number, size: number, query?: OpenApiOperationQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  if (query?.serviceId) params.serviceId = query.serviceId
  if (query?.keyword) params.keyword = query.keyword
  if (query?.method) params.method = query.method
  if (query?.isOpen !== undefined && query.isOpen !== '') params.isOpen = Number(query.isOpen)
  if (query?.isAuth !== undefined && query.isAuth !== '') params.isAuth = Number(query.isAuth)
  if (query?.status !== undefined && query.status !== '') params.status = Number(query.status)
  if (query?.syncState) params.syncState = query.syncState
  if (query?.linked !== undefined) params.linked = query.linked
  if (query?.tag) params.tag = query.tag
  return params
}

export async function listOpenApiSources() {
  const res = await get<OpenApiSource[]>('/api-docs/sources')
  return unwrap(res) ?? []
}

export async function getOpenApiSpec(serviceId: string) {
  const res = await get<string>(`/api-docs/spec/${encodeURIComponent(serviceId)}`)
  return unwrap(res)
}

export async function listOpenApiOperations(page = 1, size = DEFAULT_PAGE_SIZE, query?: OpenApiOperationQuery) {
  const res = await get<DataPaging<OpenApiOperationView>>('/api-docs/operations', {
    params: buildOperationParams(page, size, query),
  })
  const raw = unwrap(res)
  if (raw && Array.isArray(raw.contents)) {
    return raw
  }
  return { contents: [], total: 0, pageForm: { currPage: page, pageSize: size } }
}

export async function getOpenApiOperation(operationId: number) {
  const res = await get<OpenApiOperationDetail>(`/api-docs/operations/${operationId}`)
  return unwrap(res)
}

export async function syncOpenApiDocs(serviceIds?: string[]) {
  const res = await post<OpenApiSyncResult[]>('/api-docs/sync', serviceIds?.length ? { serviceIds } : {})
  return unwrap(res) ?? []
}

export async function testOpenApiOperation(payload: OpenApiTestRequest) {
  const res = await post<OpenApiTestResult>('/api-docs/test', payload)
  return unwrap(res)
}

export async function saveOpenApiUseCase(operationId: number, payload: OpenApiUseCaseSaveRequest) {
  const res = await post<OpenApiOperationDetail>(`/api-docs/operations/${operationId}/use-cases`, payload)
  return unwrap(res)
}

export async function publishOpenApiDocs(payload: OpenApiPublishRequest) {
  const res = await post<PublishedDocSummary>('/api-docs/publish', payload)
  return unwrap(res)
}

export async function exportOpenApiDocs(payload: OpenApiExportRequest) {
  const response = await http.post('/api-docs/export', payload, { responseType: 'blob' })
  return response.data as Blob
}

/** 公开文档：仅读取已发布快照，不调用内部 /api-docs/** */
export async function listPublishedDocs() {
  const res = await get<PublishedDocSummary[]>('/published-docs/openapi')
  return unwrap(res) ?? []
}

export async function getPublishedDoc(docKey: string) {
  const res = await get<string>(`/published-docs/openapi/${encodeURIComponent(docKey)}`)
  return unwrap(res)
}
