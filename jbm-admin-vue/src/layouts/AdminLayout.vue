<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, RouterView, RouterLink } from 'vue-router'
import {
  LayoutDashboard,
  Users,
  Shield,
  Menu,
  Building2,
  KeyRound,
  AppWindow,
  BookOpen,
  Route,
  Gauge,
  Globe,
  ScrollText,
  Code2,
  LogOut,
  PanelLeft,
} from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import Button from '@/components/ui/Button.vue'
import { cn } from '@/lib/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const app = useAppStore()

const navGroups = [
  {
    label: '概览',
    items: [{ name: 'dashboard', title: '仪表盘', icon: LayoutDashboard, to: '/dashboard' }],
  },
  {
    label: '系统管理',
    items: [
      { name: 'users', title: '用户管理', icon: Users, to: '/system/users' },
      { name: 'roles', title: '角色管理', icon: Shield, to: '/system/roles' },
      { name: 'menus', title: '菜单管理', icon: Menu, to: '/system/menus' },
      { name: 'orgs', title: '组织管理', icon: Building2, to: '/system/orgs' },
      { name: 'authorities', title: '权限管理', icon: KeyRound, to: '/system/authorities' },
      { name: 'apps', title: '应用管理', icon: AppWindow, to: '/system/apps' },
      { name: 'dicts', title: '字典管理', icon: BookOpen, to: '/system/dicts' },
    ],
  },
  {
    label: '网关管理',
    items: [
      { name: 'gateway-routes', title: '路由管理', icon: Route, to: '/gateway/routes' },
      { name: 'gateway-rate', title: '限流管理', icon: Gauge, to: '/gateway/rate-limit' },
      { name: 'gateway-ip', title: 'IP 限制', icon: Globe, to: '/gateway/ip-limit' },
    ],
  },
  {
    label: '其他',
    items: [
      { name: 'account-logs', title: '审计日志', icon: ScrollText, to: '/log/account' },
      { name: 'developer', title: '开发者', icon: Code2, to: '/developer' },
    ],
  },
]

const pageTitle = computed(() => (route.meta.title as string) || 'JBM 管理后台')

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
        <div
          class="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-xs font-bold text-primary-foreground"
        >
          J
        </div>
        <span v-if="!app.sidebarCollapsed" class="font-semibold">JBM 管理后台</span>
      </div>
      <nav class="flex-1 overflow-y-auto p-2">
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
                route.name === item.name && 'bg-accent font-medium text-accent-foreground',
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
          <span class="text-sm text-muted-foreground">
            {{ auth.user?.nickName || auth.user?.userName || '管理员' }}
          </span>
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
