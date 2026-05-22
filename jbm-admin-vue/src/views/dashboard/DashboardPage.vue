<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Users, UserCheck, AppWindow, Route } from '@lucide/vue'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Skeleton from '@/components/ui/Skeleton.vue'
import { getUserStatistics } from '@/api/user'
import type { UserInfoStatistics } from '@/api/types'

const stats = ref<UserInfoStatistics | null>(null)
const loading = ref(true)

const shortcuts = [
  { title: '用户管理', to: '/system/users', icon: Users },
  { title: '应用管理', to: '/system/apps', icon: AppWindow },
  { title: '路由管理', to: '/gateway/routes', icon: Route },
]

onMounted(async () => {
  try {
    stats.value = await getUserStatistics()
  } catch {
    stats.value = { onlineUser: 0, usersTotal: 0 }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader title="仪表盘" description="JBM Cluster Center 管理概览" />
    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">用户总数</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton v-if="loading" class="h-8 w-20" />
          <p v-else class="text-3xl font-bold">{{ stats?.usersTotal ?? '-' }}</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">在线用户</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton v-if="loading" class="h-8 w-20" />
          <p v-else class="text-3xl font-bold">{{ stats?.onlineUser ?? '-' }}</p>
        </CardContent>
      </Card>
      <Card class="md:col-span-2">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium">快捷入口</CardTitle>
        </CardHeader>
        <CardContent class="flex flex-wrap gap-3">
          <RouterLink
            v-for="s in shortcuts"
            :key="s.to"
            :to="s.to"
            class="flex items-center gap-2 rounded-md border px-4 py-2 text-sm hover:bg-accent"
          >
            <component :is="s.icon" class="h-4 w-4" />
            {{ s.title }}
          </RouterLink>
        </CardContent>
      </Card>
    </div>
    <Card class="mt-6">
      <CardHeader>
        <CardTitle class="text-base">环境说明（jaja7）</CardTitle>
      </CardHeader>
      <CardContent class="space-y-2 text-sm text-muted-foreground">
        <p>Gateway 代理：<code class="rounded bg-muted px-1">127.0.0.1:7777</code></p>
        <p>Center 直连：<code class="rounded bg-muted px-1">127.0.0.1:8888</code></p>
        <p>默认 OAuth 客户端：<code class="rounded bg-muted px-1">demo / demo123</code></p>
        <p class="flex items-center gap-1">
          <UserCheck class="h-4 w-4" />
          请求头自动携带 <code class="rounded bg-muted px-1">Authorization: Bearer ...</code>
        </p>
      </CardContent>
    </Card>
  </div>
</template>
