<script setup lang="ts">
import { Copy, Eye, Image as ImageIcon, KeyRound, ListTree, Plus, Pencil, RefreshCw, Upload } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import Dialog from '@/components/ui/Dialog.vue'
import FormField from '@/components/FormField.vue'
import OrgTreeSelect from '@/components/OrgTreeSelect.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { computed, onMounted, ref } from 'vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { useOrgTree } from '@/composables/useOrgTree'
import { useFeedback } from '@/composables/useFeedback'
import {
  listApps,
  deleteApp,
  createApp,
  updateApp,
  resetAppSecret,
  getAppSecret,
  getAppBranding,
  updateAppBranding,
  type AppCredentials,
} from '@/api/app'
import { docInlineUrl, uploadDoc } from '@/api/doc'
import { JBM_TEMPLATE_APP_ID, syncMenusFromJbm } from '@/api/menu'
import type { BaseApp } from '@/api/types'
import { APP_TYPE_OPTIONS, appTypeLabel } from '@/constants/appTypes'
import { optionalSnowflakeIdParam, toSnowflakeIdString } from '@/lib/snowflakeId'
import type { SnowflakeId } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'

const { orgLabel, loadOrgs } = useOrgTree()
const feedback = useFeedback()
const auth = useAuthStore()
const { isSuperAdmin } = usePermission()
const canSyncPlatformMenus = computed(() =>
  (auth.user?.roles ?? []).some((role) =>
    ['super_admin', 'platform_operator'].includes(String(role.roleCode || '')),
  ),
)

onMounted(loadOrgs)

const keyword = ref('')
const statusFilter = ref('')
const appTypeFilter = ref('')
const orgIdFilter = ref<number | string | null>(null)
const secretDialogOpen = ref(false)
const secretViewMode = ref<'view' | 'reveal'>('view')
const secretAppId = ref<SnowflakeId>()
const secretIsPersist = ref(false)
const secretAppName = ref('')
const secretClientId = ref('')
const secretValue = ref('')
const syncingAppId = ref<string>('')
const brandingDialogOpen = ref(false)
const brandingLoading = ref(false)
const brandingSaving = ref(false)
const brandingUploading = ref(false)
const brandingLogoUploading = ref(false)
const brandingApp = ref<BaseApp | null>(null)
const brandingTitle = ref('')
const brandingBackground = ref('')
const brandingLogo = ref('')
const brandingError = ref('')
const brandingPreview = computed(() => {
  const value = brandingBackground.value.trim()
  return !value || /^(?:https?:|data:|\/)/i.test(value) ? value : docInlineUrl(value)
})
const brandingLogoPreview = computed(() => {
  const value = brandingLogo.value.trim()
  return !value || /^(?:https?:|data:|\/)/i.test(value) ? value : docInlineUrl(value)
})

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseApp>(
  (p, s) =>
    listApps(p, s, {
      keyword: keyword.value || undefined,
      orgId: orgIdFilter.value !== '' && orgIdFilter.value != null ? orgIdFilter.value : undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
      appType: appTypeFilter.value || undefined,
    }),
)

function search() {
  load(1)
}

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseApp>(() => ({
  appName: '',
  code: '',
  appType: 'pc',
  orgId: undefined,
  redirectUris: '',
  publicClient: true,
  registrationEnabled: false,
  registrationDefaultRoleCode: '',
  status: 1,
}))

function appCodeOf(row: BaseApp) {
  return row.code ?? row.appCode ?? ''
}

function clientIdOf(row: BaseApp) {
  return row.apiKey ?? row.clientId ?? ''
}

function isPersistApp(row: BaseApp) {
  return row.isPersist === 1
}

function openEditApp(row: BaseApp) {
  const oauth = row.extendData?.oauth
  const registration = row.extendData?.registration
  openEdit({
    appId: row.appId,
    appName: row.appName,
    code: appCodeOf(row),
    appType: row.appType || 'pc',
    apiKey: clientIdOf(row),
    orgId: row.orgId,
    status: row.status ?? 1,
    isPersist: row.isPersist,
    redirectUris: oauth?.redirectUris?.join(', ') || '',
    publicClient: oauth?.publicClient ?? true,
    registrationEnabled: registration?.enabled ?? false,
    registrationDefaultRoleCode: registration?.defaultRoleCode || '',
  })
}

