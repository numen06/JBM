<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listAccountLogs } from '@/api/logs'
import type { BaseAccountLog } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } =
  usePagedList<BaseAccountLog>(listAccountLogs)
</script>

<template>
  <div>
    <PageHeader title="审计日志" description="POST /baseAccountLogs/pageList" />
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
            <td class="p-4">{{ row.ip }}</td>
            <td class="p-4 text-sm text-muted-foreground">{{ row.createTime }}</td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
