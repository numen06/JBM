import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentMenus } from '@/api/current'
import type { BaseMenu } from '@/api/types'
import type { JbmFrontendModule } from '@jbm7/vue-core'
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
  const extensionModules = ref<JbmFrontendModule[]>([])

  const allowedPaths = computed(() => {
    const set = new Set<string>()
    for (const m of flattenMenus(rawMenus.value)) {
      const path = normalizeMenuPath(m.path)
      if (path) set.add(path)
    }
    return set
  })

  const allowedMenuCodes = computed(() => {
    const set = new Set<string>()
    for (const m of flattenMenus(rawMenus.value)) {
      if (m.menuCode) addMenuCode(set, m.menuCode)
    }
    for (const code of authorityMenuCodes.value) {
      addMenuCode(set, code)
    }
    return set
  })

  const navGroups = computed<NavGroupDef[]>(() => {
    let builtIn = buildNavGroups(allowedPaths.value, allowedMenuCodes.value)
    if (superAdmin.value) builtIn = STATIC_NAV_GROUPS
    else if (authorityMenuCodes.value.length === 0 && (!loaded.value || rawMenus.value.length === 0)) {
      builtIn = SELF_SERVICE_NAV_GROUPS
    }
    const extensions = extensionModules.value.flatMap((module) =>
      (module.navigation ?? []).flatMap((group) => {
        const items = group.items.filter((item) =>
          superAdmin.value || (
            (!item.menuCodes?.length || item.menuCodes.some((code) => allowedMenuCodes.value.has(code))) &&
            (!item.permissions?.length || item.permissions.every((code) => allowedMenuCodes.value.has(code)))
          ),
        )
        return items.length ? [{ ...group, items }] : []
      }),
    )
    return mergeNavGroups([...builtIn, ...extensions])
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

  function registerModules(modules: JbmFrontendModule[]) {
    extensionModules.value = [...modules]
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
    registerModules,
    clear,
  }
})

function flattenMenus(menus: BaseMenu[]): BaseMenu[] {
  return menus.flatMap((menu) => [menu, ...flattenMenus(menu.children ?? [])])
}

function mergeNavGroups(groups: NavGroupDef[]) {
  const merged = new Map<string, NavGroupDef>()
  for (const group of groups) {
    const current = merged.get(group.label)
    if (!current) {
      merged.set(group.label, { ...group, items: [...group.items] })
      continue
    }
    const paths = new Set(current.items.map((item) => item.to))
    current.items.push(...group.items.filter((item) => !paths.has(item.to)))
  }
  return [...merged.values()]
}

function addMenuCode(set: Set<string>, code: string) {
  if (!code) return
  set.add(code)
  if (code.startsWith('MENU_')) set.add(code.slice(5))
}
