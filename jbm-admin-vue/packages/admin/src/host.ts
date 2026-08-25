import type { JbmClient } from '@jbm7/sdk'
import {
  createJbmVuePlugin,
  registerJbmModules,
  type JbmAccessProvider,
  type JbmFrontendModule,
} from '@jbm7/vue-core'
import type { Plugin } from 'vue'
import type { Router } from 'vue-router'
import type { JbmAdminRuntimeConfig } from '@/runtimeConfig'
import { createJbmTenantDirectory } from './tenants'

export interface CreateJbmAdminHostOptions {
  router: Router
  runtimeConfig?: JbmAdminRuntimeConfig
  modules?: JbmFrontendModule[]
  parentRouteName?: string
}

/**
 * Creates the JBM administration runtime inside an existing Vue application.
 * The host must install Pinia before calling this function. JBM remains the
 * single owner of browser tokens, current-user state and menu permissions.
 */
export async function createJbmAdminHost(options: CreateJbmAdminHostOptions) {
  const [runtimeModule, clientModule, authModule, menuModule] = await Promise.all([
    import('@/runtimeConfig'),
    import('@/platformClient'),
    import('@/stores/auth'),
    import('@/stores/menu'),
  ])
  runtimeModule.configureRuntimeConfig(options.runtimeConfig)
  const client = clientModule.createAdminPlatformClient(options.router)
  const auth = authModule.useAuthStore()
  const configuredClientId = options.runtimeConfig?.oauthClientId?.trim()
  if (configuredClientId) auth.clientId = configuredClientId
  const menu = menuModule.useMenuStore()
  const tenants = createJbmTenantDirectory(client, () => {
    const user = auth.user as Record<string, unknown> | null
    return {
      tenantId: String(auth.tenantId || user?.tenantId || '0'),
      tenantName: String(user?.companyName || user?.tenantName || ''),
    }
  })
  const access: JbmAccessProvider = {
    isAuthenticated: () => auth.isLoggedIn,
    hasMenu: (code) => menu.allowedMenuCodes.has(code),
    hasPermission: (code) => auth.user?.authorities?.some((item) =>
      item.authority === code || item.authorityId === code,
    ) ?? false,
  }
  if (options.modules?.length) {
    registerJbmModules({
      router: options.router,
      modules: options.modules,
      parentRouteName: options.parentRouteName,
    })
    menu.registerModules(options.modules)
  }
  return {
    client,
    auth,
    menu,
    tenants,
    access,
    plugin: createJbmVuePlugin({ client, access }) as Plugin,
    initialize: () => {
      auth.bindSessionStorageSync()
      return auth.init()
    },
  }
}

export type JbmAdminHost = Awaited<ReturnType<typeof createJbmAdminHost>>
export type { JbmClient }
export type { JbmTenantOption } from './tenants'
export { mergeJbmTenantOptions } from './tenants'
