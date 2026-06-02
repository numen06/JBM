<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { BookOpen, LayoutDashboard, Menu, X } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import Button from '@/components/ui/Button.vue'
import JbmLogo from '@/components/JbmLogo.vue'

const route = useRoute()
const auth = useAuthStore()
const mobileOpen = ref(false)

const navItems = [
  { label: '首页', to: '/' },
  { label: 'API 文档', to: '/docs' },
]

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const dashboardTo = computed(() =>
  auth.isLoggedIn ? { name: 'dashboard' as const } : { name: 'login' as const, query: { redirect: '/dashboard' } },
)
</script>

<template>
  <header class="sticky top-0 z-50 border-b border-border/60 bg-background/80 backdrop-blur-md">
    <div class="mx-auto flex h-16 w-full max-w-[1680px] items-center justify-between px-4 sm:px-6 lg:px-8 xl:px-10">
      <RouterLink to="/" class="flex items-center gap-2.5 font-semibold tracking-tight">
        <JbmLogo class="size-9 rounded-lg shadow-sm" />
        <span class="hidden text-lg sm:inline">JBM 开源平台</span>
      </RouterLink>

      <nav class="hidden items-center gap-1 md:flex" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="rounded-md px-3 py-2 text-sm font-medium transition-colors"
          :class="isActive(item.to) ? 'text-primary' : 'text-muted-foreground hover:text-foreground'"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="hidden items-center gap-2 md:flex">
        <RouterLink v-if="auth.isLoggedIn" :to="dashboardTo">
          <Button variant="ghost" size="sm" class="gap-1.5">
            <LayoutDashboard class="size-4" />
            控制台
          </Button>
        </RouterLink>
        <template v-else>
          <RouterLink to="/login">
            <Button variant="ghost" size="sm">登录</Button>
          </RouterLink>
          <RouterLink to="/register">
            <Button size="sm">注册</Button>
          </RouterLink>
        </template>
      </div>

      <button
        type="button"
        class="inline-flex size-9 items-center justify-center rounded-md border md:hidden"
        aria-label="打开菜单"
        @click="mobileOpen = !mobileOpen"
      >
        <X v-if="mobileOpen" class="size-5" />
        <Menu v-else class="size-5" />
      </button>
    </div>

    <div v-if="mobileOpen" class="border-t bg-background px-4 py-4 md:hidden">
      <nav class="flex flex-col gap-1">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="rounded-md px-3 py-2.5 text-sm font-medium"
          :class="isActive(item.to) ? 'bg-primary/10 text-primary' : 'text-muted-foreground'"
          @click="mobileOpen = false"
        >
          {{ item.label }}
        </RouterLink>
        <RouterLink
          to="/docs"
          class="flex items-center gap-2 rounded-md px-3 py-2.5 text-sm text-muted-foreground"
          @click="mobileOpen = false"
        >
          <BookOpen class="size-4" />
          开发者文档
        </RouterLink>
      </nav>
      <div class="mt-4 flex flex-col gap-2 border-t pt-4">
        <RouterLink v-if="auth.isLoggedIn" :to="dashboardTo" @click="mobileOpen = false">
          <Button variant="outline" class="w-full">进入控制台</Button>
        </RouterLink>
        <template v-else>
          <RouterLink to="/login" @click="mobileOpen = false">
            <Button variant="outline" class="w-full">登录</Button>
          </RouterLink>
          <RouterLink to="/register" @click="mobileOpen = false">
            <Button class="w-full">注册</Button>
          </RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>
