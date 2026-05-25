<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Plus, Pencil, UserCheck } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import {
  listDevelopers,
  createDeveloper,
  updateDeveloper,
  listPendingDevelopers,
  approveDeveloper,
  applyDeveloper,
} from '@/api/developer'
import type { BaseDeveloper } from '@/api/types'
import { useMenuStore } from '@/stores/menu'

const tab = ref<'all' | 'pending'>('all')
const pending = ref<BaseDeveloper[]>([])
const pendingLoading = ref(false)
const pendingError = ref('')
const applyLoading = ref(false)
const applyMessage = ref('')
const menuStore = useMenuStore()
const canManageDevelopers = computed(
  () =>
    menuStore.allowedPaths.has('/system/users') ||
    menuStore.allowedPaths.has('/system/roles') ||
    menuStore.allowedMenuCodes.has('developer_mgmt'),
)

const keyword = ref('')
const statusFilter = ref('')
const userTypeFilter = ref('')

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseDeveloper>(
  (p, s) =>
    listDevelopers(p, s, {
      keyword: keyword.value || undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
      userType: userTypeFilter.value || undefined,
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
} = useCrudForm<BaseDeveloper>(() => ({
  userName: '',
  nickName: '',
  userType: 'dev',
  status: 1,
  password: '',
}))

function statusLabel(status?: number) {
  if (status === 0) return '待审批'
  if (status === 1) return '正常'
  if (status === 2) return '锁定'
  return String(status ?? '-')
}

function statusVariant(status?: number): 'default' | 'secondary' | 'destructive' {
  if (status === 1) return 'default'
  if (status === 0) return 'secondary'
  return 'destructive'
}

async function loadPending() {
  pendingLoading.value = true
  pendingError.value = ''
  try {
    pending.value = await listPendingDevelopers()
  } catch (e) {
    pendingError.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    pendingLoading.value = false
  }
}

async function switchTab(next: 'all' | 'pending') {
  tab.value = next
  if (next === 'pending' && canManageDevelopers.value) {
    await loadPending()
  }
}

async function handleApprove(row: BaseDeveloper) {
  if (!row.userId) return
  await approveDeveloper(row.userId)
  await loadPending()
  load(page.value)
}

async function handleApply() {
  applyLoading.value = true
  applyMessage.value = ''
  try {
    await applyDeveloper('dev')
    applyMessage.value = '申请已提交，请等待管理员审批'
    if (canManageDevelopers.value) await loadPending()
  } catch (e) {
    applyMessage.value = e instanceof Error ? e.message : '申请失败'
  } finally {
    applyLoading.value = false
  }
}

async function handleSave() {
  if (!form.value.userName?.trim()) {
    formError.value = '用户名不能为空'
    return
  }
  if (!editing.value && !form.value.password?.trim()) {
    formError.value = '新建开发者需填写密码'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    if (editing.value && form.value.userId) {
      const payload = { ...form.value }
      if (!payload.password) delete payload.password
      await updateDeveloper(form.value.userId, payload)
    } else {
      await createDeveloper(form.value)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (canManageDevelopers.value) loadPending()
})
</script>

<template>
  <div>
    <PageHeader title="开发者" description="申请、审批与开发者账号管理">
      <template #actions>
        <template v-if="canManageDevelopers && tab === 'all'">
          <Input
            v-model="keyword"
            placeholder="用户名/昵称"
            class="w-40"
            @keyup.enter="search"
          />
          <Select v-model="statusFilter" class="w-28">
            <option value="">全部状态</option>
            <option value="1">正常</option>
            <option value="0">待审批</option>
            <option value="2">锁定</option>
          </Select>
          <Select v-model="userTypeFilter" class="w-32">
            <option value="">全部类型</option>
            <option value="dev">自研开发者</option>
            <option value="isp">服务提供商</option>
          </Select>
          <Button variant="outline" @click="search">查询</Button>
        </template>
        <Button variant="outline" :disabled="applyLoading" @click="handleApply">
          申请成为开发者
        </Button>
        <Button v-if="canManageDevelopers" @click="openCreate">
          <Plus class="mr-1 h-4 w-4" />
          新建
        </Button>
      </template>
    </PageHeader>

    <p v-if="applyMessage" class="mb-3 text-sm text-muted-foreground">{{ applyMessage }}</p>

    <Card v-if="!canManageDevelopers" class="mb-4">
      <CardHeader>
        <CardTitle class="text-base">开发者接入</CardTitle>
      </CardHeader>
      <CardContent class="space-y-2 text-sm text-muted-foreground">
        <p>申请成为开发者后，可由管理员审批并开通 API Key 与开放接口访问权限。</p>
        <p>当前账号只显示自助接入入口，不展示其他开发者和后台审批数据。</p>
      </CardContent>
    </Card>

    <div v-if="canManageDevelopers" class="mb-4 flex gap-2">
      <Button :variant="tab === 'all' ? 'default' : 'outline'" size="sm" @click="switchTab('all')">
        全部开发者
      </Button>
      <Button :variant="tab === 'pending' ? 'default' : 'outline'" size="sm" @click="switchTab('pending')">
        待审批 ({{ pending.length }})
      </Button>
    </div>

    <template v-if="canManageDevelopers && tab === 'pending'">
      <DataTableShell :loading="pendingLoading" :error="pendingError" :empty="!pending.length">
        <Table>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-10 px-4 text-left font-medium">ID</th>
              <th class="h-10 px-4 text-left font-medium">用户名</th>
              <th class="h-10 px-4 text-left font-medium">昵称</th>
              <th class="h-10 px-4 text-left font-medium">类型</th>
              <th class="h-10 px-4 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pending" :key="row.userId" class="border-b">
              <td class="p-4">{{ row.userId }}</td>
              <td class="p-4">{{ row.userName }}</td>
              <td class="p-4">{{ row.nickName }}</td>
              <td class="p-4 font-mono text-xs">{{ row.userType }}</td>
              <td class="p-4 text-right">
                <Button variant="default" size="sm" @click="handleApprove(row)">
                  <UserCheck class="mr-1 h-3.5 w-3.5" />
                  审批通过
                </Button>
              </td>
            </tr>
          </tbody>
        </Table>
      </DataTableShell>
    </template>

    <template v-else-if="canManageDevelopers">
      <DataTableShell :loading="loading" :error="error" :empty="!items.length">
        <Table>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-10 px-4 text-left font-medium">ID</th>
              <th class="h-10 px-4 text-left font-medium">用户名</th>
              <th class="h-10 px-4 text-left font-medium">昵称</th>
              <th class="h-10 px-4 text-left font-medium">类型</th>
              <th class="h-10 px-4 text-left font-medium">状态</th>
              <th class="h-10 px-4 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in items" :key="row.userId" class="border-b">
              <td class="p-4">{{ row.userId }}</td>
              <td class="p-4">{{ row.userName }}</td>
              <td class="p-4">{{ row.nickName }}</td>
              <td class="p-4 font-mono text-xs">{{ row.userType }}</td>
              <td class="p-4">
                <Badge :variant="statusVariant(row.status)">
                  {{ statusLabel(row.status) }}
                </Badge>
              </td>
              <td class="p-4 text-right space-x-1">
                <Button
                  v-if="row.status === 0"
                  variant="default"
                  size="sm"
                  @click="handleApprove(row)"
                >
                  审批
                </Button>
                <Button variant="outline" size="sm" @click="openEdit(row)">
                  <Pencil class="h-3.5 w-3.5" />
                </Button>
              </td>
            </tr>
          </tbody>
        </Table>
        <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
      </DataTableShell>
    </template>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑开发者' : '新建开发者'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="用户名" required>
        <Input v-model="form.userName" :disabled="editing" />
      </FormField>
      <FormField label="昵称">
        <Input v-model="form.nickName" />
      </FormField>
      <FormField label="类型">
        <Select v-model="form.userType">
          <option value="dev">自研开发者</option>
          <option value="isp">服务提供商</option>
        </Select>
      </FormField>
      <FormField :label="editing ? '新密码（留空不改）' : '密码'" :required="!editing">
        <Input v-model="form.password" type="password" autocomplete="new-password" />
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">正常</option>
          <option :value="0">待审批 / 禁用</option>
          <option :value="2">锁定</option>
        </Select>
      </FormField>
    </CrudDialog>
  </div>
</template>
