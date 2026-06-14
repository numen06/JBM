<script setup lang="ts">
import { ref } from 'vue'
import { Eye, Trash2, RefreshCw, ListChecks } from 'lucide-vue-next'
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
import { cleanJobLogs, listJobLogs } from '@/api/job'
import type { SysJobLog } from '@/api/types'

const keyword = ref('')
const groupFilter = ref('')
const statusFilter = ref('')
const feedback = useFeedback()
const selected = ref<SysJobLog | null>(null)

const { items, total, page, loading, error, load, pageSize } = usePagedList<SysJobLog>((p, s) =>
  listJobLogs(p, s, {
    keyword: keyword.value,
    jobGroup: groupFilter.value,
    status: statusFilter.value,
  }),
)

function logId(row: SysJobLog) {
  return row.jobLogId ?? row.id
}

function search() {
  load(1)
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

function statusLabel(status?: string | number) {
  if (status === 0 || status === '0' || String(status).toLowerCase() === 'success') return '成功'
  if (status === 1 || status === '1' || String(status).toLowerCase() === 'fail') return '失败'
  return status ? String(status) : '-'
}

function statusVariant(status?: string | number) {
  return statusLabel(status) === '失败' ? 'destructive' : 'default'
}

async function handleClean() {
  const confirmed = await feedback.confirm({
    title: '确认清空日志',
    message: '确认清空全部调度日志？',
    variant: 'destructive',
  })
  if (!confirmed) return
  await cleanJobLogs()
  feedback.toast.success('调度日志已清空')
  await load(1)
}
</script>

<template>
  <div>
    <PageHeader title="调度日志" description="Job /sysJobLog">
      <template #actions>
        <Input v-model="keyword" placeholder="任务名称" class="w-40" @keyup.enter="search" />
        <Input v-model="groupFilter" placeholder="任务组" class="w-32" @keyup.enter="search" />
        <Select v-model="statusFilter" class="w-28">
          <option value="">全部状态</option>
          <option value="0">成功</option>
          <option value="1">失败</option>
        </Select>
        <Button variant="outline" title="刷新" @click="load(page)">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
        <RouterLink to="/jobs">
          <Button variant="outline" title="任务管理">
            <ListChecks class="h-4 w-4" />
            任务
          </Button>
        </RouterLink>
        <Button variant="destructive" @click="handleClean">
          <Trash2 class="h-4 w-4" />
          清空
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
            <th class="h-10 px-4 text-left font-medium">调用目标</th>
            <th class="h-10 px-4 text-left font-medium">耗时</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-left font-medium">开始时间</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="logId(row)" class="border-b">
            <td class="p-4">{{ logId(row) }}</td>
            <td class="p-4">{{ row.jobName }}</td>
            <td class="p-4">
              <Badge variant="outline">{{ row.jobGroup || 'DEFAULT' }}</Badge>
            </td>
            <td class="p-4">
              <div class="max-w-96 truncate font-mono text-xs" :title="row.invokeTarget">
                {{ row.invokeTarget || '-' }}
              </div>
              <div v-if="row.jobMessage" class="mt-1 max-w-96 truncate text-xs text-muted-foreground">
                {{ row.jobMessage }}
              </div>
            </td>
            <td class="p-4">{{ row.runTime != null ? `${row.runTime} ms` : '-' }}</td>
            <td class="p-4">
              <Badge :variant="statusVariant(row.status)">
                {{ statusLabel(row.status) }}
              </Badge>
            </td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.startTime || row.createTime) }}</td>
            <td class="p-4 text-right">
              <Button variant="outline" size="icon" title="详情" @click="selected = row">
                <Eye class="h-4 w-4" />
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <Dialog :open="!!selected" title="日志详情" class="max-w-2xl" @update:open="selected = null">
      <div v-if="selected" class="space-y-4">
        <div class="grid gap-3 text-sm md:grid-cols-2">
          <div>
            <span class="text-muted-foreground">任务：</span>
            <span>{{ selected.jobName }}</span>
          </div>
          <div>
            <span class="text-muted-foreground">任务组：</span>
            <span>{{ selected.jobGroup || 'DEFAULT' }}</span>
          </div>
          <div>
            <span class="text-muted-foreground">开始：</span>
            <span>{{ formatTime(selected.startTime || selected.createTime) }}</span>
          </div>
          <div>
            <span class="text-muted-foreground">结束：</span>
            <span>{{ formatTime(selected.stopTime) }}</span>
          </div>
        </div>
        <div>
          <div class="mb-1 text-sm text-muted-foreground">调用目标</div>
          <pre class="max-h-32 overflow-auto rounded-md bg-muted p-3 text-xs">{{ selected.invokeTarget || '-' }}</pre>
        </div>
        <div>
          <div class="mb-1 text-sm text-muted-foreground">日志信息</div>
          <pre class="max-h-40 overflow-auto rounded-md bg-muted p-3 text-xs">{{ selected.jobMessage || '-' }}</pre>
        </div>
        <div>
          <div class="mb-1 text-sm text-muted-foreground">异常信息</div>
          <pre class="max-h-60 overflow-auto rounded-md bg-muted p-3 text-xs">{{ selected.exceptionInfo || '-' }}</pre>
        </div>
      </div>
    </Dialog>
  </div>
</template>
