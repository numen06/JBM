<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listRoles, deleteRole } from '@/api/role'
import type { BaseRole } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseRole>(listRoles)

async function handleDelete(row: BaseRole) {
  if (!row.roleId || !confirm(`确认删除角色 ${row.roleName}？`)) return
  await deleteRole(row.roleId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="角色管理" description="Center /role" />
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">编码</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">备注</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.roleId" class="border-b">
            <td class="p-4">{{ row.roleId }}</td>
            <td class="p-4">{{ row.roleCode }}</td>
            <td class="p-4">{{ row.roleName }}</td>
            <td class="p-4 text-muted-foreground">{{ row.remark }}</td>
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
