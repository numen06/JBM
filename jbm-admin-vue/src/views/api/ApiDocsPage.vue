<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  RefreshCw,
  Download,
  Send,
  ExternalLink,
  Upload,
  Search,
  Eye,
} from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Badge from '@/components/ui/Badge.vue'
import { useFeedback } from '@/composables/useFeedback'
import { useAuthStore } from '@/stores/auth'
import {
  listOpenApiSources,
  listOpenApiOperations,
  getOpenApiOperation,
  syncOpenApiDocs,
  testOpenApiOperation,
  saveOpenApiUseCase,
  exportOpenApiDocs,
  publishOpenApiDocs,
} from '@/api/openapiDocs'
import type { OpenApiOperationDetail, OpenApiOperationView, OpenApiSource, OpenApiTestResult } from '@/api/types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

const feedback = useFeedback()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const sources = ref<OpenApiSource[]>([])
const selectedServiceId = ref('')
const keyword = ref('')
const methodFilter = ref('')
const isOpenFilter = ref<number | string>('')
const isAuthFilter = ref<number | string>('')
const syncStateFilter = ref('')
const linkedFilter = ref<number | string>('')

const operations = ref<OpenApiOperationView[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const loading = ref(false)
const syncing = ref(false)

const selectedOperationId = ref<number | null>(null)
const operationDetail = ref<OpenApiOperationDetail | null>(null)
const checkedIds = ref<Set<number>>(new Set())
const detailTab = ref('overview')

type TestParam = {
  name: string
  in: string
  type: string
  required: boolean
  description?: string
  value: string
}

const testPathParams = ref<TestParam[]>([])
const testQueryParams = ref<TestParam[]>([])
const testHeaderParams = ref<TestParam[]>([])
const testBody = ref('')
const testConfirm = ref(false)
const testResult = ref<string>('')
const lastTestResult = ref<OpenApiTestResult | null>(null)
const testing = ref(false)
const savingUseCase = ref(false)
const useCaseName = ref('')
const useCaseDescription = ref('')
const previewing = ref(false)
const previewUrl = ref('')

const publishTitle = ref('JBM Open API')
const publishVersion = ref('1.0.0')
const publishSummary = ref('')
const publishDocKey = ref('default')

const selectedSource = computed(() =>
  sources.value.find((s) => s.serviceId === selectedServiceId.value),
)

const selectedMethod = computed(() =>
  (operationDetail.value?.method ?? operationDetail.value?.requestMethod ?? '').toUpperCase(),
)

function sourceScore(source: OpenApiSource) {
  if ((source.operationTotal ?? 0) > 0) return 0
  if (source.syncStatus === 'SUCCESS') return 1
  if (source.syncStatus === 'PENDING') return 2
  return 3
}

function pickDefaultSource(list: OpenApiSource[]) {
  return [...list].sort((a, b) => {
    const rank = sourceScore(a) - sourceScore(b)
    if (rank !== 0) return rank
    return (a.serviceId || '').localeCompare(b.serviceId || '')
  })[0]
}

function sourceLabel(source: OpenApiSource) {
  if (!source.title || source.title === source.serviceId) return source.serviceId
  return `${source.title} (${source.serviceId})`
}

const allCheckedOnPage = computed(() => {
  if (!operations.value.length) return false
  return operations.value.every((op) => op.operationId != null && checkedIds.value.has(op.operationId))
})

function methodBadgeClass(method?: string) {
  const map: Record<string, string> = {
    GET: 'bg-emerald-100 text-emerald-800',
    POST: 'bg-blue-100 text-blue-800',
    PUT: 'bg-amber-100 text-amber-800',
    PATCH: 'bg-orange-100 text-orange-800',
    DELETE: 'bg-red-100 text-red-800',
  }
  return map[method ?? ''] ?? 'bg-muted text-foreground'
}

function buildQuery() {
  return {
    serviceId: selectedServiceId.value || undefined,
    keyword: keyword.value || undefined,
    method: methodFilter.value || undefined,
    isOpen: isOpenFilter.value !== '' ? isOpenFilter.value : undefined,
    isAuth: isAuthFilter.value !== '' ? isAuthFilter.value : undefined,
    syncState: syncStateFilter.value || undefined,
    linked:
      linkedFilter.value === '1' ? true : linkedFilter.value === '0' ? false : undefined,
  }
}

async function loadSources() {
  sources.value = await listOpenApiSources()
  if (!selectedServiceId.value && sources.value.length) {
    selectedServiceId.value = pickDefaultSource(sources.value)?.serviceId ?? sources.value[0].serviceId
  }
}

async function loadOperations(p = page.value) {
  if (!selectedServiceId.value) {
    operations.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await listOpenApiOperations(p, pageSize.value, buildQuery())
    operations.value = data.contents ?? []
    total.value = data.total ?? 0
    page.value = p
    if (selectedOperationId.value) {
      const stillExists = operations.value.some((op) => op.operationId === selectedOperationId.value)
      if (!stillExists) {
        selectedOperationId.value = null
        operationDetail.value = null
      }
    }
    applyRouteSelection()
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '加载接口列表失败')
  } finally {
    loading.value = false
  }
}

