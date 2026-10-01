<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createKeyClient, downloadChallenge, keyLoginError, type KeyRecord, type SshChallenge } from '../lib/keyLogin'

const props = defineProps<{ authBase: string; accessToken: string; kind: 'PASSKEY' | 'SSH_KEY' }>()
const client = () => createKeyClient(props.authBase, () => props.accessToken)
const keys = ref<KeyRecord[]>([])
const busy = ref(false)
const error = ref('')
const notice = ref('')
const name = ref('我的设备')
const publicKey = ref('')
const signature = ref('')
const challenge = ref<SshChallenge>()
const visibleKeys = computed(() => keys.value.filter((key) => key.kind === props.kind))
async function run(action: () => Promise<unknown>) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try { await action() } catch (reason) { error.value = keyLoginError(reason) } finally { busy.value = false }
}
const refresh = async () => { keys.value = await client().list() }
const addPasskey = () => run(async () => { await client().registerPasskey(name.value); await refresh(); notice.value = '通行密钥已绑定，下次可直接使用 Passkey 登录。' })
const remove = (id: string) => run(async () => { await client().remove(id); await refresh(); notice.value = '密钥已移除。' })
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
</script>

<template>
  <section class="my-4 space-y-3 rounded-lg border p-4" :aria-label="kind === 'PASSKEY' ? 'Passkeys' : 'SSH 登录密钥'">
    <h3 class="font-semibold">{{ kind === 'PASSKEY' ? 'Passkeys' : 'SSH 登录密钥' }}</h3>
    <p class="text-sm opacity-70">{{ kind === 'PASSKEY' ? '通行密钥用于浏览器登录，可通过指纹、面容或设备 PIN 验证。' : 'SSH 公钥用于平台提供的签名登录；仅上传公钥，私钥留在本机。' }}</p>
    <label class="block text-sm">密钥名称<input v-model="name" maxlength="80" class="mt-1 block w-full rounded border bg-transparent p-2" /></label>
    <button v-if="kind === 'PASSKEY'" type="button" class="rounded border px-3 py-2 text-sm" :disabled="busy" @click="addPasskey">添加 Passkey</button>
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
    <p v-if="error" role="alert" class="text-sm text-red-500">{{ error }}</p>
    <p v-if="notice" role="status" class="text-sm">{{ notice }}</p>
    <ul class="space-y-2">
      <li v-for="key in visibleKeys" :key="key.id" class="flex items-center justify-between gap-2 rounded border p-2 text-sm">
        <span>{{ key.name }}</span>
        <button type="button" :disabled="busy" :aria-label="`移除 ${key.name}`" class="underline" @click="remove(key.id)">移除</button>
      </li>
      <li v-if="visibleKeys.length === 0" class="rounded border border-dashed p-3 text-sm opacity-70">尚未绑定{{ kind === 'PASSKEY' ? ' Passkey' : ' SSH 登录密钥' }}</li>
    </ul>
  </section>
</template>
