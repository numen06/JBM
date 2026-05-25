<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus, Pencil, Trash2, RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { useCrudForm } from '@/composables/useCrudForm'
import { usePagedList } from '@/composables/usePagedList'
import { useOrgTree, orgRowId } from '@/composables/useOrgTree'
import { pageOrgs, saveOrg, deleteOrg } from '@/api/org'
import type { BaseOrg } from '@/api/types'

const keyword = ref('')
const { orgLabel, loadOrgs } = useOrgTree()

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseOrg>(
  (p, s) => pageOrgs(p, s, keyword.value || undefined),
)

function search() {
  load(1)
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
} = useCrudForm<BaseOrg>(() => ({
  orgName: '',
  parentId: undefined,
  sort: 0,
  status: 1,
}))

async function handleSave() {
  if (!form.value.orgName?.trim()) {
    formError.value = '组织名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await saveOrg({
      id: editing.value ? (form.value.id ?? form.value.orgId) : undefined,
      orgName: form.value.orgName,
      parentId: form.value.parentId,
      sort: form.value.sort,
      status: form.value.status,
    })
    closeDialog()
    await load(page.value)
    await loadOrgs()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseOrg) {
  const id = orgRowId(row)
  if (!id || !confirm(`确认删除组织 ${row.orgName}？`)) return
  await deleteOrg({ id })
  await load(page.value)
  await loadOrgs()
}

onMounted(loadOrgs)
</script>

<template>
  <div>
    <PageHeader title="组织管理" description="POST /baseOrg/pageList — 分页维护组织（树结构仍通过 /baseOrg/tree 供选择器使用）">
      <template #actions>
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
        <Button @click="openCreate">
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
