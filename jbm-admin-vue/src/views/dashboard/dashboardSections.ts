import type { Component } from 'vue'
import { SELF_SERVICE_PATHS } from '@/constants/adminNav'
import {
  Users,
  UserCheck,
  Building2,
  AppWindow,
  Shield,
  KeyRound,
  Server,
  Code2,
  Radar,
  Route,
  Gauge,
  Globe,
  GitBranch,
  ScrollText,
  BookOpen,
  Menu,
  ListTree,
  Activity,
  UserCog,
  FormInput,
  BarChart3,
  Files,
} from '@lucide/vue'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

/** 快捷入口 */
export interface DashboardQuickLink {
  /** 唯一标识 */
  key: string
  /** 显示标题 */
  title: string
  /** 路由路径 */
  to: string
  /** 图标 */
  icon: Component
  /** 简短说明 */
  description?: string
  /** 路由路径权限 — 通过 menuStore.isRouteAllowed() 判断 */
  path: string
  /** 菜单编码权限 — 任一匹配即可 */
  menuCodes?: string[]
  /** 动作编码权限 — 需全部满足 */
  actionCodes?: string[]
}

/** 仪表盘工作区（第二屏分组） */
export interface DashboardSection {
  /** 唯一标识 */
  key: string
  /** 工作区名称 */
  title: string
  /** 图标 */
  icon: Component
  /** 工作区描述 */
  description: string
  /** 快捷入口列表 */
  links: DashboardQuickLink[]
  /** 整区所需的菜单编码（任一匹配即可显示此区） */
  menuCodes: string[]
  /** 整区所需的路由路径（任一匹配即可显示此区） */
  paths?: string[]
}

/** 仪表盘指标卡（第一屏） */
export interface DashboardMetric {
  /** 唯一标识 */
  key: string
  /** 指标名称 */
  title: string
  /** 图标 */
  icon: Component
  /** 数值单位，如 "个"、"人"、"条" */
  unit?: string
  /** 路由路径权限 */
  path: string
  /** 菜单编码权限（任一匹配即可） */
  menuCodes?: string[]
  /** 动作编码权限（需全部满足） */
  actionCodes?: string[]
  /** 一期对应的前端加载函数名称（用于 api/dashboard.ts 引用） */
  loaderKey?: string
  /** 说明文字 */
  description?: string
  /** 超管专属指标 */
  superAdminOnly?: boolean
}

// ---------------------------------------------------------------------------
// 权限检查函数类型
// ---------------------------------------------------------------------------

export interface PermissionContext {
  isSuperAdmin: boolean
  isRouteAllowed: (path: string) => boolean
  allowedMenuCodes: Set<string>
  hasMenu: (code: string) => boolean
  hasAction: (code: string) => boolean
}

// ---------------------------------------------------------------------------
// QuickLink 可见性
// ---------------------------------------------------------------------------

export function isQuickLinkVisible(
  link: DashboardQuickLink,
  ctx: PermissionContext,
): boolean {
  if (ctx.isSuperAdmin) return true
  if (ctx.isRouteAllowed(link.path)) return true
  if (link.menuCodes?.some((c) => ctx.allowedMenuCodes.has(c))) return true
  if (link.menuCodes?.some((c) => ctx.hasMenu(c))) return true
  if (link.actionCodes?.every((c) => ctx.hasAction(c)) && link.actionCodes.length > 0)
    return true
  return false
}

// ---------------------------------------------------------------------------
// Section 过滤
// ---------------------------------------------------------------------------

export function isSectionVisible(
  section: DashboardSection,
  ctx: PermissionContext,
): boolean {
  if (ctx.isSuperAdmin) return true
  // 整区菜单编码任一匹配
  if (section.menuCodes.some((c) => ctx.allowedMenuCodes.has(c))) return true
  if (section.menuCodes.some((c) => ctx.hasMenu(c))) return true
  // 整区路由路径任一匹配
  if (section.paths?.some((p) => ctx.isRouteAllowed(p))) return true
  // 区内快捷入口至少一个可见也显示此区
  if (section.links.some((link) => isQuickLinkVisible(link, ctx))) return true
  return false
}

export function filterSectionsByPermission(
  sections: DashboardSection[],
  ctx: PermissionContext,
): DashboardSection[] {
  return sections
    .filter((s) => isSectionVisible(s, ctx))
    .map((s) => ({
      ...s,
      links: s.links.filter((link) => isQuickLinkVisible(link, ctx)),
    }))
    .filter((s) => s.links.length > 0)
}

// ---------------------------------------------------------------------------
// Metric 过滤
// ---------------------------------------------------------------------------

