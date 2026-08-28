<script setup lang="ts">
import { ExternalLink, FileArchive, MonitorUp, Plus, PowerOff, RefreshCw, RotateCw, Trash2, Upload } from '@lucide/vue'
import { onMounted, ref } from 'vue'
import {
  bigscreenContentUrl,
  cleanBigscreen,
  deleteBigscreen,
  listBigscreens,
  reloadBigscreen,
  uploadBigscreenPackage,
  type BigscreenView,
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
    await uploadBigscreenPackage({
      ...form.value,
      id: editing.value?.id,
      file: packageFile.value,
    })
    dialogOpen.value = false
    feedback.toast.success(editing.value ? '大屏包已更新' : '大屏包已发布')
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
    message: `确定删除“${row.viewName}”及其已部署资源吗？`,
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
    await reloadBigscreen(row.id)
    feedback.toast.success('大屏已从资源包重新加载')
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
            <div class="flex gap-1">
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
