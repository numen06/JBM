import { get, unwrap } from './request'

export interface AuthorityResource {
  resourceId?: string
  resourceName?: string
  resourceType?: string
}

export interface AuthorityApi {
  apiId?: string
  apiName?: string
  path?: string
  serviceId?: string
}

export interface AuthorityMenu {
  menuId?: number
  menuName?: string
  parentId?: number
}

export async function listResources() {
  const res = await get<AuthorityResource[]>('/authority/resources')
  return unwrap(res)
}

export async function listApis(serviceId?: string) {
  const res = await get<AuthorityApi[]>('/authority/apis', {
    params: serviceId ? { serviceId } : {},
  })
  return unwrap(res)
}

export async function listAuthorityMenus() {
  const res = await get<AuthorityMenu[]>('/authority/menus')
  return unwrap(res)
}

export async function listMenuTree(appId?: number) {
  const res = await get<Record<string, unknown>[]>('/authority/menus/tree', {
    params: appId ? { appId } : {},
  })
  return unwrap(res)
}
