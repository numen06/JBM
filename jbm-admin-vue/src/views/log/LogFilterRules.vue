<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Pencil, Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import {
  createGatewayLogFilterRule,
  deleteGatewayLogFilterRule,
  listGatewayLogFilterRules,
  testGatewayLogFilterRule,
  toggleGatewayLogFilterRule,
  updateGatewayLogFilterRule,
  type GatewayLogFilterRulePayload,
} from '@/api/logs'
import type { GatewayLogFilterRule } from '@/api/types'
import { get, put, unwrap } from '@/api/request'

const auditLogTypes = { login: '登录日志', operation: '操作日志', access: '访问日志' }
type RetentionDays = Record<keyof typeof auditLogTypes, number>
type RetentionStatus = {
  days: RetentionDays
  minimumDays: number
  maximumDays: number
  editable: boolean
}

const filterRules = ref<GatewayLogFilterRule[]>([])
const loading = ref(false)
const error = ref('')
const saving = ref(false)
const editingRule = ref<GatewayLogFilterRule | null>(null)
const ruleForm = ref<GatewayLogFilterRulePayload>(defaultRuleForm())
const testPath = ref('/logs/GatewayLogs/findLogs')
const testResult = ref('')
const retention = ref<RetentionStatus | null>(null)
const draftRetention = ref<RetentionDays>({ login: 0, operation: 0, access: 0 })
const retentionLoading = ref(false)
const retentionSaving = ref(false)
const retentionError = ref('')
const retentionSaved = ref(false)

const ruleTitle = computed(() => (editingRule.value ? '编辑自定义规则' : '新增自定义规则'))
const enabledValue = computed({
  get: () => (ruleForm.value.enabled === false ? 'false' : 'true'),
  set: (value: string) => {
    ruleForm.value.enabled = value === 'true'
  },
})

function defaultRuleForm(): GatewayLogFilterRulePayload {
  return {
    ruleName: '',
    enabled: true,
    pathPattern: '',
    method: '',
    serviceId: '',
    statusCode: '',
    remark: '',
  }
}

async function loadRules() {
  loading.value = true
  error.value = ''
  try {
    filterRules.value = await listGatewayLogFilterRules()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '采集过滤规则加载失败'
  } finally {
    loading.value = false
  }
}

async function loadRetention() {
  retentionLoading.value = true
  try {
    const status = unwrap(await get<RetentionStatus>('/logs/securityAudit/retention'))
    retention.value = status
    draftRetention.value = { ...status.days }
  } catch (e) {
    retention.value = null
    retentionError.value = e instanceof Error ? e.message : '暂无法读取日志留存设置'
  } finally {
    retentionLoading.value = false
  }
}

function loadSettings() {
  retentionError.value = ''
  retentionSaved.value = false
  return Promise.all([loadRules(), loadRetention()])
}

async function saveRetention() {
  const current = retention.value
  if (!current?.editable || retentionSaving.value || retentionLoading.value) return
  const days = { ...draftRetention.value }
  if (Object.values(days).some(day => !Number.isSafeInteger(day) || day < current.minimumDays || day > current.maximumDays)) {
    retentionError.value = `请输入 ${current.minimumDays} 至 ${current.maximumDays} 天的整数`
    return
  }
  const categories = Object.keys(auditLogTypes) as (keyof RetentionDays)[]
  if (categories.some(key => days[key] < current.days[key]) &&
      !window.confirm('缩短留存周期后，超过新期限的日志会按策略清理。确认保存？')) return
  retentionSaving.value = true
  retentionError.value = ''
  retentionSaved.value = false
  try {
    unwrap(await put<RetentionStatus>('/logs/securityAudit/retention', { days, expectedDays: current.days }))
    await loadRetention()
    retentionSaved.value = !!retention.value && categories.every(key => retention.value?.days[key] === days[key])
    if (!retentionSaved.value && !retentionError.value) retentionError.value = '生效策略已变化，请核对当前值'
  } catch (e) {
    await loadRetention()
    retentionError.value = e instanceof Error ? e.message : '无法确认日志留存设置已生效'
  } finally {
    retentionSaving.value = false
  }
}

function editRule(rule: GatewayLogFilterRule) {
  if (rule.builtin) return
  editingRule.value = rule
  ruleForm.value = {
    ruleName: rule.ruleName || '',
    enabled: rule.enabled !== false,
    pathPattern: rule.pathPattern || '',
    method: rule.method || '',
    serviceId: rule.serviceId || '',
    statusCode: rule.statusCode || '',
    remark: rule.remark || '',
  }
}

