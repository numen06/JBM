<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Users, UserCheck, AppWindow, Route, Shield, FormInput, ScrollText } from '@lucide/vue'
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
  { title: '角色管理', to: '/system/roles', icon: Shield },
  { title: '应用管理', to: '/system/apps', icon: AppWindow },
  { title: '扩展字段', to: '/system/extend-fields', icon: FormInput },
  { title: '网关路由', to: '/gateway/routes', icon: Route },
  { title: '审计日志', to: '/log/account', icon: ScrollText },
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
    <PageHeader title="仪表盘" description="JBM Cluster Center 概览" />
    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">用户总数</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton v-if="loading" class="h-8 w-20" />
          <p v-else class="text-3xl font-bold">{{ stats?.usersTotal ?? '—' }}</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">在线用户</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton v-if="loading" class="h-8 w-20" />
          <p v-else class="text-3xl font-bold">{{ stats?.onlineUser ?? '—' }}</p>
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
        <p>须同时启动 <code class="rounded bg-muted px-1">Auth:5555</code>、<code class="rounded bg-muted px-1">Center:8888</code>、<code class="rounded bg-muted px-1">Gateway:7777</code>，Spring profile <code class="rounded bg-muted px-1">jaja7</code>。Center 重启后会自动补全菜单/组织/字典种子数据。</p>
        <p>
          空库启动后初始化<strong>超级管理员</strong> + <strong>标准菜单/按钮</strong>（与前端路由一致）；之后由超管在
          「菜单管理」「按钮管理」维护资源，在「角色管理」分配菜单与 ACTION_* 按钮，在「用户管理」创建账号与多凭证。
          默认超管 <code class="rounded bg-muted px-1">admin</code>，client <code class="rounded bg-muted px-1">demo/demo123</code>。
        </p>
        <p>
          运维脚本（检测 / 等待 / 登录 / 造数 / RBAC 断言，避免反复手启 Java）：
          <code class="rounded bg-muted px-1">python scripts/jbm_cluster_ops.py workflow</code>
          或分步：<code class="rounded bg-muted px-1">status</code>、<code class="rounded bg-muted px-1">wait</code>、<code class="rounded bg-muted px-1">setup-rbac</code>、<code class="rounded bg-muted px-1">test-rbac</code>
        </p>
        <p>前端与脚本仅访问 Gateway：<code class="rounded bg-muted px-1">http://127.0.0.1:7777</code>（含 <code class="rounded bg-muted px-1">/oauth2</code>、<code class="rounded bg-muted px-1">/captcha</code>、Center API）。</p>
        <p class="flex items-center gap-1">
          <UserCheck class="h-4 w-4" />
          鉴权头：<code class="rounded bg-muted px-1">Authorization: Bearer ...</code>
        </p>
      </CardContent>
    </Card>
  </div>
</template>
