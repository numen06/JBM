import { get, post, put, del, unwrap } from './request'
import type {
  GatewayRoute,
  GatewayRateLimit,
  GatewayIpLimit,
  GatewayGrayRule,
  DataPaging,
  DiscoveryService,
  DiscoveryInstance,
} from './types'
import { pageParams } from './user'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export type GatewayRouteListQuery = {
  keyword?: string
  status?: number | string
}

export async function listRoutes(page = 1, size = DEFAULT_PAGE_SIZE, query?: GatewayRouteListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = query?.keyword?.trim()
  if (kw) {
    params['gatewayRoute.routeName'] = kw
  }
  if (query?.status !== undefined && query.status !== '') {
    params['gatewayRoute.status'] = Number(query.status)
  }
  const res = await get<DataPaging<GatewayRoute>>('/gateway/routes', { params })
  return unwrap(res)
}

export type GatewayRateLimitListQuery = {
  keyword?: string
  policyType?: string
}

export async function listRateLimits(page = 1, size = DEFAULT_PAGE_SIZE, query?: GatewayRateLimitListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = query?.keyword?.trim()
  if (kw) params.policyName = kw
  if (query?.policyType) params.policyType = query.policyType
  const res = await get<DataPaging<GatewayRateLimit>>('/gateway/limit/rate', { params })
  return unwrap(res)
}

export type GatewayIpLimitListQuery = {
  keyword?: string
  policyType?: number | string
}

export async function listIpLimits(page = 1, size = DEFAULT_PAGE_SIZE, query?: GatewayIpLimitListQuery) {
  const params: Record<string, unknown> = { ...pageParams(page, size) }
  const kw = query?.keyword?.trim()
  if (kw) params.policyName = kw
  if (query?.policyType !== undefined && query.policyType !== '') {
    params.policyType = Number(query.policyType)
  }
  const res = await get<DataPaging<GatewayIpLimit>>('/gateway/limit/ip', { params })
  return unwrap(res)
}

export async function deleteRoute(routeId: number) {
  const res = await del<void>(`/gateway/routes/${routeId}`)
  return unwrap(res)
}

export async function createRoute(data: Partial<GatewayRoute>) {
  const res = await post<number>('/gateway/routes', data)
  return unwrap(res)
}

export async function updateRoute(routeId: number, data: Partial<GatewayRoute>) {
  const res = await put<void>(`/gateway/routes/${routeId}`, data)
  return unwrap(res)
}

export async function createRateLimit(data: Partial<GatewayRateLimit>) {
  const res = await post<number>('/gateway/limit/rate', data)
  return unwrap(res)
}

export async function updateRateLimit(policyId: number, data: Partial<GatewayRateLimit>) {
  const res = await put<void>(`/gateway/limit/rate/${policyId}`, data)
  return unwrap(res)
}

export async function deleteRateLimit(policyId: number) {
  const res = await del<void>(`/gateway/limit/rate/${policyId}`)
  return unwrap(res)
}

export async function createIpLimit(data: Partial<GatewayIpLimit>) {
  const res = await post<number>('/gateway/limit/ip', data)
  return unwrap(res)
}

export async function updateIpLimit(policyId: number, data: Partial<GatewayIpLimit>) {
  const res = await put<void>(`/gateway/limit/ip/${policyId}`, data)
  return unwrap(res)
}

export async function deleteIpLimit(policyId: number) {
  const res = await del<void>(`/gateway/limit/ip/${policyId}`)
  return unwrap(res)
}

export async function listDiscoveryServices() {
  const res = await get<DiscoveryService[] | string[]>('/gateway/discovery/services')
  const raw = unwrap(res) ?? []
  return raw.map((item) => {
    if (typeof item === 'string') {
      return { serviceId: item } satisfies DiscoveryService
    }
    return item
  })
}

export async function listDiscoveryInstances(serviceId: string) {
  const res = await get<DiscoveryInstance[]>(`/gateway/discovery/services/${encodeURIComponent(serviceId)}/instances`)
  return unwrap(res) ?? []
}

export async function listGrayRules() {
  const res = await get<GatewayGrayRule[]>('/gateway/gray-routes')
  return unwrap(res) ?? []
}

export async function createGrayRule(data: Partial<GatewayGrayRule>) {
  const res = await post<GatewayGrayRule>('/gateway/gray-routes', data)
  return unwrap(res)
}

export async function deleteGrayRule(ruleId: string) {
  const res = await del<boolean>(`/gateway/gray-routes/${encodeURIComponent(ruleId)}`)
  return unwrap(res)
}
