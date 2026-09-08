<script setup lang="ts">
import { ExternalLink, FileArchive, History, MonitorUp, Plus, PowerOff, RefreshCw, RotateCw, Trash2, Upload } from '@lucide/vue'
import { onMounted, ref } from 'vue'
import {
  bigscreenContentUrl,
  bigscreenStorage,
  cleanBigscreen,
  deleteBigscreen,
  listBigscreens,
  reloadBigscreen,
  pruneBigscreen,
  rollbackBigscreen,
  saveBigscreenRetention,
  uploadBigscreenPackage,
  type BigscreenView,
  type BigscreenStorage,
} from '@/api/bigscreen'
import PageHeader from '@/components/PageHeader.vue'
import FormField from '@/components/FormField.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Input from '@/components/ui/Input.vue'
import { useFeedback } from '@/composables/useFeedback'

const feedback = useFeedback()
const rows = ref<BigscreenView[]>([])
const loading = ref(false)
const filterProjectId = ref('')
const dialogOpen = ref(false)
const saving = ref(false)
const actionId = ref('')
const editing = ref<BigscreenView>()
const form = ref({ viewName: '', projectId: '', appId: '' })
const packageFile = ref<File>()
const storageOpen = ref(false)
const storageRow = ref<BigscreenView>()
const storage = ref<BigscreenStorage>()
const storageBusy = ref(false)
const storageError = ref('')
const keepVersions = ref(3)

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 ** 2).toFixed(2)} MB`
}

async function openStorage(row: BigscreenView) {
  if (storageBusy.value) return
  storageRow.value = row
  storage.value = undefined
  storageError.value = ''
  storageOpen.value = true
  storageBusy.value = true
  try {
    storage.value = await bigscreenStorage(row.id)
    keepVersions.value = storage.value.keepVersions
  } catch (error) {
    storageError.value = error instanceof Error ? error.message : '读取版本与空间失败'
  } finally {
    storageBusy.value = false
  }
}

async function manageStorage(action: 'retention' | 'prune' | 'rollback', revisionId?: string) {
  const row = storageRow.value
  const current = storage.value
  if (!row || !current || storageBusy.value) return
  const titles = { retention: '保存保留策略', prune: '清理旧资源', rollback: '回滚大屏内容' }
  const messages = {
    retention: `保留当前版本和最近 ${keepVersions.value - 1} 个历史 ZIP。超出数量的历史版本将立即淘汰，删除后不可恢复。`,
    prune: `保留“${row.viewName}”当前版本和最近 ${current.keepVersions - 1} 个历史 ZIP，归并旧备份并删除更早版本及重复展开目录。淘汰的资源不可恢复，不影响当前大屏和项目绑定。`,
    rollback: '将所选历史包重新发布为一个新版本，保留当前名称、项目绑定与保留策略。当前包会按保留策略进入历史记录。',
  }
  if (!await feedback.confirm({
    title: titles[action], message: messages[action],
    variant: action === 'rollback' ? 'default' : 'destructive',
  })) return
  storageBusy.value = true
  try {
    if (action === 'retention') storage.value = await saveBigscreenRetention(row.id, keepVersions.value)
    if (action === 'prune') storage.value = await pruneBigscreen(row.id)
    if (action === 'rollback' && revisionId) {
      const result = await rollbackBigscreen(row.id, revisionId)
      if (result.retentionWarning) feedback.toast.warning(result.retentionWarning)
      storage.value = await bigscreenStorage(row.id)
      await load()
    }
    keepVersions.value = storage.value!.keepVersions
    feedback.toast.success(action === 'rollback' ? '历史内容已重新发布' : `已完成，释放 ${formatBytes(storage.value!.releasedBytes ?? 0)}`)
  } catch (error) {
    feedback.toast.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    storageBusy.value = false
  }
}

async function load() {
  loading.value = true
  try {
    rows.value = (await listBigscreens(1, 100, filterProjectId.value)).contents ?? []
  } catch (error) {
    feedback.toast.error(error instanceof Error ? error.message : '读取大屏列表失败')
  } finally {
    loading.value = false
  }
}

function openDialog(row?: BigscreenView) {
  editing.value = row
  form.value = {
    viewName: row?.viewName ?? '',
    projectId: row?.projectId ?? filterProjectId.value,
    appId: row?.appId ?? '',
  }
  packageFile.value = undefined
  dialogOpen.value = true
}

function selectPackage(event: Event) {
  packageFile.value = (event.target as HTMLInputElement).files?.[0]
}

async function submit() {
  if (!form.value.viewName.trim() || !form.value.projectId.trim() || !packageFile.value) {
    feedback.toast.warning('请填写大屏名称、项目 ID 并选择 ZIP 包')
    return
  }
  saving.value = true
  try {
    const result = await uploadBigscreenPackage({
      ...form.value,
      id: editing.value?.id,
      file: packageFile.value,
    })
    dialogOpen.value = false
    feedback.toast.success(editing.value ? '大屏包已更新' : '大屏包已发布')
    if (result.retentionWarning) feedback.toast.warning(result.retentionWarning)
    await load()
  } catch (error) {
    feedback.toast.error(error instanceof Error ? error.message : '大屏包发布失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: BigscreenView) {
  if (!await feedback.confirm({
    title: '删除大屏',
    message: `确定删除“${row.viewName}”及其运行资源、资源包和全部受管历史版本吗？删除后不可恢复。`,
    variant: 'destructive',
  })) return
  try {
    await deleteBigscreen(row.id)
    feedback.toast.success('大屏已删除')
    await load()
  } catch (error) {
    feedback.toast.error(error instanceof Error ? error.message : '删除大屏失败')
  }
}

function preview(row: BigscreenView) {
  window.open(bigscreenContentUrl(row.viewUrl, row.version), '_blank', 'noopener,noreferrer')
}

async function reload(row: BigscreenView) {
  actionId.value = row.id
  try {
    const result = await reloadBigscreen(row.id)
    feedback.toast.success('大屏已从资源包重新加载')
    if (result.retentionWarning) feedback.toast.warning(result.retentionWarning)
    await load()
  } catch (error) {
    feedback.toast.error(error instanceof Error ? error.message : '重新加载大屏失败')
  } finally {
    actionId.value = ''
  }
}

async function unload(row: BigscreenView) {
  if (!await feedback.confirm({
    title: '卸载运行资源',
    message: `确定卸载“${row.viewName}”当前展开的运行资源吗？资源包和管理记录会保留，可随时重新加载。`,
  })) return
  actionId.value = row.id
  try {
    await cleanBigscreen(row.id)
    feedback.toast.success('运行资源已卸载，资源包仍保留')
    await load()
  } catch (error) {
    feedback.toast.error(error instanceof Error ? error.message : '卸载大屏失败')
  } finally {
    actionId.value = ''
  }
}

function formatTime(value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="大屏管理" description="JBM 统一管理大屏资源包、版本和项目绑定；业务应用只负责使用。">
      <template #actions>
        <Input v-model="filterProjectId" class="w-52" placeholder="按项目 ID 筛选" @keyup.enter="load" />
        <Button variant="outline" :disabled="loading" @click="load">
          <RefreshCw class="h-4 w-4" :class="loading ? 'animate-spin' : ''" />刷新
        </Button>
        <Button @click="openDialog()"><Plus class="h-4 w-4" />发布大屏包</Button>
      </template>
    </PageHeader>

    <section v-if="rows.length" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
      <article v-for="row in rows" :key="row.id" class="overflow-hidden rounded-lg border bg-card">
        <div class="relative flex aspect-video items-center justify-center overflow-hidden border-b bg-slate-950">
          <span class="absolute inset-4 rounded-lg border border-cyan-400/15" />
          <span class="grid h-16 w-16 place-items-center rounded-2xl border border-cyan-300/25 bg-cyan-400/10 text-cyan-200">
            <MonitorUp class="h-8 w-8" />
          </span>
        </div>
        <div class="p-4">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0"><h2 class="truncate font-semibold" :title="row.viewName">{{ row.viewName }}</h2><p class="mt-1 truncate text-xs text-muted-foreground">{{ row.id }}</p></div>
            <div class="flex shrink-0 flex-col items-end gap-1">
              <Badge variant="outline">{{ row.version || '1.0.0' }}</Badge>
              <Badge :variant="row.deployed ? 'default' : 'secondary'">{{ row.deployed ? '已加载' : '未加载' }}</Badge>
            </div>
          </div>
          <dl class="mt-4 grid grid-cols-2 gap-3 text-xs">
            <div><dt class="text-muted-foreground">项目 ID</dt><dd class="mt-1 truncate" :title="row.projectId">{{ row.projectId || '未绑定' }}</dd></div>
            <div><dt class="text-muted-foreground">应用 ID</dt><dd class="mt-1 truncate" :title="row.appId">{{ row.appId || '当前应用' }}</dd></div>
            <div class="col-span-2"><dt class="text-muted-foreground">更新时间</dt><dd class="mt-1">{{ formatTime(row.updateTime) }}</dd></div>
          </dl>
          <div class="mt-4 grid grid-cols-2 gap-2 border-t pt-3">
            <Button variant="outline" :disabled="!row.deployed" @click="preview(row)"><ExternalLink class="h-4 w-4" />预览</Button>
            <Button variant="outline" :disabled="actionId === row.id || !row.packageAvailable" @click="reload(row)"><RotateCw class="h-4 w-4" :class="actionId === row.id ? 'animate-spin' : ''" />重新加载</Button>
            <Button variant="outline" @click="openDialog(row)"><Upload class="h-4 w-4" />更新包</Button>
            <Button variant="outline" :disabled="storageBusy" @click="openStorage(row)"><History class="h-4 w-4" />版本与空间</Button>
            <div class="col-span-2 flex gap-1">
              <Button class="flex-1" variant="ghost" :disabled="actionId === row.id || !row.deployed" @click="unload(row)"><PowerOff class="h-4 w-4" />卸载</Button>
              <Button size="icon" variant="ghost" class="text-destructive" title="删除大屏" aria-label="删除大屏" @click="remove(row)"><Trash2 class="h-4 w-4" /></Button>
            </div>
          </div>
        </div>
      </article>
    </section>
    <section v-else class="grid min-h-72 place-items-center rounded-lg border border-dashed p-8 text-center">
      <div><MonitorUp class="mx-auto h-10 w-10 text-muted-foreground" /><h2 class="mt-4 font-semibold">{{ loading ? '正在读取大屏…' : '没有符合条件的大屏' }}</h2><p class="mt-2 text-sm text-muted-foreground">由 JBM 发布 ZIP 包并绑定业务项目。</p></div>
    </section>

    <Dialog v-model:open="storageOpen" :title="`版本与空间 · ${storageRow?.viewName ?? ''}`" class="max-w-2xl">
      <p v-if="storageError" class="text-sm text-destructive" role="alert">{{ storageError }}</p>
      <p v-else-if="!storage" class="text-sm text-muted-foreground" role="status">正在读取版本和空间占用…</p>
      <div v-else class="space-y-5" :aria-busy="storageBusy">
        <dl class="grid grid-cols-2 gap-3 rounded-lg bg-muted/40 p-4 text-sm sm:grid-cols-4">
          <div><dt class="text-muted-foreground">总占用</dt><dd class="mt-1 font-semibold">{{ formatBytes(storage.totalBytes) }}</dd></div>
          <div><dt class="text-muted-foreground">当前 {{ storage.currentVersion }}</dt><dd class="mt-1">{{ formatBytes(storage.currentBytes) }}</dd></div>
          <div><dt class="text-muted-foreground">历史 ZIP</dt><dd class="mt-1">{{ formatBytes(storage.historyBytes) }}</dd></div>
          <div><dt class="text-muted-foreground">旧备份</dt><dd class="mt-1">{{ formatBytes(storage.legacyBytes) }}</dd></div>
        </dl>
        <div class="space-y-2">
          <label for="bigscreen-keep-versions" class="text-sm font-medium">保留版本</label>
          <div class="flex flex-wrap items-center gap-2">
            <select id="bigscreen-keep-versions" v-model.number="keepVersions" :disabled="storageBusy" class="h-9 flex-1 rounded-md border bg-background px-3 text-sm">
              <option v-for="count in storage.maxKeepVersions" :key="count" :value="count">{{ count === 1 ? '仅当前版本（不可回滚）' : `当前版本 + 最近 ${count - 1} 个历史版本` }}{{ count === 3 ? '（默认）' : '' }}</option>
            </select>
            <Button :disabled="storageBusy || keepVersions === storage.keepVersions" @click="manageStorage('retention')">保存策略</Button>
          </div>
          <p class="text-xs leading-relaxed text-muted-foreground">发布成功后自动淘汰更早版本；历史版只保留私有 ZIP，不重复保留展开目录。发布失败保留当前版。降低保留数量会立即清理超额历史。</p>
        </div>
        <div class="flex flex-wrap items-center justify-between gap-3 rounded-lg border p-3">
          <div class="text-sm"><p class="font-medium">清理旧资源</p><p class="mt-1 text-xs text-muted-foreground">{{ storage.legacyBackups.length ? `发现 ${storage.legacyBackups.length} 项旧备份，将按当前策略归并淘汰。` : '没有待归并的旧备份，可重新检查超额历史和过期临时文件。' }}</p></div>
          <Button variant="outline" :disabled="storageBusy" @click="manageStorage('prune')"><Trash2 class="h-4 w-4" />立即清理</Button>
        </div>
        <section class="space-y-2">
          <h3 class="text-sm font-semibold">可回滚历史 · {{ storage.history.length }} 个</h3>
          <p v-if="!storage.history.length" class="rounded-lg border border-dashed p-4 text-sm text-muted-foreground">暂无历史包。后续更新时会按保留策略自动留存。</p>
          <div v-for="revision in storage.history" :key="revision.id" class="flex items-center justify-between gap-3 rounded-lg border p-3">
            <div class="min-w-0"><p class="truncate text-sm font-medium">{{ revision.version }}</p><p class="mt-1 text-xs text-muted-foreground">{{ formatTime(revision.createdAt) }} · {{ formatBytes(revision.sizeBytes) }}</p></div>
            <Button variant="outline" :disabled="storageBusy" @click="manageStorage('rollback', revision.id)"><RotateCw class="h-4 w-4" />回滚</Button>
          </div>
        </section>
      </div>
    </Dialog>

    <Dialog v-model:open="dialogOpen" :title="editing ? '更新大屏包' : '发布大屏包'">
      <form class="space-y-4" @submit.prevent="submit">
        <FormField label="大屏名称" required><Input v-model="form.viewName" maxlength="100" placeholder="例如：园区能源运营大屏" /></FormField>
        <FormField label="项目 ID" required><Input v-model="form.projectId" placeholder="业务项目 ID" /></FormField>
        <FormField label="应用 ID"><Input v-model="form.appId" placeholder="留空使用当前登录应用" /></FormField>
        <FormField label="ZIP 资源包" required>
          <label class="flex min-h-24 cursor-pointer flex-col items-center justify-center rounded-lg border border-dashed p-4 text-center hover:bg-muted/40">
            <FileArchive class="h-7 w-7 text-primary" /><span class="mt-2 text-xs">{{ packageFile?.name || '点击选择 ZIP 文件' }}</span>
            <input class="sr-only" type="file" accept=".zip,application/zip" @change="selectPackage" />
          </label>
        </FormField>
        <div class="flex justify-end gap-2 border-t pt-4"><Button variant="outline" :disabled="saving" @click="dialogOpen = false">取消</Button><Button type="submit" :disabled="saving"><Upload class="h-4 w-4" />{{ saving ? '正在部署…' : '发布并部署' }}</Button></div>
      </form>
    </Dialog>
  </div>
</template>
