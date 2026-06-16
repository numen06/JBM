import { computed, ref } from 'vue'
import { listOrgRoots, listOrgTree } from '@/api/org'
import type { BaseOrg } from '@/api/types'
import {
  isBlankSnowflakeId,
  sameSnowflakeId,
  toSnowflakeIdString,
  type SnowflakeId,
} from '@/lib/snowflakeId'

export type FlatOrg = BaseOrg & { depth: number }
export type OrgIdValue = SnowflakeId

/** 组织行主键；禁止对返回值使用 Number() */
export function orgRowId(row: BaseOrg): string | undefined {
  const raw = row.id ?? row.orgId
  if (isBlankSnowflakeId(raw)) return undefined
  return toSnowflakeIdString(raw)
}

export const sameOrgId = sameSnowflakeId

function orgParentId(org: BaseOrg): string | undefined {
  const raw = org.parentId
  if (raw == null || raw === '' || raw === 0 || raw === '0') return undefined
  return String(raw)
}

function compareOrgId(a: string, b: string) {
  return a.localeCompare(b, undefined, { numeric: true })
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
    const idA = orgRowId(a) ?? ''
    const idB = orgRowId(b) ?? ''
    return compareOrgId(idA, idB)
  })
  for (const node of nodes) {
    if (node.children?.length) sortOrgSiblings(node.children)
  }
}

/** 按 parentId 将扁平组织列表组装为树 */
export function buildOrgTree(orgs: BaseOrg[]): BaseOrg[] {
  if (!orgs.length) return []

  const flat = flattenOrgNodes(orgs)
  const byId = new Map<string, BaseOrg>()
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

/** 始终按 parentId 重建树 */
export function normalizeOrgTreeResponse(raw: BaseOrg[] | null | undefined): BaseOrg[] {
  if (!raw?.length) return []
  return buildOrgTree(raw)
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

export function findOrgInNodes(id: OrgIdValue, nodes: BaseOrg[]): BaseOrg | undefined {
  const target = String(id)
  for (const node of nodes) {
    const nodeId = orgRowId(node)
    if (nodeId === target) return node
    if (node.children?.length) {
      const found = findOrgInNodes(id, node.children)
      if (found) return found
    }
  }
  return undefined
}

export function collectNodesWithChildren(nodes: BaseOrg[], out = new Set<string>()) {
  for (const node of nodes) {
    const id = orgRowId(node)
    if (id != null && node.children?.length) {
      out.add(id)
      collectNodesWithChildren(node.children, out)
    }
  }
  return out
}

export function collectDescendantIds(org: BaseOrg): string[] {
  const ids: string[] = []
  function walk(node: BaseOrg) {
    const id = orgRowId(node)
    if (id != null) ids.push(id)
    node.children?.forEach(walk)
  }
  walk(org)
  return ids
}

export function collectAllOrgIds(nodes: BaseOrg[]): string[] {
  const ids: string[] = []
  function walk(list: BaseOrg[]) {
    for (const node of list) {
      const id = orgRowId(node)
      if (id != null) ids.push(id)
      if (node.children?.length) walk(node.children)
    }
  }
  walk(nodes)
  return ids
}

export function countDirectChildren(org: BaseOrg): number {
  return org.children?.length ?? 0
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
export function collectVisibleOrgIds(nodes: BaseOrg[], keyword: string): Set<string> | null {
  const kw = keyword.trim()
  if (!kw) return null

  const visible = new Set<string>()

  function walk(list: BaseOrg[], ancestors: string[]): boolean {
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
  return id === '1' || org.orgName === '默认组织'
}

export function useOrgTree() {
  const orgTree = ref<BaseOrg[]>([])
  const flatOrgs = ref<FlatOrg[]>([])
  const rootOrgs = ref<BaseOrg[]>([])
  const selectedRootId = ref<string | undefined>()
  const subTree = ref<BaseOrg[]>([])
  const loading = ref(false)
  const subTreeLoading = ref(false)

  const orgNameMap = computed(() => {
    const m = new Map<string, string>()
    for (const o of flatOrgs.value) {
      const id = orgRowId(o)
      if (id != null) m.set(id, o.orgName ?? '')
    }
    for (const root of rootOrgs.value) {
      const id = orgRowId(root)
      if (id != null && root.orgName) m.set(id, root.orgName)
    }
    for (const flat of flattenOrgs(subTree.value)) {
      const id = orgRowId(flat)
      if (id != null && flat.orgName) m.set(id, flat.orgName)
    }
    return m
  })

  const orgTotal = computed(() => flatOrgs.value.length || flattenOrgs(subTree.value).length + rootOrgs.value.length)

  function orgLabel(id?: OrgIdValue) {
    if (id == null || id === '') return '—'
    return orgNameMap.value.get(String(id)) ?? String(id)
  }

  function findOrgInSubTree(id: OrgIdValue, nodes: BaseOrg[] = subTree.value): BaseOrg | undefined {
    return findOrgInNodes(id, nodes)
  }

  function findOrgInTree(id: OrgIdValue): BaseOrg | undefined {
    const root = rootOrgs.value.find((o) => sameOrgId(orgRowId(o), id))
    if (root) return root
    return findOrgInSubTree(id)
  }

  function findOrgInFullTree(id: OrgIdValue, nodes: BaseOrg[] = orgTree.value): BaseOrg | undefined {
    return findOrgInNodes(id, nodes)
  }

  async function loadOrgs() {
    loading.value = true
    try {
      const raw = await listOrgTree()
      orgTree.value = normalizeOrgTreeResponse(raw)
      flatOrgs.value = flattenOrgs(orgTree.value)
    } catch {
      orgTree.value = []
      flatOrgs.value = []
    } finally {
      loading.value = false
    }
  }

  async function loadSubTree(rootId: OrgIdValue) {
    subTreeLoading.value = true
    try {
      const raw = await listOrgTree(rootId)
      subTree.value = normalizeOrgTreeResponse(raw)
      selectedRootId.value = String(rootId)
    } catch {
      subTree.value = []
    } finally {
      subTreeLoading.value = false
    }
  }

  async function loadRootOrgs(preferredRootId?: OrgIdValue) {
    loading.value = true
    try {
      rootOrgs.value = (await listOrgRoots()) ?? []
      const rootIds = rootOrgs.value
        .map(orgRowId)
        .filter((id): id is string => id != null)
      const keep = preferredRootId != null ? String(preferredRootId) : selectedRootId.value
      if (keep != null && rootIds.includes(keep)) {
        selectedRootId.value = keep
      } else if (rootIds.length) {
        selectedRootId.value = rootIds[0]
      } else {
        selectedRootId.value = undefined
        subTree.value = []
        return
      }
      await loadSubTree(selectedRootId.value)
    } catch {
      rootOrgs.value = []
      selectedRootId.value = undefined
      subTree.value = []
    } finally {
      loading.value = false
    }
  }

  return {
    orgTree,
    flatOrgs,
    rootOrgs,
    selectedRootId,
    subTree,
    orgTotal,
    loading,
    subTreeLoading,
    orgNameMap,
    orgLabel,
    loadOrgs,
    loadRootOrgs,
    loadSubTree,
    orgRowId,
    sameOrgId,
    orgOptionLabel,
    findOrgInTree,
    findOrgInFullTree,
    findOrgInSubTree,
    collectDescendantIds,
    collectVisibleOrgIds,
    isDefaultOrg,
    buildOrgTree,
    flattenOrgNodes,
    normalizeOrgTreeResponse,
  }
}
