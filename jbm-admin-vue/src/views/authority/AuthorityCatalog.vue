<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import {
  listAuthorityMenus,
  listApis,
  listAuthorityCatalog,
  type AuthorityApi,
  type AuthorityMenu,
  type OpenAuthority,
} from '@/api/authority'
import { listActions } from '@/api/action'
import type { BaseAction } from '@/api/types'

type TabId = 'menus' | 'actions' | 'apis'

const activeTab = ref<TabId>('menus')
const tabs: { id: TabId; label: string }[] = [
  { id: 'menus', label: '菜单权限' },
  { id: 'actions', label: '按钮权限' },
  { id: 'apis', label: 'API 权限' },
]

const menus = ref<AuthorityMenu[]>([])
const actions = ref<BaseAction[]>([])
const apis = ref<AuthorityApi[]>([])
const apiCatalog = ref<OpenAuthority[]>([])
const loading = ref(true)
const error = ref('')
const filter = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [m, actPage, a, catalogApi] = await Promise.all([
      listAuthorityMenus(),
      listActions(undefined, 1, 500),
      listApis(),
      listAuthorityCatalog('2'),
    ])
    menus.value = m
    actions.value = actPage.contents ?? []
    apis.value = a
    apiCatalog.value = catalogApi ?? []
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

const filteredMenus = computed(() => {
  const kw = filter.value.trim().toLowerCase()
  if (!kw) return menus.value
  return menus.value.filter(
    (m) =>
      m.menuName?.toLowerCase().includes(kw) ||
      m.menuCode?.toLowerCase().includes(kw) ||
      m.path?.toLowerCase().includes(kw),
  )
})

const filteredActions = computed(() => {
  const kw = filter.value.trim().toLowerCase()
  if (!kw) return actions.value
  return actions.value.filter(
    (a) =>
      a.actionName?.toLowerCase().includes(kw) ||
      a.actionCode?.toLowerCase().includes(kw),
  )
})

const filteredApis = computed(() => {
  const kw = filter.value.trim().toLowerCase()
  if (!kw) return apis.value
  return apis.value.filter(
    (a) =>
      a.path?.toLowerCase().includes(kw) ||
      a.apiName?.toLowerCase().includes(kw) ||
      a.serviceId?.toLowerCase().includes(kw),
  )
})

const stats = computed(() => ({
  menus: menus.value.length,
  actions: actions.value.length,
  apis: apis.value.length,
}))

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <PageHeader
      title="权限目录"
      description="系统中可分配的菜单、按钮（ACTION_*）与 API 权限元数据；实际授权请在角色/用户/客户端权限中操作。"
    >
      <template #actions>
        <Input
          v-model="filter"
          placeholder="搜索名称、编码、路径"
          class="w-48"
        />
        <Button variant="outline" :disabled="loading" @click="load">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

    <div class="grid gap-4 sm:grid-cols-3">
      <Card>
        <CardContent class="pt-6">
          <p class="text-sm text-muted-foreground">菜单权限</p>
          <p class="text-2xl font-bold">{{ stats.menus }}</p>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <p class="text-sm text-muted-foreground">按钮权限</p>
          <p class="text-2xl font-bold">{{ stats.actions }}</p>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <p class="text-sm text-muted-foreground">API 接口</p>
          <p class="text-2xl font-bold">{{ stats.apis }}</p>
        </CardContent>
      </Card>
    </div>

    <div class="flex flex-wrap gap-2 border-b pb-2">
      <Button
        v-for="t in tabs"
        :key="t.id"
        :variant="activeTab === t.id ? 'default' : 'outline'"
        size="sm"
        @click="activeTab = t.id"
      >
        {{ t.label }}
      </Button>
    </div>

    <Card v-show="activeTab === 'menus'">
      <CardContent class="pt-6">
        <DataTableShell :loading="loading" :empty="!filteredMenus.length">
          <div class="max-h-[32rem] overflow-y-auto">
            <Table>
              <thead>
                <tr class="border-b bg-muted/50">
                  <th class="h-9 px-3 text-left text-xs font-medium">ID</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">编码</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">名称</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">路径</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">父级</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in filteredMenus" :key="row.menuId" class="border-b">
                  <td class="p-2 text-sm">{{ row.menuId }}</td>
                  <td class="p-2 font-mono text-xs">{{ row.menuCode || '—' }}</td>
                  <td class="p-2 text-sm">{{ row.menuName }}</td>
                  <td class="p-2 font-mono text-xs text-muted-foreground">{{ row.path || '—' }}</td>
                  <td class="p-2 text-sm text-muted-foreground">{{ row.parentId ?? '—' }}</td>
                </tr>
              </tbody>
            </Table>
          </div>
        </DataTableShell>
      </CardContent>
    </Card>

    <Card v-show="activeTab === 'actions'">
      <CardContent class="pt-6">
        <DataTableShell :loading="loading" :empty="!filteredActions.length">
          <div class="max-h-[32rem] overflow-y-auto">
            <Table>
              <thead>
                <tr class="border-b bg-muted/50">
                  <th class="h-9 px-3 text-left text-xs font-medium">ID</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">按钮</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">权限标识</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">所属菜单</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in filteredActions" :key="row.actionId" class="border-b">
                  <td class="p-2 text-sm">{{ row.actionId }}</td>
                  <td class="p-2 text-sm">{{ row.actionName }}</td>
                  <td class="p-2">
                    <Badge variant="outline" class="font-mono text-xs">
                      ACTION_{{ row.actionCode }}
                    </Badge>
                  </td>
                  <td class="p-2 text-sm text-muted-foreground">{{ row.menuId ?? '—' }}</td>
                </tr>
              </tbody>
            </Table>
          </div>
        </DataTableShell>
      </CardContent>
    </Card>

    <Card v-show="activeTab === 'apis'">
      <CardContent class="pt-6">
        <p v-if="apiCatalog.length" class="mb-3 text-xs text-muted-foreground">
          可授权 API 目录项 {{ apiCatalog.length }} 条；下方为网关注册的全部接口路径。
        </p>
        <DataTableShell :loading="loading" :empty="!filteredApis.length">
          <div class="max-h-[32rem] overflow-y-auto">
            <Table>
              <thead>
                <tr class="border-b bg-muted/50">
                  <th class="h-9 px-3 text-left text-xs font-medium">路径</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">名称</th>
                  <th class="h-9 px-3 text-left text-xs font-medium">服务</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in filteredApis" :key="i" class="border-b">
                  <td class="p-2 font-mono text-xs">{{ row.path || row.apiName }}</td>
                  <td class="p-2 text-sm">{{ row.apiName || '—' }}</td>
                  <td class="p-2">
                    <Badge variant="secondary">{{ row.serviceId || '—' }}</Badge>
                  </td>
                </tr>
              </tbody>
            </Table>
          </div>
        </DataTableShell>
      </CardContent>
    </Card>
  </div>
</template>
