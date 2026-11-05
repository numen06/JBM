<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Table from '@/components/ui/Table.vue'
import Dialog from '@/components/ui/Dialog.vue'
import { usePagedList } from '@/composables/usePagedList'
import { get, post, unwrap } from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { isPlatformAdministrator } from '@/lib/adminAccess'
import { auditQueryBody, type AuditScope } from '@/lib/auditScope'
import { auditAction, auditChanges, auditModule, auditObject } from '@/lib/auditPresentation'
const props = withDefaults(defineProps<{ scope?: AuditScope; category?: string; title?: string }>(), { scope: 'auto' })
const auth = useAuthStore()
const globalScope = computed(() => props.scope !== 'current' && (isPlatformAdministrator(auth.user) || auth.user?.roles?.some(role => role.roleCode === 'security_auditor')))
const queryBody = (body: Record<string, unknown>) => auditQueryBody(body, props.scope, auth.activeTenantId)
const archiveState = ref('正在检查归档状态…')
const archiveError = ref(false)
interface AuditEvent { eventId: string; timestamp: number; account?: string; userId?: string; clientId?: string; tenantId?: string; operation?: string; operationName?: string; serviceId?: string; target?: string; ip?: string; requestId?: string; operationId?: string; phase?: string; operationStatus?: string; message?: string; submittedChanges?: unknown; businessRef?: Record<string, unknown>; expectedTasks?: number; businessRevision?: number; completionKind?: string; resourceIds?: unknown; success?: boolean; integrityVerified?: boolean }
const detailOpen = ref(false), detailLoading = ref(false), detailError = ref('')
const detailRows = ref<AuditEvent[]>([]), selected = ref<AuditEvent | null>(null)
const detailIncomplete = ref(false)
const detailSummary = computed(() => {
  if (detailIncomplete.value) return '记录较多，当前显示部分结果；请通过关联业务任务核对完整执行情况。'
  const expectedIds = new Set(detailRows.value.flatMap(row => Array.isArray(row.businessRef?.taskIds) ? row.businessRef.taskIds.map(String) : []))
  const expected = Math.max(expectedIds.size, ...detailRows.value.map(row => row.expectedTasks || 0))
  if (expected) {
    const results = new Map<string, AuditEvent>()
    for (const row of detailRows.value) if (row.phase === 'RESULT' && row.businessRef?.taskId != null && (!expectedIds.size || expectedIds.has(String(row.businessRef.taskId)))) results.set(String(row.businessRef.taskId), row)
    if (results.size < expected) return `已收到 ${results.size}/${expected} 项完成结果，尚未齐全；可刷新或查看关联业务任务。`
    const values = [...results.values()], failed = values.filter(row => row.operationStatus === 'FAILED').length, unknown = values.filter(row => row.operationStatus === 'UNKNOWN').length
    return failed ? `任务已完成，其中 ${failed} 项失败` : unknown ? `其中 ${unknown} 项结果待核查` : '全部任务已成功'
  }
  const terminal = [...detailRows.value].reverse().find(row => row.phase === 'RESULT')
  if (terminal) return `最终结果：${outcome(terminal)}`
  if (detailRows.value.length && detailRows.value.every(row => row.phase === 'LEGACY')) return '旧记录未保留最终执行结果，按原始信息归档。'
  if (detailRows.value.some(row => row.phase === 'ACCEPTED')) return '任务已受理，等待执行结果；可通过关联编号继续追查。'
  const start = detailRows.value.find(row => row.phase === 'START')
  return start && Date.now() - start.timestamp > 30000 ? '尚未收到完成记录，请核查实际业务状态，避免重复操作。' : '操作记录已保存，完成结果将在此继续显示。'
})
function outcome(row: AuditEvent) { return ({ STARTED: '已开始', ACCEPTED: '已受理', COMMITTED: '业务已提交', SUCCEEDED: '成功', FAILED: '失败', WRITTEN: '设备已应答', UNKNOWN: '结果待核查' } as Record<string, string>)[row.operationStatus || ''] || (row.success === true ? '成功' : row.success === false ? '失败' : '结果未知') }
function format(value: unknown) { return JSON.stringify(value, null, 2) }
async function details(row: AuditEvent) {
  selected.value = row; detailOpen.value = true; detailLoading.value = true; detailError.value = ''; detailIncomplete.value = false
  try {
    if (!row.operationId) { detailRows.value = [row]; return }
    const range = { beginTime: new Date(row.timestamp - 86400000).toISOString(), endTime: new Date(Math.min(Date.now() + 1000, row.timestamp + 29 * 86400000)).toISOString(), size: 100 }
    const result = unwrap(await post<{ records: AuditEvent[]; total: number }>('/logs/securityAudit/query', queryBody({ operationId: row.operationId, ...range })))
    detailIncomplete.value = result.total > result.records.length
    const taskIds = [...new Set(result.records.flatMap(item => Array.isArray(item.businessRef?.taskIds) ? item.businessRef.taskIds.map(String) : []))].slice(0, 100)
    const relatedResult = taskIds.length ? unwrap(await post<{ records: AuditEvent[]; total: number }>('/logs/securityAudit/query', queryBody({ businessTaskIds: taskIds, ...range }))) : null
    const related = relatedResult?.records || []
    detailIncomplete.value ||= !!relatedResult && relatedResult.total > related.length
    detailRows.value = [...new Map([...result.records, ...related].map(item => [item.eventId, item])).values()].sort((a, b) => a.timestamp - b.timestamp || (a.businessRevision || 0) - (b.businessRevision || 0))
  } catch (error) { detailError.value = error instanceof Error ? error.message : '读取操作详情失败' }
  finally { detailLoading.value = false }
}
const filters = ref({ account: '', tenantId: '', category: props.category || 'operation', operation: '', success: '', beginTime: '', endTime: '' })
const list = usePagedList<AuditEvent>(async (page, size) => {
  const { tenantId, success, operation, ...values } = filters.value
  const result = unwrap(await post<{ records: AuditEvent[]; total: number }>('/logs/securityAudit/query', queryBody({ ...Object.fromEntries(Object.entries(values).filter(([, value]) => value)), ...(globalScope.value && tenantId ? { tenantId } : {}), ...(operation ? { [values.category === 'access' ? 'method' : 'operation']: operation } : {}), ...(success ? { success: success === 'true' } : {}), page, size })))
  return { contents: result.records, total: result.total }
})
function reset() { filters.value = { account: '', tenantId: '', category: props.category || 'operation', operation: '', success: '', beginTime: '', endTime: '' }; list.load(1) }
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
    <PageHeader :title="props.title || (globalScope ? '全局审计' : '审计日志')" :description="globalScope ? '查看全部范围的操作人员、业务对象、操作内容和执行结果，记录只读。' : '查看当前业务范围内的操作人员、业务对象、操作内容和执行结果，记录只读。'">
      <template #actions><Button variant="outline" :disabled="list.loading.value" @click="refresh">刷新</Button></template>
    </PageHeader>
    <p role="status" class="text-sm" :class="archiveError ? 'text-destructive' : 'text-muted-foreground'">{{ archiveState }}</p>
    <div class="grid gap-3 rounded-lg border p-3 sm:grid-cols-2 xl:grid-cols-5">
      <Input v-model="filters.account" aria-label="操作人员" placeholder="操作人员账号" @keyup.enter="list.load(1)" />
      <Input v-if="globalScope" v-model="filters.tenantId" placeholder="范围编号（留空查询全部）" @keyup.enter="list.load(1)" />
      <label v-if="!props.category" class="space-y-1 text-xs text-muted-foreground">日志类别<select v-model="filters.category" aria-label="日志类别" class="h-9 w-full rounded-md border bg-background px-3 text-sm"><option value="">全部</option><option value="operation">业务操作</option><option value="login">登录记录</option><option value="access">接口访问</option><option value="legacy-record">旧记录</option></select></label>
      <label class="space-y-1 text-xs text-muted-foreground">操作结果<select v-model="filters.success" aria-label="操作结果" class="h-9 w-full rounded-md border bg-background px-3 text-sm"><option value="">全部结果</option><option value="true">成功</option><option value="false">失败</option></select></label>
      <label class="space-y-1 text-xs text-muted-foreground">开始时间<Input v-model="filters.beginTime" type="datetime-local" /></label>
      <label class="space-y-1 text-xs text-muted-foreground">结束时间<Input v-model="filters.endTime" type="datetime-local" /></label>
      <div class="flex gap-2 sm:col-span-2 xl:col-span-5"><Button size="sm" @click="list.load(1)">查询</Button><Button size="sm" variant="outline" @click="reset">重置</Button></div>
    </div>
    <DataTableShell :loading="list.loading.value" :error="list.error.value" :empty="!list.items.value.length">
      <Table mobile-title="操作人员" :mobile-columns="['操作时间', '业务对象', '操作内容', '操作结果']">
        <thead><tr class="border-b bg-muted/50 [&>th]:whitespace-nowrap"><th class="p-3 text-left">操作时间</th><th class="p-3 text-left">操作人员</th><th class="p-3 text-left">业务模块</th><th v-if="globalScope" class="p-3 text-left">业务范围</th><th class="p-3 text-left">业务对象</th><th class="p-3 text-left">操作内容</th><th class="p-3 text-left">操作结果</th><th class="p-3 text-left">来源 IP</th><th class="p-3 text-left">详情</th></tr></thead>
        <tbody><tr v-for="row in list.items.value" :key="row.eventId" class="border-b text-sm">
          <td class="whitespace-nowrap p-3">{{ date(row.timestamp) }}</td><td class="max-w-40 truncate p-3" :title="row.account">{{ row.account || row.userId || row.clientId || '未认证请求' }}</td><td class="whitespace-nowrap p-3">{{ auditModule(row) }}</td><td v-if="globalScope" class="max-w-32 break-all p-3 text-xs">{{ row.tenantId || '平台' }}</td><td class="max-w-52 break-words p-3" :title="row.target">{{ auditObject(row) }}</td><td class="max-w-64 break-words p-3"><div>{{ auditAction(row) }}</div><p v-if="auditChanges(row)" class="mt-1 text-xs text-muted-foreground">{{ auditChanges(row) }}</p></td><td class="whitespace-nowrap p-3">{{ outcome(row) }}</td><td class="whitespace-nowrap p-3 font-mono text-xs">{{ row.ip || '—' }}</td><td class="p-3"><Button size="sm" variant="ghost" @click="details(row)">查看</Button></td>
        </tr></tbody>
      </Table>
    </DataTableShell>
    <PaginationBar :page="list.page.value" :page-size="list.pageSize.value" :total="list.total.value" @change="list.load" />
    <Dialog :open="detailOpen" title="操作详情" @update:open="detailOpen = $event">
      <div class="space-y-4 text-sm">
        <dl v-if="selected" class="grid gap-3 rounded-lg border p-3 sm:grid-cols-2">
          <div><dt class="text-xs text-muted-foreground">操作人员</dt><dd>{{ selected.account || selected.userId || selected.clientId || '未认证请求' }}</dd></div>
          <div><dt class="text-xs text-muted-foreground">操作时间</dt><dd>{{ date(selected.timestamp) }}</dd></div>
          <div><dt class="text-xs text-muted-foreground">业务模块</dt><dd>{{ auditModule(selected) }}</dd></div>
          <div><dt class="text-xs text-muted-foreground">业务对象</dt><dd class="break-all">{{ auditObject(selected) }}</dd></div>
          <div class="sm:col-span-2"><dt class="text-xs text-muted-foreground">请求路径</dt><dd class="break-all font-mono text-xs">{{ selected.target || '身份认证' }}</dd></div>
          <div class="sm:col-span-2"><dt class="text-xs text-muted-foreground">请求编号</dt><dd class="break-all font-mono text-xs">{{ selected.requestId || '—' }}</dd></div>
        </dl>
        <p role="status" class="rounded-lg bg-muted p-3">{{ detailSummary }}</p>
        <p v-if="detailError" role="alert" class="text-destructive">{{ detailError }}</p>
        <p v-if="detailLoading" role="status">正在读取记录…</p>
        <article v-for="row in detailRows" :key="row.eventId" class="space-y-2 rounded-lg border p-3">
          <div class="flex flex-wrap justify-between gap-2"><span>{{ date(row.timestamp) }}</span><strong>{{ row.phase === 'START' ? '操作开始' : row.phase === 'WRITE_ACK' ? '写入应答' : outcome(row) }}</strong></div>
          <p v-if="row.message">{{ row.message }}</p>
          <p v-if="row.operationStatus === 'ACCEPTED'" class="text-muted-foreground">任务已受理，执行结果会继续记录；此时尚不能视为执行成功。</p>
          <p v-if="row.operationStatus === 'UNKNOWN'" class="text-muted-foreground">未能确认最终结果，请先核对业务当前状态，避免重复操作。</p>
          <div v-if="row.submittedChanges"><p class="text-muted-foreground">提交内容</p><pre class="mt-1 max-h-56 overflow-auto whitespace-pre-wrap break-all rounded bg-muted p-2 text-xs">{{ format(row.submittedChanges) }}</pre></div>
          <div v-if="row.businessRef && Object.keys(row.businessRef).length"><p class="text-muted-foreground">关联业务记录</p><pre class="mt-1 whitespace-pre-wrap break-all text-xs">{{ format(row.businessRef) }}</pre></div>
          <p class="break-all text-xs text-muted-foreground">{{ row.operationId || row.requestId }} · {{ row.integrityVerified ? '完整性已验证' : '未验证' }}</p>
        </article>
        <div class="flex justify-end"><Button variant="outline" :disabled="detailLoading" @click="selected && details(selected)">刷新结果</Button></div>
      </div>
    </Dialog>
  </div>
</template>
