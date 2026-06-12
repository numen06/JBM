<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Activity, Play, RefreshCw, Send } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/messages'
import { useFeedback } from '@/composables/useFeedback'
import { getPushPerfStatus, sendPushTest, startPushPerfTest } from '@/api/pushTest'
import type { PushTestRequest, PushTestTaskStatus } from '@/api/types'

const auth = useAuthStore()
const messages = useMessageStore()
const feedback = useFeedback()

const targetMode = ref<'self' | 'users' | 'tags'>('self')
const userIdsText = ref('')
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
const ackRate = computed(() => {
  const task = currentTask.value
  if (!task?.sentCount) return '0%'
  return `${Math.round(((task.ackCount ?? 0) / task.sentCount) * 100)}%`
})

function parseUserIds() {
  return userIdsText.value
    .split(/[,\s;]+/)
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)
}

function buildRequest(perf = false): PushTestRequest {
  const request: PushTestRequest = {
    title: title.value.trim(),
    content: content.value.trim(),
    pushMsgType: 'notification',
  }
  if (targetMode.value === 'self' && currentUserId.value) request.recUserIds = [currentUserId.value]
  if (targetMode.value === 'users') request.recUserIds = parseUserIds()
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
              <option value="users">指定用户 ID</option>
              <option value="tags">标签 tags</option>
            </Select>
          </label>
          <label v-if="targetMode === 'users'" class="block text-sm">
            <span class="mb-1 block text-muted-foreground">用户 ID</span>
            <Input v-model="userIdsText" placeholder="1,2,3" />
          </label>
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