async function selectOperation(op: OpenApiOperationView) {
  if (!op.operationId) return
  selectedOperationId.value = op.operationId
  try {
    operationDetail.value = await getOpenApiOperation(op.operationId)
    detailTab.value = 'overview'
    resetTestForm(operationDetail.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '加载接口详情失败')
  }
}

function resetTestForm(detail: OpenApiOperationDetail) {
  const params = parseJsonBlock(detail.parametersJson)
  const list = Array.isArray(params) ? params : []
  const tenantId = authStore.tenantId || '0'
  testPathParams.value = list.filter((p) => p?.in === 'path').map(toTestParam)
  testQueryParams.value = list.filter((p) => p?.in === 'query').map(toTestParam)
  testHeaderParams.value = list.filter((p) => p?.in === 'header').map(toTestParam)
  if (!testHeaderParams.value.some((p) => p.name.toLowerCase() === 'tenantid')) {
    testHeaderParams.value.unshift({
      name: 'tenantId',
      in: 'header',
      type: 'string',
      required: false,
      description: '租户标识',
      value: tenantId,
    })
  }
  const method = (detail.method ?? detail.requestMethod ?? 'GET').toUpperCase()
  testBody.value = ['GET', 'HEAD', 'OPTIONS'].includes(method) ? '' : buildRequestBodySample(detail)
  testConfirm.value = false
  testResult.value = ''
  lastTestResult.value = null
  useCaseName.value = detail.summary || `${method} ${detail.path || ''}`
  useCaseDescription.value = ''
}

function toTestParam(raw: Record<string, unknown>): TestParam {
  return {
    name: String(raw.name ?? ''),
    in: String(raw.in ?? ''),
    type: parameterType(raw),
    required: raw.required === true,
    description: typeof raw.description === 'string' ? raw.description : '',
    value: defaultParamValue(raw),
  }
}

async function syncDocs() {
  syncing.value = true
  try {
    const results = await syncOpenApiDocs(
      selectedServiceId.value ? [selectedServiceId.value] : undefined,
    )
    const failed = results.filter((r) => r.syncStatus === 'FAILED')
    if (failed.length) {
      feedback.toast.warning(`同步完成，${failed.length} 个服务失败`)
    } else {
      feedback.toast.success('同步完成')
    }
    await loadSources()
    await loadOperations(1)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '同步失败')
  } finally {
    syncing.value = false
  }
}

function toggleCheck(op: OpenApiOperationView) {
  if (!op.operationId) return
  const next = new Set(checkedIds.value)
  if (next.has(op.operationId)) next.delete(op.operationId)
  else next.add(op.operationId)
  checkedIds.value = next
}

function toggleCheckAll() {
  if (allCheckedOnPage.value) {
    for (const op of operations.value) {
      if (op.operationId) checkedIds.value.delete(op.operationId)
    }
    checkedIds.value = new Set(checkedIds.value)
  } else {
    const next = new Set(checkedIds.value)
    for (const op of operations.value) {
      if (op.operationId) next.add(op.operationId)
    }
    checkedIds.value = next
  }
}

async function doExport(format: string) {
  try {
    const blob = await exportOpenApiDocs(exportPayload(format))
    const ext = format === 'HTML' ? 'html' : format === 'MARKDOWN' ? 'md' : 'json'
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `openapi-export.${ext}`
    a.click()
    URL.revokeObjectURL(url)
    feedback.toast.success('导出已开始下载')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '导出失败')
  }
}

