import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { isPlatformAdministrator, isSuperAdministrator, platformMenuCodes } from '@/lib/adminAccess'

/** 按钮/菜单权限：authority 形如 MENU_users、ACTION_users_add */
export function usePermission() {
  const auth = useAuthStore()

  const authoritySet = computed(() => {
    const set = new Set<string>()
    for (const a of auth.user?.authorities ?? []) {
      if (a.authority) set.add(a.authority)
    }
    return set
  })

  const isSuperAdmin = computed(() => isSuperAdministrator(auth.user))
  const isPlatformAdmin = computed(() => isPlatformAdministrator(auth.user))

  function hasAuthority(code: string) {
    if (isSuperAdmin.value) return true
    return authoritySet.value.has(code)
  }

  function hasMenu(menuCode: string) {
    if (!isPlatformAdmin.value && platformMenuCodes.has(menuCode)) return false
    return hasAuthority(`MENU_${menuCode}`)
  }

  function hasAction(actionCode: string) {
    if (!isPlatformAdmin.value && [...platformMenuCodes].some(code => actionCode === code || actionCode.startsWith(code + '_'))) return false
    return hasAuthority(`ACTION_${actionCode}`)
  }

  return { authoritySet, isSuperAdmin, isPlatformAdmin, hasAuthority, hasMenu, hasAction }
}
