<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus, Pencil } from 'lucide-vue-next'
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
import { useFeedback } from '@/composables/useFeedback'
import { listIpLimits, createIpLimit, updateIpLimit, deleteIpLimit } from '@/api/gateway'
import type { GatewayIpLimit } from '@/api/types'
import { get, put, unwrap } from '@/api/request'

const keyword = ref('')
const policyTypeFilter = ref('')
const feedback = useFeedback()
interface AdminPolicy { mode: string; allowedNetworks: string[]; currentSource: string; passwordMaxAgeDays: number; revision: string; canEdit: boolean; defaultNetworks: string[] }
const adminPolicy = ref<AdminPolicy | null>(null)
const policyEditing = ref(false), policySaving = ref(false), policyMode = ref('monitor'), policyNetworks = ref(''), policyError = ref('')
function editPolicy() { if (!adminPolicy.value) return; policyMode.value = adminPolicy.value.mode; policyNetworks.value = adminPolicy.value.allowedNetworks.join('\n'); policyError.value = ''; policyEditing.value = true }
function addCurrentSource() { const ip = adminPolicy.value?.currentSource; if (ip) policyNetworks.value = [...new Set([...policyNetworks.value.split(/[\s,;，；]+/).filter(Boolean), ip + (ip.includes(':') ? '/128' : '/32')])].join('\n') }
async function savePolicy() {
  if (!adminPolicy.value) return
  policySaving.value = true; policyError.value = ''
  try {
    adminPolicy.value = unwrap(await put<AdminPolicy>('/oauth2/security/admin-policy', { mode: policyMode.value, allowedNetworks: policyNetworks.value.split(/[\s,;，；]+/).filter(Boolean), revision: adminPolicy.value.revision }))
    policyEditing.value = false
  } catch (error) { policyError.value = error instanceof Error ? error.message : '保存失败，请刷新核对' }
  finally { policySaving.value = false }
}
const adminPolicyError = ref('')
onMounted(async () => {
  try { adminPolicy.value = unwrap(await get<NonNullable<typeof adminPolicy.value>>('/oauth2/security/admin-policy')) }
  catch (error) { adminPolicyError.value = error instanceof Error ? error.message : '管理接入策略读取失败' }
})

const { items, total, page, loading, error, load, pageSize } = usePagedList<GatewayIpLimit>(
  (p, s) =>
    listIpLimits(p, s, {
      keyword: keyword.value || undefined,
      policyType: policyTypeFilter.value !== '' ? policyTypeFilter.value : undefined,
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
} = useCrudForm<GatewayIpLimit>(() => ({
  policyName: '',
  policyType: 1,
  ipAddress: '',
}))

async function handleSave() {
  if (!form.value.policyName?.trim() || !form.value.ipAddress?.trim()) {
    formError.value = '策略名称和 IP 地址不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const payload = {
      ...form.value,
      policyType: form.value.policyType != null ? Number(form.value.policyType) : 1,
    }
    if (editing.value && form.value.policyId) {
      await updateIpLimit(form.value.policyId, payload)
    } else {
      await createIpLimit(payload)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: GatewayIpLimit) {
  if (!row.policyId) return
  const confirmed = await feedback.confirm({
    title: '确认删除 IP 策略',
    message: `确认删除 IP 策略 ${row.policyName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteIpLimit(row.policyId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader title="IP 限制" description="管理员接入范围与接口访问策略">
      <template #actions>
        <Input
          v-model="keyword"
          placeholder="策略名"
          class="w-40"
          @keyup.enter="search"
        />
        <Select v-model="policyTypeFilter" class="w-28">
          <option value="">全部类型</option>
          <option value="1">白名单</option>
          <option value="0">黑名单</option>
        </Select>
        <Button variant="outline" @click="search">查询</Button>
        <Button @click="openCreate">
          <Plus class="mr-1 h-4 w-4" />
          新建
        </Button>
      </template>
    </PageHeader>
    <section class="mb-4 space-y-2 rounded-lg border p-4" aria-label="管理员接入策略">
      <div class="flex items-center justify-between gap-3"><h2 class="font-medium">管理员接入限制</h2><Badge v-if="adminPolicy" variant="secondary">{{ adminPolicy.mode === 'enforce' ? '已启用拦截' : '观察模式 尚未拦截' }}</Badge></div>
      <p v-if="adminPolicyError" class="text-sm text-destructive">{{ adminPolicyError }}</p>
      <template v-if="adminPolicy">
        <p class="text-sm text-muted-foreground">当前来源 <span class="font-mono">{{ adminPolicy.currentSource }}</span>；允许范围 {{ adminPolicy.allowedNetworks.join('、') || '尚未配置' }}。</p>
        <p class="text-sm text-muted-foreground">默认提供常用局域网范围，可在此增改 IP／网段。限制同时作用于登录和管理请求；升级保留现有启停设置。</p>
        <p v-if="adminPolicy.passwordMaxAgeDays" class="text-sm text-muted-foreground">密码定期更换周期 {{ adminPolicy.passwordMaxAgeDays }} 天，已有密码保持不变并从首次验证起计时。</p>
        <Button v-if="adminPolicy.canEdit" size="sm" variant="outline" @click="editPolicy">设置接入范围</Button>
      </template>
    </section>
    <CrudDialog v-model:open="policyEditing" title="管理员接入限制" :saving="policySaving" @save="savePolicy">
      <p v-if="policyError" role="alert" class="text-sm text-destructive">{{ policyError }}</p>
      <FormField label="限制模式"><Select v-model="policyMode"><option value="monitor">观察模式（记录但不拦截）</option><option value="enforce">启用限制（仅允许以下范围）</option></Select></FormField>
      <FormField label="允许的 IP／网段" required><textarea v-model="policyNetworks" rows="8" class="w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-sm" placeholder="每行一个 IP 或 CIDR 网段" /></FormField>
      <div class="flex flex-wrap gap-2"><Button size="sm" variant="outline" @click="policyNetworks = adminPolicy?.defaultNetworks.join('\n') || ''">使用局域网默认范围</Button><Button size="sm" variant="outline" @click="addCurrentSource">加入当前地址</Button></div>
      <p class="text-sm text-muted-foreground">启用时会检查当前管理地址是否获准，防止将自己锁定。保存后由配置中心同步到各服务。</p>
    </CrudDialog>
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">策略名</th>
            <th class="h-10 px-4 text-left font-medium">类型</th>
            <th class="h-10 px-4 text-left font-medium">IP</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.policyId" class="border-b">
            <td class="p-4">{{ row.policyId }}</td>
            <td class="p-4">{{ row.policyName }}</td>
            <td class="p-4">
              <Badge :variant="row.policyType === 1 ? 'default' : 'destructive'">
                {{ row.policyType === 1 ? '白名单' : '黑名单' }}
              </Badge>
            </td>
            <td class="p-4 font-mono text-sm">{{ row.ipAddress }}</td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" @click="openEdit(row)">
                <Pencil class="h-3.5 w-3.5" />
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
      :title="editing ? '编辑 IP 策略' : '新建 IP 策略'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="策略名称" required>
        <Input v-model="form.policyName" />
      </FormField>
      <FormField label="策略类型">
        <Select v-model="form.policyType">
          <option :value="1">白名单（允许）</option>
          <option :value="0">黑名单（拒绝）</option>
        </Select>
      </FormField>
      <FormField label="IP 地址" required>
        <Input
          v-model="form.ipAddress"
          placeholder="多个用分号分隔，最多 10 个"
          class="font-mono text-sm"
        />
      </FormField>
    </CrudDialog>
  </div>
</template>
