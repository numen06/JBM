import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  deleteMessages,
  getUnreadMessageCount,
  listCurrentMessages,
  markMessagesRead,
  markMessagesUnread,
} from '@/api/messages'
import type { PushMessage } from '@/api/types'

export const useMessageStore = defineStore('messages', () => {
  const recent = ref<PushMessage[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref('')

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
  }

  return {
    recent,
    unreadRecent,
    unreadCount,
    loading,
    error,
    refreshSummary,
    read,
    unread,
    remove,
    clear,
  }
})