function buildSavePayload() {
  return {
    appName: form.value.appName?.trim(),
    code: form.value.code?.trim(),
    appType: form.value.appType || 'pc',
    orgId: optionalSnowflakeIdParam(form.value.orgId!),
    status: Number(form.value.status ?? 1),
    redirectUris: form.value.redirectUris?.trim(),
    publicClient: form.value.publicClient !== false,
    registrationEnabled: form.value.registrationEnabled === true,
    registrationDefaultRoleCode: form.value.registrationDefaultRoleCode?.trim(),
  }
}

async function handleSave() {
  if (!form.value.appName?.trim() || !form.value.code?.trim()) {
    formError.value = '应用名称和编码不能为空'
    return
  }
  if (!form.value.orgId) {
    formError.value = '请选择所属组织'
    return
  }
  if (form.value.registrationEnabled && !form.value.registrationDefaultRoleCode?.trim()) {
    formError.value = '开放用户注册时必须配置普通租户默认角色编码'
    return
  }
  saving.value = true
  formError.value = ''
  const payload = buildSavePayload()
  try {
    if (editing.value && form.value.appId) {
      await updateApp(form.value.appId, payload)
      feedback.toast.success('应用已更新')
    } else {
      const created = await createApp(payload)
      showCredentials(created, form.value.appName)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseApp) {
  if (!row.appId || isPersistApp(row)) return
  const confirmed = await feedback.confirm({
    title: '确认删除应用',
    message: `确认删除应用 ${row.appName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  try {
    await deleteApp(row.appId)
    feedback.toast.success('应用已删除')
    load(page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '删除失败')
  }
}

function openSecretDialog(row: BaseApp, mode: 'view' | 'reveal', secret?: string) {
  secretViewMode.value = mode
  secretAppId.value = row.appId
  secretIsPersist.value = isPersistApp(row)
  secretAppName.value = row.appName || `应用 #${row.appId ?? ''}`
  secretClientId.value = clientIdOf(row)
  secretValue.value = secret ?? ''
  secretDialogOpen.value = true
}

function showCredentials(credentials: AppCredentials, appName?: string) {
  if (!credentials?.clientSecret) {
    feedback.toast.warning('应用已创建，但未返回 Client Secret，请在列表中重置密钥。')
    return
  }
  secretViewMode.value = 'reveal'
  secretAppId.value = credentials.appId
  secretIsPersist.value = false
  secretAppName.value = appName || `应用 #${credentials.appId ?? ''}`
  secretClientId.value = credentials.clientId || ''
  secretValue.value = credentials.clientSecret
  secretDialogOpen.value = true
}

function handleViewSecret(row: BaseApp) {
  if (!row.appId) return
  openSecretDialog(row, 'view')
  void loadSecret(row)
}

async function loadSecret(row: BaseApp) {
  if (!row.appId) return
  try {
    const secret = await getAppSecret(row.appId)
    secretValue.value = secret
    secretViewMode.value = 'reveal'
  } catch (e) {
    secretValue.value = ''
    secretViewMode.value = 'view'
    feedback.toast.warning(e instanceof Error ? e.message : '无法查看密钥，请尝试重置')
  }
}

async function handleResetSecret(row: BaseApp) {
  if (!row.appId || isPersistApp(row)) return
  const confirmed = await feedback.confirm({
    title: '重置 Client Secret',
    message: `确认重置 ${row.appName} 的 Client Secret？旧密钥会立即失效。`,
    variant: 'destructive',
  })
  if (!confirmed) return
  try {
    const secret = await resetAppSecret(row.appId)
    openSecretDialog(row, 'reveal', secret)
    feedback.toast.success('密钥已重置')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '重置失败')
  }
}

async function handleResetSecretFromDialog() {
  if (!secretAppId.value || secretIsPersist.value) return
  const confirmed = await feedback.confirm({
    title: '重置 Client Secret',
    message: `确认重置 ${secretAppName.value} 的 Client Secret？旧密钥会立即失效。`,
    variant: 'destructive',
  })
  if (!confirmed) return
  try {
    const secret = await resetAppSecret(secretAppId.value)
    secretViewMode.value = 'reveal'
    secretValue.value = secret
    feedback.toast.success('密钥已重置')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '重置失败')
  }
}

function isJbmTemplateApp(row: BaseApp) {
  if (row.appId == null) return false
  return toSnowflakeIdString(row.appId) === String(JBM_TEMPLATE_APP_ID)
}

async function handleSyncMenus(row: BaseApp) {
  if (!row.appId || isJbmTemplateApp(row)) return
  const confirmed = await feedback.confirm({
    title: '从 JBM 同步菜单',
    message: `将把 JBM 管理台菜单同步到「${row.appName}」，已有同编码菜单将被更新（保留菜单跳过）。`,
    confirmText: '同步',
  })
  if (!confirmed) return
  const appKey = toSnowflakeIdString(row.appId)
  syncingAppId.value = appKey
  try {
    const message = await syncMenusFromJbm(row.appId)
    feedback.toast.success(message || '菜单同步完成', '同步成功')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '同步失败', '同步失败')
  } finally {
    syncingAppId.value = ''
  }
}

async function openBranding(row: BaseApp) {
  if (!isSuperAdmin.value || !row.appId) return
  brandingApp.value = row
  brandingTitle.value = row.appName || ''
  brandingBackground.value = ''
  brandingLogo.value = ''
  brandingError.value = ''
  brandingDialogOpen.value = true
  brandingLoading.value = true
  try {
    const config = await getAppBranding(row.appId)
    brandingTitle.value = config.configContent?.title || row.appName || ''
    brandingBackground.value = config.configContent?.sysBg || ''
    brandingLogo.value = config.configContent?.sysLogo || ''
  } catch (e) {
    brandingError.value = e instanceof Error ? e.message : '读取品牌配置失败'
  } finally {
    brandingLoading.value = false
  }
}

async function handleBrandingLogoUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    brandingError.value = 'Logo 只能上传图片'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    brandingError.value = 'Logo 不能超过 2MB'
    return
  }
  brandingLogoUploading.value = true
  brandingError.value = ''
  try {
    brandingLogo.value = await uploadDoc(file, 'app-branding')
  } catch (e) {
    brandingError.value = e instanceof Error ? e.message : '上传 Logo 失败'
  } finally {
    brandingLogoUploading.value = false
  }
}

