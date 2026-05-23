import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

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

  const isSuperAdmin = computed(() => {
    const name = auth.user?.userName
    if (name === 'admin') return true
    return (auth.user?.roles ?? []).some(
      (r) => r.roleCode === 'super_admin' || r.roleId === 1,
    )
  })

  function hasAuthority(code: string) {
    if (isSuperAdmin.value) return true
    return authoritySet.value.has(code)
  }

  function hasMenu(menuCode: string) {
    return hasAuthority(`MENU_${menuCode}`)
  }

  function hasAction(actionCode: string) {
    return hasAuthority(`ACTION_${actionCode}`)
  }

  return { authoritySet, isSuperAdmin, hasAuthority, hasMenu, hasAction }
}
