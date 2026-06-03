import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { extractApiError } from '@/lib/errors'
import { apiBaseUrl } from '@/runtimeConfig'
import type { ResultBody } from './types'

const http: AxiosInstance = axios.create({
  baseURL: apiBaseUrl,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' },
})

const AUTH_PATH_PREFIXES = [
  '/oauth2',
  '/captcha',
  '/qrcode',
  '/online',
  '/token',
  '/internal/dev',
  '/internal/trust',
]

const JBM_SERVICE_PATH_PREFIXES = [
  '/auth',
  '/center',
  '/doc',
  '/push',
  '/logs',
  '/bigscreen',
  '/job',
  '/weixin',
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
    if (matchesPathPrefix(path, fullPrefix)) {
      return `${shortPrefix}${path.slice(fullPrefix.length)}`
    }
  }
  return path
}

export function withServicePrefix(url: string): string {
  if (!url || /^https?:\/\//i.test(url)) {
    return url
  }
  const normalized = url.startsWith('/') ? url : `/${url}`
  const serviceNormalized = normalizeJbmServiceAlias(normalized)
  if (hasServicePrefix(serviceNormalized)) {
    return serviceNormalized
  }
  const queryIndex = serviceNormalized.search(/[?#]/)
  const path = queryIndex >= 0 ? serviceNormalized.slice(0, queryIndex) : serviceNormalized
  const suffix = queryIndex >= 0 ? serviceNormalized.slice(queryIndex) : ''
  const service = AUTH_PATH_PREFIXES.some((prefix) => matchesPathPrefix(path, prefix))
    ? '/auth'
    : '/center'
  return `${service}${path}${suffix}`
}

http.interceptors.request.use((config) => {
  if (config.url) {
    config.url = withServicePrefix(config.url)
  }
  const auth = useAuthStore()
  if (auth.accessToken) {
    const token = auth.accessToken.startsWith('Bearer ')
      ? auth.accessToken
      : `Bearer ${auth.accessToken}`
    config.headers.Authorization = token
  }
  if (auth.tenantId) {
    config.headers.tenantId = auth.tenantId
  }
  return config
})

let refreshing = false

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status
    const config = error.config as AxiosRequestConfig & { _retry?: boolean }
    if (status === 401 && !config._retry) {
      const auth = useAuthStore()
      if (auth.refreshToken && !refreshing) {
        refreshing = true
        config._retry = true
        try {
          await auth.refreshAccessToken()
          refreshing = false
          return http(config)
        } catch {
          refreshing = false
          auth.clearSession()
          router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
        }
      } else {
        auth.clearSession()
        router.push({ name: 'login' })
      }
    }
    const message = extractApiError(error, '请求失败')
    return Promise.reject(new Error(message))
  },
)

export function isOk<T>(body: ResultBody<T> | undefined): boolean {
  if (!body) return false
  return body.success === true || body.code === 200
}

export function unwrap<T>(body: ResultBody<T>): T {
  if (!isOk(body)) {
    const code = body.code != null ? ` [${body.code}]` : ''
    const msg = body.message?.trim() || '请求失败'
    throw new Error(`${msg}${code}`)
  }
  return body.result as T
}

export async function get<T>(url: string, config?: AxiosRequestConfig) {
  const { data } = await http.get<ResultBody<T>>(withServicePrefix(url), config)
  return data
}

export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
  const { data } = await http.post<ResultBody<T>>(withServicePrefix(url), body, config)
  return data
}

export async function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
  const { data } = await http.put<ResultBody<T>>(withServicePrefix(url), body, config)
  return data
}

export async function del<T>(url: string, config?: AxiosRequestConfig) {
  const { data } = await http.delete<ResultBody<T>>(withServicePrefix(url), config)
  return data
}

export async function patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig) {
  const { data } = await http.patch<ResultBody<T>>(withServicePrefix(url), body, config)
  return data
}

export async function postForm<T>(
  url: string,
  params: URLSearchParams,
  config?: AxiosRequestConfig,
) {
  const { data } = await http.post<ResultBody<T>>(withServicePrefix(url), params, {
    ...config,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      ...(config?.headers as Record<string, string> | undefined),
    },
  })
  return data
}

export default http
