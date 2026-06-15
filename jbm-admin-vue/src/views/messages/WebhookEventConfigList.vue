<script setup lang="ts">
import { computed, ref } from 'vue'
import { Eye, Pencil, Play, RefreshCw, Webhook } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { useCrudForm } from '@/composables/useCrudForm'
import { useFeedback } from '@/composables/useFeedback'
import { usePagedList } from '@/composables/usePagedList'
import {
  listWebhookEventConfigs,
  saveWebhookEventConfig,
  triggerWebhookEvent,
} from '@/api/webhookEvents'
import type { WebhookEventConfig } from '@/api/types'

const keyword = ref('')
const serviceFilter = ref('')
const groupFilter = ref('')
const enableFilter = ref<'' | 'true' | 'false'>('')
const feedback = useFeedback()
const detail = ref<WebhookEventConfig | null>(null)

const enableQuery = computed(() => {
  if (enableFilter.value === 'true') return true
  if (enableFilter.value === 'false') return false
  return ''
})

const { items, total, page, loading, error, load, pageSize } = usePagedList<WebhookEventConfig>((p, s) =>
  listWebhookEventConfigs(p, s, {
    keyword: keyword.value,
    serviceName: serviceFilter.value,
    eventGroup: groupFilter.value,
    enable: enableQuery.value,
  }),
)

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openEdit,
  closeDialog,
} = useCrudForm<WebhookEventConfig>(() => ({
  eventId: '',
  businessEventCode: '',
  eventName: '',
  eventGroup: '',
  eventBody: '{}',
  serviceName: '',
  enable: true,
  global: false,
  url: '',
  methodType: 'POST',
}))

const enableFormValue = computed({
  get: () => (form.value.enable === false ? 'false' : 'true'),
  set: (value: string) => {
    form.value.enable = value !== 'false'
  },
})

const dialogTitle = computed(() => (editing.value ? '编辑订阅配置' : '新建订阅配置'))

