import http, { post, unwrap, withServicePrefix } from './request'
import type { DataPaging, ResultBody } from './types'
import { apiBaseUrl } from '@/runtimeConfig'

export interface BigscreenView {
  id: string
  appId?: string
  projectId?: string
  viewName: string
  viewUrl: string
  version?: string
  updateTime?: string
  deployed?: boolean
  packageAvailable?: boolean
}

export async function listBigscreens(page = 1, size = 100, projectId = '') {
  const response = await post<DataPaging<BigscreenView>>('/bigscreen/bigscreenView/pageList', {
    pageForm: { currPage: page, pageSize: size },
    bigscreenView: projectId.trim() ? { projectId: projectId.trim() } : {},
  })
  return unwrap(response)
}

export async function uploadBigscreenPackage(payload: {
  file: File
  viewName: string
  projectId: string
  appId?: string
  id?: string
}) {
  const form = new FormData()
  form.set('package', payload.file)
  form.set('viewName', payload.viewName.trim())
  form.set('projectId', payload.projectId.trim())
  if (payload.appId?.trim()) form.set('appId', payload.appId.trim())
  if (payload.id) form.set('id', payload.id)
  const { data } = await http.post<ResultBody<BigscreenView>>(
    withServicePrefix('/bigscreen/bigscreenView/package'),
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return unwrap(data)
}

export async function deleteBigscreen(id: string) {
  const response = await post<boolean>('/bigscreen/bigscreenView/delete', { id })
  return unwrap(response)
}

export async function reloadBigscreen(id: string) {
  const response = await post<BigscreenView>('/bigscreen/bigscreenView/reload', { id })
  return unwrap(response)
}

export async function cleanBigscreen(id: string) {
  const response = await post<boolean>('/bigscreen/bigscreenView/cleanView', { id })
  return unwrap(response)
}

export function bigscreenContentUrl(viewUrl: string, version?: string) {
  const path = withServicePrefix(`/bigscreen/static/${encodeURIComponent(viewUrl)}/index.html`)
  const suffix = version ? `?v=${encodeURIComponent(version)}` : ''
  return `${apiBaseUrl.replace(/\/$/, '')}${path}${suffix}`
}