function resetForm() {
  editingRule.value = null
  ruleForm.value = defaultRuleForm()
  testResult.value = ''
}

function cleanForm(): GatewayLogFilterRulePayload {
  const form = ruleForm.value
  return {
    ruleName: String(form.ruleName || '').trim(),
    enabled: form.enabled !== false,
    pathPattern: String(form.pathPattern || '').trim(),
    method: String(form.method || '').trim(),
    serviceId: String(form.serviceId || '').trim(),
    statusCode: String(form.statusCode || '').trim(),
    remark: String(form.remark || '').trim(),
  }
}

async function saveRule() {
  const payload = cleanForm()
  if (!payload.ruleName || (!payload.pathPattern && !payload.method && !payload.serviceId && !payload.statusCode)) {
    error.value = '请填写规则名称，并至少填写一个匹配条件'
    return
  }
  saving.value = true
  error.value = ''
  try {
    if (editingRule.value?.ruleId) {
      await updateGatewayLogFilterRule(editingRule.value.ruleId, payload)
    } else {
      await createGatewayLogFilterRule(payload)
    }
    resetForm()
    await loadRules()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '采集过滤规则保存失败'
  } finally {
    saving.value = false
  }
}

async function toggleRule(rule: GatewayLogFilterRule) {
  if (rule.builtin || !rule.ruleId) return
  await toggleGatewayLogFilterRule(rule.ruleId, !rule.enabled)
  await loadRules()
}

async function removeRule(rule: GatewayLogFilterRule) {
  if (rule.builtin || !rule.ruleId) return
  if (!window.confirm(`确认删除采集过滤规则「${rule.ruleName || rule.ruleId}」？`)) return
  await deleteGatewayLogFilterRule(rule.ruleId)
  if (editingRule.value?.ruleId === rule.ruleId) resetForm()
  await loadRules()
}

async function runTest() {
  testResult.value = '测试中...'
  try {
    const result = await testGatewayLogFilterRule({
      path: testPath.value,
      method: ruleForm.value.method || undefined,
      serviceId: ruleForm.value.serviceId || undefined,
      statusCode: ruleForm.value.statusCode || undefined,
    })
    testResult.value = result.matched
      ? `会在采集前过滤，命中 ${result.rules.length} 条规则`
      : '不会过滤，会继续采集'
  } catch (e) {
    testResult.value = e instanceof Error ? e.message : '测试失败'
  }
}

