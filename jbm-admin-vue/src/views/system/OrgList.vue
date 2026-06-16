<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  Plus,
  Pencil,
  Trash2,
  RefreshCw,
  ChevronRight,
  ChevronDown,
  FolderTree,
  List,
} from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import OrgTreeSelect from '@/components/OrgTreeSelect.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardContent from '@/components/ui/CardContent.vue'
import { useCrudForm } from '@/composables/useCrudForm'
import { usePagedList } from '@/composables/usePagedList'
import { useFeedback } from '@/composables/useFeedback'
import {
  useOrgTree,
  orgRowId,
  sameOrgId,
  collectVisibleOrgIds,
  collectDescendantIds,
  collectNodesWithChildren,
  findOrgInNodes,
  orgMatchesKeyword,
  isDefaultOrg,
} from '@/composables/useOrgTree'
import type { OrgIdValue } from '@/composables/useOrgTree'
import { pageOrgs, saveOrg, deleteOrg } from '@/api/org'
import type { BaseOrg } from '@/api/types'

type ViewMode = 'tree' | 'list'

const viewMode = ref<ViewMode>('tree')
const feedback = useFeedback()
const keyword = ref('')
const rootKeyword = ref('')
const treeKeyword = ref('')
const selectedOrgId = ref<string | undefined>()
const expandedIds = ref<Set<string>>(new Set())
const parentLocked = ref(false)

const {
  rootOrgs,
  selectedRootId,
  subTree,
  orgTotal,
  loading: treeLoading,
  subTreeLoading,
  orgLabel,
  loadRootOrgs,
  loadSubTree,
  loadOrgs,
} = useOrgTree()

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseOrg>(
  (p, s) => pageOrgs(p, s, keyword.value || undefined),
)

const filteredRootOrgs = computed(() => {
  const kw = rootKeyword.value.trim()
  if (!kw) return rootOrgs.value
  return rootOrgs.value.filter((o) => orgMatchesKeyword(o, kw))
})

/** 根组织 + 全部后代，用于右侧完整组织树 */
const displayTree = computed((): BaseOrg[] => {
  const root = rootOrgs.value.find((o) => sameOrgId(orgRowId(o), selectedRootId.value))
  if (!root) return []
  return [{ ...root, children: subTree.value }]
})

function findInDisplayTree(id: OrgIdValue) {
  return findOrgInNodes(id, displayTree.value)
}

const visibleOrgIds = computed(() => collectVisibleOrgIds(displayTree.value, treeKeyword.value))

type TreeRow = { org: BaseOrg; depth: number; hasChildren: boolean; expanded: boolean }

const treeRows = computed((): TreeRow[] => {
  const rows: TreeRow[] = []
  const searchActive = !!treeKeyword.value.trim()

  function walk(nodes: BaseOrg[], depth: number) {
    for (const org of nodes) {
      const id = orgRowId(org)
      if (id == null) continue
      if (visibleOrgIds.value && !visibleOrgIds.value.has(id)) continue

      const hasChildren = !!(org.children?.length)
      const expanded = searchActive || expandedIds.value.has(id)
      rows.push({ org, depth, hasChildren, expanded })
      if (hasChildren && expanded) {
        walk(org.children!, depth + 1)
      }
    }
  }

  walk(displayTree.value, 0)
  return rows
})

const selectedOrg = computed(() =>
  selectedOrgId.value != null ? findInDisplayTree(selectedOrgId.value) : undefined,
)

const selectedRootOrg = computed(() =>
  selectedRootId.value != null
    ? rootOrgs.value.find((o) => sameOrgId(orgRowId(o), selectedRootId.value))
    : undefined,
)

const directChildCount = computed(() => selectedOrg.value?.children?.length ?? 0)

const selectedOrgForEdit = ref<BaseOrg | null>(null)

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseOrg>(() => ({
  orgName: '',
  parentId: undefined,
  sort: 0,
  status: 1,
}))

const parentExcludeIds = computed(() => {
  if (!editing.value || selectedOrgForEdit.value == null) return []
  const id = orgRowId(selectedOrgForEdit.value)
  if (id == null) return []
  const full = findInDisplayTree(id) ?? selectedOrgForEdit.value
  return collectDescendantIds(full)
})

function search() {
  load(1)
}

function ensureRootSelection() {
  if (selectedRootId.value != null && rootOrgs.value.some((o) => sameOrgId(orgRowId(o), selectedRootId.value))) {
    return
  }
  const first = rootOrgs.value[0]
  if (first) {
    void selectRoot(first)
  } else {
    selectedRootId.value = undefined
    selectedOrgId.value = undefined
  }
}

