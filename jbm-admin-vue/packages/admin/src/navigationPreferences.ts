export type WorkspaceKey = 'building' | 'iot' | 'jbm'
export type NavigationOverride = { label?: string; hidden?: boolean; order?: number }
export type TenantNavigationPreferences = Record<'workspaces' | 'menus' | 'groups', Record<string, NavigationOverride>>
export interface TenantNavigationContext { tenantId: string; canManage: boolean; revision: number; preferences: TenantNavigationPreferences }
export interface NavigationCatalogItem { workspace: WorkspaceKey; group: string; path: string; label: string }
export interface NavigationGroup { label: string; items: Array<{ path: string; label: string }> }
export const workspaceNames = { building: '建筑', iot: '物连', jbm: '系统' } as const
export const emptyNavigation = (): TenantNavigationPreferences => ({ workspaces: {}, menus: {}, groups: {} })
export const isSystemBusinessMenu = (path: string) => !['/jbm/dashboard', '/jbm/profile', '/jbm/message-center'].includes(path)

// Only transform already authorized entries. Preferences are never permissions.
export function applyNavigation<T extends NavigationGroup>(groups: T[], preferences: TenantNavigationPreferences, workspace: WorkspaceKey): T[] {
  return groups.flatMap(group => {
    const override = preferences.groups[`${workspace}:${group.label}`]
    if (override?.hidden) return []
    const items = group.items.filter(item => !preferences.menus[item.path]?.hidden)
      .map(item => ({ ...item, label: preferences.menus[item.path]?.label || item.label }))
      .sort((a, b) => (preferences.menus[a.path]?.order ?? 0) - (preferences.menus[b.path]?.order ?? 0))
    return items.length ? [{ ...group, label: override?.label || group.label, items }] : []
  }).sort((a, b) => {
    const originalLabel = (group: T) => groups.find(source => source.items.some(item => item.path === group.items[0]?.path))?.label || group.label
    return (preferences.groups[`${workspace}:${originalLabel(a)}`]?.order ?? 0) - (preferences.groups[`${workspace}:${originalLabel(b)}`]?.order ?? 0)
  }) as T[]
}

export function navigationCatalog(local: NavigationCatalogItem[], raw: Array<Record<string, any>>): NavigationCatalogItem[] {
  const result = new Map(local.map(item => [item.path, item]))
  const ownedWorkspaces = new Set(local.map(item => item.workspace))
  function walk(nodes: Array<Record<string, any>>, group = '') {
    for (const node of nodes) {
      const code = String(node.menuCode || '')
      const legacyPaths: Record<string, string> = { building_iot: '/iot/console', building_vrf_energy: '/iot/air-conditioning/energy', building_device_bindings: '/device-bindings' }
      let path = legacyPaths[code] || String(node.path || '')
      if (node.children?.length) walk(node.children, node.menuName || group)
      const workspace = /^\/iot\/(console|projects|streetlights|air-conditioning|platform|feige|diagnostics)(\/|$)/.test(path) ? 'iot' : code.startsWith('building_') ? 'building' : /^iot_(app_)?/.test(code) ? 'iot' : undefined
      if (code.includes('_group_') || !workspace || ownedWorkspaces.has(workspace) || !path.startsWith('/') || path.includes(':') || path.includes('?')) continue
      if (workspace === 'iot' && !path.startsWith('/iot/')) path = `/iot${path}`
      const iotGroups: Record<string, string> = { iot_console: '工作台', iot_air_conditioning: '多联机组', iot_streetlights: '智能路灯', iot_resources: '资源建模', iot_operations: '运行控制', iot_feige_access: '飞鸽接入', iot_feige_resources: '飞鸽资源管理', iot_diagnostics: '系统诊断' }
      const canonicalGroup = workspace === 'iot' ? (path.startsWith('/iot/air-conditioning/') ? '多联机组' : ['/iot/console', '/iot/projects'].includes(path) ? '工作台' : iotGroups[code.replace('iot_app_', 'iot_')] || group) : group
      if (!result.has(path)) result.set(path, { workspace, group: canonicalGroup || workspaceNames[workspace], path, label: node.menuName || code })
    }
  }
  walk(raw)
  return [...result.values()]
}

export function availableNavigationWorkspaces<T extends { key: WorkspaceKey; label: string; shortLabel?: string; path: string }>(definitions: T[], catalog: NavigationCatalogItem[], preferences: TenantNavigationPreferences): T[] {
  return definitions.flatMap(item => {
    const override = preferences.workspaces[item.key]
    if (override?.hidden) return []
    const first = catalog.find(menu => menu.workspace === item.key && !preferences.menus[menu.path]?.hidden
      && !preferences.groups[`${item.key}:${menu.group}`]?.hidden && (item.key !== 'jbm' || isSystemBusinessMenu(menu.path)))
    if (!first) return []
    return [{ ...item, label: override?.label || workspaceNames[item.key], shortLabel: override?.label || workspaceNames[item.key], path: first.path }]
  }).sort((a, b) => (preferences.workspaces[a.key]?.order ?? 0) - (preferences.workspaces[b.key]?.order ?? 0))
}

