import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { OAuth2TokenResult } from '@/api/types'
import { getCurrentUser } from '@/api/current'
import type { CurrentUser } from '@/api/types'
import { useMenuStore } from '@/stores/menu'
import { JBM_DEFAULT_CLIENT_ID, JBM_DEFAULT_CLIENT_SECRET } from '@/constants/loginModes'

const TOKEN_KEY = 'jbm_access_token'
const REFRESH_KEY = 'jbm_refresh_token'
const CLIENT_ID_KEY = 'jbm_client_id'
const CLIENT_SECRET_KEY = 'jbm_client_secret'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem(TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_KEY) || '')
  const clientId = ref(localStorage.getItem(CLIENT_ID_KEY) || JBM_DEFAULT_CLIENT_ID)
  const clientSecret = ref(localStorage.getItem(CLIENT_SECRET_KEY) || JBM_DEFAULT_CLIENT_SECRET)
  const tenantId = ref('0')
  const user = ref<CurrentUser | null>(null)
  const mustChangePassword = ref(false)
  let storageSyncBound = false

  const isLoggedIn = computed(() => !!accessToken.value)

  function persistTokens() {
    if (accessToken.value) localStorage.setItem(TOKEN_KEY, accessToken.value)
    else localStorage.removeItem(TOKEN_KEY)
    if (refreshToken.value) localStorage.setItem(REFRESH_KEY, refreshToken.value)
    else localStorage.removeItem(REFRESH_KEY)
    localStorage.setItem(CLIENT_ID_KEY, clientId.value)
    localStorage.setItem(CLIENT_SECRET_KEY, clientSecret.value)
  }

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
      clientSecret: clientSecret.value,
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

  async function refreshAccessToken() {
    if (!refreshToken.value) throw new Error('无 refresh token')
    const token = await authApi.refreshToken(
      refreshToken.value,
      clientId.value,
      clientSecret.value,
    )
    accessToken.value = token.access_token
    if (token.refresh_token) refreshToken.value = token.refresh_token
    persistTokens()
  }

  async function fetchUser(): Promise<CurrentUser | null> {
    try {
      user.value = await getCurrentUser()
      const menuStore = useMenuStore()
      menuStore.setSuperAdmin(isSuperAdminUser(user.value))
      menuStore.setAuthorityCodes(
        user.value.authorities?.map((item) => item.authority || item.authorityId || '') ?? [],
      )
      return user.value
    } catch {
      user.value = null
      const menuStore = useMenuStore()
      menuStore.setSuperAdmin(false)
      menuStore.setAuthorityCodes([])
      return null
    }
  }

  function isSuperAdminUser(currentUser: CurrentUser | null) {
    if (!currentUser) return false
    if (currentUser.userName === 'admin') return true
    return (currentUser.roles ?? []).some(
      (role) => role.roleCode === 'super_admin' || role.roleId === 1,
    )
  }

  async function logout() {
    await authApi.logout()
    clearSession()
  }

  function clearSession() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    mustChangePassword.value = false
    useMenuStore().clear()
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
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

  async function init() {
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
    clientSecret,
    tenantId,
    user,
    mustChangePassword,
    isLoggedIn,
    login,
    loginWithToken,
    applyToken,
    clearMustChangePassword,
    logout,
    refreshAccessToken,
    fetchUser,
    clearSession,
    bindSessionStorageSync,
    init,
  }
})
