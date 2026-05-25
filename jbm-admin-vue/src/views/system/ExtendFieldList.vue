<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowDown, ArrowUp, Plus, RefreshCw, Search, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Label from '@/components/ui/Label.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Badge from '@/components/ui/Badge.vue'
import {
  getExtendFormFromDb,
  pageExtendForms,
  listFieldDefinitions,
  publishExtendForm,
  saveExtendForm,
} from '@/api/extendField'
import type { ExtendFormDefinition, FieldDefinition } from '@/api/types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

const FIELD_TYPES = [
  { value: 'string', label: '字符串' },
  { value: 'number', label: '数字' },
  { value: 'date', label: '日期' },
] as const

const forms = ref<ExtendFormDefinition[]>([])
const selectedCode = ref('')
const formCodeInput = ref('')
const formName = ref('')
const customFormId = ref<string>('')
const autoPublish = ref(true)
const fields = ref<FieldDefinition[]>([])
const meta = ref<ExtendFormDefinition | null>(null)

const listLoading = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const successMsg = ref('')
const keyword = ref('')
const groupPage = ref(1)
const groupTotal = ref(0)
const groupPageSize = DEFAULT_PAGE_SIZE

const redisDialogOpen = ref(false)
const redisFields = ref<FieldDefinition[]>([])
const redisLoading = ref(false)

function emptyField(): FieldDefinition {
  return {
    fieldName: '',
    fieldType: 'string',
    fieldLabel: '',
    required: false,
    sortable: false,
    queryable: false,
  }
}

function applyDefinition(def: ExtendFormDefinition) {
  meta.value = def
  selectedCode.value = def.formCode
  formCodeInput.value = def.formCode
  formName.value = def.formName ?? ''
  customFormId.value = def.customFormId != null ? String(def.customFormId) : ''
  fields.value =
    def.fields?.length > 0
      ? def.fields.map((f) => ({ ...emptyField(), ...f }))
      : [emptyField()]
}

async function loadGroups(page = groupPage.value, selectFirst = false) {
  listLoading.value = true
  error.value = ''
  try {
    const data = await pageExtendForms(page, groupPageSize, keyword.value.trim() || undefined)
    forms.value = data.contents ?? []
    groupTotal.value = data.total ?? 0
    groupPage.value = page
    if (selectedCode.value) {
      const current = forms.value.find((f) => f.formCode === selectedCode.value)
      if (current) applyDefinition(current)
    } else if (selectFirst && forms.value.length) {
      applyDefinition(forms.value[0])
    }
  } catch (e) {
    forms.value = []
    groupTotal.value = 0
    error.value = e instanceof Error ? e.message : '加载字段组失败'
  } finally {
    listLoading.value = false
  }
}

function refreshForms(selectFirst = false) {
  return loadGroups(groupPage.value, selectFirst)
}

function searchGroups() {
  loadGroups(1, false)
}

async function selectForm(form: ExtendFormDefinition) {
  if (!form.formCode) return
  loading.value = true
  error.value = ''
  successMsg.value = ''
  try {
    const def = await getExtendFormFromDb(form.formCode)
    applyDefinition(def)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载字段定义失败'
  } finally {
    loading.value = false
  }
}

