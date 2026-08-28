import { createJbmClient, type JbmClient, type JbmTokens } from '@jbm7/sdk'
import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { apiBaseUrl } from '@/runtimeConfig'

let platformClient: JbmClient | undefined

export function setPlatformClient(client: JbmClient) {
  platformClient = client
}

export function getPlatformClient() {
  if (!platformClient) throw new Error('JBM platform client has not been configured')
  return platformClient
}

export function createAdminPlatformClient(router: Router) {
  const client = createJbmClient({
    baseUrl: apiBaseUrl,
    tokenProvider: {
      getAccessToken: () => useAuthStore().accessToken || undefined,
      getRefreshToken: () => useAuthStore().refreshToken || undefined,
      updateTokens: (tokens) => useAuthStore().applyToken(fromSdkTokens(tokens)),
      clearTokens: () => useAuthStore().clearSession(),
    },
    tenantProvider: {
      getTenantId: () => useAuthStore().activeTenantId || useAuthStore().tenantId || undefined,
    },
    refreshTokens: async (refreshToken) => {
      const auth = useAuthStore()
      const api = await import('@/api/auth')
      const tokens = await api.refreshToken(refreshToken, auth.clientId)
      return toSdkTokens(tokens)
    },
    onUnauthorized: async () => {
      if (router.currentRoute.value.name !== 'login') {
        await router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    },
  })
  setPlatformClient(client)
  return client
}

function toSdkTokens(tokens: {
  access_token: string
  refresh_token?: string
  expires_in?: number
  token_type?: string
}): JbmTokens {
  return {
    accessToken: tokens.access_token,
    refreshToken: tokens.refresh_token,
    expiresIn: tokens.expires_in,
    tokenType: tokens.token_type,
  }
}

function fromSdkTokens(tokens: JbmTokens) {
  return {
    access_token: tokens.accessToken,
    refresh_token: tokens.refreshToken,
    expires_in: tokens.expiresIn,
    token_type: tokens.tokenType,
  }
}
