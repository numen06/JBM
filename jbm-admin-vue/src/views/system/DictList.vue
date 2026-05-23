<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { pageDicts, saveDict, deleteDict } from '@/api/dict'
import { usePermission } from '@/composables/usePermission'
import type { BaseDic } from '@/api/types'

const { hasAction } = usePermission()

const keyword = ref('')
const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseDic>(
  async (p, s) => {
    const data = await pageDicts(p, s)
    if (!keyword.value.trim()) return data
    const kw = keyword.value.trim().toLowerCase()
    const filtered = (data.contents ?? []).filter(
      (d) =>
        d.dicCode?.toLowerCase().includes(kw) ||
        d.dicName?.toLowerCase().includes(kw) ||
        d.dicValue?.toLowerCase().includes(kw),
    )
    return { ...data, contents: filtered, total: filtered.length }
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
} = useCrudForm<BaseDic>(() => ({
  code: '',
  name: '',
  remark: '',
  parentId: undefined,
}))

function search() {
  load(1)
}

async function handleSave() {
  if (!form.value.code?.trim() || !form.value.name?.trim()) {
    formError.value = '字典编码和名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await saveDict({
      ...form.value,
      parentId: form.value.parentId ? Number(form.value.parentId) : undefined,
    })
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseDic) {
  if (!row.dicId || !confirm(`确认删除字典 ${row.dicName}？`)) return
  await deleteDict({ dicId: row.dicId })
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="字典管理" description="POST /baseDic — 分页与维护">
      <template #actions>
        <Input v-model="keyword" placeholder="编码/名称/值" class="w-44" @keyup.enter="search" />
        <Button variant="outline" @click="search">筛选</Button>
        <Button v-if="hasAction('dict_add')" @click="openCreate">
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
            <th class="h-10 px-4 text-left font-medium">值</th>
            <th class="h-10 px-4 text-left font-medium">父级</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.id ?? row.dicId" class="border-b">
            <td class="p-4">{{ row.id ?? row.dicId }}</td>
            <td class="p-4 font-mono text-sm">{{ row.code ?? row.dicCode }}</td>
            <td class="p-4">{{ row.name ?? row.dicName }}</td>
            <td class="p-4">{{ row.remark ?? row.dicValue }}</td>
            <td class="p-4">{{ row.parentId ?? '—' }}</td>
            <td class="p-4 text-right space-x-1">
              <Button v-if="hasAction('dict_edit')" variant="outline" size="sm" @click="openEdit(row)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button v-if="hasAction('dict_delete')" variant="destructive" size="sm" @click="handleDelete(row)">
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
      :title="editing ? '编辑字典' : '新建字典'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="字典编码" required>
        <Input v-model="form.code" />
      </FormField>
      <FormField label="字典名称" required>
        <Input v-model="form.name" />
      </FormField>
      <FormField label="备注/值">
        <Input v-model="form.remark" />
      </FormField>
      <FormField label="父级 ID">
        <Input v-model="form.parentId" type="number" placeholder="可选" />
      </FormField>
    </CrudDialog>
  </div>
</template>
