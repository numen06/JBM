<script setup lang="ts">
import { ref } from 'vue'
import { Eye, RefreshCw, RotateCcw, Webhook } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { useFeedback } from '@/composables/useFeedback'
import { usePagedList } from '@/composables/usePagedList'
import { listWebhookTasks, retryWebhookTask } from '@/api/webhookEvents'
import type { WebhookTask } from '@/api/types'

const keyword = ref('')
const statusFilter = ref('')
const groupFilter = ref('')
const feedback = useFeedback()
const detail = ref<WebhookTask | null>(null)
const retryingId = ref('')

const { items, total, page, loading, error, load, pageSize } = usePagedList<WebhookTask>((p, s) =>
  listWebhookTasks(p, s, {
    businessEventCode: keyword.value,
    eventGroup: groupFilter.value,
    status: statusFilter.value || undefined,
  }),
)

function search() {
  load(1)
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

function statusLabel(status?: string) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'RETRYING') return '重试中'
  if (status === 'PENDING') return '待投递'
  return status || '-'
}

function statusVariant(status?: string) {
  if (status === 'FAILED') return 'destructive'
  if (status === 'SUCCESS') return 'default'
  if (status === 'RETRYING' || status === 'PENDING') return 'secondary'
  return 'outline'
}

function openDetail(row: WebhookTask) {
  detail.value = row
}

async function handleRetry(row: WebhookTask) {
  if (!row.taskId) return
  retryingId.value = row.taskId
  try {
    await retryWebhookTask(row.taskId)
    feedback.toast.success('已重新入队投递')
    await load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '重试失败')
  } finally {
    retryingId.value = ''
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="投递任务"
      description="业务事件经 RabbitMQ 分发后的 Webhook 投递记录；失败任务可通过 RabbitMQ 重试队列自动重投，也可在此手动重试。"
    >
      <template #actions>
        <Input v-model="keyword" placeholder="事件编码" class="w-40" @keyup.enter="search" />
        <Input v-model="groupFilter" placeholder="事件分组" class="w-36" @keyup.enter="search" />
        <Select v-model="statusFilter" class="w-32" @change="search">
          <option value="">全部状态</option>
          <option value="PENDING">待投递</option>
          <option value="RETRYING">重试中</option>
          <option value="SUCCESS">成功</option>
          <option value="FAILED">失败</option>
        </Select>
        <Button variant="outline" title="刷新" @click="load(page)">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
        <RouterLink to="/messages/webhook-configs">
          <Button variant="outline" title="订阅配置">
            <Webhook class="h-4 w-4" />
            订阅配置
          </Button>
        </RouterLink>
      </template>
    </PageHeader>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">任务 ID</th>
            <th class="h-10 px-4 text-left font-medium">事件</th>
            <th class="h-10 px-4 text-left font-medium">分组</th>
            <th class="h-10 px-4 text-left font-medium">目标 URL</th>
            <th class="h-10 px-4 text-left font-medium">HTTP</th>
            <th class="h-10 px-4 text-left font-medium">重试</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-left font-medium">创建时间</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.taskId" class="border-b">
            <td class="p-4 font-mono text-xs">{{ row.taskId }}</td>
            <td class="p-4">
              <div>{{ row.eventName || row.businessEventCode || '-' }}</div>
              <div class="font-mono text-xs text-muted-foreground">{{ row.businessEventCode }}</div>
            </td>
            <td class="p-4">
              <Badge variant="outline">{{ row.eventGroup || '-' }}</Badge>
            </td>
            <td class="p-4">
              <div class="max-w-64 truncate font-mono text-xs" :title="row.taskUrl || row.url">
                {{ row.taskUrl || row.url || '-' }}
              </div>
            </td>
            <td class="p-4">{{ row.httpStatus ?? '-' }}</td>
            <td class="p-4">{{ row.retryNumber ?? 0 }}</td>
            <td class="p-4">
              <Badge :variant="statusVariant(row.status)">{{ statusLabel(row.status) }}</Badge>
            </td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.createTime) }}</td>
            <td class="p-4 text-right">
              <div class="flex justify-end gap-2">
                <Button variant="ghost" size="sm" title="详情" @click="openDetail(row)">
                  <Eye class="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  :disabled="retryingId === row.taskId"
                  @click="handleRetry(row)"
                >
                  <RotateCcw class="h-4 w-4" />
                  重试
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

    <Dialog :open="!!detail" title="投递任务详情" class="max-w-2xl" @update:open="(v) => { if (!v) detail = null }">
      <div v-if="detail" class="space-y-3 text-sm">
        <div><span class="text-muted-foreground">任务 ID：</span>{{ detail.taskId }}</div>
        <div><span class="text-muted-foreground">事件 ID：</span>{{ detail.eventId }}</div>
        <div><span class="text-muted-foreground">状态：</span>{{ statusLabel(detail.status) }}</div>
        <div><span class="text-muted-foreground">HTTP 状态：</span>{{ detail.httpStatus ?? '-' }}</div>
        <div><span class="text-muted-foreground">重试次数：</span>{{ detail.retryNumber ?? 0 }}</div>
        <div><span class="text-muted-foreground">目标 URL：</span><span class="font-mono text-xs">{{ detail.taskUrl }}</span></div>
        <div>
          <span class="text-muted-foreground">请求体：</span>
          <pre class="mt-2 max-h-40 overflow-auto rounded-md bg-muted p-3 font-mono text-xs">{{ detail.request || '{}' }}</pre>
        </div>
        <div>
          <span class="text-muted-foreground">响应体：</span>
          <pre class="mt-2 max-h-40 overflow-auto rounded-md bg-muted p-3 font-mono text-xs">{{ detail.response || '-' }}</pre>
        </div>
        <div v-if="detail.errorMsg">
          <span class="text-muted-foreground">错误信息：</span>
          <pre class="mt-2 max-h-40 overflow-auto rounded-md bg-destructive/10 p-3 font-mono text-xs text-destructive">{{ detail.errorMsg }}</pre>
        </div>
      </div>
    </Dialog>
  </div>
</template>
