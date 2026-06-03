<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Save } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Badge from '@/components/ui/Badge.vue'
import { listApps } from '@/api/app'
import {
  listGrantableApis,
  listResources,
  getAppAuthorities,
  putAppAuthorities,
  type AuthorityResource,
  type OpenAuthority,
} from '@/api/authority'
import type { BaseApp } from '@/api/types'
import { useFeedback } from '@/composables/useFeedback'

const feedback = useFeedback()
const apps = ref<BaseApp[]>([])
const appFilter = ref('')
const selectedAppId = ref<string>('')
const grantableApis = ref<OpenAuthority[]>([])
const resources = ref<AuthorityResource[]>([])
const selectedAuthorityIds = ref<string[]>([])
const expireTime = ref('')
const authorityFilter = ref('')
const serviceFilter = ref('')
const authorityPage = ref(1)
const authorityPageSize = 50
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const filteredApps = computed(() => {
  const kw = appFilter.value.trim().toLowerCase()
  if (!kw) return apps.value
  return apps.value.filter(
    (a) =>
      a.appName?.toLowerCase().includes(kw) ||
      a.appCode?.toLowerCase().includes(kw),
  )
})

const selectedApp = computed(() =>
  apps.value.find((a) => String(a.appId) === selectedAppId.value),
)

const resourceByAuthority = computed(() => {
  const map = new Map<string, AuthorityResource>()
  for (const r of resources.value) {
    if (r.authority) map.set(r.authority, r)
  }
  return map
})

const services = computed(() => {
  const set = new Set<string>()
  for (const api of grantableApis.value) {
    const service = resourceByAuthority.value.get(api.authority ?? '')?.serviceId
    if (service) set.add(service)
  }
  return [...set].sort((a, b) => a.localeCompare(b))
})

const grantableList = computed(() =>
  grantableApis.value.filter((a) => {
    if (!a.authorityId) return false
    const meta = resourceByAuthority.value.get(a.authority ?? '')
    if (serviceFilter.value && meta?.serviceId !== serviceFilter.value) return false
    const kw = authorityFilter.value.trim().toLowerCase()
    if (!kw) return true
    return (
      a.authority?.toLowerCase().includes(kw) ||
      String(a.authorityId).toLowerCase().includes(kw) ||
      meta?.path?.toLowerCase().includes(kw) ||
      meta?.serviceId?.toLowerCase().includes(kw)
    )
  }),
)

const selectedVisibleCount = computed(() =>
  grantableList.value.filter((api) => selectedAuthorityIds.value.includes(String(api.authorityId))).length,
)

const pagedGrantableList = computed(() => {
  const start = (authorityPage.value - 1) * authorityPageSize
  return grantableList.value.slice(start, start + authorityPageSize)
})

async function loadApps() {
  try {
    const data = await listApps(1, 500)
    apps.value = data.contents ?? []
  } catch {
    apps.value = []
  }
}

async function loadAppPermissions() {
  const appId = Number(selectedAppId.value)
  if (!appId) return
  loading.value = true
  error.value = ''
  serviceFilter.value = ''
  authorityFilter.value = ''
  try {
    const [grantable, current, resourceList] = await Promise.all([
      listGrantableApis(),
      getAppAuthorities(appId),
      listResources(),
    ])
    grantableApis.value = grantable ?? []
    resources.value = resourceList ?? []
    selectedAuthorityIds.value = (current ?? [])
      .map((a) => String(a.authorityId))
      .filter(Boolean)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载权限失败'
  } finally {
    loading.value = false
  }
}

function onAppChange() {
  if (selectedAppId.value) loadAppPermissions()
}

watch([authorityFilter, serviceFilter, selectedAppId], () => {
  authorityPage.value = 1
})

watch(
  () => grantableList.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / authorityPageSize))
    if (authorityPage.value > maxPage) authorityPage.value = maxPage
  },
)

function toggleAuthority(id: string, checked: boolean) {
  if (checked) {
    if (!selectedAuthorityIds.value.includes(id)) selectedAuthorityIds.value.push(id)
  } else {
    selectedAuthorityIds.value = selectedAuthorityIds.value.filter((x) => x !== id)
  }
}

function setVisibleAuthorities(checked: boolean) {
  const visibleIds = grantableList.value.map((api) => String(api.authorityId)).filter(Boolean)
  if (checked) {
    selectedAuthorityIds.value = [...new Set([...selectedAuthorityIds.value, ...visibleIds])]
  } else {
    const remove = new Set(visibleIds)
    selectedAuthorityIds.value = selectedAuthorityIds.value.filter((id) => !remove.has(id))
  }
}

