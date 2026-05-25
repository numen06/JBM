<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Plus, Pencil, RefreshCw, Trash2 } from 'lucide-vue-next'
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
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useCrudForm } from '@/composables/useCrudForm'
import { usePermission } from '@/composables/usePermission'
import { useFeedback } from '@/composables/useFeedback'
import { listMenus, deleteMenu, createMenu, updateMenu, type MenuScope } from '@/api/menu'
import { listActions, createAction, updateAction, deleteAction } from '@/api/action'
import { listApps } from '@/api/app'
import type { BaseAction, BaseApp, BaseMenu } from '@/api/types'

const { isSuperAdmin } = usePermission()
const feedback = useFeedback()

const keyword = ref('')
const scopeFilter = ref<MenuScope>('visible')
const appIdFilter = ref<number | string>('')
const statusFilter = ref<number | string>('')

const apps = ref<BaseApp[]>([])
const selectedMenu = ref<BaseMenu | null>(null)
const menuActions = ref<BaseAction[]>([])
const actionsLoading = ref(false)
const actionsError = ref('')

const {
  dialogOpen: actionDialogOpen,
  editing: actionEditing,
  saving: actionSaving,
  form: actionForm,
  formError: actionFormError,
  openCreate: openActionCreate,
  openEdit: openActionEdit,
  closeDialog: closeActionDialog,
} = useCrudForm<BaseAction>(() => ({
  actionCode: '',
  actionName: '',
  menuId: undefined,
  priority: 0,
  status: 1,
}))
const appNameMap = computed(() => {
  const map = new Map<number, string>()
  for (const app of apps.value) {
    if (app.appId != null) map.set(app.appId, app.appName ?? String(app.appId))
  }
  return map
})

const { items, total, page, loading, error, load, pageSize } = usePagedList<BaseMenu>(
  (p, s) =>
    listMenus(p, s, {
      keyword: keyword.value || undefined,
      scope: scopeFilter.value,
      appId:
        appIdFilter.value !== '' && appIdFilter.value != null
          ? appIdFilter.value
          : undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
    }),
)

const menuScopeOptions: { value: MenuScope; label: string }[] = [
  { value: 'visible', label: '当前可见菜单' },
  { value: 'platform', label: '平台菜单' },
  { value: 'app', label: '应用菜单' },
  { value: 'all', label: '全部菜单' },
]

const formMenuScope = ref<'platform' | 'app'>('app')

const {
  dialogOpen,
  editing,
  saving,
  form,
  formError,
  openCreate,
  openEdit,
  closeDialog,
} = useCrudForm<BaseMenu>(() => ({
  menuCode: '',
  menuName: '',
  path: '',
  parentId: undefined,
  icon: '',
  priority: 0,
  status: 1,
  appId: undefined,
}))

function isPlatformMenu(row: BaseMenu) {
  return row.appId == null
}

function canManageMenu(row: BaseMenu) {
  if (isPlatformMenu(row) && !isSuperAdmin.value) return false
  if (row.isPersist) return false
  return true
}

function menuScopeLabel(row: BaseMenu) {
  return isPlatformMenu(row) ? '平台公共' : '应用菜单'
}

function appLabel(row: BaseMenu) {
  if (isPlatformMenu(row)) return '—'
  const id = row.appId
  if (id == null) return '—'
  return appNameMap.value.get(id) ?? String(id)
}

function search() {
  load(1)
}

function onScopeChange() {
  if (scopeFilter.value === 'platform') {
    appIdFilter.value = ''
  }
  load(1)
}

function openCreateMenu() {
  formMenuScope.value = isSuperAdmin.value ? 'platform' : 'app'
  openCreate()
  if (formMenuScope.value === 'app') {
    const first = apps.value.find((a) => a.appId != null)
    form.value.appId = first?.appId
  } else {
    form.value.appId = undefined
  }
}

function openEditMenu(row: BaseMenu) {
  openEdit(row)
  formMenuScope.value = isPlatformMenu(row) ? 'platform' : 'app'
}

function onFormScopeChange() {
  if (formMenuScope.value === 'platform') {
    form.value.appId = undefined
  } else if (form.value.appId == null) {
    const first = apps.value.find((a) => a.appId != null)
    form.value.appId = first?.appId
  }
}

