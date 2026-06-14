<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Database, Download, FolderPlus, RefreshCw, Upload } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import { useFeedback } from '@/composables/useFeedback'
import {
  createTempDocGroup,
  getDocBlob,
  listGroupItemsByToken,
  syncStorageDocs,
  uploadDocByToken,
  type BaseDoc,
  type BaseDocGroup,
  type DocSyncStorageResult,
} from '@/api/doc'

const router = useRouter()
const feedback = useFeedback()

const syncPrefix = ref('')
const syncLoading = ref(false)
const syncResult = ref<DocSyncStorageResult | null>(null)
const groupName = ref('')
const groupToken = ref('')
const currentGroup = ref<BaseDocGroup | null>(null)
const groupFile = ref<File | null>(null)
const groupItems = ref<BaseDoc[]>([])
const groupLoading = ref(false)

function onGroupFileChange(event: Event) {
  groupFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function syncStorage() {
  syncLoading.value = true
  try {
    syncResult.value = await syncStorageDocs(syncPrefix.value)
    feedback.toast.success(
      `同步完成：新增 ${syncResult.value.created}，跳过 ${syncResult.value.skipped}，失败 ${syncResult.value.failed}`,
    )
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '同步留存文档失败')
  } finally {
    syncLoading.value = false
  }
}

async function createGroup() {
  groupLoading.value = true
  try {
    currentGroup.value = await createTempDocGroup({
      docGroupName: groupName.value.trim() || '临时文档组',
    })
    groupToken.value = currentGroup.value.tokenKey || ''
    groupItems.value = []
    feedback.toast.success('临时文档组已创建')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '创建文档组失败')
  } finally {
    groupLoading.value = false
  }
}

async function submitGroupUpload() {
  if (!groupToken.value.trim()) {
    feedback.toast.warning('请先创建或输入文档组 Token')
    return
  }
  if (!groupFile.value) {
    feedback.toast.warning('请选择组内文件')
    return
  }
  groupLoading.value = true
  try {
    await uploadDocByToken(groupFile.value, groupToken.value.trim())
    groupFile.value = null
    await loadGroupItems()
    feedback.toast.success('组内文件已上传')
  } catch (e) {
    feedback.toast.error(e instanceof Error ? e.message : '组内上传失败')
  } finally {
    groupLoading.value = false
  }
}

async function loadGroupItems() {
  if (!groupToken.value.trim()) {
    feedback.toast.warning('请输入文档组 Token')
    return
  }
  groupLoading.value = true
  try {
    groupItems.value = await listGroupItemsByToken(groupToken.value.trim())
  } catch (e) {
    groupItems.value = []
    feedback.toast.error(e instanceof Error ? e.message : '查询组内文件失败')
  } finally {
    groupLoading.value = false
  }
}

