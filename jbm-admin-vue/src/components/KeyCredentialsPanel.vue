<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Button from './ui/Button.vue'
import Dialog from './ui/Dialog.vue'
import Input from './ui/Input.vue'
import { createKeyClient, downloadChallenge, downloadLoginKey, loginKeyFilename, keyLoginError, type KeyRecord, type SshChallenge, type LoginKeyFile } from '../lib/keyLogin'

const props = defineProps<{ authBase: string; accessToken: string; kind: 'PASSKEY' | 'SSH_KEY' | 'FILE_KEY'; adminUserId?: string }>()
const client = () => createKeyClient(props.authBase, () => props.accessToken, props.adminUserId)
const keys = ref<KeyRecord[]>([])
const busy = ref(false)
const error = ref('')
const notice = ref('')
const name = ref('我的设备')
const issuanceReason = ref('')
const validity = ref<'six_months' | 'permanent'>('six_months')
const publicKey = ref('')
const signature = ref('')
const challenge = ref<SshChallenge>()
const generatedFile = ref<LoginKeyFile>()
const generatedFiles = ref<Record<string, LoginKeyFile>>({})
const passwordDialog = ref(false)
const currentPassword = ref('')
const pendingAction = ref<'generate' | 'download'>('generate')
function setPasswordDialog(open: boolean) {
  if (busy.value) return
  passwordDialog.value = open
  currentPassword.value = ''
  error.value = ''
}
function clearGenerated(id?: string) {
  if (id) delete generatedFiles.value[id]
  else generatedFiles.value = {}
  if (!id || generatedFile.value?.keyId === id) generatedFile.value = undefined
}
const title = computed(() => props.kind === 'FILE_KEY' ? '密钥文件' : props.kind === 'PASSKEY' ? 'Passkeys' : 'SSH 登录密钥')
const visibleKeys = computed(() => keys.value.filter((key) => key.kind === props.kind))
async function run(action: () => Promise<unknown>) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try { await action() } catch (reason) {
    error.value = keyLoginError(reason)
    if (error.value === '密码验证已过期，请重新验证当前账号密码') {
      error.value = ''
      passwordDialog.value = true
    }
  } finally { busy.value = false }
}
const refresh = async () => { keys.value = await client().list() }
const addPasskey = () => run(async () => { await client().registerPasskey(name.value); await refresh(); notice.value = '通行密钥已绑定，下次可直接使用 Passkey 登录。' })
const remove = (id: string) => run(async () => { await client().remove(id); clearGenerated(id); await refresh(); notice.value = '密钥已移除。' })
async function executeFileAction() {
  const sessionToken = props.accessToken
  if (pendingAction.value === 'generate') {
    if (!props.adminUserId) await client().checkPasswordVerification()
    else if (!issuanceReason.value.trim()) throw new Error('请填写签发用途')
    const file = await client().registerFileKey(name.value.trim() || '我的密钥', validity.value, issuanceReason.value.trim())
    if (props.accessToken !== sessionToken) throw new Error('登录会话已变更，请重新操作')
    generatedFile.value = file
    generatedFiles.value[file.keyId] = file
    pendingAction.value = 'download'
    await refresh()
  }
  if (!generatedFile.value) throw new Error('请先生成密钥文件')
  if (!props.adminUserId) await client().checkPasswordVerification()
  if (props.accessToken !== sessionToken) throw new Error('登录会话已变更，请重新操作')
  downloadLoginKey(generatedFile.value)
  passwordDialog.value = false
  notice.value = props.adminUserId ? '已签发并发起下载。请将文件安全交付给目标用户；权限仍由用户角色决定。' : '已发起下载，请妥善保存文件。验证成功后 1 分钟内再次生成或下载无需重复输入密码。'
}
function requestFileAction(action: 'generate' | 'download', keyId?: string) {
  if (busy.value) return
  if (action === 'generate') {
    const keyName = (name.value.trim() || '我的密钥').toLowerCase()
    const existing = keys.value.find(key => key.name.trim().toLowerCase() === keyName)
    if (existing) {
      if (!generatedFiles.value[existing.id]) {
        error.value = '密钥名称已存在，请使用其他名称；已有密钥请使用之前保存的文件。'
        return
      }
      action = 'download'
      keyId = existing.id
    }
  }
  if (keyId) generatedFile.value = generatedFiles.value[keyId]
  pendingAction.value = action
  void run(executeFileAction)
}
const confirmPassword = () => run(async () => {
  const password = currentPassword.value
  currentPassword.value = ''
  await client().verifyPassword(password)
  await executeFileAction()
})
const prepareSsh = () => run(async () => {
  challenge.value = await client().sshRegisterOptions(name.value, publicKey.value)
  signature.value = ''
  downloadChallenge(challenge.value)
})
const bindSsh = () => run(async () => {
  const current = challenge.value
  challenge.value = undefined
  if (!current) throw new Error('请重新下载验证文件')
  await client().sshRegister(current.challengeId, signature.value)
  await refresh()
  notice.value = 'SSH 公钥已绑定，可使用同一把私钥签名登录。'
})
onMounted(() => run(refresh))
watch(() => props.accessToken, () => {
  currentPassword.value = ''
  passwordDialog.value = false
  clearGenerated()
})
onBeforeUnmount(clearGenerated)
</script>

