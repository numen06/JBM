/** Framework-independent chart contract, shared by ECharts and SVG dashboards. */
export const chartPalette = ['#68b894', '#8ba3b8', '#b2a488', '#958ead', '#719fa3', '#b28e95']
export const chartColors = { text: '#ebebeb', muted: '#a6a6a6', grid: '#2e2e2e', surface: '#1f1f1f', border: '#404040' }
export const designFont = 'Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif'

export function formatChartNumber(value: unknown): string {
  if (value == null || typeof value === 'boolean' || (typeof value !== 'number' && typeof value !== 'string') || String(value).trim() === '') return '—'
  const number = Number(value)
  if (!Number.isFinite(number)) return '—'
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 6, notation: 'standard' }).format(number)
}

/** Stable identity colouring: filtering/reordering series must not change colours. */
export function chartSeriesColor(key: string) {
  let hash = 0
  for (const char of key) hash = (Math.imul(hash, 31) + char.codePointAt(0)!) >>> 0
  return chartPalette[hash % chartPalette.length]!
}

export function chartPresentation(reducedMotion = false) {
  return {
    color: [...chartPalette],
    backgroundColor: 'transparent',
    textStyle: { fontFamily: designFont, color: chartColors.text, fontSize: 12 },
    animation: !reducedMotion,
    animationDuration: 220,
    animationDurationUpdate: 220,
    tooltip: { confine: true, backgroundColor: chartColors.surface, borderColor: chartColors.border,
      padding: [10, 12], textStyle: { fontFamily: designFont, color: chartColors.text, fontSize: 12 } },
  }
}
