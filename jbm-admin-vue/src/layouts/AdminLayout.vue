<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterView, RouterLink } from 'vue-router'
import { Bell, ChevronRight, LogOut, Mail, MailOpen, Moon, PanelLeft, Sun } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useMenuStore } from '@/stores/menu'
import { useMessageStore } from '@/stores/messages'
import { updatePassword } from '@/api/current'
import Button from '@/components/ui/Button.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Input from '@/components/ui/Input.vue'
import Label from '@/components/ui/Label.vue'
import JbmLogo from '@/components/JbmLogo.vue'
import { useDocImageSrc } from '@/composables/useDocImageSrc'
import { extractApiError } from '@/lib/errors'
import { cn } from '@/lib/utils'
import type { SnowflakeId } from '@/api/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const app = useAppStore()
const menuStore = useMenuStore()
const messageStore = useMessageStore()
const messagesOpen = ref(false)
const passwordReminderDismissed = ref(false)
const passwordForm = ref({
  originPassword: '',
  currentPassword: '',
  confirmPassword: '',
})
const passwordSaving = ref(false)
const passwordError = ref('')
const NAV_GROUP_STATE_KEY = 'jbm_admin_expanded_nav_groups'
const THEME_STORAGE_KEY = 'jbm_admin_theme'
type ThemeMode = 'light' | 'dark'

