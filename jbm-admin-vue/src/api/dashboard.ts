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
}

/**
 * 权限路由映射配置
 * 用于判断是否调用对应接口
 */
const ROUTE_PERMISSION_MAP = {
  userStatistics: '/system/users',
  onlineUsers: '/system/online-users',
  discoveryServices: '/gateway/services',
  routes: '/gateway/routes',
  rateLimits: '/gateway/rate-limit',
  ipLimits: '/gateway/ip-limit',
} as const

/**
 * 检查路由权限的回调类型
 */
export type RoutePermissionChecker = (path: string) => boolean

/**
 * 加载仪表盘概览数据
 *
 * 按权限并行加载现有接口，每个分区独立 try/catch，
 * 某个接口失败时只降级对应卡片，不让整个仪表盘失败。
 *
 * @param isRouteAllowed 权限检查函数，来自 menuStore.isRouteAllowed
 */
export async function loadDashboardOverview(
  isRouteAllowed: RoutePermissionChecker,
): Promise<DashboardOverview> {
  const notices: DashboardNotice[] = []
  const metrics: DashboardMetric[] = []
  const sections: DashboardSection[] = []

  const now = new Date().toISOString()

  // 用户统计区域
  const userMetrics: DashboardMetric[] = []
  const userSectionAllowed = isRouteAllowed(ROUTE_PERMISSION_MAP.userStatistics)

  if (userSectionAllowed) {
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
    } catch (e) {
      notices.push({
        key: 'userStatisticsError',
        level: 'warning',
        message: '用户统计数据加载失败',
        sectionKey: 'system',
        path: '/system/users',
      })
      userMetrics.push({
        key: 'usersTotal',
        label: '用户总数',
        value: '—',
        status: 'error',
        icon: 'users',
        path: '/system/users',
      })
      userMetrics.push({
        key: 'onlineUser',
        label: '在线用户',
        value: '—',
        status: 'error',
        icon: 'user-check',
        path: '/system/online-users',
      })
    }
  }

  if (userMetrics.length > 0) {
    sections.push({
      key: 'system',
      title: '系统概览',
      icon: 'settings',
      metrics: userMetrics,
    })
  }

  // 网关治理区域
  const gatewayMetrics: DashboardMetric[] = []
  const gatewaySectionAllowed =
    isRouteAllowed(ROUTE_PERMISSION_MAP.discoveryServices) ||
    isRouteAllowed(ROUTE_PERMISSION_MAP.routes) ||
    isRouteAllowed(ROUTE_PERMISSION_MAP.rateLimits) ||
    isRouteAllowed(ROUTE_PERMISSION_MAP.ipLimits)

  if (gatewaySectionAllowed) {
    // 服务发现 - 并行调用
    if (isRouteAllowed(ROUTE_PERMISSION_MAP.discoveryServices)) {
      try {
        const services = await loadDiscoveryServicesWithMetrics()
        gatewayMetrics.push(services.serviceMetric)
        gatewayMetrics.push(services.instanceMetric)
        if (services.notice) {
          notices.push(services.notice)
        }
      } catch (e) {
        // Nacos 等服务发现失败时显示"不可用"而不是 0
        gatewayMetrics.push({
          key: 'serviceCount',
          label: '注册服务',
          value: '不可用',
          status: 'unavailable',
          icon: 'server',
          path: '/gateway/services',
        })
        gatewayMetrics.push({
          key: 'healthyInstanceCount',
          label: '健康实例',
          value: '不可用',
          status: 'unavailable',
          icon: 'activity',
          path: '/gateway/services',
        })
        notices.push({
          key: 'discoveryServicesError',
          level: 'warning',
          message: '服务发现不可用，请检查 Nacos 或网关服务状态',
          sectionKey: 'gateway',
          path: '/gateway/services',
        })
      }
    }

    // 路由总数
    if (isRouteAllowed(ROUTE_PERMISSION_MAP.routes)) {
      try {
        const routePaging = await listRoutes(1, 1)
        gatewayMetrics.push({
          key: 'routeCount',
          label: '路由总数',
          value: routePaging?.total ?? 0,
          icon: 'route',
          path: '/gateway/routes',
        })
      } catch (e) {
        notices.push({
          key: 'routesError',
          level: 'warning',
          message: '路由数据加载失败',
          sectionKey: 'gateway',
          path: '/gateway/routes',
        })
        gatewayMetrics.push({
          key: 'routeCount',
          label: '路由总数',
          value: '—',
          status: 'error',
          icon: 'route',
          path: '/gateway/routes',
        })
      }
    }

    // 限流策略总数
    if (isRouteAllowed(ROUTE_PERMISSION_MAP.rateLimits)) {
      try {
        const rateLimitPaging = await listRateLimits(1, 1)
        gatewayMetrics.push({
          key: 'rateLimitCount',
          label: '限流策略',
          value: rateLimitPaging?.total ?? 0,
          icon: 'shield',
          path: '/gateway/rate-limit',
        })
      } catch (e) {
        notices.push({
          key: 'rateLimitsError',
          level: 'warning',
          message: '限流策略数据加载失败',
          sectionKey: 'gateway',
          path: '/gateway/rate-limit',
        })
        gatewayMetrics.push({
          key: 'rateLimitCount',
          label: '限流策略',
          value: '—',
          status: 'error',
          icon: 'shield',
          path: '/gateway/rate-limit',
        })
      }
    }

    // IP 策略总数
    if (isRouteAllowed(ROUTE_PERMISSION_MAP.ipLimits)) {
      try {
        const ipLimitPaging = await listIpLimits(1, 1)
        gatewayMetrics.push({
          key: 'ipLimitCount',
          label: 'IP 策略',
          value: ipLimitPaging?.total ?? 0,
          icon: 'lock',
          path: '/gateway/ip-limit',
        })
      } catch (e) {
        notices.push({
          key: 'ipLimitsError',
          level: 'warning',
          message: 'IP 策略数据加载失败',
          sectionKey: 'gateway',
          path: '/gateway/ip-limit',
        })
        gatewayMetrics.push({
          key: 'ipLimitCount',
          label: 'IP 策略',
          value: '—',
          status: 'error',
          icon: 'lock',
          path: '/gateway/ip-limit',
        })
      }
    }
  }

  if (gatewayMetrics.length > 0) {
    sections.push({
      key: 'gateway',
      title: '网关治理',
      icon: 'network',
      metrics: gatewayMetrics,
      description: '网关运行状态与策略统计',
    })
  }

  // 合并所有指标到顶层 metrics 数组（便于扁平展示）
  for (const section of sections) {
    for (const metric of section.metrics) {
      metrics.push(metric)
    }
  }

  return {
    metrics,
    sections,
    notices,
    loadedAt: now,
  }
}

