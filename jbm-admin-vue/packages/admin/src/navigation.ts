import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { unwrapJbmResult, type JbmClient } from '@jbm7/sdk'
export { default as JbmNavigationSettings } from '@/components/NavigationSettings.vue'

export * from './navigationPreferences'
import { emptyNavigation, type TenantNavigationContext, type TenantNavigationPreferences } from './navigationPreferences'

export function useTenantNavigation(scope: () => string, client: () => JbmClient) {
  const settings = ref<TenantNavigationContext>({ tenantId: '', canManage: false, revision: 0, preferences: emptyNavigation() })
  const loading = ref(false), saving = ref(false), error = ref('')
  let generation = 0
  async function load() {
    const key = scope(), version = ++generation
    settings.value = { tenantId: '', canManage: false, revision: 0, preferences: emptyNavigation() }
    error.value = ''; loading.value = Boolean(key)
    if (!key) return
    try {
      const value = unwrapJbmResult(await client().get<{ result: TenantNavigationContext }>('/center/current/navigation'))
      if (version === generation && scope() === key) settings.value = value
    } catch { if (version === generation) error.value = '菜单设置加载失败，请重试' }
    finally { if (version === generation) loading.value = false }
  }
  async function save(preferences: TenantNavigationPreferences) {
    if (saving.value || !settings.value.canManage) return false
    const key = scope(), version = generation
    saving.value = true; error.value = ''
    try {
      const value = unwrapJbmResult(await client().put<{ result: TenantNavigationContext }>('/center/current/navigation', { preferences, revision: settings.value.revision }))
      if (scope() !== key || generation !== version) return false
      settings.value = value
      try { localStorage.setItem('jbm:navigation-updated', JSON.stringify({ scope: key, time: Date.now() })) } catch { /* Server remains the source of truth. */ }
      return true
    } catch (reason) {
      if (scope() === key && generation === version) error.value = reason instanceof Error ? reason.message : '菜单设置保存失败'
      return false
    } finally { saving.value = false }
  }
  const refresh = (event: StorageEvent) => { if (event.key === 'jbm:navigation-updated') void load() }
  watch(scope, load, { immediate: true })
  onMounted(() => window.addEventListener('storage', refresh))
  onUnmounted(() => { generation++; window.removeEventListener('storage', refresh) })
  return { settings, loading, saving, error, preferences: computed(() => settings.value.preferences), load, save }
}
