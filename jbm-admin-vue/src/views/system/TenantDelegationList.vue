<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { useAuthStore } from '@/stores/auth'
import { useFeedback } from '@/composables/useFeedback'
import {
  createTenantDelegation,
  listTenantDelegations,
  updateTenantDelegation,
  type TenantDelegation,
} from '@/api/tenantDelegation'

const auth = useAuthStore()
const feedback = useFeedback()
const rows = ref<TenantDelegation[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const error = ref('')
const dialogOpen = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive({
  operatorAccount: '',
  read: true,
  operate: true,
  projectIds: '',
  validTo: '',
  purpose: '',
})

function jsonArray(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(String)
  if (typeof value !== 'string' || !value.trim()) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return []
  }
}

function projectIds(value: TenantDelegation['dataScope']) {
  if (!value) return []
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return Array.isArray(parsed?.projectIds) ? parsed.projectIds.map(String) : []
  } catch {
    return []
  }
}

async function load(target = page.value) {
  loading.value = true
  error.value = ''
  try {
    const result = await listTenantDelegations(target, pageSize)
    rows.value = result.contents ?? []
    total.value = result.total ?? 0
    page.value = target
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载委托授权失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    operatorAccount: '',
    read: true,
    operate: true,
    projectIds: '',
    validTo: '',
    purpose: '',
  })
  formError.value = ''
  dialogOpen.value = true
}

async function save() {
  const operatorAccount = form.operatorAccount.trim()
  const permissions = [
    form.read ? 'iot.platform.read' : '',
    form.operate ? 'iot.platform.operate' : '',
  ].filter(Boolean)
  if (!operatorAccount) {
    formError.value = '请输入运营方账号'
    return
  }
  if (!permissions.length) {
    formError.value = '至少选择一项权限'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await createTenantDelegation({
      operatorAccount,
      permissionCodes: permissions,
      resourceTypes: ['*'],
      dataScope: {
        projectIds: form.projectIds.split(',').map(value => value.trim()).filter(Boolean),
      },
      validTo: form.validTo || undefined,
      purpose: form.purpose.trim() || undefined,
    })
    dialogOpen.value = false
    await load(1)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '创建委托授权失败'
  } finally {
    saving.value = false
  }
}

async function revoke(row: TenantDelegation) {
  if (row.id == null) return
  const confirmed = await feedback.confirm({
    title: '撤销委托授权',
    message: `确认撤销对运营方 ${row.operatorAccount || row.operatorTenantId} 的授权？撤销后立即失去访问。`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await updateTenantDelegation(row.id, { status: 0 })
  await load()
}

onMounted(() => load(1))
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="委托运营" description="输入运营方账号，按权限和项目范围直接委托；授权符合协议并可随时撤销。">
      <template #actions>
        <Button variant="outline" :disabled="loading" @click="load()"><RefreshCw class="mr-1 size-4" />刷新</Button>
        <Button @click="openCreate"><Plus class="mr-1 size-4" />新建委托</Button>
      </template>
    </PageHeader>

    <div class="rounded-lg border bg-card p-4 text-sm">
      <span class="text-muted-foreground">当前数据所有方租户：</span>
      <strong class="font-mono">{{ auth.tenantId }}</strong>
    </div>

    <DataTableShell :loading="loading" :error="error" :empty="!rows.length">
      <Table mobile-title="运营方租户" :mobile-columns="['权限', '项目范围', '状态']">
        <thead><tr class="border-b bg-muted/50">
          <th class="h-10 px-4 text-left font-medium">运营方账号</th>
          <th class="h-10 px-4 text-left font-medium">权限</th>
          <th class="h-10 px-4 text-left font-medium">项目范围</th>
          <th class="h-10 px-4 text-left font-medium">有效期至</th>
          <th class="h-10 px-4 text-left font-medium">状态</th>
          <th class="h-10 px-4 text-right font-medium">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="String(row.id)" class="border-b">
            <td class="p-4"><strong>{{ row.operatorAccount || '历史租户授权' }}</strong><div class="font-mono text-xs text-muted-foreground">租户 {{ row.operatorTenantId }}</div></td>
            <td class="p-4">{{ jsonArray(row.permissionCodes).join('、') || '—' }}</td>
            <td class="p-4">{{ projectIds(row.dataScope).join('、') || '全部项目' }}</td>
            <td class="p-4">{{ row.validTo || '长期有效' }}</td>
            <td class="p-4"><Badge :variant="row.status === 1 ? 'default' : 'secondary'">{{ row.status === 1 ? '有效' : '已撤销' }}</Badge></td>
            <td class="p-4 text-right"><Button v-if="row.status === 1" variant="destructive" size="sm" @click="revoke(row)">撤销</Button></td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog v-model:open="dialogOpen" title="新建委托运营" :saving="saving" wide @save="save">
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="运营方账号" required><Input v-model="form.operatorAccount" placeholder="输入平台已注册的运营方用户名" /></FormField>
      <FormField label="权限" required>
        <div class="flex flex-wrap gap-5 rounded border p-3 text-sm">
          <label class="flex items-center gap-2"><input v-model="form.read" type="checkbox" />读取设备与数据</label>
          <label class="flex items-center gap-2"><input v-model="form.operate" type="checkbox" />控制设备与维护规则</label>
        </div>
      </FormField>
      <FormField label="项目 ID 范围"><Input v-model="form.projectIds" placeholder="例如 501,502；留空表示全部项目" /></FormField>
      <FormField label="有效期至"><Input v-model="form.validTo" type="datetime-local" /></FormField>
      <FormField label="授权用途"><Input v-model="form.purpose" placeholder="例如：东园区设备代运营" /></FormField>
      <p class="text-xs text-muted-foreground">运营方仍需具备自身角色权限；最终访问必须同时满足角色权限和本委托范围。</p>
    </CrudDialog>
  </div>
</template>
