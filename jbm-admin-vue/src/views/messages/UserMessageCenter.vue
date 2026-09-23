<script setup lang="ts">
import { computed, ref } from 'vue'
import { CheckCheck, Mail, MailOpen, RefreshCw, Search, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Dialog from '@/components/ui/Dialog.vue'
import MessageContentCell from '@/components/MessageContentCell.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useFeedback } from '@/composables/useFeedback'
import { listCurrentMessages } from '@/api/messages'
import type { PushMessage } from '@/api/types'
import { useMessageStore } from '@/stores/messages'

const feedback = useFeedback()
const messageStore = useMessageStore()
const statusFilter = ref<'all' | 'unread'>('all')
const typeFilter = ref<'all' | 'notification' | 'alarm' | 'alert'>('all')
const sourceFilter = ref<'all' | 'system' | 'user'>('all')
const selectedIds = ref<Set<string>>(new Set())
const keyword = ref('')
const openedMessage = ref<PushMessage | null>(null)
const mobileSelectionMode = ref(false)

const { items, total, page, loading, error, load, pageSize } = usePagedList<PushMessage>(
  (p, s) =>
    listCurrentMessages(p, s, {
      keyword: keyword.value || undefined,
      readFlag: statusFilter.value === 'unread' ? false : undefined,
      type: typeFilter.value === 'all' ? undefined : typeFilter.value,
      sourceType: sourceFilter.value === 'all' ? undefined : sourceFilter.value,
    }),
  12,
)

const selectedList = computed(() => [...selectedIds.value])
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

function formatMobileTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function toggleMobileSelectionMode() {
  mobileSelectionMode.value = !mobileSelectionMode.value
  if (!mobileSelectionMode.value) selectedIds.value = new Set()
}

function messagePreview(message: PushMessage) {
  const raw = typeof message.content === 'string' ? message.content : JSON.stringify(message.content ?? '')
  return raw.replace(/\s+/g, ' ').trim() || '暂无内容'
}

async function openMessage(message: PushMessage) {
  openedMessage.value = message
  if (!message.readFlag && message.msgId) {
    await messageStore.read([message.msgId])
    message.readFlag = true
    await messageStore.refreshSummary()
  }
}

function closeMessage(open: boolean) {
  if (!open) openedMessage.value = null
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

function search() {
  selectedIds.value = new Set()
  load(1)
}

function filterChanged() {
  selectedIds.value = new Set()
  load(1)
}

async function markSelectedRead() {
  const ids = selectedList.value
  if (!ids.length) return
  await messageStore.read(ids)
  selectedIds.value = new Set()
  feedback.toast.success('选中消息已标记为已读')
  await refresh()
}

async function markAllRead() {
  await messageStore.readAllCurrent()
  selectedIds.value = new Set()
  feedback.toast.success('全部消息已标记为已读')
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
    <PageHeader title="消息中心" description="查看我的站内消息和未读通知。" />

    <section class="mb-4 rounded-lg border bg-card p-3 shadow-sm" aria-label="消息筛选与批量操作">
      <div class="grid grid-cols-2 gap-2 lg:grid-cols-[minmax(16rem,1fr)_8rem_8rem_auto_auto_auto]">
        <Input
          v-model="keyword"
          placeholder="搜索标题/内容/消息ID"
          class="col-span-2 w-full lg:col-span-1"
          @keyup.enter="search"
        />
        <Select v-model="sourceFilter" class="w-full" @update:model-value="filterChanged">
          <option value="all">全部来源</option>
          <option value="system">系统通知</option>
          <option value="user">用户消息</option>
        </Select>
        <Select v-model="typeFilter" class="w-full" @update:model-value="filterChanged">
          <option value="all">全部类型</option>
          <option value="notification">通知</option>
          <option value="alarm">警报</option>
          <option value="alert">弹窗</option>
        </Select>
        <Button variant="outline" size="sm" class="col-span-2 sm:col-span-1" :disabled="loading" @click="search">
          <Search class="h-4 w-4" />
          搜索
        </Button>
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
      </div>

      <div
        v-if="selectedList.length || messageStore.unreadCount > 0"
        class="mt-3 grid grid-cols-2 items-center gap-2 border-t pt-3 sm:flex sm:flex-wrap"
      >
        <Button v-if="selectedList.length" variant="outline" size="sm" @click="markSelectedRead">
          <CheckCheck class="h-4 w-4" />
          标为已读
        </Button>
        <Button v-if="messageStore.unreadCount > 0" variant="outline" size="sm" @click="markAllRead">
          <CheckCheck class="h-4 w-4" />
          全部已读
        </Button>
        <Button v-if="selectedList.length" variant="outline" size="sm" @click="markSelectedUnread">
          <Mail class="h-4 w-4" />
          标为未读
        </Button>
        <Button v-if="selectedList.length" variant="outline" size="sm" @click="deleteSelected">
          <Trash2 class="h-4 w-4" />
          删除
        </Button>
        <span v-if="selectedList.length" class="col-span-2 text-right text-sm text-muted-foreground sm:ml-auto">已选择 {{ selectedList.length }} 条</span>
      </div>
    </section>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <div class="space-y-3 pb-3 md:hidden" aria-label="消息列表">
        <div class="flex items-center justify-between px-1 text-xs text-muted-foreground">
          <span>共 {{ total }} 条消息</span>
          <div class="flex items-center gap-3">
            <button v-if="mobileSelectionMode" type="button" class="text-primary" @click="toggleAll">{{ allChecked ? '取消全选' : '全选本页' }}</button>
            <button type="button" class="text-primary" @click="toggleMobileSelectionMode">{{ mobileSelectionMode ? '完成' : '批量管理' }}</button>
          </div>
        </div>
        <article
          v-for="message in items"
          :key="message.msgId"
          class="rounded-2xl border bg-card p-4 shadow-sm"
          :class="!message.readFlag && 'border-primary/30 bg-primary/5'"
        >
          <div class="flex items-start gap-3">
            <input
              v-if="mobileSelectionMode"
              type="checkbox"
              class="mt-3 h-4 w-4 shrink-0"
              :aria-label="`选择消息：${message.title || '无标题'}`"
              :checked="!!message.msgId && selectedIds.has(message.msgId)"
              @change="toggleRow(message)"
            />
            <button type="button" class="min-w-0 flex-1 text-left" @click="openMessage(message)">
              <span class="flex items-center justify-between gap-2">
                <span class="flex min-w-0 items-center gap-2">
                  <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                    <Mail class="h-4 w-4" />
                  </span>
                  <span class="truncate text-xs text-muted-foreground">{{ sourceLabel(message) }}</span>
                </span>
                <span class="shrink-0 text-xs text-muted-foreground">{{ formatMobileTime(message.createTime) }}</span>
              </span>
              <span class="mt-2 flex items-center gap-2">
                <span v-if="!message.readFlag" class="h-2 w-2 shrink-0 rounded-full bg-primary" aria-label="未读" />
                <span class="min-w-0 truncate text-sm" :class="message.readFlag ? 'text-foreground/80' : 'font-semibold text-foreground'">{{ message.title || '无标题' }}</span>
              </span>
              <span class="mt-1 block line-clamp-2 text-sm leading-5 text-muted-foreground">{{ messagePreview(message) }}</span>
              <span class="mt-3 inline-flex items-center gap-2 text-xs text-muted-foreground">
                <Badge :variant="typeVariant(message.type)">{{ typeLabel(message.type) }}</Badge>
                <span>{{ message.readFlag ? '已读' : '未读' }}</span>
              </span>
            </button>
          </div>
        </article>
      </div>
      <div class="hidden md:block">
      <Table class="table-fixed md:min-w-[900px]">
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 w-12 px-4 text-left">
              <input type="checkbox" :checked="allChecked" @change="toggleAll" />
            </th>
            <th class="h-10 w-24 px-4 text-left font-medium">状态</th>
            <th class="h-10 w-56 px-4 text-left font-medium">标题</th>
            <th class="h-10 px-4 text-left font-medium">内容</th>
            <th class="h-10 w-28 px-4 text-left font-medium">来源</th>
            <th class="h-10 w-20 px-4 text-left font-medium">类型</th>
            <th class="h-10 w-44 px-4 text-left font-medium">时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="message in items"
            :key="message.msgId"
            class="border-b align-top transition-colors hover:bg-muted/30"
            :class="!message.readFlag && 'bg-primary/10'"
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
            <td class="p-4 font-semibold leading-5 text-foreground">{{ message.title || '-' }}</td>
            <td class="p-4">
              <MessageContentCell :message="message" />
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
      </div>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
    <Dialog :open="!!openedMessage" title="消息详情" class="max-w-lg" @update:open="closeMessage">
      <div v-if="openedMessage" class="space-y-4">
        <div class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <span>{{ sourceLabel(openedMessage) }}</span>
          <span>·</span>
          <span>{{ formatTime(openedMessage.createTime) }}</span>
          <Badge :variant="typeVariant(openedMessage.type)">{{ typeLabel(openedMessage.type) }}</Badge>
        </div>
        <h2 class="text-lg font-semibold leading-6">{{ openedMessage.title || '无标题' }}</h2>
        <div class="max-h-[60vh] overflow-auto whitespace-pre-wrap break-words text-sm leading-6 text-foreground/90">{{ typeof openedMessage.content === 'string' ? openedMessage.content : JSON.stringify(openedMessage.content ?? '', null, 2) }}</div>
      </div>
    </Dialog>
  </div>
</template>
