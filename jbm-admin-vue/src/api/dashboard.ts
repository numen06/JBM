import { get, unwrap, isOk } from './request'
import { getUserStatistics } from './user'
import { listDiscoveryServices, listRoutes, listRateLimits, listIpLimits } from './gateway'
import type { DiscoveryService } from './types'

/**
 * 仪表盘指标项
 */
export interface DashboardMetric {
  key: string
  label: string
  value: number | string
  unit?: string
  status?: 'success' | 'warning' | 'error' | 'unavailable'
  icon?: string
  path?: string
  description?: string
}

/**
 * 仪表盘工作区/分区
 */
export interface DashboardSection {
  key: string
  title: string
  icon?: string
  metrics: DashboardMetric[]
  description?: string
}

/**
 * 仪表盘通知/降级提示
 */
export interface DashboardNotice {
  key: string
  level: 'info' | 'warning' | 'error'
  message: string
  sectionKey?: string
  path?: string
}

/**
 * 仪表盘概览结果
 */
export interface DashboardOverview {
  metrics: DashboardMetric[]
  sections: DashboardSection[]
  notices: DashboardNotice[]
  loadedAt: string
  /** 二期聚合接口返回的身份摘要（可选） */
  identity?: AggregatedDashboardIdentity
  /** 二期聚合接口返回的工作区可见性（可选） */
  sectionFlags?: AggregatedDashboardSections
  /** 二期聚合接口返回的风险提示（可选） */
  risks?: AggregatedDashboardRisk[]
  /** 数据来源：aggregated / fallback */
  source?: 'aggregated' | 'fallback'
}

/** 二期 GET /current/dashboard 响应结构 */
export interface AggregatedDashboardIdentity {
  userId?: number
  userName?: string
  nickName?: string
  roles?: string[]
  appId?: number
  clientId?: string
  visibleMenuCount?: number
  scope?: 'platform' | 'tenant' | 'app'
  tenantId?: number
}

export interface AggregatedDashboardSections {
  system?: boolean
  authority?: boolean
  api?: boolean
  gateway?: boolean
  developer?: boolean
  audit?: boolean
}

export interface AggregatedDashboardMetrics {
  usersTotal?: number
  onlineUser?: number
  appCount?: number
  orgCount?: number
  roleCount?: number
  authorityResourceCount?: number
  apiCount?: number
  apiKeyCount?: number
}

export interface AggregatedDashboardRisk {
  level: 'info' | 'warning' | 'error'
  title: string
  target?: string
  code?: string
}

export interface AggregatedDashboardResponse {
  identity?: AggregatedDashboardIdentity
  sections?: AggregatedDashboardSections
  metrics?: AggregatedDashboardMetrics
  risks?: AggregatedDashboardRisk[]
}

/**
 * 权限路由映射配置
 */
const ROUTE_PERMISSION_MAP = {
  userStatistics: '/system/users',
  onlineUsers: '/system/online-users',
  discoveryServices: '/gateway/services',
  routes: '/gateway/routes',
  rateLimits: '/gateway/rate-limit',
  ipLimits: '/gateway/ip-limit',
} as const

/** 聚合指标 key 与前端展示 key / 路由映射 */
const AGGREGATED_METRIC_DEFS: Array<{
  key: string
  label: string
  path: string
  icon: string
  aggregatedField: keyof AggregatedDashboardMetrics
}> = [
  { key: 'usersTotal', label: '用户总数', path: '/system/users', icon: 'users', aggregatedField: 'usersTotal' },
  { key: 'onlineUser', label: '在线用户', path: '/system/online-users', icon: 'user-check', aggregatedField: 'onlineUser' },
  { key: 'appCount', label: '应用数', path: '/system/apps', icon: 'app', aggregatedField: 'appCount' },
  { key: 'orgCount', label: '组织数', path: '/system/orgs', icon: 'org', aggregatedField: 'orgCount' },
  { key: 'roleCount', label: '角色数', path: '/authority/roles', icon: 'shield', aggregatedField: 'roleCount' },
  {
    key: 'authorityResourceCount',
    label: '权限资源数',
    path: '/authority/catalog',
    icon: 'list-tree',
    aggregatedField: 'authorityResourceCount',
  },
  { key: 'apiCount', label: 'API 资源数', path: '/api/registry', icon: 'server', aggregatedField: 'apiCount' },
  { key: 'apiKeyCount', label: 'API Key 数', path: '/developer/api-keys', icon: 'key', aggregatedField: 'apiKeyCount' },
]

