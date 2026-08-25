import type { App, Component } from 'vue'
import type { Pinia } from 'pinia'
import type { Router, RouteRecordRaw } from 'vue-router'
import type { JbmClient } from '@jbm7/sdk'
import type { JbmAccessProvider, JbmFrontendModule } from '@jbm7/vue-core'
import type { Plugin } from 'vue'

export interface JbmAdminRuntimeConfig {
  apiBaseUrl?: string
  debug?: string | boolean
  oauthClientId?: string
  oauthAuthorizeBaseUrl?: string
}

export interface JbmAdminRuntimeOptions {
  client: JbmClient
  runtimeConfig?: JbmAdminRuntimeConfig
  access?: JbmAccessProvider
}

export interface CreateJbmAdminHostOptions {
  router: Router
  runtimeConfig?: JbmAdminRuntimeConfig
  modules?: JbmFrontendModule[]
  parentRouteName?: string
}

export interface JbmTenantOption {
  value: string
  label: string
  name?: string
  code?: string
  source: 'current' | 'platform' | 'delegated'
}

export interface JbmAdminHostHandle {
  client: JbmClient
  access: JbmAccessProvider
  plugin: Plugin
  auth: {
    readonly isLoggedIn: boolean
    readonly accessToken: string
    readonly refreshToken: string
    readonly tenantId: string
    readonly user: unknown
    login(username: string, password: string, options?: { vcode?: string; loginType?: string }): Promise<boolean>
    registerTenant(params: {
      tenantName?: string
      organizationType?: 'personal' | 'organization'
      userName: string
      password: string
      confirmPassword?: string
      nickName?: string
      email?: string
      mobile?: string
      smsCode?: string
      vcode: string
    }): Promise<{
      tenantId: string | number
      tenantName: string
      userId: string | number
      userName: string
      appId: string | number
      roleCode: string
    }>
    registrationCaptcha(width?: number, height?: number): Promise<string>
    registrationSmsConfig(): Promise<{
      registrationRequired: boolean
      interval: number
      validTime: number
      debugBypass: boolean
    }>
    sendRegistrationSms(phone: string, imageVcode: string): Promise<void>
    logout(): Promise<void>
    refreshAccessToken(): Promise<void>
    clearSession(): void
    init(): Promise<void>
  }
  menu: {
    readonly allowedMenuCodes: ReadonlySet<string>
    registerModules(modules: JbmFrontendModule[]): void
  }
  tenants: {
    listAvailable(): Promise<JbmTenantOption[]>
  }
  initialize(): Promise<void>
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
export const JbmFeedbackHost: Component

export function configureJbmAdminRuntime(options: JbmAdminRuntimeOptions): Promise<unknown>
export function createJbmAdminApp(options?: CreateJbmAdminAppOptions): Promise<JbmAdminAppHandle>
export function createJbmAdminHost(options: CreateJbmAdminHostOptions): Promise<JbmAdminHostHandle>
export function mergeJbmTenantOptions(
  currentTenantId: string,
  currentTenantName?: string,
  platformTenants?: Array<{ tenantId?: string | number; tenantName?: string; tenantCode?: string }>,
  delegations?: Array<{ ownerTenantId?: string | number; ownerTenantName?: string; purpose?: string }>,
): JbmTenantOption[]
