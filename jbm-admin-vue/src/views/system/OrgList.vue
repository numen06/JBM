<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Button from '@/components/ui/Button.vue'
import Table from '@/components/ui/Table.vue'
import { listOrgTree } from '@/api/org'
import type { BaseOrg } from '@/api/types'

const flat = ref<Array<BaseOrg & { depth: number }>>([])
const loading = ref(true)
const error = ref('')

function flatten(orgs: BaseOrg[], depth = 0): Array<BaseOrg & { depth: number }> {
  const out: Array<BaseOrg & { depth: number }> = []
  for (const o of orgs) {
    out.push({ ...o, depth })
    if (o.children?.length) out.push(...flatten(o.children, depth + 1))
  }
  return out
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const tree = await listOrgTree()
    flat.value = flatten(tree)
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
    <PageHeader title="组织管理" description="POST /baseOrg/tree">
      <template #actions>
        <Button variant="outline" @click="load">刷新</Button>
      </template>
    </PageHeader>
    <DataTableShell :loading="loading" :error="error" :empty="!flat.length">
      <Table>
        <thead>
          <tr class="border-b bg-muted/50">
            <th class="h-10 px-4 text-left font-medium">ID</th>
            <th class="h-10 px-4 text-left font-medium">组织名称</th>
            <th class="h-10 px-4 text-left font-medium">父级</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in flat" :key="row.orgId" class="border-b">
            <td class="p-4">{{ row.orgId }}</td>
            <td class="p-4" :style="{ paddingLeft: `${row.depth * 16 + 16}px` }">{{ row.orgName }}</td>
            <td class="p-4">{{ row.parentId }}</td>
          </tr>
        </tbody>
      </Table>
    </DataTableShell>
  </div>
</template>
