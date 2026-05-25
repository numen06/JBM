import { get, post, unwrap } from './request'
import type { BaseDic, DataPaging } from './types'

export async function listDicts() {
  const res = await post<BaseDic[]>('/baseDic/list', {})
  return unwrap(res)
}

/** 字典分组（根节点，parentId 为空） */
export async function listRootDicts() {
  const res = await post<BaseDic[]>('/baseDic/root', {})
  return unwrap(res)
}

export async function pageDicts(page = 1, size = 20) {
  const res = await post<DataPaging<BaseDic>>('/baseDic/pageList', {
    pageForm: { currPage: page, pageSize: size },
  })
  return unwrap(res)
}

export async function getDicMap() {
  const res = await get<Record<string, BaseDic[]>>('/baseDic/getDicMap')
  return unwrap(res)
}

function toDicModel(data: Partial<BaseDic>) {
  const model: Record<string, unknown> = {
    id: data.id ?? data.dicId,
    code: data.code ?? data.dicCode,
    name: data.name ?? data.dicName,
    remark: data.remark ?? data.dicValue,
    parentId: data.parentId,
  }
  if (data.cssClass != null) model.cssClass = data.cssClass
  if (data.listClass != null) model.listClass = data.listClass
  if (data.serviceId != null) model.serviceId = data.serviceId
  return model
}

export async function saveDict(data: Partial<BaseDic>) {
  const res = await post<BaseDic>('/baseDic/save', { model: toDicModel(data) })
  return unwrap(res)
}

export async function deleteDict(data: Partial<BaseDic>) {
  const res = await post<void>('/baseDic/delete', { model: toDicModel(data) })
  return unwrap(res)
}
