import { get, post, put, unwrap } from './request'
import type {
  DataPaging,
  CustomFormDesign,
  ExtendFormDefinition,
  FieldDefinition,
  SaveExtendFormRequest,
} from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

const BASE = '/extend-field/forms'
const CUSTOM_FORMS_BASE = '/customForms'

export async function pageExtendForms(
  page = 1,
  size = DEFAULT_PAGE_SIZE,
  keyword?: string,
) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = keyword?.trim()
  if (kw) params.keyword = kw
  const res = await get<DataPaging<ExtendFormDefinition>>(BASE, { params })
  return unwrap(res)
}

/** @deprecated 使用 pageExtendForms */
export async function listExtendForms() {
  const page = await pageExtendForms(1, DEFAULT_PAGE_SIZE)
  return page.contents ?? []
}

export async function saveExtendForm(formCode: string, request: SaveExtendFormRequest) {
  const res = await post<ExtendFormDefinition>(`${BASE}/${encodeURIComponent(formCode)}`, request)
  return unwrap(res)
}

export async function updateExtendForm(formCode: string, request: SaveExtendFormRequest) {
  const res = await put<ExtendFormDefinition>(`${BASE}/${encodeURIComponent(formCode)}`, request)
  return unwrap(res)
}

export async function publishExtendForm(formCode: string) {
  const res = await post<boolean>(`${BASE}/${encodeURIComponent(formCode)}/publish`)
  return unwrap(res)
}

export async function getExtendFormFromDb(formCode: string) {
  const res = await get<ExtendFormDefinition>(`${BASE}/${encodeURIComponent(formCode)}`)
  return unwrap(res)
}

export async function listFieldDefinitions(formCode: string) {
  const res = await get<FieldDefinition[]>(
    `${BASE}/${encodeURIComponent(formCode)}/definitions`,
  )
  return unwrap(res)
}

export async function getCustomFormDesignDetail(codeOrId: string | number) {
  const body =
    typeof codeOrId === 'number' || /^\d+$/.test(String(codeOrId))
      ? { id: Number(codeOrId) }
      : { code: String(codeOrId) }
  const res = await post<CustomFormDesign>(`${CUSTOM_FORMS_BASE}/getDetail`, body)
  return unwrap(res)
}

export async function saveCustomFormDesign(request: CustomFormDesign) {
  const res = await post<CustomFormDesign>(`${CUSTOM_FORMS_BASE}/saveData`, request)
  return unwrap(res)
}
