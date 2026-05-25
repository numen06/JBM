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

export const useMenuStore = defineStore('menu', () => {
  const rawMenus = ref<BaseMenu[]>([])
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
      if (m.menuCode) set.add(m.menuCode)
    }
    return set
  })

  const navGroups = computed<NavGroupDef[]>(() => {
    if (!loaded.value || rawMenus.value.length === 0) {
      return SELF_SERVICE_NAV_GROUPS
    }
    return buildNavGroups(allowedPaths.value, allowedMenuCodes.value)
  })

  function isRouteAllowed(path: string): boolean {
    if (!loaded.value) return path === '/dashboard' || SELF_SERVICE_PATHS.has(path)
    if (rawMenus.value.length === 0) return path === '/dashboard' || SELF_SERVICE_PATHS.has(path)
    if (path === '/dashboard' || path.startsWith('/dashboard')) return true
    if (SELF_SERVICE_PATHS.has(path)) return true
    if (allowedPaths.value.has(path)) return true
    const normalized = normalizeMenuPath(path)
    if (normalized && allowedPaths.value.has(normalized)) return true
    const matchedNavItem = STATIC_NAV_GROUPS
      .flatMap((group) => group.items)
      .find((item) => item.to === path || item.to === normalized)
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
    loaded.value = false
    loadError.value = ''
  }

  return {
    rawMenus,
    loaded,
    loadError,
    navGroups,
    allowedPaths,
    allowedMenuCodes,
    isRouteAllowed,
    fetchMenus,
    clear,
  }
})
