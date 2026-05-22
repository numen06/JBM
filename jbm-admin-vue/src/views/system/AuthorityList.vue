<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Table from '@/components/ui/Table.vue'
import { listResources, listApis, listAuthorityMenus } from '@/api/authority'
import type { AuthorityResource, AuthorityApi, AuthorityMenu } from '@/api/authority'

const resources = ref<AuthorityResource[]>([])
const apis = ref<AuthorityApi[]>([])
const menus = ref<AuthorityMenu[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const [r, a, m] = await Promise.all([listResources(), listApis(), listAuthorityMenus()])
    resources.value = r
    apis.value = a
    menus.value = m
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-6">
    <PageHeader title="权限管理" description="Center /authority" />
    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>
    <div class="grid gap-6 lg:grid-cols-3">
      <Card>
        <CardHeader><CardTitle class="text-base">资源权限</CardTitle></CardHeader>
        <CardContent>
          <DataTableShell :loading="loading" :empty="!resources.length">
            <Table>
              <tbody>
                <tr v-for="(row, i) in resources.slice(0, 20)" :key="i" class="border-b">
                  <td class="p-2 text-sm">{{ row.resourceName || row.resourceId }}</td>
                </tr>
              </tbody>
            </Table>
          </DataTableShell>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle class="text-base">接口权限</CardTitle></CardHeader>
        <CardContent>
          <DataTableShell :loading="loading" :empty="!apis.length">
            <Table>
              <tbody>
                <tr v-for="(row, i) in apis.slice(0, 20)" :key="i" class="border-b">
                  <td class="p-2 text-sm font-mono">{{ row.path || row.apiName }}</td>
                </tr>
              </tbody>
            </Table>
          </DataTableShell>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle class="text-base">菜单权限</CardTitle></CardHeader>
        <CardContent>
          <DataTableShell :loading="loading" :empty="!menus.length">
            <Table>
              <tbody>
                <tr v-for="row in menus.slice(0, 20)" :key="row.menuId" class="border-b">
                  <td class="p-2 text-sm">{{ row.menuName }}</td>
                </tr>
              </tbody>
            </Table>
          </DataTableShell>
        </CardContent>
      </Card>
    </div>
  </div>
</template>
