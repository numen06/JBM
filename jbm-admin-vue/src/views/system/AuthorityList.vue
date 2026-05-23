<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { listResources, listApis, listAuthorityMenus } from '@/api/authority'
import type { AuthorityResource, AuthorityApi, AuthorityMenu } from '@/api/authority'

const resources = ref<AuthorityResource[]>([])
const apis = ref<AuthorityApi[]>([])
const menus = ref<AuthorityMenu[]>([])
const loading = ref(true)
const error = ref('')
const apiFilter = ref('')

const filteredApis = ref<AuthorityApi[]>([])

function applyApiFilter() {
  const kw = apiFilter.value.trim().toLowerCase()
  if (!kw) {
    filteredApis.value = apis.value
    return
  }
  filteredApis.value = apis.value.filter(
    (a) =>
      a.path?.toLowerCase().includes(kw) ||
      a.apiName?.toLowerCase().includes(kw) ||
      a.serviceId?.toLowerCase().includes(kw),
  )
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [r, a, m] = await Promise.all([listResources(), listApis(), listAuthorityMenus()])
    resources.value = r
    apis.value = a
    menus.value = m
    applyApiFilter()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <PageHeader title="权限管理" description="Center /authority — 资源、接口、菜单权限只读视图">
      <template #actions>
        <Button variant="outline" :disabled="loading" @click="load">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>
    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

    <div class="grid gap-4 sm:grid-cols-3">
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">资源权限</CardTitle>
        </CardHeader>
        <CardContent>
          <p class="text-2xl font-bold">{{ resources.length }}</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">接口权限</CardTitle>
        </CardHeader>
        <CardContent>
          <p class="text-2xl font-bold">{{ apis.length }}</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">菜单权限</CardTitle>
        </CardHeader>
        <CardContent>
          <p class="text-2xl font-bold">{{ menus.length }}</p>
        </CardContent>
      </Card>
    </div>

    <div class="grid gap-6 xl:grid-cols-3">
      <Card>
        <CardHeader>
          <CardTitle class="text-base">资源权限</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTableShell :loading="loading" :empty="!resources.length">
            <div class="max-h-96 overflow-y-auto">
              <Table>
                <thead>
                  <tr class="border-b bg-muted/50">
                    <th class="h-9 px-3 text-left text-xs font-medium">资源</th>
                    <th class="h-9 px-3 text-left text-xs font-medium">类型</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, i) in resources" :key="i" class="border-b">
                    <td class="p-2 text-sm">{{ row.resourceName || row.resourceId }}</td>
                    <td class="p-2">
                      <Badge variant="outline">{{ row.resourceType || '—' }}</Badge>
                    </td>
                  </tr>
                </tbody>
              </Table>
            </div>
          </DataTableShell>
        </CardContent>
      </Card>

      <Card>
        <CardHeader class="flex flex-row items-center justify-between space-y-0">
          <CardTitle class="text-base">接口权限</CardTitle>
          <Input
            v-model="apiFilter"
            placeholder="筛选路径/服务"
            class="h-8 w-36 text-xs"
            @input="applyApiFilter"
          />
        </CardHeader>
        <CardContent>
          <DataTableShell :loading="loading" :empty="!filteredApis.length">
            <div class="max-h-96 overflow-y-auto">
              <Table>
                <thead>
                  <tr class="border-b bg-muted/50">
                    <th class="h-9 px-3 text-left text-xs font-medium">路径</th>
                    <th class="h-9 px-3 text-left text-xs font-medium">服务</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, i) in filteredApis" :key="i" class="border-b">
                    <td class="p-2 font-mono text-xs">{{ row.path || row.apiName }}</td>
                    <td class="p-2 text-sm text-muted-foreground">{{ row.serviceId }}</td>
                  </tr>
                </tbody>
              </Table>
            </div>
          </DataTableShell>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-base">菜单权限</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTableShell :loading="loading" :empty="!menus.length">
            <div class="max-h-96 overflow-y-auto">
              <Table>
                <thead>
                  <tr class="border-b bg-muted/50">
                    <th class="h-9 px-3 text-left text-xs font-medium">ID</th>
                    <th class="h-9 px-3 text-left text-xs font-medium">名称</th>
                    <th class="h-9 px-3 text-left text-xs font-medium">父级</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in menus" :key="row.menuId" class="border-b">
                    <td class="p-2 text-sm">{{ row.menuId }}</td>
                    <td class="p-2 text-sm">{{ row.menuName }}</td>
                    <td class="p-2 text-sm text-muted-foreground">{{ row.parentId ?? '—' }}</td>
                  </tr>
                </tbody>
              </Table>
            </div>
          </DataTableShell>
        </CardContent>
      </Card>
    </div>
  </div>
</template>