async function handleBrandingUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    brandingError.value = '登录背景只能上传图片'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    brandingError.value = '登录背景不能超过 10MB'
    return
  }
  brandingUploading.value = true
  brandingError.value = ''
  try {
    brandingBackground.value = await uploadDoc(file, 'app-branding')
  } catch (e) {
    brandingError.value = e instanceof Error ? e.message : '上传背景失败'
  } finally {
    brandingUploading.value = false
  }
}

async function saveBranding() {
  if (!brandingApp.value?.appId || !brandingTitle.value.trim()) {
    brandingError.value = '登录标题不能为空'
    return
  }
  brandingSaving.value = true
  brandingError.value = ''
  try {
    await updateAppBranding(
      brandingApp.value.appId,
      brandingTitle.value.trim(),
      brandingBackground.value.trim(),
      brandingLogo.value.trim(),
    )
    brandingDialogOpen.value = false
    feedback.toast.success('登录品牌已更新')
    load(page.value)
  } catch (e) {
    brandingError.value = e instanceof Error ? e.message : '保存品牌配置失败'
  } finally {
    brandingSaving.value = false
  }
}

async function copyText(text: string) {
  await navigator.clipboard.writeText(text)
  feedback.toast.success('已复制')
}
</script>

<template>
  <div>
    <PageHeader title="应用管理" description="Center /app — OAuth 客户端应用">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="名称/编码"
          class="w-40"
          @keyup.enter="search"
        />
        <OrgTreeSelect
          v-model="orgIdFilter"
          placeholder="全部组织"
          class="w-44"
        />
        <Select v-model="statusFilter" class="w-28">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </Select>
        <Select v-model="appTypeFilter" class="w-36">
          <option value="">全部类型</option>
          <option v-for="item in APP_TYPE_OPTIONS" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </Select>
        <Button variant="outline" @click="search">查询</Button>
        <Button @click="openCreate">
          <Plus class="mr-1 h-4 w-4" />
          新建
        </Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">编码</th>
            <th class="h-10 px-4 text-left font-medium">类型</th>
            <th class="h-10 px-4 text-left font-medium">Client ID</th>
            <th class="h-10 px-4 text-left font-medium">所属组织</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.appId" class="border-b">
            <td class="p-4">{{ row.appId }}</td>
            <td class="p-4">{{ row.appName }}</td>
            <td class="p-4">{{ appCodeOf(row) }}</td>
            <td class="p-4">{{ appTypeLabel(row.appType) }}</td>
            <td class="p-4 font-mono text-xs">{{ clientIdOf(row) }}</td>
            <td class="p-4">{{ orgLabel(row.orgId) }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" title="编辑" @click="openEditApp(row)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                title="查看密钥"
                @click="handleViewSecret(row)"
              >
                <Eye class="h-3.5 w-3.5" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                title="重置 Client Secret"
                :disabled="isPersistApp(row)"
                @click="handleResetSecret(row)"
              >
                <RefreshCw class="h-3.5 w-3.5" />
              </Button>
              <Button
                v-if="canSyncPlatformMenus"
                variant="outline"
                size="sm"
                title="从 JBM 同步菜单"
                :disabled="isJbmTemplateApp(row) || (row.appId != null && syncingAppId === toSnowflakeIdString(row.appId))"
                @click="handleSyncMenus(row)"
              >
                <ListTree class="h-3.5 w-3.5" />
              </Button>
              <Button
                v-if="isSuperAdmin"
                variant="outline"
                size="sm"
                title="登录品牌"
                @click="openBranding(row)"
              >
                <ImageIcon class="h-3.5 w-3.5" />
              </Button>
              <Button
                variant="destructive"
                size="sm"
                title="删除"
                :disabled="isPersistApp(row)"
                @click="handleDelete(row)"
              >
                删除
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑应用' : '新建应用'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="应用名称" required>
        <Input v-model="form.appName" />
      </FormField>
      <FormField label="应用编码" required>
        <Input v-model="form.code" :disabled="editing && form.isPersist === 1" />
      </FormField>
      <FormField label="应用类型" required>
        <Select v-model="form.appType">
          <option v-for="item in APP_TYPE_OPTIONS" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </Select>
      </FormField>
      <FormField label="所属组织" required>
        <OrgTreeSelect v-model="form.orgId" placeholder="请选择组织" required />
      </FormField>
      <FormField v-if="editing" label="Client ID">
        <Input :model-value="form.apiKey ?? ''" class="font-mono text-sm" readonly disabled />
      </FormField>
      <p v-else class="text-xs text-muted-foreground">
        Client ID 与 Client Secret 将在创建成功后自动生成并展示。
      </p>
      <FormField label="OAuth 回调地址" required>
        <Input
          v-model="form.redirectUris"
          placeholder="https://iot.example.com/login/callback，多个用逗号分隔"
        />
      </FormField>
      <label class="flex items-center gap-2 text-sm">
        <input v-model="form.publicClient" type="checkbox" />
        浏览器公开客户端（PKCE）
      </label>
      <label class="flex items-center gap-2 text-sm">
        <input v-model="form.registrationEnabled" type="checkbox" />
        开放用户自助注册
      </label>
      <FormField v-if="form.registrationEnabled" label="普通租户默认角色编码" required>
        <Input
          v-model="form.registrationDefaultRoleCode"
          placeholder="例如 iot_admin"
        />
        <p class="mt-1 text-xs text-muted-foreground">
          注册人不能自行选择角色；系统只授予此应用已配置的默认角色。
        </p>
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
    </CrudDialog>

    <CrudDialog
      v-model:open="brandingDialogOpen"
      :title="`登录品牌 · ${brandingApp?.appName || ''}`"
      :saving="brandingSaving"
      wide
      @save="saveBranding"
    >
      <p v-if="brandingError" class="text-sm text-destructive">{{ brandingError }}</p>
      <p v-if="brandingLoading" class="text-sm text-muted-foreground">正在读取品牌配置…</p>
      <template v-else>
        <FormField label="登录标题" required>
          <Input v-model="brandingTitle" placeholder="请输入登录页和浏览器抬头" />
        </FormField>
        <FormField label="Logo">
          <div class="flex flex-wrap items-center gap-2">
            <label class="inline-flex cursor-pointer items-center rounded-md border px-3 py-2 text-sm hover:bg-muted">
              <Upload class="mr-2 h-4 w-4" />
              {{ brandingLogoUploading ? '上传中…' : '上传 Logo' }}
              <input
                class="sr-only"
                type="file"
                accept="image/png,image/jpeg,image/webp,image/svg+xml"
                :disabled="brandingLogoUploading"
                @change="handleBrandingLogoUpload"
              />
            </label>
            <Button v-if="brandingLogo" type="button" variant="outline" size="sm" @click="brandingLogo = ''">
              使用默认图标
            </Button>
          </div>
          <Input v-model="brandingLogo" class="mt-2 font-mono text-xs" placeholder="不上传则使用当前默认图标" />
          <img v-if="brandingLogoPreview" :src="brandingLogoPreview" alt="Logo 预览" class="mt-3 h-16 w-16 rounded-md border object-contain p-1" />
        </FormField>
        <FormField label="登录背景">
          <div class="flex flex-wrap items-center gap-2">
            <label class="inline-flex cursor-pointer items-center rounded-md border px-3 py-2 text-sm hover:bg-muted">
              <Upload class="mr-2 h-4 w-4" />
              {{ brandingUploading ? '上传中…' : '上传图片' }}
              <input
                class="sr-only"
                type="file"
                accept="image/png,image/jpeg,image/webp"
                :disabled="brandingUploading"
                @change="handleBrandingUpload"
              />
            </label>
            <Button
              v-if="brandingBackground"
              type="button"
              variant="outline"
              size="sm"
              @click="brandingBackground = ''"
            >
              清除背景
            </Button>
          </div>
          <Input v-model="brandingBackground" class="mt-2 font-mono text-xs" placeholder="上传后自动填写，也可输入图片地址" />
          <img
            v-if="brandingPreview"
            :src="brandingPreview"
            alt="登录背景预览"
            class="mt-3 max-h-64 w-full rounded-md border object-cover"
          />
        </FormField>
        <p class="text-xs text-muted-foreground">仅超级管理员可修改；保存后应用登录页立即读取新配置。</p>
      </template>
    </CrudDialog>

    <Dialog
      v-model:open="secretDialogOpen"
      :title="secretViewMode === 'reveal' && secretValue ? '请妥善保存 Client Secret' : '查看密钥'"
    >
      <p class="text-sm text-muted-foreground">
        <template v-if="secretViewMode === 'reveal' && secretValue">
          {{ secretAppName }} 的 OAuth 客户端凭证：
        </template>
        <template v-else-if="secretViewMode === 'view'">
          正在加载 {{ secretAppName }} 的密钥…
        </template>
        <template v-else>
          {{ secretAppName }} 的 Client Secret 仅显示一次，请妥善保存：
        </template>
      </p>
      <div class="space-y-3 rounded border bg-muted/50 p-3">
        <div class="grid gap-1">
          <span class="text-xs text-muted-foreground">Client ID</span>
          <div class="flex items-center gap-2 font-mono text-sm break-all">
            <KeyRound class="h-4 w-4 shrink-0" />
            <span class="flex-1">{{ secretClientId }}</span>
            <Button variant="ghost" size="sm" title="复制 Client ID" @click="copyText(secretClientId)">
              <Copy class="h-4 w-4" />
            </Button>
          </div>
        </div>
        <div class="grid gap-1">
          <span class="text-xs text-muted-foreground">Client Secret</span>
          <div v-if="secretValue" class="flex items-center gap-2 font-mono text-sm break-all">
            <KeyRound class="h-4 w-4 shrink-0" />
            <span class="flex-1">{{ secretValue }}</span>
            <Button variant="ghost" size="sm" title="复制 Client Secret" @click="copyText(secretValue)">
              <Copy class="h-4 w-4" />
            </Button>
          </div>
          <div v-else class="space-y-1">
            <div class="flex items-center gap-2 font-mono text-sm text-muted-foreground">
              <KeyRound class="h-4 w-4 shrink-0" />
              <span class="flex-1 tracking-widest">••••••••</span>
            </div>
            <p class="text-xs text-muted-foreground">
              密钥为旧格式无法查看，请重置密钥后再次查看。
            </p>
          </div>
        </div>
      </div>
      <p v-if="secretViewMode === 'view' && secretIsPersist" class="mt-3 text-sm text-muted-foreground">
        系统保留应用，不允许重置密钥。
      </p>
      <div class="mt-6 flex justify-end gap-2 border-t pt-4">
        <Button
          v-if="secretViewMode === 'view' && !secretIsPersist && !secretValue"
          type="button"
          variant="destructive"
          @click="handleResetSecretFromDialog"
        >
          重置密钥
        </Button>
        <Button variant="outline" type="button" @click="secretDialogOpen = false">
          {{ secretViewMode === 'view' && !secretValue ? '取消' : '关闭' }}
        </Button>
      </div>
    </Dialog>
  </div>
</template>