async function savePermissions() {
  const appId = Number(selectedAppId.value)
  if (!appId) return
  saving.value = true
  error.value = ''
  try {
    await putAppAuthorities(
      appId,
      selectedAuthorityIds.value,
      expireTime.value || undefined,
    )
    feedback.toast.success('客户端 API 权限已更新。', '已保存')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function authorityLabel(api: OpenAuthority) {
  const meta = resourceByAuthority.value.get(api.authority ?? '')
  return meta?.path || api.authority || String(api.authorityId)
}

function authorityMeta(api: OpenAuthority) {
  const meta = resourceByAuthority.value.get(api.authority ?? '')
  return meta?.serviceId || '未匹配服务'
}

function riskLabel(api: OpenAuthority) {
  const path = authorityLabel(api).toLowerCase()
  if (/(delete|remove|reset|logout|kickout|expire)/.test(path)) return '高风险'
  if (/(save|update|grant|put|batch)/.test(path)) return '写操作'
  return '普通'
}

function riskVariant(api: OpenAuthority) {
  const risk = riskLabel(api)
  if (risk === '高风险') return 'destructive'
  if (risk === '写操作') return 'secondary'
  return 'outline'
}

onMounted(loadApps)
</script>

<template>
  <div class="space-y-6">
    <PageHeader
      title="客户端权限"
      description="为 OAuth2 应用配置可调用的开放 API。先按服务或路径缩小范围，再批量授权当前筛选结果。"
    />

    <Card>
      <CardContent class="space-y-4 pt-6">
        <div class="grid gap-4 lg:grid-cols-[1fr_1fr_auto]">
          <FormField label="筛选应用">
            <Input v-model="appFilter" placeholder="应用名称 / 编码" />
          </FormField>
          <FormField label="选择客户端" required>
            <Select v-model="selectedAppId" @change="onAppChange">
              <option value="">请选择应用</option>
              <option v-for="a in filteredApps" :key="a.appId" :value="String(a.appId)">
                {{ a.appName }} ({{ a.appCode }})
              </option>
            </Select>
          </FormField>
          <FormField v-if="selectedAppId" label="权限过期时间（可选）">
            <Input v-model="expireTime" type="datetime-local" />
          </FormField>
        </div>
      </CardContent>
    </Card>

    <template v-if="selectedAppId">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 class="text-lg font-semibold">{{ selectedApp?.appName }}</h2>
          <p class="text-sm text-muted-foreground font-mono">{{ selectedApp?.appCode }}</p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">已选 {{ selectedAuthorityIds.length }}</Badge>
          <Badge variant="outline">当前筛选 {{ grantableList.length }}</Badge>
          <Button :disabled="saving || loading" @click="savePermissions">
            <Save class="h-4 w-4" />
            {{ saving ? '保存中...' : '保存权限' }}
          </Button>
        </div>
      </div>

      <p v-if="error" class="text-sm text-destructive">{{ error }}</p>
      <p v-if="loading" class="text-sm text-muted-foreground">加载中...</p>

      <div v-else class="space-y-3 rounded-lg border p-4">
        <div class="flex flex-wrap items-center gap-2">
          <Select v-model="serviceFilter" class="w-64">
            <option value="">全部服务</option>
            <option v-for="service in services" :key="service" :value="service">
              {{ service }}
            </option>
          </Select>
          <Input
            v-model="authorityFilter"
            placeholder="搜索 API 路径 / 服务 / 权限标识"
            class="w-80 max-w-full"
          />
          <Button variant="outline" size="sm" :disabled="!grantableList.length" @click="setVisibleAuthorities(true)">
            全选当前筛选
          </Button>
          <Button
            variant="ghost"
            size="sm"
            :disabled="selectedVisibleCount === 0"
            @click="setVisibleAuthorities(false)"
          >
            取消当前筛选
          </Button>
        </div>

        <div class="max-h-[36rem] space-y-1 overflow-y-auto">
          <label
            v-for="api in pagedGrantableList"
            :key="api.authorityId"
            class="flex cursor-pointer items-start gap-3 rounded px-2 py-2 hover:bg-muted/40"
          >
            <input
              type="checkbox"
              class="mt-1"
              :checked="selectedAuthorityIds.includes(String(api.authorityId))"
              @change="toggleAuthority(String(api.authorityId), ($event.target as HTMLInputElement).checked)"
            />
            <span class="min-w-0 flex-1">
              <span class="break-all font-mono text-sm">{{ authorityLabel(api) }}</span>
              <span class="mt-1 flex flex-wrap gap-2">
                <Badge variant="outline">{{ authorityMeta(api) }}</Badge>
                <Badge :variant="riskVariant(api)">{{ riskLabel(api) }}</Badge>
                <Badge variant="outline">{{ api.authorityId }}</Badge>
              </span>
            </span>
          </label>
          <p v-if="!grantableList.length" class="p-2 text-sm text-muted-foreground">
            没有匹配的 API 权限。
          </p>
        </div>
        <PaginationBar
          :page="authorityPage"
          :total="grantableList.length"
          :page-size="authorityPageSize"
          @change="authorityPage = $event"
        />
      </div>
    </template>

    <p v-else class="text-sm text-muted-foreground">请先选择一个应用（客户端）。</p>
  </div>
</template>
