<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
import { listGrantableApis } from '@/api/authority'
import { listApps } from '@/api/app'
import type { BaseApiKey, BaseApp, OpenAuthority } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } =
  usePagedList<BaseApiKey>(listApiKeys)

const apps = ref<BaseApp[]>([])
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
const authKeyId = ref<number>()
const authKeyName = ref('')
const grantableApis = ref<OpenAuthority[]>([])
const selectedAuthorityIds = ref<string[]>([])
const authExpireTime = ref('')

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
  if (!row.keyId || !confirm(`确认重置 ${row.keyName} 的 Secret？`)) return
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
  if (!row.keyId || !confirm(`确认删除 API Key「${row.keyName}」？`)) return
  await deleteApiKey(row.keyId)
  load(page.value)
}

async function openAuthority(row: BaseApiKey) {
  if (!row.keyId) return
  authKeyId.value = row.keyId
  authKeyName.value = row.keyName ?? ''
  authError.value = ''
  authDialogOpen.value = true
  try {
    const [grantable, current] = await Promise.all([
      listGrantableApis(),
      getApiKeyAuthorities(row.keyId),
    ])
    grantableApis.value = grantable
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
  grantableApis.value.filter((a) => a.authorityId),
)

onMounted(loadApps)
</script>

<template>
  <div>
    <PageHeader title="API Key 管理" description="Center /apikey — 第三方访问凭证与接口授权">
      <template #actions>
        <Button @click="openCreate">
          <Plus class="mr-1 h-4 w-4" />
          新建 API Key
        </Button>
      </template>
    </PageHeader>

    <p class="mb-4 text-sm text-muted-foreground">
      第三方调用时使用 <code class="rounded bg-muted px-1">X-App-Id</code> 传递
      <strong>apiKey</strong>；网关访问日志可通过 <strong>appKey</strong> 字段筛选。
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
          @update:model-value="(v) => { form.bizAppId = v ? Number(v) : undefined }"
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
      <div class="max-h-64 overflow-y-auto rounded border p-2 space-y-1">
        <label
          v-for="api in filteredGrantable"
          :key="api.authorityId"
          class="flex items-start gap-2 text-sm cursor-pointer hover:bg-muted/50 p-1 rounded"
        >
          <input
            type="checkbox"
            class="mt-1"
            :checked="selectedAuthorityIds.includes(api.authorityId!)"
            @change="toggleAuthority(api.authorityId!, ($event.target as HTMLInputElement).checked)"
          />
          <span class="font-mono text-xs">{{ api.authority }}</span>
        </label>
        <p v-if="!filteredGrantable.length" class="text-sm text-muted-foreground p-2">暂无可授权 API</p>
      </div>
    </CrudDialog>
  </div>
</template>
