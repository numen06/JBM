<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { Database, Archive, CalendarClock, History, RotateCcw, Plus, RefreshCw, Download, Pencil, Trash2, ArrowLeft } from '@lucide/vue'
import PageHeader from '@/components/PageHeader.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import FormField from '@/components/FormField.vue'
import Table from '@/components/ui/Table.vue'
import { usePermission } from '@/composables/usePermission'
import { useFeedback } from '@/composables/useFeedback'
import { listDatabaseSources, saveDatabaseSource, testDatabaseSource, discoverDatabases, attachDatabase, saveBackupPolicy, startDatabaseBackup, listDatabaseTasks, startDatabaseRestore, deleteDatabaseBackup, downloadDatabaseBackup, type DatabaseSource, type DatabaseTask, type BackupPolicy } from '@/api/databaseBackups'

const { isPlatformAdmin } = usePermission()
const feedback = useFeedback()
const detailTabs = [
  { id: 'sources', label: '数据库信息', icon: Database },
  { id: 'policies', label: '备份策略', icon: CalendarClock },
  { id: 'backups', label: '备份记录', icon: Archive },
  { id: 'restores', label: '还原记录', icon: History },
] as const
const listTabs = [
  { id: 'sources', label: '数据库列表', icon: Database },
  { id: 'backups', label: '备份记录', icon: Archive },
  { id: 'restores', label: '还原记录', icon: History },
] as const
const active = ref<(typeof detailTabs)[number]['id']>('sources')
const sources = ref<DatabaseSource[]>([]), tasks = ref<DatabaseTask[]>([])
const detailSourceId = ref('')
const detailSource = computed(() => sources.value.find(source => source.id === detailSourceId.value))
const displayedSources = computed(() => detailSource.value ? [detailSource.value] : [])
const tabs = computed(() => detailSourceId.value ? detailTabs : listTabs)
const recordSourceId = ref('')
const taskSourceId = computed(() => detailSourceId.value || recordSourceId.value)
const loading = ref(true), busy = ref(''), error = ref(''), page = ref(1), total = ref(0)
const sourceOpen = ref(false), policyOpen = ref(false), restoreOpen = ref(false), discoverOpen = ref(false)
const availableDatabases = ref<Array<{ database: string; tableCount: number; connected: boolean }>>([]), checkedDatabases = ref<string[]>([])
const discoverSource = ref<DatabaseSource>()
const selectedSource = ref<DatabaseSource>(), selectedBackup = ref<DatabaseTask>()
const connection = ref({ id: '', name: '', host: '', port: 3306, database: '', username: '', password: '', sslMode: 'PREFERRED' })
const policy = ref<BackupPolicy>({ enabled: true, frequency: 'daily', time: '02:00', weekday: 0, retentionDays: 14 })
const restore = ref({ mode: 'new', targetDatabase: '', confirmation: '', maintenanceConfirmed: false })
const taskKind = computed(() => active.value === 'restores' ? 'restore' : 'backup')
let timer: ReturnType<typeof setInterval> | undefined
let refreshing = false
let revision = 0
const labels: Record<string, string> = { queued: '排队中', running: '执行中', success: '成功', failed: '失败', deleted: '已删除' }
const time = (value?: string | null) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
const size = (value?: number) => value == null ? '—' : value < 1048576 ? `${(value / 1024).toFixed(1)} KB` : `${(value / 1048576).toFixed(1)} MB`
const message = (cause: unknown) => cause instanceof Error ? cause.message : '操作失败'
const weekdays = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日']
const policyText = (value: BackupPolicy) => `${value.frequency === 'weekly' ? `每${weekdays[value.weekday]}` : '每天'} ${value.time}（北京时间） · 保留 ${value.retentionDays} 天`

