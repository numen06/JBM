<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Activity, Play, RefreshCw, Search, Send, Users, X } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/messages'
import { useFeedback } from '@/composables/useFeedback'
import { getPushPerfStatus, sendPushTest, startPushPerfTest } from '@/api/pushTest'
import { listUsersByFilter } from '@/api/user'
import { listOrgTree } from '@/api/org'
import type { BaseOrg, BaseUser, PushTestRequest, PushTestTaskStatus } from '@/api/types'

const auth = useAuthStore()
const messages = useMessageStore()
const feedback = useFeedback()

type TargetMode = 'self' | 'users' | 'tags'
type UserScope = 'all' | 'myOrg' | 'myDept' | 'org' | 'dept'
type FlatOrg = BaseOrg & { depth: number }

const targetMode = ref<TargetMode>('self')
const userScope = ref<UserScope>('myOrg')
const selectedOrgId = ref('')
const userKeyword = ref('')
const userPool = ref<BaseUser[]>([])
const orgOptions = ref<FlatOrg[]>([])
const selectedUserIds = ref<Set<number>>(new Set())
const userPoolLoading = ref(false)
const userPoolTotal = ref(0)
const userPoolPage = ref(1)
const tags = ref('')
const title = ref('Push 通讯测试')
const content = ref('这是一条站内信 WebSocket 闭环测试消息')
const messageCount = ref(100)
const batchSize = ref(20)
const intervalMillis = ref(20)
const sending = ref(false)
const perfRunning = ref(false)
const currentTask = ref<PushTestTaskStatus | null>(null)
let pollTimer: number | undefined

const currentUserId = computed(() => auth.user?.userId)
const selectedUsersText = computed(() => [...selectedUserIds.value].join(','))
const selectedCount = computed(() => selectedUserIds.value.size)
const canLoadUserPool = computed(() => {
  if (targetMode.value !== 'users') return false
  if (userScope.value === 'myOrg') return !!auth.user?.companyId
  if (userScope.value === 'myDept') return !!auth.user?.departmentId
  if (userScope.value === 'org' || userScope.value === 'dept') return selectedOrgId.value !== ''
  return true
})
const ackRate = computed(() => {
  const task = currentTask.value
  if (!task?.sentCount) return '0%'
  return `${Math.round(((task.ackCount ?? 0) / task.sentCount) * 100)}%`
})

function orgId(org: BaseOrg) {
  return org.id ?? org.orgId
}

function flattenOrgs(nodes: BaseOrg[] = [], depth = 0): FlatOrg[] {
  return nodes.flatMap((org) => {
    const row = { ...org, depth }
    return [row, ...flattenOrgs(org.children ?? [], depth + 1)]
  })
}

function orgLabel(org: FlatOrg) {
  return `${org.depth ? `${'　'.repeat(org.depth)}└ ` : ''}${org.orgName ?? org.orgCode ?? orgId(org)}`
}

function userLabel(user: BaseUser) {
  return user.nickName || user.userName || `用户 ${user.userId}`
}

function scopeFilters() {
  const filters: { companyId?: number; departmentId?: number; keyword?: string; status: number } = { status: 1 }
  if (userKeyword.value.trim()) filters.keyword = userKeyword.value.trim()
  if (userScope.value === 'myOrg' && auth.user?.companyId) filters.companyId = auth.user.companyId
  if (userScope.value === 'myDept' && auth.user?.departmentId) filters.departmentId = auth.user.departmentId
  if (userScope.value === 'org' && selectedOrgId.value) filters.companyId = Number(selectedOrgId.value)
  if (userScope.value === 'dept' && selectedOrgId.value) filters.departmentId = Number(selectedOrgId.value)
  return filters
}

async function loadUserPool(page = 1) {
  if (!canLoadUserPool.value) {
    userPool.value = []
    userPoolTotal.value = 0
    return
  }
  userPoolLoading.value = true
  try {
    userPoolPage.value = page
    const data = await listUsersByFilter(page, 20, scopeFilters())
    userPool.value = data?.contents ?? []
    userPoolTotal.value = data?.total ?? userPool.value.length
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '用户池加载失败')
  } finally {
    userPoolLoading.value = false
  }
}