async function previewHtml() {
  previewing.value = true
  try {
    const blob = await exportOpenApiDocs(exportPayload('HTML'))
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = URL.createObjectURL(blob)
    feedback.toast.success('预览已生成')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '预览失败')
  } finally {
    previewing.value = false
  }
}

async function doPublish() {
  const ids = [...checkedIds.value]
  if (!ids.length) {
    feedback.toast.warning('请先勾选要发布的接口')
    return
  }
  try {
    await publishOpenApiDocs({
      docKey: publishDocKey.value || 'default',
      title: publishTitle.value,
      version: publishVersion.value,
      publishedSummary: publishSummary.value,
      selectionMode: 'CHECKED',
      operationIds: ids,
    })
    feedback.toast.success('公开文档快照已发布')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '发布失败')
  }
}

async function sendTest() {
  if (!selectedOperationId.value || !operationDetail.value) return
  const method = selectedMethod.value || 'GET'
  const needsConfirm = !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())
  if (needsConfirm && !testConfirm.value) {
    feedback.toast.warning('写操作测试请先勾选确认')
    return
  }
  const missingPath = testPathParams.value.filter((p) => p.required && !p.value.trim()).map((p) => p.name)
  if (missingPath.length) {
    feedback.toast.warning(`请填写路径参数：${missingPath.join(', ')}`)
    return
  }
  testing.value = true
  testResult.value = ''
  lastTestResult.value = null
  try {
    const pathParams = paramsToRecord(testPathParams.value, true)
    const queryParams = paramsToRecord(testQueryParams.value, false)
    const headers = paramsToRecord(testHeaderParams.value, false)
    const result = await testOpenApiOperation({
      operationId: selectedOperationId.value,
      pathParams,
      queryParams,
      headers,
      body: testBody.value || null,
      confirm: needsConfirm ? testConfirm.value : false,
    })
    lastTestResult.value = result
    testResult.value = JSON.stringify(result, null, 2)
  } catch (e) {
    testResult.value = e instanceof Error ? e.message : '测试失败'
  } finally {
    testing.value = false
  }
}

async function saveUseCase() {
  if (!selectedOperationId.value || !lastTestResult.value || !operationDetail.value) return
  savingUseCase.value = true
  try {
    const saved = await saveOpenApiUseCase(selectedOperationId.value, {
      name: useCaseName.value || operationDetail.value.summary || `${selectedMethod.value} ${operationDetail.value.path}`,
      description: useCaseDescription.value,
      pathParams: paramsToRecord(testPathParams.value, false),
      queryParams: paramsToRecord(testQueryParams.value, false),
      headers: paramsToRecord(testHeaderParams.value, false),
      body: testBody.value || null,
      requestUrl: lastTestResult.value.requestUrl,
      success: lastTestResult.value.success,
      responseStatus: lastTestResult.value.status,
      responseHeaders: lastTestResult.value.headers,
      responseBody: lastTestResult.value.bodyPreview,
      errorType: lastTestResult.value.errorType,
      errorMessage: lastTestResult.value.errorMessage,
      durationMs: lastTestResult.value.durationMs,
    })
    operationDetail.value = saved
    feedback.toast.success('测试用例已保存')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '保存用例失败')
  } finally {
    savingUseCase.value = false
  }
}

function paramsToRecord(params: TestParam[], includeEmptyRequired: boolean) {
  const record: Record<string, string> = {}
  for (const param of params) {
    const value = param.value.trim()
    if (value || (includeEmptyRequired && param.required)) {
      record[param.name] = value
    }
  }
  return record
}

function exportPayload(format: string) {
  const ids = [...checkedIds.value]
  if (ids.length) {
    return {
      format,
      selectionMode: 'CHECKED',
      operationIds: ids,
      includeSchemas: true,
      includeExamples: true,
      includeGovernance: true,
    }
  }
  const query = buildQuery()
  return {
    format,
    selectionMode: 'FILTERED',
    serviceIds: selectedServiceId.value ? [selectedServiceId.value] : undefined,
    filters: query,
    includeSchemas: true,
    includeExamples: true,
    includeGovernance: true,
  }
}

function goRegistry() {
  if (!operationDetail.value) return
  router.push({
    path: '/api/registry',
    query: {
      serviceId: operationDetail.value.serviceId,
      path: operationDetail.value.path,
      requestMethod: operationDetail.value.method ?? operationDetail.value.requestMethod,
    },
  })
}

