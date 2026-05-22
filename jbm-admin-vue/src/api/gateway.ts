import { get, post, put, del, unwrap } from './request'
import type { GatewayRoute, GatewayRateLimit, GatewayIpLimit, DataPaging } from './types'
import { pageParams } from './user'

export async function listRoutes(page = 1, size = 20) {
  const res = await get<DataPaging<GatewayRoute>>('/gateway/routes', {
    params: pageParams(page, size),
  })
  return unwrap(res)
}

export async function listRateLimits(page = 1, size = 20) {
  const res = await get<DataPaging<GatewayRateLimit>>('/gateway/limit/rate', {
    params: pageParams(page, size),
  })
  return unwrap(res)
}

export async function listIpLimits(page = 1, size = 20) {
  const res = await get<DataPaging<GatewayIpLimit>>('/gateway/limit/ip', {
    params: pageParams(page, size),
  })
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
