<script setup lang="ts">
import { computed, ref } from 'vue'
import { Eye, FilePlus2, RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Dialog from '@/components/ui/Dialog.vue'
import { usePagedList } from '@/composables/usePagedList'
import {
  createBusinessLogDemo,
  getBusinessLogContent,
  getBusinessLogLines,
  listBusinessLogs,
  type BusinessLogQuery,
} from '@/api/logs'
import type { BusinessLogLine, BusinessLogSummary } from '@/api/types'

const filters = ref<BusinessLogQuery>({
  module: '',
  operation: '',
  businessType: '',
  source: '',
  status: '',
  keyword: '',
})
const selectedLog = ref<BusinessLogSummary | null>(null)
const selectedContent = ref('')
const selectedLines = ref<BusinessLogLine[]>([])
const detailLoading = ref(false)
const demoWriting = ref(false)

const auditList = usePagedList<BusinessLogSummary>((p, s) => listBusinessLogs(p, s, buildQuery()))
const selectedTitle = computed(() =>
  selectedLog.value?.operation
  || selectedLog.value?.businessType
  || selectedLog.value?.module
  || '审计日志详情',
)

function buildQuery(): BusinessLogQuery {
  return Object.fromEntries(
    Object.entries(filters.value).filter(([, value]) => String(value ?? '').trim() !== ''),
  ) as BusinessLogQuery
}

function search() {
  auditList.load(1)
}

function resetFilters() {
  filters.value = {
    module: '',
    operation: '',
    businessType: '',
    source: '',
    status: '',
    keyword: '',
  }
  search()
}

async function writeDemoLog() {
  demoWriting.value = true
  try {
    await createBusinessLogDemo()
    await auditList.load(1)
  } finally {
    demoWriting.value = false
  }
}

async function openBusinessLog(row: BusinessLogSummary) {
  if (!row.logId) return
  selectedLog.value = row
  selectedContent.value = ''
  selectedLines.value = []
  detailLoading.value = true
  try {
    const [content, lines] = await Promise.all([
      getBusinessLogContent(row.logId),
      getBusinessLogLines(row.logId),
    ])
    selectedContent.value = content || ''
    selectedLines.value = lines ?? []
  } finally {
    detailLoading.value = false
  }
}

function formatTime(t?: string) {
  if (!t) return '-'
  const time = new Date(t)
  if (Number.isNaN(time.getTime())) return t
  return time.toLocaleString()
}
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="审计日志" description="BusinessLog / businessLog">
      <template #actions>
        <Button variant="outline" :disabled="demoWriting" @click="writeDemoLog">
          <FilePlus2 class="h-4 w-4" />
          测试写入
        </Button>
        <Button variant="outline" :disabled="auditList.loading.value" @click="auditList.load(auditList.page.value)">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <div class="grid gap-3 rounded-lg border p-3 md:grid-cols-6">
      <Input v-model="filters.module" placeholder="业务模块" @keyup.enter="search" />
      <Input v-model="filters.operation" placeholder="操作" @keyup.enter="search" />
      <Input v-model="filters.businessType" placeholder="业务类型" @keyup.enter="search" />
      <Input v-model="filters.source" placeholder="来源系统" @keyup.enter="search" />
      <Select v-model="filters.status">
        <option value="">全部状态</option>
        <option value="ACTIVE">ACTIVE</option>
        <option value="DONE">DONE</option>
        <option value="FAILED">FAILED</option>
      </Select>
      <Input v-model="filters.keyword" placeholder="关键词" @keyup.enter="search" />
      <div class="flex gap-2 md:col-span-6">
        <Button size="sm" @click="search">搜索</Button>
        <Button size="sm" variant="outline" @click="resetFilters">重置</Button>
      </div>
    </div>

    <DataTableShell
      :loading="auditList.loading.value"
      :error="auditList.error.value"
      :empty="!auditList.items.value.length"
    >
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">业务</th>
            <th class="h-10 px-4 text-left font-medium">操作</th>
            <th class="h-10 px-4 text-left font-medium">来源</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-left font-medium">行数</th>
            <th class="h-10 px-4 text-left font-medium">更新时间</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in auditList.items.value" :key="row.logId" class="border-b">
            <td class="p-4">
              <div class="font-medium">{{ row.businessType || row.module || '-' }}</div>
              <div class="text-xs text-muted-foreground">{{ row.module || row.businessId || '-' }}</div>
            </td>
            <td class="p-4">{{ row.operation || '-' }}</td>
            <td class="p-4">{{ row.source || row.username || '-' }}</td>
            <td class="p-4">{{ row.status || '-' }}</td>
            <td class="p-4">{{ row.totalLines ?? 0 }}</td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.updateTime || row.createTime) }}</td>
            <td class="p-4 text-right">
              <Button size="sm" variant="outline" @click="openBusinessLog(row)">
                <Eye class="h-4 w-4" />
                详情
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar
        :page="auditList.page.value"
        :total="auditList.total.value"
        :page-size="auditList.pageSize.value"
        @change="auditList.load"
      />
    </DataTableShell>

    <Dialog :open="!!selectedLog" :title="selectedTitle" class="max-w-4xl" @update:open="selectedLog = null">
      <div v-if="detailLoading" class="p-6 text-sm text-muted-foreground">加载中...</div>
      <div v-else class="space-y-4">
        <div class="grid gap-3 text-sm md:grid-cols-4">
          <div>
            <div class="text-muted-foreground">业务模块</div>
            <div>{{ selectedLog?.module || '-' }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">业务类型</div>
            <div>{{ selectedLog?.businessType || selectedLog?.module || '-' }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">操作</div>
            <div>{{ selectedLog?.operation || '-' }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">来源</div>
            <div>{{ selectedLog?.source || selectedLog?.username || '-' }}</div>
          </div>
          <div>
            <div class="text-muted-foreground">行数</div>
            <div>{{ selectedLines.length }}</div>
          </div>
        </div>
        <pre class="max-h-[52vh] overflow-auto rounded-lg border bg-muted/30 p-4 text-sm leading-6">{{ selectedContent || '暂无内容' }}</pre>
      </div>
    </Dialog>
  </div>
</template>
