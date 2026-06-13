<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterView, RouterLink } from 'vue-router'
import { Bell, LogOut, Mail, MailOpen, PanelLeft } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useMenuStore } from '@/stores/menu'
import { useMessageStore } from '@/stores/messages'
import Button from '@/components/ui/Button.vue'
import JbmLogo from '@/components/JbmLogo.vue'
import { cn } from '@/lib/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const app = useAppStore()
const menuStore = useMenuStore()
const messageStore = useMessageStore()
const messagesOpen = ref(false)

const navGroups = computed(() => menuStore.navGroups)
const activeNavPath = computed(() => {
  const items = navGroups.value.flatMap((group) => group.items)
  return items
    .filter((item) => route.path === item.to || route.path.startsWith(`${item.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0]?.to
})
const unreadLabel = computed(() =>
  messageStore.unreadCount > 99 ? '99+' : String(messageStore.unreadCount),
)

const pageTitle = computed(() => (route.meta.title as string) || 'JBM 管理后台')

const roleHint = computed(() => {
  const roles = auth.user?.roles
  if (!roles?.length) return ''
  return roles.map((r) => r.roleName || r.roleCode).join('、')
})

async function handleLogout() {
  messageStore.disconnectRealtime()
  await auth.logout()
  messageStore.clear()
  router.push({ name: 'login' })
}

function formatTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
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

function messageSource(message: { sysMsg?: boolean; sendUserId?: number }) {
  return message.sysMsg || !message.sendUserId ? '系统通知' : '用户消息'
}

async function toggleMessages() {
  messagesOpen.value = !messagesOpen.value
  if (messagesOpen.value) await messageStore.refreshSummary()
}

async function openMessageCenter() {
  messagesOpen.value = false
  await router.push({ name: 'messages' })
}

async function markRecentRead(msgId?: string) {
  if (!msgId) return
  await messageStore.read([msgId])
}

onMounted(() => {
  messageStore.refreshSummary()
  messageStore.connectRealtime()
})

watch(
  () => auth.accessToken,
  (token) => {
    if (token) {
      messageStore.connectRealtime()
    } else {
      messageStore.disconnectRealtime()
      messageStore.clear()
      if (!route.meta.public) {
        router.replace({ name: 'login', query: { redirect: route.fullPath } })
      }
    }
  },
)
</script>

<template>
  <div class="flex min-h-screen bg-muted/30">
    <aside
      :class="
        cn(
          'flex flex-col border-r bg-card transition-all',
          app.sidebarCollapsed ? 'w-16' : 'w-56',
        )
      "
    >
      <div class="flex h-14 items-center gap-2 border-b px-4">
        <JbmLogo class="size-8 rounded-md" alt="JBM" />
        <span v-if="!app.sidebarCollapsed" class="font-semibold">JBM 管理后台</span>
      </div>
      <nav class="flex-1 overflow-y-auto p-2">
        <p
          v-if="menuStore.loadError && !app.sidebarCollapsed"
          class="mb-2 px-2 text-xs text-destructive"
        >
          {{ menuStore.loadError }}
        </p>
        <div v-for="group in navGroups" :key="group.label" class="mb-4">
          <p
            v-if="!app.sidebarCollapsed"
            class="mb-1 px-2 text-xs font-medium text-muted-foreground"
          >
            {{ group.label }}
          </p>
          <RouterLink
            v-for="item in group.items"
            :key="item.name"
            :to="item.to"
            :class="
              cn(
                'mb-0.5 flex items-center gap-2 rounded-md px-2 py-2 text-sm transition-colors hover:bg-accent',
                activeNavPath === item.to && 'bg-accent font-medium text-accent-foreground',
              )
            "
            :title="item.title"
          >
            <component :is="item.icon" class="h-4 w-4 shrink-0" />
            <span v-if="!app.sidebarCollapsed">{{ item.title }}</span>
          </RouterLink>
        </div>
      </nav>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex h-14 items-center justify-between border-b bg-card px-4">
        <div class="flex items-center gap-2">
          <Button variant="ghost" size="icon" @click="app.toggleSidebar">
            <PanelLeft class="h-4 w-4" />
          </Button>
          <span class="text-sm font-medium">{{ pageTitle }}</span>
        </div>
        <div class="flex items-center gap-3">
          <div class="relative">
            <Button
              variant="ghost"
              size="icon"
              title="消息中心"
              class="relative"
              @click="toggleMessages"
            >
              <Bell class="h-4 w-4" />
              <span
                v-if="messageStore.unreadCount > 0"
                class="absolute -right-1 -top-1 min-w-5 rounded-full bg-destructive px-1 text-[10px] font-semibold leading-5 text-destructive-foreground"
              >
                {{ unreadLabel }}
              </span>
            </Button>
            <section
              v-if="messagesOpen"
              class="absolute right-0 top-11 z-50 w-80 rounded-lg border bg-card shadow-xl"
            >
              <header class="flex items-center justify-between border-b px-4 py-3">
                <div>
                  <h2 class="text-sm font-semibold">消息中心</h2>
                  <p class="text-xs text-muted-foreground">未读 {{ messageStore.unreadCount }} 条</p>
                </div>
                <Button variant="ghost" size="sm" @click="openMessageCenter">查看全部</Button>
              </header>
              <div class="max-h-96 overflow-y-auto">
                <p v-if="messageStore.loading" class="px-4 py-6 text-center text-sm text-muted-foreground">
                  正在加载消息...
                </p>
                <p
                  v-else-if="messageStore.error"
                  class="px-4 py-6 text-center text-sm text-destructive"
                >
                  {{ messageStore.error }}
                </p>
                <p
                  v-else-if="!messageStore.recent.length"
                  class="px-4 py-6 text-center text-sm text-muted-foreground"
                >
                  暂无消息
                </p>
                <template v-else>
                  <button
                    v-for="message in messageStore.recent"
                    :key="message.msgId"
                    type="button"
                    class="flex w-full gap-3 border-b px-4 py-3 text-left last:border-b-0 hover:bg-muted/60"
                    @click="markRecentRead(message.msgId)"
                  >
                    <MailOpen
                      v-if="message.readFlag"
                      class="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground"
                    />
                    <Mail v-else class="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                    <span class="min-w-0 flex-1">
                      <span class="block truncate text-sm font-medium">{{ message.title || '未命名消息' }}</span>
                      <span class="mt-1 block text-xs text-muted-foreground">
                        {{ messageSource(message) }}
                      </span>
                      <span class="mt-1 block line-clamp-2 text-xs leading-5 text-muted-foreground">
                        {{ contentText(message.content) }}
                      </span>
                      <span class="mt-1 block text-xs text-muted-foreground">
                        {{ formatTime(message.createTime) }}
                      </span>
                    </span>
                  </button>
                </template>
              </div>
            </section>
          </div>
          <div class="text-right text-sm">
            <p>{{ auth.user?.nickName || auth.user?.userName || '管理员' }}</p>
            <p v-if="roleHint" class="text-xs text-muted-foreground">{{ roleHint }}</p>
          </div>
          <Button variant="outline" size="sm" @click="handleLogout">
            <LogOut class="h-4 w-4" />
            退出
          </Button>
        </div>
      </header>
      <main class="min-w-0 flex-1 overflow-auto p-4 lg:p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
