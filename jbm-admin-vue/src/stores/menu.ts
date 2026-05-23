import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentMenus } from '@/api/current'
import type { BaseMenu } from '@/api/types'
import { buildNavGroups, normalizeMenuPath, STATIC_NAV_GROUPS, type NavGroupDef } from '@/constants/adminNav'

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
      return STATIC_NAV_GROUPS
    }
    return buildNavGroups(allowedPaths.value, allowedMenuCodes.value)
  })

  const isFullAccess = computed(
    () => !loaded.value || rawMenus.value.length >= 10 || allowedPaths.value.size >= 10,
  )

  function isRouteAllowed(path: string): boolean {
    if (!loaded.value || rawMenus.value.length === 0 || isFullAccess.value) return true
    if (path === '/dashboard' || path.startsWith('/dashboard')) return true
    return allowedPaths.value.has(path)
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
    isFullAccess,
    isRouteAllowed,
    fetchMenus,
    clear,
  }
})
