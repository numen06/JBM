<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { AlertTriangle, BarChart3, CheckCircle2, Mail, MailOpen, RefreshCw, Search, Users } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import UserSearchSelect from '@/components/UserSearchSelect.vue'
import MessageContentCell from '@/components/MessageContentCell.vue'
import { usePagedList } from '@/composables/usePagedList'
import { getMessageRecordStats, listMessageRecords } from '@/api/messages'
import { getUser } from '@/api/user'
import type { BaseUser, PushMessage } from '@/api/types'

const statusFilter = ref<'all' | 'unread'>('all')
const typeFilter = ref<'all' | 'notification' | 'alarm' | 'alert'>('all')
const sourceFilter = ref<'all' | 'system' | 'user'>('all')
const wayFilter = ref<'all' | 'internal' | 'email' | 'sms' | 'wechat' | 'mqtt' | 'app'>('all')
const deliveryFilter = ref<'all' | 'unsent' | 'wait' | 'issued' | 'fail'>('all')
const recUserId = ref<number | null>(null)
const keyword = ref('')
const statsLoading = ref(false)
const userCache = ref<Record<number, BaseUser | null>>({})
const stats = ref({
  total: 0,
  unread: 0,
  read: 0,
  system: 0,
  user: 0,
  failed: 0,
})

const { items, total, page, loading, error, load, pageSize } = usePagedList<PushMessage>(
  (p, s) =>
    listMessageRecords(p, s, {
      keyword: keyword.value || undefined,
      readFlag: statusFilter.value === 'unread' ? false : undefined,
      type: typeFilter.value === 'all' ? undefined : typeFilter.value,
      sourceType: sourceFilter.value === 'all' ? undefined : sourceFilter.value,
      pushWay: wayFilter.value === 'all' ? undefined : wayFilter.value,
      pushStatus: deliveryFilter.value === 'all' ? undefined : deliveryFilter.value,
      recUserId: recUserId.value ?? undefined,
    }),
  12,
)

function toggleFilter(filter: 'all' | 'unread') {
  statusFilter.value = filter
  load(1)
}

function formatTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
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
  await Promise.all([load(page.value), loadStats()])
}

function search() {
  load(1)
}

function filterChanged() {
  load(1)
}

function pushStatusLabel(status?: string) {
  if (status === 'issued') return '已发送'
  if (status === 'wait') return '等待中'
  if (status === 'fail' || status === 'failed') return '失败'
  if (status === 'unsent') return '未投递'
  return status || '-'
}

function userIdLabel(value?: number) {
  const userId = normalizeUserId(value)
  if (userId === 0) return '全局/广播'
  if (userId == null) return '-'
  const user = userCache.value[userId]
  const name = user?.nickName || user?.userName
  return name ? `${name} (${userId})` : `用户 ${userId}`
}

function normalizeUserId(value?: number) {
  if (value == null) return null
  const id = Number(value)
  return Number.isFinite(id) ? id : null
}

function userLink(value?: number) {
  const userId = normalizeUserId(value)
  if (userId == null || userId === 0) return null
  return { name: 'users', query: { userId: String(userId) } }
}

function formatNumber(value: number) {
  return Number(value || 0).toLocaleString()
}

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await getMessageRecordStats()
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  loadStats()
})

watch(
  items,
  async (rows) => {
    const userIds = [...new Set(rows
      .map((message) => normalizeUserId(message.recUserId))
      .filter((id): id is number => id != null && id !== 0 && userCache.value[id] === undefined))]
    if (!userIds.length) return
    const users = await Promise.all(userIds.map(async (id) => {
      try {
        return [id, await getUser(id)] as const
      } catch {
        return [id, null] as const
      }
    }))
    userCache.value = {
      ...userCache.value,
      ...Object.fromEntries(users),
    }
  },
  { immediate: true },
)
</script>

