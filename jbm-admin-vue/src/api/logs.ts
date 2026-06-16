import { del, get, post, put, unwrap } from './request'
import type {
  BaseAccountLog,
  BusinessLogLine,
  BusinessLogSummary,
  ClusterAccessInfo,
  DataPaging,
  GatewayLog,
  GatewayLogFilterRule,
} from './types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export interface AccountLogQuery {
  account?: string
  loginIp?: string
  accountType?: string
  loginStatus?: boolean | string
}

export async function listAccountLogs(page = 1, size = DEFAULT_PAGE_SIZE, query: AccountLogQuery = {}) {
  const body: Record<string, unknown> = {
    pageForm: { currPage: page, pageSize: size },
  }
  const filters = Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== undefined && String(value).trim() !== ''),
  )
  if (Object.keys(filters).length) {
    body.baseAccountLogs = filters
  }
  const res = await post<DataPaging<BaseAccountLog>>('/baseAccountLogs/pageList', body)
  return unwrap(res)
}

export async function getClusterAccessInfo() {
  const res = await post<ClusterAccessInfo>('/logs/clusterAccess/getClusterAccessInfo', {})
  return unwrap(res)
}

export interface GatewayLogQuery {
  path?: string
  serviceId?: string
  method?: string
  status?: string
  ip?: string
  appKey?: string
  accessId?: string
  keyword?: string
}

export async function getGatewayLogDetail(accessId: string) {
  const res = await post<GatewayLog>('/logs/GatewayLogs/getByAccessId', { accessId })
  return unwrap(res)
}

export async function listGatewayLogs(page = 1, size = DEFAULT_PAGE_SIZE, query: GatewayLogQuery = {}) {
  const res = await post<DataPaging<GatewayLog>>('/logs/GatewayLogs/findLogs', {
    gatewayLogs: query,
    pageForm: { currPage: page, pageSize: size },
  })
  return unwrap(res)
}

export type GatewayLogFilterRulePayload = Pick<
  GatewayLogFilterRule,
  'ruleName' | 'enabled' | 'pathPattern' | 'method' | 'serviceId' | 'statusCode' | 'remark'
>

export async function listGatewayLogFilterRules() {
  const res = await get<GatewayLogFilterRule[]>('/logs/GatewayLogs/filterRules')
  return unwrap(res)
}

export async function createGatewayLogFilterRule(payload: GatewayLogFilterRulePayload) {
  const res = await post<GatewayLogFilterRule>('/logs/GatewayLogs/filterRules', payload)
  return unwrap(res)
}

export async function updateGatewayLogFilterRule(ruleId: string, payload: GatewayLogFilterRulePayload) {
  const res = await put<GatewayLogFilterRule>(`/logs/GatewayLogs/filterRules/${encodeURIComponent(ruleId)}`, payload)
  return unwrap(res)
}

export async function deleteGatewayLogFilterRule(ruleId: string) {
  const res = await del<boolean>(`/logs/GatewayLogs/filterRules/${encodeURIComponent(ruleId)}`)
  return unwrap(res)
}

export async function toggleGatewayLogFilterRule(ruleId: string, enabled: boolean) {
  const res = await post<GatewayLogFilterRule>(
    `/logs/GatewayLogs/filterRules/${encodeURIComponent(ruleId)}/toggle`,
    { enabled },
  )
  return unwrap(res)
}

export async function testGatewayLogFilterRule(payload: {
  path: string
  method?: string
  serviceId?: string
  statusCode?: string
}) {
  const res = await post<{ matched: boolean; rules: GatewayLogFilterRule[] }>('/logs/GatewayLogs/filterRules/test', payload)
  return unwrap(res)
}

export interface BusinessLogQuery {
  logId?: string
  module?: string
  operation?: string
  status?: string
  businessType?: string
  businessId?: string
  source?: string
  traceId?: string
  keyword?: string
}

export async function listBusinessLogs(page = 1, size = DEFAULT_PAGE_SIZE, query: BusinessLogQuery = {}) {
  const res = await post<DataPaging<BusinessLogSummary>>('/logs/businessLog/query', {
    ...query,
    pageForm: { currPage: page, pageSize: size },
  })
  return unwrap(res)
}

export async function getBusinessLogLines(logId: string) {
  const res = await get<BusinessLogLine[]>(`/logs/businessLog/get/${encodeURIComponent(logId)}`)
  return unwrap(res)
}

export async function getBusinessLogContent(logId: string) {
  const res = await get<string>(`/logs/businessLog/get/${encodeURIComponent(logId)}?format=full&formatted=false`)
  return unwrap(res)
}

export async function createBusinessLogDemo() {
  const res = await post<{ logId: string }>('/logs/businessLog/demo', { mode: 'frontend' })
  return unwrap(res)
}
