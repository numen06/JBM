<script setup lang="ts">
import { ref } from 'vue'
import { createKeyClient, keyLoginError } from '../lib/keyLogin'

const props = defineProps<{ authBase: string; clientId: string; disabled?: boolean;
  login: (proof: string, kind: string) => Promise<unknown> }>()
const emit = defineEmits<{ 'busy-change': [value: boolean] }>()
const busy = ref(false)
const error = ref('')
async function selectFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || busy.value || props.disabled) return
  busy.value = true
  emit('busy-change', true)
  error.value = ''
  try {
    const proof = await createKeyClient(props.authBase, () => '').fileKeyProof(file, props.clientId)
    await props.login(proof, 'FILE_KEY')
  } catch (reason) { error.value = keyLoginError(reason) }
  finally { busy.value = false; emit('busy-change', false) }
}
</script>

<template>
  <section class="my-4 space-y-3 rounded-lg border p-4" aria-label="密钥文件登录">
    <p class="text-sm font-medium">选择密钥文件，直接登录对应账号</p>
    <p class="text-xs opacity-70">请选择登录后在用户中心下载的 .jbmkey 文件。无需输入账号密码，不调用系统认证。</p>
    <label class="block space-y-2 text-sm">
      <span>{{ busy ? '正在验证并登录…' : '选择密钥文件' }}</span>
      <input type="file" accept=".jbmkey,.json,application/json" :disabled="busy || disabled"
        class="block w-full rounded border bg-transparent p-2 text-sm" @change="selectFile" />
    </label>
    <p v-if="error" role="alert" class="text-sm text-red-500">{{ error }}</p>
  </section>
</template>