export function isMetricVisible(
  metric: DashboardMetric,
  ctx: PermissionContext,
): boolean {
  if (metric.superAdminOnly && !ctx.isSuperAdmin) return false
  if (ctx.isSuperAdmin) return true
  if (ctx.isRouteAllowed(metric.path)) return true
  if (metric.menuCodes?.some((c) => ctx.allowedMenuCodes.has(c))) return true
  if (metric.menuCodes?.some((c) => ctx.hasMenu(c))) return true
  if (
    metric.actionCodes &&
    metric.actionCodes.length > 0 &&
    metric.actionCodes.every((c) => ctx.hasAction(c))
  )
    return true
  return false
}

export function filterMetricsByPermission(
  metrics: DashboardMetric[],
  ctx: PermissionContext,
): DashboardMetric[] {
  return metrics.filter((m) => isMetricVisible(m, ctx))
}

/** 非自助路径（需后台菜单授权） */
function isAdminOnlyPath(path: string): boolean {
  return path !== '/dashboard' && !SELF_SERVICE_PATHS.has(path)
}

/**
 * 仅拥有仪表盘与开放平台自助入口，无系统/权限/网关等管理菜单。
 */
export function isSelfServiceOnly(ctx: PermissionContext): boolean {
  if (ctx.isSuperAdmin) return false

  const visibleMetrics = filterMetricsByPermission(DASHBOARD_METRICS, ctx)
  if (visibleMetrics.some((m) => isAdminOnlyPath(m.path))) return false

  const visibleSections = filterSectionsByPermission(DASHBOARD_SECTIONS, ctx)
  if (visibleSections.some((s) => s.key !== 'developer')) return false

  return true
}

// ---------------------------------------------------------------------------
// 全量指标卡定义
// ---------------------------------------------------------------------------

export const DASHBOARD_METRICS: DashboardMetric[] = [
  // —— 系统概览 ——
  {
    key: 'usersTotal',
    title: '用户总数',
    icon: Users,
    unit: '人',
    path: '/system/users',
    menuCodes: ['user', 'users'],
    loaderKey: 'usersTotal',
    description: '系统注册用户总数',
  },
  {
    key: 'onlineUsers',
    title: '在线用户',
    icon: UserCheck,
    unit: '人',
    path: '/system/online-users',
    menuCodes: ['onlineUsers'],
    loaderKey: 'onlineUsers',
    description: '当前在线会话数',
  },
  {
    key: 'appCount',
    title: '应用数',
    icon: AppWindow,
    unit: '个',
    path: '/system/apps',
    menuCodes: ['apps', 'app'],
    loaderKey: 'appCount',
    description: '已注册应用总数',
  },
  {
    key: 'orgCount',
    title: '组织数',
    icon: Building2,
    unit: '个',
    path: '/system/orgs',
    menuCodes: ['orgs', 'org'],
    loaderKey: 'orgCount',
    description: '组织架构总数',
  },
  // —— 权限治理 ——
  {
    key: 'roleCount',
    title: '角色数',
    icon: Shield,
    unit: '个',
    path: '/authority/roles',
    menuCodes: ['role', 'roles'],
    loaderKey: 'roleCount',
    description: '已定义角色总数',
  },
  {
    key: 'authorityResourceCount',
    title: '权限资源数',
    icon: ListTree,
    unit: '条',
    path: '/authority/catalog',
    menuCodes: ['authority', 'authority_catalog'],
    loaderKey: 'authorityResourceCount',
    description: '菜单/按钮/API 权限资源总数',
  },
  // —— API 治理 ——
  {
    key: 'apiCount',
    title: 'API 资源数',
    icon: Server,
    unit: '个',
    path: '/api/registry',
    menuCodes: ['api_registry', 'api_mgmt', 'authority'],
    loaderKey: 'apiCount',
    description: '已注册 API 资源总数',
  },
  {
    key: 'apiKeyCount',
    title: 'API Key 数',
    icon: KeyRound,
    unit: '个',
    path: '/developer/api-keys',
    menuCodes: ['api_keys', 'developer'],
    loaderKey: 'apiKeyCount',
    description: '已颁发的 API Key 总数',
  },
  {
    key: 'apiMonitor',
    title: '今日 API 调用',
    icon: BarChart3,
    unit: '次',
    path: '/api/monitor',
    menuCodes: ['api_monitor'],
    loaderKey: 'apiMonitor',
    description: '今日网关 API 调用总数',
  },
  // —— 网关治理 ——
  {
    key: 'serviceCount',
    title: '注册服务数',
    icon: Radar,
    unit: '个',
    path: '/gateway/services',
    menuCodes: ['gw_services'],
    loaderKey: 'serviceCount',
    description: '注册到网关的服务总数',
  },
  {
    key: 'healthyInstanceCount',
    title: '健康实例数',
    icon: Activity,
    unit: '个',
    path: '/gateway/services',
    menuCodes: ['gw_services'],
    loaderKey: 'healthyInstanceCount',
    description: '健康运行的服务实例数',
  },
  {
    key: 'routeCount',
    title: '路由数',
    icon: Route,
    unit: '条',
    path: '/gateway/routes',
    menuCodes: ['gw_routes'],
    loaderKey: 'routeCount',
    description: '已配置网关路由总数',
  },
  {
    key: 'rateLimitCount',
    title: '限流策略数',
    icon: Gauge,
    unit: '条',
    path: '/gateway/rate-limit',
    menuCodes: ['gw_rate'],
    loaderKey: 'rateLimitCount',
    description: '已配置的限流策略总数',
  },
  {
    key: 'ipLimitCount',
    title: 'IP 策略数',
    icon: Globe,
    unit: '条',
    path: '/gateway/ip-limit',
    menuCodes: ['gw_ip'],
    loaderKey: 'ipLimitCount',
    description: '已配置的 IP 限制策略总数',
  },
  // —— 审计安全 ——
  {
    key: 'auditLogEntry',
    title: '审计日志',
    icon: ScrollText,
    path: '/log/account',
    menuCodes: ['account_logs'],
    description: '查看系统审计日志',
  },
]

