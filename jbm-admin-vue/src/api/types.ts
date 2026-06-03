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
  sortRule?: string
  keyword?: string
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
  /** 首次登录或默认密码，须修改密码 */
  must_change_password?: boolean
}

export interface BaseUser {
  userId?: number
  userName?: string
  nickName?: string
  mobile?: string
  email?: string
  status?: number
  createTime?: string
  password?: string
  userType?: string
  companyId?: number
  departmentId?: number
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
  authorityId?: number
  icon?: string
  sort?: number
  priority?: number
  status?: number
  appId?: number
  isPersist?: boolean
  hidden?: number
  children?: BaseMenu[]
}

export interface BaseAction {
  actionId?: number
  actionCode?: string
  actionName?: string
  menuId?: number
  priority?: number
  status?: number
}

export interface BaseAccount {
  accountId?: number
  userId?: number
  account?: string
  accountType?: string
  status?: number
  domain?: string
}

export interface BaseOrg {
  id?: number
  orgId?: number
  orgName?: string
  orgCode?: string
  orgType?: string
  managerId?: number
  parentId?: number
  sort?: number
  status?: number
  children?: BaseOrg[]
}

export interface BaseUserOrg {
  id?: number
  userId?: number
  orgId?: number
  expireTime?: string
}

export interface BaseApp {
  appId?: number
  appName?: string
  appCode?: string
  clientId?: string
  orgId?: number
  status?: number
}

export interface BaseDic {
  id?: number | string
  dicId?: number | string
  code?: string
  dicCode?: string
  name?: string
  dicName?: string
  remark?: string
  dicValue?: string
  parentId?: number | string
  level?: number
  cssClass?: string
  listClass?: string
  serviceId?: string
  children?: BaseDic[]
}

export interface BaseAuthority {
  authorityId?: number
  authority?: string
  authorityName?: string
  resourceType?: string
}

export interface BaseApi {
  apiId?: number
  apiCode?: string
  apiName?: string
  serviceId?: string
  path?: string
  requestMethod?: string
  apiCategory?: string
  status?: number
  isAuth?: boolean | number
  isOpen?: number
  accessLog?: boolean
  isPersist?: boolean
  priority?: number
  businessScope?: string
  apiDesc?: string
}

export interface DiscoveryService {
  serviceId: string
  serviceName?: string
  instanceCount?: number
  healthyCount?: number
  versions?: string[]
  clusters?: string[]
  instances?: DiscoveryInstance[]
}

export interface DiscoveryInstance {
  instanceId?: string
  host?: string
  port?: number
  uri?: string
  secure?: boolean
  scheme?: string
  metadata?: Record<string, string>
  healthy?: boolean
  version?: string
  cluster?: string
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
  policyType?: string
  limitQuota?: number
  intervalUnit?: string
}

export interface GatewayIpLimit {
  policyId?: number
  policyName?: string
  policyType?: number
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
  userId?: number
  userName?: string
  nickName?: string
  userType?: string
  status?: number
  password?: string
  createTime?: string
}

export interface BaseApiKey {
  keyId?: number
  developerId?: number
  bizAppId?: number | null
  apiKey?: string
  secretKey?: string
  keyName?: string
  keyDesc?: string
  clientName?: string
  scopeModules?: string
  expireTime?: string
  status?: number
  lastUsedTime?: string
  createTime?: string
}

export interface OpenAuthority {
  authorityId?: string
  authority?: string
  expireTime?: string
}

export interface UserInfoStatistics {
  onlineUser?: number
  usersTotal?: number
}

/** 在线会话（对齐后端 SysUserOnline） */
export interface SysUserOnline {
  tokenId?: string
  userId?: number
  deptId?: number
  deptName?: string
  companyId?: number
  companyName?: string
  appId?: number
  appName?: string
  userName?: string
  ipaddr?: string
  loginLocation?: string
  browser?: string
  os?: string
  loginTime?: string
  expiredTime?: string
  activityTime?: string
}

export interface OnlineUserSearchForm {
  ipaddr?: string
  userName?: string
  appId?: number
  companyId?: number
  pageForm?: PageForm
}

export interface CurrentUser {
  userId?: number
  userName?: string
  nickName?: string
  avatar?: string
  roles?: BaseRole[]
  authorities?: { authorityId?: string; authority?: string }[]
}

export type PushMessageType = 'notification' | 'alarm' | 'alert' | string
export type PushMessageWay =
  | 'internal'
  | 'mqtt'
  | 'wechat'
  | 'miniapp'
  | 'email'
  | 'sms'
  | 'app'
  | string

export interface PushMessage {
  msgId?: string
  pushStatus?: string
  pushWay?: PushMessageWay
  readFlag?: boolean
  content?: unknown
  title?: string
  level?: number
  type?: PushMessageType
  createTime?: string
}

export interface PushMessageQuery {
  readFlag?: boolean
  pageForm?: PageForm
}

/** 扩展字段元数据（与后端 FieldDefinition 对齐） */
export interface FieldDefinition {
  fieldName: string
  fieldType: string
  fieldLabel: string
  required?: boolean
  sortable?: boolean
  queryable?: boolean
  defaultValue?: unknown
  options?: Record<string, unknown>
}

/** 扩展字段表单定义（库表真源） */
export interface ExtendFormDefinition {
  id?: number
  tenantId?: number
  formCode: string
  formName?: string
  fields: FieldDefinition[]
  version?: number
  customFormId?: number
  updateTime?: string
}

/** 保存扩展字段表单定义请求体 */
export interface SaveExtendFormRequest {
  formName?: string
  fields: FieldDefinition[]
  customFormId?: number
  autoPublish?: boolean
}
