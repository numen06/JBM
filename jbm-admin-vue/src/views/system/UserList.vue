<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Plus, Pencil } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import OrgTreeSelect from '@/components/OrgTreeSelect.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { orgRowId, useOrgTree } from '@/composables/useOrgTree'
import {
  listUsers,
  closeUser,
  createUser,
  updateUser,
  getUserRoles,
  getUserAccounts,
  getUserOrgs,
} from '@/api/user'
import { listAllRoles } from '@/api/role'
import { usePermission } from '@/composables/usePermission'
import type { BaseAccount, BaseRole, BaseUser } from '@/api/types'

const { hasAction } = usePermission()
const { flatOrgs, orgLabel, loadOrgs } = useOrgTree()

const allRoles = ref<BaseRole[]>([])
const selectedRoleIds = ref<string[]>([])
const selectedExtraOrgIds = ref<string[]>([])
const userAccounts = ref<BaseAccount[]>([])

onMounted(async () => {
  await loadOrgs()
  try {
    allRoles.value = await listAllRoles()
  } catch {
    allRoles.value = []
  }
})

const keyword = ref('')
const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseUser>(
  (p, s) => listUsers(p, s, keyword.value || undefined),
)

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseUser>(() => ({
  userName: '',
  nickName: '',
  mobile: '',
  email: '',
  status: 1,
  password: '',
  companyId: undefined,
  departmentId: undefined,
}))

const extraOrgOptions = computed(() =>
  flatOrgs.value.filter((o) => {
    const id = orgRowId(o)
    if (id == null) return false
    const primary = form.value.companyId ? Number(form.value.companyId) : null
    return primary == null || id !== primary
  }),
)

function search() {
  load(1)
}

async function openEditUser(row: BaseUser) {
  openEdit(row)
  selectedRoleIds.value = []
  selectedExtraOrgIds.value = []
  userAccounts.value = []
  if (!row.userId) return
  try {
    const [roles, accounts, userOrgs] = await Promise.all([
      getUserRoles(row.userId),
      getUserAccounts(row.userId),
      getUserOrgs(row.userId),
    ])
    selectedRoleIds.value = roles
      .map((r) => (r.roleId != null ? String(r.roleId) : ''))
      .filter(Boolean)
    userAccounts.value = accounts
    selectedExtraOrgIds.value = userOrgs
      .map((u) => (u.orgId != null ? String(u.orgId) : ''))
      .filter(Boolean)
  } catch {
    selectedRoleIds.value = []
    userAccounts.value = []
    selectedExtraOrgIds.value = []
  }
}

function openCreateUser() {
  selectedRoleIds.value = []
  selectedExtraOrgIds.value = []
  userAccounts.value = []
  openCreate()
}

const accountTypeLabel: Record<string, string> = {
  username: '用户名',
  mobile: '手机号',
  email: '邮箱',
}

function toggleRole(roleId?: number) {
  if (roleId == null) return
  const id = String(roleId)
  const idx = selectedRoleIds.value.indexOf(id)
  if (idx >= 0) selectedRoleIds.value.splice(idx, 1)
  else selectedRoleIds.value.push(id)
}

function toggleExtraOrg(orgId?: number) {
  if (orgId == null) return
  const id = String(orgId)
  const idx = selectedExtraOrgIds.value.indexOf(id)
  if (idx >= 0) selectedExtraOrgIds.value.splice(idx, 1)
  else selectedExtraOrgIds.value.push(id)
}

