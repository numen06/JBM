<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Eye, Pause, Play, RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import Dialog from '@/components/ui/Dialog.vue'
import { usePagedList } from '@/composables/usePagedList'
import {
  getClusterAccessInfo,
  listGatewayLogs,
  type GatewayLogQuery,
} from '@/api/logs'
import type { ClusterAccessInfo, GatewayLog } from '@/api/types'

const props = withDefaults(defineProps<{ category?: 'access' | 'login' }>(), { category: 'access' })
const stats = ref<ClusterAccessInfo>({})
const statsLoading = ref(false)
const statsError = ref('')
const selectedGatewayLog = ref<GatewayLog | null>(null)
const filters = ref<GatewayLogQuery>(defaultFilters())
const autoRefreshEnabled = ref(true)
const AUTO_REFRESH_MS = 10_000
let autoRefreshTimer: ReturnType<typeof setInterval> | undefined

const gatewayList = usePagedList<GatewayLog>((p, s) => listGatewayLogs(p, s, cleanQuery()))
const isLoginLogs = computed(() => props.category === 'login')
const pageTitle = computed(() => (isLoginLogs.value ? '登录日志' : '访问日志'))
const pageDescription = computed(() => (isLoginLogs.value ? '认证登录访问记录' : '网关访问记录'))
const detailTitle = computed(() => selectedGatewayLog.value?.path || `${pageTitle.value}详情`)

function defaultFilters(): GatewayLogQuery {
  return {
    path: props.category === 'login' ? '/auth' : '',
    serviceId: '',
    method: '',
    status: '',
    ip: '',
    appKey: '',
  }
}

function cleanQuery(): GatewayLogQuery {
  return Object.fromEntries(
    Object.entries(filters.value).filter(([, value]) => String(value ?? '').trim() !== ''),
  ) as GatewayLogQuery
}

async function loadStats(silent = false) {
  if (isLoginLogs.value) return
  if (!silent) {
    statsLoading.value = true
    statsError.value = ''
  }
  try {
    stats.value = await getClusterAccessInfo()
    if (silent) {
      statsError.value = ''
    }
  } catch (e) {
    if (!silent) {
      statsError.value = e instanceof Error ? e.message : '统计加载失败'
    }
  } finally {
    if (!silent) {
      statsLoading.value = false
    }
  }
}

function refresh(silent = false) {
  loadStats(silent)
  gatewayList.load(gatewayList.page.value, gatewayList.pageSize.value, { silent })
}

function stopAutoRefresh() {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = undefined
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  if (!autoRefreshEnabled.value) return
  autoRefreshTimer = setInterval(() => {
    if (selectedGatewayLog.value || gatewayList.loading.value || statsLoading.value) return
    refresh(true)
  }, AUTO_REFRESH_MS)
}

function toggleAutoRefresh() {
  autoRefreshEnabled.value = !autoRefreshEnabled.value
  if (autoRefreshEnabled.value) {
    startAutoRefresh()
    refresh(true)
  } else {
    stopAutoRefresh()
  }
}

function search() {
  loadStats()
  gatewayList.load(1)
}

function resetFilters() {
  filters.value = defaultFilters()
  search()
}

function formatTime(value?: string | number) {
  if (value == null || value === '') return '-'
  const normalized = typeof value === 'string' && /^\d{11,}$/.test(value) ? Number(value) : value
  const time = new Date(normalized)
  if (Number.isNaN(time.getTime())) return String(value)
  return time.toLocaleString()
}

function gatewayId(row: GatewayLog, index: number) {
  return row.id ?? row.logId ?? row.accessId ?? row.requestId ?? row.traceId ?? index
}

function gatewayMethod(row: GatewayLog) {
  return row.method || row.requestMethod || '-'
}

function gatewayStatus(row: GatewayLog) {
  return row.httpStatus ?? row.status ?? '-'
}

function gatewayCost(row: GatewayLog) {
  return row.spendTime ?? row.costTime ?? row.useTime ?? '-'
}

function gatewayIp(row: GatewayLog) {
  return row.requestIp || row.ip || '-'
}

function gatewayTime(row: GatewayLog) {
  return formatTime(row.requestTime || row.createTime || row.timestamp)
}

function redactSensitive(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(redactSensitive)
  if (!value || typeof value !== 'object') return value
  const result: Record<string, unknown> = {}
  for (const [key, item] of Object.entries(value as Record<string, unknown>)) {
    if (/password|passwd|pwd|token|secret|authorization|credential/i.test(key)) {
      result[key] = '******'
    } else {
      result[key] = redactSensitive(item)
    }
  }
  return result
}

function pretty(value: unknown) {
  if (value == null || value === '') return '-'
  if (typeof value === 'string') {
    try {
      return JSON.stringify(redactSensitive(JSON.parse(value)), null, 2)
    } catch {
      return value
    }
  }
  return JSON.stringify(redactSensitive(value), null, 2)
}

watch(
  () => props.category,
  () => {
    selectedGatewayLog.value = null
    filters.value = defaultFilters()
    refresh()
  },
)

onMounted(() => {
  refresh()
  startAutoRefresh()
})

onUnmounted(stopAutoRefresh)
</script>

