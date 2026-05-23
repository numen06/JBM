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
  MousePointerClick,
} from '@lucide/vue'

/** 后端菜单 path 与前端路由对齐 */
export const MENU_PATH_ALIASES: Record<string, string> = {
  '/system/user': '/system/users',
  '/system/role': '/system/roles',
  '/system/authority': '/system/authorities',
  '/system/developer': '/developer',
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
      { name: 'roles', title: '角色管理', icon: Shield, to: '/system/roles', menuCodes: ['role', 'roles'] },
      { name: 'menus', title: '菜单管理', icon: Menu, to: '/system/menus', menuCodes: ['menus', 'menu'] },
      {
        name: 'actions',
        title: '按钮管理',
        icon: MousePointerClick,
        to: '/system/actions',
        menuCodes: ['actions', 'action'],
      },
      { name: 'orgs', title: '组织管理', icon: Building2, to: '/system/orgs', menuCodes: ['orgs', 'org'] },
      {
        name: 'authorities',
        title: '权限管理',
        icon: KeyRound,
        to: '/system/authorities',
        menuCodes: ['authority'],
      },
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
    label: '网关管理',
    items: [
      { name: 'gateway-routes', title: '路由管理', icon: Route, to: '/gateway/routes', menuCodes: ['gw_routes'] },
      { name: 'gateway-rate', title: '限流管理', icon: Gauge, to: '/gateway/rate-limit', menuCodes: ['gw_rate'] },
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

export function buildNavGroups(
  allowedPaths: Set<string>,
  allowedMenuCodes: Set<string>,
): NavGroupDef[] {
  if (allowedPaths.size === 0 && allowedMenuCodes.size === 0) {
    return STATIC_NAV_GROUPS
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
