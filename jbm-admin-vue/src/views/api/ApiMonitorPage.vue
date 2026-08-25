<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Activity, Clock3, RefreshCw, Route, TriangleAlert } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { getClusterAccessInfo, listGatewayLogs } from '@/api/logs'
import type { ClusterAccessInfo, GatewayLog } from '@/api/types'

const logs = ref<GatewayLog[]>([])
const stats = ref<ClusterAccessInfo>({})
const loading = ref(false)
const error = ref('')
const lastRefresh = ref('')

function statusOf(row: GatewayLog) {
  return Number(row.httpStatus ?? row.status ?? 0)
}

function latencyOf(row: GatewayLog) {
  return Number(row.spendTime ?? row.costTime ?? row.useTime ?? 0) || 0
}

const errorCount = computed(() => logs.value.filter((row) => statusOf(row) >= 400 || row.error).length)
const errorRate = computed(() => (logs.value.length ? (errorCount.value / logs.value.length) * 100 : 0))
const averageLatency = computed(() => {
  if (!logs.value.length) return 0
  return Math.round(logs.value.reduce((sum, row) => sum + latencyOf(row), 0) / logs.value.length)
})
const p95Latency = computed(() => {
  const values = logs.value.map(latencyOf).sort((a, b) => a - b)
  return values.length ? values[Math.max(0, Math.ceil(values.length * 0.95) - 1)] : 0
})

const endpoints = computed(() => {
  const groups = new Map<string, { path: string; serviceId: string; count: number; errors: number; latency: number }>()
  for (const row of logs.value) {
    const path = row.path || '未知路径'
    const serviceId = row.serviceId || '未知服务'
    const key = `${serviceId}:${path}`
    const current = groups.get(key) || { path, serviceId, count: 0, errors: 0, latency: 0 }
    current.count += 1
    current.latency += latencyOf(row)
    if (statusOf(row) >= 400 || row.error) current.errors += 1
    groups.set(key, current)
  }
  return [...groups.values()]
    .map((row) => ({ ...row, averageLatency: Math.round(row.latency / row.count) }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [access, page] = await Promise.all([getClusterAccessInfo(), listGatewayLogs(1, 200)])
    stats.value = access
    logs.value = page.contents || []
    lastRefresh.value = new Date().toLocaleTimeString()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载 API 监控数据失败'
    logs.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="API 监控" description="基于网关访问日志统计最近 200 次请求的调用量、错误率和延迟。">
      <template #actions>
        <span v-if="lastRefresh" class="text-xs text-muted-foreground">最近刷新 {{ lastRefresh }}</span>
        <Button variant="outline" :disabled="loading" @click="load"><RefreshCw class="mr-1 size-4" />刷新</Button>
      </template>
    </PageHeader>

    <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
      <Card><CardContent class="pt-5"><Activity class="size-4 text-muted-foreground" /><p class="mt-2 text-xs text-muted-foreground">历史调用</p><p class="text-2xl font-semibold">{{ stats.total ?? 0 }}</p></CardContent></Card>
      <Card><CardContent class="pt-5"><Route class="size-4 text-muted-foreground" /><p class="mt-2 text-xs text-muted-foreground">今日调用</p><p class="text-2xl font-semibold">{{ stats.today ?? 0 }}</p></CardContent></Card>
      <Card><CardContent class="pt-5"><TriangleAlert class="size-4 text-muted-foreground" /><p class="mt-2 text-xs text-muted-foreground">样本错误率</p><p class="text-2xl font-semibold">{{ errorRate.toFixed(1) }}%</p></CardContent></Card>
      <Card><CardContent class="pt-5"><Clock3 class="size-4 text-muted-foreground" /><p class="mt-2 text-xs text-muted-foreground">平均延迟</p><p class="text-2xl font-semibold">{{ averageLatency }} ms</p></CardContent></Card>
      <Card><CardContent class="pt-5"><Clock3 class="size-4 text-muted-foreground" /><p class="mt-2 text-xs text-muted-foreground">P95 延迟</p><p class="text-2xl font-semibold">{{ p95Latency }} ms</p></CardContent></Card>
    </div>

    <DataTableShell :loading="loading" :error="error" :empty="!endpoints.length">
      <div class="grid gap-3 md:hidden">
        <div v-for="row in endpoints" :key="`${row.serviceId}:${row.path}`" class="rounded-lg border bg-card p-4 shadow-sm">
          <div class="flex items-start justify-between gap-3">
            <p class="break-all font-mono text-xs">{{ row.path }}</p>
            <Badge variant="secondary">{{ row.count }} 次</Badge>
          </div>
          <p class="mt-2 break-all text-xs text-muted-foreground">{{ row.serviceId }}</p>
          <div class="mt-3 flex justify-between text-sm"><span>平均 {{ row.averageLatency }} ms</span><span :class="row.errors ? 'text-destructive' : 'text-muted-foreground'">错误 {{ row.errors }}</span></div>
        </div>
      </div>
      <Table class="hidden md:table">
        <thead><tr class="border-b bg-muted/50"><th class="h-10 px-4 text-left font-medium">服务</th><th class="h-10 px-4 text-left font-medium">路径</th><th class="h-10 px-4 text-right font-medium">调用次数</th><th class="h-10 px-4 text-right font-medium">平均延迟</th><th class="h-10 px-4 text-right font-medium">错误</th></tr></thead>
        <tbody>
          <tr v-for="row in endpoints" :key="`${row.serviceId}:${row.path}`" class="border-b">
            <td class="p-4">{{ row.serviceId }}</td><td class="p-4 font-mono text-xs">{{ row.path }}</td><td class="p-4 text-right">{{ row.count }}</td><td class="p-4 text-right">{{ row.averageLatency }} ms</td><td class="p-4 text-right"><Badge :variant="row.errors ? 'destructive' : 'secondary'">{{ row.errors }}</Badge></td>
          </tr>
        </tbody>
      </Table>
    </DataTableShell>
  </div>
</template>