async function load() {
  if (!isPlatformAdmin.value || refreshing) { loading.value = false; return }
  refreshing = true
  const kind = taskKind.value, requestedPage = page.value, sourceId = taskSourceId.value, requestedRevision = revision
  try {
    const [dbs, records] = await Promise.all([listDatabaseSources(), listDatabaseTasks(kind, requestedPage, sourceId)])
    if (requestedRevision !== revision) return
    sources.value = dbs
    if (recordSourceId.value && !dbs.some(source => source.id === recordSourceId.value)) recordSourceId.value = ''
    if (kind === taskKind.value && requestedPage === page.value && sourceId === taskSourceId.value) {
      tasks.value = records.items
      total.value = records.total
    }
    error.value = ''
  } catch (cause) { error.value = message(cause) }
  finally {
    loading.value = false; refreshing = false
    if (!busy.value && (requestedRevision !== revision || kind !== taskKind.value || requestedPage !== page.value || sourceId !== taskSourceId.value)) void load()
  }
}
async function act(key: string, action: () => Promise<void>) {
  if (busy.value) return
  busy.value = key
  revision++
  try { await action() } catch (cause) { feedback.toast.error(message(cause)) }
  finally { revision++; busy.value = ''; await load() }
}
async function backup(source: DatabaseSource) {
  await act(`backup:${source.id}`, async () => {
    await startDatabaseBackup(source.id)
    enterDetail(source, 'backups'); page.value = 1
    await load()
    feedback.toast.success('已创建备份任务')
  })
}
async function test(source: DatabaseSource) {
  await act(`test:${source.id}`, async () => {
    const result = await testDatabaseSource(source.id)
    await load()
    if (result.status !== 'online') throw new Error(result.error || '连接失败')
    feedback.toast.success(`连接正常，${result.tableCount} 张表`)
  })
}
function editSource(source?: DatabaseSource) {
  connection.value = source ? { id: source.id, name: source.name, host: source.host, port: source.port, database: source.database, username: source.username, password: '', sslMode: source.sslMode } : { id: '', name: '', host: '', port: 3306, database: '', username: '', password: '', sslMode: 'PREFERRED' }
  sourceOpen.value = true
}
async function discover(source: DatabaseSource) {
  await act(`discover:${source.id}`, async () => {
    discoverSource.value = source
    checkedDatabases.value = []
    availableDatabases.value = await discoverDatabases(source.id)
    discoverOpen.value = true
  })
}
async function attachSelected() {
  if (!discoverSource.value || !checkedDatabases.value.length) return
  await act('attach', async () => {
    for (const database of [...checkedDatabases.value]) {
      const result = await attachDatabase(discoverSource.value!.id, database)
      availableDatabases.value = await discoverDatabases(discoverSource.value!.id)
      checkedDatabases.value = checkedDatabases.value.filter(name => name !== database)
      await load()
      if (result.status !== 'online') throw new Error(`${database}：${result.error || '连接测试失败'}`)
    }
    discoverOpen.value = false
    await load()
    feedback.toast.success('所选数据库已接入')
  })
}
async function saveSource() {
  await act('save-source', async () => {
    const result = await saveDatabaseSource({ ...connection.value, id: connection.value.id || undefined })
    sourceOpen.value = false
    await load()
    if (result.status !== 'online') feedback.toast.warning(result.error || '连接未通过测试，请检查配置')
    else feedback.toast.success('数据库已接入')
  })
}
function editPolicy(source: DatabaseSource) {
  selectedSource.value = source
  policy.value = { ...source.policy }
  policyOpen.value = true
}
async function togglePolicy(source: DatabaseSource) {
  await act(`policy:${source.id}`, async () => {
    const result = await saveBackupPolicy(source.id, { ...source.policy, enabled: !source.policy.enabled })
    source.policy = result
    await load()
    feedback.toast.success(result.enabled ? '定时备份已启用' : '定时备份已暂停')
  })
}
function enterDetail(source: DatabaseSource, tab: (typeof detailTabs)[number]['id'] = 'sources') {
  detailSourceId.value = source.id
  active.value = tab
}
function leaveDetail() {
  detailSourceId.value = ''
  active.value = 'sources'
}
function showRecords(source: DatabaseSource) {
  enterDetail(source, 'backups')
}
async function savePolicy() {
  if (!selectedSource.value) return
  await act('save-policy', async () => {
    await saveBackupPolicy(selectedSource.value!.id, { ...policy.value })
    policyOpen.value = false; await load()
    feedback.toast.success(policy.value.enabled ? '定时备份已启用' : '定时备份已暂停')
  })
}
function openRestore(task: DatabaseTask) {
  selectedBackup.value = task
  restore.value = { mode: 'new', targetDatabase: `${task.database.slice(0, 35)}_restore_${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`, confirmation: '', maintenanceConfirmed: false }
  restoreOpen.value = true
}
watch(() => restore.value.mode, mode => {
  if (selectedBackup.value) restore.value.targetDatabase = mode === 'replace' ? selectedBackup.value.database : `${selectedBackup.value.database.slice(0, 35)}_restore_${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`
})
async function submitRestore() {
  if (!selectedBackup.value) return
  await act('restore', async () => {
    await startDatabaseRestore({ backupId: selectedBackup.value!.id, ...restore.value })
    restoreOpen.value = false; active.value = 'restores'; page.value = 1; await load()
    feedback.toast.success('已创建还原任务')
  })
}
async function remove(task: DatabaseTask) {
  if (!await feedback.confirm({ title: '删除备份', message: `删除 ${task.filename || task.database}？`, variant: 'destructive' })) return
  await act(`delete:${task.id}`, async () => { await deleteDatabaseBackup(task.id); await load(); feedback.toast.success('备份已删除') })
}
async function details(task: DatabaseTask) {
  await feedback.alert({ title: '任务详情', message: [task.error, task.sha256 ? `SHA-256：${task.sha256}` : '', task.verifiedTables != null ? `已核对 ${task.verifiedTables} 张表` : '', task.safetyBackupId ? `还原前备份：${task.safetyBackupId}` : '', `任务编号：${task.id}`].filter(Boolean).join('\n') })
}
watch([active, taskSourceId], () => { tasks.value = []; total.value = 0; page.value = 1; void load() })
watch(page, () => void load())
onMounted(() => { void load(); timer = setInterval(() => void load(), 4000) })
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div v-if="isPlatformAdmin" class="space-y-5 database-maintenance">
    <PageHeader :title="detailSourceId ? '数据库详情' : '数据库运维'">
      <template #actions>
        <Button v-if="detailSourceId" variant="outline" @click="leaveDetail"><ArrowLeft class="h-4 w-4" />返回数据库列表</Button>
        <Button variant="outline" :disabled="!!busy || loading" @click="load"><RefreshCw class="h-4 w-4" />刷新</Button>
        <Button v-if="!detailSourceId" @click="editSource()"><Plus class="h-4 w-4" />接入数据库</Button>
      </template>
    </PageHeader>
    <p v-if="error" class="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive" role="alert">{{ error }}</p>
    <p v-if="detailSource" class="text-sm text-muted-foreground">{{ detailSource.name }} · {{ detailSource.databaseType }} · <span class="font-mono">{{ detailSource.host }}:{{ detailSource.port }} / {{ detailSource.database }}</span></p>
    <nav class="ops-tabs" aria-label="数据库运维功能"><button v-for="tab in tabs" :key="tab.id" type="button" :aria-current="active === tab.id ? 'page' : undefined" :class="{ selected: active === tab.id }" @click="active = tab.id"><component :is="tab.icon" class="h-4 w-4" />{{ tab.label }}</button></nav>
    <p v-if="loading" class="p-8 text-center text-sm text-muted-foreground">加载中…</p>
    <section v-else-if="active === 'sources' && !detailSourceId" aria-label="数据库列表">
      <Card class="overflow-hidden"><div class="overflow-x-auto"><Table><thead><tr><th>数据库</th><th>类型</th><th>版本</th><th>连接状态</th><th>定时备份</th><th class="text-right">操作</th></tr></thead><tbody><tr v-for="source in sources" :key="source.id"><td><button type="button" class="font-semibold text-primary hover:underline" @click="enterDetail(source)">{{ source.name }}</button><Badge v-if="source.isDefault" variant="outline" class="ml-2">系统默认</Badge><p class="mt-1 break-all font-mono text-xs text-muted-foreground">{{ source.host }}:{{ source.port }}</p><p class="mt-1 font-mono text-xs text-muted-foreground">{{ source.database }}</p></td><td>{{ source.databaseType }}</td><td>{{ source.version || '—' }}</td><td><Badge class="whitespace-nowrap" :variant="source.status === 'online' ? 'default' : source.status === 'error' ? 'destructive' : 'secondary'">{{ source.status === 'online' ? '连接正常' : source.status === 'error' ? '连接失败' : '未测试' }}</Badge></td><td><div class="flex items-center gap-2"><button type="button" role="switch" :aria-checked="source.policy.enabled" :aria-label="`${source.name}定时备份`" :disabled="!!busy" class="ops-switch" @click="togglePolicy(source)"><span /></button><span class="whitespace-nowrap text-xs">{{ source.policy.enabled ? '已启用' : '未启用' }}</span></div><p class="mt-2 whitespace-nowrap text-xs text-muted-foreground">{{ policyText(source.policy) }}</p></td><td><div class="flex justify-end gap-2"><Button variant="outline" size="sm" @click="showRecords(source)">备份记录</Button><Button size="sm" @click="enterDetail(source)">详情</Button></div></td></tr></tbody></Table></div><p v-if="!sources.length && !error" class="ops-empty">暂无数据库</p></Card>
    </section>
    <section v-else-if="active === 'sources'" class="space-y-4" aria-label="数据库信息">
      <Card v-for="source in displayedSources" :key="source.id" role="article" :aria-label="`${source.name}数据库`" class="overflow-hidden">
        <div class="ops-heading"><div class="flex items-center gap-3"><span class="ops-icon"><Database class="h-5 w-5" /></span><div><h2 class="font-semibold">{{ source.name }}</h2><p class="mt-1 font-mono text-xs text-muted-foreground">{{ source.host }}:{{ source.port }} / {{ source.database }}</p></div></div><div class="flex gap-2"><Badge v-if="source.isDefault" variant="outline">系统默认</Badge><Badge :variant="source.status === 'online' ? 'default' : source.status === 'error' ? 'destructive' : 'secondary'">{{ source.status === 'online' ? '连接正常' : source.status === 'error' ? '连接失败' : '未测试' }}</Badge></div></div>
        <dl class="ops-details"><div><dt>数据库类型</dt><dd>{{ source.databaseType }}</dd></div><div><dt>数据库版本</dt><dd>{{ source.version || '—' }}</dd></div><div><dt>数据表</dt><dd>{{ source.tableCount ?? '—' }}</dd></div><div><dt>最近成功备份</dt><dd>{{ time(source.lastBackupAt) }}</dd></div><div><dt>下次定时备份</dt><dd>{{ source.policy.enabled ? time(source.policy.nextRunAt) : '未启用' }}</dd></div></dl>
        <p v-if="source.error" class="break-all px-5 pb-4 text-sm text-destructive">{{ source.error }}</p>
        <div class="ops-policy"><div><p class="text-sm font-medium">定时备份</p><p class="mt-1 text-xs text-muted-foreground">{{ policyText(source.policy) }}</p></div><div class="flex items-center gap-3"><span class="text-sm" :class="source.policy.enabled ? 'text-primary' : 'text-muted-foreground'">{{ source.policy.enabled ? '已启用' : '未启用' }}</span><button type="button" role="switch" :aria-checked="source.policy.enabled" :aria-label="`${source.name}定时备份`" :disabled="!!busy" class="ops-switch" @click="togglePolicy(source)"><span /></button></div></div>
        <div class="ops-footer"><span class="text-xs text-muted-foreground">{{ source.username }} · {{ source.sslMode === 'REQUIRED' ? 'TLS 加密' : '自动协商 TLS' }}</span><div class="flex flex-wrap gap-2"><Button variant="outline" size="sm" :disabled="!!busy" @click="discover(source)">同服务器接入</Button><Button v-if="!source.isDefault" variant="outline" size="sm" :disabled="!!busy" @click="editSource(source)"><Pencil class="h-3.5 w-3.5" />编辑</Button><Button variant="outline" size="sm" :disabled="!!busy" @click="test(source)">{{ busy === `test:${source.id}` ? '测试中…' : '测试连接' }}</Button><Button variant="outline" size="sm" :disabled="!!busy" @click="editPolicy(source)"><CalendarClock class="h-3.5 w-3.5" />策略设置</Button><Button variant="outline" size="sm" @click="showRecords(source)">备份记录</Button><Button size="sm" :disabled="!!busy" @click="backup(source)"><Archive class="h-3.5 w-3.5" />{{ busy === `backup:${source.id}` ? '提交中…' : '立即备份' }}</Button></div></div>
      </Card>
      <p v-if="!sources.length && !error" class="ops-empty">暂无数据库</p>
    </section>
    <section v-else-if="active === 'policies'" class="space-y-4" aria-label="备份策略">
      <Card v-for="source in displayedSources" :key="source.id" class="ops-heading"><div><h2 class="font-semibold">{{ source.name }}</h2><p class="mt-2 text-sm text-muted-foreground">{{ policyText(source.policy) }}</p><p class="mt-2 text-xs text-muted-foreground">下次执行：{{ time(source.policy.nextRunAt) }}</p></div><div class="flex items-center gap-3"><span class="text-sm" :class="source.policy.enabled ? 'text-primary' : 'text-muted-foreground'">{{ source.policy.enabled ? '已启用' : '未启用' }}</span><button type="button" role="switch" :aria-checked="source.policy.enabled" :aria-label="`${source.name}定时备份`" :disabled="!!busy" class="ops-switch" @click="togglePolicy(source)"><span /></button><Button variant="outline" :disabled="!!busy" @click="editPolicy(source)">调整策略</Button></div></Card>
    </section>
    <section v-else :aria-label="active === 'backups' ? '备份记录' : '还原记录'">
      <div v-if="!detailSourceId" class="ops-record-filters" role="group" aria-label="按数据库筛选"><span class="text-sm text-muted-foreground">数据库</span><Button size="sm" :variant="!recordSourceId ? 'default' : 'outline'" :aria-pressed="!recordSourceId" @click="recordSourceId = ''">全部数据库</Button><Button v-for="source in sources" :key="source.id" size="sm" :variant="recordSourceId === source.id ? 'default' : 'outline'" :aria-pressed="recordSourceId === source.id" :title="`${source.host}:${source.port} / ${source.database}`" @click="recordSourceId = source.id">{{ source.name }}</Button></div>
      <Card class="overflow-hidden"><div class="overflow-x-auto"><Table><thead><tr><th>数据库</th><th>时间</th><th>{{ active === 'backups' ? '来源' : '目标库' }}</th><th>状态</th><th>{{ active === 'backups' ? '文件大小' : '完成时间' }}</th><th class="text-right">操作</th></tr></thead><tbody><tr v-for="task in tasks" :key="task.id"><td><div class="font-medium">{{ task.sourceName }}</div><div class="mt-1 font-mono text-xs text-muted-foreground">{{ task.database }}</div></td><td class="whitespace-nowrap">{{ time(task.createdAt) }}</td><td>{{ active === 'backups' ? task.origin === 'scheduled' ? '定时备份' : task.origin === 'before_restore' ? '还原前备份' : '手动备份' : task.targetDatabase }}</td><td><Badge :variant="task.status === 'failed' ? 'destructive' : task.status === 'success' ? 'default' : 'secondary'">{{ labels[task.status] || task.status }}</Badge></td><td class="whitespace-nowrap">{{ active === 'backups' ? size(task.size) : time(task.finishedAt) }}</td><td><div class="flex justify-end gap-1"><Button variant="ghost" size="sm" @click="details(task)">{{ task.status === 'failed' ? '查看原因' : '详情' }}</Button><template v-if="active === 'backups' && task.status === 'success'"><Button variant="outline" size="sm" :disabled="!!busy" @click="act(`download:${task.id}`, () => downloadDatabaseBackup(task))"><Download class="h-3.5 w-3.5" />下载</Button><Button size="sm" :disabled="!!busy" @click="openRestore(task)"><RotateCcw class="h-3.5 w-3.5" />还原</Button><Button v-if="task.origin !== 'before_restore'" variant="ghost" size="sm" :disabled="!!busy" aria-label="删除备份" @click="remove(task)"><Trash2 class="h-3.5 w-3.5" /></Button></template></div></td></tr></tbody></Table></div><p v-if="!tasks.length && !error" class="ops-empty">{{ taskSourceId ? '该数据库' : '' }}{{ active === 'backups' ? '暂无备份记录' : '暂无还原记录' }}</p></Card>
      <div class="mt-3 flex items-center justify-end gap-3 text-sm"><span>共 {{ total }} 条 · 第 {{ page }} 页</span><Button variant="outline" size="sm" :disabled="page <= 1" @click="page--">上一页</Button><Button variant="outline" size="sm" :disabled="page * 20 >= total" @click="page++">下一页</Button></div>
    </section>

    <Dialog :open="discoverOpen" title="接入同服务器数据库" @update:open="discoverOpen = $event"><div class="ops-form"><p class="text-sm text-muted-foreground">{{ discoverSource?.host }}:{{ discoverSource?.port }}</p><label v-for="item in availableDatabases" :key="item.database" class="flex items-center gap-3 rounded-md border p-3 text-sm"><input v-model="checkedDatabases" type="checkbox" :value="item.database" :disabled="item.connected" /><span class="flex-1 font-mono">{{ item.database }}</span><span class="text-xs text-muted-foreground">{{ item.tableCount }} 张表</span><Badge v-if="item.connected" variant="secondary">已接入</Badge></label><p v-if="!availableDatabases.length" class="text-sm text-muted-foreground">没有其他可访问的数据库</p><div class="ops-actions"><Button variant="outline" @click="discoverOpen = false">取消</Button><Button :disabled="!!busy || !checkedDatabases.length" @click="attachSelected">接入所选数据库</Button></div></div></Dialog>
    <Dialog :open="sourceOpen" :title="connection.id ? '编辑数据库' : '接入数据库'" @update:open="sourceOpen = $event"><form class="ops-form" @submit.prevent="saveSource"><FormField label="连接名称"><Input v-model="connection.name" aria-label="连接名称" required maxlength="80" /></FormField><FormField label="数据库类型"><Input model-value="MySQL" readonly /></FormField><div class="grid grid-cols-2 gap-3"><FormField label="主机地址"><Input v-model="connection.host" aria-label="主机地址" required /></FormField><FormField label="端口"><Input v-model.number="connection.port" aria-label="端口" type="number" min="1" max="65535" required /></FormField></div><FormField label="数据库名"><Input v-model="connection.database" aria-label="数据库名" required maxlength="64" /></FormField><FormField label="用户名"><Input v-model="connection.username" aria-label="用户名" required autocomplete="off" /></FormField><FormField :label="connection.id ? '密码（留空保持原密码）' : '密码'"><Input v-model="connection.password" aria-label="密码" type="password" autocomplete="new-password" /></FormField><FormField label="连接加密"><Select v-model="connection.sslMode" aria-label="连接加密"><option value="PREFERRED">自动协商 TLS</option><option value="REQUIRED">必须使用 TLS</option></Select></FormField><div class="ops-actions"><Button variant="outline" @click="sourceOpen = false">取消</Button><Button type="submit" :disabled="!!busy">保存并测试</Button></div></form></Dialog>
    <Dialog :open="policyOpen" title="定时备份" @update:open="policyOpen = $event"><form class="ops-form" @submit.prevent="savePolicy"><p class="font-medium">{{ selectedSource?.name }}</p><label class="flex items-center gap-2 text-sm"><input v-model="policy.enabled" type="checkbox" />启用定时备份</label><FormField label="执行周期"><Select v-model="policy.frequency" aria-label="执行周期"><option value="daily">每天</option><option value="weekly">每周</option></Select></FormField><FormField v-if="policy.frequency === 'weekly'" label="执行日期"><Select v-model.number="policy.weekday" aria-label="执行日期"><option v-for="(day, index) in weekdays" :key="day" :value="index">{{ day }}</option></Select></FormField><FormField label="执行时间（北京时间）"><Input v-model="policy.time" aria-label="执行时间" type="time" required /></FormField><FormField label="保留天数"><Input v-model.number="policy.retentionDays" aria-label="保留天数" type="number" min="1" max="365" required /></FormField><div class="ops-actions"><Button variant="outline" @click="policyOpen = false">取消</Button><Button type="submit" :disabled="!!busy">保存</Button></div></form></Dialog>
    <Dialog :open="restoreOpen" title="还原数据库" @update:open="restoreOpen = $event"><form class="ops-form" @submit.prevent="submitRestore"><p class="break-all text-sm">备份：{{ selectedBackup?.filename }}<br />备份时间：{{ time(selectedBackup?.createdAt) }}</p><FormField label="还原方式"><Select v-model="restore.mode" aria-label="还原方式"><option value="new">还原到新库</option><option value="replace">覆盖原库</option></Select></FormField><FormField label="目标数据库名"><Input v-model="restore.targetDatabase" aria-label="目标数据库名" :readonly="restore.mode === 'replace'" maxlength="64" required /></FormField><template v-if="restore.mode === 'replace'"><p class="rounded-md bg-destructive/10 p-3 text-sm text-destructive">原库数据将被备份时的数据覆盖。系统会先生成一份还原前备份。</p><label class="flex items-start gap-2 text-sm"><input v-model="restore.maintenanceConfirmed" type="checkbox" required class="mt-1" />已停止目标数据库的所有业务写入，并进入维护窗口</label><FormField :label="`输入原库名 ${selectedBackup?.database} 确认`"><Input v-model="restore.confirmation" aria-label="确认原库名" required autocomplete="off" /></FormField></template><div class="ops-actions"><Button variant="outline" @click="restoreOpen = false">取消</Button><Button type="submit" :variant="restore.mode === 'replace' ? 'destructive' : 'default'" :disabled="!!busy || (restore.mode === 'replace' && (!restore.maintenanceConfirmed || restore.confirmation !== selectedBackup?.database))">提交还原</Button></div></form></Dialog>
  </div>
  <Card v-else class="ops-empty" role="alert">仅平台运维管理员可访问数据库运维</Card>
