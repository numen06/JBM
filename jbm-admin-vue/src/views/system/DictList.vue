<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Plus, Pencil, Trash2, RefreshCw, FolderOpen, Search } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import { useCrudForm } from '@/composables/useCrudForm'
import { pageRootDicts, pageDictItems, saveDict, deleteDict } from '@/api/dict'
import { usePermission } from '@/composables/usePermission'
import type { BaseDic } from '@/api/types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

const { hasAction } = usePermission()

type DialogMode = 'group' | 'item'

const GROUP_PAGE_SIZE = DEFAULT_PAGE_SIZE
const ITEM_PAGE_SIZE = DEFAULT_PAGE_SIZE

function dicId(row: BaseDic): number | string | undefined {
  const raw = row.id ?? row.dicId
  if (raw == null || raw === '') return undefined
  return raw
}

function dicCode(row: BaseDic) {
  return row.code ?? row.dicCode ?? ''
}

function dicName(row: BaseDic) {
  return row.name ?? row.dicName ?? ''
}

function dicRemark(row: BaseDic) {
  return row.remark ?? row.dicValue ?? ''
}

const groups = ref<BaseDic[]>([])
const groupTotal = ref(0)
const groupPage = ref(1)
const groupsLoading = ref(false)
const groupsError = ref('')
const groupKeyword = ref('')
const groupKeywordApplied = ref('')

const items = ref<BaseDic[]>([])
const itemTotal = ref(0)
const itemPage = ref(1)
const itemsLoading = ref(false)
const itemsError = ref('')
const itemKeyword = ref('')
const itemKeywordApplied = ref('')

const selectedGroup = ref<BaseDic | null>(null)
const dialogMode = ref<DialogMode>('group')

let groupSearchTimer: ReturnType<typeof setTimeout> | undefined
let itemSearchTimer: ReturnType<typeof setTimeout> | undefined

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate: openCrudCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseDic>(() => ({
  code: '',
  name: '',
  remark: '',
  parentId: undefined,
}))

const dialogTitle = computed(() => {
  if (dialogMode.value === 'group') {
    return editing.value ? '编辑字典分组' : '新建字典分组'
  }
  return editing.value ? '编辑字典项' : '新建字典项'
})

async function loadGroups(page = groupPage.value) {
  groupsLoading.value = true
  groupsError.value = ''
  try {
    const data = await pageRootDicts(page, GROUP_PAGE_SIZE, groupKeywordApplied.value || undefined)
    groups.value = data.contents ?? []
    groupTotal.value = data.total ?? 0
    groupPage.value = page

    if (selectedGroup.value) {
      const id = dicId(selectedGroup.value)
      const still = groups.value.find((g) => dicId(g) === id)
      if (still) {
        selectedGroup.value = still
      } else if (groups.value.length) {
        selectedGroup.value = groups.value[0]
      } else {
        selectedGroup.value = null
      }
    } else if (groups.value.length) {
      selectedGroup.value = groups.value[0]
    }
  } catch (e) {
    groupsError.value = e instanceof Error ? e.message : '加载分组失败'
    groups.value = []
    groupTotal.value = 0
  } finally {
    groupsLoading.value = false
  }
}

async function loadItems(page = itemPage.value) {
  const g = selectedGroup.value
  if (!g || !dicId(g)) {
    items.value = []
    itemTotal.value = 0
    return
  }
  itemsLoading.value = true
  itemsError.value = ''
  try {
    const data = await pageDictItems(
      dicId(g)!,
      page,
      ITEM_PAGE_SIZE,
      itemKeywordApplied.value || undefined,
    )
    items.value = data.contents ?? []
    itemTotal.value = data.total ?? 0
    itemPage.value = page
  } catch (e) {
    itemsError.value = e instanceof Error ? e.message : '加载字典项失败'
    items.value = []
    itemTotal.value = 0
  } finally {
    itemsLoading.value = false
  }
}

async function refreshAll() {
  await loadGroups(groupPage.value)
  if (selectedGroup.value) {
    await loadItems(itemPage.value)
  }
}

function applyGroupSearch() {
  groupKeywordApplied.value = groupKeyword.value.trim()
  loadGroups(1)
}

function applyItemSearch() {
  itemKeywordApplied.value = itemKeyword.value.trim()
  loadItems(1)
}

function onGroupPageChange(p: number) {
  loadGroups(p)
}

function onItemPageChange(p: number) {
  loadItems(p)
}

function selectGroup(g: BaseDic) {
  selectedGroup.value = g
}

function openCreateGroup() {
  dialogMode.value = 'group'
  openCrudCreate()
  form.value = { code: '', name: '', remark: '', parentId: undefined }
}

function openEditGroup(g: BaseDic) {
  dialogMode.value = 'group'
  openEdit({
    id: dicId(g),
    code: dicCode(g),
    name: dicName(g),
    remark: dicRemark(g),
    parentId: undefined,
  })
}

