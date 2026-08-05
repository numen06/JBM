export interface JbmAdminRuntimeConfig {
  apiBaseUrl?: string
  debug?: string | boolean
  oauthClientId?: string
  oauthAuthorizeBaseUrl?: string
}

declare global {
  interface Window {
    JBM_ADMIN_CONFIG?: JbmAdminRuntimeConfig
  }
}

export const runtimeConfig: JbmAdminRuntimeConfig = {}
export let apiBaseUrl = normalizeBaseUrl(
  import.meta.env.VITE_API_BASE_URL,
  import.meta.env.DEV ? '' : '/v3/api/',
)
export let adminDebugEnabled = false

export function configureRuntimeConfig(config: JbmAdminRuntimeConfig = {}) {
  for (const key of Object.keys(runtimeConfig) as Array<keyof JbmAdminRuntimeConfig>) {
    delete runtimeConfig[key]
  }
  Object.assign(runtimeConfig, config)
  apiBaseUrl = normalizeBaseUrl(
    runtimeConfig.apiBaseUrl || import.meta.env.VITE_API_BASE_URL,
    import.meta.env.DEV ? '' : '/v3/api/',
  )
  adminDebugEnabled = isRuntimeFlagEnabled(runtimeConfig.debug)
}

function normalizeBaseUrl(value: string | undefined, fallback: string): string {
  const raw = value?.trim() || fallback
  if (!raw) return ''
  if (/^https?:\/\//i.test(raw)) return raw.replace(/\/+$/, '')
  return `/${raw.replace(/^\/+|\/+$/g, '')}/`
}

export function isRuntimeFlagEnabled(value: string | boolean | undefined): boolean {
  if (typeof value === 'boolean') return value
  return ['1', 'true', 'on', 'yes'].includes(String(value ?? '').trim().toLowerCase())
}

export function isRuntimeFlagDisabled(value: string | boolean | undefined): boolean {
  if (typeof value === 'boolean') return !value
  return ['0', 'false', 'off', 'no'].includes(String(value ?? '').trim().toLowerCase())
}
