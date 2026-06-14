import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentMenus } from '@/api/current'
import type { BaseMenu } from '@/api/types'
import {
  buildNavGroups,
  normalizeMenuPath,
  SELF_SERVICE_NAV_GROUPS,
  SELF_SERVICE_PATHS,
  STATIC_NAV_GROUPS,
  type NavGroupDef,
} from '@/constants/adminNav'

function isPathInBase(path: string, base: string) {
  return path === base || path.startsWith(`${base}/`)
}

export const useMenuStore = defineStore('menu', () => {
  const rawMenus = ref<BaseMenu[]>([])
  const authorityMenuCodes = ref<string[]>([])
  const superAdmin = ref(false)
  const loaded = ref(false)
  const loadError = ref('')

  const allowedPaths = computed(() => {
    const set = new Set<string>()
    for (const m of rawMenus.value) {
      const path = normalizeMenuPath(m.path)
      if (path) set.add(path)
    }
    return set
  })

  const allowedMenuCodes = computed(() => {
    const set = new Set<string>()
    for (const m of rawMenus.value) {
      if (m.menuCode) addMenuCode(set, m.menuCode)
    }
    for (const code of authorityMenuCodes.value) {
      addMenuCode(set, code)
    }
    return set
  })

  const navGroups = computed<NavGroupDef[]>(() => {
    if (superAdmin.value) return STATIC_NAV_GROUPS
    if (!loaded.value || rawMenus.value.length === 0) {
      return authorityMenuCodes.value.length === 0
        ? SELF_SERVICE_NAV_GROUPS
        : buildNavGroups(allowedPaths.value, allowedMenuCodes.value)
    }
    return buildNavGroups(allowedPaths.value, allowedMenuCodes.value)
  })

  function isRouteAllowed(path: string): boolean {
    if (superAdmin.value) return true
    if (path === '/message-center') return true
    if (!loaded.value) {
      return path === '/dashboard' || [...SELF_SERVICE_PATHS].some((p) => isPathInBase(path, p))
    }
    if (rawMenus.value.length === 0 && authorityMenuCodes.value.length === 0) {
      return path === '/dashboard' || [...SELF_SERVICE_PATHS].some((p) => isPathInBase(path, p))
    }
    if (path === '/dashboard' || path.startsWith('/dashboard')) return true
    if ([...SELF_SERVICE_PATHS].some((p) => isPathInBase(path, p))) return true
    if (allowedPaths.value.has(path)) return true
    if ([...allowedPaths.value].some((p) => isPathInBase(path, p))) return true
    const normalized = normalizeMenuPath(path)
    if (normalized && allowedPaths.value.has(normalized)) return true
    const matchedNavItem = STATIC_NAV_GROUPS
      .flatMap((group) => group.items)
      .find((item) => isPathInBase(path, item.to) || item.to === normalized)
    if (matchedNavItem?.menuCodes?.some((code) => allowedMenuCodes.value.has(code))) {
      return true
    }
    return false
  }

  async function fetchMenus() {
    loadError.value = ''
    try {
      rawMenus.value = await getCurrentMenus()
    } catch (e) {
      rawMenus.value = []
      loadError.value = e instanceof Error ? e.message : '菜单加载失败'
    } finally {
      loaded.value = true
    }
  }

  function clear() {
    rawMenus.value = []
    authorityMenuCodes.value = []
    superAdmin.value = false
    loaded.value = false
    loadError.value = ''
  }

  function setAuthorityCodes(codes: string[]) {
    authorityMenuCodes.value = codes
  }

  function setSuperAdmin(value: boolean) {
    superAdmin.value = value
  }

  return {
    rawMenus,
    superAdmin,
    loaded,
    loadError,
    navGroups,
    allowedPaths,
    allowedMenuCodes,
    isRouteAllowed,
    fetchMenus,
    setAuthorityCodes,
    setSuperAdmin,
    clear,
  }
})

function addMenuCode(set: Set<string>, code: string) {
  if (!code) return
  set.add(code)
  if (code.startsWith('MENU_')) set.add(code.slice(5))
}
