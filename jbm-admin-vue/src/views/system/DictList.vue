<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Plus, Pencil, Trash2, RefreshCw, FolderOpen } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import { useCrudForm } from '@/composables/useCrudForm'
import { getDicMap, listRootDicts, saveDict, deleteDict } from '@/api/dict'
import { usePermission } from '@/composables/usePermission'
import type { BaseDic } from '@/api/types'

const { hasAction } = usePermission()

type DialogMode = 'group' | 'item'

function dicId(row: BaseDic) {
  return row.id ?? row.dicId
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
const dicMap = ref<Record<string, BaseDic[]>>({})
const selectedGroup = ref<BaseDic | null>(null)
const groupsLoading = ref(false)
const itemsLoading = ref(false)
const groupsError = ref('')
const itemsError = ref('')
const groupKeyword = ref('')
const itemKeyword = ref('')
const dialogMode = ref<DialogMode>('group')

const filteredGroups = computed(() => {
  const kw = groupKeyword.value.trim().toLowerCase()
  if (!kw) return groups.value
  return groups.value.filter(
    (g) => dicCode(g).toLowerCase().includes(kw) || dicName(g).toLowerCase().includes(kw),
  )
})

const groupItems = computed(() => {
  const g = selectedGroup.value
  if (!g) return []
  const code = dicCode(g)
  const list = dicMap.value[code] ?? []
  const kw = itemKeyword.value.trim().toLowerCase()
  if (!kw) return list
  return list.filter(
    (d) =>
      dicCode(d).toLowerCase().includes(kw) ||
      dicName(d).toLowerCase().includes(kw) ||
      dicRemark(d).toLowerCase().includes(kw),
  )
})

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

async function loadGroups() {
  groupsLoading.value = true
  groupsError.value = ''
  try {
    const list = await listRootDicts()
    groups.value = list ?? []
    if (selectedGroup.value) {
      const id = dicId(selectedGroup.value)
      const still = groups.value.find((g) => dicId(g) === id)
      selectedGroup.value = still ?? groups.value[0] ?? null
    } else if (groups.value.length) {
      selectedGroup.value = groups.value[0]
    }
  } catch (e) {
    groupsError.value = e instanceof Error ? e.message : '加载分组失败'
  } finally {
    groupsLoading.value = false
  }
}

async function loadDicMap() {
  itemsLoading.value = true
  itemsError.value = ''
  try {
    dicMap.value = (await getDicMap()) ?? {}
  } catch (e) {
    itemsError.value = e instanceof Error ? e.message : '加载字典项失败'
  } finally {
    itemsLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadGroups(), loadDicMap()])
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
  const code = dicCode(g)
  const childCount = (dicMap.value[code] ?? []).length
  const msg =
    childCount > 0
      ? `分组「${dicName(g)}」下还有 ${childCount} 个字典项，确认删除分组？`
      : `确认删除分组「${dicName(g)}」？`
  if (!confirm(msg)) return
  await deleteDict({ id })
  if (selectedGroup.value && dicId(selectedGroup.value) === id) {
    selectedGroup.value = null
  }
  await refreshAll()
}

async function handleDeleteItem(row: BaseDic) {
  const id = dicId(row)
  if (!id || !confirm(`确认删除字典项「${dicName(row)}」？`)) return
  await deleteDict({ id })
  await loadDicMap()
}

onMounted(refreshAll)

watch(selectedGroup, () => {
  itemKeyword.value = ''
})
</script>

<template>
  <div>
    <PageHeader title="字典管理" description="分组 + 字典项，两层树结构维护">
      <template #actions>
        <Button variant="outline" size="sm" :disabled="groupsLoading || itemsLoading" @click="refreshAll">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <div class="flex flex-col gap-4 lg:flex-row lg:items-stretch">
      <!-- 左侧：字典分组 -->
      <aside
        class="flex w-full shrink-0 flex-col rounded-lg border bg-card lg:w-72"
      >
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
        <div class="border-b px-3 py-2">
          <Input v-model="groupKeyword" placeholder="搜索分组" class="h-8 text-sm" />
        </div>
        <div class="min-h-[200px] flex-1 overflow-y-auto p-2">
          <p v-if="groupsLoading" class="px-2 py-4 text-sm text-muted-foreground">加载中…</p>
          <p v-else-if="groupsError" class="px-2 py-4 text-sm text-destructive">{{ groupsError }}</p>
          <p v-else-if="!filteredGroups.length" class="px-2 py-4 text-sm text-muted-foreground">
            暂无分组
          </p>
          <ul v-else class="space-y-0.5">
            <li
              v-for="g in filteredGroups"
              :key="dicId(g)"
              class="group flex cursor-pointer items-center gap-2 rounded-md px-2 py-2 text-sm transition-colors hover:bg-muted"
              :class="
                selectedGroup && dicId(selectedGroup) === dicId(g)
                  ? 'bg-primary/10 text-primary font-medium'
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
      </aside>

      <!-- 右侧：字典项 -->
      <section class="min-w-0 flex-1">
        <div v-if="!selectedGroup" class="rounded-lg border bg-muted/30 px-6 py-16 text-center text-muted-foreground">
          请从左侧选择一个字典分组
        </div>
        <template v-else>
          <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 class="text-lg font-semibold">{{ dicName(selectedGroup) }}</h2>
              <p class="font-mono text-sm text-muted-foreground">{{ dicCode(selectedGroup) }}</p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <Input
                v-model="itemKeyword"
                placeholder="编码/名称/备注"
                class="w-44"
              />
              <Button v-if="hasAction('dict_add')" @click="openCreateItem">
                <Plus class="mr-1 h-4 w-4" />
                新建字典项
              </Button>
            </div>
          </div>
          <DataTableShell
            :loading="itemsLoading"
            :error="itemsError"
            :empty="!groupItems.length"
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
                <tr v-for="row in groupItems" :key="dicId(row)" class="border-b">
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
