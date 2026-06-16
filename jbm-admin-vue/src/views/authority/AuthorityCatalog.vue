<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RefreshCw, Search } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import {
  listAuthorityMenus,
  listApis,
  listAuthorityCatalog,
  listResources,
  type AuthorityApi,
  type AuthorityMenu,
  type AuthorityResource,
  type OpenAuthority,
} from '@/api/authority'
import { listActions } from '@/api/action'
import { toSnowflakeIdString } from '@/lib/snowflakeId'
import type { BaseAction } from '@/api/types'

type TabId = 'apis' | 'pages'
type ApiAccessFilter = 'all' | 'protected' | 'open'
type CatalogApiRow = AuthorityApi & Pick<AuthorityResource, 'isAuth' | 'isOpen' | 'status'>

const route = useRoute()
const router = useRouter()
const activeTab = computed<TabId>(() => (route.path.endsWith('/pages') ? 'pages' : 'apis'))
const tabs: { id: TabId; label: string }[] = [
  { id: 'apis', label: 'API 权限' },
  { id: 'pages', label: '页面权限' },
]

const menus = ref<AuthorityMenu[]>([])
const actions = ref<BaseAction[]>([])
const apis = ref<AuthorityApi[]>([])
const resources = ref<AuthorityResource[]>([])
const apiCatalog = ref<OpenAuthority[]>([])
const loading = ref(true)
const error = ref('')
const filter = ref('')
const serviceFilter = ref('')
const apiAccessFilter = ref<ApiAccessFilter>('all')
const listPage = ref(1)
const listPageSize = 50

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [m, actPage, a, catalogApi, resourceList] = await Promise.all([
      listAuthorityMenus(),
      listActions(undefined, 1, 500),
      listApis(),
      listAuthorityCatalog('2'),
      listResources(),
    ])
    menus.value = m
    actions.value = actPage.contents ?? []
    apis.value = a
    apiCatalog.value = catalogApi ?? []
    resources.value = resourceList ?? []
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

const actionsByMenu = computed(() => {
  const map = new Map<string, BaseAction[]>()
  for (const action of actions.value) {
    if (action.menuId == null) continue
    const key = toSnowflakeIdString(action.menuId)
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(action)
  }
  return map
})

const filteredPageMenus = computed(() => {
  const kw = filter.value.trim().toLowerCase()
  return menus.value.filter((m) => {
    if (!kw) return true
    const menuHit =
      m.menuName?.toLowerCase().includes(kw) ||
      m.menuCode?.toLowerCase().includes(kw) ||
      m.path?.toLowerCase().includes(kw)
    if (menuHit) return true
    const menuActs = m.menuId ? actionsByMenu.value.get(toSnowflakeIdString(m.menuId)) ?? [] : []
    return menuActs.some(
      (a) =>
        a.actionName?.toLowerCase().includes(kw) ||
        a.actionCode?.toLowerCase().includes(kw),
    )
  })
})

const apiRows = computed<CatalogApiRow[]>(() => {
  if (apis.value.length) {
    return apis.value.map((api) => {
      const meta = resources.value.find((r) => String(r.authorityId) === String(api.authorityId))
      return { ...api, isAuth: meta?.isAuth, isOpen: meta?.isOpen, status: meta?.status }
    })
  }
  return resources.value
    .filter((r) => r.authority?.startsWith('API_'))
    .map((r) => ({
      apiId: String(r.authorityId ?? ''),
      apiName: r.authority,
      path: r.path,
      serviceId: r.serviceId,
      authorityId: r.authorityId,
      authority: r.authority,
      prefix: r.prefix,
      isAuth: r.isAuth,
      isOpen: r.isOpen,
      status: r.status,
    }))
})

const services = computed(() => {
  const set = new Set<string>()
  for (const row of apiRows.value) {
    if (row.serviceId) set.add(row.serviceId)
  }
  return [...set].sort((a, b) => a.localeCompare(b))
})

