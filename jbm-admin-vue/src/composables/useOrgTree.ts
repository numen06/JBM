import { computed, ref } from 'vue'
import { listOrgTree } from '@/api/org'
import type { BaseOrg } from '@/api/types'

export type FlatOrg = BaseOrg & { depth: number }

export function orgRowId(row: BaseOrg) {
  return row.id ?? row.orgId
}

export function flattenOrgs(orgs: BaseOrg[], depth = 0): FlatOrg[] {
  const out: FlatOrg[] = []
  for (const o of orgs) {
    out.push({ ...o, depth })
    if (o.children?.length) out.push(...flattenOrgs(o.children, depth + 1))
  }
  return out
}

export function orgOptionLabel(row: FlatOrg) {
  const prefix = row.depth ? `${'　'.repeat(row.depth)}└ ` : ''
  return `${prefix}${row.orgName ?? orgRowId(row)}`
}

export function useOrgTree() {
  const flatOrgs = ref<FlatOrg[]>([])
  const loading = ref(false)

  const orgNameMap = computed(() => {
    const m = new Map<number, string>()
    for (const o of flatOrgs.value) {
      const id = orgRowId(o)
      if (id != null) m.set(id, o.orgName ?? '')
    }
    return m
  })

  function orgLabel(id?: number) {
    if (id == null) return '—'
    return orgNameMap.value.get(id) ?? String(id)
  }

  async function loadOrgs() {
    loading.value = true
    try {
      flatOrgs.value = flattenOrgs(await listOrgTree())
    } catch {
      flatOrgs.value = []
    } finally {
      loading.value = false
    }
  }

  return { flatOrgs, loading, orgNameMap, orgLabel, loadOrgs, orgRowId, orgOptionLabel }
}
