import type { SnowflakeId } from '@/lib/snowflakeId'

export interface ResultBody<T = unknown> {
  code?: number
  message?: string
  result?: T
  success?: boolean
  httpStatus?: number
}

/** 后端 Long / 雪花 ID；禁止 Number()，请用 @/lib/snowflakeId */
export type { SnowflakeId } from '@/lib/snowflakeId'

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
  userId?: SnowflakeId
  userName?: string
  nickName?: string
  realName?: string
  avatar?: string
  userDesc?: string
  mobile?: string
  email?: string
  status?: number
  createTime?: string
  password?: string
  userType?: string
  companyId?: SnowflakeId
  departmentId?: SnowflakeId
}

export interface BaseRole {
  roleId?: SnowflakeId
  appId?: SnowflakeId
  roleCode?: string
  roleName?: string
  status?: number
  remark?: string
}

export interface BaseMenu {
  menuId?: SnowflakeId
  menuCode?: string
  menuName?: string
  parentId?: SnowflakeId
  path?: string
  authorityId?: SnowflakeId
  icon?: string
  sort?: number
  priority?: number
  status?: number
  appId?: SnowflakeId
  isPersist?: boolean
  hidden?: number
  children?: BaseMenu[]
}

export interface BaseAction {
  actionId?: SnowflakeId
  actionCode?: string
  actionName?: string
  menuId?: SnowflakeId
  priority?: number
  status?: number
}

export interface BaseAccount {
  accountId?: SnowflakeId
  userId?: SnowflakeId
  account?: string
  accountType?: string
  status?: number
  domain?: string
}

export interface BaseOrg {
  id?: SnowflakeId
  orgId?: SnowflakeId
  orgName?: string
  orgCode?: string
  orgType?: string
  managerId?: SnowflakeId
  parentId?: SnowflakeId
  groupId?: string
  sort?: number
  status?: number
  children?: BaseOrg[]
}

export interface BaseUserOrg {
  id?: SnowflakeId
  userId?: SnowflakeId
  orgId?: SnowflakeId
  expireTime?: string
}

export interface BaseApp {
  appId?: SnowflakeId
  appName?: string
  /** 业务编码，对应后端 MasterDataEntity.code */
  code?: string
  /** @deprecated 旧前端别名，请使用 code */
  appCode?: string
  /** OAuth Client ID，对应后端 apiKey */
  apiKey?: string
  /** @deprecated 旧前端别名，请使用 apiKey */
  clientId?: string
  secretKey?: string
  orgId?: SnowflakeId
  status?: number
  isPersist?: number
  appType?: string
  redirectUris?: string
  publicClient?: boolean
  registrationEnabled?: boolean
  registrationDefaultRoleCode?: string
  extendData?: {
    oauth?: { redirectUris?: string[]; publicClient?: boolean }
    registration?: { enabled?: boolean; mode?: 'tenant'; defaultRoleCode?: string }
  }
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
  controlSummary?: ApiControlSummary
}

