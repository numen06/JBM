import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { OAuth2TokenResult } from '@/api/types'
import { getCurrentUser } from '@/api/current'
import { fetchCaptchaBase64, fetchSmsCodeConfig, sendSmsCode } from '@/api/captcha'
import type { CurrentUser } from '@/api/types'
import { useMenuStore } from '@/stores/menu'
import { JBM_DEFAULT_CLIENT_ID } from '@/constants/loginModes'
import { isPlatformAdministrator, isSuperAdministrator } from '@/lib/adminAccess'
import { runtimeConfig } from '@/runtimeConfig'

const TOKEN_KEY = 'jbm_access_token'
const REFRESH_KEY = 'jbm_refresh_token'
const CLIENT_ID_KEY = 'jbm_client_id'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_KEY) || sessionStorage.getItem(REFRESH_KEY) || '')
  const clientId = ref(localStorage.getItem(CLIENT_ID_KEY) || JBM_DEFAULT_CLIENT_ID)
  const tenantId = ref('0')
  const activeTenantId = ref('0')
  const user = ref<CurrentUser | null>(null)
  const mustChangePassword = ref(false)
  let storageSyncBound = false
  let activityBound = false
  let sessionCheckRunning = false
  let lastActivitySent = 0

  function bindSessionActivity() {
    if (activityBound || typeof window === 'undefined') return
    activityBound = true
    const check = async (activity = false) => {
      if (!accessToken.value || sessionCheckRunning) return
      sessionCheckRunning = true
      try {
        try { await authApi.sessionStatus(accessToken.value, activity) }
        catch (error) {
          if ((error as { status?: number }).status !== 401 || !refreshToken.value) throw error
          await refreshAccessToken()
          await authApi.sessionStatus(accessToken.value, activity)
        }
      } catch (error) {
        const status = (error as { status?: number }).status
        if (status === 400 || status === 401 || status === 403) {
          clearSession()
          if (runtimeConfig.onSessionExpired) await runtimeConfig.onSessionExpired()
          else window.location.replace(`${import.meta.env.BASE_URL}login`)
        }
      } finally { sessionCheckRunning = false }
    }
    const activity = (event: Event) => {
      if (!event.isTrusted || document.visibilityState !== 'visible' || Date.now() - lastActivitySent < 60_000) return
      lastActivitySent = Date.now()
      void check(true)
    }
    for (const name of ['pointerdown', 'keydown', 'wheel', 'touchstart']) window.addEventListener(name, activity, { passive: true, capture: true })
    window.setInterval(() => { void check() }, 30_000)
  }

  const isLoggedIn = computed(() => !!accessToken.value)

  function persistTokens() {
    if (accessToken.value) localStorage.setItem(TOKEN_KEY, accessToken.value)
    else localStorage.removeItem(TOKEN_KEY)
    if (refreshToken.value) localStorage.setItem(REFRESH_KEY, refreshToken.value)
    else localStorage.removeItem(REFRESH_KEY)
    localStorage.setItem(CLIENT_ID_KEY, clientId.value)
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_KEY)
  }

  persistTokens()

  function applyToken(token: OAuth2TokenResult) {
    accessToken.value = token.access_token
    refreshToken.value = token.refresh_token || ''
    mustChangePassword.value = !!token.must_change_password
    persistTokens()
  }

  async function login(
    username: string,
    password: string,
    options?: { vcode?: string; loginType?: string },
  ) {
    const token = await authApi.login({
      username,
      password,
      vcode: options?.vcode,
      loginType: options?.loginType ?? 'PASSWORD',
      clientId: clientId.value,
    })
    applyToken(token)
    await fetchUser()
    await useMenuStore().fetchMenus()
    return mustChangePassword.value
  }

  async function loginWithToken(token: OAuth2TokenResult) {
    applyToken(token)
    await fetchUser()
    await useMenuStore().fetchMenus()
    return mustChangePassword.value
  }

  async function registerTenant(params: Omit<authApi.RegisterParams, 'clientId'>) {
    return authApi.register({ ...params, clientId: clientId.value })
  }

  function registrationCaptcha(width = 120, height = 40) {
    return fetchCaptchaBase64(width, height)
  }

  function registrationSmsConfig() {
    return fetchSmsCodeConfig()
  }

  function sendRegistrationSms(phone: string, imageVcode: string) {
    return sendSmsCode(phone, imageVcode)
  }

  async function refreshAccessToken() {
    const expiredAccessToken = accessToken.value
    const refresh = async () => {
      syncSessionFromStorage()
      if (accessToken.value && accessToken.value !== expiredAccessToken) return
      if (!refreshToken.value) throw new Error('无 refresh token')
      const token = await authApi.refreshToken(
        refreshToken.value,
        clientId.value,
      )
      accessToken.value = token.access_token
      if (token.refresh_token) refreshToken.value = token.refresh_token
      persistTokens()
    }
    if (typeof navigator !== 'undefined' && navigator.locks) {
      await navigator.locks.request('jbm-auth-token-refresh', refresh)
      return
    }
    await refresh()
  }

  async function fetchUser(): Promise<CurrentUser | null> {
    try {
      user.value = await getCurrentUser()
      tenantId.value = String(user.value?.companyId || '0')
      activeTenantId.value = tenantId.value
      const menuStore = useMenuStore()
      menuStore.setSuperAdmin(isSuperAdminUser(user.value))
      menuStore.setPlatformAccount(isPlatformAdministrator(user.value))
      menuStore.setAuthorityCodes(
        user.value.authorities?.map((item) => item.authority || item.authorityId || '') ?? [],
      )
      return user.value
    } catch {
      user.value = null
      tenantId.value = '0'
      activeTenantId.value = '0'
      const menuStore = useMenuStore()
      menuStore.setSuperAdmin(false)
      menuStore.setPlatformAccount(false)
      menuStore.setAuthorityCodes([])
      return null
    }
  }

  function isSuperAdminUser(currentUser: CurrentUser | null) {
    return isSuperAdministrator(currentUser)
  }

  async function logout() {
    try {
      await authApi.logout(refreshToken.value)
    } finally {
      clearSession()
    }
  }

  function clearSession() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    tenantId.value = '0'
    activeTenantId.value = '0'
    mustChangePassword.value = false
    useMenuStore().clear()
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_KEY)
  }

  function syncSessionFromStorage(event?: StorageEvent) {
    if (event?.key && ![TOKEN_KEY, REFRESH_KEY].includes(event.key)) return
    const nextAccessToken = localStorage.getItem(TOKEN_KEY) || ''
    const nextRefreshToken = localStorage.getItem(REFRESH_KEY) || ''
    if (nextAccessToken === accessToken.value && nextRefreshToken === refreshToken.value) return
    const tokenChanged = nextAccessToken !== accessToken.value
    accessToken.value = nextAccessToken
    refreshToken.value = nextRefreshToken
    if (tokenChanged) {
      user.value = null
      mustChangePassword.value = false
      useMenuStore().clear()
      if (nextAccessToken) {
        init()
      } else if (event) {
        if (runtimeConfig.onSessionExpired) void runtimeConfig.onSessionExpired()
        else window.location.replace(`${import.meta.env.BASE_URL}login`)
      }
    }
  }

  function bindSessionStorageSync() {
    if (storageSyncBound || typeof window === 'undefined') return
    storageSyncBound = true
    window.addEventListener('storage', syncSessionFromStorage)
  }

  function clearMustChangePassword() {
    mustChangePassword.value = false
  }

  function setActiveTenant(value: string) {
    activeTenantId.value = String(value || tenantId.value || '0')
  }

  async function init() {
    bindSessionActivity()
    if (accessToken.value) {
      const currentUser = await fetchUser()
      if (!currentUser) {
        clearSession()
        return
      }
      await useMenuStore().fetchMenus()
    }
  }

  return {
    accessToken,
    refreshToken,
    clientId,
    tenantId,
    activeTenantId,
    user,
    mustChangePassword,
    isLoggedIn,
    login,
    registerTenant,
    registrationCaptcha,
    registrationSmsConfig,
    sendRegistrationSms,
    loginWithToken,
    applyToken,
    clearMustChangePassword,
    setActiveTenant,
    logout,
    refreshAccessToken,
    fetchUser,
    clearSession,
    bindSessionStorageSync,
    bindSessionActivity,
    init,
  }
})
