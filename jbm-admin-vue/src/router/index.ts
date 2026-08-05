import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'
import { adminHostRoutes } from '@jbm7/admin'
import { installJbmAdminRouteGuard } from '@/adminRouteGuard'

export function createJbmAdminRouter(history: RouterHistory = createWebHistory(import.meta.env.BASE_URL)) {
  const router = createRouter({ history, routes: adminHostRoutes })
  installJbmAdminRouteGuard(router)
  return router
}

export default createJbmAdminRouter()
