/** 与 BaseApp.appType 及后端 normalizeAppDefaults 默认值一致 */
export const APP_TYPE_OPTIONS = [
  { value: 'server', label: '服务应用' },
  { value: 'app', label: '手机应用' },
  { value: 'pc', label: 'PC 网页应用' },
  { value: 'wap', label: '手机网页应用' },
] as const

export type AppTypeValue = (typeof APP_TYPE_OPTIONS)[number]['value']

const APP_TYPE_LABELS = Object.fromEntries(
  APP_TYPE_OPTIONS.map((item) => [item.value, item.label]),
) as Record<string, string>

export function appTypeLabel(appType?: string | null): string {
  if (!appType) return '—'
  return APP_TYPE_LABELS[appType] ?? appType
}
