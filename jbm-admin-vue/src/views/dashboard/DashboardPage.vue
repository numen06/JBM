<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ChevronDown, ChevronRight, AlertCircle, Info, Activity } from '@lucide/vue'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Skeleton from '@/components/ui/Skeleton.vue'
import Badge from '@/components/ui/Badge.vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { usePermission } from '@/composables/usePermission'
import {
  DASHBOARD_METRICS,
  DASHBOARD_SECTIONS,
  filterSectionsByPermission,
  filterMetricsByPermission,
  isSelfServiceOnly,
  type PermissionContext,
} from '@/views/dashboard/dashboardSections'
import {
  loadDashboardOverview,
  API_MONITOR_PLACEHOLDER,
  type DashboardMetric as ApiMetric,
  type DashboardNotice,
  type AggregatedDashboardIdentity,
} from '@/api/dashboard'

const auth = useAuthStore()
const menuStore = useMenuStore()
const permission = usePermission()

const metricsLoading = ref(false)
const loadedMetricsByKey = ref<Map<string, ApiMetric>>(new Map())
const dashboardNotices = ref<DashboardNotice[]>([])
const dashboardIdentity = ref<AggregatedDashboardIdentity | null>(null)

const permCtx = computed<PermissionContext>(() => ({
  isSuperAdmin: permission.isSuperAdmin.value,
  isRouteAllowed: menuStore.isRouteAllowed,
  allowedMenuCodes: menuStore.allowedMenuCodes,
  hasMenu: permission.hasMenu,
  hasAction: permission.hasAction,
}))

const selfServiceMode = computed(() => isSelfServiceOnly(permCtx.value))

const pageDescription = computed(() =>
  selfServiceMode.value ? '开放平台工作台' : '平台控制台',
)

const roleLabel = computed(() => {
  const roles = auth.user?.roles ?? []
  if (!roles.length) return '—'
  return roles.map((r) => r.roleName || r.roleCode || String(r.roleId ?? '')).join('、')
})

const visibleMenuCount = computed(() =>
  menuStore.navGroups
    .flatMap((g) => g.items)
    .filter((item) => item.to !== '/dashboard')
    .length,
)

const displayNickName = computed(
  () => dashboardIdentity.value?.nickName ?? auth.user?.nickName,
)

const displayVisibleMenuCount = computed(
  () => dashboardIdentity.value?.visibleMenuCount ?? visibleMenuCount.value,
)

const scopeLabel = computed(() => {
  const scope = dashboardIdentity.value?.scope
  if (scope === 'platform') return '平台汇总'
  if (scope === 'app') return '当前应用'
  return null
})

const menuStatusBadge = computed(() => {
  if (!menuStore.loaded) return { variant: 'secondary' as const, text: '加载中' }
  if (menuStore.loadError) return { variant: 'destructive' as const, text: '加载失败' }
  if (menuStore.rawMenus.length === 0) return { variant: 'outline' as const, text: '自助模式' }
  return { variant: 'default' as const, text: '已加载' }
})

const visibleMetricDefs = computed(() => filterMetricsByPermission(DASHBOARD_METRICS, permCtx.value))

const visibleSections = computed(() =>
  filterSectionsByPermission(DASHBOARD_SECTIONS, permCtx.value),
)

const showApiMonitorMetric = computed(() => menuStore.isRouteAllowed('/api/monitor'))

/** API 返回 key 与配置 key 的别名 */
const METRIC_KEY_ALIASES: Record<string, string[]> = {
  onlineUsers: ['onlineUser'],
  onlineUser: ['onlineUsers'],
}

function resolveLoadedMetric(defKey: string, loaderKey?: string): ApiMetric | undefined {
  const candidates = [loaderKey, defKey, ...(METRIC_KEY_ALIASES[defKey] ?? [])].filter(
    (k): k is string => !!k,
  )
  for (const k of candidates) {
    const m = loadedMetricsByKey.value.get(k)
    if (m) return m
  }
  return undefined
}

function displayMetricValue(defKey: string, loaderKey?: string): {
  text: string
  badge?: string
  badgeVariant?: 'default' | 'secondary' | 'outline' | 'destructive'
} {
  if (defKey === 'apiMonitor' || loaderKey === 'apiMonitor') {
    return { text: API_MONITOR_PLACEHOLDER.value as string, badge: '待接入', badgeVariant: 'outline' }
  }

  const loaded = resolveLoadedMetric(defKey, loaderKey)
  if (!loaded) {
    if (metricsLoading.value) return { text: '' }
    return { text: '—' }
  }

  if (loaded.status === 'unavailable') {
    return {
      text: '—',
      badge: typeof loaded.value === 'string' ? loaded.value : '不可用',
      badgeVariant: 'secondary',
    }
  }
  if (loaded.status === 'error') {
    return { text: '—', badge: '加载失败', badgeVariant: 'destructive' }
  }
  if (loaded.status === 'warning') {
    const val = loaded.value
    return {
      text: val === undefined || val === null ? '—' : String(val),
      badge: '异常',
      badgeVariant: 'outline',
    }
  }

  const val = loaded.value
  if (val === undefined || val === null) return { text: '—' }
  return { text: String(val) }
}

