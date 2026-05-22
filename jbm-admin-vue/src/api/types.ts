export interface ResultBody<T = unknown> {
  code?: number
  message?: string
  result?: T
  success?: boolean
  httpStatus?: number
}

export interface PageForm {
  currPage?: number
  pageSize?: number
}

export interface DataPaging<T> {
  contents?: T[]
  total?: number
  totalPage?: number
  pageForm?: PageForm
}

export interface OAuth2TokenResult {
  access_token: string
  refresh_token?: string
  expires_in?: number
  token_type?: string
  scope?: string
}

export interface BaseUser {
  userId?: number
  userName?: string
  nickName?: string
  mobile?: string
  email?: string
  status?: number
  createTime?: string
}

export interface BaseRole {
  roleId?: number
  roleCode?: string
  roleName?: string
  status?: number
  remark?: string
}

export interface BaseMenu {
  menuId?: number
  menuCode?: string
  menuName?: string
  parentId?: number
  path?: string
  icon?: string
  sort?: number
  status?: number
  appId?: number
  children?: BaseMenu[]
}

export interface BaseOrg {
  orgId?: number
  orgName?: string
  parentId?: number
  sort?: number
  status?: number
  children?: BaseOrg[]
}

export interface BaseApp {
  appId?: number
  appName?: string
  appCode?: string
  clientId?: string
  status?: number
}

export interface BaseDic {
  dicId?: number
  dicCode?: string
  dicName?: string
  dicValue?: string
  parentId?: number
}

export interface BaseAuthority {
  authorityId?: number
  authority?: string
  authorityName?: string
  resourceType?: string
}

export interface GatewayRoute {
  routeId?: number
  routeName?: string
  path?: string
  serviceId?: string
  url?: string
  status?: number
}

export interface GatewayRateLimit {
  policyId?: number
  policyName?: string
  limitQuota?: number
  intervalUnit?: string
}

export interface GatewayIpLimit {
  policyId?: number
  policyName?: string
  ipAddress?: string
}

export interface BaseAccountLog {
  logId?: number
  userName?: string
  operation?: string
  createTime?: string
  ip?: string
}

export interface BaseDeveloper {
  developerId?: number
  developerName?: string
  userName?: string
  status?: number
}

export interface UserInfoStatistics {
  onlineUser?: number
  usersTotal?: number
}

export interface CurrentUser {
  userId?: number
  userName?: string
  nickName?: string
  avatar?: string
}
