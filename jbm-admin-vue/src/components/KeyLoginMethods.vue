<script setup lang="ts">
import { ref } from 'vue'
import { createKeyClient, downloadChallenge, keyLoginError, type SshChallenge } from '../lib/keyLogin'

const props = defineProps<{ authBase: string; clientId: string; method: 'PASSKEY' | 'SSH_KEY'; disabled?: boolean;
  login: (proof: string, kind: string) => Promise<unknown> }>()
const busy = ref(false)
const error = ref('')
const publicKey = ref('')
const signature = ref('')
const challenge = ref<SshChallenge>()
async function run(action: () => Promise<unknown>) {
  if (busy.value || props.disabled) return
  busy.value = true
  error.value = ''
  try { await action() } catch (reason) { error.value = keyLoginError(reason) } finally { busy.value = false }
}
const client = () => createKeyClient(props.authBase, () => '')
const passkey = () => run(async () => props.login(await client().passkeyProof(props.clientId), 'PASSKEY'))
const prepareSsh = () => run(async () => {
  challenge.value = await client().sshOptions(props.clientId)
  signature.value = ''
  downloadChallenge(challenge.value)
})
const loginSsh = () => run(async () => {
  const current = challenge.value
  challenge.value = undefined
  if (!current) throw new Error('请重新下载验证文件')
  await props.login(JSON.stringify({ challengeId: current.challengeId, publicKey: publicKey.value, signature: signature.value }), 'SSH_KEY')
})
</script>

<template>
  <section class="my-4 space-y-3 rounded-lg border p-4" :aria-label="method === 'PASSKEY' ? 'Passkey 登录' : 'SSH 签名登录'">
    <template v-if="method === 'PASSKEY'">
      <p class="text-sm font-medium">使用 Passkey 登录</p>
      <p class="text-xs opacity-70">使用已绑定的通行密钥，通过指纹、面容或设备 PIN 验证身份。</p>
      <button type="button" class="w-full rounded-md border px-4 py-3 font-medium" :disabled="busy || disabled" @click="passkey">
        {{ busy ? '正在验证…' : '继续使用 Passkey' }}
      </button>
    </template>
    <div v-else class="space-y-3">
      <p class="text-sm font-medium">使用 SSH 密钥签名登录</p>
      <p class="text-xs opacity-70">这是平台提供的签名登录方式。先在 SSH 登录密钥设置中绑定公钥；私钥始终留在本机。</p>
      <textarea v-model="publicKey" aria-label="SSH 公钥" placeholder="粘贴 .pub 公钥" rows="2" class="w-full rounded border bg-transparent p-2 text-xs" />
      <button type="button" class="rounded border px-3 py-2 text-sm" :disabled="busy || disabled" @click="prepareSsh">下载一次性验证文件</button>
      <template v-if="challenge">
        <p class="text-xs">在下载目录执行（替换为你的私钥路径，验证文件 2 分钟内有效）：</p>
        <code class="block break-all text-xs">ssh-keygen -Y sign -f ~/.ssh/id_ed25519 -n jbm-key-login jbm-login-challenge.txt</code>
        <textarea v-model="signature" aria-label="SSH 签名" placeholder="粘贴生成的 jbm-login-challenge.txt.sig 内容" rows="4" class="w-full rounded border bg-transparent p-2 text-xs" />
        <button type="button" class="rounded border px-3 py-2 text-sm" :disabled="busy || disabled || !signature || !publicKey" @click="loginSsh">验证并登录</button>
      </template>
    </div>
    <p v-if="error" role="alert" class="text-sm text-red-500">{{ error }}</p>
  </section>
</template>
