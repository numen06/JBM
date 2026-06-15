declare global {
  interface Window {
    JBM_ADMIN_CONFIG?: {
      apiBaseUrl?: string
      debug?: string | boolean
      localDevLogin?: string | boolean
      localDevPassword?: string
      localDevUsers?: string
      oauthClientId?: string
      oauthClientSecret?: string
      loginPassword?: string
    }
  }
}

export const runtimeConfig = window.JBM_ADMIN_CONFIG ?? {}

function normalizeBaseUrl(value: string | undefined, fallback: string): string {
  const raw = value?.trim() || fallback
  if (!raw) return ''
  if (/^https?:\/\//i.test(raw)) {
    return raw.replace(/\/+$/, '')
  }
  return `/${raw.replace(/^\/+|\/+$/g, '')}/`
}

export const apiBaseUrl = normalizeBaseUrl(
  runtimeConfig.apiBaseUrl || import.meta.env.VITE_API_BASE_URL,
  import.meta.env.DEV ? '' : '/v3/api/',
)

export function isRuntimeFlagEnabled(value: string | boolean | undefined): boolean {
  if (typeof value === 'boolean') return value
  return ['1', 'true', 'on', 'yes'].includes(String(value ?? '').trim().toLowerCase())
}

export function isRuntimeFlagDisabled(value: string | boolean | undefined): boolean {
  if (typeof value === 'boolean') return !value
  return ['0', 'false', 'off', 'no'].includes(String(value ?? '').trim().toLowerCase())
}

export const adminDebugEnabled = isRuntimeFlagEnabled(runtimeConfig.debug)
