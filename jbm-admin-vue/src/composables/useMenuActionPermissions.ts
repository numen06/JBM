import { ref, type Ref } from 'vue'
import type { OpenAuthority } from '@/api/types'
import type { BaseAction } from '@/api/types'
import { toSnowflakeIdString, type SnowflakeId } from '@/lib/snowflakeId'

export function useMenuActionPermissions(
  authorityCatalog: Ref<OpenAuthority[]>,
  menuActions: Ref<Record<string, BaseAction[]>>,
) {
  const selectedAuthorityIds = ref<Set<string>>(new Set())

  function authorityIdForActionCode(actionCode: string) {
    const key = `ACTION_${actionCode}`
    const hit = authorityCatalog.value.find((c) => c.authority === key)
    return hit?.authorityId ? String(hit.authorityId) : null
  }

  function menuActionAuthorityIds(menuId: SnowflakeId) {
    return (menuActions.value[toSnowflakeIdString(menuId)] ?? [])
      .map((a) => (a.actionCode ? authorityIdForActionCode(a.actionCode) : null))
      .filter((id): id is string => !!id)
  }

  function isMenuFullyChecked(menuId?: SnowflakeId) {
    if (!menuId) return false
    return selectedAuthorityIds.value.has(String(menuId))
  }

  function isMenuIndeterminate(menuId?: SnowflakeId) {
    if (!menuId) return false
    if (isMenuFullyChecked(menuId)) return false
    const actionIds = menuActionAuthorityIds(menuId)
    if (!actionIds.length) return false
    const selectedCount = actionIds.filter((id) => selectedAuthorityIds.value.has(id)).length
    return selectedCount > 0
  }

  function toggleMenu(menuId?: SnowflakeId) {
    if (!menuId) return
    const menuKey = String(menuId)
    const next = new Set(selectedAuthorityIds.value)
    if (next.has(menuKey)) {
      next.delete(menuKey)
      for (const actionId of menuActionAuthorityIds(menuId)) {
        next.delete(actionId)
      }
    } else {
      next.add(menuKey)
    }
    selectedAuthorityIds.value = next
  }

  function toggleAction(actionCode?: string, menuId?: SnowflakeId) {
    if (!actionCode) return
    const actionId = authorityIdForActionCode(actionCode)
    if (!actionId) return
    const next = new Set(selectedAuthorityIds.value)
    if (next.has(actionId)) {
      next.delete(actionId)
    } else {
      next.add(actionId)
      if (menuId != null) next.add(String(menuId))
    }
    selectedAuthorityIds.value = next
  }

  function selectAllActionsForMenu(menuId?: SnowflakeId) {
    if (!menuId) return
    const next = new Set(selectedAuthorityIds.value)
    next.add(String(menuId))
    for (const actionId of menuActionAuthorityIds(menuId)) {
      next.add(actionId)
    }
    selectedAuthorityIds.value = next
  }

  function clearAllActionsForMenu(menuId?: SnowflakeId) {
    if (!menuId) return
    const next = new Set(selectedAuthorityIds.value)
    next.delete(String(menuId))
    for (const actionId of menuActionAuthorityIds(menuId)) {
      next.delete(actionId)
    }
    selectedAuthorityIds.value = next
  }

  function ensureMenuPermissionsBeforeSave() {
    const next = new Set(selectedAuthorityIds.value)
    for (const [menuIdStr, actions] of Object.entries(menuActions.value)) {
      const hasSelectedAction = actions.some((a) => {
        const id = a.actionCode ? authorityIdForActionCode(a.actionCode) : null
        return id != null && next.has(id)
      })
      if (hasSelectedAction) next.add(menuIdStr)
    }
    selectedAuthorityIds.value = next
    return Array.from(next)
  }

  function resetSelected(ids: string[]) {
    selectedAuthorityIds.value = new Set(ids.filter(Boolean))
  }

  return {
    selectedAuthorityIds,
    authorityIdForActionCode,
    isMenuFullyChecked,
    isMenuIndeterminate,
    toggleMenu,
    toggleAction,
    selectAllActionsForMenu,
    clearAllActionsForMenu,
    ensureMenuPermissionsBeforeSave,
    resetSelected,
  }
}
