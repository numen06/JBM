<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Mail, Pencil, Plus, RefreshCw, Settings, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import DataTableShell from '@/components/DataTableShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Table from '@/components/ui/Table.vue'
import { useFeedback } from '@/composables/useFeedback'
import { usePagedList } from '@/composables/usePagedList'
import {
  deleteEmailConfigs,
  deletePushConfigs,
  listEmailConfigs,
  listPushConfigs,
  saveEmailConfig,
  savePushConfig,
  testNtfyConfig,
} from '@/api/pushChannels'
import type { EmailPushConfig, PushConfigInfo } from '@/api/types'

const feedback = useFeedback()

const CHANNEL_TYPES = [
  { value: 1, code: 'internal', label: '站内通知' },
  { value: 2, code: 'email', label: '邮箱' },
  { value: 3, code: 'sms', label: '短信' },
  { value: 4, code: 'wechat', label: '微信公众号' },
  { value: 5, code: 'miniapp', label: '微信小程序' },
  { value: 6, code: 'mqtt', label: 'MQTT' },
  { value: 7, code: 'app', label: 'App 推送' },
  { value: 8, code: 'ntfy', label: 'ntfy 推送' },
]

const selectedChannel = ref('all')
const configDialogOpen = ref(false)
const emailDialogOpen = ref(false)
const saving = ref(false)
const testingId = ref<number>()
const ntfyForm = reactive({ serverUrl: '', topicPrefix: 'jbm', token: '', priority: '3' })

const configForm = reactive<PushConfigInfo>({
  enable: true,
  type: 1,
  releaseContent: '',
})

const emailForm = reactive<EmailPushConfig>({
  host: '',
  port: '465',
  username: '',
  password: '',
})

const {
  items: configs,
  total: configTotal,
  page: configPage,
  loading: configLoading,
  error: configError,
  load: loadConfigs,
  pageSize: configPageSize,
} = usePagedList<PushConfigInfo>(
  (p, s) =>
    listPushConfigs(
      p,
      s,
      selectedChannel.value === 'all' ? {} : { type: Number(selectedChannel.value) },
    ),
  10,
)

const {
  items: emailConfigs,
  total: emailTotal,
  page: emailPage,
  loading: emailLoading,
  error: emailError,
  load: loadEmails,
  pageSize: emailPageSize,
} = usePagedList<EmailPushConfig>((p, s) => listEmailConfigs(p, s), 10)

const configDialogTitle = computed(() => (configForm.id ? '编辑渠道配置' : '新增渠道配置'))
const emailDialogTitle = computed(() => (emailForm.id ? '编辑邮箱配置' : '新增邮箱配置'))
const configTypeValue = computed({
  get: () => String(configForm.type ?? 1),
  set: (value: string) => {
    configForm.type = Number(value)
  },
})
const configEnableValue = computed({
  get: () => (configForm.enable === false ? 'false' : 'true'),
  set: (value: string) => {
    configForm.enable = value === 'true'
  },
})

function channelLabel(type?: number) {
  return CHANNEL_TYPES.find((item) => item.value === Number(type))?.label ?? `渠道 ${type ?? '-'}`
}

function formatTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

function contentPreview(value?: string) {
  if (!value) return '-'
  return value.length > 120 ? `${value.slice(0, 120)}...` : value
}

function resetConfigForm(type = selectedChannel.value === 'all' ? 1 : Number(selectedChannel.value)) {
  configForm.id = undefined
  configForm.enable = true
  configForm.type = type
  configForm.releaseContent = ''
}

function resetEmailForm() {
  emailForm.id = undefined
  emailForm.host = ''
  emailForm.port = '465'
  emailForm.username = ''
  emailForm.password = ''
}

function openConfigDialog(config?: PushConfigInfo) {
  if (config) {
    configForm.id = config.id
    configForm.enable = config.enable ?? true
    configForm.type = config.type ?? 1
    configForm.releaseContent = config.releaseContent ?? ''
  } else {
    resetConfigForm()
  }
  Object.assign(ntfyForm, { serverUrl: '', topicPrefix: 'jbm', token: '', priority: '3' })
  if (configForm.type === 8 && configForm.releaseContent) {
    try {
      const value = JSON.parse(configForm.releaseContent)
      Object.assign(ntfyForm, value, { priority: String(value.priority ?? 3) })
    } catch {
      feedback.toast.warning('ntfy 配置格式有误，请重新填写')
    }
  }
  configDialogOpen.value = true
}

function openEmailDialog(config?: EmailPushConfig) {
  if (config) {
    emailForm.id = config.id
    emailForm.host = config.host ?? ''
    emailForm.port = config.port ?? '465'
    emailForm.username = config.username ?? ''
    emailForm.password = config.password ?? ''
  } else {
    resetEmailForm()
  }
  emailDialogOpen.value = true
}