<template>
  <div>
    <PageHeader title="消息管理" description="查询站内消息记录，用于投递追溯和问题排查。">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="搜索标题/内容/用户/消息ID"
          class="w-56"
          @keyup.enter="search"
        />
        <UserSearchSelect
          v-model="recUserId"
          include-broadcast
          placeholder="搜索接收用户"
          class="w-64"
          @update:model-value="filterChanged"
        />
        <Select v-model="sourceFilter" class="w-32" @update:model-value="filterChanged">
          <option value="all">全部来源</option>
          <option value="system">系统通知</option>
          <option value="user">用户消息</option>
        </Select>
        <Select v-model="typeFilter" class="w-32" @update:model-value="filterChanged">
          <option value="all">全部类型</option>
          <option value="notification">通知</option>
          <option value="alarm">警报</option>
          <option value="alert">弹窗</option>
        </Select>
        <Select v-model="wayFilter" class="w-32" @update:model-value="filterChanged">
          <option value="all">全部渠道</option>
          <option value="internal">站内</option>
          <option value="email">邮箱</option>
          <option value="sms">短信</option>
          <option value="wechat">微信</option>
          <option value="mqtt">MQTT</option>
          <option value="app">App</option>
        </Select>
        <Select v-model="deliveryFilter" class="w-32" @update:model-value="filterChanged">
          <option value="all">全部投递</option>
          <option value="unsent">未投递</option>
          <option value="wait">等待中</option>
          <option value="issued">已发送</option>
          <option value="fail">失败</option>
        </Select>
        <Button variant="outline" size="sm" :disabled="loading" @click="search">
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
      </template>
    </PageHeader>

    <div class="mb-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
      <Card>
        <CardContent class="flex items-center justify-between p-4">
          <div>
            <p class="text-xs text-muted-foreground">消息总量</p>
            <p class="mt-1 text-2xl font-semibold">{{ statsLoading ? '-' : formatNumber(stats.total) }}</p>
          </div>
          <BarChart3 class="h-5 w-5 text-muted-foreground" />
        </CardContent>
      </Card>
      <Card>
        <CardContent class="flex items-center justify-between p-4">
          <div>
            <p class="text-xs text-muted-foreground">未读消息</p>
            <p class="mt-1 text-2xl font-semibold">{{ statsLoading ? '-' : formatNumber(stats.unread) }}</p>
          </div>
          <Mail class="h-5 w-5 text-primary" />
        </CardContent>
      </Card>
      <Card>
        <CardContent class="flex items-center justify-between p-4">
          <div>
            <p class="text-xs text-muted-foreground">已读消息</p>
            <p class="mt-1 text-2xl font-semibold">{{ statsLoading ? '-' : formatNumber(stats.read) }}</p>
          </div>
          <CheckCircle2 class="h-5 w-5 text-muted-foreground" />
        </CardContent>
      </Card>
      <Card>
        <CardContent class="flex items-center justify-between p-4">
          <div>
            <p class="text-xs text-muted-foreground">系统通知</p>
            <p class="mt-1 text-2xl font-semibold">{{ statsLoading ? '-' : formatNumber(stats.system) }}</p>
          </div>
          <MailOpen class="h-5 w-5 text-muted-foreground" />
        </CardContent>
      </Card>
      <Card>
        <CardContent class="flex items-center justify-between p-4">
          <div>
            <p class="text-xs text-muted-foreground">用户消息</p>
            <p class="mt-1 text-2xl font-semibold">{{ statsLoading ? '-' : formatNumber(stats.user) }}</p>
          </div>
          <Users class="h-5 w-5 text-muted-foreground" />
        </CardContent>
      </Card>
      <Card>
        <CardContent class="flex items-center justify-between p-4">
          <div>
            <p class="text-xs text-muted-foreground">投递失败</p>
            <p class="mt-1 text-2xl font-semibold">{{ statsLoading ? '-' : formatNumber(stats.failed) }}</p>
          </div>
          <AlertTriangle class="h-5 w-5 text-destructive" />
        </CardContent>
      </Card>
    </div>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">阅读状态</th>
            <th class="h-10 px-4 text-left font-medium">标题</th>
            <th class="h-10 px-4 text-left font-medium">内容</th>
            <th class="h-10 px-4 text-left font-medium">接收用户</th>
            <th class="h-10 px-4 text-left font-medium">来源</th>
            <th class="h-10 px-4 text-left font-medium">类型</th>
            <th class="h-10 px-4 text-left font-medium">投递</th>
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
              <Badge :variant="message.readFlag ? 'outline' : 'default'">
                <MailOpen v-if="message.readFlag" class="mr-1 h-3.5 w-3.5" />
                <Mail v-else class="mr-1 h-3.5 w-3.5" />
                {{ message.readFlag ? '已读' : '未读' }}
              </Badge>
            </td>
            <td class="max-w-[240px] p-4 font-medium">{{ message.title || '-' }}</td>
            <td class="max-w-[360px] p-4">
              <MessageContentCell :message="message" />
            </td>
            <td class="p-4 text-sm">
              <RouterLink
                v-if="userLink(message.recUserId)"
                :to="userLink(message.recUserId)!"
                class="font-medium text-primary hover:underline"
              >
                {{ userIdLabel(message.recUserId) }}
              </RouterLink>
              <span v-else class="text-muted-foreground">{{ userIdLabel(message.recUserId) }}</span>
            </td>
            <td class="p-4">
              <Badge :variant="sourceVariant(message)">{{ sourceLabel(message) }}</Badge>
            </td>
            <td class="p-4">
              <Badge :variant="typeVariant(message.type)">{{ typeLabel(message.type) }}</Badge>
            </td>
            <td class="p-4 text-sm text-muted-foreground">{{ pushStatusLabel(message.pushStatus) }}</td>
            <td class="p-4 text-sm text-muted-foreground">{{ formatTime(message.createTime) }}</td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>
  </div>
</template>
