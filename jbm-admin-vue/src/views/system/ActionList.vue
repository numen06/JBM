<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import { useCrudForm } from '@/composables/useCrudForm'
import { usePagedList } from '@/composables/usePagedList'
import { useFeedback } from '@/composables/useFeedback'
import { listActions, createAction, updateAction, deleteAction } from '@/api/action'
import { listAllMenus } from '@/api/menu'
import type { BaseAction, BaseMenu } from '@/api/types'

const filterMenuId = ref<string>('')
const menus = ref<BaseMenu[]>([])
const feedback = useFeedback()

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseAction>(
  (p, size) => {
    const mid = filterMenuId.value ? Number(filterMenuId.value) : undefined
    return listActions(mid, p, size)
  },
)

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseAction>(() => ({
  actionCode: '',
  actionName: '',
  menuId: undefined,
  priority: 0,
  status: 1,
}))

async function loadMenus() {
  try {
    menus.value = await listAllMenus()
  } catch {
    menus.value = []
  }
}

function menuLabel(menuId?: number) {
  if (!menuId) return '—'
  const m = menus.value.find((x) => x.menuId === menuId)
  return m ? `${m.menuName} (${m.menuCode})` : String(menuId)
}

function openCreateAction() {
  openCreate()
  if (filterMenuId.value) {
    form.value.menuId = Number(filterMenuId.value)
  }
}

async function handleSave() {
  if (!form.value.actionCode?.trim() || !form.value.actionName?.trim()) {
    formError.value = '按钮编码和名称不能为空'
    return
  }
  if (!form.value.menuId) {
    formError.value = '请选择所属菜单'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const payload = {
      ...form.value,
      menuId: Number(form.value.menuId),
      priority: form.value.priority != null ? Number(form.value.priority) : 0,
    }
    if (editing.value && form.value.actionId) {
      await updateAction(form.value.actionId, payload)
    } else {
      await createAction(payload)
    }
    closeDialog()
    await load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseAction) {
  if (!row.actionId) return
  const confirmed = await feedback.confirm({
    title: '确认删除按钮',
    message: `确认删除按钮 ${row.actionName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteAction(row.actionId)
  await load(page.value)
}

watch(filterMenuId, () => load(1))
onMounted(async () => {
  await loadMenus()
})
</script>

<template>
  <div>
    <PageHeader
      title="按钮管理"
      description="Center /action — 标准按钮由启动种子写入；超管可增删改，权限标识为 ACTION_{actionCode}"
    >
      <template #actions>
        <Select v-model="filterMenuId" class="w-48">
          <option value="">全部菜单</option>
          <option v-for="m in menus" :key="m.menuId" :value="String(m.menuId)">
            {{ m.menuName }}
          </option>
        </Select>
        <Button @click="openCreateAction">
          <Plus class="mr-1 h-4 w-4" />
          新建
        </Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">编码</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">所属菜单</th>
            <th class="h-10 px-4 text-left font-medium">权限标识</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.actionId" class="border-b">
            <td class="p-4">{{ row.actionId }}</td>
            <td class="p-4 font-mono text-sm">{{ row.actionCode }}</td>
            <td class="p-4">{{ row.actionName }}</td>
            <td class="p-4 text-sm">{{ menuLabel(row.menuId) }}</td>
            <td class="p-4 font-mono text-xs text-muted-foreground">
              ACTION_{{ row.actionCode }}
            </td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" @click="openEdit(row)">
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
      :title="editing ? '编辑按钮' : '新建按钮'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="所属菜单" required>
        <Select v-model="form.menuId">
          <option :value="undefined">请选择</option>
          <option v-for="m in menus" :key="m.menuId" :value="m.menuId">
            {{ m.menuName }} ({{ m.path }})
          </option>
        </Select>
      </FormField>
      <FormField label="按钮编码" required>
        <Input v-model="form.actionCode" placeholder="如 users_add" />
      </FormField>
      <FormField label="按钮名称" required>
        <Input v-model="form.actionName" />
      </FormField>
      <FormField label="排序">
        <Input v-model="form.priority" type="number" />
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
