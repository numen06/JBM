<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Download,
  Eye,
  FileText,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  Settings,
  Trash2,
  Upload,
  X,
} from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import DocTextEditor from '@/components/doc/DocTextEditor.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Badge from '@/components/ui/Badge.vue'
import Dialog from '@/components/ui/Dialog.vue'
import FormField from '@/components/FormField.vue'
import { useFeedback } from '@/composables/useFeedback'
import { usePagedList } from '@/composables/usePagedList'
import {
  deleteDocsByPaths,
  docDownloadUrl,
  getDocBlob,
  getDocText,
  getDocViewUrl,
  listDocs,
  saveDoc,
  saveDocText,
  uploadDoc,
  type BaseDoc,
  type DocListQuery,
} from '@/api/doc'
import { canPreviewEdit, contentLabel, guessDocLanguage, isOfficeDoc, isTextEditable } from '@/utils/docContent'

const router = useRouter()
const feedback = useFeedback()
const filters = ref<DocListQuery>({ keyword: '', docGroup: '', contentType: '', state: '' })
const uploadFile = ref<File | null>(null)
const uploadGroup = ref('')
const uploadLoading = ref(false)
const metadataDialogOpen = ref(false)
const metadataLoading = ref(false)
const editingDoc = ref<BaseDoc | null>(null)
const metadataForm = ref<Partial<BaseDoc>>(emptyMetadataForm())
const previewDoc = ref<BaseDoc | null>(null)
const previewUrl = ref('')
const previewText = ref('')
const previewTextOriginal = ref('')
const previewTextDirty = ref(false)
const previewMode = ref<'view' | 'edit'>('view')
const previewLoading = ref(false)
const previewSaving = ref(false)
const wpsLoading = ref(false)

const docs = usePagedList<BaseDoc>((p, s) => listDocs(p, s, filters.value), 20)

function search() {
  docs.load(1)
}

function resetFilters() {
  filters.value = { keyword: '', docGroup: '', contentType: '', state: '' }
  docs.load(1)
}

function refresh() {
  docs.load(docs.page.value)
}

function onUploadFileChange(event: Event) {
  uploadFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function submitUpload() {
  if (!uploadFile.value) {
    feedback.toast.warning('请选择要上传的文件')
    return
  }
  uploadLoading.value = true
  try {
    const path = await uploadDoc(uploadFile.value, uploadGroup.value)
    feedback.toast.success(`已上传：${path}`)
    uploadFile.value = null
    await docs.load(1)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

function emptyMetadataForm(): Partial<BaseDoc> {
  return {
    docName: '',
    docPath: '',
    docGroup: '',
    state: 'ACTIVE',
    contentType: '',
    expirationTime: '',
  }
}

function openCreateMetadata() {
  editingDoc.value = null
  metadataForm.value = emptyMetadataForm()
  metadataDialogOpen.value = true
}

function openEditMetadata(row: BaseDoc) {
  editingDoc.value = row
  metadataForm.value = {
    docId: row.docId,
    docName: row.docName || '',
    docPath: row.docPath || '',
    docGroup: row.docGroup || '',
    state: row.state || 'ACTIVE',
    contentType: row.contentType || '',
    expirationTime: row.expirationTime || '',
  }
  metadataDialogOpen.value = true
}

async function submitMetadata() {
  const docPath = metadataForm.value.docPath?.trim()
  if (!docPath) {
    feedback.toast.warning('请填写文档路径')
    return
  }
  metadataLoading.value = true
  try {
    await saveDoc({
      ...metadataForm.value,
      docPath,
      docName: metadataForm.value.docName?.trim() || docPath.split('/').filter(Boolean).pop() || docPath,
      docGroup: metadataForm.value.docGroup?.trim() || undefined,
      state: metadataForm.value.state || 'ACTIVE',
      contentType: metadataForm.value.contentType?.trim() || undefined,
      expirationTime: metadataForm.value.expirationTime?.trim() || undefined,
    })
    feedback.toast.success(editingDoc.value ? '文档元数据已更新' : '文档元数据已新增')
    metadataDialogOpen.value = false
    await docs.load(editingDoc.value ? docs.page.value : 1)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '保存文档元数据失败')
  } finally {
    metadataLoading.value = false
  }
}

function clearPreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  previewText.value = ''
  previewTextOriginal.value = ''
  previewTextDirty.value = false
  previewMode.value = 'view'
}

async function openPreview(row: BaseDoc) {
  clearPreview()
  previewDoc.value = row
  if (!row.docPath) return
  previewLoading.value = true
  try {
    if (isTextEditable(row)) {
      const result = await getDocText(row.docPath)
      previewText.value = result.content || ''
      previewTextOriginal.value = previewText.value
      previewDoc.value = result.doc || row
      previewMode.value = 'view'
    } else {
      const blob = await getDocBlob(row.docPath)
      previewUrl.value = URL.createObjectURL(blob)
    }
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '预览失败')
  } finally {
    previewLoading.value = false
  }
}

