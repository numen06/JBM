<script setup lang="ts">
import { Copy, Eye, KeyRound, Plus, Pencil, RefreshCw } from 'lucide-vue-next'
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
import { onMounted, ref } from 'vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { useOrgTree } from '@/composables/useOrgTree'
import { useFeedback } from '@/composables/useFeedback'
import { listApps, deleteApp, createApp, updateApp, resetAppSecret, getAppSecret, type AppCredentials } from '@/api/app'
import type { BaseApp } from '@/api/types'

const { orgLabel, loadOrgs } = useOrgTree()
const feedback = useFeedback()

onMounted(loadOrgs)

const keyword = ref('')
const statusFilter = ref('')
const orgIdFilter = ref<number | string | null>(null)
const secretDialogOpen = ref(false)
const secretViewMode = ref<'view' | 'reveal'>('view')
const secretAppId = ref<number>()
const secretIsPersist = ref(false)
const secretAppName = ref('')
const secretClientId = ref('')
const secretValue = ref('')

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseApp>(
  (p, s) =>
    listApps(p, s, {
      keyword: keyword.value || undefined,
      orgId: orgIdFilter.value !== '' && orgIdFilter.value != null ? orgIdFilter.value : undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
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
  appCode: '',
  clientId: '',
  orgId: undefined,
  status: 1,
}))

function isPersistApp(row: BaseApp) {
  return row.isPersist === 1
}

async function handleSave() {
  if (!form.value.appName?.trim() || !form.value.appCode?.trim()) {
    formError.value = '应用名称和编码不能为空'
    return
  }
  if (!form.value.orgId) {
    formError.value = '请选择所属组织'
    return
  }
  saving.value = true
  formError.value = ''
  const payload = { ...form.value, orgId: Number(form.value.orgId) }
  try {
    if (editing.value && form.value.appId) {
      await updateApp(form.value.appId, payload)
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
  if (!row.appId) return
  const confirmed = await feedback.confirm({
    title: '确认删除应用',
    message: `确认删除应用 ${row.appName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteApp(row.appId)
  load(page.value)
}

function clientIdOf(row: BaseApp) {
  return row.apiKey || row.clientId || ''
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
  const secret = await resetAppSecret(row.appId)
  openSecretDialog(row, 'reveal', secret)
}

async function handleResetSecretFromDialog() {
  if (!secretAppId.value || secretIsPersist.value) return
  const confirmed = await feedback.confirm({
    title: '重置 Client Secret',
    message: `确认重置 ${secretAppName.value} 的 Client Secret？旧密钥会立即失效。`,
    variant: 'destructive',
  })
  if (!confirmed) return
  const secret = await resetAppSecret(secretAppId.value)
  secretViewMode.value = 'reveal'
  secretValue.value = secret
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
            <td class="p-4">{{ row.appCode }}</td>
            <td class="p-4 font-mono text-xs">{{ clientIdOf(row) }}</td>
            <td class="p-4">{{ orgLabel(row.orgId) }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" @click="openEdit(row)">
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
              <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
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
        <Input v-model="form.appCode" />
      </FormField>
      <FormField label="所属组织" required>
        <OrgTreeSelect v-model="form.orgId" placeholder="请选择组织" required />
      </FormField>
      <FormField label="Client ID">
        <Input v-model="form.clientId" class="font-mono text-sm" />
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
    </CrudDialog>

    <Dialog
      v-model:open="secretDialogOpen"
      :title="secretViewMode === 'reveal' ? '请妥善保存 Client Secret' : '查看密钥'"
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
        <Button variant="outline" type="button" @click="secretDialogOpen = false">
          {{ secretViewMode === 'reveal' ? '关闭' : '取消' }}
        </Button>
        <Button
          v-if="secretViewMode === 'view' && !secretIsPersist"
          type="button"
          variant="destructive"
          @click="handleResetSecretFromDialog"
        >
          重置密钥
        </Button>
        <Button
          v-if="secretViewMode === 'reveal' && secretValue"
          type="button"
          @click="secretDialogOpen = false"
        >
          关闭
        </Button>
      </div>
    </Dialog>
  </div>
</template>
