<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Pencil } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { useFeedback } from '@/composables/useFeedback'
import {
  listRoutes,
  deleteRoute,
  createRoute,
  updateRoute,
  listDiscoveryServices,
} from '@/api/gateway'
import type { GatewayRoute } from '@/api/types'

const keyword = ref('')
const statusFilter = ref('')
const feedback = useFeedback()

const { items, total, page, loading, error, load, pageSize } = usePagedList<GatewayRoute>(
  (p, s) =>
    listRoutes(p, s, {
      keyword: keyword.value || undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
    }),
)

function search() {
  load(1)
}

// ── 服务发现下拉 ──
const discoveryServices = ref<{ serviceId: string; healthyCount?: number }[]>([])
const serviceLoading = ref(false)

async function loadServices() {
  serviceLoading.value = true
  try {
    const list = await listDiscoveryServices()
    discoveryServices.value = list.map((s) => ({
      serviceId: s.serviceId,
      healthyCount: s.healthyCount,
    }))
  } catch {
    discoveryServices.value = []
  } finally {
    serviceLoading.value = false
  }
}

const serviceMap = computed(() => {
  const map = new Map<string, number | undefined>()
  for (const s of discoveryServices.value) {
    map.set(s.serviceId, s.healthyCount)
  }
  return map
})

// ── 路由表单 ──
const routeMode = ref<'service' | 'url'>('service')

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<GatewayRoute>(() => ({
  routeName: '',
  path: '',
  serviceId: '',
  url: '',
  status: 1,
}))

function openCreateRoute() {
  routeMode.value = 'service'
  openCreate()
}

function openEditRoute(row: GatewayRoute) {
  openEdit(row)
  if (row.url && !row.serviceId) {
    routeMode.value = 'url'
  } else {
    routeMode.value = 'service'
  }
}

function onServiceSelect() {
  if (routeMode.value === 'service' && form.value.serviceId) {
    form.value.url = `lb://${form.value.serviceId}`
  }
}

function onRouteModeChange() {
  if (routeMode.value === 'service') {
    form.value.url = form.value.serviceId ? `lb://${form.value.serviceId}` : ''
  } else {
    form.value.serviceId = ''
  }
}

async function handleSave() {
  if (!form.value.routeName?.trim() || !form.value.path?.trim()) {
    formError.value = '路由名称和路径不能为空'
    return
  }
  if (routeMode.value === 'service') {
    if (!form.value.serviceId?.trim()) {
      formError.value = '请选择目标服务'
      return
    }
    form.value.url = `lb://${form.value.serviceId}`
  } else {
    form.value.serviceId = ''
    if (!form.value.url?.trim()) {
      formError.value = '请填写目标 URL'
      return
    }
  }
  saving.value = true
  formError.value = ''
  try {
    if (editing.value && form.value.routeId) {
      await updateRoute(form.value.routeId, form.value)
    } else {
      await createRoute(form.value)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: GatewayRoute) {
  if (!row.routeId) return
  const confirmed = await feedback.confirm({
    title: '确认删除路由',
    message: `确认删除路由 ${row.routeName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteRoute(row.routeId)
  load(page.value)
}

function getHealthyCount(serviceId?: string): number | undefined {
  if (!serviceId) return undefined
  return serviceMap.value.get(serviceId)
}

onMounted(loadServices)
</script>

<template>
  <div>
    <PageHeader title="网关路由" description="GET /gateway/routes">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="名称/路径/服务"
          class="w-44"
          @keyup.enter="search"
        />
        <Select v-model="statusFilter" class="w-28">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </Select>
        <Button variant="outline" @click="search">查询</Button>
        <Button @click="openCreateRoute">
          <Plus class="mr-1 h-4 w-4" />
          新建
        </Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">路径</th>
            <th class="h-10 px-4 text-left font-medium">服务</th>
            <th class="h-10 px-4 text-left font-medium">健康实例</th>
            <th class="h-10 px-4 text-left font-medium">目标 URL</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.routeId" class="border-b">
            <td class="p-4">{{ row.routeId }}</td>
            <td class="p-4">{{ row.routeName }}</td>
            <td class="p-4 font-mono text-xs">{{ row.path }}</td>
            <td class="p-4">
              <Badge v-if="row.serviceId" variant="secondary">{{ row.serviceId }}</Badge>
              <span v-else class="text-muted-foreground">—</span>
            </td>
            <td class="p-4">
              <template v-if="getHealthyCount(row.serviceId) != null">
                <Badge :variant="(getHealthyCount(row.serviceId) ?? 0) > 0 ? 'default' : 'destructive'">
                  {{ getHealthyCount(row.serviceId) }}
                </Badge>
              </template>
              <span v-else class="text-muted-foreground">—</span>
            </td>
            <td class="p-4 font-mono text-xs text-muted-foreground">{{ row.url }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" @click="openEditRoute(row)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑路由' : '新建路由'"
      :saving="saving"
      wide
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="路由名称" required>
        <Input v-model="form.routeName" />
      </FormField>
      <FormField label="匹配路径" required>
        <Input v-model="form.path" placeholder="/api/xxx/**" class="font-mono text-sm" />
      </FormField>
      <FormField label="目标模式">
        <Select v-model="routeMode" @change="onRouteModeChange">
          <option value="service">服务选择</option>
          <option value="url">自定义 URL</option>
        </Select>
      </FormField>
      <template v-if="routeMode === 'service'">
        <FormField label="目标服务" required>
          <Select
            v-model="form.serviceId"
            :disabled="serviceLoading"
            @change="onServiceSelect"
          >
            <option value="">{{ serviceLoading ? '加载中...' : '请选择服务' }}</option>
            <option v-for="s in discoveryServices" :key="s.serviceId" :value="s.serviceId">
              {{ s.serviceId }}{{ s.healthyCount != null ? ` (${s.healthyCount} 健康)` : '' }}
            </option>
          </Select>
        </FormField>
        <p v-if="form.serviceId" class="text-xs text-muted-foreground">
          目标地址：<code class="font-mono">lb://{{ form.serviceId }}</code>
        </p>
      </template>
      <template v-else>
        <FormField label="目标 URL" required>
          <Input v-model="form.url" placeholder="http://host:port" class="font-mono text-sm" />
        </FormField>
        <p class="text-xs text-muted-foreground">填写完整目标地址，适用于转发到非注册中心的服务。</p>
      </template>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
    </CrudDialog>
  </div>
</template>
