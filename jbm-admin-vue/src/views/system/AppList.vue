<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listApps, deleteApp } from '@/api/app'
import type { BaseApp } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseApp>(listApps)

async function handleDelete(row: BaseApp) {
  if (!row.appId || !confirm(`确认删除应用 ${row.appName}？`)) return
  await deleteApp(row.appId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="应用管理" description="Center /app" />
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">编码</th>
            <th class="h-10 px-4 text-left font-medium">Client ID</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.appId" class="border-b">
            <td class="p-4">{{ row.appId }}</td>
            <td class="p-4">{{ row.appName }}</td>
            <td class="p-4">{{ row.appCode }}</td>
            <td class="p-4 font-mono text-xs">{{ row.clientId }}</td>
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
