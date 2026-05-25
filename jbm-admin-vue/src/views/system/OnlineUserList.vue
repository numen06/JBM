<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import OrgTreeSelect from '@/components/OrgTreeSelect.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { usePermission } from '@/composables/usePermission'
import { useFeedback } from '@/composables/useFeedback'
import { listApps } from '@/api/app'
import {
  expireToken,
  expireTokenImmediately,
  kickoutUser,
  listOnlineUsers,
  logoutUser,
} from '@/api/online'
import type { BaseApp, SysUserOnline } from '@/api/types'

const { hasAction, isSuperAdmin } = usePermission()
const feedback = useFeedback()

const userName = ref('')
const ipaddr = ref('')
const appIdFilter = ref<number | string>('')
const companyIdFilter = ref<number | string | null>('')
const apps = ref<BaseApp[]>([])
const actionError = ref('')
const actionLoading = ref(false)

const appNameMap = computed(() => {
  const map = new Map<number, string>()
  for (const app of apps.value) {
    if (app.appId != null) map.set(app.appId, app.appName ?? String(app.appId))
  }
  return map
})

onMounted(async () => {
  try {
    const data = await listApps(1, 200)
    apps.value = data.contents ?? []
  } catch {
    apps.value = []
  }
})

function buildSearch() {
  return {
    userName: userName.value.trim() || undefined,
    ipaddr: ipaddr.value.trim() || undefined,
    appId:
      appIdFilter.value !== '' && appIdFilter.value != null
        ? Number(appIdFilter.value)
        : undefined,
    companyId:
      isSuperAdmin.value && companyIdFilter.value !== '' && companyIdFilter.value != null
        ? Number(companyIdFilter.value)
        : undefined,
  }
}

const { items, total, page, loading, error, load, pageSize } = usePagedList<SysUserOnline>(
  (p, s) => listOnlineUsers(p, s, buildSearch()),
)

function appLabel(row: SysUserOnline) {
  if (row.appName) return row.appName
  if (row.appId == null) return '—'
  return appNameMap.value.get(row.appId) ?? String(row.appId)
}

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
  if (!row.tokenId) return
  const confirmed = await feedback.confirm({
    title: '确认踢出用户',
    message: `确认踢出用户 ${row.userName ?? row.tokenId}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await runAction(() => kickoutUser(row.tokenId!))
}

async function handleLogout(row: SysUserOnline) {
  if (!row.tokenId) return
  const confirmed = await feedback.confirm({
    title: '确认注销会话',
    message: `确认注销会话 ${row.userName ?? row.tokenId}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await runAction(() => logoutUser(row.tokenId!))
}

async function handleExpire(row: SysUserOnline) {
  if (!row.tokenId) return
  const raw = await feedback.prompt({
    title: '设置过期时间',
    message: '设置多少分钟后过期？',
    defaultValue: '30',
    placeholder: '分钟',
  })
  if (raw == null) return
  const minutes = Number.parseInt(raw, 10)
  if (!Number.isFinite(minutes) || minutes <= 0) {
    await feedback.alert({
      title: '输入无效',
      message: '过期时间必须为正整数（分钟）',
      variant: 'destructive',
    })
    return
  }
  await runAction(() => expireToken(row.tokenId!, minutes))
}

async function handleExpireImmediately(row: SysUserOnline) {
  if (!row.tokenId) return
  const confirmed = await feedback.confirm({
    title: '确认立即过期',
    message: `确认立即过期会话 ${row.userName ?? row.tokenId}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
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
        <OrgTreeSelect
          v-if="isSuperAdmin"
          v-model="companyIdFilter"
          placeholder="全部组织"
          class="w-44"
          @update:model-value="search"
        />
        <Select v-model="appIdFilter" class="w-40" @change="search">
          <option value="">全部应用</option>
          <option v-for="app in apps" :key="app.appId" :value="app.appId">
            {{ app.appName }}
          </option>
        </Select>
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
            <th class="h-10 px-4 text-left font-medium">组织</th>
            <th class="h-10 px-4 text-left font-medium">应用</th>
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
            <td class="p-4">{{ row.companyName ?? '—' }}</td>
            <td class="p-4">{{ appLabel(row) }}</td>
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
