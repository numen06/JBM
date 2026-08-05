import type { JbmClient } from '@jbm7/sdk'
import { createJbmVuePlugin, type JbmFrontendModule } from '@jbm7/vue-core'
import type { JbmAdminRuntimeConfig } from '@/runtimeConfig'
import { adminChildModules } from './modules'

export interface JbmAdminRuntimeOptions {
  client: JbmClient
  runtimeConfig?: JbmAdminRuntimeConfig
}

export interface CreateJbmAdminAppOptions {
  mount?: string | Element
  base?: string
  runtimeConfig?: JbmAdminRuntimeConfig
  modules?: JbmFrontendModule[]
}

export async function configureJbmAdminRuntime(options: JbmAdminRuntimeOptions) {
  const [{ configureRuntimeConfig }, { setPlatformClient }] = await Promise.all([
    import('@/runtimeConfig'),
    import('@/platformClient'),
  ])
  configureRuntimeConfig(options.runtimeConfig)
  setPlatformClient(options.client)
  return createJbmVuePlugin({ client: options.client })
}

export async function createJbmAdminApp(options: CreateJbmAdminAppOptions = {}) {
  const runtimeModule = await import('@/runtimeConfig')
  runtimeModule.configureRuntimeConfig(options.runtimeConfig)
  const [vue, piniaModule, routerModule, appModule, guardModule, clientModule] = await Promise.all([
    import('vue'),
    import('pinia'),
    import('vue-router'),
    import('@/App.vue'),
    import('@/adminRouteGuard'),
    import('@/platformClient'),
  ])
  const app = vue.createApp(appModule.default)
  const pinia = piniaModule.createPinia()
  app.use(pinia)
  const router = routerModule.createRouter({
    history: routerModule.createWebHistory(options.base ?? '/'),
    routes: (await import('./modules')).adminHostRoutes,
  })
  guardModule.installJbmAdminRouteGuard(router)
  const client = clientModule.createAdminPlatformClient(router)
  const [{ useAuthStore }, { useMenuStore }] = await Promise.all([
    import('@/stores/auth'),
    import('@/stores/menu'),
  ])
  const authStore = useAuthStore()
  const menuStore = useMenuStore()
  app.use(createJbmVuePlugin({
    client,
    access: {
      isAuthenticated: () => authStore.isLoggedIn,
      hasMenu: (code) => menuStore.allowedMenuCodes.has(code),
      hasPermission: (code) => authStore.user?.authorities?.some((item) =>
        item.authority === code || item.authorityId === code,
      ) ?? false,
    },
  }))
  app.use(router)
  if (options.modules?.length) {
    const { registerJbmModules } = await import('@jbm7/vue-core')
    registerJbmModules({ router, modules: options.modules, parentRouteName: 'jbm-admin-shell' })
    menuStore.registerModules(options.modules)
  }
  await router.isReady()
  app.mount(options.mount ?? '#app')
  return { app, router, pinia, client, builtInModules: adminChildModules }
}
