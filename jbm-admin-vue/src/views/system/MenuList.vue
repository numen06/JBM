<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Plus, Pencil, RefreshCw } from 'lucide-vue-next'
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
import { usePermission } from '@/composables/usePermission'
import { listMenus, deleteMenu, createMenu, updateMenu, type MenuScope } from '@/api/menu'
import { listApps } from '@/api/app'
import type { BaseApp, BaseMenu } from '@/api/types'

const { isSuperAdmin } = usePermission()

const keyword = ref('')
const scopeFilter = ref<MenuScope>('visible')
const appIdFilter = ref<number | string>('')
const statusFilter = ref<number | string>('')

const apps = ref<BaseApp[]>([])
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
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: BaseMenu) {
  if (!row.menuId) return
  if (isPlatformMenu(row) && !isSuperAdmin.value) {
    alert('平台公共菜单仅平台超管可删除')
    return
  }
  if (row.isPersist) {
    alert('保留菜单不可删除')
    return
  }
  if (!confirm(`确认删除菜单 ${row.menuName}？`)) return
  try {
    await deleteMenu(row.menuId)
    load(page.value)
  } catch (e) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(async () => {
  await loadApps()
})
</script>

<template>
  <div>
    <PageHeader
      title="菜单管理"
      description="平台公共菜单（appId 为空）全局共享；应用菜单归属指定应用。租户管理员仅可管理本组织应用菜单。"
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
          <tr v-for="row in items" :key="row.menuId" class="border-b">
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
            <td class="p-4 text-right space-x-1">
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
  </div>
</template>
