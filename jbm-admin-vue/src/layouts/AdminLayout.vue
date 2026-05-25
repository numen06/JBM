<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, RouterView, RouterLink } from 'vue-router'
import { LogOut, PanelLeft } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useMenuStore } from '@/stores/menu'
import Button from '@/components/ui/Button.vue'
import JbmLogo from '@/components/JbmLogo.vue'
import { cn } from '@/lib/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const app = useAppStore()
const menuStore = useMenuStore()

const navGroups = computed(() => menuStore.navGroups)

const pageTitle = computed(() => (route.meta.title as string) || 'JBM 管理后台')

const roleHint = computed(() => {
  const roles = auth.user?.roles
  if (!roles?.length) return ''
  return roles.map((r) => r.roleName || r.roleCode).join('、')
})

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'login' })
}
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
                route.path === item.to && 'bg-accent font-medium text-accent-foreground',
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

    <div class="flex flex-1 flex-col">
      <header class="flex h-14 items-center justify-between border-b bg-card px-4">
        <div class="flex items-center gap-2">
          <Button variant="ghost" size="icon" @click="app.toggleSidebar">
            <PanelLeft class="h-4 w-4" />
          </Button>
          <span class="text-sm font-medium">{{ pageTitle }}</span>
        </div>
        <div class="flex items-center gap-3">
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
      <main class="flex-1 overflow-auto p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
