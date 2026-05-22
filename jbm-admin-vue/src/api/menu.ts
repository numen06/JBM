import { get, post, put, del, unwrap } from './request'
import type { BaseMenu, DataPaging } from './types'
import { pageParams } from './user'

export async function listMenus(page = 1, size = 20) {
  const res = await get<DataPaging<BaseMenu>>('/menu', { params: pageParams(page, size) })
  return unwrap(res)
}

export async function listAllMenus(appId?: number) {
  const res = await get<BaseMenu[]>('/menu/all', { params: appId ? { appId } : {} })
  return unwrap(res)
}

export async function createMenu(data: Partial<BaseMenu>) {
  const res = await post<number>('/menu', data)
  return unwrap(res)
}

export async function updateMenu(menuId: number, data: Partial<BaseMenu>) {
  const res = await put<void>(`/menu/${menuId}`, data)
  return unwrap(res)
}

export async function deleteMenu(menuId: number) {
  const res = await del<void>(`/menu/${menuId}`)
  return unwrap(res)
}
