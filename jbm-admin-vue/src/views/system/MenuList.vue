<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Button from '@/components/ui/Button.vue'
import Table from '@/components/ui/Table.vue'
import { listAllMenus, deleteMenu } from '@/api/menu'
import type { BaseMenu } from '@/api/types'

const items = ref<BaseMenu[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await listAllMenus()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleDelete(row: BaseMenu) {
  if (!row.menuId || !confirm(`确认删除菜单 ${row.menuName}？`)) return
  await deleteMenu(row.menuId)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="菜单管理" description="GET /menu/all">
      <template #actions>
        <Button variant="outline" @click="load">刷新</Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">路径</th>
            <th class="h-10 px-4 text-left font-medium">父级</th>
            <th class="h-10 px-4 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.menuId" class="border-b">
            <td class="p-4">{{ row.menuId }}</td>
            <td class="p-4">{{ row.menuName }}</td>
            <td class="p-4 font-mono text-xs">{{ row.path }}</td>
            <td class="p-4">{{ row.parentId }}</td>
            <td class="p-4 text-right">
              <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
            </td>
          </tr>
        </tbody>
      </Table>
    </DataTableShell>
  </div>
</template>
