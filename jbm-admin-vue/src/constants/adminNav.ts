import type { Component } from 'vue'
import {
  LayoutDashboard,
  Users,
  Shield,
  Menu,
  Building2,
  KeyRound,
  AppWindow,
  BookOpen,
  FormInput,
  Route,
  Gauge,
  Globe,
  ScrollText,
  Code2,
  Radar,
  UserCheck,
  UserCog,
  ListTree,
  Server,
  Activity,
  GitBranch,
  FileCode2,
  Bell,
  Send,
  ListChecks,
  ClipboardList,
  LogIn,
  Files,
  Webhook,
} from '@lucide/vue'

/** 后端菜单 path 与前端路由对齐 */
export const MENU_PATH_ALIASES: Record<string, string> = {
  '/system/user': '/system/users',
  '/system/role': '/authority/roles',
  '/system/roles': '/authority/roles',
  '/system/authority': '/authority/catalog',
  '/system/authorities': '/authority/catalog',
  '/system/developer': '/developer',
  '/system/online-users/index': '/system/online-users',
  '/system/actions': '/system/menus',
  '/authority/catalog/apis': '/authority/catalog',
  '/authority/catalog/pages': '/authority/catalog',
  '/developer/pending': '/developer',
  '/monitor/job': '/jobs',
  '/monitor/job/log': '/jobs/logs',
  '/monitor/log': '/log/gateway',
  '/monitor/logs': '/log/gateway',
  '/job': '/jobs',
  '/job/logs': '/jobs/logs',
  '/logs': '/log/gateway',
  '/logs/gateway': '/log/gateway',
  '/logs/access': '/log/gateway',
  '/logs/filter-rules': '/log/filter-rules',
  '/logs/account': '/log/account',
  '/logs/login': '/log/login',
  '/doc': '/documents',
  '/doc/files': '/documents',
  '/document': '/documents',
  '/documents/files': '/documents',
  '/GatewayLogs': '/log/gateway',
  '/baseAccountLogs': '/log/account',
  '/accountLogs': '/log/account',
  '/messages/push-test': '/messages/send-test',
  '/messages/webhook-config': '/messages/webhook-configs',
  '/messages/webhook-task': '/messages/webhook-tasks',
}

export function normalizeMenuPath(path?: string): string {
  if (!path || path === '/') return ''
  const p = path.startsWith('/') ? path : `/${path}`
  return MENU_PATH_ALIASES[p] ?? p
}

export interface NavItemDef {
  name: string
  title: string
  icon: Component
  to: string
  menuCodes?: string[]
}

export interface NavGroupDef {
  label: string
  items: NavItemDef[]
}

