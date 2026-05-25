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
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { useFeedback } from '@/composables/useFeedback'
import {
  listRateLimits,
  createRateLimit,
  updateRateLimit,
  deleteRateLimit,
} from '@/api/gateway'
import type { GatewayRateLimit } from '@/api/types'

const keyword = ref('')
const policyTypeFilter = ref('')
const feedback = useFeedback()

const { items, total, page, loading, error, load, pageSize } = usePagedList<GatewayRateLimit>(
  (p, s) =>
    listRateLimits(p, s, {
      keyword: keyword.value || undefined,
      policyType: policyTypeFilter.value || undefined,
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
} = useCrudForm<GatewayRateLimit>(() => ({
  policyName: '',
  policyType: 'url',
  limitQuota: 100,
  intervalUnit: 'seconds',
}))

async function handleSave() {
  if (!form.value.policyName?.trim()) {
    formError.value = '策略名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const payload = {
      ...form.value,
      limitQuota: form.value.limitQuota != null ? Number(form.value.limitQuota) : undefined,
    }
    if (editing.value && form.value.policyId) {
      await updateRateLimit(form.value.policyId, payload)
    } else {
      await createRateLimit(payload)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: GatewayRateLimit) {
  if (!row.policyId) return
  const confirmed = await feedback.confirm({
    title: '确认删除限流策略',
    message: `确认删除限流策略 ${row.policyName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteRateLimit(row.policyId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="限流策略" description="GET /gateway/limit/rate">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="策略名"
          class="w-40"
          @keyup.enter="search"
        />
        <Select v-model="policyTypeFilter" class="w-28">
          <option value="">全部类型</option>
          <option value="url">URL</option>
          <option value="origin">Origin</option>
          <option value="user">User</option>
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
            <th class="h-10 px-4 text-left font-medium">配额</th>
            <th class="h-10 px-4 text-left font-medium">周期</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.policyId" class="border-b">
            <td class="p-4">{{ row.policyId }}</td>
            <td class="p-4">{{ row.policyName }}</td>
            <td class="p-4">{{ row.policyType }}</td>
            <td class="p-4">{{ row.limitQuota }}</td>
            <td class="p-4">{{ row.intervalUnit }}</td>
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
      :title="editing ? '编辑限流策略' : '新建限流策略'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="策略名称" required>
        <Input v-model="form.policyName" />
      </FormField>
      <FormField label="规则类型">
        <Select v-model="form.policyType">
          <option value="url">URL</option>
          <option value="origin">Origin</option>
          <option value="user">User</option>
        </Select>
      </FormField>
      <FormField label="限制数">
        <Input v-model="form.limitQuota" type="number" />
      </FormField>
      <FormField label="时间单位">
        <Select v-model="form.intervalUnit">
          <option value="seconds">秒</option>
          <option value="minutes">分钟</option>
          <option value="hours">小时</option>
          <option value="days">天</option>
        </Select>
      </FormField>
    </CrudDialog>
  </div>
</template>
