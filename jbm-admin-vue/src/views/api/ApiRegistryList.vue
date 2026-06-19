<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Pencil, Trash2, RefreshCw, BookOpen, ShieldCheck, Globe2, KeyRound, Gauge, Network, Lock } from 'lucide-vue-next'
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
  listBaseApis,
  createBaseApi,
  updateBaseApi,
  deleteBaseApi,
  batchDeleteBaseApis,
  batchPatchApiStatus,
  batchPatchApiOpen,
  batchPatchApiAuth,
  batchPatchApiAccessLog,
  listApiServices,
  type BaseApiListQuery,
} from '@/api/baseApi'
import type { BaseApi } from '@/api/types'

const feedback = useFeedback()
const route = useRoute()
const router = useRouter()

const keyword = ref('')
const serviceFilter = ref('')
const pathFilter = ref('')
const methodFilter = ref('')
const statusFilter = ref<number | string>('')
const isOpenFilter = ref<number | string>('')
const isAuthFilter = ref<number | string>('')
const accessLogFilter = ref<number | string>('')

const services = ref<string[]>([])

const selectedIds = ref<Set<number>>(new Set())
const allChecked = computed(() => {
  if (!items.value.length) return false
  return items.value.every((r) => r.apiId != null && selectedIds.value.has(r.apiId))
})

type SummaryCountKey =
  | 'authorityCount'
  | 'apiKeyGrantCount'
  | 'rateLimitPolicyCount'
  | 'ipLimitPolicyCount'

function summaryCount(row: BaseApi, key: SummaryCountKey) {
  const value = row.controlSummary?.[key]
  const numericValue = Number(value ?? 0)
  return Number.isFinite(numericValue) ? numericValue : 0
}

const pageStats = computed(() => {
  const rows = items.value
  return {
    external: rows.filter((row) => row.isOpen === 1).length,
    internal: rows.filter((row) => row.isOpen !== 1).length,
    auth: rows.filter((row) => row.isAuth === true || row.isAuth === 1).length,
    apiKey: rows.filter((row) => summaryCount(row, 'apiKeyGrantCount') > 0).length,
    rateLimit: rows.filter((row) => summaryCount(row, 'rateLimitPolicyCount') > 0).length,
    ipLimit: rows.filter((row) => summaryCount(row, 'ipLimitPolicyCount') > 0).length,
  }
})

function buildQuery(): BaseApiListQuery {
  return {
    keyword: keyword.value || undefined,
    serviceId: serviceFilter.value || undefined,
    path: pathFilter.value || undefined,
    requestMethod: methodFilter.value || undefined,
    status: statusFilter.value !== '' ? statusFilter.value : undefined,
    isOpen: isOpenFilter.value !== '' ? isOpenFilter.value : undefined,
    isAuth: isAuthFilter.value !== '' ? isAuthFilter.value : undefined,
    accessLog:
      accessLogFilter.value !== ''
        ? accessLogFilter.value === 1 || accessLogFilter.value === '1'
          ? 'true'
          : 'false'
        : undefined,
  }
}

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseApi>(
  (_page, _size) => listBaseApis(_page, _size, buildQuery()),
  20,
)

function search() {
  selectedIds.value = new Set()
  load(1)
}

function toggleRow(row: BaseApi) {
  if (row.apiId == null) return
  const next = new Set(selectedIds.value)
  if (next.has(row.apiId)) next.delete(row.apiId)
  else next.add(row.apiId)
  selectedIds.value = next
}

function toggleAll() {
  if (allChecked.value) {
    selectedIds.value = new Set()
  } else {
    const next = new Set(selectedIds.value)
    for (const r of items.value) {
      if (r.apiId != null) next.add(r.apiId)
    }
    selectedIds.value = next
  }
}

watch(page, () => {
  selectedIds.value = new Set()
})

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseApi>(() => ({
  apiCode: '',
  apiName: '',
  serviceId: '',
  path: '',
  requestMethod: 'GET',
  apiCategory: '',
  status: 1,
  isOpen: 0,
  isAuth: false,
  accessLog: true,
  priority: 0,
  businessScope: '',
}))

const methodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']

const displayApiCode = computed(() => {
  const code = form.value.apiCode?.trim() ?? ''
  return code ? `API_${code}` : ''
})

function handleOpenCreate() {
  openCreate()
}

function handleOpenEdit(row: BaseApi) {
  openEdit(row)
}

async function handleSave() {
  const code = form.value.apiCode?.trim()
  const name = form.value.apiName?.trim()
  if (!code) {
    formError.value = 'API 编码不能为空'
    return
  }
  if (!name) {
    formError.value = 'API 名称不能为空'
    return
  }
  if (code.startsWith('API_')) {
    formError.value = '请勿输入 API_ 前缀，系统会自动添加'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    if (editing.value && form.value.apiId) {
      await updateBaseApi(form.value.apiId, form.value)
    } else {
      await createBaseApi(form.value)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseApi) {
  if (!row.apiId) return
  const confirmed = await feedback.confirm({
    title: '确认删除',
    message: `确认删除 API ${row.apiName ?? row.apiId}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  try {
    await deleteBaseApi(row.apiId)
    selectedIds.value.delete(row.apiId)
    load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '删除失败', '删除失败')
  }
}

async function handleBatchDelete() {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  const confirmed = await feedback.confirm({
    title: '批量删除',
    message: `确认删除选中的 ${ids.length} 个 API？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  try {
    await batchDeleteBaseApis(ids)
    selectedIds.value = new Set()
    load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '操作失败', '批量删除失败')
  }
}

async function handleBatchStatus(status: number) {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  const label = status === 1 ? '启用' : '停用'
  const confirmed = await feedback.confirm({
    title: `批量${label}`,
    message: `确认将选中的 ${ids.length} 个 API ${label}？`,
  })
  if (!confirmed) return
  try {
    await batchPatchApiStatus(ids, status)
    load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '操作失败', '操作失败')
  }
}

async function handleBatchOpen(open: boolean) {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  const label = open ? '设为公开' : '设为保护'
  const confirmed = await feedback.confirm({
    title: `批量${label}`,
    message: `确认将选中的 ${ids.length} 个 API ${label}？`,
  })
  if (!confirmed) return
  try {
    await batchPatchApiOpen(ids, open)
    load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '操作失败', '操作失败')
  }
}

async function handleBatchAuth(auth: boolean) {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  const label = auth ? '设为需认证' : '设为免认证'
  const confirmed = await feedback.confirm({
    title: `批量${label}`,
    message: `确认将选中的 ${ids.length} 个 API ${label}？`,
  })
  if (!confirmed) return
  try {
    await batchPatchApiAuth(ids, auth ? 1 : 0)
    load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '操作失败', '操作失败')
  }
}

async function handleBatchAccessLog(accessLog: boolean) {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  const label = accessLog ? '开启访问日志' : '关闭访问日志'
  const confirmed = await feedback.confirm({
    title: `批量${label}`,
    message: `确认将选中的 ${ids.length} 个 API ${label}？`,
  })
  if (!confirmed) return
  try {
    await batchPatchApiAccessLog(ids, accessLog)
    load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '操作失败', '操作失败')
  }
}

async function handleBatchControlMode(mode: 'internal' | 'external') {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  const external = mode === 'external'
  const confirmed = await feedback.confirm({
    title: external ? '转为外部受控资源' : '转为内部受控资源',
    message: external
      ? `确认将选中的 ${ids.length} 个 API 设置为外部开放、需认证并开启访问日志？API Key 授权仍在 API Key 管理中分配。`
      : `确认将选中的 ${ids.length} 个 API 设置为内部保护、需认证并开启访问日志？`,
  })
  if (!confirmed) return
  try {
    await batchPatchApiOpen(ids, external)
    await batchPatchApiAuth(ids, 1)
    await batchPatchApiAccessLog(ids, true)
    load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '操作失败', '受控模式更新失败')
  }
}

async function loadServices() {
  try {
    services.value = await listApiServices()
  } catch {
    services.value = []
  }
}

