<script setup lang="ts">
import { Plus, Pencil } from 'lucide-vue-next'
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
import { onMounted } from 'vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { useOrgTree } from '@/composables/useOrgTree'
import { listApps, deleteApp, createApp, updateApp } from '@/api/app'
import type { BaseApp } from '@/api/types'

const { orgLabel, loadOrgs } = useOrgTree()

onMounted(loadOrgs)

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseApp>(listApps)

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseApp>(() => ({
  appName: '',
  appCode: '',
  clientId: '',
  orgId: undefined,
  status: 1,
}))

async function handleSave() {
  if (!form.value.appName?.trim() || !form.value.appCode?.trim()) {
    formError.value = '应用名称和编码不能为空'
    return
  }
  if (!form.value.orgId) {
    formError.value = '请选择所属组织'
    return
  }
  saving.value = true
  formError.value = ''
  const payload = { ...form.value, orgId: Number(form.value.orgId) }
  try {
    if (editing.value && form.value.appId) {
      await updateApp(form.value.appId, payload)
    } else {
      await createApp(payload)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseApp) {
  if (!row.appId || !confirm(`确认删除应用 ${row.appName}？`)) return
  await deleteApp(row.appId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="应用管理" description="Center /app — OAuth 客户端应用">
      <template #actions>
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
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">编码</th>
            <th class="h-10 px-4 text-left font-medium">Client ID</th>
            <th class="h-10 px-4 text-left font-medium">所属组织</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.appId" class="border-b">
            <td class="p-4">{{ row.appId }}</td>
            <td class="p-4">{{ row.appName }}</td>
            <td class="p-4">{{ row.appCode }}</td>
            <td class="p-4 font-mono text-xs">{{ row.clientId }}</td>
            <td class="p-4">{{ orgLabel(row.orgId) }}</td>
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
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑应用' : '新建应用'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="应用名称" required>
        <Input v-model="form.appName" />
      </FormField>
      <FormField label="应用编码" required>
        <Input v-model="form.appCode" />
      </FormField>
      <FormField label="所属组织" required>
        <OrgTreeSelect v-model="form.orgId" placeholder="请选择组织" required />
      </FormField>
      <FormField label="Client ID">
        <Input v-model="form.clientId" class="font-mono text-sm" />
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
