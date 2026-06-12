<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RefreshCw, RotateCcw, Search } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import OrgTreeSelect from '@/components/OrgTreeSelect.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { usePermission } from '@/composables/usePermission'
import { useFeedback } from '@/composables/useFeedback'
import { useAuthStore } from '@/stores/auth'
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
const auth = useAuthStore()

const userName = ref('')
const ipaddr = ref('')
const appIdFilter = ref<number | string>('')
const companyIdFilter = ref<number | string | null>('')
const apps = ref<BaseApp[]>([])
const actionError = ref('')
const actionLoading = ref(false)

const currentToken = computed(() => auth.accessToken.replace(/^Bearer\s+/i, ''))

const appNameMap = computed(() => {
  const map = new Map<number, string>()
  for (const app of apps.value) {
    if (app.appId != null && !map.has(app.appId)) {
      map.set(app.appId, app.appName ?? String(app.appId))
    }
  }
  return map
})

const appOptions = computed(() => {
  const seen = new Set<number>()
  return apps.value.filter((app) => {
    if (app.appId == null || seen.has(app.appId)) return false
    seen.add(app.appId)
    return true
  })
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

const activeFilters = computed(() => {
  const filters: string[] = []
  if (userName.value.trim()) filters.push(`用户：${userName.value.trim()}`)
  if (ipaddr.value.trim()) filters.push(`IP：${ipaddr.value.trim()}`)
  if (appIdFilter.value) filters.push(`应用：${appNameMap.value.get(Number(appIdFilter.value)) ?? appIdFilter.value}`)
  if (companyIdFilter.value) filters.push('已筛组织')
  return filters
})

const pageStats = computed(() => {
  const browserCount = new Set(items.value.map((row) => row.browser).filter(Boolean)).size
  const current = items.value.some((row) => isCurrentSession(row))
  return { browserCount, current }
})

function appLabel(row: SysUserOnline) {
  if (row.appName) return row.appName
  if (row.appId == null) return '-'
  return appNameMap.value.get(row.appId) ?? String(row.appId)
}

function search() {
  load(1)
}

function resetFilters() {
  userName.value = ''
  ipaddr.value = ''
  appIdFilter.value = ''
  companyIdFilter.value = ''
  load(1)
}

function parseTime(t?: string) {
  if (!t) return '-'
  const date = new Date(t)
  if (Number.isNaN(date.getTime())) return t
  return date.getTime()
}

function formatTime(t?: string) {
  const time = parseTime(t)
  if (typeof time !== 'number') return time
  return new Date(time).toLocaleString()
}

function effectiveExpiredAt(row: SysUserOnline) {
  const times = [parseTime(row.activityTime), parseTime(row.expiredTime)]
    .filter((time): time is number => typeof time === 'number')
  if (!times.length) return undefined
  return Math.min(...times)
}

function formatEffectiveExpiredTime(row: SysUserOnline) {
  const time = effectiveExpiredAt(row)
  if (time == null) return '-'
  return new Date(time).toLocaleString()
}

function isExpired(row: SysUserOnline) {
  const time = effectiveExpiredAt(row)
  return time != null && time < Date.now()
}

function isCurrentSession(row: SysUserOnline) {
  return !!row.tokenId && row.tokenId === currentToken.value
}

function shortToken(tokenId?: string) {
  if (!tokenId) return '-'
  if (tokenId.length <= 18) return tokenId
  return `${tokenId.slice(0, 10)}...${tokenId.slice(-6)}`
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
  if (!row.tokenId || isCurrentSession(row)) return
  const confirmed = await feedback.confirm({
    title: '踢出在线用户',
    message: `将立即踢出 ${row.userName ?? row.tokenId}，该会话需要重新登录。`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await runAction(() => kickoutUser(row.tokenId!))
}

async function handleLogout(row: SysUserOnline) {
  if (!row.tokenId || isCurrentSession(row)) return
  const confirmed = await feedback.confirm({
    title: '注销令牌',
    message: `将注销 ${row.userName ?? row.tokenId} 的令牌，适合处理异常或泄露会话。`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await runAction(() => logoutUser(row.tokenId!))
}

async function handleExpire(row: SysUserOnline) {
  if (!row.tokenId) return
  const raw = await feedback.prompt({
    title: '设置过期时间',
    message: `让 ${row.userName ?? row.tokenId} 的会话在多少分钟后过期？`,
    defaultValue: '30',
    placeholder: '分钟',
  })
  if (raw == null) return
  const minutes = Number.parseInt(raw, 10)
  if (!Number.isFinite(minutes) || minutes <= 0) {
    feedback.toast.error('过期时间必须为正整数（分钟）。', '输入无效')
    return
  }
  await runAction(() => expireToken(row.tokenId!, minutes))
}

async function handleExpireImmediately(row: SysUserOnline) {
  if (!row.tokenId || isCurrentSession(row)) return
  const confirmed = await feedback.confirm({
    title: '立即过期会话',
    message: `将立即让 ${row.userName ?? row.tokenId} 的会话失效。`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await runAction(() => expireTokenImmediately(row.tokenId!))
}
</script>

<template>
  <div class="space-y-4">
    <PageHeader
      title="在线用户"
      description="查看当前登录会话，按用户、IP、应用和组织定位异常会话，并执行踢出、注销或过期控制。"
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
          <option v-for="app in appOptions" :key="app.appId" :value="app.appId">
            {{ app.appName || app.appCode || app.appId }}
          </option>
        </Select>
        <Input v-model="userName" placeholder="用户名" class="w-36" @keyup.enter="search" />
        <Input v-model="ipaddr" placeholder="IP 地址" class="w-36" @keyup.enter="search" />
        <Button variant="outline" @click="search">
          <Search class="h-4 w-4" />
          查询
        </Button>
        <Button variant="ghost" :disabled="loading || actionLoading" @click="resetFilters">
          <RotateCcw class="h-4 w-4" />
          重置
        </Button>
        <Button variant="outline" :disabled="loading || actionLoading" @click="load(page)">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <div class="flex flex-wrap items-center gap-2 rounded-md border bg-muted/20 px-3 py-2 text-sm">
      <span>共 {{ total }} 个在线会话</span>
      <Badge variant="secondary">当前页 {{ items.length }} 条</Badge>
      <Badge v-if="pageStats.current" variant="outline">包含我的会话</Badge>
      <Badge v-if="pageStats.browserCount" variant="outline">{{ pageStats.browserCount }} 种客户端</Badge>
      <span v-if="activeFilters.length" class="text-muted-foreground">
        筛选：{{ activeFilters.join(' / ') }}
      </span>
      <span v-else class="text-muted-foreground">未设置筛选条件</span>
    </div>

    <p v-if="actionError" class="text-sm text-destructive">{{ actionError }}</p>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length" empty-text="没有匹配的在线会话">
      <div class="overflow-x-auto">
        <Table>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-10 px-4 text-left font-medium">用户</th>
              <th class="h-10 px-4 text-left font-medium">来源</th>
              <th class="h-10 px-4 text-left font-medium">客户端</th>
              <th class="h-10 px-4 text-left font-medium">登录时间</th>
              <th class="h-10 px-4 text-left font-medium">生效过期时间</th>
              <th class="h-10 px-4 text-left font-medium">Token</th>
              <th class="h-10 px-4 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in items"
              :key="row.tokenId"
              class="border-b align-top"
              :class="isCurrentSession(row) ? 'bg-primary/5' : ''"
            >
              <td class="p-4">
                <div class="font-medium">{{ row.userName ?? '-' }}</div>
                <div class="mt-1 flex flex-wrap gap-1">
                  <Badge v-if="isCurrentSession(row)" variant="default">我的会话</Badge>
                  <Badge v-if="isExpired(row)" variant="destructive">已过期</Badge>
                  <Badge v-else variant="secondary">在线</Badge>
                </div>
                <div class="mt-1 text-xs text-muted-foreground">
                  {{ row.companyName ?? '-' }} / {{ row.deptName ?? '-' }}
                </div>
              </td>
              <td class="p-4">
                <div class="font-mono text-sm">{{ row.ipaddr ?? '-' }}</div>
                <div class="text-xs text-muted-foreground">{{ row.loginLocation ?? '-' }}</div>
                <div class="mt-1 text-xs text-muted-foreground">应用：{{ appLabel(row) }}</div>
              </td>
              <td class="p-4 text-sm">
                <div>{{ row.browser ?? '-' }}</div>
                <div class="text-xs text-muted-foreground">{{ row.os ?? '-' }}</div>
              </td>
              <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.loginTime) }}</td>
              <td class="p-4 text-sm text-muted-foreground">
                <div class="text-foreground">{{ formatEffectiveExpiredTime(row) }}</div>
                <div v-if="row.activityTime" class="mt-1 text-xs">空闲：{{ formatTime(row.activityTime) }}</div>
                <div v-if="row.expiredTime" class="text-xs">硬过期：{{ formatTime(row.expiredTime) }}</div>
              </td>
              <td class="p-4 font-mono text-xs text-muted-foreground" :title="row.tokenId">
                {{ shortToken(row.tokenId) }}
              </td>
              <td class="p-4 text-right">
                <div class="flex flex-wrap justify-end gap-1">
                  <Button
                    v-if="hasAction('monitor:online:forceLogout')"
                    variant="outline"
                    size="sm"
                    :disabled="actionLoading || isCurrentSession(row)"
                    title="让该会话重新登录"
                    @click="handleKickout(row)"
                  >
                    踢下线
                  </Button>
                  <Button
                    v-if="hasAction('monitor:online:logout')"
                    variant="destructive"
                    size="sm"
                    :disabled="actionLoading || isCurrentSession(row)"
                    title="注销令牌，适合处理疑似泄露会话"
                    @click="handleLogout(row)"
                  >
                    注销
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    :disabled="actionLoading"
                    title="设置几分钟后过期"
                    @click="handleExpire(row)"
                  >
                    定时过期
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    :disabled="actionLoading || isCurrentSession(row)"
                    title="立即让该会话失效"
                    @click="handleExpireImmediately(row)"
                  >
                    立即过期
                  </Button>
                </div>
              </td>
            </tr>
          </tbody>
        </Table>
      </div>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