function goDocs(row: BaseApi) {
  router.push({
    path: '/api/docs',
    query: {
      serviceId: row.serviceId,
      path: row.path,
      method: row.requestMethod,
    },
  })
}

function goApiKeys() {
  router.push('/developer/api-keys')
}

function goRateLimit(row: BaseApi) {
  router.push({
    path: '/gateway/rate-limit',
    query: { apiId: row.apiId, serviceId: row.serviceId, path: row.path },
  })
}

function goIpLimit(row: BaseApi) {
  router.push({
    path: '/gateway/ip-limit',
    query: { apiId: row.apiId, serviceId: row.serviceId, path: row.path },
  })
}

function controlModeLabel(row: BaseApi) {
  const mode = row.controlSummary?.controlMode
  const labels: Record<string, string> = {
    external_api_key: '外部 API Key',
    external_authenticated: '外部认证',
    external_guarded: '外部策略',
    external_public: '外部公开',
    internal_authenticated: '内部认证',
    internal_service: '内部服务',
  }
  return labels[mode ?? ''] ?? (row.isOpen === 1 ? '外部资源' : '内部资源')
}

function controlModeVariant(row: BaseApi) {
  const mode = row.controlSummary?.controlMode ?? ''
  if (mode.includes('api_key') || mode.includes('authenticated')) return 'default'
  if (mode.includes('public')) return 'secondary'
  return 'outline'
}

function applyRouteQuery() {
  const q = route.query
  if (typeof q.serviceId === 'string') serviceFilter.value = q.serviceId
  if (typeof q.path === 'string') pathFilter.value = q.path
  if (typeof q.requestMethod === 'string') methodFilter.value = q.requestMethod
}

onMounted(async () => {
  applyRouteQuery()
  await loadServices()
  if (route.query.serviceId || route.query.path) {
    search()
  }
})
</script>