/**
 * 加载服务发现数据并计算指标
 * 服务发现可能依赖 Nacos，失败时需要特殊处理
 */
async function loadDiscoveryServicesWithMetrics(): Promise<{
  serviceMetric: DashboardMetric
  instanceMetric: DashboardMetric
  notice?: DashboardNotice
}> {
  const services = await listDiscoveryServices()

  // 计算服务数和健康实例数
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

  // 如果有服务但没有健康实例，添加警告
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

/**
 * 计算所有服务的健康实例总数
 */
function calculateHealthyInstances(services: DiscoveryService[]): number {
  let total = 0
  for (const service of services) {
    // 如果服务本身有 healthyCount 字段，直接使用
    if (service.healthyCount !== undefined) {
      total += service.healthyCount
      continue
    }

    // 否则从 instances 数组计算
    if (service.instances && service.instances.length > 0) {
      const healthy = service.instances.filter(
        (inst) => inst.healthy === true,
      ).length
      total += healthy
    }
  }
  return total
}

/**
 * API 监控指标占位
 * 当前是规划占位，不做假数据趋势
 */
export const API_MONITOR_PLACEHOLDER: DashboardMetric = {
  key: 'apiMonitor',
  label: 'API 监控',
  value: '待接入',
  status: 'unavailable',
  icon: 'chart-line',
  description: 'API 监控指标正在规划中',
}