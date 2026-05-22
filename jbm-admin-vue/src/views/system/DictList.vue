<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Button from '@/components/ui/Button.vue'
import Table from '@/components/ui/Table.vue'
import { listDicts } from '@/api/dict'
import type { BaseDic } from '@/api/types'

const items = ref<BaseDic[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await listDicts()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="字典管理" description="POST /baseDic/list">
      <template #actions>
        <Button variant="outline" @click="load">刷新</Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!items.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">编码</th>
            <th class="h-10 px-4 text-left font-medium">名称</th>
            <th class="h-10 px-4 text-left font-medium">值</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.dicId" class="border-b">
            <td class="p-4">{{ row.dicId }}</td>
            <td class="p-4">{{ row.dicCode }}</td>
            <td class="p-4">{{ row.dicName }}</td>
            <td class="p-4">{{ row.dicValue }}</td>
          </tr>
        </tbody>
      </Table>
    </DataTableShell>
  </div>
</template>
