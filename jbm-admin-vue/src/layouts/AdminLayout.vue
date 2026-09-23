<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterView } from 'vue-router'
import { JbmWorkspaceShell, JbmSidebarNavigation } from '@jbm7/vue-core'
import { Bell, LogOut, Mail, MailOpen } from '@lucide/vue'
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
import { passwordPolicyError } from '@/lib/passwordPolicy'
import type { SnowflakeId } from '@/api/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const app = useAppStore()
const menuStore = useMenuStore()
const messageStore = useMessageStore()
const messagesOpen = ref(false)
const mobileSidebarOpen = ref(false)
const passwordReminderDismissed = ref(false)
const passwordForm = ref({
  originPassword: '',
  currentPassword: '',
  confirmPassword: '',
})
const passwordSaving = ref(false)
const passwordError = ref('')
const navGroups = computed(() => menuStore.navGroups)
const activeNavPath = computed(() => {
  const items = navGroups.value.flatMap((group) => group.items)
  return items
    .filter((item) => route.path === item.to || route.path.startsWith(`${item.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0]?.to
})
const shellNavGroups = computed(() => navGroups.value.map(group => ({ label: group.label, items: group.items.map(item => ({ path: item.to, label: item.title, icon: item.icon, active: activeNavPath.value === item.to })) })))
const unreadLabel = computed(() =>
  messageStore.unreadCount > 99 ? '99+' : String(messageStore.unreadCount),
)
const showPasswordReminder = computed(
  () => auth.mustChangePassword && !passwordReminderDismissed.value,
)

const pageTitle = computed(() => (route.meta.title as string) || 'JBM 管理后台')
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
  window.location.replace('/login')
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
  const policyError = passwordPolicyError(passwordForm.value.currentPassword)
  if (policyError) {
    passwordError.value = policyError
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

onMounted(() => {
  messageStore.refreshSummary()
  messageStore.connectRealtime()
})

watch(() => route.fullPath, () => { mobileSidebarOpen.value = false })

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
  <JbmWorkspaceShell :collapsed="app.sidebarCollapsed" @update:collapsed="app.toggleSidebar()" v-model:mobile-open="mobileSidebarOpen" brand="JBM 管理后台" workspace="JBM 管理工作台" :title="pageTitle">
    <template #logo><JbmLogo alt="JBM" /></template>
    <template #sidebar>
      <p v-if="menuStore.loadError" class="m-3 text-xs text-destructive">{{ menuStore.loadError }}</p>
      <JbmSidebarNavigation :groups="shellNavGroups" @navigate="router.push($event)" />
    </template>
    <template #actions>
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
              class="fixed left-2 right-2 top-16 z-50 rounded-lg border bg-card shadow-xl sm:absolute sm:left-auto sm:right-0 sm:top-11 sm:w-80"
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
          <Button variant="outline" size="sm" aria-label="退出登录" @click="handleLogout">
            <LogOut class="h-4 w-4" />
            <span class="hidden sm:inline">退出</span>
          </Button>
    </template>
    <template #default><RouterView /></template>
    <template #overlays>
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
    </template>
  </JbmWorkspaceShell>
</template>
