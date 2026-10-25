<script setup lang="ts">
import { ref, watch } from 'vue'
import Dialog from './ui/Dialog.vue'
import Button from './ui/Button.vue'

const props = defineProps<{ authBase: string; accessToken: string; securityUrl?: string }>()
const emit = defineEmits<{ 'active-change': [active: boolean] }>()
type LoginAlert = { id: string; time: string; clientId: string; previous: { ip: string; location: string; time: string }; current: { ip: string; location: string } }
const alerts = ref<LoginAlert[]>([])
const open = ref(false)
const busy = ref(false)
const error = ref('')
function formatTime(value: string) {
  const iso = value.replace(' ', 'T')
  const date = new Date(/(?:Z|[+-]\d\d:\d\d)$/i.test(iso) ? iso : `${iso}Z`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}
watch(open, value => emit('active-change', value))
async function request(token: string, acknowledge = false) {
  const response = await fetch(`${props.authBase.replace(/\/$/, '')}/oauth2/security/login-alerts${acknowledge ? '/acknowledge' : ''}`, {
    method: acknowledge ? 'POST' : 'GET', headers: { Authorization: `Bearer ${token}`, ...(acknowledge ? { 'Content-Type': 'application/json' } : {}) },
    ...(acknowledge ? { body: JSON.stringify({ ids: alerts.value.map(alert => alert.id) }) } : {}),
  })
  const body = await response.json()
  if (!response.ok || !body.success) throw new Error(body.message || '登录安全提醒暂时不可用')
  return body.result as LoginAlert[]
}
async function load() {
  const token = props.accessToken
  if (!token) { alerts.value = []; open.value = false; error.value = ''; return }
  try {
    const result = await request(token)
    if (token !== props.accessToken) return
    alerts.value = result
    error.value = ''
    open.value = result.length > 0 && !window.location.pathname.endsWith('/profile')
  } catch (reason) {
    if (token === props.accessToken) error.value = reason instanceof Error ? reason.message : '登录安全提醒暂时不可用'
  }
}
async function acknowledge() {
  if (busy.value) return
  const token = props.accessToken
  busy.value = true
  try {
    const result = await request(token, true)
    if (token !== props.accessToken) return
    alerts.value = result
    open.value = result.length > 0
    error.value = ''
  } catch (reason) { error.value = reason instanceof Error ? reason.message : '确认失败，请重试' }
  finally { busy.value = false }
}
watch(() => props.accessToken, () => { alerts.value = []; open.value = false; void load() }, { immediate: true })
</script>

<template>
  <div v-if="alerts.length || error" role="alert" class="mx-4 my-3 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-amber-500/40 bg-amber-500/10 px-4 py-3 text-sm">
    <span>{{ alerts.length ? '账号存在异地登录记录，请确认是否为本人操作。' : error }}</span>
    <button type="button" class="underline" @click="alerts.length ? open = true : load()">{{ alerts.length ? '查看异常记录' : '重试' }}</button>
  </div>
  <Dialog :open="open" title="检测到异地登录" @update:open="open = $event">
    <div class="space-y-4">
      <p class="text-sm">以下登录地点与之前的成功登录不同，请确认是否为本人操作。</p>
      <ul class="space-y-3">
        <li v-for="alert in alerts" :key="alert.id" class="space-y-2 rounded border p-3 text-sm">
          <p class="font-medium">{{ formatTime(alert.time) }}</p>
          <p>本次：{{ alert.current.location }} <span class="font-mono text-xs">{{ alert.current.ip }}</span></p>
          <p class="text-muted-foreground">此前：{{ alert.previous.location }} <span class="font-mono text-xs">{{ alert.previous.ip }}</span> · {{ formatTime(alert.previous.time) }}</p>
        </li>
      </ul>
      <p class="text-xs leading-5 text-muted-foreground">IP 属地是近似判断，VPN、代理或移动网络可能导致变化。若非本人操作，请立即修改密码，并移除不认识的登录密钥。</p>
      <p v-if="error" role="alert" class="text-sm text-red-500">{{ error }}</p>
      <div class="flex flex-wrap justify-end gap-3">
        <a :href="securityUrl || '/profile?tab=authentication'" class="rounded border px-3 py-2 text-sm" @click="open = false">前往账号安全</a>
        <Button :disabled="busy" @click="acknowledge">{{ busy ? '正在确认…' : '已知悉此提醒' }}</Button>
      </div>
    </div>
  </Dialog>
</template>
