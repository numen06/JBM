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
  collectVisibleOrgIds,
  collectDescendantIds,
  findOrgInTree,
  isDefaultOrg,
} from '@/composables/useOrgTree'
import { pageOrgs, saveOrg, deleteOrg } from '@/api/org'
import type { BaseOrg } from '@/api/types'

type ViewMode = 'tree' | 'list'

const viewMode = ref<ViewMode>('tree')
const feedback = useFeedback()
const keyword = ref('')
const treeKeyword = ref('')
const selectedOrgId = ref<number | undefined>()
const expandedIds = ref<Set<number>>(new Set())
const parentLocked = ref(false)

const {
  orgTree,
  loading: treeLoading,
  orgLabel,
  loadOrgs,
} = useOrgTree()

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseOrg>(
  (p, s) => pageOrgs(p, s, keyword.value || undefined),
)

const visibleOrgIds = computed(() => collectVisibleOrgIds(orgTree.value, treeKeyword.value))

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

  walk(orgTree.value, 0)
  return rows
})

const selectedOrg = computed(() =>
  selectedOrgId.value != null ? findOrgInTree(selectedOrgId.value) : undefined,
)

const childOrgs = computed(() => selectedOrg.value?.children ?? [])

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
  const org = selectedOrgForEdit.value
  return collectDescendantIds(org)
})

function search() {
  load(1)
}

function ensureTreeSelection() {
  if (selectedOrgId.value != null && findOrgInTree(selectedOrgId.value)) return
  const first = orgTree.value[0]
  if (first) {
    selectOrg(first)
  } else {
    selectedOrgId.value = undefined
  }
}

function expandAncestors(id: number) {
  const next = new Set(expandedIds.value)
  let current = findOrgInTree(id)
  while (current?.parentId != null) {
    next.add(current.parentId)
    current = findOrgInTree(current.parentId)
  }
  expandedIds.value = next
}

function selectOrg(org: BaseOrg) {
  const id = orgRowId(org)
  if (id == null) return
  selectedOrgId.value = id
  expandAncestors(id)
}

function toggleExpand(id: number, event?: Event) {
  event?.stopPropagation()
  const next = new Set(expandedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedIds.value = next
}

function expandAllRoots() {
  const next = new Set<number>()
  for (const org of orgTree.value) {
    const id = orgRowId(org)
    if (id != null) next.add(id)
  }
  expandedIds.value = next
}

async function refreshTree() {
  await loadOrgs()
  ensureTreeSelection()
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
  const target = parent ?? selectedOrg.value
  parentLocked.value = !!target
  selectedOrgForEdit.value = null
  openCreate()
  form.value.parentId = target ? orgRowId(target) : undefined
}

function openEditOrg(row: BaseOrg) {
  parentLocked.value = false
  selectedOrgForEdit.value = row
  openEdit(row)
}

async function handleSave() {
  if (!form.value.orgName?.trim()) {
    formError.value = '组织名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const saved = await saveOrg({
      id: editing.value ? (form.value.id ?? form.value.orgId) : undefined,
      orgName: form.value.orgName,
      parentId: form.value.parentId ?? undefined,
      sort: form.value.sort,
      status: form.value.status,
    })
    closeDialog()
    await refreshTree()
    const savedId = orgRowId(saved) ?? form.value.id ?? form.value.orgId
    if (savedId != null) {
      selectedOrgId.value = savedId
      expandAncestors(savedId)
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
  if (org.children?.length) return '该组织下仍有子组织，请先删除或迁移子组织'
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
  if (selectedOrgId.value === id) {
    selectedOrgId.value = undefined
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
  expandAllRoots()
})
</script>

<template>
  <div>
    <PageHeader
      title="组织管理"
      description="默认以组织树管理；列表视图用于搜索与分页维护全部组织"
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
          <Input
            v-model="treeKeyword"
            placeholder="搜索组织"
            class="w-40"
          />
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

    <!-- 组织树视图 -->
    <div v-if="viewMode === 'tree'" class="flex gap-4 min-h-[520px]">
      <aside class="w-72 shrink-0 rounded-lg border bg-card">
        <div class="border-b px-3 py-2 text-sm font-medium text-muted-foreground">
          组织树
        </div>
        <div v-if="treeLoading" class="p-4 text-sm text-muted-foreground">加载中…</div>
        <div v-else-if="!treeRows.length" class="p-4 text-sm text-muted-foreground">
          {{ treeKeyword ? '未找到匹配组织' : '暂无组织数据' }}
        </div>
        <ul v-else class="max-h-[560px] overflow-y-auto py-1">
          <li
            v-for="row in treeRows"
            :key="orgRowId(row.org)"
            class="flex cursor-pointer items-center gap-1 rounded-sm px-2 py-1.5 text-sm hover:bg-muted/60"
            :class="orgRowId(row.org) === selectedOrgId ? 'bg-primary/10 text-primary font-medium' : ''"
            :style="{ paddingLeft: `${row.depth * 16 + 8}px` }"
            @click="selectOrg(row.org)"
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
            <Badge
              v-if="row.org.status !== 1"
              variant="secondary"
              class="ml-1 shrink-0 text-xs"
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
                <dd>{{ childOrgs.length }} 个</dd>
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
            请从左侧选择组织，或新建根组织
          </CardContent>
        </Card>

        <Card>
          <CardHeader class="pb-3">
            <CardTitle class="text-base">直属子组织</CardTitle>
          </CardHeader>
          <CardContent class="p-0">
            <DataTableShell :loading="treeLoading" :empty="!childOrgs.length">
              <Table>
                <thead>
                  <tr class="border-b bg-muted/50">
                    <th class="h-10 px-4 text-left font-medium">名称</th>
                    <th class="h-10 px-4 text-left font-medium">编码</th>
                    <th class="h-10 px-4 text-left font-medium">排序</th>
                    <th class="h-10 px-4 text-left font-medium">状态</th>
                    <th class="h-10 px-4 text-right font-medium">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="child in childOrgs"
                    :key="orgRowId(child)"
                    class="border-b cursor-pointer hover:bg-muted/30"
                    @click="selectOrg(child)"
                  >
                    <td class="p-4">{{ child.orgName }}</td>
                    <td class="p-4">{{ child.orgCode || '—' }}</td>
                    <td class="p-4">{{ child.sort ?? 0 }}</td>
                    <td class="p-4">
                      <Badge :variant="child.status === 1 ? 'default' : 'secondary'">
                        {{ child.status === 1 ? '正常' : '停用' }}
                      </Badge>
                    </td>
                    <td class="p-4 text-right space-x-1" @click.stop>
                      <Button variant="outline" size="sm" @click="openEditOrg(child)">
                        <Pencil class="h-3.5 w-3.5" />
                      </Button>
                      <Button variant="destructive" size="sm" @click="handleDelete(child)">
                        <Trash2 class="h-3.5 w-3.5" />
                      </Button>
                    </td>
                  </tr>
                </tbody>
              </Table>
            </DataTableShell>
          </CardContent>
        </Card>
      </main>
    </div>

    <!-- 列表视图 -->
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
