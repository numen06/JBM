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
    label: '其他',
    items: [
      { name: 'account-logs', title: '审计日志', icon: ScrollText, to: '/log/account', menuCodes: ['account_logs'] },
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