// ---------------------------------------------------------------------------
// 全量工作区定义
// ---------------------------------------------------------------------------

export const DASHBOARD_SECTIONS: DashboardSection[] = [
  // —— 系统治理 ——
  {
    key: 'system',
    title: '系统治理',
    icon: BarChart3,
    description: '用户、组织、应用等基础数据管理',
    menuCodes: ['user', 'users', 'onlineUsers', 'orgs', 'org', 'apps', 'app', 'dicts', 'dict', 'extend_fields'],
    paths: ['/system/users', '/system/online-users', '/system/orgs', '/system/apps', '/system/dicts', '/system/extend-fields'],
    links: [
      {
        key: 'system-users',
        title: '用户管理',
        to: '/system/users',
        icon: Users,
        description: '管理系统用户账号',
        path: '/system/users',
        menuCodes: ['user', 'users'],
      },
      {
        key: 'system-online',
        title: '在线用户',
        to: '/system/online-users',
        icon: UserCheck,
        description: '查看当前在线会话',
        path: '/system/online-users',
        menuCodes: ['onlineUsers'],
      },
      {
        key: 'system-orgs',
        title: '组织管理',
        to: '/system/orgs',
        icon: Building2,
        description: '管理组织架构',
        path: '/system/orgs',
        menuCodes: ['orgs', 'org'],
      },
      {
        key: 'system-apps',
        title: '应用管理',
        to: '/system/apps',
        icon: AppWindow,
        description: '管理接入应用',
        path: '/system/apps',
        menuCodes: ['apps', 'app'],
      },
      {
        key: 'system-dicts',
        title: '字典管理',
        to: '/system/dicts',
        icon: BookOpen,
        description: '管理系统数据字典',
        path: '/system/dicts',
        menuCodes: ['dicts', 'dict'],
      },
      {
        key: 'system-extend',
        title: '扩展字段',
        to: '/system/extend-fields',
        icon: FormInput,
        description: '管理自定义扩展字段',
        path: '/system/extend-fields',
        menuCodes: ['extend_fields'],
      },
    ],
  },

  // —— 权限治理 ——
  {
    key: 'authority',
    title: '权限治理',
    icon: Shield,
    description: '角色、权限目录、菜单按钮、用户授权管理',
    menuCodes: ['role', 'roles', 'authority', 'authority_catalog', 'menus', 'menu', 'actions', 'action'],
    paths: ['/authority/roles', '/authority/user-permissions', '/authority/client-permissions', '/authority/catalog', '/system/menus'],
    links: [
      {
        key: 'auth-roles',
        title: '角色管理',
        to: '/authority/roles',
        icon: Shield,
        description: '管理系统角色及其权限',
        path: '/authority/roles',
        menuCodes: ['role', 'roles'],
      },
      {
        key: 'auth-user-perm',
        title: '用户权限',
        to: '/authority/user-permissions',
        icon: UserCog,
        description: '管理用户直接授权',
        path: '/authority/user-permissions',
        menuCodes: ['authority', 'authority_catalog', 'user_perm', 'user_permissions'],
      },
      {
        key: 'auth-client-perm',
        title: '客户端权限',
        to: '/authority/client-permissions',
        icon: KeyRound,
        description: '管理应用客户端权限',
        path: '/authority/client-permissions',
        menuCodes: ['authority', 'authority_catalog', 'client_perm', 'app_perm'],
      },
      {
        key: 'auth-catalog',
        title: '权限目录',
        to: '/authority/catalog',
        icon: ListTree,
        description: '查看权限资源目录',
        path: '/authority/catalog',
        menuCodes: ['authority', 'authority_catalog'],
      },
      {
        key: 'auth-menus',
        title: '菜单与按钮',
        to: '/system/menus',
        icon: Menu,
        description: '管理菜单和按钮定义',
        path: '/system/menus',
        menuCodes: ['menus', 'menu', 'actions', 'action'],
      },
    ],
  },

  // —— API 治理 ——
  {
    key: 'api',
    title: 'API 治理',
    icon: Server,
    description: 'API 资源注册、权限与监控管理',
    menuCodes: ['api_registry', 'api_mgmt', 'api_monitor', 'authority'],
    paths: ['/api/registry', '/api/monitor'],
    links: [
      {
        key: 'api-registry',
        title: 'API 资源管理',
        to: '/api/registry',
        icon: Server,
        description: '管理 API 资源注册',
        path: '/api/registry',
        menuCodes: ['api_registry', 'api_mgmt', 'authority'],
      },
      {
        key: 'api-monitor',
        title: 'API 监控',
        to: '/api/monitor',
        icon: Activity,
        description: '查看 API 调用监控',
        path: '/api/monitor',
        menuCodes: ['api_monitor'],
      },
    ],
  },

  // —— 网关治理 ——
  {
    key: 'gateway',
    title: '网关治理',
    icon: Radar,
    description: '服务发现、路由、限流、IP 限制、灰度发布',
    menuCodes: ['gw_services', 'gw_routes', 'gw_rate', 'gw_ip', 'gw_gray'],
    paths: ['/gateway/services', '/gateway/routes', '/gateway/rate-limit', '/gateway/ip-limit', '/gateway/gray-release'],
    links: [
      {
        key: 'gw-services',
        title: '服务发现',
        to: '/gateway/services',
        icon: Radar,
        description: '查看注册服务与实例状态',
        path: '/gateway/services',
        menuCodes: ['gw_services'],
      },
      {
        key: 'gw-routes',
        title: '路由管理',
        to: '/gateway/routes',
        icon: Route,
        description: '管理网关路由规则',
        path: '/gateway/routes',
        menuCodes: ['gw_routes'],
      },
      {
        key: 'gw-rate',
        title: '限流策略',
        to: '/gateway/rate-limit',
        icon: Gauge,
        description: '管理 API 限流策略',
        path: '/gateway/rate-limit',
        menuCodes: ['gw_rate'],
      },
      {
        key: 'gw-ip',
        title: 'IP 限制',
        to: '/gateway/ip-limit',
        icon: Globe,
        description: '管理 IP 黑白名单策略',
        path: '/gateway/ip-limit',
        menuCodes: ['gw_ip'],
      },
      {
        key: 'gw-gray',
        title: '灰度发布',
        to: '/gateway/gray-release',
        icon: GitBranch,
        description: '管理灰度发布策略',
        path: '/gateway/gray-release',
        menuCodes: ['gw_gray'],
      },
    ],
  },

  // —— 文档管理 ——
  {
    key: 'documents',
    title: '文档管理',
    icon: Files,
    description: '上传、预览、下载和维护业务文档',
    menuCodes: ['documents', 'doc_files', 'doc_mgmt', 'base_doc'],
    paths: ['/documents'],
    links: [
      {
        key: 'doc-files',
        title: '文件管理',
        to: '/documents',
        icon: Files,
        description: '管理文档资源与临时上传分组',
        path: '/documents',
        menuCodes: ['documents', 'doc_files', 'doc_mgmt', 'base_doc'],
      },
    ],
  },

  // —— 开放平台 ——
  {
    key: 'developer',
    title: '开放平台',
    icon: Code2,
    description: '开发者管理、API Key、API 文档',
    menuCodes: ['developer', 'developer_mgmt', 'api_keys'],
    paths: ['/developer', '/developer/api-keys', '/docs'],
    links: [
      {
        key: 'dev-developer',
        title: '开发者',
        to: '/developer',
        icon: Code2,
        description: '管理开发者认证',
        path: '/developer',
        menuCodes: ['developer', 'developer_mgmt'],
      },
      {
        key: 'dev-api-keys',
        title: 'API Keys',
        to: '/developer/api-keys',
        icon: KeyRound,
        description: '管理 API 密钥',
        path: '/developer/api-keys',
        menuCodes: ['api_keys'],
      },
      {
        key: 'dev-wiki',
        title: 'API Wiki',
        to: '/docs',
        icon: BookOpen,
        description: '查看 API 文档',
        path: '/docs',
      },
    ],
  },
]