onMounted(loadSettings)
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="采集设置" description="查看日志留存策略与访问日志采集规则">
      <template #actions>
        <Button variant="outline" :disabled="loading || retentionLoading || retentionSaving" @click="loadSettings">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <section class="space-y-3 rounded-md border px-4 py-3" aria-labelledby="audit-retention-heading">
      <h2 id="audit-retention-heading" class="font-medium">日志留存设置</h2>
      <form v-if="retention" class="space-y-3" @submit.prevent="saveRetention">
        <div class="grid gap-4 sm:grid-cols-3">
          <div v-for="(label, key) in auditLogTypes" :key="key" class="space-y-1">
            <label :for="'retention-' + key" class="text-sm font-medium">{{ label }}</label>
            <div class="text-sm text-muted-foreground">当前生效 {{ retention.days[key] }} 天</div>
            <Input
              v-if="retention.editable"
              :id="'retention-' + key"
              v-model.number="draftRetention[key]"
              type="number"
              @update:model-value="retentionSaved = false"
              :min="retention.minimumDays"
              :max="retention.maximumDays"
              :disabled="retentionSaving || retentionLoading"
              required
            />
          </div>
        </div>
        <Button v-if="retention.editable" type="submit" :disabled="retentionSaving || retentionLoading">
          {{ retentionSaving ? '等待日志库确认生效…' : '保存留存设置' }}
        </Button>
      </form>
      <p v-else-if="retentionLoading" class="text-sm text-muted-foreground">正在读取生效策略…</p>
      <p v-if="retentionError" role="alert" class="text-sm text-destructive">{{ retentionError }}</p>
      <p v-if="retentionSaved" role="status" class="text-sm">留存设置已生效</p>
    </section>

    <div class="rounded-md border bg-muted/30 px-3 py-2 text-sm text-muted-foreground">
      命中启用规则的访问不会捕获响应体，不发送 RabbitMQ，也不会写入日志存储。系统内置规则默认排除日志查询、访问统计、健康检查和接口文档访问。
    </div>

    <div v-if="error" class="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
      {{ error }}
    </div>

    <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_380px]">
      <DataTableShell :loading="loading" :error="error" :empty="!filterRules.length">
        <Table>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-10 px-3 text-left font-medium">规则</th>
              <th class="h-10 px-3 text-left font-medium">路径</th>
              <th class="h-10 px-3 text-left font-medium">方法</th>
              <th class="h-10 px-3 text-left font-medium">服务</th>
              <th class="h-10 px-3 text-left font-medium">状态</th>
              <th class="h-10 px-3 text-left font-medium">命中</th>
              <th class="h-10 px-3 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in filterRules" :key="rule.ruleId" class="border-b">
              <td class="p-3">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="font-medium">{{ rule.ruleName || '-' }}</span>
                  <Badge :variant="rule.builtin ? 'secondary' : 'outline'">{{ rule.builtin ? '系统内置' : '自定义' }}</Badge>
                  <Badge :variant="rule.enabled === false ? 'outline' : 'default'">{{ rule.enabled === false ? '停用' : '启用' }}</Badge>
                </div>
                <div v-if="rule.remark" class="mt-1 text-xs text-muted-foreground">{{ rule.remark }}</div>
              </td>
              <td class="max-w-[280px] truncate p-3 font-mono text-xs">{{ rule.pathPattern || '-' }}</td>
              <td class="p-3">{{ rule.method || '-' }}</td>
              <td class="max-w-[180px] truncate p-3 text-sm">{{ rule.serviceId || '-' }}</td>
              <td class="p-3">{{ rule.statusCode || '-' }}</td>
              <td class="p-3">{{ rule.hitCount ?? 0 }}</td>
              <td class="p-3 text-right">
                <div v-if="!rule.builtin" class="inline-flex gap-1">
                  <Button size="sm" variant="outline" @click="toggleRule(rule)">
                    {{ rule.enabled === false ? '启用' : '停用' }}
                  </Button>
                  <Button size="sm" variant="outline" title="编辑" @click="editRule(rule)">
                    <Pencil class="h-3.5 w-3.5" />
                  </Button>
                  <Button size="sm" variant="outline" title="删除" @click="removeRule(rule)">
                    <Trash2 class="h-3.5 w-3.5" />
                  </Button>
                </div>
                <span v-else class="text-xs text-muted-foreground">只读</span>
              </td>
            </tr>
          </tbody>
        </Table>
      </DataTableShell>

      <div class="space-y-3 rounded-md border p-3">
        <div class="flex items-center justify-between">
          <div class="font-medium">{{ ruleTitle }}</div>
          <Button size="sm" variant="outline" @click="resetForm">
            <Plus class="h-3.5 w-3.5" />
            新建
          </Button>
        </div>
        <Input v-model="ruleForm.ruleName" placeholder="规则名称，例如忽略健康检查" />
        <Select v-model="enabledValue">
          <option value="true">启用</option>
          <option value="false">停用</option>
        </Select>
        <Input v-model="ruleForm.pathPattern" placeholder="请求路径，例如 /actuator/**" />
        <Select v-model="ruleForm.method">
          <option value="">全部方法</option>
          <option value="GET">GET</option>
          <option value="POST">POST</option>
          <option value="PUT">PUT</option>
          <option value="DELETE">DELETE</option>
          <option value="PATCH">PATCH</option>
          <option value="OPTIONS">OPTIONS</option>
        </Select>
        <Input v-model="ruleForm.serviceId" placeholder="服务名，可选" />
        <Input v-model="ruleForm.statusCode" placeholder="状态码，可选" />
        <textarea
          v-model="ruleForm.remark"
          class="min-h-20 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          placeholder="备注"
        />
        <div class="flex gap-2">
          <Button class="flex-1" :disabled="saving" @click="saveRule">
            {{ saving ? '保存中...' : '保存规则' }}
          </Button>
          <Button variant="outline" :disabled="saving" @click="resetForm">取消</Button>
        </div>
        <div class="border-t pt-3">
          <div class="mb-2 text-sm font-medium">规则测试</div>
          <div class="flex gap-2">
            <Input v-model="testPath" placeholder="测试路径" />
            <Button variant="outline" @click="runTest">测试</Button>
          </div>
          <div v-if="testResult" class="mt-2 text-sm text-muted-foreground">{{ testResult }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

