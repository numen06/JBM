import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

export function installJbmAdminRouteGuard(router: Router) {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()
    if (to.meta.public) {
      if (auth.isLoggedIn && to.meta.authRedirect) {
        if (!auth.user) {
          const currentUser = await auth.fetchUser()
          if (!currentUser) {
            auth.clearSession()
            return true
          }
        }
        return { name: 'dashboard' }
      }
      return true
    }
    if (!auth.isLoggedIn) return { name: 'login', query: { redirect: to.fullPath } }
    if (!auth.user) {
      const currentUser = await auth.fetchUser()
      if (!currentUser) {
        auth.clearSession()
        return { name: 'login', query: { redirect: to.fullPath } }
      }
    }
    const menuStore = useMenuStore()
    if (!menuStore.loaded) await menuStore.fetchMenus()
    if (!auth.isLoggedIn) return { name: 'login', query: { redirect: to.fullPath } }
    if (isMessageAdminRoute(to.path) && isMessageAdminToolAllowed(auth.user)) return true
    if (to.name === 'profile') return true
    if (to.meta.menuCode && menuStore.allowedMenuCodes.has(to.meta.menuCode)) return true
    if (!menuStore.isRouteAllowed(to.path)) return { name: 'dashboard' }
    return true
  })
}

function isMessageAdminRoute(path: string) {
  return ['/messages/send-test', '/messages/channels', '/messages/webhook-configs', '/messages/webhook-tasks'].includes(path)
}

function isMessageAdminToolAllowed(user: ReturnType<typeof useAuthStore>['user']) {
  if (!user) return false
  if (user.userId === 1 || user.userName?.toLowerCase() === 'admin') return true
  const authorities = user.authorities?.map((item) => item.authority || item.authorityId || '') ?? []
  return authorities.some((item) => [
    'message_send_test', 'MENU_message_send_test', 'message_push_test', 'MENU_message_push_test',
    'message_channels', 'MENU_message_channels', 'push_test', 'MENU_push_test', 'push_config',
    'MENU_push_config', 'webhook_event_config', 'MENU_webhook_event_config', 'webhook_tasks',
    'MENU_webhook_tasks', 'push_webhook', 'MENU_push_webhook', 'ACTION_push:test', 'MENU_push',
  ].includes(item))
}
