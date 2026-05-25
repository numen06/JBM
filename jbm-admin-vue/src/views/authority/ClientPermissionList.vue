<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Save } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
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
  getAppAuthorities,
  putAppAuthorities,
  type OpenAuthority,
} from '@/api/authority'
import type { BaseApp } from '@/api/types'
import { useFeedback } from '@/composables/useFeedback'

const feedback = useFeedback()
const apps = ref<BaseApp[]>([])
const appFilter = ref('')
const selectedAppId = ref<string>('')
const grantableApis = ref<OpenAuthority[]>([])
const selectedAuthorityIds = ref<string[]>([])
const expireTime = ref('')
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
  try {
    const [grantable, current] = await Promise.all([
      listGrantableApis(),
      getAppAuthorities(appId),
    ])
    grantableApis.value = grantable ?? []
    selectedAuthorityIds.value = (current ?? [])
      .map((a) => String(a.authorityId))
      .filter(Boolean)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function onAppChange() {
  if (selectedAppId.value) loadAppPermissions()
}

function toggleAuthority(id: string, checked: boolean) {
  if (checked) {
    if (!selectedAuthorityIds.value.includes(id)) {
      selectedAuthorityIds.value.push(id)
    }
  } else {
    selectedAuthorityIds.value = selectedAuthorityIds.value.filter((x) => x !== id)
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
    await feedback.alert({ title: '已保存', message: '客户端 API 权限已更新。' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

const grantableList = computed(() =>
  grantableApis.value.filter((a) => a.authorityId),
)

onMounted(loadApps)
</script>

<template>
  <div class="space-y-6">
    <PageHeader
      title="客户端权限"
      description="为 OAuth2 应用（客户端）配置可调用的开放 API；对应 Center PUT /authority/apps/{appId}。"
    />

    <Card>
      <CardContent class="space-y-4 pt-6">
        <div class="grid gap-4 sm:grid-cols-2">
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
        </div>
        <FormField v-if="selectedAppId" label="权限过期时间（可选）">
          <Input
            v-model="expireTime"
            type="datetime-local"
            class="max-w-xs"
          />
        </FormField>
      </CardContent>
    </Card>

    <template v-if="selectedAppId">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-lg font-semibold">{{ selectedApp?.appName }}</h2>
          <p class="text-sm text-muted-foreground font-mono">{{ selectedApp?.appCode }}</p>
        </div>
        <Button :disabled="saving || loading" @click="savePermissions">
          <Save class="mr-1 h-4 w-4" />
          {{ saving ? '保存中…' : '保存' }}
        </Button>
      </div>

      <p v-if="error" class="text-sm text-destructive">{{ error }}</p>
      <p v-if="loading" class="text-sm text-muted-foreground">加载中…</p>

      <div
        v-else
        class="max-h-[36rem] space-y-2 overflow-y-auto rounded-lg border p-4"
      >
        <label
          v-for="api in grantableList"
          :key="api.authorityId"
          class="flex cursor-pointer items-start gap-2 rounded px-2 py-1.5 hover:bg-muted/40"
        >
          <input
            type="checkbox"
            :checked="selectedAuthorityIds.includes(String(api.authorityId))"
            @change="toggleAuthority(String(api.authorityId), ($event.target as HTMLInputElement).checked)"
          />
          <span class="min-w-0 flex-1">
            <span class="font-mono text-sm">{{ api.authority }}</span>
            <Badge variant="outline" class="ml-2 text-xs">{{ api.authorityId }}</Badge>
          </span>
        </label>
        <p v-if="!grantableList.length" class="text-sm text-muted-foreground">
          暂无可授权的 API 目录项。
        </p>
      </div>
    </template>

    <p v-else class="text-sm text-muted-foreground">请先选择一个应用（客户端）。</p>
  </div>
</template>
