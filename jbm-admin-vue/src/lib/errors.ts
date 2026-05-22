import type { AxiosError } from 'axios'
import type { ResultBody } from '@/api/types'

function pickString(...values: unknown[]): string | undefined {
  for (const v of values) {
    if (typeof v === 'string' && v.trim()) return v.trim()
  }
  return undefined
}

/** 从 ResultBody 或 Sa-Token 风格响应中提取可读错误 */
export function extractResultBodyError(data: unknown, fallback = '请求失败'): string {
  if (!data || typeof data !== 'object') return fallback
  const body = data as Record<string, unknown>
  const nested = body.data as Record<string, unknown> | undefined
  return (
    pickString(
      body.message,
      body.msg,
      body.error_description,
      body.error,
      body.exception,
      nested?.message,
      nested?.msg,
    ) ?? fallback
  )
}

/** 从 axios 异常或已解析的 ResultBody 提取错误文案 */
export function extractApiError(err: unknown, fallback = '请求失败'): string {
  if (err instanceof Error && err.message && !(err as AxiosError).isAxiosError) {
    return err.message
  }
  const ax = err as AxiosError<ResultBody<unknown> | Record<string, unknown>>
  if (ax?.isAxiosError) {
    const data = ax.response?.data
    if (data) return extractResultBodyError(data, fallback)
    if (ax.message) return ax.message
  }
  if (err && typeof err === 'object' && 'message' in err) {
    const msg = (err as ResultBody<unknown>).message
    if (typeof msg === 'string' && msg.trim()) return msg.trim()
  }
  return fallback
}
