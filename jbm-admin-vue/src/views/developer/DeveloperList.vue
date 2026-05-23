<script setup lang="ts">
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
import { listDevelopers, createDeveloper, updateDeveloper } from '@/api/developer'
import type { BaseDeveloper } from '@/api/types'

const { items, total, page, loading, error, load, pageSize } =
  usePagedList<BaseDeveloper>(listDevelopers)

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

async function handleSave() {
  if (!form.value.userName?.trim()) {
    formError.value = '用户名不能为空'
    return
  }
  if (!editing.value && !form.value.password?.trim()) {
    formError.value = '新建开发者须填写密码'
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
</script>

<template>
  <div>
    <PageHeader title="开发者" description="GET /developer — 系统开发者账号">
      <template #actions>
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
              <Badge :variant="row.status === 1 ? 'default' : 'secondary'">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </Badge>
            </td>
            <td class="p-4 text-right">
              <Button variant="outline" size="sm" @click="openEdit(row)">
                <Pencil class="h-3.5 w-3.5" />
              </Button>
            </td>
          </tr>
        </tbody>
      </Table>
      <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="load" />
    </DataTableShell>

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
          <option :value="0">禁用</option>
        </Select>
      </FormField>
    </CrudDialog>
  </div>
</template>
