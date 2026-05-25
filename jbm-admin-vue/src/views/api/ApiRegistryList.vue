<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import { listApis, listResources, type AuthorityApi } from '@/api/authority'

const apis = ref<AuthorityApi[]>([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const serviceFilter = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [apiList, resourceList] = await Promise.all([
      listApis(serviceFilter.value || undefined),
      listResources(),
    ])
    apis.value = apiList.length
      ? apiList
      : resourceList
          .filter((r) => r.authority?.startsWith('API_'))
          .filter((r) => !serviceFilter.value || r.serviceId === serviceFilter.value)
          .map((r) => ({
            apiId: String(r.authorityId ?? ''),
            apiName: r.authority,
            path: r.path,
            serviceId: r.serviceId,
            authorityId: r.authorityId,
            authority: r.authority,
            prefix: r.prefix,
          }))
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
    apis.value = []
  } finally {
    loading.value = false
  }
}

const serviceIds = computed(() => {
  const set = new Set<string>()
  for (const a of apis.value) {
    if (a.serviceId) set.add(a.serviceId)
  }
  return [...set].sort()
})

const filteredApis = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return apis.value.filter((a) => {
    if (serviceFilter.value && a.serviceId !== serviceFilter.value) return false
    if (!kw) return true
    return (
      a.path?.toLowerCase().includes(kw) ||
      a.apiName?.toLowerCase().includes(kw) ||
      a.serviceId?.toLowerCase().includes(kw)
    )
  })
})

const groupedByService = computed(() => {
  const map = new Map<string, AuthorityApi[]>()
  for (const a of filteredApis.value) {
    const key = a.serviceId || '未分类'
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(a)
  }
  return [...map.entries()].sort(([a], [b]) => a.localeCompare(b))
})

const viewMode = ref<'table' | 'group'>('group')

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <PageHeader
      title="API 注册中心"
      description="网关注册的开放 API 接口目录，按微服务聚合；权限分配请前往客户端权限或 API Key。"
    >
      <template #actions>
        <Select v-model="serviceFilter" class="w-40" @change="load">
          <option value="">全部服务</option>
          <option v-for="s in serviceIds" :key="s" :value="s">{{ s }}</option>
        </Select>
        <Input v-model="keyword" placeholder="路径 / 名称" class="w-44" />
        <Button variant="outline" size="sm" @click="viewMode = viewMode === 'table' ? 'group' : 'table'">
          {{ viewMode === 'group' ? '表格视图' : '分组视图' }}
        </Button>
        <Button variant="outline" :disabled="loading" @click="load">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

    <Card>
      <CardContent class="flex gap-6 pt-6">
        <div>
          <p class="text-sm text-muted-foreground">接口总数</p>
          <p class="text-2xl font-bold">{{ filteredApis.length }}</p>
        </div>
        <div>
          <p class="text-sm text-muted-foreground">微服务数</p>
          <p class="text-2xl font-bold">{{ groupedByService.length }}</p>
        </div>
      </CardContent>
    </Card>

    <DataTableShell v-if="viewMode === 'table'" :loading="loading" :empty="!filteredApis.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">路径</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">服务</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in filteredApis" :key="i" class="border-b">
            <td class="p-4 font-mono text-xs">{{ row.path || row.apiName }}</td>
            <td class="p-4">{{ row.apiName || '—' }}</td>
            <td class="p-4">
              <Badge variant="secondary">{{ row.serviceId || '—' }}</Badge>
            </td>
          </tr>
        </tbody>
      </Table>
    </DataTableShell>

    <div v-else class="space-y-6">
      <p v-if="loading" class="text-sm text-muted-foreground">加载中…</p>
      <div
        v-for="[service, items] in groupedByService"
        :key="service"
        class="rounded-lg border"
      >
        <div class="flex items-center justify-between border-b bg-muted/30 px-4 py-2">
          <h3 class="font-medium">{{ service }}</h3>
          <Badge variant="outline">{{ items.length }} 个接口</Badge>
        </div>
        <ul class="divide-y">
          <li
            v-for="(row, i) in items"
            :key="i"
            class="flex flex-wrap items-center gap-2 px-4 py-2 font-mono text-xs"
          >
            <span class="text-foreground">{{ row.path || row.apiName }}</span>
            <span v-if="row.apiName && row.path" class="text-muted-foreground">{{ row.apiName }}</span>
          </li>
        </ul>
      </div>
      <p v-if="!loading && !groupedByService.length" class="text-sm text-muted-foreground">
        暂无接口数据
      </p>
    </div>
  </div>
</template>