<template>
  <section class="my-4 space-y-3 rounded-lg border p-4" :aria-label="title">
    <h3 class="font-semibold">{{ title }}</h3>
    <p v-if="kind === 'FILE_KEY'" class="text-sm opacity-70">下载后可在登录页选择文件直接登录，无需系统认证。密钥文件等同登录凭证，请妥善保存；丢失后可在此移除。</p>
    <p v-else class="text-sm opacity-70">{{ kind === 'PASSKEY' ? '通行密钥用于浏览器登录，可通过指纹、面容或设备 PIN 验证。' : 'SSH 公钥用于平台提供的签名登录；仅上传公钥，私钥留在本机。' }}</p>
    <label class="block text-sm">密钥名称<input v-model="name" maxlength="80" class="mt-1 block w-full rounded border bg-transparent p-2" /></label>
    <template v-if="kind === 'FILE_KEY'">
      <label v-if="adminUserId" class="block text-sm">签发用途<input v-model="issuanceReason" maxlength="250" :disabled="busy" required class="mt-1 block w-full rounded border bg-transparent p-2" placeholder="说明为此用户开通密钥登录的用途" /></label>
      <label class="block text-sm">有效期<select v-model="validity" :disabled="busy" class="mt-1 block w-full rounded border bg-background p-2"><option value="six_months">半年（默认）</option><option value="permanent">长期有效</option></select></label>
      <p class="text-xs opacity-70">长期有效的密钥仍会在修改密码或移除后失效。</p>
      <div class="flex flex-wrap items-center gap-3">
        <Button type="button" :disabled="busy" @click="requestFileAction('generate')">{{ busy ? '正在处理…' : '生成并下载密钥' }}</Button>
      </div>
      <p class="text-xs opacity-70">名称不可重复。本页生成的密钥可重复下载；刷新或离开页面后，请使用已保存的文件。</p>
    </template>
    <button v-else-if="kind === 'PASSKEY'" type="button" class="rounded border px-3 py-2 text-sm" :disabled="busy" @click="addPasskey">添加 Passkey</button>
    <div v-else class="space-y-3">
      <textarea v-model="publicKey" aria-label="待绑定的 SSH 公钥" placeholder="粘贴 .pub 公钥；不要粘贴私钥" rows="2" class="w-full rounded border bg-transparent p-2 text-xs" />
      <button type="button" class="rounded border px-3 py-2 text-sm" :disabled="busy || !publicKey" @click="prepareSsh">下载绑定验证文件</button>
      <template v-if="challenge">
        <p class="text-xs">在下载目录执行，替换为你的私钥路径（2 分钟内有效）：</p>
        <code class="block break-all text-xs">ssh-keygen -Y sign -f ~/.ssh/id_ed25519 -n jbm-key-login jbm-login-challenge.txt</code>
        <textarea v-model="signature" aria-label="绑定验证签名" placeholder="粘贴 jbm-login-challenge.txt.sig 内容" rows="4" class="w-full rounded border bg-transparent p-2 text-xs" />
        <button type="button" class="rounded border px-3 py-2 text-sm" :disabled="busy || !signature" @click="bindSsh">验证并绑定</button>
      </template>
    </div>
    <p v-if="busy" role="status" class="text-sm">正在处理…</p>
    <p v-if="error && !passwordDialog" role="alert" class="text-sm text-red-500">{{ error }}</p>
    <p v-if="notice" role="status" class="text-sm">{{ notice }}</p>
    <ul class="space-y-2">
      <li v-for="key in visibleKeys" :key="key.id" class="flex items-center justify-between gap-2 rounded border p-2 text-sm">
        <span class="min-w-0">
          <span class="block break-words font-medium">{{ key.name }}</span>
          <span class="mt-1 block text-xs opacity-70">签发：{{ new Date(key.created_at * 1000).toLocaleString() }}</span>
          <span class="mt-1 block text-xs" :class="key.is_valid === false ? 'text-red-500' : 'opacity-70'">{{ key.expires_at ? `到期：${new Date(key.expires_at * 1000).toLocaleString()}` : '长期有效' }}</span>
          <span v-if="key.is_valid === false" class="mt-1 block text-xs text-red-500">{{ key.invalid_reason }}</span>
          <span v-if="kind === 'FILE_KEY'" class="mt-1 block text-xs opacity-70">{{ key.last_used_at ? `最近使用 ${new Date(key.last_used_at * 1000).toLocaleString()}` : '尚未使用' }}</span>
        </span>
        <span class="flex shrink-0 gap-3">
          <button v-if="generatedFiles[key.id]" type="button" :disabled="busy" :aria-label="`下载 ${key.name}`" :title="loginKeyFilename(generatedFiles[key.id]!)" class="underline" @click="requestFileAction('download', key.id)">下载</button>
          <button type="button" :disabled="busy" :aria-label="`移除 ${key.name}`" class="underline" @click="remove(key.id)">移除</button>
        </span>
      </li>
      <li v-if="visibleKeys.length === 0" class="rounded border border-dashed p-3 text-sm opacity-70">尚未绑定{{ title }}</li>
    </ul>
  </section>
  <Dialog v-if="kind === 'FILE_KEY'" :open="passwordDialog" title="验证账号密码" @update:open="setPasswordDialog">
    <form class="space-y-4" @submit.prevent="confirmPassword">
      <p class="text-sm text-muted-foreground">{{ pendingAction === 'generate' ? '请输入当前账号密码，验证通过后生成并下载密钥文件。' : '请输入当前账号密码，验证通过后下载本次密钥文件。' }}验证成功后 1 分钟内免重复验证。</p>
      <label for="file-key-current-password" class="block text-sm font-medium">当前密码</label>
      <Input id="file-key-current-password" v-model="currentPassword" type="password" autocomplete="current-password" placeholder="请输入当前账号密码" :disabled="busy" required />
      <p v-if="error" role="alert" class="text-sm text-red-500">{{ error }}</p>
      <div class="flex justify-end gap-3">
        <Button type="button" variant="outline" :disabled="busy" @click="setPasswordDialog(false)">取消</Button>
        <Button type="submit" :disabled="busy || !currentPassword">{{ busy ? '正在处理…' : '验证并继续' }}</Button>
      </div>
    </form>
  </Dialog>
</template>