function search() {
  load(1)
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

function enableLabel(value?: boolean) {
  return value === false ? '停用' : '启用'
}

function enableVariant(value?: boolean) {
  return value === false ? 'secondary' : 'default'
}

function openDetail(row: WebhookEventConfig) {
  detail.value = row
}

function openEditConfig(row: WebhookEventConfig) {
  openEdit({ ...row })
}

async function saveConfig() {
  if (!form.value.businessEventCode?.trim() || !form.value.url?.trim()) {
    formError.value = '事件编码与投递 URL 不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await saveWebhookEventConfig(form.value)
    feedback.toast.success('配置已保存')
    closeDialog()
    await load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function toggleEnable(row: WebhookEventConfig) {
  if (!row.eventId) return
  const next = row.enable === false
  await saveWebhookEventConfig({ ...row, enable: next })
  feedback.toast.success(next ? '已启用' : '已停用')
  await load(page.value)
}

async function triggerEvent(row: WebhookEventConfig) {
  if (!row.eventId) return
  const confirmed = await feedback.confirm({
    title: '触发投递',
    message: `确认向「${row.eventName || row.businessEventCode}」的订阅目标发送一次测试投递？`,
  })
  if (!confirmed) return
  await triggerWebhookEvent(row.eventId)
  feedback.toast.success('投递任务已入队')
}
</script>

<template>
  <div>
    <PageHeader
      title="事件订阅配置"
      description="各业务服务启动时通过 @BusinessEventListener 动态注册；此处可查看、启停订阅目标并手动触发投递。"
    >
      <template #actions>
        <Input v-model="keyword" placeholder="事件编码 / 名称" class="w-44" @keyup.enter="search" />
        <Input v-model="serviceFilter" placeholder="服务名" class="w-36" @keyup.enter="search" />
        <Input v-model="groupFilter" placeholder="事件分组" class="w-36" @keyup.enter="search" />
        <Select v-model="enableFilter" class="w-28" @change="search">
          <option value="">全部状态</option>
          <option value="true">启用</option>
          <option value="false">停用</option>
        </Select>
        <Button variant="outline" title="刷新" @click="load(page)">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
        <RouterLink to="/messages/webhook-tasks">
          <Button variant="outline" title="投递任务">
            <Webhook class="h-4 w-4" />
            投递任务
          </Button>
        </RouterLink>
      </template>
    </PageHeader>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">事件名称</th>
            <th class="h-10 px-4 text-left font-medium">事件编码</th>
            <th class="h-10 px-4 text-left font-medium">分组</th>
            <th class="h-10 px-4 text-left font-medium">服务</th>
            <th class="h-10 px-4 text-left font-medium">投递 URL</th>
            <th class="h-10 px-4 text-left font-medium">方法</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-left font-medium">更新时间</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.eventId" class="border-b">
            <td class="p-4">{{ row.eventName || '-' }}</td>
            <td class="p-4 font-mono text-xs">{{ row.businessEventCode || '-' }}</td>
            <td class="p-4">
              <Badge variant="outline">{{ row.eventGroup || '-' }}</Badge>
            </td>
            <td class="p-4">{{ row.serviceName || '-' }}</td>
            <td class="p-4">
              <div class="max-w-72 truncate font-mono text-xs" :title="row.url">{{ row.url || '-' }}</div>
            </td>
            <td class="p-4">{{ row.methodType || 'POST' }}</td>
            <td class="p-4">
              <Badge :variant="enableVariant(row.enable)">{{ enableLabel(row.enable) }}</Badge>
            </td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.updateTime || row.createTime) }}</td>
            <td class="p-4 text-right">
              <div class="flex justify-end gap-2">
                <Button variant="ghost" size="sm" title="详情" @click="openDetail(row)">
                  <Eye class="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="sm" title="编辑" @click="openEditConfig(row)">
                  <Pencil class="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="sm" title="触发投递" @click="triggerEvent(row)">
                  <Play class="h-4 w-4" />
                </Button>
                <Button variant="outline" size="sm" @click="toggleEnable(row)">
                  {{ row.enable === false ? '启用' : '停用' }}
                </Button>
              </div>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar
        :page="page"
        :page-size="pageSize"
        :total="total"
        @change="(p, s) => load(p, s)"
      />
    </DataTableShell>

    <CrudDialog
      :open="dialogOpen"
      :title="dialogTitle"
      :saving="saving"
      @update:open="dialogOpen = $event"
      @save="saveConfig"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="事件编码" required>
        <Input v-model="form.businessEventCode" placeholder="如 OrderCreatedEvent" />
      </FormField>
      <FormField label="事件名称">
        <Input v-model="form.eventName" />
      </FormField>
      <FormField label="事件分组">
        <Input v-model="form.eventGroup" />
      </FormField>
      <FormField label="服务名">
        <Input v-model="form.serviceName" placeholder="注册来源服务" />
      </FormField>
      <FormField label="投递 URL" required>
        <Input v-model="form.url" placeholder="feign://service/path 或 https://..." />
      </FormField>
      <FormField label="HTTP 方法">
        <Select v-model="form.methodType">
          <option value="POST">POST</option>
          <option value="GET">GET</option>
        </Select>
      </FormField>
      <FormField label="默认请求体 (JSON)">
        <textarea
          v-model="form.eventBody"
          class="min-h-28 w-full rounded-md border bg-background px-3 py-2 font-mono text-xs"
        />
      </FormField>
      <FormField label="启用">
        <Select v-model="enableFormValue">
          <option value="true">启用</option>
          <option value="false">停用</option>
        </Select>
      </FormField>
    </CrudDialog>

    <Dialog :open="!!detail" title="订阅配置详情" class="max-w-2xl" @update:open="(v) => { if (!v) detail = null }">
      <div v-if="detail" class="space-y-3 text-sm">
        <div><span class="text-muted-foreground">事件 ID：</span>{{ detail.eventId }}</div>
        <div><span class="text-muted-foreground">事件编码：</span>{{ detail.businessEventCode }}</div>
        <div><span class="text-muted-foreground">分组：</span>{{ detail.eventGroup }}</div>
        <div><span class="text-muted-foreground">服务：</span>{{ detail.serviceName }}</div>
        <div><span class="text-muted-foreground">URL：</span><span class="font-mono text-xs">{{ detail.url }}</span></div>
        <div><span class="text-muted-foreground">批次时间：</span>{{ detail.batchTime || '-' }}</div>
        <div>
          <span class="text-muted-foreground">默认请求体：</span>
          <pre class="mt-2 max-h-48 overflow-auto rounded-md bg-muted p-3 font-mono text-xs">{{ detail.eventBody || '{}' }}</pre>
        </div>
      </div>
    </Dialog>
  </div>
</template>
