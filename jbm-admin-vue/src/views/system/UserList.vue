<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Pencil } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useOrgTree } from '@/composables/useOrgTree'
import { useFeedback } from '@/composables/useFeedback'
import { listUsers, closeUser } from '@/api/user'
import { usePermission } from '@/composables/usePermission'
import { toSnowflakeIdString } from '@/lib/snowflakeId'
import type { BaseUser } from '@/api/types'

const { hasAction } = usePermission()
const feedback = useFeedback()
const router = useRouter()
const { orgLabel, loadOrgs } = useOrgTree()

onMounted(() => {
  loadOrgs()
})

const keyword = ref('')
const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseUser>(
  (p, s) => listUsers(p, s, keyword.value || undefined),
)

function search() {
  load(1)
}

function openCreateUser() {
  router.push({ name: 'user-new' })
}

function openEditUser(row: BaseUser) {
  if (!row.userId) return
  router.push({ name: 'user-edit', params: { userId: toSnowflakeIdString(row.userId) } })
}

async function handleClose(row: BaseUser) {
  if (!row.userId) return
  const confirmed = await feedback.confirm({
    title: '确认注销用户',
    message: `确认注销用户 ${row.userName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await closeUser(row.userId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader
      title="用户管理"
      description="Center /user — 按钮权限 ACTION_users_*；同一用户可用用户名/手机/邮箱登录"
    >
      <template #actions>
        <Input v-model="keyword" placeholder="关键字" class="w-40" @keyup.enter="search" />
        <Button variant="outline" @click="search">查询</Button>
        <Button v-if="hasAction('users_add')" @click="openCreateUser">
          <Plus class="mr-1 h-4 w-4" />
          新建
        </Button>
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
            <th class="h-10 px-4 text-left font-medium">邮箱</th>
            <th class="h-10 px-4 text-left font-medium">所属组织</th>
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
            <td class="p-4">{{ row.email }}</td>
            <td class="p-4">{{ orgLabel(row.companyId) }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '正常' : row.status === 0 ? '禁用' : '其他' }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1">
              <Button
                v-if="hasAction('users_edit')"
                variant="outline"
                size="sm"
                @click="openEditUser(row)"
              >
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button
                v-if="hasAction('users_delete')"
                variant="destructive"
                size="sm"
                @click="handleClose(row)"
              >
                注销
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
