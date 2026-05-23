<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Trash2, ArrowUp, ArrowDown, RefreshCw } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Label from '@/components/ui/Label.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Badge from '@/components/ui/Badge.vue'
import {
  getExtendFormFromDb,
  listFieldDefinitions,
  publishExtendForm,
  saveExtendForm,
} from '@/api/extendField'
import type { ExtendFormDefinition, FieldDefinition } from '@/api/types'

const FIELD_TYPES = [
  { value: 'string', label: '字符串' },
  { value: 'number', label: '数字' },
  { value: 'date', label: '日期' },
] as const

const formCodeInput = ref('')
const formName = ref('')
const customFormId = ref<string>('')
const autoPublish = ref(true)
const fields = ref<FieldDefinition[]>([])
const meta = ref<ExtendFormDefinition | null>(null)

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const successMsg = ref('')

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
  formName.value = def.formName ?? ''
  customFormId.value = def.customFormId != null ? String(def.customFormId) : ''
  fields.value =
    def.fields?.length > 0
      ? def.fields.map((f) => ({ ...emptyField(), ...f }))
      : [emptyField()]
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
  } catch (e) {
    meta.value = null
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
    const request = buildRequest()
    const def = await saveExtendForm(code, request)
    applyDefinition(def)
    successMsg.value = autoPublish.value
      ? `已保存并发布到 Redis（版本 ${def.version ?? '-'}）`
      : `已保存到数据库（版本 ${def.version ?? '-'}）`
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
</script>

<template>
  <div>
    <PageHeader
      title="扩展字段管理"
      description="Center /extend-field/forms — 表单定义入库并发布到 Redis"
    >
      <template #actions>
        <Button variant="outline" :disabled="loading" @click="loadFromDb">
          <RefreshCw class="mr-1 h-4 w-4" />
          从库加载
        </Button>
      </template>
    </PageHeader>

    <div class="mb-6 space-y-4 rounded-lg border bg-card p-4">
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="space-y-2 sm:col-span-2">
          <Label for="formCode">表单编码 formCode</Label>
          <div class="flex gap-2">
            <Input
              id="formCode"
              v-model="formCodeInput"
              placeholder="例如 order_form"
              class="font-mono"
            />
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

      <div class="flex flex-wrap items-center gap-4">
        <label class="flex items-center gap-2 text-sm">
          <input v-model="autoPublish" type="checkbox" class="rounded border" />
          保存后自动发布到 Redis
        </label>
        <template v-if="meta">
          <Badge variant="secondary">版本 {{ meta.version ?? '-' }}</Badge>
          <span class="text-sm text-muted-foreground">
            更新时间：{{ formatTime(meta.updateTime) }}
          </span>
          <span v-if="meta.tenantId != null" class="text-sm text-muted-foreground">
            租户 ID：{{ meta.tenantId }}
          </span>
        </template>
      </div>

      <p v-if="successMsg" class="text-sm text-green-600">{{ successMsg }}</p>
      <p v-if="error" class="text-sm text-destructive">{{ error }}</p>
    </div>

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
          {{ saving ? '保存中…' : '保存' }}
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
                  <Button
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8"
                    :disabled="index === 0"
                    @click="moveField(index, -1)"
                  >
                    <ArrowUp class="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8"
                    :disabled="index === fields.length - 1"
                    @click="moveField(index, 1)"
                  >
                    <ArrowDown class="h-4 w-4" />
                  </Button>
                </div>
              </td>
              <td class="p-2">
                <Input v-model="row.fieldName" placeholder="field_name" class="font-mono text-xs" />
              </td>
              <td class="p-2">
                <Select v-model="row.fieldType" class="min-w-[100px]">
                  <option v-for="t in FIELD_TYPES" :key="t.value" :value="t.value">
                    {{ t.label }}
                  </option>
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
                  :model-value="
                    row.defaultValue != null && row.defaultValue !== ''
                      ? String(row.defaultValue)
                      : ''
                  "
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

    <Dialog v-model:open="redisDialogOpen" title="Redis 当前字段定义" class="max-w-2xl">
      <DataTableShell
        :loading="redisLoading"
        :error="''"
        :empty="!redisLoading && !redisFields.length"
      >
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
