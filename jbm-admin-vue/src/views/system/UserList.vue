<script setup lang="ts">
import { ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listUsers, closeUser } from '@/api/user'
import type { BaseUser } from '@/api/types'

const keyword = ref('')
const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseUser>(
  (p, s) => listUsers(p, s, keyword.value || undefined),
)

function search() {
  load(1)
}

async function handleClose(row: BaseUser) {
  if (!row.userId || !confirm(`确认注销用户 ${row.userName}？`)) return
  await closeUser(row.userId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="用户管理" description="Center /user">
      <template #actions>
        <Input v-model="keyword" placeholder="关键字" class="w-40" @keyup.enter="search" />
        <Button variant="outline" @click="search">查询</Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">用户名</th>
            <th class="h-10 px-4 text-left font-medium">昵称</th>
            <th class="h-10 px-4 text-left font-medium">手机</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.userId" class="border-b">
            <td class="p-4">{{ row.userId }}</td>
            <td class="p-4">{{ row.userName }}</td>
            <td class="p-4">{{ row.nickName }}</td>
            <td class="p-4">{{ row.mobile }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </Badge>
            </td>
            <td class="p-4 text-right">
              <Button variant="destructive" size="sm" @click="handleClose(row)">注销</Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
