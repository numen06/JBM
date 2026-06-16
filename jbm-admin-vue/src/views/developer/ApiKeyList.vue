<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { KeyRound, Plus, Pencil, Shield, Copy, RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import {
  listApiKeys,
  createApiKey,
  updateApiKey,
  resetApiKeySecret,
  updateApiKeyStatus,
  deleteApiKey,
  getApiKeyAuthorities,
  grantApiKeyAuthorities,
} from '@/api/apikey'
import { listGrantableApis, listResources, type AuthorityResource } from '@/api/authority'
import { listApps } from '@/api/app'
import { getDeveloper } from '@/api/developer'
import type { BaseApiKey, BaseApp, OpenAuthority, SnowflakeId } from '@/api/types'
import { optionalSnowflakeIdParam } from '@/lib/snowflakeId'
import { useAuthStore } from '@/stores/auth'
import { useFeedback } from '@/composables/useFeedback'

const keyword = ref('')
const statusFilter = ref('')
const feedback = useFeedback()

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseApiKey>(
  (p, s) =>
    listApiKeys(p, s, {
      keyword: keyword.value || undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
    }),
)

function search() {
  load(1)
}

const apps = ref<BaseApp[]>([])
const auth = useAuthStore()
const developerStatus = ref<number | null>(null)
const developerStatusLoaded = ref(false)
const secretDialogOpen = ref(false)
const secretValue = ref('')
const secretKeyName = ref('')

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseApiKey>(() => ({
  keyName: '',
  clientName: '',
  keyDesc: '',
  bizAppId: undefined,
  status: 1,
}))

const authDialogOpen = ref(false)
const authSaving = ref(false)
const authError = ref('')
const authKeyId = ref<SnowflakeId>()
const authKeyName = ref('')
const grantableApis = ref<OpenAuthority[]>([])
const authorityResources = ref<AuthorityResource[]>([])
const selectedAuthorityIds = ref<string[]>([])
const authExpireTime = ref('')
const authorityKeyword = ref('')
const authorityService = ref('')
const authorityPage = ref(1)
const authorityPageSize = 50

function statusLabel(s?: number) {
  if (s === 0) return '禁用'
  if (s === 1) return '启用'
  return String(s ?? '-')
}

async function loadApps() {
  try {
    const data = await listApps(1, 200)
    apps.value = data.contents ?? []
  } catch {
    apps.value = []
  }
}

async function handleSave() {
  if (!form.value.keyName?.trim()) {
    formError.value = '名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  const payload = { ...form.value }
  if (!payload.bizAppId) {
    payload.bizAppId = undefined
  }
  try {
    if (editing.value && form.value.keyId) {
      await updateApiKey(form.value.keyId, payload)
    } else {
      const created = await createApiKey(payload)
      if (created.secretKey) {
        secretValue.value = created.secretKey
        secretKeyName.value = created.keyName ?? created.apiKey ?? ''
        secretDialogOpen.value = true
      }
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleResetSecret(row: BaseApiKey) {
  if (!row.keyId) return
  const confirmed = await feedback.confirm({
    title: '确认重置 Secret',
    message: `确认重置 ${row.keyName} 的 Secret？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  const secret = await resetApiKeySecret(row.keyId)
  secretValue.value = secret
  secretKeyName.value = row.keyName ?? row.apiKey ?? ''
  secretDialogOpen.value = true
}

async function handleToggleStatus(row: BaseApiKey) {
  if (!row.keyId) return
  const next = row.status === 1 ? 0 : 1
  await updateApiKeyStatus(row.keyId, next)
  load(page.value)
}

async function handleDelete(row: BaseApiKey) {
  if (!row.keyId) return
  const confirmed = await feedback.confirm({
    title: '确认删除 API Key',
    message: `确认删除 API Key「${row.keyName}」？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteApiKey(row.keyId)
  load(page.value)
}

async function openAuthority(row: BaseApiKey) {
  if (!row.keyId) return
  authKeyId.value = row.keyId
  authKeyName.value = row.keyName ?? ''
  authError.value = ''
  authorityKeyword.value = ''
  authorityService.value = ''
  authDialogOpen.value = true
  try {
    const [grantable, current, resources] = await Promise.all([
      listGrantableApis(),
      getApiKeyAuthorities(row.keyId),
      listResources(),
    ])
    grantableApis.value = grantable
    authorityResources.value = resources ?? []
    selectedAuthorityIds.value = (current ?? [])
      .map((a) => a.authorityId)
      .filter((id): id is string => !!id)
  } catch (e) {
    authError.value = e instanceof Error ? e.message : '加载权限失败'
  }
}

async function saveAuthority() {
  if (!authKeyId.value) return
  authSaving.value = true
  authError.value = ''
  try {
    await grantApiKeyAuthorities(authKeyId.value, {
      authorityIds: selectedAuthorityIds.value,
      authorityExpireTime: authExpireTime.value || undefined,
    })
    authDialogOpen.value = false
  } catch (e) {
    authError.value = e instanceof Error ? e.message : '授权失败'
  } finally {
    authSaving.value = false
  }
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

async function copyText(text: string) {
  await navigator.clipboard.writeText(text)
}

const filteredGrantable = computed(() =>
  grantableApis.value.filter((a) => {
    if (!a.authorityId) return false
    const meta = resourceByAuthority.value.get(a.authority ?? '')
    if (authorityService.value && meta?.serviceId !== authorityService.value) return false
    const kw = authorityKeyword.value.trim().toLowerCase()
    if (!kw) return true
    return (
      a.authority?.toLowerCase().includes(kw) ||
      String(a.authorityId).toLowerCase().includes(kw) ||
      meta?.path?.toLowerCase().includes(kw) ||
      meta?.serviceId?.toLowerCase().includes(kw)
    )
  }),
)

const resourceByAuthority = computed(() => {
  const map = new Map<string, AuthorityResource>()
  for (const r of authorityResources.value) {
    if (r.authority) map.set(r.authority, r)
  }
  return map
})

const authorityServices = computed(() => {
  const set = new Set<string>()
  for (const api of grantableApis.value) {
    const service = resourceByAuthority.value.get(api.authority ?? '')?.serviceId
    if (service) set.add(service)
  }
  return [...set].sort((a, b) => a.localeCompare(b))
})

const selectedVisibleCount = computed(() =>
  filteredGrantable.value.filter((api) => selectedAuthorityIds.value.includes(String(api.authorityId))).length,
)

const pagedGrantable = computed(() => {
  const start = (authorityPage.value - 1) * authorityPageSize
  return filteredGrantable.value.slice(start, start + authorityPageSize)
})

function authorityLabel(api: OpenAuthority) {
  const meta = resourceByAuthority.value.get(api.authority ?? '')
  return meta?.path || api.authority || String(api.authorityId)
}

function authorityMeta(api: OpenAuthority) {
  const meta = resourceByAuthority.value.get(api.authority ?? '')
  return meta?.serviceId || '未匹配服务'
}

function setVisibleAuthorities(checked: boolean) {
  const ids = filteredGrantable.value.map((api) => String(api.authorityId)).filter(Boolean)
  if (checked) {
    selectedAuthorityIds.value = [...new Set([...selectedAuthorityIds.value, ...ids])]
  } else {
    const remove = new Set(ids)
    selectedAuthorityIds.value = selectedAuthorityIds.value.filter((id) => !remove.has(id))
  }
}

watch([authorityKeyword, authorityService, authDialogOpen], () => {
  authorityPage.value = 1
})

watch(
  () => filteredGrantable.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / authorityPageSize))
    if (authorityPage.value > maxPage) authorityPage.value = maxPage
  },
)

const canCreateApiKey = computed(() => developerStatus.value === 1)
const developerStatusText = computed(() => {
  if (!developerStatusLoaded.value) return '正在检查开发者状态...'
  if (developerStatus.value === 1) return ''
  if (developerStatus.value === 0) return '开发者申请待审批，通过后即可创建 API Key。'
  return '当前账号还不是已审批开发者，请先在“开发者”页面提交申请。'
})

async function loadDeveloperStatus() {
  developerStatusLoaded.value = false
  try {
    const userId = auth.user?.userId
    if (!userId) {
      developerStatus.value = null
      return
    }
    const developer = await getDeveloper(userId)
    developerStatus.value = developer?.status ?? null
  } catch {
    developerStatus.value = null
  } finally {
    developerStatusLoaded.value = true
  }
}

onMounted(() => {
  loadApps()
  loadDeveloperStatus()
})
</script>

<template>
  <div>
    <PageHeader title="API Key 管理" description="Center /apikey — 第三方访问凭证与接口授权">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="名称/客户"
          class="w-40"
          @keyup.enter="search"
        />
        <Select v-model="statusFilter" class="w-28">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </Select>
        <Button variant="outline" @click="search">查询</Button>
        <Button :disabled="!canCreateApiKey" @click="openCreate">
          <Plus class="mr-1 h-4 w-4" />
          新建 API Key
        </Button>
      </template>
    </PageHeader>

    <p class="mb-4 text-sm text-muted-foreground">
      第三方调用时使用 <code class="rounded bg-muted px-1">X-App-Id</code> 传递
      <strong>apiKey</strong>；网关访问日志可通过 <strong>appKey</strong> 字段筛选。
    </p>

    <p v-if="developerStatusText" class="mb-4 rounded-md border bg-muted/40 px-3 py-2 text-sm text-muted-foreground">
      {{ developerStatusText }}
    </p>

    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">客户</th>
            <th class="h-10 px-4 text-left font-medium">AccessKey</th>
            <th class="h-10 px-4 text-left font-medium">业务应用</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.keyId" class="border-b">
            <td class="p-4">{{ row.keyId }}</td>
            <td class="p-4">{{ row.keyName }}</td>
            <td class="p-4">{{ row.clientName || '—' }}</td>
            <td class="p-4 font-mono text-xs">{{ row.apiKey }}</td>
            <td class="p-4">{{ row.bizAppId ?? '个人 Key' }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ statusLabel(row.status) }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" title="授权" @click="openAuthority(row)">
                <Shield class="h-3.5 w-3.5" />
              </Button>
              <Button variant="outline" size="sm" title="编辑" @click="openEdit(row)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button variant="outline" size="sm" title="重置 Secret" @click="handleResetSecret(row)">
                <RefreshCw class="h-3.5 w-3.5" />
              </Button>
              <Button variant="outline" size="sm" @click="handleToggleStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </Button>
              <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑 API Key' : '新建 API Key'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="名称" required>
        <Input v-model="form.keyName" />
      </FormField>
      <FormField label="第三方客户">
        <Input v-model="form.clientName" />
      </FormField>
      <FormField label="描述">
        <Input v-model="form.keyDesc" />
      </FormField>
      <FormField label="所属业务应用（可选）">
        <Select
          :model-value="form.bizAppId ?? ''"
          @update:model-value="(v) => { form.bizAppId = v ? optionalSnowflakeIdParam(v) : undefined }"
        >
          <option value="">个人 Key</option>
          <option v-for="app in apps" :key="app.appId" :value="String(app.appId)">
            {{ app.appName }} (#{{ app.appId }})
          </option>
        </Select>
      </FormField>
      <FormField v-if="editing" label="状态">
        <Select v-model="form.status">
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </Select>
      </FormField>
    </CrudDialog>

    <CrudDialog
      v-model:open="secretDialogOpen"
      title="请妥善保存 Secret"
      :saving="false"
      @save="secretDialogOpen = false"
    >
      <p class="mb-2 text-sm text-muted-foreground">
        {{ secretKeyName }} 的 Secret 仅显示一次：
      </p>
      <div class="flex items-center gap-2 rounded border bg-muted/50 p-3 font-mono text-sm break-all">
        <KeyRound class="h-4 w-4 shrink-0" />
        <span class="flex-1">{{ secretValue }}</span>
        <Button variant="ghost" size="sm" @click="copyText(secretValue)">
          <Copy class="h-4 w-4" />
        </Button>
      </div>
    </CrudDialog>

    <CrudDialog
      v-model:open="authDialogOpen"
      :title="`接口授权 — ${authKeyName}`"
      :saving="authSaving"
      @save="saveAuthority"
    >
      <p v-if="authError" class="text-sm text-destructive">{{ authError }}</p>
      <FormField label="授权过期时间（可选）">
        <Input v-model="authExpireTime" type="datetime-local" />
      </FormField>
      <p class="mb-2 text-sm text-muted-foreground">仅可选择您拥有的 API 权限：</p>
      <div class="mb-3 flex flex-wrap items-center gap-2 text-sm">
        <Badge variant="secondary">已选 {{ selectedAuthorityIds.length }}</Badge>
        <Badge variant="outline">当前筛选 {{ filteredGrantable.length }}</Badge>
      </div>
      <div class="mb-3 flex flex-wrap items-center gap-2">
        <Select v-model="authorityService" class="w-56">
          <option value="">全部服务</option>
          <option v-for="service in authorityServices" :key="service" :value="service">
            {{ service }}
          </option>
        </Select>
        <Input
          v-model="authorityKeyword"
          placeholder="搜索 API 路径 / 服务 / 标识"
          class="w-64"
        />
        <Button variant="outline" size="sm" :disabled="!filteredGrantable.length" @click="setVisibleAuthorities(true)">
          全选当前筛选
        </Button>
        <Button variant="ghost" size="sm" :disabled="selectedVisibleCount === 0" @click="setVisibleAuthorities(false)">
          取消当前筛选
        </Button>
      </div>
      <div class="max-h-80 overflow-y-auto rounded border p-2 space-y-1">
        <label
          v-for="api in pagedGrantable"
          :key="api.authorityId"
          class="flex items-start gap-2 text-sm cursor-pointer hover:bg-muted/50 p-1 rounded"
        >
          <input
            type="checkbox"
            class="mt-1"
            :checked="selectedAuthorityIds.includes(api.authorityId!)"
            @change="toggleAuthority(api.authorityId!, ($event.target as HTMLInputElement).checked)"
          />
          <span class="min-w-0 flex-1">
            <span class="block break-all font-mono text-xs">{{ authorityLabel(api) }}</span>
            <span class="mt-1 inline-flex flex-wrap gap-1">
              <Badge variant="outline">{{ authorityMeta(api) }}</Badge>
              <Badge variant="outline">{{ api.authorityId }}</Badge>
            </span>
          </span>
        </label>
        <p v-if="!filteredGrantable.length" class="text-sm text-muted-foreground p-2">暂无可授权 API</p>
      </div>
      <PaginationBar
        :page="authorityPage"
        :total="filteredGrantable.length"
        :page-size="authorityPageSize"
        @change="authorityPage = $event"
      />
    </CrudDialog>
  </div>
</template>