function applyRouteSelection() {
  const serviceId = route.query.serviceId as string | undefined
  const path = route.query.path as string | undefined
  const method = (route.query.method as string | undefined) ?? (route.query.requestMethod as string | undefined)
  if (serviceId) selectedServiceId.value = serviceId
  if (path && method) {
    const match = operations.value.find(
      (op) => op.path === path && op.method?.toUpperCase() === method.toUpperCase(),
    )
    if (match) selectOperation(match)
  }
}

function parseJsonBlock(raw?: string) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return raw
  }
}

function parameterType(raw: Record<string, unknown>) {
  const schema = raw.schema as Record<string, unknown> | undefined
  if (schema) return schemaType(schema)
  const type = String(raw.type ?? 'string')
  if (type === 'array') {
    const items = raw.items as Record<string, unknown> | undefined
    return `array<${String(items?.type ?? 'object')}>`
  }
  return type
}

function defaultParamValue(raw: Record<string, unknown>) {
  const value = raw.example ?? raw.default
  if (value != null) return String(value)
  const type = parameterType(raw)
  if (type.startsWith('number') || type.startsWith('integer')) return '0'
  if (type.startsWith('boolean')) return 'false'
  return ''
}

function buildRequestBodySample(detail: OpenApiOperationDetail) {
  const schema = requestSchema(detail)
  const schemas = parseJsonBlock(detail.schemasJson)
  if (!schema) return '{}'
  return JSON.stringify(sampleFromSchema(schema, isPlainObject(schemas) ? schemas : {}, 0), null, 2)
}

function requestSchema(detail: OpenApiOperationDetail): Record<string, unknown> | null {
  const body = parseJsonBlock(detail.requestBodyJson)
  const fromBody = schemaFromBody(body)
  if (fromBody) return fromBody
  const params = parseJsonBlock(detail.parametersJson)
  if (Array.isArray(params)) {
    const bodyParam = params.find((p) => p?.in === 'body' && p.schema)
    if (bodyParam?.schema) return bodyParam.schema
  }
  return null
}

function schemaFromBody(body: unknown): Record<string, unknown> | null {
  if (!isPlainObject(body)) return null
  if (isPlainObject(body.schema)) return body.schema
  if (isPlainObject(body.content)) {
    const json = body.content['application/json']
    if (isPlainObject(json) && isPlainObject(json.schema)) return json.schema
    const first = Object.values(body.content).find(isPlainObject)
    if (isPlainObject(first) && isPlainObject(first.schema)) return first.schema
  }
  return null
}

function sampleFromSchema(schema: Record<string, unknown>, schemas: Record<string, unknown>, depth: number): unknown {
  if (depth > 3) return {}
  const resolved = resolveSchema(schema, schemas)
  if (resolved.example != null) return resolved.example
  if (resolved.default != null) return resolved.default
  if (resolved.type === 'array') {
    return [sampleFromSchema(asObject(resolved.items), schemas, depth + 1)]
  }
  if (isPlainObject(resolved.properties)) {
    const result: Record<string, unknown> = {}
    for (const [key, value] of Object.entries(resolved.properties)) {
      result[key] = sampleFromSchema(asObject(value), schemas, depth + 1)
    }
    return result
  }
  if (resolved.type === 'integer' || resolved.type === 'number') return 0
  if (resolved.type === 'boolean') return false
  return 'string'
}

function resolveSchema(schema: Record<string, unknown>, schemas: Record<string, unknown>) {
  const ref = typeof schema.$ref === 'string' ? schema.$ref : ''
  if (ref) {
    const key = ref.slice(ref.lastIndexOf('/') + 1)
    const resolved = schemas[key]
    if (isPlainObject(resolved)) return resolved
  }
  if (Array.isArray(schema.allOf)) {
    return schema.allOf.reduce<Record<string, unknown>>((acc, item) => {
      if (isPlainObject(item)) Object.assign(acc, resolveSchema(item, schemas))
      return acc
    }, {})
  }
  return schema
}

