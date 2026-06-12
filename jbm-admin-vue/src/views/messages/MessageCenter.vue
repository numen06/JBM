<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { CheckCheck, Mail, MailOpen, RefreshCw, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useFeedback } from '@/composables/useFeedback'
import { listCurrentMessages } from '@/api/messages'
import type { PushMessage } from '@/api/types'
import { useMessageStore } from '@/stores/messages'
import { useAuthStore } from '@/stores/auth'

const feedback = useFeedback()
const messageStore = useMessageStore()
const auth = useAuthStore()
const statusFilter = ref<'all' | 'unread'>('all')
const selectedIds = ref<Set<string>>(new Set())

const { items, total, page, loading, error, load, pageSize } = usePagedList<PushMessage>(
  (p, s) =>
    listCurrentMessages(p, s, {
      readFlag: statusFilter.value === 'unread' ? false : undefined,
    }),
  12,
)

const selectedList = computed(() => [...selectedIds.value])
const canUsePushTest = computed(() => {
  if (auth.user?.userId === 1 || auth.user?.userName?.toLowerCase() === 'admin') return true
  const authorities = auth.user?.authorities?.map((item) => item.authority || item.authorityId || '') ?? []
  return authorities.some((item) =>
    ['message_push_test', 'MENU_message_push_test', 'push_test', 'MENU_push_test', 'ACTION_push:test', 'MENU_push'].includes(item),
  )
})
const allChecked = computed(() => {
  if (!items.value.length) return false
  return items.value.every((message) => message.msgId && selectedIds.value.has(message.msgId))
})

function toggleFilter(filter: 'all' | 'unread') {
  statusFilter.value = filter
  selectedIds.value = new Set()
  load(1)
}

function toggleAll() {
  if (allChecked.value) {
    selectedIds.value = new Set()
    return
  }
  selectedIds.value = new Set(items.value.map((message) => message.msgId).filter(Boolean) as string[])
}

function toggleRow(message: PushMessage) {
  if (!message.msgId) return
  const next = new Set(selectedIds.value)
  if (next.has(message.msgId)) next.delete(message.msgId)
  else next.add(message.msgId)
  selectedIds.value = next
}

function formatTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

function contentText(value: unknown) {
  if (value == null) return '-'
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

function typeLabel(type?: string) {
  if (type === 'alarm') return '警报'
  if (type === 'alert') return '弹窗'
  return '通知'
}

function typeVariant(type?: string) {
  if (type === 'alarm' || type === 'alert') return 'destructive'
  return 'secondary'
}

function sourceLabel(message: PushMessage) {
  return message.sysMsg || !message.sendUserId ? '系统通知' : '用户消息'
}

function sourceVariant(message: PushMessage) {
  return message.sysMsg || !message.sendUserId ? 'secondary' : 'outline'
}

async function refresh() {
  await load(page.value)
  await messageStore.refreshSummary()
}

async function markSelectedRead() {
  const ids = selectedList.value
  if (!ids.length) return
  await messageStore.read(ids)
  selectedIds.value = new Set()
  feedback.toast.success('选中消息已标记为已读')
  await refresh()
}

async function markSelectedUnread() {
  const ids = selectedList.value
  if (!ids.length) return
  await messageStore.unread(ids)
  selectedIds.value = new Set()
  feedback.toast.success('选中消息已标记为未读')
  await refresh()
}

async function deleteSelected() {
  const ids = selectedList.value
  if (!ids.length) return
  const confirmed = await feedback.confirm({
    title: '删除消息',
    message: `确认删除选中的 ${ids.length} 条消息？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await messageStore.remove(ids)
  selectedIds.value = new Set()
  feedback.toast.success('选中消息已删除')
  await refresh()
}
</script>

<template>
  <div>
    <PageHeader title="消息中心" description="查看站内通知、警报与系统弹窗消息。">
      <template #actions>
        <RouterLink v-if="canUsePushTest" to="/messages/push-test">
          <Button variant="outline" size="sm">
            通讯测试
          </Button>
        </RouterLink>
        <div class="inline-flex rounded-md border bg-background p-1">
          <Button
            :variant="statusFilter === 'all' ? 'secondary' : 'ghost'"
            size="sm"
            @click="toggleFilter('all')"
          >
            全部
          </Button>
          <Button
            :variant="statusFilter === 'unread' ? 'secondary' : 'ghost'"
            size="sm"
            @click="toggleFilter('unread')"
          >
            未读
          </Button>
        </div>
        <Button variant="outline" size="sm" :disabled="loading" @click="refresh">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Button variant="outline" size="sm" :disabled="!selectedList.length" @click="markSelectedRead">
        <CheckCheck class="h-4 w-4" />
        标为已读
      </Button>
      <Button variant="outline" size="sm" :disabled="!selectedList.length" @click="markSelectedUnread">
        <Mail class="h-4 w-4" />
        标为未读
      </Button>
      <Button variant="outline" size="sm" :disabled="!selectedList.length" @click="deleteSelected">
        <Trash2 class="h-4 w-4" />
        删除
      </Button>
      <span class="text-sm text-muted-foreground">已选择 {{ selectedList.length }} 条</span>
    </div>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 w-12 px-4 text-left">
              <input type="checkbox" :checked="allChecked" @change="toggleAll" />
            </th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-left font-medium">标题</th>
            <th class="h-10 px-4 text-left font-medium">内容</th>
            <th class="h-10 px-4 text-left font-medium">来源</th>
            <th class="h-10 px-4 text-left font-medium">类型</th>
            <th class="h-10 px-4 text-left font-medium">时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="message in items"
            :key="message.msgId"
            class="border-b align-top"
            :class="!message.readFlag && 'bg-primary/5'"
          >
            <td class="p-4">
              <input
                type="checkbox"
                :checked="!!message.msgId && selectedIds.has(message.msgId)"
                @change="toggleRow(message)"
              />
            </td>
            <td class="p-4">
              <Badge :variant="message.readFlag ? 'outline' : 'default'">
                <MailOpen v-if="message.readFlag" class="mr-1 h-3.5 w-3.5" />
                <Mail v-else class="mr-1 h-3.5 w-3.5" />
                {{ message.readFlag ? '已读' : '未读' }}
              </Badge>
            </td>
            <td class="max-w-[240px] p-4 font-medium">{{ message.title || '-' }}</td>
            <td class="max-w-[420px] p-4 text-sm text-muted-foreground">
              <p class="line-clamp-3 whitespace-pre-line">{{ contentText(message.content) }}</p>
            </td>
            <td class="p-4">
              <Badge :variant="sourceVariant(message)">{{ sourceLabel(message) }}</Badge>
            </td>
            <td class="p-4">
              <Badge :variant="typeVariant(message.type)">{{ typeLabel(message.type) }}</Badge>
            </td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(message.createTime) }}</td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
