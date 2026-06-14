<script setup lang="ts">
import { computed, ref } from 'vue'
import { Plus, Pencil, Play, Pause, RotateCcw, Trash2, ScrollText } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
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
import { useCrudForm } from '@/composables/useCrudForm'
import { useFeedback } from '@/composables/useFeedback'
import { usePagedList } from '@/composables/usePagedList'
import {
  changeJobStatus,
  createJob,
  deleteJob,
  listJobs,
  runJob,
  updateJob,
} from '@/api/job'
import { listBaseApis } from '@/api/baseApi'
import { listOpenApiOperations } from '@/api/openapiDocs'
import type { BaseApi, OpenApiOperationView, SysJob } from '@/api/types'

const keyword = ref('')
const groupFilter = ref('')
const statusFilter = ref('')
const feedback = useFeedback()
const targetLoading = ref(false)
const targetError = ref('')
const targetKeyword = ref('')
const selectedTarget = ref('')

type TargetKind = 'registered' | 'api'

type JobTargetOption = {
  value: string
  kind: TargetKind
  label: string
  meta: string
  jobName: string
  jobGroup: string
  invokeTarget: string
  methodType: string
  cronExpression?: string
  description?: string
  job?: SysJob
}

const { items, total, page, loading, error, load, pageSize } = usePagedList<SysJob>((p, s) =>
  listJobs(p, s, {
    keyword: keyword.value,
    jobGroup: groupFilter.value,
    status: statusFilter.value as SysJob['status'] | '',
  }),
)

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<SysJob>(() => ({
  jobName: '',
  jobGroup: 'DEFAULT',
  invokeTarget: '',
  methodType: 'GET',
  cronExpression: '0 */5 * * * ?',
  misfirePolicy: 'DO_NOTHING',
  concurrent: true,
  recordLog: true,
  status: 'PAUSE',
  description: '',
}))

const dialogTitle = computed(() => (editing.value ? '编辑任务' : '新建任务'))

const targetOptions = ref<JobTargetOption[]>([])

const registeredTargets = computed(() => filterTargets('registered'))
const apiTargets = computed(() => filterTargets('api'))

function filterTargets(kind: TargetKind) {
  const kw = targetKeyword.value.trim().toLowerCase()
  return targetOptions.value.filter((item) => {
    if (item.kind !== kind) return false
    if (!kw) return true
    return [item.label, item.meta, item.jobName, item.jobGroup, item.invokeTarget]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(kw))
  })
}

function jobId(row: SysJob) {
  return row.jobId ?? row.id
}

function search() {
  load(1)
}

function openCreateJob() {
  openCreate()
  selectedTarget.value = ''
  targetKeyword.value = ''
  void loadTargetOptions()
}

function openEditJob(row: SysJob) {
  openEdit({
    ...row,
    status: normalizeStatus(row.status),
    misfirePolicy: normalizeMisfire(row.misfirePolicy),
  })
  selectedTarget.value = targetValueForJob(row)
  targetKeyword.value = ''
  void loadTargetOptions().then(() => {
    selectedTarget.value = findTargetValue(row) || targetValueForJob(row)
  })
}

function normalizeStatus(status: SysJob['status'] | undefined) {
  if (status === 0) return 'NORMAL'
  if (status === 1) return 'PAUSE'
  return status ?? 'PAUSE'
}

function normalizeMisfire(policy: SysJob['misfirePolicy'] | undefined) {
  if (policy === 0) return 'DEFAULT'
  if (policy === 1) return 'IGNORE_MISFIRES'
  if (policy === 2) return 'FIRE_AND_PROCEED'
  if (policy === 3) return 'DO_NOTHING'
  return policy ?? 'DO_NOTHING'
}

function statusLabel(status: SysJob['status'] | undefined) {
  return normalizeStatus(status) === 'NORMAL' ? '运行' : '暂停'
}

function statusVariant(status: SysJob['status'] | undefined) {
  return normalizeStatus(status) === 'NORMAL' ? 'default' : 'secondary'
}

function misfireLabel(policy: SysJob['misfirePolicy'] | undefined) {
  const value = normalizeMisfire(policy)
  return {
    DEFAULT: '默认',
    IGNORE_MISFIRES: '立即触发',
    FIRE_AND_PROCEED: '触发一次',
    DO_NOTHING: '跳过',
}[value as string] ?? '跳过'
}