<template>
  <div class="space-y-4">
    <PageHeader :title="pageTitle" :description="pageDescription">
      <template #actions>
        <Button
          variant="outline"
          :class="autoRefreshEnabled ? 'border-primary/40 text-primary' : ''"
          @click="toggleAutoRefresh"
        >
          <Pause v-if="autoRefreshEnabled" class="h-4 w-4" />
          <Play v-else class="h-4 w-4" />
          {{ autoRefreshEnabled ? '自动刷新中' : '开启自动刷新' }}
        </Button>
        <Button variant="outline" :disabled="statsLoading || gatewayList.loading.value" @click="refresh()">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <div class="grid gap-3 rounded-lg border p-3 md:grid-cols-6">
      <Input v-model="filters.path" placeholder="请求路径" @keyup.enter="search" />
      <Input v-model="filters.serviceId" placeholder="服务名" @keyup.enter="search" />
      <Select v-model="filters.method">
        <option value="">全部方法</option>
        <option value="GET">GET</option>
        <option value="POST">POST</option>
        <option value="PUT">PUT</option>
        <option value="DELETE">DELETE</option>
        <option value="PATCH">PATCH</option>
      </Select>
      <Input v-model="filters.status" placeholder="状态码" @keyup.enter="search" />
      <Input v-model="filters.ip" placeholder="来源 IP" @keyup.enter="search" />
      <Input v-model="filters.appKey" placeholder="应用 / API Key" @keyup.enter="search" />
      <div class="flex gap-2 md:col-span-6">
        <Button size="sm" @click="search">搜索</Button>
        <Button size="sm" variant="outline" @click="resetFilters">重置</Button>
      </div>
    </div>

    <div v-if="!isLoginLogs" class="grid gap-3 md:grid-cols-3">
      <div class="rounded-lg border p-4">
        <div class="text-sm text-muted-foreground">访问总量</div>
        <div class="mt-2 text-2xl font-semibold">{{ stats.total ?? 0 }}</div>
      </div>
      <div class="rounded-lg border p-4">
        <div class="text-sm text-muted-foreground">今日访问</div>
        <div class="mt-2 text-2xl font-semibold">{{ stats.today ?? 0 }}</div>
      </div>
      <div class="rounded-lg border p-4">
        <div class="text-sm text-muted-foreground">采集状态</div>
        <div class="mt-3">
          <Badge :variant="statsError ? 'destructive' : 'secondary'">
            {{ statsError || (autoRefreshEnabled ? 'RabbitMQ 消费中 · 每 10 秒刷新' : 'RabbitMQ 消费中') }}
          </Badge>
        </div>
      </div>
    </div>

    <DataTableShell
      :loading="gatewayList.loading.value"
      :error="gatewayList.error.value"
      :empty="!gatewayList.items.value.length"
    >
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">时间</th>
            <th class="h-10 px-4 text-left font-medium">方法</th>
            <th class="h-10 px-4 text-left font-medium">业务路径</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-left font-medium">耗时</th>
            <th class="h-10 px-4 text-left font-medium">来源</th>
            <th class="h-10 px-4 text-left font-medium">服务</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in gatewayList.items.value" :key="gatewayId(row, index)" class="border-b">
            <td class="p-4 text-sm text-muted-foreground">{{ gatewayTime(row) }}</td>
            <td class="p-4">
              <Badge variant="outline">{{ gatewayMethod(row) }}</Badge>
            </td>
            <td class="max-w-[360px] truncate p-4 font-mono text-sm">{{ row.path || '-' }}</td>
            <td class="p-4">{{ gatewayStatus(row) }}</td>
            <td class="p-4">{{ gatewayCost(row) }}</td>
            <td class="p-4 font-mono text-sm">{{ gatewayIp(row) }}</td>
            <td class="max-w-[220px] truncate p-4 text-sm">{{ row.serviceId || row.appName || '-' }}</td>
            <td class="p-4 text-right">
              <Button size="sm" variant="outline" @click="selectedGatewayLog = row">
                <Eye class="h-4 w-4" />
                详情
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar
        :page="gatewayList.page.value"
        :total="gatewayList.total.value"
        :page-size="gatewayList.pageSize.value"
        @change="gatewayList.load"
      />
    </DataTableShell>

    <Dialog
      :open="!!selectedGatewayLog"
      :title="detailTitle"
      class="max-w-5xl"
      @update:open="selectedGatewayLog = null"
    >
      <div v-if="selectedGatewayLog" class="space-y-4">
        <div class="grid gap-3 text-sm md:grid-cols-4">
          <div>
            <div class="text-muted-foreground">状态</div>
            <div>{{ gatewayStatus(selectedGatewayLog) }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">方法</div>
            <div>{{ gatewayMethod(selectedGatewayLog) }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">耗时</div>
            <div>{{ gatewayCost(selectedGatewayLog) }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">来源 IP</div>
            <div class="font-mono">{{ gatewayIp(selectedGatewayLog) }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">请求时间</div>
            <div>{{ gatewayTime(selectedGatewayLog) }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">响应时间</div>
            <div>{{ formatTime(selectedGatewayLog.responseTime) }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">服务</div>
            <div class="truncate">{{ selectedGatewayLog.serviceId || '-' }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">用户</div>
            <div>{{ selectedGatewayLog.userName || selectedGatewayLog.requestRealName || '-' }}</div>
          </div>
        </div>

        <div class="grid gap-4 lg:grid-cols-2">
          <div>
            <div class="mb-2 text-sm font-medium">请求参数</div>
            <pre class="max-h-64 overflow-auto rounded-lg border bg-muted/30 p-4 text-xs leading-5">{{ pretty(selectedGatewayLog.params) }}</pre>
          </div>
          <div>
            <div class="mb-2 text-sm font-medium">请求头</div>
            <pre class="max-h-64 overflow-auto rounded-lg border bg-muted/30 p-4 text-xs leading-5">{{ pretty(selectedGatewayLog.headers) }}</pre>
          </div>
        </div>

        <div>
          <div class="mb-2 text-sm font-medium">响应内容</div>
          <pre class="max-h-80 overflow-auto rounded-lg border bg-muted/30 p-4 text-xs leading-5">{{ pretty(selectedGatewayLog.responseBody || selectedGatewayLog.error) }}</pre>
        </div>
      </div>
    </Dialog>

  </div>
</template>