export interface ApiControlSummary {
  controlMode?: string
  visibility?: 'internal' | 'external' | string
  authentication?: 'required' | 'anonymous' | string
  authorityCount?: number | string
  apiKeyGrantCount?: number | string
  rateLimitPolicyCount?: number | string
  ipLimitPolicyCount?: number | string
  externallyControlled?: boolean
  internallyControlled?: boolean
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

export interface GatewayGrayTargetInstance {
  ip: string
  port: number
  weight: number
}

export interface GatewayGrayRule {
  id: string
  path: string
  serviceId?: string
  enabled: boolean
  percent: number
  headerName?: string
  headerValue?: string
  metadata: Record<string, string>
  targetInstances: GatewayGrayTargetInstance[]
  stickyHeader: string
}

export type JobStatus = 'NORMAL' | 'PAUSE' | 0 | 1
export type MisfirePolicy = 'DEFAULT' | 'IGNORE_MISFIRES' | 'FIRE_AND_PROCEED' | 'DO_NOTHING' | 0 | 1 | 2 | 3

export interface SysJob {
  id?: number
  jobId?: number
  jobName?: string
  jobGroup?: string
  invokeTarget?: string
  methodType?: string
  cronExpression?: string
  misfirePolicy?: MisfirePolicy
  concurrent?: boolean
  recordLog?: boolean
  status?: JobStatus
  createBy?: string
  updateBy?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export interface SysJobLog {
  id?: number
  jobLogId?: number
  jobName?: string
  runTime?: number
  jobGroup?: string
  invokeTarget?: string
  jobMessage?: string
  status?: string | number
  exceptionInfo?: string
  startTime?: string
  stopTime?: string
  createTime?: string
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
  id?: number | string
  logId?: number | string
  userId?: number | string
  account?: string
  userName?: string
  accountType?: string
  loginTime?: string | number
  loginIp?: string
  loginLocation?: string
  browser?: string
  os?: string
  loginAgent?: string
  loginStatus?: boolean
  loginNums?: number
  domain?: string
  message?: string
  operation?: string
  createTime?: string
  ip?: string
}

export interface ClusterAccessInfo {
  total?: number
  today?: number
}

export interface GatewayLog {
  id?: number | string
  logId?: number | string
  accessId?: string
  requestId?: string
  traceId?: string
  appKey?: string
  appName?: string
  headers?: string
  params?: string
  responseBody?: string
  userName?: string
  requestRealName?: string
  serviceId?: string
  path?: string
  method?: string
  requestMethod?: string
  status?: number | string
  httpStatus?: number | string
  spendTime?: number | string
  costTime?: number | string
  useTime?: number | string
  ip?: string
  requestIp?: string
  createTime?: string
  requestTime?: string
  responseTime?: string
  timestamp?: string
  error?: string
}

export interface GatewayLogFilterRule {
  ruleId?: string
  ruleName?: string
  enabled?: boolean
  builtin?: boolean
  pathPattern?: string
  method?: string
  serviceId?: string
  statusCode?: string
  remark?: string
  hitCount?: number
  lastHitTime?: string
  createTime?: string
  updateTime?: string
}

export interface BusinessLogSummary {
  logId?: string
  module?: string
  operation?: string
  username?: string
  userId?: string
  traceId?: string
  status?: string
  requestIp?: string
  businessType?: string
  businessId?: string
  source?: string
  totalLines?: number
  createTime?: string
  updateTime?: string
  expireDate?: string
}

export interface BusinessLogLine {
  logId?: string
  lineNumber?: number
  content?: string
  createTime?: string
  traceId?: string
  businessType?: string
  businessId?: string
  source?: string
  stageCode?: string
  stageName?: string
  stageStatus?: string
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
  keyId?: SnowflakeId
  developerId?: SnowflakeId
  bizAppId?: SnowflakeId | null
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
  currentSession?: boolean
  userId?: SnowflakeId
  deptId?: SnowflakeId
  deptName?: string
  companyId?: SnowflakeId
  companyName?: string
  appId?: SnowflakeId
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
  appId?: SnowflakeId
  companyId?: SnowflakeId
  pageForm?: PageForm
}

export interface CurrentUser {
  userId?: SnowflakeId
  userName?: string
  nickName?: string
  realName?: string
  userDesc?: string
  avatar?: string
  mobile?: string
  email?: string
  companyId?: SnowflakeId
  departmentId?: SnowflakeId
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
  msgBodyId?: SnowflakeId
  recUserId?: SnowflakeId
  sendUserId?: SnowflakeId
  sysMsg?: boolean
  pushStatus?: string
  pushWay?: PushMessageWay
  readFlag?: boolean
  content?: unknown
  title?: string
  level?: number
  type?: PushMessageType
  createTime?: string
  url?: string
  extend?: Record<string, unknown>
  testRunId?: string
  clientSentAt?: number
}

export interface PushMessageQuery {
  keyword?: string
  readFlag?: boolean
  type?: PushMessageType
  sourceType?: 'system' | 'user'
  pushWay?: PushMessageWay
  pushStatus?: string
  recUserId?: SnowflakeId
  pageForm?: PageForm
}

export interface PushConfigInfo {
  id?: number
  enable?: boolean
  type?: number
  releaseContent?: string
  createTime?: string
  updateTime?: string
}

export interface EmailPushConfig {
  id?: number
  host?: string
  username?: string
  password?: string
  port?: string
  createTime?: string
  updateTime?: string
}

export interface SmsNotificationRequest {
  phoneNumber: string
  templateCode: string
  signName: string
  params: Record<string, string | number>
  title?: string
  content?: string
  recUserId?: SnowflakeId
  sendUserId?: SnowflakeId
  sysMsg?: boolean
  showInMessageCenter?: boolean
}

export type ChannelPushWay = 'internal' | 'email' | 'sms' | 'mqtt' | 'wechat' | 'miniapp' | 'app'

export interface ChannelNotificationRequest {
  pushWay: ChannelPushWay
  title?: string
  content?: string
  recUserId?: SnowflakeId
  recUserIds?: SnowflakeId[]
  sendUserId?: SnowflakeId
  sysMsg?: boolean
  showInMessageCenter?: boolean
  phoneNumber?: string
  receiver?: string
  templateCode?: string
  signName?: string
  params?: Record<string, string | number>
  topic?: string
  body?: unknown
  qos?: number
}

export interface PushTestRequest {
  recUserIds?: SnowflakeId[]
  tags?: string
  title?: string
  content?: string
  pushMsgType?: PushMessageType
  extend?: Record<string, unknown>
  messageCount?: number
  batchSize?: number
  intervalMillis?: number
  waitAck?: boolean
  showInMessageCenter?: boolean
}

export interface PushTestTaskStatus {
  taskId?: string
  status?: string
  requestedMessages?: number
  resolvedUsers?: number
  startedAt?: number
  finishedAt?: number
  sentCount?: number
  failedCount?: number
  ackCount?: number
  avgLatencyMs?: number
  maxLatencyMs?: number
  errorMessage?: string
}

export interface PushTestAck {
  testRunId?: string
  msgId?: string
  recUserId?: SnowflakeId
  receivedAt?: number
  latencyMs?: number
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

/** 自定义表单字段明细（设计态真源） */
export interface CustomFormsItem {
  id?: number | string
  formId?: number | string
  fieldName: string
  labelName: string
  fieldType: 'text' | 'number' | 'date' | 'radio' | 'checkbox' | string
  componentType:
    | 'input'
    | 'textarea'
    | 'select'
    | 'inputNumber'
    | 'datePicker'
    | 'switchPicker'
    | 'radio'
    | 'checkbox'
    | 'cascader'
    | 'slot'
    | string
  format?: string
  decimalType?: string
  decimalValue?: number
  choiceType?: string
  choiceValue?: string
  dateType?: string
  isRequired?: boolean
  isShow?: boolean
  isFilter?: boolean
  fieldBelong?: string
  valueKey?: string
  labelKey?: string
  childrenKey?: string
}

/** 自定义表单（设计态真源） */
export interface CustomFormDesign {
  id?: number | string
  code?: string
  name?: string
  menuIds?: string
  formOrTable?: 'form' | 'table' | string
  dataSource?: string
  detail?: string
  customFormsItemList?: CustomFormsItem[]
  autoPublishExtendField?: boolean
}

export interface OpenApiSource {
  serviceId: string
  title?: string
  url?: string
  syncStatus?: string
  syncMessage?: string
  operationTotal?: number
  linkedApiTotal?: number
  unlinkedApiTotal?: number
  lastSyncTime?: string
}

export interface OpenApiOperationView {
  operationId?: number
  serviceId?: string
  method?: string
  requestMethod?: string
  path?: string
  tag?: string
  summary?: string
  apiId?: number
  apiCode?: string
  isOpen?: number
  isAuth?: boolean
  status?: number
  linked?: boolean
  syncState?: string
  deprecated?: number
}

export interface OpenApiOperationDetail extends OpenApiOperationView {
  docId?: number
  description?: string
  tags?: string
  parametersJson?: string
  requestBodyJson?: string
  responsesJson?: string
  schemasJson?: string
  securityJson?: string
  examplesJson?: string
  rawOperationJson?: string
  requestMethod?: string
}

export interface OpenApiSyncResult {
  serviceId?: string
  syncStatus?: string
  syncMessage?: string
  operationTotal?: number
  linkedApiTotal?: number
  unlinkedApiTotal?: number
  sourceHash?: string
  syncTime?: string
}

export interface OpenApiTestRequest {
  operationId?: number
  serviceId?: string
  path?: string
  method?: string
  pathParams?: Record<string, string>
  queryParams?: Record<string, string>
  headers?: Record<string, string>
  body?: string | null
  confirm?: boolean
  gatewayBaseUrl?: string
}

export interface OpenApiUseCaseSaveRequest {
  name?: string
  description?: string
  pathParams?: Record<string, string>
  queryParams?: Record<string, string>
  headers?: Record<string, string>
  body?: string | null
  requestUrl?: string
  success?: boolean
  responseStatus?: number
  responseHeaders?: Record<string, string>
  responseBody?: string
  errorType?: string
  errorMessage?: string
  durationMs?: number
}

export interface OpenApiTestResult {
  success?: boolean
  status?: number
  durationMs?: number
  headers?: Record<string, string>
  bodyPreview?: string
  truncated?: boolean
  target?: string
  requestUrl?: string
  errorType?: string
  errorMessage?: string
  authorizationApplied?: boolean
}

export interface OpenApiExportRequest {
  format?: string
  selectionMode?: string
  serviceIds?: string[]
  operationIds?: number[]
  filters?: Record<string, unknown>
  includeSchemas?: boolean
  includeExamples?: boolean
  includeGovernance?: boolean
}

export interface OpenApiPublishRequest {
  docKey?: string
  title?: string
  version?: string
  publishedSummary?: string
  format?: string
  selectionMode?: string
  serviceIds?: string[]
  operationIds?: number[]
  filters?: Record<string, unknown>
}

export interface PublishedDocSummary {
  docKey?: string
  title?: string
  version?: string
  contentType?: string
  publishedAt?: string
  publishedSummary?: string
  url?: string
}

export type OpenApiOperationQuery = {
  serviceId?: string
  keyword?: string
  method?: string
  isOpen?: number | string
  isAuth?: number | string
  status?: number | string
  syncState?: string
  linked?: boolean
  tag?: string
}

export interface WebhookEventConfig {
  eventId?: string
  businessEventCode?: string
  eventName?: string
  eventGroup?: string
  eventBody?: string
  internal?: boolean
  serviceName?: string
  enable?: boolean
  global?: boolean
  url?: string
  authHeader?: string
  methodType?: string
  batchTime?: string
  createTime?: string
  updateTime?: string
}

export interface WebhookTask {
  taskId?: string
  eventId?: string
  taskUrl?: string
  taskMethod?: string
  request?: string
  response?: string
  httpStatus?: number
  retryNumber?: number
  errorMsg?: string
  status?: string
  createTime?: string
  updateTime?: string
  eventName?: string
  eventGroup?: string
  businessEventCode?: string
  url?: string
}

export interface WebhookEventConfigQuery {
  keyword?: string
  businessEventCode?: string
  eventName?: string
  eventGroup?: string
  serviceName?: string
  enable?: boolean | ''
}

export interface WebhookTaskQuery {
  keyword?: string
  status?: string
  httpStatus?: number | ''
  businessEventCode?: string
  eventName?: string
  eventGroup?: string
  beginTime?: string
  endTime?: string
}
