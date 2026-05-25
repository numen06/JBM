<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Plus, Pencil, RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { useCrudForm } from '@/composables/useCrudForm'
import { listAllMenus, deleteMenu, createMenu, updateMenu } from '@/api/menu'
import type { BaseMenu } from '@/api/types'

const items = ref<BaseMenu[]>([])
const keyword = ref('')
const loading = ref(true)
const error = ref('')

const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return items.value
  return items.value.filter(
    (row) =>
      row.menuCode?.toLowerCase().includes(kw) ||
      row.menuName?.toLowerCase().includes(kw) ||
      row.path?.toLowerCase().includes(kw),
  )
})

function search() {
  /* 本地过滤，无需请求 */
}

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseMenu>(() => ({
  menuCode: '',
  menuName: '',
  path: '',
  parentId: undefined,
  icon: '',
  sort: 0,
  status: 1,
}))

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await listAllMenus()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.value.menuCode?.trim() || !form.value.menuName?.trim()) {
    formError.value = '菜单编码和名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const payload = {
      ...form.value,
      parentId: form.value.parentId ? Number(form.value.parentId) : undefined,
      sort: form.value.sort != null ? Number(form.value.sort) : 0,
    }
    if (editing.value && form.value.menuId) {
      await updateMenu(form.value.menuId, payload)
    } else {
      await createMenu(payload)
    }
    closeDialog()
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseMenu) {
  if (!row.menuId || !confirm(`确认删除菜单 ${row.menuName}？`)) return
  await deleteMenu(row.menuId)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      title="菜单管理"
      description="标准菜单由启动种子写入；超管可增删改（MENU_* 权限标识）"
    >
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="编码/名称/路径"
          class="w-44"
          @keyup.enter="search"
        />
        <Button variant="outline" @click="load">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
        <Button @click="openCreate">
          <Plus class="mr-1 h-4 w-4" />
          新建
        </Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!filteredItems.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">编码</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">路径</th>
            <th class="h-10 px-4 text-left font-medium">父级</th>
            <th class="h-10 px-4 text-left font-medium">排序</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in filteredItems" :key="row.menuId" class="border-b">
            <td class="p-4">{{ row.menuId }}</td>
            <td class="p-4 font-mono text-xs">{{ row.menuCode }}</td>
            <td class="p-4">{{ row.menuName }}</td>
            <td class="p-4 font-mono text-xs">{{ row.path }}</td>
            <td class="p-4">{{ row.parentId ?? '—' }}</td>
            <td class="p-4">{{ row.sort }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" @click="openEdit(row)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
            </td>
          </tr>
        </tbody>
      </Table>
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑菜单' : '新建菜单'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="菜单编码" required>
        <Input v-model="form.menuCode" />
      </FormField>
      <FormField label="菜单名称" required>
        <Input v-model="form.menuName" />
      </FormField>
      <FormField label="路径">
        <Input v-model="form.path" placeholder="/system/xxx" />
      </FormField>
      <FormField label="父级 ID">
        <Input v-model="form.parentId" type="number" placeholder="根菜单留空" />
      </FormField>
      <FormField label="图标">
        <Input v-model="form.icon" placeholder="lucide 图标名" />
      </FormField>
      <FormField label="排序">
        <Input v-model="form.sort" type="number" />
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
    </CrudDialog>
  </div>
</template>
