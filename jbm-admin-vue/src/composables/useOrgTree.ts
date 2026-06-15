import { computed, ref } from 'vue'
import { listOrgTree } from '@/api/org'
import type { BaseOrg } from '@/api/types'

export type FlatOrg = BaseOrg & { depth: number }

const orgTree = ref<BaseOrg[]>([])
const flatOrgs = ref<FlatOrg[]>([])
const loading = ref(false)

export function orgRowId(row: BaseOrg) {
  return row.id ?? row.orgId
}

function orgParentId(org: BaseOrg): number | undefined {
  const raw = org.parentId
  if (raw == null) return undefined
  const n = typeof raw === 'number' ? raw : Number(raw)
  if (Number.isNaN(n) || n === 0) return undefined
  return n
}

/** 将嵌套或扁平的组织列表统一拍平（不含 children） */
export function flattenOrgNodes(orgs: BaseOrg[]): BaseOrg[] {
  const out: BaseOrg[] = []
  function walk(nodes: BaseOrg[]) {
    for (const node of nodes) {
      const { children, ...rest } = node
      out.push(rest)
      if (children?.length) walk(children)
    }
  }
  walk(orgs)
  return out
}

function sortOrgSiblings(nodes: BaseOrg[]) {
  nodes.sort((a, b) => {
    const sortDiff = (a.sort ?? 0) - (b.sort ?? 0)
    if (sortDiff !== 0) return sortDiff
    const idA = orgRowId(a) ?? 0
    const idB = orgRowId(b) ?? 0
    return idA - idB
  })
  for (const node of nodes) {
    if (node.children?.length) sortOrgSiblings(node.children)
  }
}

/** 按 parentId 将扁平组织列表组装为树（兼容后端 flat / nested 两种返回） */
export function buildOrgTree(orgs: BaseOrg[]): BaseOrg[] {
  if (!orgs.length) return []

  const flat = flattenOrgNodes(orgs)
  const byId = new Map<number, BaseOrg>()
  for (const org of flat) {
    const id = orgRowId(org)
    if (id == null) continue
    byId.set(id, { ...org, children: [] })
  }

  const roots: BaseOrg[] = []
  for (const org of byId.values()) {
    const parentId = orgParentId(org)
    const parent = parentId != null ? byId.get(parentId) : undefined
    if (parent) {
      parent.children!.push(org)
    } else {
      roots.push(org)
    }
  }

  sortOrgSiblings(roots)
  return roots
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

export function findOrgInTree(id: number, nodes: BaseOrg[] = orgTree.value): BaseOrg | undefined {
  for (const node of nodes) {
    const nodeId = orgRowId(node)
    if (nodeId === id) return node
    if (node.children?.length) {
      const found = findOrgInTree(id, node.children)
      if (found) return found
    }
  }
  return undefined
}

export function collectDescendantIds(org: BaseOrg): number[] {
  const ids: number[] = []
  function walk(node: BaseOrg) {
    const id = orgRowId(node)
    if (id != null) ids.push(id)
    node.children?.forEach(walk)
  }
  walk(org)
  return ids
}

export function orgMatchesKeyword(org: BaseOrg, keyword: string) {
  const kw = keyword.trim().toLowerCase()
  if (!kw) return true
  return (
    (org.orgName ?? '').toLowerCase().includes(kw) ||
    (org.orgCode ?? '').toLowerCase().includes(kw) ||
    String(orgRowId(org) ?? '').includes(kw)
  )
}

/** 搜索时返回应可见的节点 ID（含匹配项及其祖先） */
export function collectVisibleOrgIds(nodes: BaseOrg[], keyword: string): Set<number> | null {
  const kw = keyword.trim()
  if (!kw) return null

  const visible = new Set<number>()

  function walk(list: BaseOrg[], ancestors: number[]): boolean {
    let branchMatch = false
    for (const node of list) {
      const id = orgRowId(node)
      if (id == null) continue
      const childMatch = node.children?.length ? walk(node.children, [...ancestors, id]) : false
      const selfMatch = orgMatchesKeyword(node, kw)
      if (selfMatch || childMatch) {
        branchMatch = true
        visible.add(id)
        ancestors.forEach((aid) => visible.add(aid))
      }
    }
    return branchMatch
  }

  walk(nodes, [])
  return visible
}

export function isDefaultOrg(org: BaseOrg) {
  const id = orgRowId(org)
  return id === 1 || org.orgName === '默认组织'
}

export function useOrgTree() {
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
      const raw = await listOrgTree()
      orgTree.value = buildOrgTree(raw ?? [])
      flatOrgs.value = flattenOrgs(orgTree.value)
    } catch {
      orgTree.value = []
      flatOrgs.value = []
    } finally {
      loading.value = false
    }
  }

  const orgTotal = computed(() => flatOrgs.value.length)

  return {
    orgTree,
    flatOrgs,
    orgTotal,
    loading,
    orgNameMap,
    orgLabel,
    loadOrgs,
    orgRowId,
    orgOptionLabel,
    findOrgInTree,
    collectDescendantIds,
    collectVisibleOrgIds,
    isDefaultOrg,
    buildOrgTree,
    flattenOrgNodes,
  }
}
