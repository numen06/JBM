import type { AxiosRequestConfig } from 'axios'
import { getPlatformClient } from '@/platformClient'
import { isJbmResultOk, unwrapJbmResult } from '@jbm7/sdk'
import type { ResultBody } from './types'

const AUTH_PATH_PREFIXES = [
  '/oauth2', '/captcha', '/qrcode', '/online', '/token', '/internal/dev', '/internal/trust',
]

const JBM_SERVICE_PATH_PREFIXES = [
  '/auth', '/center', '/doc', '/push', '/logs', '/bigscreen', '/job', '/weixin',
  '/gateway/discovery', '/gateway/gray-routes',
]

const JBM_SERVICE_ALIAS_MAP: Array<[string, string]> = [
  ['/jbm-cluster-platform-auth', '/auth'],
  ['/jbm-cluster-platform-center', '/center'],
  ['/jbm-cluster-platform-doc', '/doc'],
  ['/jbm-cluster-platform-push', '/push'],
  ['/jbm-cluster-platform-logs', '/logs'],
  ['/jbm-cluster-platform-bigscreen', '/bigscreen'],
  ['/jbm-cluster-platform-job', '/job'],
  ['/jbm-cluster-platform-weixin', '/weixin'],
]

function matchesPathPrefix(path: string, prefix: string): boolean {
  return path === prefix || path.startsWith(`${prefix}/`)
}

function hasServicePrefix(path: string): boolean {
  return JBM_SERVICE_PATH_PREFIXES.some((prefix) => matchesPathPrefix(path, prefix))
}

function normalizeJbmServiceAlias(path: string): string {
  for (const [fullPrefix, shortPrefix] of JBM_SERVICE_ALIAS_MAP) {
    if (matchesPathPrefix(path, fullPrefix)) return `${shortPrefix}${path.slice(fullPrefix.length)}`
  }
  return path
}

/** Compatibility routing belongs to the admin package; @jbm7/sdk itself never guesses a service. */
export function withServicePrefix(url: string): string {
  if (!url || /^https?:\/\//i.test(url)) return url
  const normalized = url.startsWith('/') ? url : `/${url}`
  const serviceNormalized = normalizeJbmServiceAlias(normalized)
  if (hasServicePrefix(serviceNormalized)) return serviceNormalized
  const queryIndex = serviceNormalized.search(/[?#]/)
  const path = queryIndex >= 0 ? serviceNormalized.slice(0, queryIndex) : serviceNormalized
  const suffix = queryIndex >= 0 ? serviceNormalized.slice(queryIndex) : ''
  const service = AUTH_PATH_PREFIXES.some((prefix) => matchesPathPrefix(path, prefix)) ? '/auth' : '/center'
  return `${service}${path}${suffix}`
}

function shouldSkipAuthRefresh(path: string) {
  return ['/auth/oauth2/token', '/auth/oauth2/refresh', '/auth/oauth2/callback'].some((item) =>
    matchesPathPrefix(path.split(/[?#]/)[0] ?? path, item),
  )
}

function requestOptions(config?: AxiosRequestConfig) {
  const headers = config?.headers ? Object.fromEntries(Object.entries(config.headers).map(([key, value]) => [key, String(value)])) : undefined
  return {
    params: config?.params as Record<string, unknown> | undefined,
    headers,
    responseType: config?.responseType,
    signal: config?.signal as AbortSignal | undefined,
  }
}

async function requestData<T>(method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE', url: string, body?: unknown, config?: AxiosRequestConfig) {
  const path = withServicePrefix(url)
  return getPlatformClient().request<T>(method, path, {
    ...requestOptions(config),
    body,
    skipAuthRefresh: shouldSkipAuthRefresh(path),
  })
}

const http = {
  async get<T>(url: string, config?: AxiosRequestConfig) {
    return { data: await requestData<T>('GET', url, undefined, config) }
  },
  async post<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
    return { data: await requestData<T>('POST', url, body, config) }
  },
  async put<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
    return { data: await requestData<T>('PUT', url, body, config) }
  },
  async patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
    return { data: await requestData<T>('PATCH', url, body, config) }
  },
  async delete<T>(url: string, config?: AxiosRequestConfig) {
    return { data: await requestData<T>('DELETE', url, undefined, config) }
  },
}

export function isOk<T>(body: ResultBody<T> | undefined): boolean {
  return isJbmResultOk(body)
}

export function unwrap<T>(body: ResultBody<T>): T {
  return unwrapJbmResult(body)
}

export async function get<T>(url: string, config?: AxiosRequestConfig) {
  return requestData<ResultBody<T>>('GET', url, undefined, config)
}

export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
  return requestData<ResultBody<T>>('POST', url, body, config)
}

export async function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
  return requestData<ResultBody<T>>('PUT', url, body, config)
}

export async function del<T>(url: string, config?: AxiosRequestConfig) {
  return requestData<ResultBody<T>>('DELETE', url, undefined, config)
}

export async function patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
  return requestData<ResultBody<T>>('PATCH', url, body, config)
}

export async function postForm<T>(url: string, params: URLSearchParams, config?: AxiosRequestConfig) {
  return requestData<ResultBody<T>>('POST', url, params, {
    ...config,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', ...config?.headers },
  })
}

export default http
