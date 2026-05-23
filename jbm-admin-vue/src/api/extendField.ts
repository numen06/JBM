import { get, post, put, unwrap } from './request'
import type {
  ExtendFormDefinition,
  FieldDefinition,
  SaveExtendFormRequest,
} from './types'

const BASE = '/extend-field/forms'

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