async function loadFromDb() {
  const code = formCodeInput.value.trim()
  if (!code) {
    error.value = '请输入表单编码 formCode'
    return
  }
  loading.value = true
  error.value = ''
  successMsg.value = ''
  try {
    const def = await getExtendFormFromDb(code)
    applyDefinition(def)
    successMsg.value = `已加载表单「${code}」`
    await loadGroups(1)
  } catch (e) {
    meta.value = null
    selectedCode.value = ''
    formName.value = ''
    customFormId.value = ''
    fields.value = [emptyField()]
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function startNewForm() {
  const code = formCodeInput.value.trim()
  if (!code) {
    error.value = '请先输入表单编码 formCode'
    return
  }
  meta.value = null
  selectedCode.value = code
  formName.value = ''
  customFormId.value = ''
  fields.value = [emptyField()]
  error.value = ''
  successMsg.value = `新建表单「${code}」，编辑字段后保存`
}

function addField() {
  fields.value.push(emptyField())
}

function removeField(index: number) {
  if (fields.value.length <= 1) {
    fields.value = [emptyField()]
    return
  }
  fields.value.splice(index, 1)
}

function moveField(index: number, delta: number) {
  const next = index + delta
  if (next < 0 || next >= fields.value.length) return
  const list = [...fields.value]
  const [item] = list.splice(index, 1)
  list.splice(next, 0, item)
  fields.value = list
}

function buildRequest() {
  const normalized = fields.value
    .filter((f) => f.fieldName?.trim() || f.fieldLabel?.trim())
    .map((f) => ({
      fieldName: f.fieldName.trim(),
      fieldType: f.fieldType || 'string',
      fieldLabel: f.fieldLabel.trim() || f.fieldName.trim(),
      required: !!f.required,
      sortable: !!f.sortable,
      queryable: !!f.queryable,
      defaultValue: f.defaultValue === '' ? undefined : f.defaultValue,
      options: f.options,
    }))

  if (!normalized.length) {
    throw new Error('请至少配置一个有效字段（字段名不能为空）')
  }

  return {
    formName: formName.value.trim() || undefined,
    fields: normalized,
    customFormId: customFormId.value ? Number(customFormId.value) : undefined,
    autoPublish: autoPublish.value,
  }
}

async function handleSave() {
  const code = formCodeInput.value.trim()
  if (!code) {
    error.value = '请输入表单编码 formCode'
    return
  }
  saving.value = true
  error.value = ''
  successMsg.value = ''
  try {
    const def = await saveExtendForm(code, buildRequest())
    applyDefinition(def)
    successMsg.value = autoPublish.value
      ? `已保存并发布到 Redis（版本 ${def.version ?? '-'}）`
      : `已保存到数据库（版本 ${def.version ?? '-'}）`
    await refreshForms()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handlePublishOnly() {
  const code = formCodeInput.value.trim()
  if (!code) {
    error.value = '请输入表单编码 formCode'
    return
  }
  saving.value = true
  error.value = ''
  successMsg.value = ''
  try {
    await publishExtendForm(code)
    successMsg.value = `已从库重新发布「${code}」到 Redis`
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发布失败'
  } finally {
    saving.value = false
  }
}

async function showRedisDefinitions() {
  const code = formCodeInput.value.trim()
  if (!code) {
    error.value = '请输入表单编码 formCode'
    return
  }
  redisDialogOpen.value = true
  redisLoading.value = true
  redisFields.value = []
  try {
    redisFields.value = await listFieldDefinitions(code)
  } catch (e) {
    redisFields.value = []
    error.value = e instanceof Error ? e.message : '读取 Redis 失败'
    redisDialogOpen.value = false
  } finally {
    redisLoading.value = false
  }
}

function formatTime(t?: string) {
  if (!t) return '-'
  try {
    return new Date(t).toLocaleString()
  } catch {
    return t
  }
}

onMounted(() => loadGroups(1, true))
</script>

<template>
  <div>
    <PageHeader
      title="扩展字段管理"
      description="字段组 + 字段定义，数据库为真源，按需发布到 Redis"
    >
      <template #actions>
        <Button variant="outline" :disabled="listLoading || loading" @click="refreshForms(true)">
          <RefreshCw class="mr-1 h-4 w-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <div class="flex flex-col gap-4 lg:flex-row">
      <aside class="flex w-full shrink-0 flex-col rounded-lg border bg-card lg:w-80">
        <div class="flex items-center justify-between border-b px-3 py-2">
          <span class="text-sm font-medium">字段组</span>
          <Badge variant="secondary">{{ groupTotal }}</Badge>
        </div>
        <div class="flex gap-1 border-b px-3 py-2">
          <Input
            v-model="keyword"
            placeholder="编码/名称"
            class="h-8 flex-1 text-sm"
            @keyup.enter="searchGroups"
          />
          <Button variant="outline" size="sm" class="h-8 px-2" @click="searchGroups">
            <Search class="h-3.5 w-3.5" />
          </Button>
        </div>
        <div class="min-h-[280px] flex-1 overflow-y-auto p-2">
          <p v-if="listLoading" class="px-2 py-4 text-sm text-muted-foreground">加载中...</p>
          <p v-else-if="!forms.length" class="px-2 py-4 text-sm text-muted-foreground">
            {{ keyword ? '无匹配字段组' : '暂无字段组，可在右侧输入 formCode 新建' }}
          </p>
          <ul v-else class="space-y-0.5">
            <li v-for="f in forms" :key="f.formCode">
              <button
                type="button"
                data-testid="field-group-item"
                class="w-full rounded-md px-3 py-2 text-left text-sm transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                :class="selectedCode === f.formCode ? 'bg-primary/10 text-primary' : ''"
                @click="selectForm(f)"
              >
                <div class="flex items-center justify-between gap-2">
                  <span class="truncate font-medium">{{ f.formName || f.formCode }}</span>
                  <Badge v-if="f.version != null" variant="secondary">v{{ f.version }}</Badge>
                </div>
                <div class="mt-1 truncate font-mono text-xs text-muted-foreground">{{ f.formCode }}</div>
                <div class="mt-1 text-xs text-muted-foreground">{{ f.fields?.length ?? 0 }} 个字段</div>
              </button>
            </li>
          </ul>
        </div>
        <PaginationBar
          v-if="groupTotal > 0"
          :page="groupPage"
          :total="groupTotal"
          :page-size="groupPageSize"
          @change="(p) => loadGroups(p)"
        />
      </aside>

      <section class="min-w-0 flex-1">
        <div class="mb-4 rounded-lg border bg-card p-4">
          <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <div class="space-y-2 sm:col-span-2">
              <Label for="formCode">表单编码 formCode</Label>
              <div class="flex gap-2">
                <Input id="formCode" v-model="formCodeInput" placeholder="例如 order_form" class="font-mono" />
                <Button variant="outline" @click="startNewForm">新建</Button>
                <Button @click="loadFromDb">加载</Button>
              </div>
            </div>
            <div class="space-y-2">
              <Label for="formName">表单名称</Label>
              <Input id="formName" v-model="formName" placeholder="显示名称" />
            </div>
            <div class="space-y-2">
              <Label for="customFormId">关联 custom_forms.id</Label>
              <Input id="customFormId" v-model="customFormId" type="number" placeholder="可选" />
            </div>
          </div>

          <div class="mt-4 flex flex-wrap items-center gap-4">
            <label class="flex items-center gap-2 text-sm">
              <input v-model="autoPublish" type="checkbox" class="rounded border" />
              保存后自动发布到 Redis
            </label>
            <template v-if="meta">
              <Badge variant="secondary">版本 {{ meta.version ?? '-' }}</Badge>
              <span class="text-sm text-muted-foreground">更新时间：{{ formatTime(meta.updateTime) }}</span>
              <span v-if="meta.tenantId != null" class="text-sm text-muted-foreground">
                租户 ID：{{ meta.tenantId }}
              </span>
            </template>
          </div>

          <p v-if="successMsg" class="mt-3 text-sm text-green-600">{{ successMsg }}</p>
          <p v-if="error" class="mt-3 text-sm text-destructive">{{ error }}</p>
        </div>

        <div v-if="!formCodeInput" class="rounded-lg border bg-muted/30 px-6 py-16 text-center text-muted-foreground">
          请从左侧选择字段组，或输入 formCode 新建字段组
        </div>
        <template v-else>
          <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
            <h2 class="text-lg font-semibold">字段定义</h2>
            <div class="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" @click="addField">
                <Plus class="mr-1 h-4 w-4" />
                添加字段
              </Button>
              <Button variant="outline" size="sm" :disabled="saving" @click="showRedisDefinitions">
                查看 Redis 定义
              </Button>
              <Button variant="outline" size="sm" :disabled="saving" @click="handlePublishOnly">
                仅发布到 Redis
              </Button>
              <Button size="sm" :disabled="saving" @click="handleSave">
                {{ saving ? '保存中...' : '保存' }}
              </Button>
            </div>
          </div>

          <DataTableShell :loading="loading" :error="''" :empty="false">
            <div class="overflow-x-auto rounded-md border">
              <Table>
                <thead>
                  <tr class="border-b bg-muted/50">
                    <th class="h-10 w-20 px-2 text-left font-medium">排序</th>
                    <th class="h-10 px-2 text-left font-medium">字段名</th>
                    <th class="h-10 px-2 text-left font-medium">类型</th>
                    <th class="h-10 px-2 text-left font-medium">标签</th>
                    <th class="h-10 px-2 text-center font-medium">必填</th>
                    <th class="h-10 px-2 text-center font-medium">可排序</th>
                    <th class="h-10 px-2 text-center font-medium">可查询</th>
                    <th class="h-10 px-2 text-left font-medium">默认值</th>
                    <th class="h-10 w-16 px-2 text-right font-medium">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in fields" :key="index" class="border-b">
                    <td class="p-2">
                      <div class="flex gap-0.5">
                        <Button variant="ghost" size="icon" class="h-8 w-8" :disabled="index === 0" @click="moveField(index, -1)">
                          <ArrowUp class="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" class="h-8 w-8" :disabled="index === fields.length - 1" @click="moveField(index, 1)">
                          <ArrowDown class="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                    <td class="p-2">
                      <Input v-model="row.fieldName" placeholder="field_name" class="font-mono text-xs" />
                    </td>
                    <td class="p-2">
                      <Select v-model="row.fieldType" class="min-w-[100px]">
                        <option v-for="t in FIELD_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
                      </Select>
                    </td>
                    <td class="p-2">
                      <Input v-model="row.fieldLabel" placeholder="显示标签" class="text-xs" />
                    </td>
                    <td class="p-2 text-center">
                      <input v-model="row.required" type="checkbox" class="rounded border" />
                    </td>
                    <td class="p-2 text-center">
                      <input v-model="row.sortable" type="checkbox" class="rounded border" />
                    </td>
                    <td class="p-2 text-center">
                      <input v-model="row.queryable" type="checkbox" class="rounded border" />
                    </td>
                    <td class="p-2">
                      <Input
                        :model-value="row.defaultValue != null && row.defaultValue !== '' ? String(row.defaultValue) : ''"
                        placeholder="可选"
                        class="text-xs"
                        @update:model-value="row.defaultValue = $event || undefined"
                      />
                    </td>
                    <td class="p-2 text-right">
                      <Button variant="ghost" size="icon" class="h-8 w-8" @click="removeField(index)">
                        <Trash2 class="h-4 w-4 text-destructive" />
                      </Button>
                    </td>
                  </tr>
                </tbody>
              </Table>
            </div>
          </DataTableShell>
        </template>
      </section>
    </div>

    <Dialog v-model:open="redisDialogOpen" title="Redis 当前字段定义" class="max-w-2xl">
      <DataTableShell :loading="redisLoading" :error="''" :empty="!redisLoading && !redisFields.length">
        <Table>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-10 px-4 text-left font-medium">字段名</th>
              <th class="h-10 px-4 text-left font-medium">类型</th>
              <th class="h-10 px-4 text-left font-medium">标签</th>
              <th class="h-10 px-4 text-left font-medium">必填</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(f, i) in redisFields" :key="i" class="border-b">
              <td class="p-4 font-mono text-sm">{{ f.fieldName }}</td>
              <td class="p-4">{{ f.fieldType }}</td>
              <td class="p-4">{{ f.fieldLabel }}</td>
              <td class="p-4">{{ f.required ? '是' : '否' }}</td>
            </tr>
          </tbody>
        </Table>
      </DataTableShell>
      <div class="mt-4 flex justify-end">
        <Button variant="outline" @click="redisDialogOpen = false">关闭</Button>
      </div>
    </Dialog>
  </div>
</template>