function expandAncestors(id: OrgIdValue) {
  const next = new Set(expandedIds.value)
  let current = findInDisplayTree(id)
  while (current) {
    const currentId = orgRowId(current)
    if (currentId == null) break
    next.add(currentId)
    if (current.parentId == null) break
    current = findInDisplayTree(current.parentId)
  }
  expandedIds.value = next
}

function expandAllBranches() {
  expandedIds.value = collectNodesWithChildren(displayTree.value)
  const rootId = selectedRootId.value
  if (rootId != null) expandedIds.value.add(rootId)
}

async function selectRoot(org: BaseOrg) {
  const id = orgRowId(org)
  if (id == null) return
  selectedOrgId.value = id
  if (selectedRootId.value !== id) {
    await loadSubTree(id)
  }
  expandAllBranches()
}

function selectTreeNode(org: BaseOrg) {
  const id = orgRowId(org)
  if (id == null) return
  selectedOrgId.value = id
  expandAncestors(id)
}

function toggleExpand(id: string, event?: Event) {
  event?.stopPropagation()
  const next = new Set(expandedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedIds.value = next
}

async function refreshTree() {
  const keepRootId = selectedRootId.value
  const keepOrgId = selectedOrgId.value
  await Promise.all([loadRootOrgs(keepRootId), loadOrgs()])
  if (keepOrgId != null && findInDisplayTree(keepOrgId)) {
    selectedOrgId.value = keepOrgId
    expandAncestors(keepOrgId)
  } else if (selectedRootId.value != null) {
    selectedOrgId.value = selectedRootId.value
    expandAllBranches()
  } else {
    ensureRootSelection()
  }
}

async function refreshAll() {
  await refreshTree()
  if (viewMode.value === 'list') {
    await load(page.value)
  }
}

function openCreateRoot() {
  parentLocked.value = false
  selectedOrgForEdit.value = null
  openCreate()
  form.value.parentId = undefined
}

function openCreateChild(parent?: BaseOrg) {
  const target = parent ?? selectedOrg.value ?? selectedRootOrg.value
  parentLocked.value = !!target
  selectedOrgForEdit.value = null
  openCreate()
  form.value.parentId = target ? orgRowId(target) : undefined
}

function openEditOrg(row: BaseOrg) {
  parentLocked.value = false
  const id = orgRowId(row)
  selectedOrgForEdit.value = (id != null ? findInDisplayTree(id) : undefined) ?? row
  openEdit(selectedOrgForEdit.value)
}

async function handleSave() {
  if (!form.value.orgName?.trim()) {
    formError.value = '组织名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  const parentId = form.value.parentId ?? undefined
  try {
    const saved = await saveOrg({
      id: editing.value ? (form.value.id ?? form.value.orgId) : undefined,
      orgName: form.value.orgName,
      parentId,
      sort: form.value.sort,
      status: form.value.status,
    })
    closeDialog()
    const savedIdRaw = orgRowId(saved) ?? form.value.id ?? form.value.orgId
    const savedId = savedIdRaw != null ? String(savedIdRaw) : undefined
    await refreshTree()
    if (savedId != null) {
      selectedOrgId.value = savedId
      if (parentId == null) {
        selectedRootId.value = savedId
        await loadSubTree(savedId)
      }
      expandAncestors(savedId)
      expandAllBranches()
    }
    if (viewMode.value === 'list') {
      await load(page.value)
    }
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function deleteBlockedReason(org: BaseOrg) {
  if (isDefaultOrg(org)) return '默认组织为系统保留数据，不允许删除'
  const id = orgRowId(org)
  const full = id != null ? findInDisplayTree(id) ?? org : org
  if (full.children?.length) return '该组织下仍有子组织，请先删除或迁移子组织'
  return null
}

async function handleDelete(row: BaseOrg) {
  const id = orgRowId(row)
  if (!id) return
  const blocked = deleteBlockedReason(row)
  if (blocked) {
    feedback.toast.warning(blocked, '无法删除组织')
    return
  }
  const confirmed = await feedback.confirm({
    title: '确认删除组织',
    message: `确认删除组织「${row.orgName}」？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteOrg({ id })
  if (selectedOrgId.value != null && sameOrgId(selectedOrgId.value, id)) {
    selectedOrgId.value = selectedRootId.value
  }
  await refreshAll()
}

watch(viewMode, (mode) => {
  if (mode === 'list' && !items.value.length) {
    load(1)
  }
})

onMounted(async () => {
  await refreshTree()
})
</script>

<template>
  <div>
    <PageHeader
      title="组织管理"
      :description="`共 ${orgTotal} 个组织；左侧选择根组织，右侧维护完整组织树（支持多级子部门）`"
    >
      <template #actions>
        <div class="inline-flex rounded-md border bg-muted/30 p-0.5">
          <Button
            :variant="viewMode === 'tree' ? 'default' : 'ghost'"
            size="sm"
            class="h-8"
            @click="viewMode = 'tree'"
          >
            <FolderTree class="mr-1 h-4 w-4" />
            组织树
          </Button>
          <Button
            :variant="viewMode === 'list' ? 'default' : 'ghost'"
            size="sm"
            class="h-8"
            @click="viewMode = 'list'"
          >
            <List class="mr-1 h-4 w-4" />
            列表
          </Button>
        </div>

        <template v-if="viewMode === 'tree'">
          <Button variant="outline" @click="refreshTree">
            <RefreshCw class="mr-1 h-4 w-4" />
            刷新
          </Button>
          <Button @click="openCreateRoot">
            <Plus class="mr-1 h-4 w-4" />
            新建根组织
          </Button>
        </template>

        <template v-else>
          <Input
            v-model="keyword"
            placeholder="组织名称"
            class="w-40"
            @keyup.enter="search"
          />
          <Button variant="outline" @click="search">搜索</Button>
          <Button variant="outline" @click="load(page)">
            <RefreshCw class="mr-1 h-4 w-4" />
            刷新
          </Button>
          <Button @click="openCreateRoot">
            <Plus class="mr-1 h-4 w-4" />
            新建
          </Button>
        </template>
      </template>
    </PageHeader>

    <div v-if="viewMode === 'tree'" class="flex gap-4 min-h-[520px]">
      <aside class="w-72 shrink-0 flex flex-col rounded-lg border bg-card">
        <div class="border-b px-3 py-2 text-sm font-medium text-muted-foreground">
          根组织
        </div>
        <div class="border-b p-2">
          <Input v-model="rootKeyword" placeholder="搜索根组织" class="h-8" />
        </div>
        <div v-if="treeLoading && !rootOrgs.length" class="p-4 text-sm text-muted-foreground">
          加载中…
        </div>
        <div v-else-if="!filteredRootOrgs.length" class="p-4 text-sm text-muted-foreground">
          {{ rootKeyword ? '未找到匹配根组织' : '暂无根组织' }}
        </div>
        <ul v-else class="flex-1 max-h-[560px] overflow-y-auto py-1">
          <li
            v-for="root in filteredRootOrgs"
            :key="orgRowId(root)"
            class="flex cursor-pointer items-center gap-2 rounded-sm px-3 py-2 text-sm hover:bg-muted/60"
            :class="sameOrgId(orgRowId(root), selectedRootId) ? 'bg-primary/10 text-primary font-medium' : ''"
            @click="selectRoot(root)"
          >
            <span class="truncate flex-1">{{ root.orgName }}</span>
            <Badge
              v-if="root.status !== 1"
              variant="secondary"
              class="shrink-0 text-xs"
            >
              停用
            </Badge>
          </li>
        </ul>
      </aside>

      <main class="min-w-0 flex-1 space-y-4">
        <Card v-if="selectedOrg">
          <CardHeader class="flex flex-row items-start justify-between space-y-0 pb-3">
            <div>
              <CardTitle>{{ selectedOrg.orgName }}</CardTitle>
              <p class="mt-1 text-sm text-muted-foreground">
                ID {{ orgRowId(selectedOrg) }}
                <span v-if="selectedOrg.orgCode"> · 编码 {{ selectedOrg.orgCode }}</span>
              </p>
            </div>
            <div class="flex shrink-0 gap-2">
              <Button size="sm" @click="openCreateChild(selectedOrg)">
                <Plus class="mr-1 h-3.5 w-3.5" />
                新建子组织
              </Button>
              <Button variant="outline" size="sm" @click="openEditOrg(selectedOrg)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button
                variant="destructive"
                size="sm"
                :disabled="!!deleteBlockedReason(selectedOrg)"
                @click="handleDelete(selectedOrg)"
              >
                <Trash2 class="h-3.5 w-3.5" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <dl class="grid grid-cols-2 gap-x-6 gap-y-3 text-sm md:grid-cols-3">
              <div>
                <dt class="text-muted-foreground">父级组织</dt>
                <dd>{{ orgLabel(selectedOrg.parentId) }}</dd>
              </div>
              <div>
                <dt class="text-muted-foreground">排序</dt>
                <dd>{{ selectedOrg.sort ?? 0 }}</dd>
              </div>
              <div>
                <dt class="text-muted-foreground">状态</dt>
                <dd>
                  <Badge :variant="selectedOrg.status === 1 ? 'default' : 'secondary'">
                    {{ selectedOrg.status === 1 ? '正常' : '停用' }}
                  </Badge>
                </dd>
              </div>
              <div>
                <dt class="text-muted-foreground">直属子组织</dt>
                <dd>{{ directChildCount }} 个</dd>
              </div>
            </dl>
            <p
              v-if="deleteBlockedReason(selectedOrg)"
              class="mt-3 text-xs text-muted-foreground"
            >
              {{ deleteBlockedReason(selectedOrg) }}
            </p>
          </CardContent>
        </Card>

        <Card v-else>
          <CardContent class="py-10 text-center text-sm text-muted-foreground">
            请从左侧选择根组织，或新建根组织
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-3">
            <CardTitle class="text-base">组织树</CardTitle>
            <div class="flex items-center gap-2">
              <Input
                v-model="treeKeyword"
                placeholder="搜索组织"
                class="h-8 w-40"
              />
              <Button variant="outline" size="sm" class="h-8" @click="expandAllBranches">
                全部展开
              </Button>
              <span v-if="subTreeLoading" class="text-xs text-muted-foreground">加载中…</span>
            </div>
          </CardHeader>
          <CardContent class="p-0">
            <div v-if="!selectedRootId" class="px-4 py-8 text-center text-sm text-muted-foreground">
              请先选择根组织
            </div>
            <div
              v-else-if="!subTreeLoading && !treeRows.length"
              class="px-4 py-8 text-center text-sm text-muted-foreground"
            >
              {{ treeKeyword ? '未找到匹配组织' : '该根组织下暂无子组织' }}
            </div>
            <ul v-else class="max-h-[480px] overflow-y-auto border-t py-1">
              <li
                v-for="row in treeRows"
                :key="orgRowId(row.org)"
                class="group flex cursor-pointer items-center gap-1 rounded-sm px-3 py-1.5 text-sm hover:bg-muted/60"
                :class="sameOrgId(orgRowId(row.org), selectedOrgId) ? 'bg-primary/10 text-primary font-medium' : ''"
                :style="{ paddingLeft: `${row.depth * 16 + 12}px` }"
                @click="selectTreeNode(row.org)"
              >
                <button
                  v-if="row.hasChildren"
                  type="button"
                  class="inline-flex h-5 w-5 shrink-0 items-center justify-center rounded hover:bg-muted"
                  @click="toggleExpand(orgRowId(row.org)!, $event)"
                >
                  <ChevronDown v-if="row.expanded" class="h-3.5 w-3.5" />
                  <ChevronRight v-else class="h-3.5 w-3.5" />
                </button>
                <span v-else class="inline-block w-5 shrink-0" />
                <span class="truncate flex-1">{{ row.org.orgName }}</span>
                <div
                  class="flex shrink-0 items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100"
                  @click.stop
                >
                  <Button
                    variant="ghost"
                    size="sm"
                    class="h-7 w-7 p-0"
                    title="新建子组织"
                    @click="openCreateChild(row.org)"
                  >
                    <Plus class="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    class="h-7 w-7 p-0"
                    title="编辑"
                    @click="openEditOrg(row.org)"
                  >
                    <Pencil class="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    class="h-7 w-7 p-0 text-destructive hover:text-destructive"
                    title="删除"
                    :disabled="!!deleteBlockedReason(row.org)"
                    @click="handleDelete(row.org)"
                  >
                    <Trash2 class="h-3.5 w-3.5" />
                  </Button>
                </div>
                <Badge
                  v-if="row.org.status !== 1"
                  variant="secondary"
                  class="ml-1 shrink-0 text-xs"
                >
                  停用
                </Badge>
              </li>
            </ul>
          </CardContent>
        </Card>
      </main>
    </div>

    <DataTableShell v-else :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">组织名称</th>
            <th class="h-10 px-4 text-left font-medium">父级</th>
            <th class="h-10 px-4 text-left font-medium">排序</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="orgRowId(row)" class="border-b">
            <td class="p-4">{{ orgRowId(row) }}</td>
            <td class="p-4">{{ row.orgName }}</td>
            <td class="p-4">{{ orgLabel(row.parentId) }}</td>
            <td class="p-4">{{ row.sort }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '正常' : '停用' }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" title="新建子组织" @click="openCreateChild(row)">
                <Plus class="h-3.5 w-3.5" />
              </Button>
              <Button variant="outline" size="sm" @click="openEditOrg(row)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button variant="destructive" size="sm" @click="handleDelete(row)">
                <Trash2 class="h-3.5 w-3.5" />
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑组织' : parentLocked ? '新建子组织' : '新建组织'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="组织名称" required>
        <Input v-model="form.orgName" />
      </FormField>
      <FormField label="父级组织">
        <OrgTreeSelect
          v-model="form.parentId"
          placeholder="根组织留空"
          :disabled="parentLocked"
          :exclude-ids="parentExcludeIds"
        />
      </FormField>
      <FormField label="排序">
        <Input v-model="form.sort" type="number" />
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">正常</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
    </CrudDialog>
  </div>
</template>
