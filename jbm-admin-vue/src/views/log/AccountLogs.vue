<script setup lang="ts">
import { ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listAccountLogs } from '@/api/logs'
import type { BaseAccountLog } from '@/api/types'

const userFilter = ref('')

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseAccountLog>(
  async (p, s) => {
    const data = await listAccountLogs(p, s)
    if (!userFilter.value.trim()) return data
    const kw = userFilter.value.trim().toLowerCase()
    const filtered = (data.contents ?? []).filter(
      (l) =>
        l.userName?.toLowerCase().includes(kw) ||
        l.operation?.toLowerCase().includes(kw) ||
        l.ip?.includes(kw),
    )
    return { ...data, contents: filtered, total: filtered.length }
  },
)

function search() {
  load(1)
}

function formatTime(t?: string) {
  if (!t) return '—'
  try {
    return new Date(t).toLocaleString()
  } catch {
    return t
  }
}
</script>

<template>
  <div>
    <PageHeader title="审计日志" description="POST /baseAccountLogs/pageList — 账号操作审计">
      <template #actions>
        <Input
          v-model="userFilter"
          placeholder="用户/操作/IP"
          class="w-44"
          @keyup.enter="search"
        />
        <Button variant="outline" @click="search">筛选</Button>
        <Button variant="outline" :disabled="loading" @click="load(page)">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">用户</th>
            <th class="h-10 px-4 text-left font-medium">操作</th>
            <th class="h-10 px-4 text-left font-medium">IP</th>
            <th class="h-10 px-4 text-left font-medium">时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.logId" class="border-b">
            <td class="p-4">{{ row.logId }}</td>
            <td class="p-4">{{ row.userName }}</td>
            <td class="p-4">{{ row.operation }}</td>
            <td class="p-4 font-mono text-sm">{{ row.ip }}</td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.createTime) }}</td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