const allNotices = computed(() => {
  const list: DashboardNotice[] = [...dashboardNotices.value]
  if (menuStore.loadError) {
    list.unshift({
      key: 'menu-load-error',
      level: 'error',
      message: `菜单加载失败：${menuStore.loadError}。请重新登录或检查 /current/user/menus。`,
    })
  }
  if (selfServiceMode.value) {
    list.push({
      key: 'self-service-hint',
      level: 'info',
      message: '当前为开放平台自助工作台，仅展示开发者相关入口。',
    })
  }
  return list
})

const devEnvOpen = ref(false)
const isDev = import.meta.env.DEV

async function fetchOverview() {
  if (selfServiceMode.value) {
    loadedMetricsByKey.value = new Map()
    dashboardNotices.value = []
    dashboardIdentity.value = null
    metricsLoading.value = false
    return
  }

  metricsLoading.value = true
  try {
    const overview = await loadDashboardOverview(menuStore.isRouteAllowed)
    const map = new Map<string, ApiMetric>()
    for (const m of overview.metrics) {
      map.set(m.key, m)
    }
    loadedMetricsByKey.value = map
    dashboardNotices.value = overview.notices
    dashboardIdentity.value = overview.identity ?? null
  } catch {
    loadedMetricsByKey.value = new Map()
    dashboardNotices.value = [
      {
        key: 'overview-fatal',
        level: 'error',
        message: '仪表盘数据加载失败，请稍后刷新页面。',
      },
    ]
  } finally {
    metricsLoading.value = false
  }
}

onMounted(() => {
  void fetchOverview()
})

watch(selfServiceMode, () => {
  void fetchOverview()
})

const metricCards = computed(() => {
  const cards = visibleMetricDefs.value.map((def) => ({
    def,
    display: displayMetricValue(def.key, def.loaderKey),
  }))
  if (showApiMonitorMetric.value) {
    cards.push({
      def: {
        key: 'apiMonitor',
        title: API_MONITOR_PLACEHOLDER.label,
        icon: Activity,
        path: '/api/monitor',
        description: API_MONITOR_PLACEHOLDER.description,
      },
      display: {
        text: String(API_MONITOR_PLACEHOLDER.value),
        badge: '待接入',
        badgeVariant: 'outline' as const,
      },
    })
  }
  return cards.slice(0, 8)
})
</script>

