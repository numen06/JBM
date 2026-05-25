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
import { usePermission } from '@/composables/usePermission'
import {
  expireToken,
  expireTokenImmediately,
  kickoutUser,
  listOnlineUsers,
  logoutUser,
} from '@/api/online'
import type { SysUserOnline } from '@/api/types'

const { hasAction } = usePermission()

const userName = ref('')
const ipaddr = ref('')
const actionError = ref('')
const actionLoading = ref(false)

const { items, total, page, loading, error, load, pageSize } = usePagedList<SysUserOnline>(
  (p, s) =>
    listOnlineUsers(p, s, {
      userName: userName.value.trim() || undefined,
      ipaddr: ipaddr.value.trim() || undefined,
    }),
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

function shortToken(tokenId?: string) {
  if (!tokenId) return '—'
  if (tokenId.length <= 16) return tokenId
  return `${tokenId.slice(0, 8)}…${tokenId.slice(-6)}`
}

async function runAction(fn: () => Promise<unknown>) {
  actionError.value = ''
  actionLoading.value = true
  try {
    await fn()
    await load(page.value)
  } catch (e) {
    actionError.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    actionLoading.value = false
  }
}

async function handleKickout(row: SysUserOnline) {
  if (!row.tokenId || !confirm(`确认踢出用户 ${row.userName ?? row.tokenId}？`)) return
  await runAction(() => kickoutUser(row.tokenId!))
}

async function handleLogout(row: SysUserOnline) {
  if (!row.tokenId || !confirm(`确认注销会话 ${row.userName ?? row.tokenId}？`)) return
  await runAction(() => logoutUser(row.tokenId!))
}

async function handleExpire(row: SysUserOnline) {
  if (!row.tokenId) return
  const raw = prompt('设置多少分钟后过期？', '30')
  if (raw == null) return
  const minutes = Number.parseInt(raw, 10)
  if (!Number.isFinite(minutes) || minutes <= 0) {
    actionError.value = '过期时间须为正整数（分钟）'
    return
  }
  await runAction(() => expireToken(row.tokenId!, minutes))
}

async function handleExpireImmediately(row: SysUserOnline) {
  if (!row.tokenId || !confirm(`确认立即过期会话 ${row.userName ?? row.tokenId}？`)) return
  await runAction(() => expireTokenImmediately(row.tokenId!))
}
</script>

<template>
  <div>
    <PageHeader
      title="在线用户"
      description="Auth /online — 监控当前登录会话，支持踢出、注销与过期控制"
    >
      <template #actions>
        <Input
          v-model="userName"
          placeholder="用户名"
          class="w-36"
          @keyup.enter="search"
        />
        <Input
          v-model="ipaddr"
          placeholder="IP 地址"
          class="w-36"
          @keyup.enter="search"
        />
        <Button variant="outline" @click="search">查询</Button>
        <Button variant="outline" :disabled="loading || actionLoading" @click="load(page)">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <p v-if="actionError" class="mb-3 text-sm text-destructive">{{ actionError }}</p>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">用户名</th>
            <th class="h-10 px-4 text-left font-medium">部门</th>
            <th class="h-10 px-4 text-left font-medium">IP</th>
            <th class="h-10 px-4 text-left font-medium">登录地点</th>
            <th class="h-10 px-4 text-left font-medium">浏览器</th>
            <th class="h-10 px-4 text-left font-medium">系统</th>
            <th class="h-10 px-4 text-left font-medium">登录时间</th>
            <th class="h-10 px-4 text-left font-medium">过期时间</th>
            <th class="h-10 px-4 text-left font-medium">活动超时</th>
            <th class="h-10 px-4 text-left font-medium">Token</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.tokenId" class="border-b">
            <td class="p-4 font-medium">{{ row.userName ?? '—' }}</td>
            <td class="p-4">{{ row.deptName ?? '—' }}</td>
            <td class="p-4 font-mono text-sm">{{ row.ipaddr ?? '—' }}</td>
            <td class="p-4 text-sm">{{ row.loginLocation ?? '—' }}</td>
            <td class="p-4 text-sm">{{ row.browser ?? '—' }}</td>
            <td class="p-4 text-sm">{{ row.os ?? '—' }}</td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.loginTime) }}</td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.expiredTime) }}</td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.activityTime) }}</td>
            <td class="p-4 font-mono text-xs text-muted-foreground" :title="row.tokenId">
              {{ shortToken(row.tokenId) }}
            </td>
            <td class="p-4 text-right space-x-1 whitespace-nowrap">
              <Button
                v-if="hasAction('monitor:online:forceLogout')"
                variant="outline"
                size="sm"
                :disabled="actionLoading"
                @click="handleKickout(row)"
              >
                踢出
              </Button>
              <Button
                v-if="hasAction('monitor:online:logout')"
                variant="destructive"
                size="sm"
                :disabled="actionLoading"
                @click="handleLogout(row)"
              >
                注销
              </Button>
              <Button
                variant="outline"
                size="sm"
                :disabled="actionLoading"
                @click="handleExpire(row)"
              >
                设过期
              </Button>
              <Button
                variant="outline"
                size="sm"
                :disabled="actionLoading"
                @click="handleExpireImmediately(row)"
              >
                立即过期
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
