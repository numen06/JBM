<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RefreshCw, ChevronDown, ChevronRight } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import { listDiscoveryServices, listDiscoveryInstances } from '@/api/gateway'
import type { DiscoveryService, DiscoveryInstance } from '@/api/types'

const services = ref<DiscoveryService[]>([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const lastRefresh = ref('')
const page = ref(1)
const pageSize = 20
const expandedServiceId = ref<string | null>(null)
const instanceLoading = ref(false)
const instanceError = ref('')
const instances = ref<DiscoveryInstance[]>([])

async function load() {
  loading.value = true
  error.value = ''
  try {
    services.value = await listDiscoveryServices()
    lastRefresh.value = new Date().toLocaleTimeString()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载服务列表失败'
    services.value = []
  } finally {
    loading.value = false
  }
}

async function toggleExpand(serviceId: string) {
  if (expandedServiceId.value === serviceId) {
    expandedServiceId.value = null
    instances.value = []
    return
  }
  expandedServiceId.value = serviceId
  instanceLoading.value = true
  instanceError.value = ''
  instances.value = []
  try {
    instances.value = await listDiscoveryInstances(serviceId)
  } catch (e) {
    instanceError.value = e instanceof Error ? e.message : '加载实例失败'
  } finally {
    instanceLoading.value = false
  }
}

const filteredServices = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return services.value
  return services.value.filter((s) => s.serviceId?.toLowerCase().includes(kw))
})

const pagedServices = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredServices.value.slice(start, start + pageSize)
})

function extractVersions(svc: DiscoveryService): string[] {
  if (svc.versions?.length) return svc.versions
  const set = new Set<string>()
  for (const inst of svc.instances ?? []) {
    const v = inst.metadata?.version || inst.version
    if (v) set.add(v)
  }
  return [...set]
}

function extractClusters(svc: DiscoveryService): string[] {
  if (svc.clusters?.length) return svc.clusters
  const set = new Set<string>()
  for (const inst of svc.instances ?? []) {
    const c = inst.metadata?.cluster || inst.cluster
    if (c) set.add(c)
  }
  return [...set]
}

function metadataEntries(inst: DiscoveryInstance): string[] {
  const meta = inst.metadata
  if (!meta || !Object.keys(meta).length) return []
  return Object.entries(meta).map(([k, v]) => `${k}=${v}`)
}

watch(keyword, () => {
  page.value = 1
})

watch(
  () => filteredServices.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize))
    if (page.value > maxPage) page.value = maxPage
  },
)

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <PageHeader title="服务发现" description="注册中心服务实例一览">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="搜索服务名"
          class="w-44"
        />
        <Button variant="outline" :disabled="loading" @click="load">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <Card>
      <CardContent class="flex gap-8 pt-6">
        <div>
          <p class="text-sm text-muted-foreground">服务数</p>
          <p class="text-2xl font-bold">{{ services.length }}</p>
        </div>
        <div>
          <p class="text-sm text-muted-foreground">最近刷新</p>
          <p class="text-lg font-medium">{{ lastRefresh || '—' }}</p>
        </div>
      </CardContent>
    </Card>

    <DataTableShell :loading="loading" :error="error" :empty="!filteredServices.length">
      <Table mobile-title="服务名" :mobile-columns="['实例数', '健康实例', '版本']">
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 w-10 px-2 text-center font-medium"></th>
            <th class="h-10 px-4 text-left font-medium">服务名</th>
            <th class="h-10 px-4 text-left font-medium">实例数</th>
            <th class="h-10 px-4 text-left font-medium">健康实例</th>
            <th class="h-10 px-4 text-left font-medium">版本</th>
            <th class="h-10 px-4 text-left font-medium">集群</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="svc in pagedServices" :key="svc.serviceId">
            <tr
              class="cursor-pointer border-b hover:bg-muted/30"
              @click="toggleExpand(svc.serviceId)"
            >
              <td class="p-2 text-center">
                <ChevronDown v-if="expandedServiceId === svc.serviceId" class="inline h-4 w-4" />
                <ChevronRight v-else class="inline h-4 w-4" />
              </td>
              <td class="p-4 font-medium">{{ svc.serviceId }}</td>
              <td class="p-4">{{ svc.instanceCount ?? '—' }}</td>
              <td class="p-4">
                <Badge
                  v-if="svc.healthyCount != null"
                  :variant="svc.healthyCount > 0 ? 'default' : 'destructive'"
                >
                  {{ svc.healthyCount }}
                </Badge>
                <span v-else>—</span>
              </td>
              <td class="p-4">
                <div class="flex flex-wrap gap-1">
                  <Badge v-for="v in extractVersions(svc)" :key="v" variant="outline">{{ v }}</Badge>
                  <span v-if="!extractVersions(svc).length" class="text-muted-foreground">—</span>
                </div>
              </td>
              <td class="p-4">
                <div class="flex flex-wrap gap-1">
                  <Badge v-for="c in extractClusters(svc)" :key="c" variant="secondary">{{ c }}</Badge>
                  <span v-if="!extractClusters(svc).length" class="text-muted-foreground">—</span>
                </div>
              </td>
            </tr>
            <tr v-if="expandedServiceId === svc.serviceId">
              <td colspan="6" class="bg-muted/10 px-8 py-4">
                <p v-if="instanceLoading" class="text-sm text-muted-foreground">加载实例中…</p>
                <p v-else-if="instanceError" class="text-sm text-destructive">{{ instanceError }}</p>
                <Table v-else-if="instances.length" mobile-title="URI" :mobile-columns="['Host', 'Port', '状态']" class="w-full text-sm">
                  <thead>
                    <tr class="border-b">
                      <th class="px-3 py-2 text-left font-medium">Host</th>
                      <th class="px-3 py-2 text-left font-medium">Port</th>
                      <th class="px-3 py-2 text-left font-medium">URI</th>
                      <th class="px-3 py-2 text-left font-medium">状态</th>
                      <th class="px-3 py-2 text-left font-medium">Metadata</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="inst in instances" :key="inst.instanceId ?? inst.uri" class="border-b last:border-0">
                      <td class="px-3 py-2 font-mono text-xs">{{ inst.host ?? '—' }}</td>
                      <td class="px-3 py-2 font-mono text-xs">{{ inst.port ?? '—' }}</td>
                      <td class="px-3 py-2 font-mono text-xs">{{ inst.uri ?? '—' }}</td>
                      <td class="px-3 py-2">
                        <Badge :variant="inst.healthy !== false ? 'default' : 'destructive'">
                          {{ inst.healthy !== false ? '健康' : '异常' }}
                        </Badge>
                      </td>
                      <td class="px-3 py-2">
                        <div class="flex flex-wrap gap-1">
                          <Badge
                            v-for="entry in metadataEntries(inst)"
                            :key="entry"
                            variant="outline"
                            class="font-mono text-xs"
                          >
                            {{ entry }}
                          </Badge>
                          <span v-if="!metadataEntries(inst).length" class="text-muted-foreground">—</span>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </Table>
                <p v-else class="text-sm text-muted-foreground">暂无实例</p>
              </td>
            </tr>
          </template>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="filteredServices.length" :page-size="pageSize" @change="page = $event" />
    </DataTableShell>
  </div>
</template>