async function handleSave() {
  if (!form.value.userName?.trim()) {
    formError.value = '用户名不能为空'
    return
  }
  if (!editing.value && !form.value.password?.trim()) {
    formError.value = '新建用户须填写密码'
    return
  }
  saving.value = true
  formError.value = ''
  const companyId = form.value.companyId ? Number(form.value.companyId) : undefined
  const departmentId = form.value.departmentId ? Number(form.value.departmentId) : undefined
  const orgIds = selectedExtraOrgIds.value
  try {
    if (editing.value && form.value.userId) {
      await updateUser(form.value.userId, {
        nickName: form.value.nickName,
        mobile: form.value.mobile,
        email: form.value.email,
        status: form.value.status,
        companyId,
        departmentId,
        orgIds,
        ...(form.value.password ? { password: form.value.password } : {}),
        roleIds: selectedRoleIds.value,
      })
    } else {
      await createUser({ ...form.value, companyId, departmentId, orgIds })
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleClose(row: BaseUser) {
  if (!row.userId || !confirm(`确认注销用户 ${row.userName}？`)) return
  await closeUser(row.userId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader
      title="用户管理"
      description="Center /user — 按钮权限 ACTION_users_*；同一用户可用用户名/手机/邮箱登录"
    >
      <template #actions>
        <Input v-model="keyword" placeholder="关键字" class="w-40" @keyup.enter="search" />
        <Button variant="outline" @click="search">查询</Button>
        <Button v-if="hasAction('users_add')" @click="openCreateUser">
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
            <th class="h-10 px-4 text-left font-medium">用户名</th>
            <th class="h-10 px-4 text-left font-medium">昵称</th>
            <th class="h-10 px-4 text-left font-medium">手机</th>
            <th class="h-10 px-4 text-left font-medium">邮箱</th>
            <th class="h-10 px-4 text-left font-medium">所属组织</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.userId" class="border-b">
            <td class="p-4">{{ row.userId }}</td>
            <td class="p-4">{{ row.userName }}</td>
            <td class="p-4">{{ row.nickName }}</td>
            <td class="p-4">{{ row.mobile }}</td>
            <td class="p-4">{{ row.email }}</td>
            <td class="p-4">{{ orgLabel(row.companyId) }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '正常' : row.status === 0 ? '禁用' : '其他' }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1">
              <Button
                v-if="hasAction('users_edit')"
                variant="outline"
                size="sm"
                @click="openEditUser(row)"
              >
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button
                v-if="hasAction('users_delete')"
                variant="destructive"
                size="sm"
                @click="handleClose(row)"
              >
                注销
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑用户' : '新建用户'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="用户名" required>
        <Input v-model="form.userName" :disabled="editing" placeholder="登录名" />
      </FormField>
      <FormField label="昵称">
        <Input v-model="form.nickName" placeholder="显示名称" />
      </FormField>
      <FormField label="手机">
        <Input v-model="form.mobile" />
      </FormField>
      <FormField label="邮箱">
        <Input v-model="form.email" type="email" />
      </FormField>
      <FormField label="所属组织">
        <OrgTreeSelect v-model="form.companyId" placeholder="— 未选择 —" />
      </FormField>
      <FormField label="部门（可选）">
        <OrgTreeSelect v-model="form.departmentId" placeholder="— 未选择 —" />
      </FormField>
      <FormField
        v-if="extraOrgOptions.length"
        label="跨组织数据授权（可选）"
      >
        <p class="mb-2 text-xs text-muted-foreground">
          除主组织外，可授权访问其他组织的用户数据（不含主组织本身）。
        </p>
        <div class="flex max-h-36 flex-wrap gap-2 overflow-y-auto rounded border p-2">
          <label
            v-for="o in extraOrgOptions"
            :key="`extra-${orgRowId(o)}`"
            class="flex cursor-pointer items-center gap-1.5 rounded border px-2 py-1 text-sm"
          >
            <input
              type="checkbox"
              :checked="selectedExtraOrgIds.includes(String(orgRowId(o)))"
              @change="toggleExtraOrg(orgRowId(o))"
            />
            {{ o.orgName }}
          </label>
        </div>
      </FormField>
      <FormField :label="editing ? '新密码（留空不改）' : '密码'" :required="!editing">
        <Input v-model="form.password" type="password" autocomplete="new-password" />
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">正常</option>
          <option :value="0">禁用</option>
        </Select>
      </FormField>
      <FormField v-if="editing && userAccounts.length" label="登录凭证（同用户多账号）">
        <ul class="space-y-1 rounded border bg-muted/30 p-2 text-sm">
          <li v-for="acc in userAccounts" :key="acc.accountId" class="font-mono">
            <span class="text-muted-foreground">{{ accountTypeLabel[acc.accountType ?? ''] ?? acc.accountType }}：</span>
            {{ acc.account }}
          </li>
        </ul>
        <p class="mt-1 text-xs text-muted-foreground">
          保存手机/邮箱后会自动注册对应凭证；可用任一凭证 + 同一密码登录。
        </p>
      </FormField>
      <FormField v-if="editing && allRoles.length" label="角色">
        <div class="flex flex-wrap gap-2">
          <label
            v-for="r in allRoles"
            :key="r.roleId"
            class="flex cursor-pointer items-center gap-1.5 rounded border px-2 py-1 text-sm"
          >
            <input
              type="checkbox"
              :checked="selectedRoleIds.includes(String(r.roleId))"
              @change="toggleRole(r.roleId)"
            />
            {{ r.roleName }}
          </label>
        </div>
      </FormField>
    </CrudDialog>
  </div>
</template>