async function loadMenuActions(menuId?: number) {
  if (!menuId) {
    menuActions.value = []
    return
  }
  actionsLoading.value = true
  actionsError.value = ''
  try {
    const data = await listActions(menuId, 1, 200)
    menuActions.value = data.contents ?? []
  } catch (e) {
    actionsError.value = e instanceof Error ? e.message : '加载按钮失败'
    menuActions.value = []
  } finally {
    actionsLoading.value = false
  }
}

function selectMenu(row: BaseMenu) {
  selectedMenu.value = row
  loadMenuActions(row.menuId)
}

function openCreateActionForMenu() {
  if (!selectedMenu.value?.menuId) return
  openActionCreate()
  actionForm.value.menuId = selectedMenu.value.menuId
}

async function handleActionSave() {
  if (!actionForm.value.actionCode?.trim() || !actionForm.value.actionName?.trim()) {
    actionFormError.value = '按钮编码和名称不能为空'
    return
  }
  if (!actionForm.value.menuId) {
    actionFormError.value = '必须归属菜单'
    return
  }
  actionSaving.value = true
  actionFormError.value = ''
  try {
    const payload = {
      ...actionForm.value,
      menuId: Number(actionForm.value.menuId),
      priority: actionForm.value.priority != null ? Number(actionForm.value.priority) : 0,
    }
    if (actionEditing.value && actionForm.value.actionId) {
      await updateAction(actionForm.value.actionId, payload)
    } else {
      await createAction(payload)
    }
    closeActionDialog()
    await loadMenuActions(selectedMenu.value?.menuId)
  } catch (e) {
    actionFormError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    actionSaving.value = false
  }
}