const filteredApis = computed(() => {
  const kw = filter.value.trim().toLowerCase()
  return apiRows.value.filter((a) => {
    if (serviceFilter.value && a.serviceId !== serviceFilter.value) return false
    if (apiAccessFilter.value === 'protected' && a.isOpen === 1) return false
    if (apiAccessFilter.value === 'open' && a.isOpen !== 1) return false
    if (!kw) return true
    return (
      a.path?.toLowerCase().includes(kw) ||
      a.apiName?.toLowerCase().includes(kw) ||
      a.serviceId?.toLowerCase().includes(kw) ||
      a.authority?.toLowerCase().includes(kw)
    )
  })
})

const activeTotal = computed(() => {
  if (activeTab.value === 'apis') return filteredApis.value.length
  return filteredPageMenus.value.length
})

const pagedPageMenus = computed(() => paginate(filteredPageMenus.value))
const pagedApis = computed(() => paginate(filteredApis.value))

const stats = computed(() => {
  const apiTotal = apiRows.value.length
  return {
    menus: menus.value.length,
    actions: actions.value.length,
    apis: apiTotal,
    services: services.value.length,
    openApis: apiRows.value.filter((row) => row.isOpen === 1).length,
    protectedApis: apiRows.value.filter((row) => row.isOpen !== 1).length,
  }
})

function riskVariant(row: CatalogApiRow) {
  const path = row.path?.toLowerCase() ?? ''
  if (/(delete|remove|reset|logout|kickout|expire)/.test(path)) return 'destructive'
  if (/(save|update|grant|put|batch)/.test(path)) return 'secondary'
  return 'outline'
}

function riskLabel(row: CatalogApiRow) {
  const path = row.path?.toLowerCase() ?? ''
  if (/(delete|remove|reset|logout|kickout|expire)/.test(path)) return '高风险'
  if (/(save|update|grant|put|batch)/.test(path)) return '写操作'
  return '只读/普通'
}

function paginate<T>(rows: T[]) {
  const start = (listPage.value - 1) * listPageSize
  return rows.slice(start, start + listPageSize)
}

function switchTab(tab: TabId) {
  router.push({ name: tab === 'apis' ? 'authority-catalog-apis' : 'authority-catalog-pages' })
}

watch([activeTab, filter, serviceFilter, apiAccessFilter], () => {
  listPage.value = 1
})

