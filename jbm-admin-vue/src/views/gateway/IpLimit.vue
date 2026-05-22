<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listIpLimits } from '@/api/gateway'
import type { GatewayIpLimit } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } =
  usePagedList<GatewayIpLimit>(listIpLimits)
</script>

<template>
  <div>
    <PageHeader title="IP 限制" description="GET /gateway/limit/ip" />
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">策略名</th>
            <th class="h-10 px-4 text-left font-medium">IP</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.policyId" class="border-b">
            <td class="p-4">{{ row.policyId }}</td>
            <td class="p-4">{{ row.policyName }}</td>
            <td class="p-4 font-mono text-sm">{{ row.ipAddress }}</td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