<template>
  <div class="space-y-6">
    <PageHeader title="仪表盘" :description="pageDescription" />

    <!-- 身份与状态条 -->
    <Card>
      <CardContent class="flex flex-col gap-3 py-4 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
        <div class="space-y-1 text-sm">
          <p>
            <span class="text-muted-foreground">账号：</span>
            <span class="font-medium">{{ auth.user?.userName ?? '—' }}</span>
            <span v-if="displayNickName" class="text-muted-foreground">
              （{{ displayNickName }}）
            </span>
          </p>
          <p>
            <span class="text-muted-foreground">角色：</span>
            <span>{{ roleLabel }}</span>
          </p>
          <p>
            <span class="text-muted-foreground">客户端：</span>
            <code class="rounded bg-muted px-1.5 py-0.5 text-xs">{{ auth.clientId }}</code>
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-3 text-sm">
          <span class="text-muted-foreground">
            可见菜单
            <span class="font-semibold text-foreground">{{ displayVisibleMenuCount }}</span>
            项
          </span>
          <Badge v-if="scopeLabel" variant="outline">{{ scopeLabel }}</Badge>
          <Badge :variant="menuStatusBadge.variant">{{ menuStatusBadge.text }}</Badge>
        </div>
      </CardContent>
    </Card>

    <!-- 待办与风险 / 通知 -->
    <div v-if="allNotices.length" class="space-y-2">
      <div
        v-for="notice in allNotices"
        :key="notice.key"
        class="flex items-start gap-2 rounded-md border px-3 py-2 text-sm"
        :class="
          notice.level === 'error'
            ? 'border-destructive/40 bg-destructive/5 text-destructive'
            : notice.level === 'warning'
              ? 'border-amber-500/40 bg-amber-500/5 text-amber-900 dark:text-amber-200'
              : 'border-border bg-muted/30 text-muted-foreground'
        "
      >
        <AlertCircle v-if="notice.level !== 'info'" class="mt-0.5 h-4 w-4 shrink-0" />
        <Info v-else class="mt-0.5 h-4 w-4 shrink-0" />
        <span>{{ notice.message }}</span>
      </div>
    </div>

    <!-- 关键指标 -->
    <section v-if="metricCards.length && !selfServiceMode">
      <h2 class="mb-3 text-sm font-medium text-muted-foreground">平台状态</h2>
      <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card v-for="{ def, display } in metricCards" :key="def.key">
          <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle class="text-sm font-medium text-muted-foreground">
              {{ def.title }}
            </CardTitle>
            <component :is="def.icon" class="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <Skeleton v-if="metricsLoading" class="h-8 w-20" />
            <div v-else class="flex flex-wrap items-baseline gap-2">
              <span class="text-3xl font-bold tabular-nums">{{ display.text }}</span>
              <span v-if="def.unit && display.text !== '—'" class="text-sm text-muted-foreground">
                {{ def.unit }}
              </span>
              <Badge v-if="display.badge" :variant="display.badgeVariant ?? 'secondary'">
                {{ display.badge }}
              </Badge>
            </div>
            <p v-if="def.description" class="mt-1 text-xs text-muted-foreground">
              {{ def.description }}
            </p>
          </CardContent>
        </Card>
      </div>
    </section>

    <!-- 工作区入口 -->
    <section v-if="visibleSections.length">
      <h2 class="mb-3 text-sm font-medium text-muted-foreground">
        {{ selfServiceMode ? '开放平台' : '治理工作台' }}
      </h2>
      <div class="grid gap-4 lg:grid-cols-2">
        <Card v-for="section in visibleSections" :key="section.key">
          <CardHeader class="pb-2">
            <div class="flex items-center gap-2">
              <component :is="section.icon" class="h-5 w-5 text-muted-foreground" />
              <div>
                <CardTitle class="text-base">{{ section.title }}</CardTitle>
                <p class="text-xs text-muted-foreground">{{ section.description }}</p>
              </div>
            </div>
          </CardHeader>
          <CardContent class="flex flex-col gap-2">
            <RouterLink
              v-for="link in section.links"
              :key="link.key"
              :to="link.to"
              class="flex items-start gap-3 rounded-md border px-3 py-2 text-sm transition-colors hover:bg-accent"
            >
              <component :is="link.icon" class="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
              <span>
                <span class="font-medium">{{ link.title }}</span>
                <span v-if="link.description" class="mt-0.5 block text-xs text-muted-foreground">
                  {{ link.description }}
                </span>
              </span>
            </RouterLink>
          </CardContent>
        </Card>
      </div>
    </section>

    <!-- 开发环境说明（仅 DEV，默认折叠） -->
    <section v-if="isDev" class="border-t pt-4">
      <button
        type="button"
        class="flex w-full items-center gap-2 text-left text-sm font-medium text-muted-foreground hover:text-foreground"
        @click="devEnvOpen = !devEnvOpen"
      >
        <component :is="devEnvOpen ? ChevronDown : ChevronRight" class="h-4 w-4" />
        本地联调提示（jaja7）
      </button>
      <div v-show="devEnvOpen" class="mt-3 space-y-2 text-sm text-muted-foreground">
        <p>
          须同时启动
          <code class="rounded bg-muted px-1">Auth:5555</code>、
          <code class="rounded bg-muted px-1">Center:8888</code>、
          <code class="rounded bg-muted px-1">Gateway:7777</code>，Spring profile
          <code class="rounded bg-muted px-1">jaja7</code>。Center 重启后会自动补全菜单/组织/字典种子数据。
        </p>
        <p>
          空库启动后初始化<strong>超级管理员</strong> + <strong>标准菜单/按钮</strong>（与前端路由一致）；之后由超管在
          「菜单管理」「按钮管理」维护资源，在「角色管理」分配菜单与 ACTION_* 按钮，在「用户管理」创建账号与多凭证。
          默认超管 <code class="rounded bg-muted px-1">admin</code>，client
          <code class="rounded bg-muted px-1">demo/demo123</code>。
        </p>
        <p>
          运维脚本：
          <code class="rounded bg-muted px-1">python scripts/jbm_cluster_ops.py workflow</code>
          或分步：<code class="rounded bg-muted px-1">status</code>、
          <code class="rounded bg-muted px-1">wait</code>、
          <code class="rounded bg-muted px-1">setup-rbac</code>、
          <code class="rounded bg-muted px-1">test-rbac</code>
        </p>
        <p>
          前端与脚本仅访问 Gateway：
          <code class="rounded bg-muted px-1">http://127.0.0.1:7777</code>（含
          <code class="rounded bg-muted px-1">/oauth2</code>、
          <code class="rounded bg-muted px-1">/captcha</code>、Center API）。
        </p>
        <p>鉴权头：<code class="rounded bg-muted px-1">Authorization: Bearer ...</code></p>
      </div>
    </section>
  </div>
</template>
