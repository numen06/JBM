declare global {
  interface Window {
    JBM_ADMIN_CONFIG?: {
      apiBaseUrl?: string
    }
  }
}

function normalizeBaseUrl(value: string | undefined, fallback: string): string {
  const raw = value?.trim() || fallback
  if (!raw) return ''
  if (/^https?:\/\//i.test(raw)) {
    return raw.replace(/\/+$/, '')
  }
  return `/${raw.replace(/^\/+|\/+$/g, '')}/`
}

export const apiBaseUrl = normalizeBaseUrl(
  window.JBM_ADMIN_CONFIG?.apiBaseUrl || import.meta.env.VITE_API_BASE_URL,
  import.meta.env.DEV ? '' : '/v3/api/',
)
