<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import { usePagedList } from '@/composables/usePagedList'
import { get, post, unwrap } from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { isPlatformAdministrator } from '@/lib/adminAccess'
const auth = useAuthStore()
const globalScope = computed(() => isPlatformAdministrator(auth.user) || auth.user?.roles?.some(role => role.roleCode === 'security_auditor'))
const archiveState = ref('正在检查归档状态…')
const archiveError = ref(false)
interface AuditEvent { eventId: string; timestamp: number; account?: string; userId?: string; clientId?: string; tenantId?: string; operation?: string; target?: string; ip?: string; requestId?: string; success?: boolean; integrityVerified?: boolean }
const filters = ref({ account: '', tenantId: '', category: '', operation: '', beginTime: '', endTime: '' })
const list = usePagedList<AuditEvent>(async (page, size) => {
  const { tenantId, ...values } = filters.value
  const result = unwrap(await post<{ records: AuditEvent[]; total: number }>('/logs/securityAudit/query', { ...Object.fromEntries(Object.entries(values).filter(([, value]) => value)), ...(globalScope.value && tenantId ? { tenantId } : {}), page, size }))
  return { contents: result.records, total: result.total }
})
function reset() { filters.value = { account: '', tenantId: '', category: '', operation: '', beginTime: '', endTime: '' }; list.load(1) }
async function checkArchive() {
  try { const state = unwrap(await get<{ healthy: boolean }>('/logs/securityAudit/status')); archiveError.value = !state.healthy; archiveState.value = state.healthy ? '日志归档正常 · 记录只读' : '日志归档异常，请联系管理员' }
  catch (error) { archiveError.value = true; archiveState.value = error instanceof Error ? error.message : '暂时无法核实归档状态' }
}
function refresh() { void list.load(list.page.value); void checkArchive() }
onMounted(checkArchive)
function date(value: number) { return new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false }) }
</script>
<template>
  <div class="space-y-4">
    <PageHeader :title="globalScope ? '安全审计' : '本租户审计'" :description="globalScope ? '查看各租户及平台的登录与重要管理操作，记录只读。' : '仅显示本租户已核实归属的登录与管理记录，包含操作账号、时间和结果。'">
      <template #actions><Button variant="outline" :disabled="list.loading.value" @click="refresh">刷新</Button></template>
    </PageHeader>
    <p role="status" class="text-sm" :class="archiveError ? 'text-destructive' : 'text-muted-foreground'">{{ archiveState }}</p>
    <div class="grid gap-3 rounded-lg border p-3 sm:grid-cols-2 xl:grid-cols-5">
      <Input v-model="filters.account" placeholder="操作账号" @keyup.enter="list.load(1)" />
      <Input v-if="globalScope" v-model="filters.tenantId" placeholder="租户编号（留空查询全部）" @keyup.enter="list.load(1)" />
      <label class="space-y-1 text-xs text-muted-foreground">日志类别<select v-model="filters.category" class="h-9 w-full rounded-md border bg-background px-3 text-sm"><option value="">全部</option><option value="login">登录记录</option><option value="access">访问与管理操作</option></select></label>
      <Input v-model="filters.operation" placeholder="操作类型" @keyup.enter="list.load(1)" />
      <label class="space-y-1 text-xs text-muted-foreground">开始时间<Input v-model="filters.beginTime" type="datetime-local" /></label>
      <label class="space-y-1 text-xs text-muted-foreground">结束时间<Input v-model="filters.endTime" type="datetime-local" /></label>
      <div class="flex gap-2 sm:col-span-2 xl:col-span-5"><Button size="sm" @click="list.load(1)">查询</Button><Button size="sm" variant="outline" @click="reset">重置</Button></div>
    </div>
    <DataTableShell :loading="list.loading.value" :error="list.error.value" :empty="!list.items.value.length">
      <Table mobile-title="账号" :mobile-columns="['时间', '操作', '结果']">
        <thead><tr class="border-b bg-muted/50 [&>th]:whitespace-nowrap"><th class="p-3 text-left">时间</th><th class="p-3 text-left">账号</th><th v-if="globalScope" class="p-3 text-left">租户</th><th class="p-3 text-left">操作</th><th class="p-3 text-left">目标</th><th class="p-3 text-left">来源 IP</th><th class="p-3 text-left">结果</th><th class="p-3 text-left">请求编号</th></tr></thead>
        <tbody><tr v-for="row in list.items.value" :key="row.eventId" class="border-b text-sm">
          <td class="whitespace-nowrap p-3">{{ date(row.timestamp) }}</td><td class="max-w-52 truncate p-3" :title="row.account">{{ row.account || row.userId || row.clientId || '未认证请求' }}</td><td v-if="globalScope" class="p-3">{{ row.tenantId || '平台' }}</td><td class="whitespace-nowrap p-3">{{ row.operation }}</td><td class="max-w-64 break-all p-3">{{ row.target || '身份认证' }}</td><td class="whitespace-nowrap p-3 font-mono text-xs">{{ row.ip || '—' }}</td><td class="whitespace-nowrap p-3">{{ row.success === true ? '成功' : row.success === false ? '失败' : '结果未知' }}</td><td class="max-w-40 break-all p-3 font-mono text-xs">{{ row.requestId || '—' }}</td>
        </tr></tbody>
      </Table>
    </DataTableShell>
    <PaginationBar :page="list.page.value" :page-size="list.pageSize.value" :total="list.total.value" @change="list.load" />
  </div>
</template>