async function confirmDiscardPreviewChanges() {
  if (!previewTextDirty.value || previewMode.value !== 'edit') return true
  return feedback.confirm({
    title: '放弃未保存的修改',
    message: '当前正文有未保存的修改，确认放弃吗？',
    confirmText: '放弃修改',
    variant: 'destructive',
  })
}

async function closePreview() {
  const canClose = await confirmDiscardPreviewChanges()
  if (!canClose) return
  previewDoc.value = null
  clearPreview()
}

async function handlePreviewDialogOpen(open: boolean) {
  if (open) return
  await closePreview()
}

function enterPreviewEdit() {
  previewTextOriginal.value = previewText.value
  previewTextDirty.value = false
  previewMode.value = 'edit'
}

async function cancelPreviewEdit() {
  if (previewTextDirty.value) {
    const confirmed = await feedback.confirm({
      title: '放弃未保存的修改',
      message: '当前正文有未保存的修改，确认取消编辑吗？',
      confirmText: '放弃修改',
      variant: 'destructive',
    })
    if (!confirmed) return
  }
  previewText.value = previewTextOriginal.value
  previewTextDirty.value = false
  previewMode.value = 'view'
}

async function handlePreviewEdit() {
  if (!previewDoc.value) return
  if (isTextEditable(previewDoc.value)) {
    enterPreviewEdit()
    return
  }
  if (isOfficeDoc(previewDoc.value)) {
    await openWps(previewDoc.value)
  }
}

function onPreviewTextUpdate(value: string) {
  previewText.value = value
  previewTextDirty.value = value !== previewTextOriginal.value
}

async function removeDoc(row: BaseDoc) {
  const path = row.docPath
  if (!path) return
  const confirmed = await feedback.confirm({
    title: '删除文档',
    message: `确认删除「${row.docName || path}」吗？文件内容和元数据都会删除。`,
    confirmText: '删除',
    variant: 'destructive',
  })
  if (!confirmed) return
  try {
    await deleteDocsByPaths([path])
    feedback.toast.success('文档已删除')
    await docs.load(docs.page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function openWps(row: BaseDoc) {
  if (!row.docPath) return
  wpsLoading.value = true
  try {
    const token = await getDocViewUrl(docDownloadUrl(row.docPath))
    if (token.wpsUrl) {
      window.open(token.wpsUrl, '_blank', 'noopener,noreferrer')
    } else {
      feedback.toast.warning('服务未返回 WPS 预览地址')
    }
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '生成 WPS 预览失败')
  } finally {
    wpsLoading.value = false
  }
}

async function openDownload(row: BaseDoc) {
  if (!row.docPath) return
  try {
    const blob = await getDocBlob(row.docPath)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.docName || row.docPath.split('/').filter(Boolean).pop() || 'download'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '下载失败')
  }
}

async function savePreviewText() {
  const docPath = previewDoc.value?.docPath
  if (!docPath) return
  previewSaving.value = true
  try {
    const updated = await saveDocText(docPath, previewText.value)
    previewDoc.value = updated
    previewTextOriginal.value = previewText.value
    previewTextDirty.value = false
    previewMode.value = 'view'
    feedback.toast.success('文本内容已保存')
    await docs.load(docs.page.value)
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '保存文本内容失败')
  } finally {
    previewSaving.value = false
  }
}