async function handleSaveConfig() {
  if (!configForm.type) {
    feedback.toast.warning('请选择渠道')
    return
  }
  saving.value = true
  try {
    if (configForm.type === 8) {
      configForm.releaseContent = JSON.stringify({
        serverUrl: ntfyForm.serverUrl.trim(), topicPrefix: ntfyForm.topicPrefix.trim(),
        token: ntfyForm.token.trim(), priority: Number(ntfyForm.priority),
      })
    }
    await savePushConfig({ ...configForm, type: Number(configForm.type) })
    feedback.toast.success('渠道配置已保存')
    configDialogOpen.value = false
    await loadConfigs(configPage.value)
  } finally {
    saving.value = false
  }
}

async function handleTestNtfy(config: PushConfigInfo) {
  if (!config.id) return
  testingId.value = config.id
  try {
    await testNtfyConfig(config.id)
    feedback.toast.success('测试消息已提交到 ntfy，请在订阅端查收')
  } catch (error) {
    feedback.toast.error(error instanceof Error ? error.message : 'ntfy 发送失败')
  } finally {
    testingId.value = undefined
  }
}

async function handleDeleteConfig(config: PushConfigInfo) {
  if (!config.id) return
  const confirmed = await feedback.confirm({
    title: '删除渠道配置',
    message: `确认删除 ${channelLabel(config.type)} 的配置？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deletePushConfigs([config.id])
  feedback.toast.success('渠道配置已删除')
  await loadConfigs(configPage.value)
}

async function handleSaveEmail() {
  if (!emailForm.host?.trim() || !emailForm.username?.trim()) {
    feedback.toast.warning('请填写 SMTP 主机和用户名')
    return
  }
  saving.value = true
  try {
    await saveEmailConfig({ ...emailForm })
    feedback.toast.success('邮箱配置已保存')
    emailDialogOpen.value = false
    await loadEmails(emailPage.value)
  } finally {
    saving.value = false
  }
}

async function handleDeleteEmail(config: EmailPushConfig) {
  if (!config.id) return
  const confirmed = await feedback.confirm({
    title: '删除邮箱配置',
    message: `确认删除 ${config.host || config.username || config.id}？`,
    variant: 'destructive',
  })
  if (!confirmed) return
  await deleteEmailConfigs([config.id])
  feedback.toast.success('邮箱配置已删除')
  await loadEmails(emailPage.value)
}

async function refreshAll() {
  await Promise.all([loadConfigs(configPage.value), loadEmails(emailPage.value)])
}
</script>

<template>
  <div class="space-y-5">
    <PageHeader title="渠道设置" description="配置站内信、邮箱、短信等推送渠道的启停和连接参数。">
      <template #actions>
        <Select v-model="selectedChannel" class="w-36" @update:model-value="loadConfigs(1)">
          <option value="all">全部渠道</option>
          <option v-for="channel in CHANNEL_TYPES" :key="channel.value" :value="channel.value">
            {{ channel.label }}
          </option>
        </Select>
        <Button variant="outline" size="sm" :disabled="configLoading || emailLoading" @click="refreshAll">
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
        <Button size="sm" @click="openConfigDialog()">
          <Plus class="h-4 w-4" />
          新增渠道
        </Button>
        <Button variant="outline" size="sm" @click="openEmailDialog()">
          <Mail class="h-4 w-4" />
          新增邮箱
        </Button>
      </template>
    </PageHeader>

    <section class="space-y-3">
      <div class="flex items-center gap-2">
        <Settings class="h-4 w-4 text-muted-foreground" />
        <h2 class="text-sm font-semibold">通用渠道</h2>
      </div>
      <DataTableShell :loading="configLoading" :error="configError" :empty="!configs.length">
        <Table>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-10 px-4 text-left font-medium">渠道</th>
              <th class="h-10 px-4 text-left font-medium">状态</th>
              <th class="h-10 px-4 text-left font-medium">配置内容</th>
              <th class="h-10 px-4 text-left font-medium">更新时间</th>
              <th class="h-10 w-36 px-4 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="config in configs" :key="config.id" class="border-b align-top">
              <td class="p-4 font-medium">{{ channelLabel(config.type) }}</td>
              <td class="p-4">
                <Badge :variant="config.enable === false ? 'outline' : 'default'">
                  {{ config.enable === false ? '停用' : '启用' }}
                </Badge>
              </td>
              <td class="max-w-[560px] p-4 text-sm text-muted-foreground">
                <p class="line-clamp-3 whitespace-pre-line">{{ contentPreview(config.releaseContent) }}</p>
              </td>
              <td class="p-4 text-sm text-muted-foreground">{{ formatTime(config.updateTime || config.createTime) }}</td>
              <td class="p-4">
                <div class="flex justify-end gap-2">
                  <Button v-if="config.type === 8" variant="outline" size="sm" :disabled="config.enable === false || testingId != null" @click="handleTestNtfy(config)">测试发送</Button>
                  <Button variant="outline" size="sm" @click="openConfigDialog(config)">
                    <Pencil class="h-4 w-4" />
                  </Button>
                  <Button variant="outline" size="sm" @click="handleDeleteConfig(config)">
                    <Trash2 class="h-4 w-4" />
                  </Button>
                </div>
              </td>
            </tr>
          </tbody>
        </Table>
        <PaginationBar :page="configPage" :total="configTotal" :page-size="configPageSize" @change="loadConfigs" />
      </DataTableShell>
    </section>

    <section class="space-y-3">
      <div class="flex items-center gap-2">
        <Mail class="h-4 w-4 text-muted-foreground" />
        <h2 class="text-sm font-semibold">邮箱 SMTP</h2>
      </div>
      <DataTableShell :loading="emailLoading" :error="emailError" :empty="!emailConfigs.length">
        <Table>
          <thead>
            <tr class="border-b bg-muted/50">
              <th class="h-10 px-4 text-left font-medium">主机</th>
              <th class="h-10 px-4 text-left font-medium">端口</th>
              <th class="h-10 px-4 text-left font-medium">用户名</th>
              <th class="h-10 px-4 text-left font-medium">密码</th>
              <th class="h-10 px-4 text-left font-medium">更新时间</th>
              <th class="h-10 w-36 px-4 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="config in emailConfigs" :key="config.id" class="border-b">
              <td class="p-4 font-medium">{{ config.host || '-' }}</td>
              <td class="p-4 text-sm text-muted-foreground">{{ config.port || '-' }}</td>
              <td class="p-4 text-sm text-muted-foreground">{{ config.username || '-' }}</td>
              <td class="p-4 text-sm text-muted-foreground">{{ config.password ? '******' : '-' }}</td>
              <td class="p-4 text-sm text-muted-foreground">{{ formatTime(config.updateTime || config.createTime) }}</td>
              <td class="p-4">
                <div class="flex justify-end gap-2">
                  <Button variant="outline" size="sm" @click="openEmailDialog(config)">
                    <Pencil class="h-4 w-4" />
                  </Button>
                  <Button variant="outline" size="sm" @click="handleDeleteEmail(config)">
                    <Trash2 class="h-4 w-4" />
                  </Button>
                </div>
              </td>
            </tr>
          </tbody>
        </Table>
        <PaginationBar :page="emailPage" :total="emailTotal" :page-size="emailPageSize" @change="loadEmails" />
      </DataTableShell>
    </section>

    <CrudDialog v-model:open="configDialogOpen" :title="configDialogTitle" :saving="saving" wide @save="handleSaveConfig">
      <div class="grid gap-4 md:grid-cols-2">
        <FormField label="渠道" required>
          <Select v-model="configTypeValue">
            <option v-for="channel in CHANNEL_TYPES" :key="channel.value" :value="channel.value">
              {{ channel.label }}
            </option>
          </Select>
        </FormField>
        <FormField label="状态">
          <Select v-model="configEnableValue">
            <option value="true">启用</option>
            <option value="false">停用</option>
          </Select>
        </FormField>
      </div>
      <div v-if="configForm.type === 8" class="grid gap-4 md:grid-cols-2">
        <FormField label="服务地址" required>
          <Input v-model="ntfyForm.serverUrl" placeholder="https://notify.hz-aitech.com" />
        </FormField>
        <FormField label="主题前缀" required>
          <Input v-model="ntfyForm.topicPrefix" placeholder="jbm" />
        </FormField>
        <FormField label="访问 Token">
          <Input v-model="ntfyForm.token" type="password" autocomplete="new-password" placeholder="tk_…（公开服务可留空）" />
          <p class="mt-1 text-xs text-muted-foreground">已保存的 Token 显示为 ********；不修改即保留，清空则移除认证。</p>
        </FormField>
        <FormField label="默认优先级">
          <Select v-model="ntfyForm.priority">
            <option value="1">最低</option><option value="2">低</option><option value="3">默认</option><option value="4">高</option><option value="5">紧急</option>
          </Select>
        </FormField>
        <p class="text-xs text-muted-foreground md:col-span-2">按 JBM 收件人生成主题：个人为「前缀-user-用户ID」，广播为「前缀-broadcast」。测试发送给当前登录用户；订阅账号需要对应主题的读取权限。</p>
      </div>
      <FormField v-else label="配置内容">
        <textarea
          v-model="configForm.releaseContent"
          class="min-h-40 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          placeholder='例如 {"accessKey":"...","secret":"..."}'
        />
      </FormField>
    </CrudDialog>

    <CrudDialog v-model:open="emailDialogOpen" :title="emailDialogTitle" :saving="saving" wide @save="handleSaveEmail">
      <div class="grid gap-4 md:grid-cols-2">
        <FormField label="SMTP 主机" required>
          <Input v-model="emailForm.host" placeholder="smtp.example.com" />
        </FormField>
        <FormField label="端口">
          <Input v-model="emailForm.port" placeholder="465" />
        </FormField>
        <FormField label="用户名" required>
          <Input v-model="emailForm.username" placeholder="mail@example.com" />
        </FormField>
        <FormField label="密码">
          <Input v-model="emailForm.password" type="password" placeholder="授权码或密码" />
        </FormField>
      </div>
    </CrudDialog>
  </div>
</template>