async function handleActionDelete(row: BaseAction) {
  if (!row.actionId) return
  const confirmed = await feedback.confirm({
    title: '确认删除按钮',
    message: `确认删除按钮 ${row.actionName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteAction(row.actionId)
  await loadMenuActions(selectedMenu.value?.menuId)
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
  if (!form.value.menuCode?.trim() || !form.value.menuName?.trim()) {
    formError.value = '菜单编码和名称不能为空'
    return
  }
  if (formMenuScope.value === 'app' && !form.value.appId) {
    formError.value = '应用菜单必须选择所属应用'
    return
  }
  if (formMenuScope.value === 'platform' && !isSuperAdmin.value) {
    formError.value = '仅平台超管可创建/修改平台公共菜单'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const payload: Partial<BaseMenu> = {
      ...form.value,
      parentId: form.value.parentId ? Number(form.value.parentId) : undefined,
      priority:
        form.value.priority != null
          ? Number(form.value.priority)
          : form.value.sort != null
            ? Number(form.value.sort)
            : 0,
      appId: formMenuScope.value === 'platform' ? undefined : Number(form.value.appId),
    }
    if (editing.value && form.value.menuId) {
      await updateMenu(form.value.menuId, payload)
    } else {
      await createMenu(payload)
    }
    closeDialog()
    await load(page.value)
    if (selectedMenu.value?.menuId === form.value.menuId) {
      selectedMenu.value = { ...selectedMenu.value, ...payload }
    }
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseMenu) {
  if (!row.menuId) return
  if (isPlatformMenu(row) && !isSuperAdmin.value) {
    await feedback.alert({
      title: '无法删除菜单',
      message: '平台公共菜单仅平台超管可删除',
      variant: 'destructive',
    })
    return
  }
  if (row.isPersist) {
    await feedback.alert({
      title: '无法删除菜单',
      message: '保留菜单不可删除',
      variant: 'destructive',
    })
    return
  }
  const confirmed = await feedback.confirm({
    title: '确认删除菜单',
    message: `确认删除菜单 ${row.menuName}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  try {
    await deleteMenu(row.menuId)
    if (selectedMenu.value?.menuId === row.menuId) {
      selectedMenu.value = null
      menuActions.value = []
    }
    load(page.value)
  } catch (e) {
    await feedback.alert({
      title: '删除失败',
      message: e instanceof Error ? e.message : '删除失败',
      variant: 'destructive',
    })
  }
}

onMounted(async () => {
  await loadApps()
})
</script>

<template>
  <div>
    <PageHeader
      title="菜单与按钮"
      description="维护菜单及其下挂按钮权限（ACTION_{actionCode}）；选中菜单行后在下方管理按钮。"
    >
      <template #actions>
        <Select v-model="scopeFilter" class="w-36" @change="onScopeChange">
          <option v-for="opt in menuScopeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </Select>
        <Select
          v-if="scopeFilter === 'app' || scopeFilter === 'visible'"
          v-model="appIdFilter"
          class="w-40"
          @change="search"
        >
          <option value="">全部应用</option>
          <option v-for="app in apps" :key="app.appId" :value="app.appId">
            {{ app.appName }}
          </option>
        </Select>
        <Select v-model="statusFilter" class="w-28" @change="search">
          <option value="">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
        <Input
          v-model="keyword"
          placeholder="编码/名称/路径"
          class="w-44"
          @keyup.enter="search"
        />
        <Button variant="outline" @click="search">
          <RefreshCw class="mr-1 h-4 w-4" />
          搜索
        </Button>
        <Button @click="openCreateMenu">
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
            <th class="h-10 px-4 text-left font-medium">路径</th>
            <th class="h-10 px-4 text-left font-medium">范围</th>
            <th class="h-10 px-4 text-left font-medium">应用</th>
            <th class="h-10 px-4 text-left font-medium">父级</th>
            <th class="h-10 px-4 text-left font-medium">排序</th>
            <th class="h-10 px-4 text-left font-medium">状态</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in items"
            :key="row.menuId"
            class="border-b cursor-pointer hover:bg-muted/30"
            :class="selectedMenu?.menuId === row.menuId ? 'bg-muted/40' : ''"
            @click="selectMenu(row)"
          >
            <td class="p-4">{{ row.menuId }}</td>
            <td class="p-4 font-mono text-xs">{{ row.menuCode }}</td>
            <td class="p-4">
              {{ row.menuName }}
              <Badge v-if="isPlatformMenu(row)" variant="secondary" class="ml-1">平台</Badge>
              <Badge v-if="row.isPersist" variant="outline" class="ml-1">保留</Badge>
            </td>
            <td class="p-4 font-mono text-xs">{{ row.path }}</td>
            <td class="p-4">{{ menuScopeLabel(row) }}</td>
            <td class="p-4">{{ appLabel(row) }}</td>
            <td class="p-4">{{ row.parentId ?? '—' }}</td>
            <td class="p-4">{{ row.priority ?? row.sort ?? 0 }}</td>
            <td class="p-4">
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </Badge>
            </td>
            <td class="p-4 text-right space-x-1" @click.stop>
              <Button
                variant="outline"
                size="sm"
                :disabled="!canManageMenu(row)"
                :title="
                  isPlatformMenu(row) && !isSuperAdmin
                    ? '平台公共菜单仅平台超管可编辑'
                    : row.isPersist
                      ? '保留菜单不可编辑'
                      : undefined
                "
                @click="openEditMenu(row)"
              >
                <Pencil class="h-3.5 w-3.5" />
              </Button>
              <Button
                variant="destructive"
                size="sm"
                :disabled="!canManageMenu(row)"
                :title="
                  isPlatformMenu(row) && !isSuperAdmin
                    ? '平台公共菜单仅平台超管可删除'
                    : row.isPersist
                      ? '保留菜单不可删除'
                      : undefined
                "
                @click="handleDelete(row)"
              >
                删除
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar
        v-if="total > 0"
        :page="page"
        :total="total"
        :page-size="pageSize"
        @change="load"
      />
    </DataTableShell>

    <Card v-if="selectedMenu" class="mt-6">
      <CardContent class="space-y-4 pt-6">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h3 class="font-semibold">{{ selectedMenu.menuName }} — 按钮管理</h3>
            <p class="text-sm text-muted-foreground">
              菜单 ID {{ selectedMenu.menuId }} · {{ selectedMenu.path || '无路径' }}
            </p>
          </div>
          <Button @click="openCreateActionForMenu">
            <Plus class="mr-1 h-4 w-4" />
            新增按钮
          </Button>
        </div>
        <p v-if="actionsError" class="text-sm text-destructive">{{ actionsError }}</p>
        <p v-if="actionsLoading" class="text-sm text-muted-foreground">加载按钮中…</p>
        <Table v-else>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-9 px-3 text-left text-xs font-medium">ID</th>
              <th class="h-9 px-3 text-left text-xs font-medium">名称</th>
              <th class="h-9 px-3 text-left text-xs font-medium">权限标识</th>
              <th class="h-9 px-3 text-left text-xs font-medium">排序</th>
              <th class="h-9 px-3 text-left text-xs font-medium">状态</th>
              <th class="h-9 px-3 text-right text-xs font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="act in menuActions" :key="act.actionId" class="border-b">
              <td class="p-3 text-sm">{{ act.actionId }}</td>
              <td class="p-3 text-sm">{{ act.actionName }}</td>
              <td class="p-3">
                <Badge variant="outline" class="font-mono text-xs">ACTION_{{ act.actionCode }}</Badge>
              </td>
              <td class="p-3 text-sm">{{ act.priority ?? 0 }}</td>
              <td class="p-3">
                <Badge :variant="act.status === 1 ? 'default' : 'secondary'">
                  {{ act.status === 1 ? '启用' : '停用' }}
                </Badge>
              </td>
              <td class="p-3 text-right space-x-1">
                <Button variant="outline" size="sm" @click="openActionEdit(act)">
                  <Pencil class="h-3.5 w-3.5" />
                </Button>
                <Button variant="destructive" size="sm" @click="handleActionDelete(act)">
                  <Trash2 class="h-3.5 w-3.5" />
                </Button>
              </td>
            </tr>
          </tbody>
        </Table>
        <p v-if="!actionsLoading && !menuActions.length" class="text-sm text-muted-foreground">
          当前菜单暂无按钮，点击「新增按钮」创建。
        </p>
      </CardContent>
    </Card>
    <p v-else class="mt-4 text-sm text-muted-foreground">点击表格中的菜单行以管理其按钮。</p>

    <CrudDialog
      v-model:open="dialogOpen"
      :title="editing ? '编辑菜单' : '新建菜单'"
      :saving="saving"
      @save="handleSave"
    >
      <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
      <FormField label="菜单范围">
        <Select
          v-model="formMenuScope"
          :disabled="editing && isPlatformMenu(form) && !isSuperAdmin"
          @change="onFormScopeChange"
        >
          <option v-if="isSuperAdmin" value="platform">平台公共菜单</option>
          <option value="app">应用菜单</option>
        </Select>
      </FormField>
      <FormField v-if="formMenuScope === 'app'" label="所属应用" required>
        <Select v-model="form.appId">
          <option v-for="app in apps" :key="app.appId" :value="app.appId">
            {{ app.appName }}
          </option>
        </Select>
      </FormField>
      <FormField label="菜单编码" required>
        <Input v-model="form.menuCode" />
      </FormField>
      <FormField label="菜单名称" required>
        <Input v-model="form.menuName" />
      </FormField>
      <FormField label="路径">
        <Input v-model="form.path" placeholder="/system/xxx" />
      </FormField>
      <FormField label="父级 ID">
        <Input v-model="form.parentId" type="number" placeholder="根菜单留空" />
      </FormField>
      <FormField label="图标">
        <Input v-model="form.icon" placeholder="lucide 图标名" />
      </FormField>
      <FormField label="排序">
        <Input v-model="form.priority" type="number" />
      </FormField>
      <FormField label="状态">
        <Select v-model="form.status">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
    </CrudDialog>

    <CrudDialog
      v-model:open="actionDialogOpen"
      :title="actionEditing ? '编辑按钮' : '新建按钮'"
      :saving="actionSaving"
      @save="handleActionSave"
    >
      <p v-if="actionFormError" class="text-sm text-destructive">{{ actionFormError }}</p>
      <FormField label="所属菜单">
        <Input :model-value="selectedMenu?.menuName ?? ''" disabled />
      </FormField>
      <FormField label="按钮编码" required>
        <Input v-model="actionForm.actionCode" placeholder="如 users_add" />
      </FormField>
      <FormField label="按钮名称" required>
        <Input v-model="actionForm.actionName" />
      </FormField>
      <p class="text-xs text-muted-foreground">
        权限标识：ACTION_{{ actionForm.actionCode || '…' }}
      </p>
      <FormField label="排序">
        <Input v-model="actionForm.priority" type="number" />
      </FormField>
      <FormField label="状态">
        <Select v-model="actionForm.status">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </Select>
      </FormField>
    </CrudDialog>
  </div>
</template>