/** 全量静态导航（种子数据补全前兜底） */
export const STATIC_NAV_GROUPS: NavGroupDef[] = [
  {
    label: '概览',
    items: [{ name: 'dashboard', title: '仪表盘', icon: LayoutDashboard, to: '/dashboard' }],
  },
  {
    label: '系统管理',
    items: [
      { name: 'users', title: '用户管理', icon: Users, to: '/system/users', menuCodes: ['user', 'users'] },
      {
        name: 'online-users',
        title: '在线用户',
        icon: UserCheck,
        to: '/system/online-users',
        menuCodes: ['onlineUsers'],
      },
      { name: 'orgs', title: '组织管理', icon: Building2, to: '/system/orgs', menuCodes: ['orgs', 'org'] },
      { name: 'apps', title: '应用管理', icon: AppWindow, to: '/system/apps', menuCodes: ['apps', 'app'] },
      { name: 'dicts', title: '字典管理', icon: BookOpen, to: '/system/dicts', menuCodes: ['dicts', 'dict'] },
      {
        name: 'extend-fields',
        title: '扩展字段管理',
        icon: FormInput,
        to: '/system/extend-fields',
        menuCodes: ['extend_fields'],
      },
    ],
  },
  {
    label: '权限管理',
    items: [
      { name: 'roles', title: '角色管理', icon: Shield, to: '/authority/roles', menuCodes: ['role', 'roles'] },
      {
        name: 'user-permissions',
        title: '用户权限',
        icon: UserCog,
        to: '/authority/user-permissions',
        menuCodes: ['authority', 'authority_catalog', 'user_perm', 'user_permissions'],
      },
      {
        name: 'client-permissions',
        title: '客户端权限',
        icon: KeyRound,
        to: '/authority/client-permissions',
        menuCodes: ['authority', 'authority_catalog', 'client_perm', 'app_perm'],
      },
      {
        name: 'authority-catalog',
        title: '权限目录',
        icon: ListTree,
        to: '/authority/catalog',
        menuCodes: ['authority', 'authority_catalog'],
      },
      { name: 'menus', title: '菜单与按钮', icon: Menu, to: '/system/menus', menuCodes: ['menus', 'menu', 'actions', 'action'] },
    ],
  },
  {
    label: 'API 管理',
    items: [
      {
        name: 'api-registry',
        title: 'API 资源管理',
        icon: Server,
        to: '/api/registry',
        menuCodes: ['authority', 'authority_catalog', 'api_registry', 'api_mgmt'],
      },
      {
        name: 'api-docs',
        title: 'API 文档与调试',
        icon: FileCode2,
        to: '/api/docs',
        menuCodes: ['api_docs'],
      },
      {
        name: 'api-monitor',
        title: 'API 监控',
        icon: Activity,
        to: '/api/monitor',
        menuCodes: ['api_monitor'],
      },
    ],
  },
  {
    label: '网关管理',
    items: [
      { name: 'gateway-services', title: '服务发现', icon: Radar, to: '/gateway/services', menuCodes: ['gw_services'] },
      { name: 'gateway-routes', title: '路由管理', icon: Route, to: '/gateway/routes', menuCodes: ['gw_routes'] },
      { name: 'gateway-gray', title: '灰度发布', icon: GitBranch, to: '/gateway/gray-release', menuCodes: ['gw_gray'] },
      { name: 'gateway-rate', title: '限流策略', icon: Gauge, to: '/gateway/rate-limit', menuCodes: ['gw_rate'] },
      { name: 'gateway-ip', title: 'IP 限制', icon: Globe, to: '/gateway/ip-limit', menuCodes: ['gw_ip'] },
    ],
  },
  {
    label: '消息管理',
    items: [
      {
        name: 'messages',
        title: '消息记录',
        icon: Bell,
        to: '/messages',
        menuCodes: ['messages', 'message_records', 'message_center', 'push'],
      },
      {
        name: 'message-send-test',
        title: '发送测试',
        icon: Send,
        to: '/messages/send-test',
        menuCodes: ['message_send_test', 'message_push_test', 'push_test', 'pushTasks'],
      },
      {
        name: 'message-channels',
        title: '渠道设置',
        icon: KeyRound,
        to: '/messages/channels',
        menuCodes: ['message_channels', 'push_config', 'push'],
      },
      {
        name: 'webhook-event-configs',
        title: '事件订阅配置',
        icon: Webhook,
        to: '/messages/webhook-configs',
        menuCodes: ['webhook_event_config', 'push_webhook', 'push', 'messages'],
      },
      {
        name: 'webhook-tasks',
        title: '投递任务',
        icon: ClipboardList,
        to: '/messages/webhook-tasks',
        menuCodes: ['webhook_tasks', 'push_webhook', 'push', 'messages'],
      },
    ],
  },
  {
    label: '任务调度',
    items: [
      {
        name: 'jobs',
        title: '任务管理',
        icon: ListChecks,
        to: '/jobs',
        menuCodes: ['jobs', 'job', 'task_jobs', 'monitor_job'],
      },
      {
        name: 'job-logs',
        title: '调度日志',
        icon: ClipboardList,
        to: '/jobs/logs',
        menuCodes: ['job_logs', 'task_job_logs', 'monitor_job_log'],
      },
    ],
  },
  {
    label: '文档管理',
    items: [
      {
        name: 'documents',
        title: '文件管理',
        icon: Files,
        to: '/documents',
        menuCodes: ['documents', 'doc_files', 'doc_mgmt', 'base_doc'],
      },
    ],
  },
  {
    label: '日志管理',
    items: [
      {
        name: 'log-management',
        title: '访问日志',
        icon: ScrollText,
        to: '/log/gateway',
        menuCodes: ['account_logs', 'logs', 'log', 'gateway', 'gateway_logs', 'access_logs', 'cluster_access'],
      },
      {
        name: 'login-logs',
        title: '登录日志',
        icon: LogIn,
        to: '/log/login',
        menuCodes: ['account_logs', 'login_logs', 'login_log', 'auth_logs', 'audit_logs'],
      },
      {
        name: 'log-filter-rules',
        title: '采集设置',
        icon: ListChecks,
        to: '/log/filter-rules',
        menuCodes: ['account_logs', 'gateway_logs', 'access_logs', 'log_filter_rules', 'cluster_access'],
      },
      {
        name: 'account-logs',
        title: '审计日志',
        icon: ScrollText,
        to: '/log/account',
        menuCodes: ['account_logs', 'business_logs', 'audit_logs'],
      },
    ],
  },
  {
    label: '其他',
    items: [
      { name: 'developer', title: '开发者', icon: Code2, to: '/developer', menuCodes: ['developer', 'developer_mgmt'] },
    ],
  },
]

/** 普通注册用户的自助接入入口；后台管理菜单必须来自后端授权。 */
export const SELF_SERVICE_NAV_GROUPS: NavGroupDef[] = [
  {
    label: '概览',
    items: [{ name: 'dashboard', title: '仪表盘', icon: LayoutDashboard, to: '/dashboard' }],
  },
  {
    label: '开放平台',
    items: [
      { name: 'developer', title: '开发者', icon: Code2, to: '/developer', menuCodes: ['developer', 'developer_mgmt'] },
      { name: 'api-keys', title: 'API Keys', icon: KeyRound, to: '/developer/api-keys', menuCodes: ['api_keys'] },
      { name: 'docs', title: 'API Wiki', icon: BookOpen, to: '/docs' },
    ],
  },
]

export const SELF_SERVICE_PATHS = new Set(
  SELF_SERVICE_NAV_GROUPS.flatMap((group) => group.items.map((item) => item.to)),
)

export function buildNavGroups(
  allowedPaths: Set<string>,
  allowedMenuCodes: Set<string>,
): NavGroupDef[] {
  if (allowedPaths.size === 0 && allowedMenuCodes.size === 0) {
    return SELF_SERVICE_NAV_GROUPS
  }

  const groups: NavGroupDef[] = []
  for (const g of STATIC_NAV_GROUPS) {
    const items = g.items.filter((item) => {
      if (item.to === '/dashboard') return allowedPaths.has('/dashboard') || allowedPaths.size === 0
      if (allowedPaths.has(item.to)) return true
      if (item.menuCodes?.some((c) => allowedMenuCodes.has(c))) return true
      return false
    })
    if (items.length) groups.push({ label: g.label, items })
  }
  return groups.length ? groups : STATIC_NAV_GROUPS
}
