import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import {
  deleteMessages,
  getUnreadMessageCount,
  listCurrentMessages,
  markCurrentMessagesAllRead,
  markMessagesRead,
  markMessagesUnread,
} from '@/api/messages'
import { ackPushTestMessage } from '@/api/pushTest'
import { apiBaseUrl } from '@/runtimeConfig'
import { useFeedback } from '@/composables/useFeedback'
import { useAuthStore } from '@/stores/auth'
import type { PushMessage } from '@/api/types'

export const useMessageStore = defineStore('messages', () => {
  const feedback = useFeedback()
  const recent = ref<PushMessage[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref('')
  const wsConnected = ref(false)
  const wsConnecting = ref(false)
  const wsError = ref('')
  const lastRealtimeMessage = ref<PushMessage | null>(null)
  let stompClient: Client | null = null
  let messageSubscription: StompSubscription | null = null

  const unreadRecent = computed(() => recent.value.filter((message) => !message.readFlag))

  async function refreshSummary() {
    loading.value = true
    error.value = ''
    try {
      const [page, count] = await Promise.all([
        listCurrentMessages(1, 5),
        getUnreadMessageCount(),
      ])
      recent.value = page.contents ?? []
      unreadCount.value = count
    } catch (e) {
      error.value = e instanceof Error ? e.message : '消息加载失败'
      recent.value = []
      unreadCount.value = 0
    } finally {
      loading.value = false
    }
  }

  async function read(ids: string[]) {
    await markMessagesRead(ids)
    const idSet = new Set(ids)
    const changedUnread = recent.value.filter(
      (message) => message.msgId && idSet.has(message.msgId) && !message.readFlag,
    ).length
    recent.value = recent.value.map((message) =>
      message.msgId && idSet.has(message.msgId) ? { ...message, readFlag: true } : message,
    )
    unreadCount.value = Math.max(0, unreadCount.value - changedUnread)
  }

  async function readAllCurrent() {
    await markCurrentMessagesAllRead()
    recent.value = recent.value.map((message) => ({ ...message, readFlag: true }))
    unreadCount.value = 0
  }

  async function unread(ids: string[]) {
    await markMessagesUnread(ids)
    const idSet = new Set(ids)
    const changedRead = recent.value.filter(
      (message) => message.msgId && idSet.has(message.msgId) && message.readFlag,
    ).length
    recent.value = recent.value.map((message) =>
      message.msgId && idSet.has(message.msgId) ? { ...message, readFlag: false } : message,
    )
    unreadCount.value += changedRead
  }

  async function remove(ids: string[]) {
    await deleteMessages(ids)
    const idSet = new Set(ids)
    const removedUnread = recent.value.filter(
      (message) => message.msgId && idSet.has(message.msgId) && !message.readFlag,
    ).length
    recent.value = recent.value.filter((message) => !message.msgId || !idSet.has(message.msgId))
    unreadCount.value = Math.max(0, unreadCount.value - removedUnread)
  }

  function clear() {
    recent.value = []
    unreadCount.value = 0
    loading.value = false
    error.value = ''
    lastRealtimeMessage.value = null
  }

  function buildWsUrl() {
    const path = '/push/ws'
    if (/^https?:\/\//i.test(apiBaseUrl)) {
      const url = new URL(apiBaseUrl)
      url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
      url.pathname = joinUrlPath(url.pathname, path)
      url.search = ''
      return url.toString()
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const basePath = apiBaseUrl || '/'
    return `${protocol}//${window.location.host}${joinUrlPath(basePath, path)}`
  }

  function joinUrlPath(base: string, path: string) {
    const normalizedBase = base.replace(/\/+$/, '')
    const normalizedPath = path.replace(/^\/+/, '')
    if (!normalizedBase) return `/${normalizedPath}`
    return `${normalizedBase}/${normalizedPath}`
  }

  function connectRealtime() {
    const auth = useAuthStore()
    if (!auth.accessToken || wsConnected.value || wsConnecting.value) return
    disconnectRealtime()
    wsConnecting.value = true
    wsError.value = ''
    const token = auth.accessToken.startsWith('Bearer ') ? auth.accessToken : `Bearer ${auth.accessToken}`
    stompClient = new Client({
      brokerURL: buildWsUrl(),
      connectHeaders: { Authorization: token },
      reconnectDelay: 3000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 0,
      onConnect: () => {
        wsConnected.value = true
        wsConnecting.value = false
        wsError.value = ''
        messageSubscription = stompClient?.subscribe('/user/queue/messages', handleRealtimeMessage) ?? null
      },
      onDisconnect: () => {
        wsConnected.value = false
        wsConnecting.value = false
        messageSubscription = null
      },
      onStompError: (frame) => {
        wsError.value = frame.headers.message || 'WebSocket连接失败'
      },
      onWebSocketClose: () => {
        wsConnected.value = false
        wsConnecting.value = false
        messageSubscription = null
      },
      onWebSocketError: () => {
        wsError.value = 'WebSocket连接异常'
      },
    })
    stompClient.activate()
  }

  function disconnectRealtime() {
    messageSubscription?.unsubscribe()
    messageSubscription = null
    if (stompClient) {
      const client = stompClient
      stompClient = null
      client.deactivate()
    }
    wsConnected.value = false
    wsConnecting.value = false
  }

  function handleRealtimeMessage(message: IMessage) {
    const payload = JSON.parse(message.body) as PushMessage
    lastRealtimeMessage.value = payload
    if (payload.testRunId) {
      const receivedAt = Date.now()
      ackPushTestMessage({
        testRunId: payload.testRunId,
        msgId: payload.msgId,
        recUserId: payload.recUserId,
        receivedAt,
        latencyMs: payload.clientSentAt ? receivedAt - payload.clientSentAt : undefined,
      }).catch(() => undefined)
      if (!shouldShowTestMessage(payload)) return
    }
    if (payload.msgId && !recent.value.some((item) => item.msgId === payload.msgId)) {
      recent.value = [payload, ...recent.value].slice(0, 5)
    }
    if (!payload.readFlag) unreadCount.value += 1
    notifyRealtimeMessage(payload)
  }

  function notifyRealtimeMessage(message: PushMessage) {
    if (message.testRunId && !shouldShowTestMessage(message)) return
    const source = message.sysMsg || !message.sendUserId ? '系统通知' : '用户消息'
    const title = `${source}：${message.title || '新消息'}`
    const content = contentText(message.content)
    if (message.type === 'alarm' || message.type === 'alert') {
      feedback.toast.warning(content, title)
      return
    }
    feedback.toast.info(content, title)
  }

  function contentText(value: unknown) {
    if (value == null) return ''
    if (typeof value === 'string') return value
    try {
      return JSON.stringify(value)
    } catch {
      return String(value)
    }
  }

  function shouldShowTestMessage(message: PushMessage) {
    return message.extend?.showInMessageCenter === true || message.extend?.showInMessageCenter === 'true'
  }

  return {
    recent,
    unreadRecent,
    unreadCount,
    loading,
    error,
    wsConnected,
    wsConnecting,
    wsError,
    lastRealtimeMessage,
    refreshSummary,
    read,
    readAllCurrent,
    unread,
    remove,
    clear,
    connectRealtime,
    disconnectRealtime,
  }
})