const navGroups = computed(() => menuStore.navGroups)
const activeNavPath = computed(() => {
  const items = navGroups.value.flatMap((group) => group.items)
  return items
    .filter((item) => route.path === item.to || route.path.startsWith(`${item.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0]?.to
})
const activeGroupLabel = computed(() =>
  navGroups.value.find((group) => group.items.some((item) => item.to === activeNavPath.value))?.label,
)
const expandedGroupLabels = ref<string[]>([])
const unreadLabel = computed(() =>
  messageStore.unreadCount > 99 ? '99+' : String(messageStore.unreadCount),
)
const showPasswordReminder = computed(
  () => auth.mustChangePassword && !passwordReminderDismissed.value,
)

const pageTitle = computed(() => (route.meta.title as string) || 'JBM 管理后台')
const themeMode = ref<ThemeMode>(readInitialTheme())
const isDarkTheme = computed(() => themeMode.value === 'dark')
const avatarSrc = useDocImageSrc(computed(() => auth.user?.avatar))
const userInitial = computed(() => {
  const name = auth.user?.nickName || auth.user?.userName || '管'
  return name.slice(0, 1).toUpperCase()
})

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

function dismissPasswordReminder() {
  passwordReminderDismissed.value = true
  passwordError.value = ''
}

async function submitPasswordChange() {
  passwordError.value = ''
  if (!passwordForm.value.originPassword || !passwordForm.value.currentPassword || !passwordForm.value.confirmPassword) {
    passwordError.value = '请填写当前密码、新密码和确认密码'
    return
  }
  if (passwordForm.value.currentPassword.length < 6) {
    passwordError.value = '新密码至少 6 位'
    return
  }
  passwordSaving.value = true
  try {
    await updatePassword(passwordForm.value)
    auth.clearMustChangePassword()
    passwordReminderDismissed.value = true
    passwordForm.value = {
      originPassword: '',
      currentPassword: '',
      confirmPassword: '',
    }
  } catch (e) {
    passwordError.value = extractApiError(e, '修改密码失败')
  } finally {
    passwordSaving.value = false
  }
}

function readInitialTheme(): ThemeMode {
  if (typeof window === 'undefined') return 'light'
  const stored = localStorage.getItem(THEME_STORAGE_KEY)
  if (stored === 'light' || stored === 'dark') return stored
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme(mode: ThemeMode) {
  if (typeof document === 'undefined') return
  document.documentElement.classList.toggle('dark', mode === 'dark')
  document.documentElement.dataset.theme = mode
}

function toggleTheme() {
  themeMode.value = isDarkTheme.value ? 'light' : 'dark'
  localStorage.setItem(THEME_STORAGE_KEY, themeMode.value)
  applyTheme(themeMode.value)
}

function openProfile() {
  router.push({ name: 'profile' })
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

function messageSource(message: { sysMsg?: boolean; sendUserId?: SnowflakeId }) {
  return message.sysMsg || !message.sendUserId ? '系统通知' : '用户消息'
}

async function toggleMessages() {
  messagesOpen.value = !messagesOpen.value
  if (messagesOpen.value) await messageStore.refreshSummary()
}

async function openMessageCenter() {
  messagesOpen.value = false
  await router.push({ name: 'message-center' })
}

async function markRecentRead(msgId?: string) {
  if (!msgId) return
  await messageStore.read([msgId])
}

function persistExpandedGroups(labels = expandedGroupLabels.value) {
  localStorage.setItem(NAV_GROUP_STATE_KEY, JSON.stringify(labels))
}

function setExpandedGroups(labels: string[]) {
  const validLabels = new Set(navGroups.value.map((group) => group.label))
  expandedGroupLabels.value = [...new Set(labels)].filter((label) => validLabels.has(label))
  persistExpandedGroups()
}

function isGroupExpanded(label: string) {
  return expandedGroupLabels.value.includes(label)
}

function toggleGroup(label: string) {
  if (app.sidebarCollapsed) return
  if (isGroupExpanded(label)) {
    setExpandedGroups(expandedGroupLabels.value.filter((item) => item !== label))
  } else {
    setExpandedGroups([...expandedGroupLabels.value, label])
  }
}

function expandCurrentGroup() {
  const active = activeGroupLabel.value
  if (!active || expandedGroupLabels.value.includes(active)) return
  setExpandedGroups([...expandedGroupLabels.value, active])
}

function loadExpandedGroups() {
  const stored = localStorage.getItem(NAV_GROUP_STATE_KEY)
  if (stored) {
    try {
      const parsed = JSON.parse(stored)
      if (Array.isArray(parsed)) {
        setExpandedGroups(parsed.filter((label): label is string => typeof label === 'string'))
        expandCurrentGroup()
        return
      }
    } catch {
      localStorage.removeItem(NAV_GROUP_STATE_KEY)
    }
  }
  setExpandedGroups(activeGroupLabel.value ? [activeGroupLabel.value] : navGroups.value.slice(0, 1).map((group) => group.label))
}

onMounted(() => {
  applyTheme(themeMode.value)
  loadExpandedGroups()
  messageStore.refreshSummary()
  messageStore.connectRealtime()
})

watch(activeGroupLabel, expandCurrentGroup)

watch(
  () => navGroups.value.map((group) => group.label).join('|'),
  () => {
    setExpandedGroups(expandedGroupLabels.value)
    expandCurrentGroup()
    if (!expandedGroupLabels.value.length && navGroups.value.length) {
      setExpandedGroups([navGroups.value[0].label])
    }
  },
)

watch(
  () => auth.accessToken,
  (token) => {
    if (token) {
      passwordReminderDismissed.value = false
      messageStore.connectRealtime()
    } else {
      passwordReminderDismissed.value = false
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
        <div v-for="group in navGroups" :key="group.label" class="mb-1">
          <button
            v-if="!app.sidebarCollapsed"
            type="button"
            class="mb-1 flex h-8 w-full items-center justify-between rounded-md px-2 text-left text-xs font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            :aria-expanded="isGroupExpanded(group.label)"
            @click="toggleGroup(group.label)"
          >
            <span class="truncate">{{ group.label }}</span>
            <span class="ml-2 flex items-center gap-1">
              <span class="text-[10px] font-normal text-muted-foreground/80">{{ group.items.length }}</span>
              <ChevronRight
                :class="
                  cn(
                    'h-3.5 w-3.5 shrink-0 transition-transform',
                    isGroupExpanded(group.label) && 'rotate-90',
                  )
                "
              />
            </span>
          </button>
          <div v-else class="mx-2 my-2 h-px bg-border" :title="group.label" />
          <div v-show="app.sidebarCollapsed || isGroupExpanded(group.label)" class="space-y-0.5">
            <RouterLink
              v-for="item in group.items"
              :key="item.name"
              :to="item.to"
              :class="
                cn(
                  'flex items-center gap-2 rounded-md px-2 py-2 text-sm transition-colors hover:bg-accent',
                  app.sidebarCollapsed && 'justify-center',
                  activeNavPath === item.to && 'bg-accent font-medium text-accent-foreground',
                )
              "
              :title="item.title"
            >
              <component :is="item.icon" class="h-4 w-4 shrink-0" />
              <span v-if="!app.sidebarCollapsed" class="truncate">{{ item.title }}</span>
            </RouterLink>
          </div>
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
          <Button
            variant="ghost"
            size="icon"
            :title="isDarkTheme ? '切换白色皮肤' : '切换黑色皮肤'"
            @click="toggleTheme"
          >
            <Sun v-if="isDarkTheme" class="h-4 w-4" />
            <Moon v-else class="h-4 w-4" />
          </Button>
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
          <button
            type="button"
            class="flex min-w-0 items-center gap-2 rounded-md px-2 py-1.5 text-left transition-colors hover:bg-accent hover:text-accent-foreground"
            title="个人中心"
            @click="openProfile"
          >
            <img
              v-if="avatarSrc"
              :src="avatarSrc"
              alt="头像"
              class="h-8 w-8 shrink-0 rounded-full border object-cover"
            />
            <span
              v-else
              class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border bg-muted text-xs font-semibold"
            >
              {{ userInitial }}
            </span>
            <span class="hidden min-w-0 text-right text-sm sm:block">
              <span class="block truncate">{{ auth.user?.nickName || auth.user?.userName || '管理员' }}</span>
              <span v-if="roleHint" class="block truncate text-xs text-muted-foreground">{{ roleHint }}</span>
            </span>
          </button>
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

    <Dialog
      :open="showPasswordReminder"
      title="建议修改初始密码"
      class="max-w-md"
      @update:open="(v) => { if (!v) dismissPasswordReminder() }"
    >
      <p class="mb-4 text-sm text-muted-foreground">
        当前账号仍在使用默认或重置后的密码。可以现在修改，也可以稍后在个人中心处理。
      </p>
      <form class="space-y-4" @submit.prevent="submitPasswordChange">
        <div class="space-y-2">
          <Label>当前密码</Label>
          <Input v-model="passwordForm.originPassword" type="password" autocomplete="current-password" />
        </div>
        <div class="space-y-2">
          <Label>新密码</Label>
          <Input v-model="passwordForm.currentPassword" type="password" autocomplete="new-password" />
        </div>
        <div class="space-y-2">
          <Label>确认新密码</Label>
          <Input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" />
        </div>
        <div
          v-if="passwordError"
          role="alert"
          class="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
        >
          {{ passwordError }}
        </div>
        <div class="flex justify-end gap-2">
          <Button type="button" variant="outline" :disabled="passwordSaving" @click="dismissPasswordReminder">
            稍后修改
          </Button>
          <Button type="submit" :disabled="passwordSaving">
            {{ passwordSaving ? '提交中...' : '确认修改' }}
          </Button>
        </div>
      </form>
    </Dialog>
  </div>
</template>