function applyQuickScope(scope: UserScope) {
  if (scope === 'myOrg' && !auth.user?.companyId) {
    feedback.toast.warning('当前账号没有组织信息，已切换到全部用户')
    scope = 'all'
  }
  if (scope === 'myDept' && !auth.user?.departmentId) {
    feedback.toast.warning('当前账号没有部门信息，已切换到全部用户')
    scope = 'all'
  }
  userScope.value = scope
  if (scope !== 'org' && scope !== 'dept') selectedOrgId.value = ''
  loadUserPool(1)
}

function toggleUser(user: BaseUser) {
  if (!user.userId) return
  const next = new Set(selectedUserIds.value)
  if (next.has(user.userId)) next.delete(user.userId)
  else next.add(user.userId)
  selectedUserIds.value = next
}

function selectVisibleUsers() {
  const next = new Set(selectedUserIds.value)
  userPool.value.forEach((user) => {
    if (user.userId) next.add(user.userId)
  })
  selectedUserIds.value = next
}

function clearSelectedUsers() {
  selectedUserIds.value = new Set()
}

async function loadOrgs() {
  try {
    orgOptions.value = flattenOrgs((await listOrgTree()) ?? [])
  } catch {
    orgOptions.value = []
  }
}

function buildRequest(perf = false): PushTestRequest {
  const request: PushTestRequest = {
    title: title.value.trim(),
    content: content.value.trim(),
    pushMsgType: 'notification',
  }
  if (targetMode.value === 'self' && currentUserId.value) request.recUserIds = [currentUserId.value]
  if (targetMode.value === 'users') request.recUserIds = [...selectedUserIds.value]
  if (targetMode.value === 'tags') request.tags = tags.value.trim()
  if (perf) {
    request.messageCount = Number(messageCount.value) || 100
    request.batchSize = Number(batchSize.value) || 20
    request.intervalMillis = Number(intervalMillis.value) || 0
    request.waitAck = true
  }
  return request
}

async function sendOnce() {
  if (targetMode.value === 'users' && selectedUserIds.value.size === 0) {
    feedback.toast.warning('请先从用户池选择接收用户')
    return
  }
  sending.value = true
  try {
    currentTask.value = await sendPushTest(buildRequest())
    feedback.toast.success('测试消息已发送')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '发送失败')
  } finally {
    sending.value = false
  }
}

async function startPerf() {
  if (targetMode.value === 'users' && selectedUserIds.value.size === 0) {
    feedback.toast.warning('请先从用户池选择接收用户')
    return
  }
  perfRunning.value = true
  try {
    currentTask.value = await startPushPerfTest(buildRequest(true))
    startPolling()
  } catch (e) {
    perfRunning.value = false
    feedback.toast.error(e instanceof Error ? e.message : '启动失败')
  }
}

async function refreshTask() {
  if (!currentTask.value?.taskId) return
  currentTask.value = await getPushPerfStatus(currentTask.value.taskId)
  if (currentTask.value?.status !== 'RUNNING') {
    perfRunning.value = false
    stopPolling()
  }
}

function startPolling() {
  stopPolling()
  pollTimer = window.setInterval(() => {
    refreshTask().catch(() => undefined)
  }, 1000)
}

function stopPolling() {
  if (pollTimer) window.clearInterval(pollTimer)
  pollTimer = undefined
}

function statusVariant(status?: string) {
  if (status === 'FAILED') return 'destructive'
  if (status === 'RUNNING') return 'default'
  return 'secondary'
}

onMounted(() => {
  messages.connectRealtime()
  loadOrgs()
})