<template>
  <div>
    <PageHeader
      title="API 资源管理"
      description="维护 base_api 接口资源，控制认证、开放访问、访问日志和授权入口。"
    >
      <template #actions>
        <Select v-model="serviceFilter" class="w-36" @change="search">
          <option value="">全部服务</option>
          <option v-for="s in services" :key="s" :value="s">{{ s }}</option>
        </Select>
        <Select v-model="statusFilter" class="w-24" @change="search">
          <option value="">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
        <Select v-model="isOpenFilter" class="w-24" @change="search">
          <option value="">全部开放</option>
          <option :value="1">公开</option>
          <option :value="0">保护</option>
        </Select>
        <Select v-model="isAuthFilter" class="w-28" @change="search">
          <option value="">全部认证</option>
          <option :value="1">需认证</option>
          <option :value="0">免认证</option>
        </Select>
        <Select v-model="accessLogFilter" class="w-28" @change="search">
          <option value="">全部日志</option>
          <option :value="1">有日志</option>
          <option :value="0">无日志</option>
        </Select>
        <Input v-model="keyword" placeholder="名称/路径/编码" class="w-40" @keyup.enter="search" />
        <Button variant="outline" @click="search">
          <RefreshCw class="mr-1 h-4 w-4" />
          搜索
        </Button>
        <Button @click="handleOpenCreate">
          <Plus class="mr-1 h-4 w-4" />
          新建
        </Button>
      </template>
    </PageHeader>

    <div v-if="selectedIds.size" class="mb-3 flex flex-wrap items-center gap-2">
      <span class="text-sm text-muted-foreground">已选 {{ selectedIds.size }} 项：</span>
      <Button variant="outline" size="sm" @click="handleBatchControlMode('internal')">
        <ShieldCheck class="mr-1 h-3.5 w-3.5" />
        转内部受控
      </Button>
      <Button variant="outline" size="sm" @click="handleBatchControlMode('external')">
        <Globe2 class="mr-1 h-3.5 w-3.5" />
        转外部受控
      </Button>
      <Button variant="outline" size="sm" @click="handleBatchStatus(1)">批量启用</Button>
      <Button variant="outline" size="sm" @click="handleBatchStatus(0)">批量停用</Button>
      <Button variant="outline" size="sm" @click="handleBatchOpen(true)">批量公开</Button>
      <Button variant="outline" size="sm" @click="handleBatchOpen(false)">批量保护</Button>
      <Button variant="outline" size="sm" @click="handleBatchAuth(true)">批量需认证</Button>
      <Button variant="outline" size="sm" @click="handleBatchAuth(false)">批量免认证</Button>
      <Button variant="outline" size="sm" @click="handleBatchAccessLog(true)">批量开日志</Button>
      <Button variant="outline" size="sm" @click="handleBatchAccessLog(false)">批量关日志</Button>
      <Button variant="destructive" size="sm" @click="handleBatchDelete">
        <Trash2 class="mr-1 h-3.5 w-3.5" />
        批量删除
      </Button>
    </div>

    <div class="mb-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-6">
      <div class="rounded border bg-background px-3 py-2">
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <Globe2 class="h-3.5 w-3.5" />
          外部资源
        </div>
        <div class="mt-1 text-lg font-semibold">{{ pageStats.external }}</div>
      </div>
      <div class="rounded border bg-background px-3 py-2">
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <ShieldCheck class="h-3.5 w-3.5" />
          内部资源
        </div>
        <div class="mt-1 text-lg font-semibold">{{ pageStats.internal }}</div>
      </div>
      <div class="rounded border bg-background px-3 py-2">
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <Lock class="h-3.5 w-3.5" />
          需认证
        </div>
        <div class="mt-1 text-lg font-semibold">{{ pageStats.auth }}</div>
      </div>
      <div class="rounded border bg-background px-3 py-2">
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <KeyRound class="h-3.5 w-3.5" />
          API Key
        </div>
        <div class="mt-1 text-lg font-semibold">{{ pageStats.apiKey }}</div>
      </div>
      <div class="rounded border bg-background px-3 py-2">
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <Gauge class="h-3.5 w-3.5" />
          限流
        </div>
        <div class="mt-1 text-lg font-semibold">{{ pageStats.rateLimit }}</div>
      </div>
      <div class="rounded border bg-background px-3 py-2">
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <Network class="h-3.5 w-3.5" />
          IP 策略
        </div>
        <div class="mt-1 text-lg font-semibold">{{ pageStats.ipLimit }}</div>
      </div>
    </div>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 w-10 px-2 text-center font-medium">
              <input type="checkbox" :checked="allChecked" @change="toggleAll" />
            </th>
            <th class="h-10 px-3 text-left font-medium">ID</th>
            <th class="h-10 px-3 text-left font-medium">权限标识</th>
            <th class="h-10 px-3 text-left font-medium">名称</th>
            <th class="h-10 px-3 text-left font-medium">服务</th>
            <th class="h-10 px-3 text-left font-medium">路径</th>
            <th class="h-10 px-3 text-left font-medium">方法</th>
            <th class="h-10 px-3 text-left font-medium">受控模式</th>
            <th class="h-10 px-3 text-left font-medium">治理绑定</th>
            <th class="h-10 px-3 text-center font-medium">状态</th>
            <th class="h-10 px-3 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.apiId" class="border-b">
            <td class="w-10 p-2 text-center">
              <input
                type="checkbox"
                :checked="row.apiId != null && selectedIds.has(row.apiId)"
                @change="toggleRow(row)"
              />
            </td>
            <td class="p-3">{{ row.apiId }}</td>
            <td class="p-3 font-mono text-xs">API_{{ row.apiCode }}</td>
            <td class="p-3">{{ row.apiName }}</td>
            <td class="p-3">
              <Badge variant="secondary">{{ row.serviceId || '—' }}</Badge>
            </td>
            <td class="p-3 font-mono text-xs">{{ row.path }}</td>
            <td class="p-3">
              <Badge variant="outline">{{ row.requestMethod || '—' }}</Badge>
            </td>
            <td class="p-3">
              <div class="flex flex-wrap items-center gap-1">
                <Badge :variant="controlModeVariant(row)">
                  {{ controlModeLabel(row) }}
                </Badge>
                <Badge :variant="row.accessLog ? 'outline' : 'secondary'">
                  {{ row.accessLog ? '日志' : '无日志' }}
                </Badge>
              </div>
            </td>
            <td class="p-3">
              <div class="flex flex-wrap items-center gap-1">
                <Badge variant="outline">权限 {{ summaryCount(row, 'authorityCount') }}</Badge>
                <Badge
                  :variant="summaryCount(row, 'apiKeyGrantCount') > 0 ? 'default' : 'secondary'"
                >
                  Key {{ summaryCount(row, 'apiKeyGrantCount') }}
                </Badge>
                <Badge
                  :variant="summaryCount(row, 'rateLimitPolicyCount') > 0 ? 'default' : 'secondary'"
                >
                  限流 {{ summaryCount(row, 'rateLimitPolicyCount') }}
                </Badge>
                <Badge
                  :variant="summaryCount(row, 'ipLimitPolicyCount') > 0 ? 'default' : 'secondary'"
                >
                  IP {{ summaryCount(row, 'ipLimitPolicyCount') }}
                </Badge>
              </div>
            </td>
            <td class="p-3 text-center">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </Badge>
            </td>
            <td class="p-3 text-right space-x-1">
              <Button variant="outline" size="sm" title="查看文档" @click="goDocs(row)">
                <BookOpen class="h-3.5 w-3.5" />
              </Button>
              <Button variant="outline" size="sm" title="API Key 授权" @click="goApiKeys">
                <KeyRound class="h-3.5 w-3.5" />
              </Button>
              <Button variant="outline" size="sm" title="限流策略" @click="goRateLimit(row)">
                <Gauge class="h-3.5 w-3.5" />
              </Button>
              <Button variant="outline" size="sm" title="IP 策略" @click="goIpLimit(row)">
                <Network class="h-3.5 w-3.5" />
              </Button>
              <Button variant="outline" size="sm" @click="handleOpenEdit(row)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar v-if="total > 0" :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑 API' : '新建 API'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="权限标识">
        <Input :model-value="displayApiCode" disabled class="bg-muted" />
      </FormField>
      <FormField label="API 编码" required>
        <Input v-model="form.apiCode" placeholder="不含 API_ 前缀" :disabled="editing" />
      </FormField>
      <FormField label="API 名称" required>
        <Input v-model="form.apiName" />
      </FormField>
      <FormField label="所属服务" required>
        <Select v-model="form.serviceId">
          <option value="">请选择</option>
          <option v-for="s in services" :key="s" :value="s">{{ s }}</option>
        </Select>
      </FormField>
      <FormField label="路径" required>
        <Input v-model="form.path" placeholder="/api/xxx" />
      </FormField>
      <FormField label="请求方法">
        <Select v-model="form.requestMethod">
          <option v-for="m in methodOptions" :key="m" :value="m">{{ m }}</option>
        </Select>
      </FormField>
      <FormField label="分类">
        <Input v-model="form.apiCategory" placeholder="如：业务接口、系统接口" />
      </FormField>
      <FormField label="业务范围">
        <Input v-model="form.businessScope" placeholder="如：用户、订单" />
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
      <FormField label="是否开放">
        <Select v-model="form.isOpen">
          <option :value="1">公开</option>
          <option :value="0">保护</option>
        </Select>
      </FormField>
      <FormField label="需要认证">
        <Select
          :model-value="form.isAuth ? 1 : 0"
          @update:model-value="(v) => { form.isAuth = Number(v) === 1 }"
        >
          <option :value="1">是</option>
          <option :value="0">否</option>
        </Select>
      </FormField>
      <FormField label="访问日志">
        <Select
          :model-value="form.accessLog ? 1 : 0"
          @update:model-value="(v) => { form.accessLog = Number(v) === 1 }"
        >
          <option :value="1">开启</option>
          <option :value="0">关闭</option>
        </Select>
      </FormField>
      <FormField label="优先级">
        <Input v-model="form.priority" type="number" />
      </FormField>
    </CrudDialog>
  </div>
</template>
