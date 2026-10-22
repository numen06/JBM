type AdminIdentity = { userName?: string; admin?: boolean; roles?: Array<{ roleCode?: string }> } | null

export function isSuperAdministrator(user: AdminIdentity): boolean {
  return !!user && (user.admin === true || user.userName === 'admin' || user.roles?.some(role => role.roleCode === 'super_admin') === true)
}

export function isPlatformAdministrator(user: AdminIdentity): boolean {
  return isSuperAdministrator(user) || user?.roles?.some(role => role.roleCode === 'platform_operator') === true
}

const platformPaths = ['/system/apps', '/system/menus', '/system/roles', '/system/database-backups', '/authority', '/api', '/gateway']
export const platformMenuCodes = new Set([
  'apps', 'app', 'roles', 'role', 'authority', 'authority_catalog', 'user_perm', 'user_permissions',
  'client_perm', 'client_permissions', 'api_perm', 'api_permissions', 'menus', 'menu', 'actions', 'action',
  'api_registry', 'api_mgmt', 'api_docs', 'api_monitor', 'gw_services', 'gw_routes', 'gw_gray', 'gw_rate', 'gw_ip',
  'database_backups',
])

export function isPlatformAdminPath(path: string): boolean {
  const normalized = path.replace(/^\/jbm(?=\/)/, '')
  return platformPaths.some(base => normalized === base || normalized.startsWith(base + '/'))
}
