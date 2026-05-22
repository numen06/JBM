<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listDevelopers } from '@/api/developer'
import type { BaseDeveloper } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } =
  usePagedList<BaseDeveloper>(listDevelopers)
</script>

<template>
  <div>
    <PageHeader title="开发者" description="GET /developer" />
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">用户名</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.developerId" class="border-b">
            <td class="p-4">{{ row.developerId }}</td>
            <td class="p-4">{{ row.developerName }}</td>
            <td class="p-4">{{ row.userName }}</td>
            <td class="p-4">
              <Badge>{{ row.status === 1 ? '正常' : '禁用' }}</Badge>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
