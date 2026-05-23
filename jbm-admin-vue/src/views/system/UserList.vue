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
import { listUsers, closeUser, createUser, updateUser, getUserRoles, getUserAccounts } from '@/api/user'
import { listAllRoles } from '@/api/role'
import { usePermission } from '@/composables/usePermission'
import type { BaseAccount, BaseRole, BaseUser } from '@/api/types'

const { hasAction } = usePermission()

const allRoles = ref<BaseRole[]>([])
const selectedRoleIds = ref<string[]>([])
const userAccounts = ref<BaseAccount[]>([])

onMounted(async () => {
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
}))

function search() {
  load(1)
}

async function openEditUser(row: BaseUser) {
  openEdit(row)
  selectedRoleIds.value = []
  userAccounts.value = []
  if (!row.userId) return
  try {
    const [roles, accounts] = await Promise.all([
      getUserRoles(row.userId),
      getUserAccounts(row.userId),
    ])
    selectedRoleIds.value = roles
      .map((r) => (r.roleId != null ? String(r.roleId) : ''))
      .filter(Boolean)
    userAccounts.value = accounts
  } catch {
    selectedRoleIds.value = []
    userAccounts.value = []
  }
}

function openCreateUser() {
  selectedRoleIds.value = []
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
  try {
    if (editing.value && form.value.userId) {
      await updateUser(form.value.userId, {
        nickName: form.value.nickName,
        mobile: form.value.mobile,
        email: form.value.email,
        status: form.value.status,
        ...(form.value.password ? { password: form.value.password } : {}),
        roleIds: selectedRoleIds.value,
      })
    } else {
      await createUser(form.value)
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