async function downloadItem(row: BaseDoc) {
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
</script>

<template>
  <div class="space-y-4">
    <PageHeader title="文档功能区" description="留存文档同步、临时文档组、Token 上传与组内文件管理。">
      <template #actions>
        <Button variant="outline" @click="router.push({ name: 'documents' })">
          <ArrowLeft class="h-4 w-4" />
          返回列表
        </Button>
        <Button variant="outline" :disabled="syncLoading || groupLoading" @click="loadGroupItems">
          <RefreshCw class="h-4 w-4" />
          刷新组文件
        </Button>
      </template>
    </PageHeader>

    <div class="grid gap-4 xl:grid-cols-[420px_minmax(0,1fr)]">
      <section class="space-y-4">
        <div class="rounded-lg border p-4">
          <div class="mb-3 flex items-center gap-2">
            <Database class="h-4 w-4" />
            <h2 class="font-semibold">留存文档同步</h2>
          </div>
          <div class="space-y-3">
            <Input v-model="syncPrefix" placeholder="对象前缀，可留空" @keyup.enter="syncStorage" />
            <Button class="w-full" :disabled="syncLoading" @click="syncStorage">
              <Database class="h-4 w-4" />
              扫描 MinIO / 本地存储
            </Button>
          </div>
          <div v-if="syncResult" class="mt-3 grid grid-cols-4 gap-2 text-center text-sm">
            <div class="rounded-md bg-muted p-2">
              <div class="font-semibold">{{ syncResult.scanned }}</div>
              <div class="text-xs text-muted-foreground">扫描</div>
            </div>
            <div class="rounded-md bg-muted p-2">
              <div class="font-semibold">{{ syncResult.created }}</div>
              <div class="text-xs text-muted-foreground">新增</div>
            </div>
            <div class="rounded-md bg-muted p-2">
              <div class="font-semibold">{{ syncResult.skipped }}</div>
              <div class="text-xs text-muted-foreground">跳过</div>
            </div>
            <div class="rounded-md bg-muted p-2">
              <div class="font-semibold">{{ syncResult.failed }}</div>
              <div class="text-xs text-muted-foreground">失败</div>
            </div>
          </div>
          <div v-if="syncResult" class="mt-3 rounded-md bg-muted p-3 text-xs text-muted-foreground">
            <div>存储：{{ syncResult.backend || 'unknown' }}</div>
            <div v-if="syncResult.bucket">Bucket：{{ syncResult.bucket }}</div>
            <div v-if="syncResult.endpointUrl" class="break-all">Endpoint：{{ syncResult.endpointUrl }}</div>
            <div v-if="syncResult.localDir" class="break-all">目录：{{ syncResult.localDir }}</div>
          </div>
        </div>

        <div class="rounded-lg border p-4">
          <div class="mb-3 flex items-center gap-2">
            <FolderPlus class="h-4 w-4" />
            <h2 class="font-semibold">临时文档组</h2>
          </div>
          <div class="space-y-3">
            <Input v-model="groupName" placeholder="分组名称" />
            <Button class="w-full" :disabled="groupLoading" @click="createGroup">创建分组并生成 Token</Button>
            <Input v-model="groupToken" placeholder="Doc-Token-Key" />
            <Button class="w-full" variant="outline" :disabled="groupLoading" @click="loadGroupItems">查询组内文件</Button>
          </div>
          <div v-if="currentGroup" class="mt-3 rounded-md bg-muted p-3 text-sm">
            <div>分组：{{ currentGroup.docGroupName || currentGroup.groupPath }}</div>
            <div class="mt-1 break-all font-mono text-xs text-muted-foreground">{{ currentGroup.groupPath }}</div>
          </div>
        </div>

        <div class="rounded-lg border p-4">
          <div class="mb-3 font-semibold">Token 上传</div>
          <div class="space-y-3">
            <input
              type="file"
              class="h-9 w-full rounded-md border border-input px-3 py-1 text-sm"
              @change="onGroupFileChange"
            />
            <Button class="w-full" :disabled="groupLoading" @click="submitGroupUpload">
              <Upload class="h-4 w-4" />
              上传到文档组
            </Button>
          </div>
          <p class="mt-2 text-xs text-muted-foreground">已选择：{{ groupFile?.name || '未选择文件' }}</p>
        </div>
      </section>

      <section class="rounded-lg border p-4">
        <div class="mb-4 font-semibold">组内文件</div>
        <div v-if="groupItems.length" class="divide-y rounded-md border">
          <div v-for="item in groupItems" :key="item.docId || item.docPath" class="flex items-center gap-3 p-3">
            <div class="min-w-0 flex-1">
              <div class="truncate font-medium">{{ item.docName || item.docPath }}</div>
              <div class="truncate font-mono text-xs text-muted-foreground">{{ item.docPath }}</div>
            </div>
            <Button size="sm" variant="outline" @click="downloadItem(item)">
              <Download class="h-4 w-4" />
              下载
            </Button>
          </div>
        </div>
        <div v-else class="text-sm text-muted-foreground">暂无组内文件</div>
      </section>
    </div>
  </div>
</template>
