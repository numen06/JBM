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
import { useFeedback } from '@/composables/useFeedback'
import { listIpLimits, createIpLimit, updateIpLimit, deleteIpLimit } from '@/api/gateway'
import type { GatewayIpLimit } from '@/api/types'

const keyword = ref('')
const policyTypeFilter = ref('')
const feedback = useFeedback()

const { items, total, page, loading, error, load, pageSize } = usePagedList<GatewayIpLimit>(
  (p, s) =>
    listIpLimits(p, s, {
      keyword: keyword.value || undefined,
      policyType: policyTypeFilter.value !== '' ? policyTypeFilter.value : undefined,
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
} = useCrudForm<GatewayIpLimit>(() => ({
  policyName: '',
  policyType: 1,
  ipAddress: '',
}))

async function handleSave() {
  if (!form.value.policyName?.trim() || !form.value.ipAddress?.trim()) {
    formError.value = '策略名称和 IP 地址不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const payload = {
      ...form.value,
      policyType: form.value.policyType != null ? Number(form.value.policyType) : 1,
    }
    if (editing.value && form.value.policyId) {
      await updateIpLimit(form.value.policyId, payload)
    } else {
      await createIpLimit(payload)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: GatewayIpLimit) {
  if (!row.policyId) return
  const confirmed = await feedback.confirm({
    title: '确认删除 IP 策略',
    message: `确认删除 IP 策略 ${row.policyName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteIpLimit(row.policyId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="IP 限制" description="GET /gateway/limit/ip">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="策略名"
          class="w-40"
          @keyup.enter="search"
        />
        <Select v-model="policyTypeFilter" class="w-28">
          <option value="">全部类型</option>
          <option value="1">白名单</option>
          <option value="0">黑名单</option>
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
            <th class="h-10 px-4 text-left font-medium">策略名</th>
            <th class="h-10 px-4 text-left font-medium">类型</th>
            <th class="h-10 px-4 text-left font-medium">IP</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.policyId" class="border-b">
            <td class="p-4">{{ row.policyId }}</td>
            <td class="p-4">{{ row.policyName }}</td>
            <td class="p-4">
              <Badge :variant="row.policyType === 1 ? 'default' : 'destructive'">
                {{ row.policyType === 1 ? '白名单' : '黑名单' }}
              </Badge>
            </td>
            <td class="p-4 font-mono text-sm">{{ row.ipAddress }}</td>
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
      :title="editing ? '编辑 IP 策略' : '新建 IP 策略'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="策略名称" required>
        <Input v-model="form.policyName" />
      </FormField>
      <FormField label="策略类型">
        <Select v-model="form.policyType">
          <option :value="1">白名单（允许）</option>
          <option :value="0">黑名单（拒绝）</option>
        </Select>
      </FormField>
      <FormField label="IP 地址" required>
        <Input
          v-model="form.ipAddress"
          placeholder="多个用分号分隔，最多 10 个"
          class="font-mono text-sm"
        />
      </FormField>
    </CrudDialog>
  </div>
</template>