watch(targetMode, (mode) => {
  if (mode !== 'users') return
  if (userScope.value === 'myOrg' && !auth.user?.companyId) userScope.value = 'all'
  if (userScope.value === 'myDept' && !auth.user?.departmentId) userScope.value = 'all'
  loadUserPool(1)
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div>
    <PageHeader title="Push 通讯测试" description="验证 RabbitMQ 入站、站内信落库、WebSocket 到达和 ACK 指标。">
      <template #actions>
        <Badge :variant="messages.wsConnected ? 'default' : 'destructive'">
          {{ messages.wsConnected ? 'WS 已连接' : messages.wsConnecting ? 'WS 连接中' : 'WS 未连接' }}
        </Badge>
        <Button variant="outline" size="sm" @click="messages.connectRealtime">
          <RefreshCw class="h-4 w-4" />
          重连
        </Button>
      </template>
    </PageHeader>

    <div class="grid gap-4 xl:grid-cols-[minmax(0,420px)_1fr]">
      <section class="rounded-md border bg-card p-4">
        <h2 class="mb-4 text-sm font-semibold">发送参数</h2>
        <div class="space-y-3">
          <label class="block text-sm">
            <span class="mb-1 block text-muted-foreground">目标</span>
            <Select v-model="targetMode">
              <option value="self">当前用户</option>
              <option value="users">用户池选择</option>
              <option value="tags">标签 tags</option>
            </Select>
          </label>
          <div v-if="targetMode === 'users'" class="space-y-3 rounded-md border p-3">
            <div class="flex flex-wrap items-center gap-2">
              <Button size="sm" :variant="userScope === 'myOrg' ? 'default' : 'outline'" @click="applyQuickScope('myOrg')">
                本组织
              </Button>
              <Button size="sm" :variant="userScope === 'myDept' ? 'default' : 'outline'" @click="applyQuickScope('myDept')">
                本部门
              </Button>
              <Button size="sm" :variant="userScope === 'all' ? 'default' : 'outline'" @click="applyQuickScope('all')">
                全部
              </Button>
              <Button size="sm" :variant="userScope === 'org' ? 'default' : 'outline'" @click="applyQuickScope('org')">
                指定组织
              </Button>
              <Button size="sm" :variant="userScope === 'dept' ? 'default' : 'outline'" @click="applyQuickScope('dept')">
                指定部门
              </Button>
            </div>
            <label v-if="userScope === 'org' || userScope === 'dept'" class="block text-sm">
              <span class="mb-1 block text-muted-foreground">{{ userScope === 'org' ? '组织' : '部门' }}</span>
              <Select v-model="selectedOrgId" @update:model-value="loadUserPool(1)">
                <option value="">请选择</option>
                <option v-for="org in orgOptions" :key="orgId(org)" :value="orgId(org)">
                  {{ orgLabel(org) }}
                </option>
              </Select>
            </label>
            <div class="flex gap-2">
              <Input v-model="userKeyword" placeholder="搜索用户名" @keyup.enter="loadUserPool(1)" />
              <Button variant="outline" @click="loadUserPool(1)">
                <Search class="h-4 w-4" />
                搜索
              </Button>
            </div>
            <div class="flex flex-wrap items-center justify-between gap-2 text-xs text-muted-foreground">
              <span>已选 {{ selectedCount }} 人，当前结果 {{ userPool.length }} / {{ userPoolTotal }}</span>
              <div class="flex gap-2">
                <Button size="sm" variant="outline" :disabled="!userPool.length" @click="selectVisibleUsers">
                  <Users class="h-4 w-4" />
                  选择本页
                </Button>
                <Button size="sm" variant="ghost" :disabled="!selectedCount" @click="clearSelectedUsers">
                  <X class="h-4 w-4" />
                  清空
                </Button>
              </div>
            </div>
            <div class="max-h-56 overflow-auto rounded-md border">
              <label
                v-for="user in userPool"
                :key="user.userId"
                class="flex cursor-pointer items-center gap-3 border-b px-3 py-2 text-sm last:border-b-0 hover:bg-accent"
              >
                <input
                  type="checkbox"
                  class="h-4 w-4"
                  :checked="!!user.userId && selectedUserIds.has(user.userId)"
                  @change="toggleUser(user)"
                />
                <span class="min-w-0 flex-1">
                  <span class="block truncate font-medium">{{ userLabel(user) }}</span>
                  <span class="block truncate text-xs text-muted-foreground">
                    ID {{ user.userId }} · {{ user.userName || '-' }} · 组织 {{ user.companyId || '-' }} · 部门 {{ user.departmentId || '-' }}
                  </span>
                </span>
                <Badge :variant="user.status === 1 ? 'secondary' : 'outline'">
                  {{ user.status === 1 ? '正常' : `状态 ${user.status ?? '-'}` }}
                </Badge>
              </label>
              <div v-if="!userPool.length" class="px-3 py-6 text-center text-sm text-muted-foreground">
                {{ userPoolLoading ? '加载中...' : canLoadUserPool ? '暂无用户' : '请选择组织或部门' }}
              </div>
            </div>
            <div class="flex items-center justify-between gap-2">
              <Button size="sm" variant="outline" :disabled="userPoolPage <= 1 || userPoolLoading" @click="loadUserPool(userPoolPage - 1)">
                上一页
              </Button>
              <span class="text-xs text-muted-foreground">第 {{ userPoolPage }} 页</span>
              <Button
                size="sm"
                variant="outline"
                :disabled="userPoolLoading || userPoolPage * 20 >= userPoolTotal"
                @click="loadUserPool(userPoolPage + 1)"
              >
                下一页
              </Button>
            </div>
            <Input :model-value="selectedUsersText" readonly placeholder="已选用户 ID 会显示在这里" />
          </div>
          <label v-if="targetMode === 'tags'" class="block text-sm">
            <span class="mb-1 block text-muted-foreground">tags</span>
            <Input v-model="tags" placeholder="user:1,2 或 1,2" />
          </label>
          <label class="block text-sm">
            <span class="mb-1 block text-muted-foreground">标题</span>
            <Input v-model="title" />
          </label>
          <label class="block text-sm">
            <span class="mb-1 block text-muted-foreground">内容</span>
            <textarea
              v-model="content"
              class="min-h-24 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </label>
          <div class="flex gap-2">
            <Button :disabled="sending" @click="sendOnce">
              <Send class="h-4 w-4" />
              发送一次
            </Button>
            <Button variant="outline" :disabled="!messages.wsConnected" @click="messages.refreshSummary">
              <RefreshCw class="h-4 w-4" />
              刷新摘要
            </Button>
          </div>
        </div>
      </section>

      <section class="rounded-md border bg-card p-4">
        <h2 class="mb-4 text-sm font-semibold">轻压测</h2>
        <div class="grid gap-3 md:grid-cols-3">
          <label class="block text-sm">
            <span class="mb-1 block text-muted-foreground">消息数</span>
            <Input v-model="messageCount" type="number" min="1" max="5000" />
          </label>
          <label class="block text-sm">
            <span class="mb-1 block text-muted-foreground">批次大小</span>
            <Input v-model="batchSize" type="number" min="1" max="500" />
          </label>
          <label class="block text-sm">
            <span class="mb-1 block text-muted-foreground">间隔 ms</span>
            <Input v-model="intervalMillis" type="number" min="0" />
          </label>
        </div>
        <div class="mt-4 flex flex-wrap gap-2">
          <Button :disabled="perfRunning" @click="startPerf">
            <Play class="h-4 w-4" />
            启动轻压测
          </Button>
          <Button variant="outline" :disabled="!currentTask?.taskId" @click="refreshTask">
            <RefreshCw class="h-4 w-4" />
            刷新任务
          </Button>
        </div>

        <div class="mt-5 grid gap-3 md:grid-cols-4">
          <div class="rounded-md border p-3">
            <p class="text-xs text-muted-foreground">任务状态</p>
            <Badge class="mt-2" :variant="statusVariant(currentTask?.status)">
              {{ currentTask?.status || '-' }}
            </Badge>
          </div>
          <div class="rounded-md border p-3">
            <p class="text-xs text-muted-foreground">发送 / ACK</p>
            <p class="mt-2 text-lg font-semibold">{{ currentTask?.sentCount ?? 0 }} / {{ currentTask?.ackCount ?? 0 }}</p>
          </div>
          <div class="rounded-md border p-3">
            <p class="text-xs text-muted-foreground">ACK 到达率</p>
            <p class="mt-2 text-lg font-semibold">{{ ackRate }}</p>
          </div>
          <div class="rounded-md border p-3">
            <p class="text-xs text-muted-foreground">平均 / 最大延迟</p>
            <p class="mt-2 text-lg font-semibold">{{ currentTask?.avgLatencyMs ?? 0 }} / {{ currentTask?.maxLatencyMs ?? 0 }} ms</p>
          </div>
        </div>

        <div class="mt-5 rounded-md border p-3">
          <div class="mb-2 flex items-center gap-2 text-sm font-medium">
            <Activity class="h-4 w-4" />
            最近实时消息
          </div>
          <pre class="max-h-56 overflow-auto rounded bg-muted p-3 text-xs">{{ messages.lastRealtimeMessage || '暂无实时消息' }}</pre>
        </div>
      </section>
    </div>
  </div>
</template>
