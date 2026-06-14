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

const filterRules = ref<GatewayLogFilterRule[]>([])
const loading = ref(false)
const error = ref('')
const saving = ref(false)
const editingRule = ref<GatewayLogFilterRule | null>(null)
const ruleForm = ref<GatewayLogFilterRulePayload>(defaultRuleForm())
const testPath = ref('/logs/GatewayLogs/findLogs')
const testResult = ref('')

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

onMounted(loadRules)
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="采集设置" description="访问日志入库前的网关采集规则">
      <template #actions>
        <Button variant="outline" :disabled="loading" @click="loadRules">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

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

