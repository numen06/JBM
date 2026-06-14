import http, { get, post, unwrap, withServicePrefix } from './request'
import { apiBaseUrl } from '@/runtimeConfig'
import type { DataPaging, ResultBody } from './types'

export interface BaseDoc {
  docId?: string
  docName?: string
  size?: number
  docGroupId?: string
  docGroup?: string
  docPath?: string
  state?: string
  contentType?: string
  effectiveTime?: number
  expirationTime?: string
  version?: unknown
  creator?: string | number
  createTime?: string
  updateTime?: string
}

export interface BaseDocGroup {
  groupId?: string
  groupPath?: string
  expirationTime?: string
  autoClear?: boolean
  maxQuantity?: number
  tokenKey?: string
  docGroupName?: string
  createTime?: string
  updateTime?: string
}

export interface DocListQuery {
  keyword?: string
  docName?: string
  docPath?: string
  docGroup?: string
  contentType?: string
  state?: string
}

export interface DocSyncStorageResult {
  scanned: number
  created: number
  skipped: number
  failed: number
  backend?: string
  endpointUrl?: string
  bucket?: string
  localDir?: string
}

export interface DocTextPayload {
  doc?: BaseDoc
  content: string
  encoding?: string
  editable?: boolean
  maxSize?: number
}

function pageForm(page: number, size: number) {
  return { currPage: page, pageSize: size, sortRule: 'updateTime:desc' }
}

function cleanDocQuery(query: DocListQuery = {}) {
  const keyword = query.keyword?.trim()
  return {
    ...(query.docName?.trim() ? { docName: query.docName.trim() } : {}),
    ...(query.docPath?.trim() ? { docPath: query.docPath.trim() } : {}),
    ...(query.docGroup?.trim() ? { docGroup: query.docGroup.trim() } : {}),
    ...(query.contentType?.trim() ? { contentType: query.contentType.trim() } : {}),
    ...(query.state?.trim() ? { state: query.state.trim() } : {}),
    ...(keyword ? { docName: keyword } : {}),
  }
}

export async function listDocs(page = 1, size = 20, query: DocListQuery = {}) {
  const res = await post<DataPaging<BaseDoc>>('/doc/baseDoc/pageList', {
    baseDoc: cleanDocQuery(query),
    pageForm: pageForm(page, size),
  })
  return unwrap(res)
}

export async function getDocModel(docId: string) {
  const res = await post<BaseDoc | null>('/doc/baseDoc/model', { baseDoc: { docId } })
  return unwrap(res)
}

export async function findDocByPath(docPath: string) {
  const page = await listDocs(1, 1, { docPath })
  return page.contents?.[0] ?? null
}

export async function uploadDoc(file: File, group?: string) {
  const form = new FormData()
  form.append('file', file)
  const params = group?.trim() ? { group: group.trim() } : undefined
  const { data } = await http.post<ResultBody<string>>(withServicePrefix('/doc/upload'), form, {
    params,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(data)
}

export async function saveDoc(doc: Partial<BaseDoc>) {
  const res = await post<BaseDoc>('/doc/baseDoc/save', { baseDoc: doc })
  return unwrap(res)
}

export async function syncStorageDocs(prefix?: string) {
  const res = await post<DocSyncStorageResult>('/doc/baseDoc/syncStorage', prefix?.trim() ? { prefix: prefix.trim() } : {})
  return unwrap(res)
}

export async function getDocText(docPath: string) {
  const res = await post<DocTextPayload>('/doc/baseDoc/text/get', { baseDoc: { docPath } })
  return unwrap(res)
}

export async function saveDocText(docPath: string, content: string) {
  const res = await post<BaseDoc>('/doc/baseDoc/text/save', { baseDoc: { docPath, content } })
  return unwrap(res)
}

export async function createTempDocGroup(payload: Partial<BaseDocGroup> = {}) {
  const res = await post<BaseDocGroup>('/doc/baseDocGroup/createTempGroup', payload)
  return unwrap(res)
}

export async function uploadDocByToken(file: File, tokenKey: string) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<ResultBody<string>>(withServicePrefix('/doc/baseDocGroup/uploadByToken'), form, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Doc-Token-Key': tokenKey,
    },
  })
  return unwrap(data)
}

export async function listGroupItemsByToken(tokenKey: string) {
  const res = await post<BaseDoc[]>('/doc/baseDocGroup/findGroupItemByToken', {}, {
    headers: { 'Doc-Token-Key': tokenKey },
  })
  return unwrap(res) ?? []
}

export async function deleteDocsByPaths(paths: string[]) {
  const res = await post<boolean>('/doc/baseDoc/deleteByPaths', { paths })
  return unwrap(res)
}

export async function getDocViewUrl(fileUrl: string) {
  const res = await get<{ expires_in?: number; token?: string; wpsUrl?: string }>('/doc/getViewUrl', {
    params: { fileUrl },
  })
  return unwrap(res)
}

export async function getDocBlob(docPath: string) {
  const { data } = await http.get<Blob>(withServicePrefix(`/doc/get/${encodeDocPath(docPath)}`), {
    responseType: 'blob',
  })
  return data
}

function encodeDocPath(path: string) {
  return path
    .split('/')
    .filter(Boolean)
    .map((part) => encodeURIComponent(part))
    .join('/')
}

export function docInlineUrl(docPath?: string) {
  if (!docPath) return ''
  return `${apiBaseUrl.replace(/\/$/, '')}${withServicePrefix(`/doc/get/${encodeDocPath(docPath)}`)}`
}

export function docDownloadUrl(docPath?: string) {
  if (!docPath) return ''
  return `${apiBaseUrl.replace(/\/$/, '')}${withServicePrefix(`/doc/download/${encodeDocPath(docPath)}`)}`
}