export type RoutePermissionChecker = (path: string) => boolean

/**
 * 调用二期聚合接口 GET /current/dashboard
 */
export async function fetchCurrentDashboard(): Promise<AggregatedDashboardResponse | null> {
  try {
    const body = await get<AggregatedDashboardResponse>('/current/dashboard')
    if (!isOk(body)) return null
    return unwrap(body) ?? null
  } catch {
    return null
  }
}

function metricsFromAggregated(
  aggregated: AggregatedDashboardMetrics | undefined,
  isRouteAllowed: RoutePermissionChecker,
): DashboardMetric[] {
  if (!aggregated) return []
  const metrics: DashboardMetric[] = []
  for (const def of AGGREGATED_METRIC_DEFS) {
    const value = aggregated[def.aggregatedField]
    if (value === undefined || value === null) continue
    if (!isRouteAllowed(def.path)) continue
    metrics.push({
      key: def.key,
      label: def.label,
      value,
      icon: def.icon,
      path: def.path,
    })
  }
  return metrics
}

function risksToNotices(risks: AggregatedDashboardRisk[] | undefined): DashboardNotice[] {
  if (!risks?.length) return []
  return risks.map((risk, index) => ({
    key: risk.code ?? `risk-${index}`,
    level: risk.level,
    message: risk.title,
    path: risk.target,
  }))
}

/**
 * 加载仪表盘概览数据
 *
 * 优先调用 GET /current/dashboard；失败或未授权时降级到一期并行加载。
 * 网关运行态指标仍由前端补充（Center 不聚合 Nacos/路由数据）。
 */
export async function loadDashboardOverview(
  isRouteAllowed: RoutePermissionChecker,
): Promise<DashboardOverview> {
  const now = new Date().toISOString()
  const aggregated = await fetchCurrentDashboard()

  if (aggregated) {
    const metrics = metricsFromAggregated(aggregated.metrics, isRouteAllowed)
    const gatewayPart = await loadGatewayMetrics(isRouteAllowed)
    const mergedMetrics = mergeMetricsByKey(metrics, gatewayPart.metrics)
    const notices = [...risksToNotices(aggregated.risks), ...gatewayPart.notices]

    return {
      metrics: mergedMetrics,
      sections: buildSectionsFromMetrics(mergedMetrics),
      notices,
      loadedAt: now,
      identity: aggregated.identity,
      sectionFlags: aggregated.sections,
      risks: aggregated.risks,
      source: 'aggregated',
    }
  }

  const fallback = await loadDashboardOverviewFallback(isRouteAllowed)
  return { ...fallback, source: 'fallback' }
}

function mergeMetricsByKey(primary: DashboardMetric[], extra: DashboardMetric[]): DashboardMetric[] {
  const map = new Map<string, DashboardMetric>()
  for (const m of primary) map.set(m.key, m)
  for (const m of extra) {
    if (!map.has(m.key)) map.set(m.key, m)
  }
  return Array.from(map.values())
}

function buildSectionsFromMetrics(metrics: DashboardMetric[]): DashboardSection[] {
  const systemKeys = new Set(['usersTotal', 'onlineUser', 'appCount', 'orgCount'])
  const authorityKeys = new Set(['roleCount', 'authorityResourceCount'])
  const apiKeys = new Set(['apiCount', 'apiKeyCount'])
  const gatewayKeys = new Set([
    'serviceCount',
    'healthyInstanceCount',
    'routeCount',
    'rateLimitCount',
    'ipLimitCount',
  ])

  const system = metrics.filter((m) => systemKeys.has(m.key))
  const authority = metrics.filter((m) => authorityKeys.has(m.key))
  const api = metrics.filter((m) => apiKeys.has(m.key))
  const gateway = metrics.filter((m) => gatewayKeys.has(m.key))

  const sections: DashboardSection[] = []
  if (system.length) sections.push({ key: 'system', title: '系统概览', icon: 'settings', metrics: system })
  if (authority.length) sections.push({ key: 'authority', title: '权限治理', icon: 'shield', metrics: authority })
  if (api.length) sections.push({ key: 'api', title: 'API 治理', icon: 'server', metrics: api })
  if (gateway.length) {
    sections.push({
      key: 'gateway',
      title: '网关治理',
      icon: 'network',
      metrics: gateway,
      description: '网关运行状态与策略统计',
    })
  }
  return sections
}

