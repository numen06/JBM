<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Pencil } from 'lucide-vue-next'
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
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { listRoutes, deleteRoute, createRoute, updateRoute } from '@/api/gateway'
import type { GatewayRoute } from '@/api/types'

const keyword = ref('')
const statusFilter = ref('')

const { items, total, page, loading, error, load, pageSize } = usePagedList<GatewayRoute>(
  (p, s) =>
    listRoutes(p, s, {
      keyword: keyword.value || undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
    }),
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
} = useCrudForm<GatewayRoute>(() => ({
  routeName: '',
  path: '',
  serviceId: '',
  url: '',
  status: 1,
}))

async function handleSave() {
  if (!form.value.routeName?.trim() || !form.value.path?.trim()) {
    formError.value = '路由名称和路径不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    if (editing.value && form.value.routeId) {
      await updateRoute(form.value.routeId, form.value)
    } else {
      await createRoute(form.value)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: GatewayRoute) {
  if (!row.routeId || !confirm(`确认删除路由 ${row.routeName}？`)) return
  await deleteRoute(row.routeId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="网关路由" description="GET /gateway/routes">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="名称/路径/服务"
          class="w-44"
          @keyup.enter="search"
        />
        <Select v-model="statusFilter" class="w-28">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </Select>
        <Button variant="outline" @click="search">查询</Button>
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
            <th class="h-10 px-4 text-left font-medium">路径</th>
            <th class="h-10 px-4 text-left font-medium">服务</th>
            <th class="h-10 px-4 text-left font-medium">目标 URL</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.routeId" class="border-b">
            <td class="p-4">{{ row.routeId }}</td>
            <td class="p-4">{{ row.routeName }}</td>
            <td class="p-4 font-mono text-xs">{{ row.path }}</td>
            <td class="p-4">{{ row.serviceId }}</td>
            <td class="p-4 font-mono text-xs text-muted-foreground">{{ row.url }}</td>
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
      :title="editing ? '编辑路由' : '新建路由'"
      :saving="saving"
      wide
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="路由名称" required>
        <Input v-model="form.routeName" />
      </FormField>
      <FormField label="匹配路径" required>
        <Input v-model="form.path" placeholder="/api/xxx/**" class="font-mono text-sm" />
      </FormField>
      <FormField label="服务 ID">
        <Input v-model="form.serviceId" placeholder="lb://service-name" />
      </FormField>
      <FormField label="目标 URL">
        <Input v-model="form.url" placeholder="http://host:port" class="font-mono text-sm" />
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