function schemaType(schema: Record<string, unknown>): string {
  if (typeof schema.$ref === 'string') return schema.$ref.slice(schema.$ref.lastIndexOf('/') + 1)
  if (schema.type === 'array') return `array<${schemaType(asObject(schema.items))}>`
  return String(schema.type ?? (schema.properties ? 'object' : 'object'))
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function asObject(value: unknown): Record<string, unknown> {
  return isPlainObject(value) ? value : {}
}

watch(selectedServiceId, () => {
  page.value = 1
  checkedIds.value = new Set()
  loadOperations(1)
})

watch([keyword, methodFilter, isOpenFilter, isAuthFilter, syncStateFilter, linkedFilter], () => {
  loadOperations(1)
})

onMounted(async () => {
  try {
    await loadSources()
    if (route.query.serviceId) {
      selectedServiceId.value = String(route.query.serviceId)
    }
    await loadOperations(1)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '初始化失败')
  }
})
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="API 文档与调试" description="聚合各服务 OpenAPI，关联 API 资源治理状态，支持安全测试与文档发布。" />

    <div class="flex flex-wrap items-center gap-2 rounded-lg border bg-card p-3">
      <Select v-model="selectedServiceId" class="w-56">
        <option value="">选择服务</option>
        <option v-for="s in sources" :key="s.serviceId" :value="s.serviceId">
          {{ sourceLabel(s) }}
        </option>
      </Select>
      <Button variant="outline" :disabled="syncing" @click="syncDocs">
        <RefreshCw class="mr-1 size-4" :class="{ 'animate-spin': syncing }" />
        同步
      </Button>
      <div class="relative min-w-[180px] flex-1">
        <Search class="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
        <Input v-model="keyword" placeholder="搜索 path / summary" class="pl-9" />
      </div>
      <Select v-model="methodFilter" class="w-28">
        <option value="">Method</option>
        <option v-for="m in ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']" :key="m" :value="m">{{ m }}</option>
      </Select>
      <Select v-model="isOpenFilter" class="w-28">
        <option value="">开放</option>
        <option value="1">已开放</option>
        <option value="0">内部</option>
      </Select>
      <Select v-model="isAuthFilter" class="w-28">
        <option value="">认证</option>
        <option value="1">需认证</option>
        <option value="0">免认证</option>
      </Select>
      <Select v-model="linkedFilter" class="w-32">
        <option value="">关联</option>
        <option value="1">已关联</option>
        <option value="0">未关联</option>
      </Select>
      <Select v-model="syncStateFilter" class="w-32">
        <option value="">状态</option>
        <option value="NEW">新增</option>
        <option value="CHANGED">变更</option>
        <option value="ACTIVE">正常</option>
        <option value="MISSING">已下线</option>
      </Select>
      <Badge v-if="selectedSource" variant="outline">
        {{ selectedSource.syncStatus }} · {{ selectedSource.operationTotal ?? 0 }} 接口
      </Badge>
    </div>

    <div class="grid gap-4 lg:grid-cols-[minmax(280px,360px)_1fr]">
      <div class="rounded-lg border bg-card">
        <div class="flex items-center justify-between border-b px-3 py-2 text-sm font-medium">
          <label class="flex items-center gap-2">
            <input type="checkbox" :checked="allCheckedOnPage" @change="toggleCheckAll" />
            接口目录
          </label>
          <span class="text-muted-foreground">{{ checkedIds.size }} 已选</span>
        </div>
        <div v-if="loading" class="p-4 text-sm text-muted-foreground">加载中...</div>
        <div v-else-if="!operations.length" class="p-4 text-sm text-muted-foreground">
          暂无接口，请先同步 OpenAPI 文档。
        </div>
        <ul v-else class="max-h-[520px] divide-y overflow-y-auto">
          <li
            v-for="op in operations"
            :key="op.operationId"
            class="cursor-pointer px-3 py-2 text-sm hover:bg-muted/50"
            :class="{ 'bg-muted': selectedOperationId === op.operationId }"
            @click="selectOperation(op)"
          >
            <div class="flex items-start gap-2">
              <input
                type="checkbox"
                :checked="op.operationId != null && checkedIds.has(op.operationId)"
                @click.stop
                @change="toggleCheck(op)"
              />
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-1">
                  <span class="rounded px-1.5 py-0.5 text-xs font-medium" :class="methodBadgeClass(op.method)">
                    {{ op.method }}
                  </span>
                  <code class="truncate text-xs">{{ op.path }}</code>
                </div>
                <p class="truncate text-muted-foreground">{{ op.summary || '—' }}</p>
                <div class="mt-1 flex flex-wrap gap-1">
                  <Badge variant="outline">{{ op.isOpen === 1 ? '开放' : '内部' }}</Badge>
                  <Badge variant="outline">{{ op.isAuth ? '需认证' : '免认证' }}</Badge>
                  <Badge v-if="!op.linked" variant="destructive">未关联</Badge>
                  <Badge v-if="op.syncState === 'CHANGED'" variant="secondary">变更</Badge>
                </div>
              </div>
            </div>
          </li>
        </ul>
        <PaginationBar
          v-model:page="page"
          :total="total"
          :page-size="pageSize"
          class="border-t px-2 py-2"
          @update:page="loadOperations"
        />
      </div>

      <div class="space-y-4">
        <div v-if="!operationDetail" class="rounded-lg border bg-card p-8 text-center text-muted-foreground">
          请选择左侧接口查看详情
        </div>
        <template v-else>
          <div class="rounded-lg border bg-card p-4">
            <div class="flex flex-wrap items-start justify-between gap-2">
              <div>
                <div class="flex items-center gap-2">
                  <span class="rounded px-2 py-0.5 text-sm font-medium" :class="methodBadgeClass(selectedMethod)">
                    {{ selectedMethod }}
                  </span>
                  <code class="text-sm">{{ operationDetail.path }}</code>
                </div>
                <h2 class="mt-1 text-lg font-semibold">{{ operationDetail.summary || '接口详情' }}</h2>
                <p class="text-sm text-muted-foreground">{{ operationDetail.description }}</p>
              </div>
              <Button variant="outline" size="sm" @click="goRegistry">
                <ExternalLink class="mr-1 size-4" />
                去 API 资源管理
              </Button>
            </div>
            <div class="mt-3 flex flex-wrap gap-2 text-sm">
              <Badge>apiId: {{ operationDetail.apiId ?? '—' }}</Badge>
              <Badge variant="outline">apiCode: {{ operationDetail.apiCode ?? '—' }}</Badge>
              <Badge variant="outline">{{ operationDetail.isOpen === 1 ? '已开放' : '内部' }}</Badge>
              <Badge variant="outline">{{ operationDetail.isAuth ? '需要认证' : '免认证' }}</Badge>
              <Badge variant="outline">状态: {{ operationDetail.status ?? '—' }}</Badge>
            </div>
            <div class="mt-4 flex gap-2 border-b">
              <button
                v-for="tab in ['overview', 'params', 'body', 'responses', 'raw']"
                :key="tab"
                type="button"
                class="border-b-2 px-3 py-2 text-sm capitalize"
                :class="detailTab === tab ? 'border-primary text-primary' : 'border-transparent text-muted-foreground'"
                @click="detailTab = tab"
              >
                {{ tab }}
              </button>
            </div>
            <div class="mt-3 max-h-72 overflow-auto text-sm">
              <pre v-if="detailTab === 'overview'" class="whitespace-pre-wrap">{{
                operationDetail.description || operationDetail.summary || '—'
              }}</pre>
              <pre v-else-if="detailTab === 'params'" class="text-xs">{{
                JSON.stringify(parseJsonBlock(operationDetail.parametersJson), null, 2)
              }}</pre>
              <pre v-else-if="detailTab === 'body'" class="text-xs">{{
                JSON.stringify(parseJsonBlock(operationDetail.requestBodyJson), null, 2)
              }}</pre>
              <pre v-else-if="detailTab === 'responses'" class="text-xs">{{
                JSON.stringify(parseJsonBlock(operationDetail.responsesJson), null, 2)
              }}</pre>
              <pre v-else class="text-xs">{{
                JSON.stringify(parseJsonBlock(operationDetail.rawOperationJson), null, 2)
              }}</pre>
            </div>
          </div>

          <div class="rounded-lg border bg-card p-4">
            <h3 class="mb-2 font-medium">安全测试</h3>
            <div v-if="testPathParams.length" class="mb-3">
              <div class="mb-1 text-xs text-muted-foreground">Path Params</div>
              <div class="space-y-2">
                <div v-for="param in testPathParams" :key="`path-${param.name}`" class="grid gap-2 md:grid-cols-[180px_1fr]">
                  <label class="text-sm">
                    {{ param.name }}
                    <span v-if="param.required" class="text-destructive">*</span>
                    <span class="ml-1 text-xs text-muted-foreground">{{ param.type }}</span>
                  </label>
                  <Input v-model="param.value" :placeholder="param.description || param.name" />
                </div>
              </div>
            </div>
            <div v-if="testQueryParams.length" class="mb-3">
              <div class="mb-1 text-xs text-muted-foreground">Query Params</div>
              <div class="space-y-2">
                <div v-for="param in testQueryParams" :key="`query-${param.name}`" class="grid gap-2 md:grid-cols-[180px_1fr]">
                  <label class="text-sm">
                    {{ param.name }}
                    <span v-if="param.required" class="text-destructive">*</span>
                    <span class="ml-1 text-xs text-muted-foreground">{{ param.type }}</span>
                  </label>
                  <Input v-model="param.value" :placeholder="param.description || param.name" />
                </div>
              </div>
            </div>
            <div class="mb-3">
              <div class="mb-1 text-xs text-muted-foreground">Headers</div>
              <div class="space-y-2">
                <div v-for="param in testHeaderParams" :key="`header-${param.name}`" class="grid gap-2 md:grid-cols-[180px_1fr]">
                  <label class="text-sm">
                    {{ param.name }}
                    <span v-if="param.required" class="text-destructive">*</span>
                    <span class="ml-1 text-xs text-muted-foreground">{{ param.type }}</span>
                  </label>
                  <Input v-model="param.value" :placeholder="param.description || param.name" />
                </div>
              </div>
            </div>
            <div class="mt-2">
              <label class="text-xs text-muted-foreground">Body</label>
              <textarea v-model="testBody" rows="4" class="mt-1 w-full rounded border bg-background p-2 font-mono text-xs" />
            </div>
            <label v-if="selectedMethod && !['GET','HEAD','OPTIONS'].includes(selectedMethod)" class="mt-2 flex items-center gap-2 text-sm">
              <input v-model="testConfirm" type="checkbox" />
              确认执行写操作测试
            </label>
            <div class="mt-3 flex gap-2">
              <Button :disabled="testing" @click="sendTest">
                <Send class="mr-1 size-4" />
                发送
              </Button>
            </div>
            <pre v-if="testResult" class="mt-3 max-h-48 overflow-auto rounded bg-muted p-2 text-xs">{{ testResult }}</pre>
            <div v-if="lastTestResult" class="mt-3 rounded border bg-background p-3">
              <div class="grid gap-2 md:grid-cols-2">
                <Input v-model="useCaseName" placeholder="用例名称" />
                <Input v-model="useCaseDescription" placeholder="用例说明，例如：正常查询 / 参数为空 / 权限不足" />
              </div>
              <Button class="mt-2" size="sm" variant="secondary" :disabled="savingUseCase" @click="saveUseCase">
                <Upload class="mr-1 size-4" />
                保存为用例
              </Button>
            </div>
          </div>
        </template>

        <div class="rounded-lg border bg-card p-4">
          <h3 class="mb-2 font-medium">导出 / 发布</h3>
          <p class="mb-3 text-sm text-muted-foreground">
            已选择 {{ checkedIds.size }} 个接口；未选择时导出当前筛选范围。
          </p>
          <div class="flex flex-wrap gap-2">
            <Button variant="outline" size="sm" :disabled="previewing" @click="previewHtml">
              <Eye class="mr-1 size-4" /> 在线预览
            </Button>
            <Button variant="outline" size="sm" @click="doExport('JSON')">
              <Download class="mr-1 size-4" /> JSON
            </Button>
            <Button variant="outline" size="sm" @click="doExport('MARKDOWN')">
              <Download class="mr-1 size-4" /> Markdown
            </Button>
            <Button variant="outline" size="sm" @click="doExport('HTML')">
              <Download class="mr-1 size-4" /> HTML
            </Button>
          </div>
          <div class="mt-4 grid gap-2 md:grid-cols-2">
            <Input v-model="publishDocKey" placeholder="docKey (default)" />
            <Input v-model="publishTitle" placeholder="发布标题" />
            <Input v-model="publishVersion" placeholder="版本" />
            <Input v-model="publishSummary" placeholder="发布说明" />
          </div>
          <Button class="mt-3" variant="secondary" @click="doPublish">
            <Upload class="mr-1 size-4" />
            发布为公开文档
          </Button>
          <iframe
            v-if="previewUrl"
            :src="previewUrl"
            title="OpenAPI 文档预览"
            class="mt-4 h-[560px] w-full rounded border bg-background"
          />
        </div>
      </div>
    </div>
  </div>
</template>
