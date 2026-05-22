<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listRateLimits } from '@/api/gateway'
import type { GatewayRateLimit } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } =
  usePagedList<GatewayRateLimit>(listRateLimits)
</script>

<template>
  <div>
    <PageHeader title="限流策略" description="GET /gateway/limit/rate" />
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">策略名</th>
            <th class="h-10 px-4 text-left font-medium">配额</th>
            <th class="h-10 px-4 text-left font-medium">周期</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.policyId" class="border-b">
            <td class="p-4">{{ row.policyId }}</td>
            <td class="p-4">{{ row.policyName }}</td>
            <td class="p-4">{{ row.limitQuota }}</td>
            <td class="p-4">{{ row.intervalUnit }}</td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
