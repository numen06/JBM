<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listRoutes, deleteRoute } from '@/api/gateway'
import type { GatewayRoute } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } = usePagedList<GatewayRoute>(listRoutes)

async function handleDelete(row: GatewayRoute) {
  if (!row.routeId || !confirm(`确认删除路由 ${row.routeName}？`)) return
  await deleteRoute(row.routeId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="网关路由" description="GET /gateway/routes" />
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">路径</th>
            <th class="h-10 px-4 text-left font-medium">服务</th>
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
            <td class="p-4">
              <Badge>{{ row.status === 1 ? '启用' : '停用' }}</Badge>
            </td>
            <td class="p-4 text-right">
              <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