function openCreateItem() {
  if (!selectedGroup.value) return
  dialogMode.value = 'item'
  openCrudCreate()
  form.value = {
    code: '',
    name: '',
    remark: '',
    parentId: dicId(selectedGroup.value),
  }
}

function openEditItem(row: BaseDic) {
  dialogMode.value = 'item'
  openEdit({
    id: dicId(row),
    code: dicCode(row),
    name: dicName(row),
    remark: dicRemark(row),
    parentId: row.parentId ?? dicId(selectedGroup.value!),
  })
}

async function handleSave() {
  if (!form.value.code?.trim() || !form.value.name?.trim()) {
    formError.value = '编码和名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const payload: Partial<BaseDic> = {
      id: editing.value ? (form.value.id ?? form.value.dicId) : undefined,
      code: form.value.code?.trim(),
      name: form.value.name?.trim(),
      remark: form.value.remark?.trim() || undefined,
    }
    if (dialogMode.value === 'item') {
      payload.parentId = form.value.parentId ?? dicId(selectedGroup.value!)
    } else {
      payload.parentId = undefined
    }
    await saveDict(payload)
    closeDialog()
    await refreshAll()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDeleteGroup(g: BaseDic) {
  const id = dicId(g)
  if (!id) return
  let childCount = 0
  try {
    const peek = await pageDictItems(id, 1, 1)
    childCount = peek.total ?? 0
  } catch {
    /* ignore */
  }
  const msg =
    childCount > 0
      ? `分组「${dicName(g)}」下还有 ${childCount} 个字典项，确认删除分组？`
      : `确认删除分组「${dicName(g)}」？`
  if (!confirm(msg)) return
  await deleteDict({ id })
  if (selectedGroup.value && dicId(selectedGroup.value) === id) {
    selectedGroup.value = null
    items.value = []
    itemTotal.value = 0
  }
  await loadGroups(1)
}

async function handleDeleteItem(row: BaseDic) {
  const id = dicId(row)
  if (!id || !confirm(`确认删除字典项「${dicName(row)}」？`)) return
  await deleteDict({ id })
  await loadItems(itemPage.value)
}

watch(selectedGroup, (g) => {
  itemKeyword.value = ''
  itemKeywordApplied.value = ''
  itemPage.value = 1
  if (g) loadItems(1)
  else {
    items.value = []
    itemTotal.value = 0
  }
})

watch(groupKeyword, () => {
  clearTimeout(groupSearchTimer)
  groupSearchTimer = setTimeout(applyGroupSearch, 400)
})

watch(itemKeyword, () => {
  if (!selectedGroup.value) return
  clearTimeout(itemSearchTimer)
  itemSearchTimer = setTimeout(applyItemSearch, 400)
})

onMounted(() => loadGroups(1))
</script>

<template>
  <div>
    <PageHeader title="字典管理" description="分组 + 字典项，支持分页与关键词过滤">
      <template #actions>
        <Button variant="outline" size="sm" :disabled="groupsLoading || itemsLoading" @click="refreshAll">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <div class="flex flex-col gap-4 lg:flex-row lg:items-stretch">
      <!-- 左侧：字典分组 -->
      <aside class="flex w-full shrink-0 flex-col rounded-lg border bg-card lg:w-80">
        <div class="flex items-center justify-between border-b px-3 py-2">
          <span class="text-sm font-medium">字典分组</span>
          <Button
            v-if="hasAction('dict_add')"
            variant="outline"
            size="sm"
            @click="openCreateGroup"
          >
            <Plus class="h-3.5 w-3.5" />
          </Button>
        </div>
        <div class="flex gap-1 border-b px-3 py-2">
          <Input
            v-model="groupKeyword"
            placeholder="编码/名称"
            class="h-8 flex-1 text-sm"
            @keyup.enter="applyGroupSearch"
          />
          <Button variant="outline" size="sm" class="h-8 px-2" @click="applyGroupSearch">
            <Search class="h-3.5 w-3.5" />
          </Button>
        </div>
        <div class="min-h-[240px] flex-1 overflow-y-auto p-2">
          <p v-if="groupsLoading" class="px-2 py-4 text-sm text-muted-foreground">加载中…</p>
          <p v-else-if="groupsError" class="px-2 py-4 text-sm text-destructive">{{ groupsError }}</p>
          <p v-else-if="!groups.length" class="px-2 py-4 text-sm text-muted-foreground">
            {{ groupKeywordApplied ? '无匹配分组' : '暂无分组' }}
          </p>
          <ul v-else class="space-y-0.5">
            <li
              v-for="g in groups"
              :key="dicId(g)"
              class="group flex cursor-pointer items-center gap-2 rounded-md px-2 py-2 text-sm transition-colors hover:bg-muted"
              :class="
                selectedGroup && dicId(selectedGroup) === dicId(g)
                  ? 'bg-primary/10 font-medium text-primary'
                  : ''
              "
              @click="selectGroup(g)"
            >
              <FolderOpen class="h-4 w-4 shrink-0 opacity-60" />
              <div class="min-w-0 flex-1">
                <div class="truncate">{{ dicName(g) }}</div>
                <div class="truncate font-mono text-xs text-muted-foreground">{{ dicCode(g) }}</div>
              </div>
              <div
                class="flex shrink-0 gap-0.5 opacity-0 group-hover:opacity-100"
                :class="selectedGroup && dicId(selectedGroup) === dicId(g) ? 'opacity-100' : ''"
                @click.stop
              >
                <Button
                  v-if="hasAction('dict_edit')"
                  variant="ghost"
                  size="sm"
                  class="h-7 w-7 p-0"
                  @click="openEditGroup(g)"
                >
                  <Pencil class="h-3.5 w-3.5" />
                </Button>
                <Button
                  v-if="hasAction('dict_delete')"
                  variant="ghost"
                  size="sm"
                  class="h-7 w-7 p-0 text-destructive"
                  @click="handleDeleteGroup(g)"
                >
                  <Trash2 class="h-3.5 w-3.5" />
                </Button>
              </div>
            </li>
          </ul>
        </div>
        <PaginationBar
          v-if="groupTotal > 0"
          class="text-xs"
          :page="groupPage"
          :total="groupTotal"
          :page-size="GROUP_PAGE_SIZE"
          @change="onGroupPageChange"
        />
      </aside>

      <!-- 右侧：字典项 -->
      <section class="min-w-0 flex-1">
        <div
          v-if="!selectedGroup"
          class="rounded-lg border bg-muted/30 px-6 py-16 text-center text-muted-foreground"
        >
          请从左侧选择一个字典分组
        </div>
        <template v-else>
          <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 class="text-lg font-semibold">{{ dicName(selectedGroup) }}</h2>
              <p class="font-mono text-sm text-muted-foreground">
                {{ dicCode(selectedGroup) }}
                <span v-if="itemTotal > 0" class="ml-2 text-foreground/70">共 {{ itemTotal }} 项</span>
              </p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <Input
                v-model="itemKeyword"
                placeholder="编码/名称/备注"
                class="w-44"
                @keyup.enter="applyItemSearch"
              />
              <Button variant="outline" size="sm" @click="applyItemSearch">
                <Search class="h-3.5 w-3.5" />
              </Button>
              <Button v-if="hasAction('dict_add')" @click="openCreateItem">
                <Plus class="mr-1 h-4 w-4" />
                新建字典项
              </Button>
            </div>
          </div>
          <DataTableShell
            :loading="itemsLoading"
            :error="itemsError"
            :empty="!items.length"
            empty-text="该分组下暂无字典项"
          >
            <Table>
              <thead>
                <tr class="border-b bg-muted/50">
                  <th class="h-10 px-4 text-left font-medium">编码</th>
                  <th class="h-10 px-4 text-left font-medium">名称</th>
                  <th class="h-10 px-4 text-left font-medium">备注</th>
                  <th class="h-10 px-4 text-right font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in items" :key="dicId(row)" class="border-b">
                  <td class="p-4 font-mono text-sm">{{ dicCode(row) }}</td>
                  <td class="p-4">{{ dicName(row) }}</td>
                  <td class="p-4 text-muted-foreground">{{ dicRemark(row) || '—' }}</td>
                  <td class="space-x-1 p-4 text-right">
                    <Button
                      v-if="hasAction('dict_edit')"
                      variant="outline"
                      size="sm"
                      @click="openEditItem(row)"
                    >
                      <Pencil class="h-3.5 w-3.5" />
                    </Button>
                    <Button
                      v-if="hasAction('dict_delete')"
                      variant="destructive"
                      size="sm"
                      @click="handleDeleteItem(row)"
                    >
                      <Trash2 class="h-3.5 w-3.5" />
                    </Button>
                  </td>
                </tr>
              </tbody>
            </Table>
            <PaginationBar
              :page="itemPage"
              :total="itemTotal"
              :page-size="ITEM_PAGE_SIZE"
              @change="onItemPageChange"
            />
          </DataTableShell>
        </template>
      </section>
    </div>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="dialogTitle"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="编码" required>
        <Input v-model="form.code" :disabled="editing && dialogMode === 'group'" />
      </FormField>
      <FormField label="名称" required>
        <Input v-model="form.name" />
      </FormField>
      <FormField v-if="dialogMode === 'item'" label="备注">
        <Input v-model="form.remark" />
      </FormField>
      <p v-if="dialogMode === 'group'" class="text-xs text-muted-foreground">
        分组为根节点，用于归类字典项（对应后端 parentId 为空）。
      </p>
      <p v-else-if="selectedGroup" class="text-xs text-muted-foreground">
        将保存到分组「{{ dicName(selectedGroup) }}」（{{ dicCode(selectedGroup) }}）
      </p>
    </CrudDialog>
  </div>
</template>