watch(activeTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / listPageSize))
  if (listPage.value > maxPage) listPage.value = maxPage
})

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <PageHeader
      title="权限资源"
      description="管理员用于确认系统可分配的 API 权限与页面权限（菜单 + 按钮 ACTION_*）。"
    >
      <template #actions>
        <Select v-if="activeTab === 'apis'" v-model="serviceFilter" class="w-52">
          <option value="">全部服务</option>
          <option v-for="service in services" :key="service" :value="service">
            {{ service }}
          </option>
        </Select>
        <Select v-if="activeTab === 'apis'" v-model="apiAccessFilter" class="w-32">
          <option value="all">全部 API</option>
          <option value="protected">需授权</option>
          <option value="open">公开访问</option>
        </Select>
        <Input v-model="filter" placeholder="搜索路径 / 名称 / 标识" class="w-56" />
        <Button variant="outline" :disabled="loading" @click="load">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

    <div class="grid gap-4 md:grid-cols-4">
      <Card>
        <CardContent class="pt-6">
          <p class="text-sm text-muted-foreground">API 资源</p>
          <p class="text-2xl font-bold">{{ stats.apis }}</p>
          <p class="text-xs text-muted-foreground">{{ stats.services }} 个服务</p>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <p class="text-sm text-muted-foreground">需授权 API</p>
          <p class="text-2xl font-bold">{{ stats.protectedApis }}</p>
          <p class="text-xs text-muted-foreground">授权清单生效范围</p>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <p class="text-sm text-muted-foreground">页面权限</p>
          <p class="text-2xl font-bold">{{ stats.menus }} 菜单</p>
          <p class="text-xs text-muted-foreground">{{ stats.actions }} 个按钮</p>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <p class="text-sm text-muted-foreground">公开 API</p>
          <p class="text-2xl font-bold">{{ stats.openApis }}</p>
          <p class="text-xs text-muted-foreground">无需授权即可访问</p>
        </CardContent>
      </Card>
    </div>

    <div class="flex flex-wrap items-center gap-2 border-b pb-2">
      <Button
        v-for="t in tabs"
        :key="t.id"
        :variant="activeTab === t.id ? 'default' : 'outline'"
        size="sm"
        @click="switchTab(t.id)"
      >
        {{ t.label }}
      </Button>
      <span class="ml-auto inline-flex items-center gap-1 text-sm text-muted-foreground">
        <Search class="h-4 w-4" />
        当前匹配 {{ activeTotal }} 条
      </span>
    </div>

    <Card v-show="activeTab === 'pages'">
      <CardContent class="pt-6">
        <DataTableShell :loading="loading" :empty="!filteredPageMenus.length">
          <div class="max-h-[36rem] space-y-3 overflow-y-auto">
            <div
              v-for="menu in pagedPageMenus"
              :key="menu.menuId"
              class="rounded border px-3 py-2"
            >
              <div class="flex flex-wrap items-center gap-2 text-sm font-medium">
                <span>{{ menu.menuName }}</span>
                <span class="font-mono text-xs font-normal text-muted-foreground">{{ menu.path || '—' }}</span>
                <Badge variant="outline" class="font-mono text-xs">MENU_{{ menu.menuCode }}</Badge>
              </div>
              <div
                v-if="menu.menuId && (actionsByMenu.get(toSnowflakeIdString(menu.menuId))?.length ?? 0) > 0"
                class="mt-2 ml-2 flex flex-wrap gap-2"
              >
                <Badge
                  v-for="act in actionsByMenu.get(toSnowflakeIdString(menu.menuId))"
                  :key="act.actionId"
                  variant="secondary"
                  class="font-normal"
                >
                  {{ act.actionName }}
                  <span class="ml-1 font-mono text-xs">ACTION_{{ act.actionCode }}</span>
                </Badge>
              </div>
              <p v-else class="mt-1 text-xs text-muted-foreground">暂无按钮权限</p>
            </div>
          </div>
          <PaginationBar
            :page="listPage"
            :total="filteredPageMenus.length"
            :page-size="listPageSize"
            @change="listPage = $event"
          />
        </DataTableShell>
      </CardContent>
    </Card>

    <Card v-show="activeTab === 'apis'">
      <CardContent class="pt-6">
        <div class="mb-3 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <span>可授权目录 {{ apiCatalog.length }} 条</span>
          <Badge variant="outline">公开 {{ stats.openApis }}</Badge>
          <Badge variant="outline">需授权 {{ stats.protectedApis }}</Badge>
        </div>
        <DataTableShell :loading="loading" :empty="!filteredApis.length">
          <div class="max-h-[36rem] overflow-y-auto">
            <Table>
              <thead>
                <tr class="border-b bg-muted/50">
                  <th class="h-9 px-3 text-left text-xs font-medium">API 路径</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">服务</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">访问</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">风险</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">权限标识</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in pagedApis" :key="row.apiId || i" class="border-b">
                  <td class="p-2 font-mono text-xs">{{ row.path || row.apiName || '-' }}</td>
                  <td class="p-2">
                    <Badge variant="secondary">{{ row.serviceId || '-' }}</Badge>
                  </td>
                  <td class="p-2">
                    <Badge :variant="row.isOpen === 1 ? 'outline' : 'default'">
                      {{ row.isOpen === 1 ? '公开' : '需授权' }}
                    </Badge>
                  </td>
                  <td class="p-2">
                    <Badge :variant="riskVariant(row)">{{ riskLabel(row) }}</Badge>
                  </td>
                  <td class="p-2 font-mono text-xs text-muted-foreground">{{ row.authority || row.apiName }}</td>
                </tr>
              </tbody>
            </Table>
          </div>
          <PaginationBar :page="listPage" :total="filteredApis.length" :page-size="listPageSize" @change="listPage = $event" />
        </DataTableShell>
      </CardContent>
    </Card>
  </div>
</template>
