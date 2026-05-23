<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus, Pencil, Trash2, RefreshCw } from 'lucide-vue-next'
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
import { listOrgTree, saveOrg, deleteOrg } from '@/api/org'
import type { BaseOrg } from '@/api/types'

const flat = ref<Array<BaseOrg & { depth: number }>>([])
const loading = ref(true)
const error = ref('')

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

function flatten(orgs: BaseOrg[], depth = 0): Array<BaseOrg & { depth: number }> {
  const out: Array<BaseOrg & { depth: number }> = []
  for (const o of orgs) {
    out.push({ ...o, depth })
    if (o.children?.length) out.push(...flatten(o.children, depth + 1))
  }
  return out
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const tree = await listOrgTree()
    flat.value = flatten(tree)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.value.orgName?.trim()) {
    formError.value = '组织名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await saveOrg({
      ...form.value,
      id: editing.value ? (form.value.id ?? form.value.orgId) : undefined,
      parentId: form.value.parentId ? Number(form.value.parentId) : undefined,
      sort: form.value.sort != null ? Number(form.value.sort) : 0,
    })
    closeDialog()
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseOrg) {
  const id = row.id ?? row.orgId
  if (!id || !confirm(`确认删除组织 ${row.orgName}？`)) return
  await deleteOrg({ id })
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="组织管理" description="POST /baseOrg/tree — 树形组织维护">
      <template #actions>
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
    <DataTableShell :loading="loading" :error="error" :empty="!flat.length">
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
          <tr v-for="row in flat" :key="row.id ?? row.orgId" class="border-b">
            <td class="p-4">{{ row.id ?? row.orgId }}</td>
            <td class="p-4" :style="{ paddingLeft: `${row.depth * 16 + 16}px` }">{{ row.orgName }}</td>
            <td class="p-4">{{ row.parentId ?? '—' }}</td>
            <td class="p-4">{{ row.sort }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '正常' : '停用' }}
              </Badge>
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
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑组织' : '新建组织'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="组织名称" required>
        <Input v-model="form.orgName" />
      </FormField>
      <FormField label="父级 ID">
        <Input v-model="form.parentId" type="number" placeholder="根组织留空" />
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