function formatSize(value?: number) {
  if (!value) return '-'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

function formatTime(value?: string) {
  if (!value) return '-'
  const time = new Date(value)
  if (Number.isNaN(time.getTime())) return value.replace('T', ' ').slice(0, 19)
  return time.toLocaleString()
}

function docKey(row: BaseDoc, index: number) {
  return row.docId || row.docPath || index
}

onUnmounted(clearPreview)
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="文档管理" description="上传、预览、下载与维护文档元数据，兼容 jbm-cluster-platform-doc 接口。">
      <template #actions>
        <Button variant="outline" @click="router.push({ name: 'document-tools' })">
          <Settings class="h-4 w-4" />
          功能区
        </Button>
        <Button variant="outline" @click="openCreateMetadata">
          <Plus class="h-4 w-4" />
          新增元数据
        </Button>
        <Button variant="outline" :disabled="docs.loading.value || uploadLoading" @click="refresh">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <section class="space-y-4">
      <div class="rounded-lg border p-3">
        <div class="grid gap-3 md:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)_minmax(0,1fr)_160px_auto]">
          <Input v-model="filters.keyword" placeholder="搜索文件名 / 路径" @keyup.enter="search" />
          <Input v-model="filters.docGroup" placeholder="文档分组" @keyup.enter="search" />
          <Input v-model="filters.contentType" placeholder="文件类型" @keyup.enter="search" />
          <Select v-model="filters.state">
            <option value="">全部状态</option>
            <option value="ACTIVE">正常</option>
            <option value="ARCHIVED">归档</option>
            <option value="DISABLED">停用</option>
          </Select>
          <div class="flex gap-2">
            <Button @click="search">
              <Search class="h-4 w-4" />
              搜索
            </Button>
            <Button variant="outline" @click="resetFilters">重置</Button>
          </div>
        </div>
      </div>

      <div class="rounded-lg border p-3">
        <div class="grid gap-3 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)_auto]">
          <input
            type="file"
            class="h-9 rounded-md border border-input px-3 py-1 text-sm"
            @change="onUploadFileChange"
          />
          <Input v-model="uploadGroup" placeholder="上传到分组，可留空" />
          <Button :disabled="uploadLoading" @click="submitUpload">
            <Upload class="h-4 w-4" />
            上传文档
          </Button>
        </div>
        <p class="mt-2 text-xs text-muted-foreground">已选择：{{ uploadFile?.name || '未选择文件' }}</p>
      </div>

      <DataTableShell :loading="docs.loading.value" :error="docs.error.value" :empty="!docs.items.value.length">
        <Table>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-10 px-4 text-left font-medium">文件</th>
              <th class="h-10 px-4 text-left font-medium">分组</th>
              <th class="h-10 px-4 text-left font-medium">类型</th>
              <th class="h-10 px-4 text-left font-medium">状态</th>
              <th class="h-10 px-4 text-left font-medium">大小</th>
              <th class="h-10 px-4 text-left font-medium">更新时间</th>
              <th class="h-10 px-4 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in docs.items.value" :key="docKey(row, index)" class="border-b">
              <td class="max-w-[420px] p-4">
                <div class="flex min-w-0 items-center gap-2">
                  <FileText class="h-4 w-4 shrink-0 text-muted-foreground" />
                  <div class="min-w-0">
                    <div class="truncate font-medium">{{ row.docName || row.docPath || '-' }}</div>
                    <div class="truncate font-mono text-xs text-muted-foreground">{{ row.docPath || '-' }}</div>
                  </div>
                </div>
              </td>
              <td class="p-4 text-sm">{{ row.docGroup || '-' }}</td>
              <td class="p-4"><Badge variant="outline">{{ contentLabel(row) }}</Badge></td>
              <td class="p-4">
                <Badge :variant="row.state === 'ACTIVE' || !row.state ? 'default' : 'secondary'">
                  {{ row.state || 'ACTIVE' }}
                </Badge>
              </td>
              <td class="p-4 text-sm">{{ formatSize(row.size) }}</td>
              <td class="p-4 text-sm text-muted-foreground">{{ formatTime(row.updateTime || row.createTime) }}</td>
              <td class="p-4">
                <div class="flex justify-end gap-2">
                  <Button size="sm" variant="outline" title="预览" @click="openPreview(row)">
                    <Eye class="h-4 w-4" />
                  </Button>
                  <Button size="sm" variant="outline" title="编辑元数据" @click="openEditMetadata(row)">
                    <Pencil class="h-4 w-4" />
                  </Button>
                  <Button size="sm" variant="outline" title="下载" @click="openDownload(row)">
                    <Download class="h-4 w-4" />
                  </Button>
                  <Button size="sm" variant="outline" :disabled="wpsLoading" title="WPS 编辑" @click="openWps(row)">
                    WPS
                  </Button>
                  <Button size="sm" variant="destructive" title="删除" @click="removeDoc(row)">
                    <Trash2 class="h-4 w-4" />
                  </Button>
                </div>
              </td>
            </tr>
          </tbody>
        </Table>
        <PaginationBar
          :page="docs.page.value"
          :total="docs.total.value"
          :page-size="docs.pageSize.value"
          @change="docs.load"
        />
      </DataTableShell>
    </section>

    <Dialog
      :open="!!previewDoc"
      :title="previewDoc?.docName || previewDoc?.docPath || '文档预览'"
      class="max-w-6xl"
      @update:open="handlePreviewDialogOpen"
    >
      <div class="space-y-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div class="min-w-0 break-all font-mono text-xs text-muted-foreground">{{ previewDoc?.docPath }}</div>
          <div class="flex flex-wrap gap-2">
            <template v-if="previewDoc && isTextEditable(previewDoc) && previewMode === 'edit'">
              <Button :disabled="previewSaving || !previewTextDirty" @click="savePreviewText">
                <Save class="h-4 w-4" />
                保存
              </Button>
              <Button variant="outline" :disabled="previewSaving" @click="cancelPreviewEdit">
                <X class="h-4 w-4" />
                取消
              </Button>
            </template>
            <Button
              v-else-if="previewDoc && canPreviewEdit(previewDoc)"
              variant="outline"
              :disabled="wpsLoading || previewLoading"
              @click="handlePreviewEdit"
            >
              <Pencil class="h-4 w-4" />
              编辑
            </Button>
            <Button v-if="previewDoc" variant="outline" @click="openEditMetadata(previewDoc)">
              <Pencil class="h-4 w-4" />
              编辑元数据
            </Button>
            <Button v-if="previewDoc" variant="outline" @click="openDownload(previewDoc)">
              <Download class="h-4 w-4" />
              下载
            </Button>
          </div>
        </div>
        <DocTextEditor
          v-if="previewDoc && isTextEditable(previewDoc)"
          :model-value="previewText"
          :readonly="previewMode === 'view'"
          :language="guessDocLanguage(previewDoc.docPath)"
          :loading="previewLoading"
          @update:model-value="onPreviewTextUpdate"
        />
        <iframe
          v-else-if="previewUrl"
          :src="previewUrl"
          title="文档预览"
          class="h-[68vh] w-full rounded-md border"
        />
        <div v-else class="flex h-[68vh] items-center justify-center rounded-md border text-sm text-muted-foreground">
          {{ previewLoading ? '正在加载预览...' : '暂无可预览文件' }}
        </div>
      </div>
    </Dialog>

    <Dialog
      v-model:open="metadataDialogOpen"
      :title="editingDoc ? '编辑文档元数据' : '新增文档元数据'"
      class="max-w-2xl"
    >
      <div class="space-y-4">
        <div class="grid gap-4 md:grid-cols-2">
          <FormField label="文档名称">
            <Input v-model="metadataForm.docName" placeholder="例如 合同留存.pdf" />
          </FormField>
          <FormField label="文档路径" required>
            <Input v-model="metadataForm.docPath" :disabled="!!editingDoc" placeholder="例如 legacy/manual.pdf" />
          </FormField>
          <FormField label="文档分组">
            <Input v-model="metadataForm.docGroup" placeholder="例如 legacy" />
          </FormField>
          <FormField label="状态">
            <Select v-model="metadataForm.state">
              <option value="ACTIVE">正常</option>
              <option value="ARCHIVED">归档</option>
              <option value="DISABLED">停用</option>
            </Select>
          </FormField>
          <FormField label="文件类型">
            <Input v-model="metadataForm.contentType" placeholder="例如 application/pdf" />
          </FormField>
          <FormField label="过期时间">
            <Input v-model="metadataForm.expirationTime" placeholder="例如 2026-12-31T23:59:59" />
          </FormField>
        </div>
        <div class="flex justify-end gap-2">
          <Button variant="outline" @click="metadataDialogOpen = false">取消</Button>
          <Button :disabled="metadataLoading" @click="submitMetadata">保存</Button>
        </div>
      </div>
    </Dialog>
  </div>
</template>
