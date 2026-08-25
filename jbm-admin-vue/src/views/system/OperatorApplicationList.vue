<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import Table from '@/components/ui/Table.vue'
import {
  listOperatorApplications,
  reviewOperatorApplication,
  type OperatorApplication,
} from '@/api/operatorApplication'

const rows = ref<OperatorApplication[]>([])
const loading = ref(false)
const error = ref('')

function statusLabel(status?: number) {
  return status === 0 ? '待审核' : status === 1 ? '已通过' : '已驳回'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await listOperatorApplications()
    rows.value = result.contents ?? []
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载运营申请失败'
  } finally {
    loading.value = false
  }
}

async function review(row: OperatorApplication, status: 1 | 2) {
  if (row.id == null) return
  await reviewOperatorApplication(row.id, status)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="运营申请审核" description="普通租户主动申请，平台审核通过后才获得 IoT 运营方角色。">
      <template #actions>
        <Button variant="outline" :disabled="loading" @click="load"><RefreshCw class="mr-1 size-4" />刷新</Button>
      </template>
    </PageHeader>

    <DataTableShell :loading="loading" :error="error" :empty="!rows.length">
      <Table>
        <thead><tr class="border-b bg-muted/50"><th class="p-3 text-left">租户 ID</th><th class="p-3 text-left">申请人</th><th class="p-3 text-left">申请说明</th><th class="p-3 text-left">状态</th><th class="p-3 text-right">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="String(row.id)" class="border-b">
            <td class="p-3 font-mono text-xs">{{ row.tenantId }}</td>
            <td class="p-3 font-mono text-xs">{{ row.applicantUserId }}</td>
            <td class="p-3">{{ row.reason || '-' }}</td>
            <td class="p-3"><Badge :variant="row.status === 1 ? 'default' : 'secondary'">{{ statusLabel(row.status) }}</Badge></td>
            <td class="space-x-2 p-3 text-right">
              <template v-if="row.status === 0">
                <Button size="sm" @click="review(row, 1)">通过</Button>
                <Button size="sm" variant="outline" @click="review(row, 2)">驳回</Button>
              </template>
              <span v-else class="text-xs text-muted-foreground">已处理</span>
            </td>
          </tr>
        </tbody>
      </Table>
    </DataTableShell>
  </div>
</template>
