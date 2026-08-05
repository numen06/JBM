import type { App } from 'vue'
import type { Pinia } from 'pinia'
import type { Router, RouteRecordRaw } from 'vue-router'
import type { JbmClient } from '@jbm7/sdk'
import type { JbmFrontendModule } from '@jbm7/vue-core'

export interface JbmAdminRuntimeConfig {
  apiBaseUrl?: string
  debug?: string | boolean
  oauthClientId?: string
  oauthAuthorizeBaseUrl?: string
}

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

export interface JbmAdminAppHandle {
  app: App
  router: Router
  pinia: Pinia
  client: JbmClient
  builtInModules: JbmFrontendModule[]
}

export const authModule: JbmFrontendModule
export const coreModule: JbmFrontendModule
export const systemModule: JbmFrontendModule
export const authorityModule: JbmFrontendModule
export const openapiModule: JbmFrontendModule
export const gatewayModule: JbmFrontendModule
export const logsModule: JbmFrontendModule
export const messagesModule: JbmFrontendModule
export const jobsModule: JbmFrontendModule
export const documentsModule: JbmFrontendModule
export const adminChildModules: JbmFrontendModule[]
export const adminModules: JbmFrontendModule[]
export const adminHostRoutes: RouteRecordRaw[]

export function configureJbmAdminRuntime(options: JbmAdminRuntimeOptions): Promise<unknown>
export function createJbmAdminApp(options?: CreateJbmAdminAppOptions): Promise<JbmAdminAppHandle>
