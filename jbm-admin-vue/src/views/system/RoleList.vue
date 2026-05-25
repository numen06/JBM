<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Pencil, KeyRound } from 'lucide-vue-next'
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
import { useMenuActionPermissions } from '@/composables/useMenuActionPermissions'
import { listRoles, deleteRole, createRole, updateRole } from '@/api/role'
import {
  listAuthorityMenus,
  listAuthorityCatalog,
  getRoleAuthorities,
  putRoleAuthorities,
  type AuthorityMenu,
  type OpenAuthority,
} from '@/api/authority'
import { listActions } from '@/api/action'
import type { BaseAction, BaseRole } from '@/api/types'

const keyword = ref('')
const feedback = useFeedback()
const statusFilter = ref('')

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseRole>(
  (p, s) =>
    listRoles(p, s, {
      keyword: keyword.value || undefined,
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
} = useCrudForm<BaseRole>(() => ({
  roleCode: '',
  roleName: '',
  remark: '',
  status: 1,
}))

const permDialogOpen = ref(false)
const permRole = ref<BaseRole | null>(null)
const allMenus = ref<AuthorityMenu[]>([])
const menuActions = ref<Record<number, BaseAction[]>>({})
const authorityCatalog = ref<OpenAuthority[]>([])
const permSaving = ref(false)
const permError = ref('')

const {
  selectedAuthorityIds,
  authorityIdForActionCode,
  isMenuFullyChecked,
  isMenuIndeterminate,
  toggleMenu,
  toggleAction,
  selectAllActionsForMenu,
  clearAllActionsForMenu,
  ensureMenuPermissionsBeforeSave,
  resetSelected,
} = useMenuActionPermissions(authorityCatalog, menuActions)

function syncMenuCheckbox(el: unknown, menuId?: number) {
  if (el instanceof HTMLInputElement && menuId) {
    el.indeterminate = isMenuIndeterminate(menuId)
  }
}

async function openPermissions(row: BaseRole) {
  if (!row.roleId) return
  permRole.value = row
  permError.value = ''
  permDialogOpen.value = true
  try {
    const [menus, catalog, granted, allActions] = await Promise.all([
      listAuthorityMenus(),
      listAuthorityCatalog('1'),
      getRoleAuthorities(row.roleId),
      listActions(undefined, 1, 500),
    ])
    authorityCatalog.value = catalog ?? []
    allMenus.value = menus.filter((m) => m.menuId && m.path && m.path !== '/')
    const byMenu: Record<number, BaseAction[]> = {}
    for (const a of allActions.contents ?? []) {
      if (a.menuId == null) continue
      if (!byMenu[a.menuId]) byMenu[a.menuId] = []
      byMenu[a.menuId].push(a)
    }
    menuActions.value = byMenu
    resetSelected(granted.map((g) => String(g.authorityId)).filter(Boolean))
  } catch (e) {
    permError.value = e instanceof Error ? e.message : '加载权限失败'
  }
}

async function savePermissions() {
  if (!permRole.value?.roleId) return
  permSaving.value = true
  permError.value = ''
  try {
    const ids = ensureMenuPermissionsBeforeSave()
    await putRoleAuthorities(permRole.value.roleId, ids)
    permDialogOpen.value = false
  } catch (e) {
    permError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    permSaving.value = false
  }
}

async function handleSave() {
  if (!form.value.roleCode?.trim() || !form.value.roleName?.trim()) {
    formError.value = '角色编码和名称不能为空'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    if (editing.value && form.value.roleId) {
      await updateRole(form.value.roleId, form.value)
    } else {
      await createRole(form.value)
    }
    closeDialog()
    load(page.value)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseRole) {
  if (!row.roleId) return
  const confirmed = await feedback.confirm({
    title: '确认删除角色',
    message: `确认删除角色 ${row.roleName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteRole(row.roleId)
  load(page.value)
}
</script>

<template>
  <div>
    <PageHeader
      title="角色管理"
      description="Center /role — 为角色分配菜单与按钮权限（ACTION_*）"
    >
      <template #actions>
        <Input v-model="keyword" placeholder="编码/名称" class="w-40" @keyup.enter="search" />
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
            <th class="h-10 px-4 text-left font-medium">编码</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-left font-medium">备注</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.roleId" class="border-b">
            <td class="p-4">{{ row.roleId }}</td>
            <td class="p-4 font-mono text-sm">{{ row.roleCode }}</td>
            <td class="p-4">{{ row.roleName }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </Badge>
            </td>
            <td class="p-4 text-muted-foreground">{{ row.remark }}</td>
            <td class="p-4 text-right space-x-1">
              <Button variant="outline" size="sm" title="分配菜单与按钮权限" @click="openPermissions(row)">
                <KeyRound class="h-3.5 w-3.5" />
              </Button>
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
      :title="editing ? '编辑角色' : '新建角色'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="角色编码" required>
        <Input v-model="form.roleCode" placeholder="ROLE_xxx" />
      </FormField>
      <FormField label="角色名称" required>
        <Input v-model="form.roleName" />
      </FormField>
      <FormField label="备注">
        <Input v-model="form.remark" />
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
    </CrudDialog>

    <CrudDialog
      v-model:open="permDialogOpen"
      :title="`权限：${permRole?.roleName ?? ''}`"
      :saving="permSaving"
      wide
      @save="savePermissions"
    >
      <p v-if="permError" class="text-sm text-destructive">{{ permError }}</p>
      <p class="text-sm text-muted-foreground">
        勾选按钮会自动勾选所属菜单；取消菜单会同步取消其下按钮。
      </p>
      <div class="max-h-96 space-y-3 overflow-y-auto">
        <div
          v-for="m in allMenus"
          :key="m.menuId"
          class="rounded border px-3 py-2"
        >
          <div class="flex flex-wrap items-center gap-2">
            <label class="flex flex-1 cursor-pointer items-center gap-2 text-sm font-medium">
              <input
                type="checkbox"
                :checked="isMenuFullyChecked(m.menuId)"
                :ref="(el) => syncMenuCheckbox(el, m.menuId)"
                @change="toggleMenu(m.menuId)"
              />
              {{ m.menuName }}
              <span class="font-mono text-xs font-normal text-muted-foreground">{{ m.path }}</span>
            </label>
            <div v-if="m.menuId && (menuActions[m.menuId]?.length ?? 0) > 0" class="flex gap-1">
              <Button variant="outline" size="sm" type="button" @click="selectAllActionsForMenu(m.menuId)">
                全选按钮
              </Button>
              <Button variant="outline" size="sm" type="button" @click="clearAllActionsForMenu(m.menuId)">
                清空按钮
              </Button>
            </div>
          </div>
          <div
            v-if="m.menuId && (menuActions[m.menuId]?.length ?? 0) > 0"
            class="mt-2 ml-6 flex flex-wrap gap-2"
          >
            <label
              v-for="act in menuActions[m.menuId]"
              :key="act.actionId"
              class="flex cursor-pointer items-center gap-1 rounded bg-muted/40 px-2 py-1 text-xs"
            >
              <input
                type="checkbox"
                :checked="
                  !!act.actionCode &&
                  selectedAuthorityIds.has(authorityIdForActionCode(act.actionCode) ?? '')
                "
                @change="toggleAction(act.actionCode, m.menuId)"
              />
              {{ act.actionName }}
              <span class="font-mono text-muted-foreground">ACTION_{{ act.actionCode }}</span>
            </label>
          </div>
        </div>
      </div>
    </CrudDialog>
  </div>
</template>