</template>

<style scoped>
.ops-record-filters{display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:16px}.ops-policy{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:16px 20px;border-top:1px solid hsl(var(--border))}.ops-switch{position:relative;flex-shrink:0;width:40px;height:24px;border-radius:9999px;background:hsl(var(--muted-foreground)/.35);transition:background .15s}.ops-switch[aria-checked="true"]{background:hsl(var(--primary))}.ops-switch span{display:block;width:18px;height:18px;border-radius:50%;background:white;transform:translateX(3px);transition:transform .15s}.ops-switch[aria-checked="true"] span{transform:translateX(19px)}.ops-switch:focus-visible{outline:2px solid hsl(var(--ring));outline-offset:3px}.ops-switch:disabled{opacity:.5;cursor:wait}.ops-tabs{display:flex;gap:8px;border-bottom:1px solid hsl(var(--border));overflow-x:auto}.ops-tabs button{display:flex;align-items:center;gap:8px;white-space:nowrap;padding:12px 16px;border-bottom:2px solid transparent;font-size:14px;color:hsl(var(--muted-foreground))}.ops-tabs button.selected{border-bottom-color:hsl(var(--primary));color:hsl(var(--primary));font-weight:600}.ops-tabs button:focus-visible{outline:2px solid hsl(var(--ring));outline-offset:-3px}.ops-heading,.ops-footer{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:20px;flex-wrap:wrap}.ops-icon{display:flex;align-items:center;justify-content:center;width:44px;height:44px;border-radius:10px;background:hsl(var(--primary)/.1);color:hsl(var(--primary))}.ops-details{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:20px;padding:4px 20px 24px}.ops-details dt{font-size:12px;color:hsl(var(--muted-foreground))}.ops-details dd{margin-top:8px;font-size:13px;overflow-wrap:anywhere}.ops-footer{border-top:1px solid hsl(var(--border));background:hsl(var(--muted)/.2)}.ops-empty{padding:48px 24px;text-align:center;font-size:14px;color:hsl(var(--muted-foreground))}.ops-form{display:flex;flex-direction:column;gap:16px;max-height:75vh;overflow-y:auto;padding:2px}.ops-actions{display:flex;justify-content:flex-end;gap:8px;padding-top:16px;border-top:1px solid hsl(var(--border))}th,td{padding:14px 16px;text-align:left;font-size:13px}th{color:hsl(var(--muted-foreground));font-weight:500;white-space:nowrap}tr{border-bottom:1px solid hsl(var(--border))}tbody tr:last-child{border-bottom:0}@media(max-width:640px){.ops-details{grid-template-columns:repeat(2,minmax(0,1fr))}.ops-tabs button{padding:12px}.ops-footer>div{width:100%}}
</style>
