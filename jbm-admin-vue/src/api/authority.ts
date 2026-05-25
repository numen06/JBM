import { get, put, unwrap } from './request'

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
  menuCode?: string
  menuName?: string
  path?: string
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

/** 权限目录：type=1 菜单+按钮，type=2 API */
export async function listAuthorityCatalog(type = '1') {
  const res = await get<OpenAuthority[]>('/authority/catalog', { params: { type } })
  return unwrap(res)
}

export async function listGrantableApis() {
  const res = await get<OpenAuthority[]>('/authority/apis/grantable')
  return unwrap(res)
}

export async function listMenuTree(appId?: number) {
  const res = await get<Record<string, unknown>[]>('/authority/menus/tree', {
    params: appId ? { appId } : {},
  })
  return unwrap(res)
}

export interface OpenAuthority {
  authorityId?: string
  authority?: string
}

export async function getRoleAuthorities(roleId: number) {
  const res = await get<OpenAuthority[]>(`/authority/roles/${roleId}`)
  return unwrap(res)
}

export async function putRoleAuthorities(roleId: number, authorityIds: string[]) {
  const res = await put<void>(`/authority/roles/${roleId}`, { authorityIds })
  return unwrap(res)
}

export async function getUserAuthorities(userId: number) {
  const res = await get<OpenAuthority[]>(`/authority/users/${userId}`)
  return unwrap(res)
}

export async function putUserAuthorities(userId: number, authorityIds: string[]) {
  const res = await put<void>(`/authority/users/${userId}`, { authorityIds })
  return unwrap(res)
}

export async function getAppAuthorities(appId: number) {
  const res = await get<OpenAuthority[]>(`/authority/apps/${appId}`)
  return unwrap(res)
}

export async function putAppAuthorities(
  appId: number,
  authorityIds: string[],
  expireTime?: string,
) {
  const res = await put<void>(`/authority/apps/${appId}`, { authorityIds, expireTime })
  return unwrap(res)
}