/** 仅加载网关相关指标（Center 聚合接口不包含） */
async function loadGatewayMetrics(isRouteAllowed: RoutePermissionChecker): Promise<{
  metrics: DashboardMetric[]
  notices: DashboardNotice[]
}> {
  const notices: DashboardNotice[] = []
  const metrics: DashboardMetric[] = []

  const gatewayAllowed =
    isRouteAllowed(ROUTE_PERMISSION_MAP.discoveryServices) ||
    isRouteAllowed(ROUTE_PERMISSION_MAP.routes) ||
    isRouteAllowed(ROUTE_PERMISSION_MAP.rateLimits) ||
    isRouteAllowed(ROUTE_PERMISSION_MAP.ipLimits)

  if (!gatewayAllowed) {
    return { metrics, notices }
  }

  if (isRouteAllowed(ROUTE_PERMISSION_MAP.discoveryServices)) {
    try {
      const services = await loadDiscoveryServicesWithMetrics()
      metrics.push(services.serviceMetric, services.instanceMetric)
      if (services.notice) notices.push(services.notice)
    } catch {
      metrics.push(
        {
          key: 'serviceCount',
          label: '注册服务',
          value: '不可用',
          status: 'unavailable',
          icon: 'server',
          path: '/gateway/services',
        },
        {
          key: 'healthyInstanceCount',
          label: '健康实例',
          value: '不可用',
          status: 'unavailable',
          icon: 'activity',
          path: '/gateway/services',
        },
      )
      notices.push({
        key: 'discoveryServicesError',
        level: 'warning',
        message: '服务发现不可用，请检查 Nacos 或网关服务状态',
        sectionKey: 'gateway',
        path: '/gateway/services',
      })
    }
  }

  if (isRouteAllowed(ROUTE_PERMISSION_MAP.routes)) {
    try {
      const routePaging = await listRoutes(1, 1)
      metrics.push({
        key: 'routeCount',
        label: '路由总数',
        value: routePaging?.total ?? 0,
        icon: 'route',
        path: '/gateway/routes',
      })
    } catch {
      notices.push({
        key: 'routesError',
        level: 'warning',
        message: '路由数据加载失败',
        sectionKey: 'gateway',
        path: '/gateway/routes',
      })
      metrics.push({
        key: 'routeCount',
        label: '路由总数',
        value: '—',
        status: 'error',
        icon: 'route',
        path: '/gateway/routes',
      })
    }
  }

  if (isRouteAllowed(ROUTE_PERMISSION_MAP.rateLimits)) {
    try {
      const rateLimitPaging = await listRateLimits(1, 1)
      metrics.push({
        key: 'rateLimitCount',
        label: '限流策略',
        value: rateLimitPaging?.total ?? 0,
        icon: 'shield',
        path: '/gateway/rate-limit',
      })
    } catch {
      notices.push({
        key: 'rateLimitsError',
        level: 'warning',
        message: '限流策略数据加载失败',
        sectionKey: 'gateway',
        path: '/gateway/rate-limit',
      })
      metrics.push({
        key: 'rateLimitCount',
        label: '限流策略',
        value: '—',
        status: 'error',
        icon: 'shield',
        path: '/gateway/rate-limit',
      })
    }
  }

  if (isRouteAllowed(ROUTE_PERMISSION_MAP.ipLimits)) {
    try {
      const ipLimitPaging = await listIpLimits(1, 1)
      metrics.push({
        key: 'ipLimitCount',
        label: 'IP 策略',
        value: ipLimitPaging?.total ?? 0,
        icon: 'lock',
        path: '/gateway/ip-limit',
      })
    } catch {
      notices.push({
        key: 'ipLimitsError',
        level: 'warning',
        message: 'IP 策略数据加载失败',
        sectionKey: 'gateway',
        path: '/gateway/ip-limit',
      })
      metrics.push({
        key: 'ipLimitCount',
        label: 'IP 策略',
        value: '—',
        status: 'error',
        icon: 'lock',
        path: '/gateway/ip-limit',
      })
    }
  }

  return { metrics, notices }
}

