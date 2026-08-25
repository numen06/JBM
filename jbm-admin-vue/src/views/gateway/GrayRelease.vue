<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { useFeedback } from '@/composables/useFeedback'
import {
  createGrayRule,
  deleteGrayRule,
  listDiscoveryServices,
  listGrayRules,
} from '@/api/gateway'
import type { GatewayGrayRule } from '@/api/types'

const rules = ref<GatewayGrayRule[]>([])
const services = ref<string[]>([])
const loading = ref(false)
const error = ref('')
const dialogOpen = ref(false)
const saving = ref(false)
const formError = ref('')
const feedback = useFeedback()
const form = ref(emptyForm())

function emptyForm() {
  return {
    id: '',
    path: '/**',
    serviceId: '',
    percent: 10,
    headerName: '',
    headerValue: '',
    metadataKey: 'version',
    metadataValue: '',
    stickyHeader: 'X-Gray-Key',
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [ruleRows, serviceRows] = await Promise.all([listGrayRules(), listDiscoveryServices()])
    rules.value = ruleRows
    services.value = serviceRows.map((item) => item.serviceId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载灰度规则失败'
    rules.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = emptyForm()
  formError.value = ''
  dialogOpen.value = true
}

async function save() {
  const value = form.value
  if (!value.id.trim() || !value.path.trim() || !value.serviceId.trim()) {
    formError.value = '规则 ID、匹配路径和目标服务不能为空'
    return
  }
  if (!value.metadataKey.trim() || !value.metadataValue.trim()) {
    formError.value = '目标实例元数据不能为空，例如 version=canary'
    return
  }
  if (value.percent < 1 || value.percent > 100) {
    formError.value = '灰度比例必须在 1 到 100 之间'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await createGrayRule({
      id: value.id.trim(),
      path: value.path.trim(),
      serviceId: value.serviceId.trim(),
      percent: Number(value.percent),
      enabled: true,
      headerName: value.headerName.trim() || undefined,
      headerValue: value.headerValue.trim() || undefined,
      metadata: { [value.metadataKey.trim()]: value.metadataValue.trim() },
      targetInstances: [],
      stickyHeader: value.stickyHeader.trim() || 'X-Gray-Key',
    })
    dialogOpen.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存灰度规则失败'
  } finally {
    saving.value = false
  }
}

async function remove(row: GatewayGrayRule) {
  const confirmed = await feedback.confirm({
    title: '确认删除灰度规则',
    message: `确认删除规则 ${row.id}？删除后流量立即恢复默认实例选择。`,
    variant: 'destructive',
  })
  if (!confirmed) return
  try {
    await deleteGrayRule(row.id)
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '删除灰度规则失败'
  }
}

function metadataText(row: GatewayGrayRule) {
  const values = Object.entries(row.metadata || {}).map(([key, value]) => `${key}=${value}`)
  return values.length ? values.join(', ') : '指定实例'
}

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="灰度发布" description="按路径、Header、稳定流量比例和 Nacos 实例元数据进行灰度引流。">
      <template #actions>
        <Button variant="outline" :disabled="loading" @click="load">
          <RefreshCw class="mr-1 size-4" />刷新
        </Button>
        <Button @click="openCreate"><Plus class="mr-1 size-4" />新增规则</Button>
      </template>
    </PageHeader>

    <DataTableShell :loading="loading" :error="error" :empty="!rules.length">
      <div class="grid gap-3 md:hidden">
        <div v-for="row in rules" :key="row.id" class="rounded-lg border bg-card p-4 shadow-sm">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="font-medium">{{ row.id }}</p>
              <p class="mt-1 break-all font-mono text-xs text-muted-foreground">{{ row.path }}</p>
            </div>
            <Badge>{{ row.percent }}%</Badge>
          </div>
          <dl class="mt-3 grid gap-2 text-sm">
            <div><dt class="text-muted-foreground">服务</dt><dd class="break-all">{{ row.serviceId || '全部服务' }}</dd></div>
            <div><dt class="text-muted-foreground">目标实例</dt><dd class="break-all font-mono text-xs">{{ metadataText(row) }}</dd></div>
            <div><dt class="text-muted-foreground">Header</dt><dd>{{ row.headerName ? `${row.headerName}=${row.headerValue || '*'}` : '不限制' }}</dd></div>
          </dl>
          <Button class="mt-4 w-full" variant="destructive" size="sm" @click="remove(row)">
            <Trash2 class="mr-1 size-4" />删除
          </Button>
        </div>
      </div>
      <Table class="hidden md:table">
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">规则</th>
            <th class="h-10 px-4 text-left font-medium">路径</th>
            <th class="h-10 px-4 text-left font-medium">服务</th>
            <th class="h-10 px-4 text-left font-medium">目标实例</th>
            <th class="h-10 px-4 text-left font-medium">Header</th>
            <th class="h-10 px-4 text-left font-medium">比例</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rules" :key="row.id" class="border-b">
            <td class="p-4 font-medium">{{ row.id }}</td>
            <td class="p-4 font-mono text-xs">{{ row.path }}</td>
            <td class="p-4">{{ row.serviceId || '全部服务' }}</td>
            <td class="p-4 font-mono text-xs">{{ metadataText(row) }}</td>
            <td class="p-4">{{ row.headerName ? `${row.headerName}=${row.headerValue || '*'}` : '—' }}</td>
            <td class="p-4"><Badge>{{ row.percent }}%</Badge></td>
            <td class="p-4 text-right">
              <Button variant="destructive" size="sm" @click="remove(row)">删除</Button>
            </td>
          </tr>
        </tbody>
      </Table>
    </DataTableShell>

    <CrudDialog v-model:open="dialogOpen" title="新增灰度规则" :saving="saving" wide @save="save">
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <div class="grid gap-4 sm:grid-cols-2">
        <FormField label="规则 ID" required><Input v-model="form.id" placeholder="center-canary" /></FormField>
        <FormField label="目标服务" required>
          <Select v-model="form.serviceId">
            <option value="">请选择服务</option>
            <option v-for="service in services" :key="service" :value="service">{{ service }}</option>
          </Select>
        </FormField>
        <FormField label="匹配路径" required><Input v-model="form.path" class="font-mono" placeholder="/center/**" /></FormField>
        <FormField label="流量比例 (%)" required><Input v-model.number="form.percent" type="number" min="1" max="100" /></FormField>
        <FormField label="实例元数据键" required><Input v-model="form.metadataKey" class="font-mono" placeholder="version" /></FormField>
        <FormField label="实例元数据值" required><Input v-model="form.metadataValue" class="font-mono" placeholder="canary" /></FormField>
        <FormField label="Header 名称"><Input v-model="form.headerName" class="font-mono" placeholder="X-Release-Channel" /></FormField>
        <FormField label="Header 值"><Input v-model="form.headerValue" class="font-mono" placeholder="canary" /></FormField>
        <FormField label="稳定分流 Header"><Input v-model="form.stickyHeader" class="font-mono" placeholder="X-Gray-Key" /></FormField>
      </div>
      <p class="text-xs text-muted-foreground">
        规则命中后只会选择元数据匹配的健康 Nacos 实例；相同稳定键会持续命中相同实例。
      </p>
    </CrudDialog>
  </div>
</template>