function targetSourceLabel(row: SysJob) {
  const target = row.invokeTarget || ''
  if (/^feign:\/\//i.test(target)) return '接口'
  if (/^https?:\/\//i.test(target)) return 'HTTP'
  return '本地'
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

function normalizeMethod(method?: string) {
  return (method || 'GET').toUpperCase()
}

function normalizeTargetPath(path?: string) {
  const value = path?.trim() || ''
  if (!value) return ''
  return value.startsWith('/') ? value : `/${value}`
}

function buildFeignTarget(api: BaseApi) {
  const serviceId = api.serviceId?.trim()
  const path = normalizeTargetPath(api.path)
  if (!serviceId || !path) return ''
  return `feign://${serviceId}${path}`
}

function targetValueForJob(row: SysJob) {
  const id = jobId(row)
  if (id) return `job:${id}`
  return `job:${row.jobGroup || 'DEFAULT'}:${row.invokeTarget || row.jobName || ''}`
}

function findTargetValue(row: SysJob) {
  const target = row.invokeTarget?.trim()
  const group = row.jobGroup?.trim()
  const matched = targetOptions.value.find((item) => {
    if (item.job && jobId(item.job) && jobId(item.job) === jobId(row)) return true
    return target && item.invokeTarget === target && (!group || item.jobGroup === group)
  })
  return matched?.value ?? ''
}

function buildRegisteredTarget(row: SysJob): JobTargetOption | null {
  const invokeTarget = row.invokeTarget?.trim()
  if (!invokeTarget) return null
  const method = normalizeMethod(row.methodType)
  const group = row.jobGroup || 'DEFAULT'
  return {
    value: targetValueForJob(row),
    kind: 'registered',
    label: row.jobName || invokeTarget,
    meta: `${group} · ${method} · ${row.cronExpression || '-'}`,
    jobName: row.jobName || invokeTarget,
    jobGroup: group,
    invokeTarget,
    methodType: method,
    cronExpression: row.cronExpression,
    description: row.description,
    job: row,
  }
}

function buildApiTarget(api: BaseApi): JobTargetOption | null {
  if (!api.requestMethod) return null
  const invokeTarget = buildFeignTarget(api)
  if (!invokeTarget) return null
  const method = normalizeMethod(api.requestMethod)
  const name = api.apiName || api.path || invokeTarget
  return {
    value: `api:${api.apiId || `${api.serviceId}:${method}:${api.path}`}`,
    kind: 'api',
    label: name,
    meta: `${api.serviceId || '-'} · ${method} · ${api.path || '-'}`,
    jobName: name,
    jobGroup: api.serviceId || 'DEFAULT',
    invokeTarget,
    methodType: method,
    description: api.apiDesc,
  }
}

function buildOperationTarget(operation: OpenApiOperationView): JobTargetOption | null {
  const serviceId = operation.serviceId?.trim()
  const path = normalizeTargetPath(operation.path)
  const method = normalizeMethod(operation.method || operation.requestMethod)
  if (!serviceId || !path || !method) return null
  const invokeTarget = `feign://${serviceId}${path}`
  const name = operation.summary || operation.path || invokeTarget
  return {
    value: `op:${operation.operationId || `${serviceId}:${method}:${path}`}`,
    kind: 'api',
    label: name,
    meta: `${serviceId} · ${method} · ${path}`,
    jobName: name,
    jobGroup: serviceId,
    invokeTarget,
    methodType: method,
  }
}

async function loadTargetOptions() {
  targetLoading.value = true
  targetError.value = ''
  try {
    const [jobs, operations, apis] = await Promise.all([
      listJobs(1, 1000),
      listOpenApiOperations(1, 1000, { status: 1 }),
      listBaseApis(1, 1000, { status: 1 }),
    ])
    const options: JobTargetOption[] = []
    for (const row of jobs.contents ?? []) {
      const target = buildRegisteredTarget(row)
      if (target) options.push(target)
    }
    const registeredTargetKeys = new Set(
      options.map((item) => `${item.jobGroup}|${item.methodType}|${item.invokeTarget}`),
    )
    const apiTargetKeys = new Set<string>()
    for (const operation of operations.contents ?? []) {
      const target = buildOperationTarget(operation)
      if (!target) continue
      const key = `${target.jobGroup}|${target.methodType}|${target.invokeTarget}`
      if (registeredTargetKeys.has(key) || apiTargetKeys.has(key)) continue
      apiTargetKeys.add(key)
      options.push(target)
    }
    for (const api of apis.contents ?? []) {
      const target = buildApiTarget(api)
      if (!target) continue
      const key = `${target.jobGroup}|${target.methodType}|${target.invokeTarget}`
      if (!registeredTargetKeys.has(key) && !apiTargetKeys.has(key)) options.push(target)
    }
    targetOptions.value = options
  } catch (e) {
    targetError.value = e instanceof Error ? e.message : '调度目标加载失败'
  } finally {
    targetLoading.value = false
  }
}

function applyTargetOption(option: JobTargetOption) {
  form.value.jobName = option.jobName
  form.value.jobGroup = option.jobGroup
  form.value.invokeTarget = option.invokeTarget
  form.value.methodType = option.methodType
  if (option.cronExpression) {
    form.value.cronExpression = option.cronExpression
  }
  if (option.description) {
    form.value.description = option.description
  }
}

function handleTargetChange(value: string) {
  selectedTarget.value = value
  if (!value || value === '__custom__') return
  const option = targetOptions.value.find((item) => item.value === value)
  if (!option) return
  if (option.kind === 'registered' && option.job) {
    openEditJob(option.job)
    return
  }
  applyTargetOption(option)
}

async function handleSave() {
  if (!form.value.jobName?.trim() || !form.value.jobGroup?.trim()) {
    formError.value = '任务名称和任务组不能为空'
    return
  }
  if (!form.value.cronExpression?.trim()) {
    formError.value = 'Cron 表达式不能为空'
    return
  }
  if (!form.value.invokeTarget?.trim()) {
    formError.value = '请选择接口或任务目标'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const id = jobId(form.value)
    if (editing.value && id) {
      await updateJob(id, form.value)
      feedback.toast.success('任务已更新')
    } else {
      await createJob(form.value)
      feedback.toast.success('任务已创建')
    }
    closeDialog()
    await load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleStatus(row: SysJob) {
  const id = jobId(row)
  if (!id) return
  const next = normalizeStatus(row.status) === 'NORMAL' ? 'PAUSE' : 'NORMAL'
  await changeJobStatus(id, next)
  feedback.toast.success(next === 'NORMAL' ? '任务已恢复' : '任务已暂停')
  await load(page.value)
}

async function handleRun(row: SysJob) {
  const id = jobId(row)
  if (!id) return
  await runJob(id)
  feedback.toast.success('已触发一次执行')
  await load(page.value)
}

async function handleDelete(row: SysJob) {
  const id = jobId(row)
  if (!id) return
  const confirmed = await feedback.confirm({
    title: '确认删除任务',
    message: `确认删除任务 ${row.jobName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteJob(id)
  feedback.toast.success('任务已删除')
  await load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="任务管理" description="统一调度中心">
      <template #actions>
        <Input v-model="keyword" placeholder="任务名称" class="w-40" @keyup.enter="search" />
        <Input v-model="groupFilter" placeholder="任务组" class="w-32" @keyup.enter="search" />
        <Select v-model="statusFilter" class="w-28">
          <option value="">全部状态</option>
          <option value="NORMAL">运行</option>
          <option value="PAUSE">暂停</option>
        </Select>
        <Button variant="outline" @click="search">查询</Button>
        <RouterLink to="/jobs/logs">
          <Button variant="outline" title="调度日志">
            <ScrollText class="h-4 w-4" />
            日志
          </Button>
        </RouterLink>
        <Button @click="openCreateJob">
          <Plus class="h-4 w-4" />
          新建
        </Button>
      </template>
    </PageHeader>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">任务</th>
            <th class="h-10 px-4 text-left font-medium">组</th>
            <th class="h-10 px-4 text-left font-medium">Cron</th>
            <th class="h-10 px-4 text-left font-medium">调度目标</th>
            <th class="h-10 px-4 text-left font-medium">策略</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-left font-medium">更新时间</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="jobId(row)" class="border-b">
            <td class="p-4">{{ jobId(row) }}</td>
            <td class="p-4">
              <div class="font-medium">{{ row.jobName }}</div>
              <div v-if="row.description" class="mt-1 max-w-56 truncate text-xs text-muted-foreground">
                {{ row.description }}
              </div>
            </td>
            <td class="p-4">
              <Badge variant="outline">{{ row.jobGroup || 'DEFAULT' }}</Badge>
            </td>
            <td class="p-4 font-mono text-xs">{{ row.cronExpression }}</td>
            <td class="p-4">
              <div class="max-w-72 truncate font-mono text-xs" :title="row.invokeTarget">
                {{ row.invokeTarget }}
              </div>
              <div class="mt-1 flex flex-wrap items-center gap-1 text-xs text-muted-foreground">
                <Badge variant="secondary">{{ targetSourceLabel(row) }}</Badge>
                <span>{{ row.methodType || '-' }} · {{ row.concurrent ? '并发' : '串行' }} · {{ row.recordLog ? '记录日志' : '不记录' }}</span>
              </div>
            </td>
            <td class="p-4">{{ misfireLabel(row.misfirePolicy) }}</td>
            <td class="p-4">
              <Badge :variant="statusVariant(row.status)">
                {{ statusLabel(row.status) }}
              </Badge>
            </td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.updateTime || row.createTime) }}</td>
            <td class="p-4 text-right">
              <div class="flex justify-end gap-1">
                <Button variant="outline" size="icon" title="立即执行" @click="handleRun(row)">
                  <Play class="h-4 w-4" />
                </Button>
                <Button variant="outline" size="icon" :title="normalizeStatus(row.status) === 'NORMAL' ? '暂停' : '恢复'" @click="handleStatus(row)">
                  <Pause v-if="normalizeStatus(row.status) === 'NORMAL'" class="h-4 w-4" />
                  <RotateCcw v-else class="h-4 w-4" />
                </Button>
                <Button variant="outline" size="icon" title="编辑" @click="openEditJob(row)">
                  <Pencil class="h-4 w-4" />
                </Button>
                <Button variant="destructive" size="icon" title="删除" @click="handleDelete(row)">
                  <Trash2 class="h-4 w-4" />
                </Button>
              </div>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog v-model:open="dialogOpen" :title="dialogTitle" :saving="saving" wide @save="handleSave">
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="调度目标" required>
        <div class="grid gap-2 md:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
          <Input v-model="targetKeyword" placeholder="筛选任务、接口或服务" />
          <Select
            :model-value="selectedTarget"
            :disabled="targetLoading"
            @update:model-value="handleTargetChange"
          >
            <option value="">{{ targetLoading ? '加载中' : '选择调度目标' }}</option>
            <option value="__custom__">手动输入</option>
            <optgroup v-if="registeredTargets.length" label="统一调度注册任务">
              <option v-for="item in registeredTargets" :key="item.value" :value="item.value">
                {{ item.label }} · {{ item.meta }}
              </option>
            </optgroup>
            <optgroup v-if="apiTargets.length" label="API 接口资源">
              <option v-for="item in apiTargets" :key="item.value" :value="item.value">
                {{ item.label }} · {{ item.meta }}
              </option>
            </optgroup>
          </Select>
        </div>
        <p v-if="targetError" class="text-xs text-destructive">{{ targetError }}</p>
      </FormField>
      <div class="grid gap-4 md:grid-cols-2">
        <FormField label="任务名称" required>
          <Input v-model="form.jobName" />
        </FormField>
        <FormField label="任务组" required>
          <Input v-model="form.jobGroup" />
        </FormField>
      </div>
      <FormField label="调用地址" required>
        <Input v-model="form.invokeTarget" class="font-mono text-sm" placeholder="feign://service/path 或 bean.method()" />
      </FormField>
      <div class="grid gap-4 md:grid-cols-2">
        <FormField label="请求方法">
          <Select v-model="form.methodType">
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
          </Select>
        </FormField>
        <FormField label="Cron" required>
          <Input v-model="form.cronExpression" class="font-mono text-sm" />
        </FormField>
      </div>
      <div class="grid gap-4 md:grid-cols-2">
        <FormField label="错过策略">
          <Select v-model="form.misfirePolicy">
            <option value="DEFAULT">默认</option>
            <option value="IGNORE_MISFIRES">立即触发</option>
            <option value="FIRE_AND_PROCEED">触发一次</option>
            <option value="DO_NOTHING">跳过</option>
          </Select>
        </FormField>
        <FormField label="状态">
          <Select v-model="form.status">
            <option value="NORMAL">运行</option>
            <option value="PAUSE">暂停</option>
          </Select>
        </FormField>
      </div>
      <div class="grid gap-4 md:grid-cols-2">
        <label class="flex h-10 items-center gap-2 rounded-md border px-3 text-sm">
          <input v-model="form.concurrent" type="checkbox" class="h-4 w-4" />
          允许并发
        </label>
        <label class="flex h-10 items-center gap-2 rounded-md border px-3 text-sm">
          <input v-model="form.recordLog" type="checkbox" class="h-4 w-4" />
          记录日志
        </label>
      </div>
      <FormField label="描述">
        <textarea
          v-model="form.description"
          rows="3"
          class="min-h-20 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
        />
      </FormField>
    </CrudDialog>
  </div>
</template>