/** 一期降级：按权限并行加载现有接口 */
async function loadDashboardOverviewFallback(
  isRouteAllowed: RoutePermissionChecker,
): Promise<DashboardOverview> {
  const notices: DashboardNotice[] = []
  const metrics: DashboardMetric[] = []
  const sections: DashboardSection[] = []
  const now = new Date().toISOString()

  const userMetrics: DashboardMetric[] = []
  if (isRouteAllowed(ROUTE_PERMISSION_MAP.userStatistics)) {
    try {
      const stats = await getUserStatistics()
      if (stats) {
        if (stats.usersTotal !== undefined) {
          userMetrics.push({
            key: 'usersTotal',
            label: '用户总数',
            value: stats.usersTotal,
            icon: 'users',
            path: '/system/users',
          })
        }
        if (stats.onlineUser !== undefined) {
          userMetrics.push({
            key: 'onlineUser',
            label: '在线用户',
            value: stats.onlineUser,
            icon: 'user-check',
            path: '/system/online-users',
          })
        }
      }
    } catch {
      notices.push({
        key: 'userStatisticsError',
        level: 'warning',
        message: '用户统计数据加载失败',
        sectionKey: 'system',
        path: '/system/users',
      })
      userMetrics.push(
        {
          key: 'usersTotal',
          label: '用户总数',
          value: '—',
          status: 'error',
          icon: 'users',
          path: '/system/users',
        },
        {
          key: 'onlineUser',
          label: '在线用户',
          value: '—',
          status: 'error',
          icon: 'user-check',
          path: '/system/online-users',
        },
      )
    }
  }

  if (userMetrics.length > 0) {
    sections.push({ key: 'system', title: '系统概览', icon: 'settings', metrics: userMetrics })
  }

  const gatewayPart = await loadGatewayMetrics(isRouteAllowed)
  notices.push(...gatewayPart.notices)
  if (gatewayPart.metrics.length > 0) {
    sections.push({
      key: 'gateway',
      title: '网关治理',
      icon: 'network',
      metrics: gatewayPart.metrics,
      description: '网关运行状态与策略统计',
    })
  }

  for (const section of sections) {
    for (const metric of section.metrics) {
      metrics.push(metric)
    }
  }

  return { metrics, sections, notices, loadedAt: now }
}

async function loadDiscoveryServicesWithMetrics(): Promise<{
  serviceMetric: DashboardMetric
  instanceMetric: DashboardMetric
  notice?: DashboardNotice
}> {
  const services = await listDiscoveryServices()
  const serviceCount = services.length
  const healthyInstanceCount = calculateHealthyInstances(services)

  const serviceMetric: DashboardMetric = {
    key: 'serviceCount',
    label: '注册服务',
    value: serviceCount,
    icon: 'server',
    path: '/gateway/services',
  }

  const instanceMetric: DashboardMetric = {
    key: 'healthyInstanceCount',
    label: '健康实例',
    value: healthyInstanceCount,
    icon: 'activity',
    path: '/gateway/services',
  }

  let notice: DashboardNotice | undefined
  if (serviceCount > 0 && healthyInstanceCount === 0) {
    notice = {
      key: 'noHealthyInstances',
      level: 'warning',
      message: '存在注册服务但无健康实例',
      sectionKey: 'gateway',
      path: '/gateway/services',
    }
    instanceMetric.status = 'warning'
  }

  return { serviceMetric, instanceMetric, notice }
}

function calculateHealthyInstances(services: DiscoveryService[]): number {
  let total = 0
  for (const service of services) {
    if (service.healthyCount !== undefined) {
      total += service.healthyCount
      continue
    }
    if (service.instances && service.instances.length > 0) {
      total += service.instances.filter((inst) => inst.healthy === true).length
    }
  }
  return total
}

/**
 * API 监控指标占位
 */
export const API_MONITOR_PLACEHOLDER: DashboardMetric = {
  key: 'apiMonitor',
  label: 'API 监控',
  value: '待接入',
  status: 'unavailable',
  icon: 'chart-line',
  description: 'API 监控指标正在规划中',
}
