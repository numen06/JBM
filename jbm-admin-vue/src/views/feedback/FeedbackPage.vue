<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { MessageSquare, RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { get, post, put, unwrap } from '@/api/request'

const props = defineProps<{ manage?: boolean }>()
const route = useRoute()
type Feedback = { id: string; username: string; tenantId: string; appId: string; feedbackType: string; title: string; description: string; contact: string; sourcePage: string; status: string; handleNote: string; revision: number; createdAt: string }
const context = ref<{ canManage: boolean; deploymentMode: string }>()
const loading = ref(true), busy = ref(false), error = ref(''), notice = ref(''), receipt = ref('')
const title = ref(''), feedbackType = ref('suggestion'), description = ref(''), contact = ref('')
const status = ref(''), page = ref(1), pageSize = ref(20), total = ref(0), rows = ref<Feedback[]>([]), selected = ref<Feedback>()
const handleStatus = ref('pending'), handleNote = ref('')
const labels: Record<string, string> = { suggestion: '功能建议', problem: '问题反馈', other: '其他意见', pending: '待处理', processing: '处理中', resolved: '已处理' }
const base = computed(() => route.path.replace(/\/manage\/?$/, '').replace(/\/$/, ''))
const time = (value: string) => new Date(value).toLocaleString('zh-CN', { hour12: false })
const message = (reason: unknown) => reason instanceof Error ? reason.message : '请求失败，请稍后重试'

async function load() {
  loading.value = true; error.value = ''; rows.value = []; selected.value = undefined
  try {
    context.value = unwrap(await get<{ canManage: boolean; deploymentMode: string }>('/feedback/context'))
    if (props.manage) {
      if (!context.value?.canManage) throw new Error('仅平台运营或超级管理员可以查看和处理反馈')
      const result = unwrap(await get<{ items: Feedback[]; total: number }>('/feedback', { params: { page: page.value, size: pageSize.value, ...(status.value ? { status: status.value } : {}) } }))
      rows.value = result.items; total.value = result.total
    }
  } catch (reason) { error.value = message(reason) }
  finally { loading.value = false }
}

async function submit() {
  if (busy.value) return
  error.value = ''; notice.value = ''; busy.value = true
  try {
    const sourcePage = typeof route.query.from === 'string' && route.query.from.startsWith('/') && !route.query.from.startsWith('//') ? route.query.from.split(/[?#]/)[0] : ''
    const result = unwrap(await post<{ id: string }>('/feedback/submit', { title: title.value.trim(), feedbackType: feedbackType.value, description: description.value.trim(), contact: contact.value.trim(), sourcePage }))
    receipt.value = result.id
    title.value = ''; description.value = ''; contact.value = ''
    notice.value = '提交成功，平台运营方已收到你的反馈。'
  } catch (reason) { error.value = message(reason) }
  finally { busy.value = false }
}

async function open(row: Feedback) {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    selected.value = unwrap(await get<Feedback>(`/feedback/${row.id}`))
    handleStatus.value = selected.value.status; handleNote.value = selected.value.handleNote
  } catch (reason) { error.value = message(reason) }
  finally { busy.value = false }
}

async function save() {
  if (!selected.value || busy.value) return
  busy.value = true; error.value = ''; notice.value = ''
  try {
    await put(`/feedback/${selected.value.id}`, { status: handleStatus.value, handleNote: handleNote.value.trim(), revision: selected.value.revision })
    await load(); notice.value = '处理结果已保存。'
  } catch (reason) { error.value = message(reason) }
  finally { busy.value = false }
}

onMounted(load)
watch(() => props.manage, load)
</script>

<template>
  <main class="mx-auto w-full max-w-5xl space-y-5">
    <PageHeader :title="manage ? '意见反馈管理' : '意见反馈'" :description="manage ? '集中查看用户意见并记录处理结果。' : '告诉我们你的建议，或描述使用中遇到的问题。'">
      <template #actions>
        <RouterLink v-if="!manage && context?.canManage" :to="`${base}/manage`"><Button variant="outline">反馈管理</Button></RouterLink>
        <RouterLink v-if="manage" :to="base"><Button variant="outline"><MessageSquare class="h-4 w-4" />提交反馈</Button></RouterLink>
      </template>
    </PageHeader>
    <p v-if="loading" role="status" class="py-8 text-center text-sm text-muted-foreground">正在读取…</p>
    <p v-if="error" role="alert" class="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">{{ error }} <Button v-if="!context || manage" variant="outline" size="sm" @click="load">重试</Button></p>
    <div v-if="notice" role="status" class="rounded-lg border border-primary/30 bg-primary/5 p-4 text-sm">
      <p>{{ notice }}</p><p v-if="!manage && receipt" class="mt-2 break-all text-xs text-muted-foreground">反馈编号：{{ receipt }}</p>
    </div>
    <template v-if="context && !loading">
      <p class="text-sm text-muted-foreground">{{ context.deploymentMode === 'local' ? '反馈由本地部署的平台运营方接收和处理，保存在当前部署中。' : '反馈由当前云平台运营方统一接收和处理。' }}仅平台运营和超级管理员可以查看反馈内容。</p>
      <form v-if="!manage" class="space-y-5 rounded-xl border bg-card p-5 sm:p-6" @submit.prevent="submit">
        <FormField label="反馈类型"><Select v-model="feedbackType" :disabled="busy"><option value="suggestion">功能建议</option><option value="problem">问题反馈</option><option value="other">其他意见</option></Select></FormField>
        <FormField label="标题" required><Input v-model="title" required maxlength="120" placeholder="用一句话描述你的意见" :disabled="busy" /></FormField>
        <FormField label="反馈内容" required><textarea v-model="description" required maxlength="5000" rows="7" placeholder="请描述你的建议，或遇到问题时的操作和现象" :disabled="busy" class="w-full rounded-md border border-input bg-background p-3 text-sm" /><p class="text-right text-xs text-muted-foreground">{{ description.length }}/5000</p></FormField>
        <FormField label="联系方式（选填）"><Input v-model="contact" maxlength="200" placeholder="手机号或邮箱，方便运营方联系你" :disabled="busy" /></FormField>
        <Button type="submit" :disabled="busy || !title.trim() || !description.trim()">{{ busy ? '正在提交…' : '提交反馈' }}</Button>
      </form>
      <template v-else-if="context.canManage">
        <div class="flex flex-wrap items-center gap-3"><Select v-model="status" class="w-36" :disabled="busy" @change="page = 1; load()"><option value="">全部状态</option><option value="pending">待处理</option><option value="processing">处理中</option><option value="resolved">已处理</option></Select><Button variant="outline" :disabled="busy" @click="load"><RefreshCw class="h-4 w-4" />刷新</Button><span class="text-sm text-muted-foreground">共 {{ total }} 条</span></div>
        <div v-if="selected" class="space-y-4 rounded-xl border bg-card p-5">
          <div class="flex items-start justify-between gap-3"><h2 class="break-words font-semibold">{{ selected.title }}</h2><Button variant="outline" size="sm" :disabled="busy" @click="selected = undefined">收起详情</Button></div>
          <p class="text-xs text-muted-foreground">{{ selected.username }} · 租户 {{ selected.tenantId }} · 应用 {{ selected.appId }} · {{ time(selected.createdAt) }}</p>
          <p class="whitespace-pre-wrap break-words text-sm">{{ selected.description }}</p>
          <p class="break-all text-sm">联系方式：{{ selected.contact || '未填写' }}</p><p v-if="selected.sourcePage" class="break-all text-xs text-muted-foreground">来源页面：{{ selected.sourcePage }}</p>
          <form class="space-y-4 border-t pt-4" @submit.prevent="save"><FormField label="处理状态"><Select v-model="handleStatus" :disabled="busy"><option value="pending">待处理</option><option value="processing">处理中</option><option value="resolved">已处理</option></Select></FormField><FormField label="处理备注"><textarea v-model="handleNote" rows="3" maxlength="2000" :disabled="busy" class="w-full rounded-md border border-input bg-background p-3 text-sm" /></FormField><Button type="submit" :disabled="busy">{{ busy ? '正在保存…' : '保存处理结果' }}</Button></form>
        </div>
        <p v-if="!rows.length" class="rounded-xl border py-12 text-center text-sm text-muted-foreground">暂无反馈</p>
        <div v-else class="divide-y rounded-xl border bg-card"><button v-for="row in rows" :key="row.id" class="block w-full space-y-2 p-4 text-left transition hover:bg-muted/50 disabled:opacity-60" :disabled="busy" @click="open(row)"><div class="flex items-start justify-between gap-3"><span class="break-words font-medium">{{ row.title }}</span><span class="shrink-0 rounded border px-2 py-0.5 text-xs">{{ labels[row.status] }}</span></div><p class="text-xs text-muted-foreground">{{ labels[row.feedbackType] }} · {{ row.username }} · {{ time(row.createdAt) }}</p></button></div>
        <PaginationBar :page="page" :total="total" :page-size="pageSize" @change="(nextPage, nextSize) => { page = nextPage; pageSize = nextSize || pageSize; load() }" />
      </template>
    </template>
  </main>
</template>
