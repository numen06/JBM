<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { confirmQrLogin, markQrScanned } from '@/api/qrcode'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Label from '@/components/ui/Label.vue'
import { DEV_CAPTCHA_CODE, JBM_DEFAULT_PASSWORD, JBM_DEFAULT_USERNAME } from '@/constants/loginModes'
import { extractApiError } from '@/lib/errors'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

const username = ref(JBM_DEFAULT_USERNAME)
const password = ref(JBM_DEFAULT_PASSWORD)
const vcode = ref(DEV_CAPTCHA_CODE)
const loading = ref(false)
const scanned = ref(false)
const confirmed = ref(false)
const error = ref('')

const qrCode = computed(() => String(route.query.code || '').trim())

async function markScanned() {
  if (!qrCode.value) {
    error.value = '二维码参数缺失'
    return
  }
  try {
    await markQrScanned(qrCode.value)
    scanned.value = true
  } catch (e) {
    error.value = extractApiError(e, '二维码已过期，请在电脑端刷新')
  }
}

async function confirmLogin() {
  if (!qrCode.value) {
    error.value = '二维码参数缺失'
    return
  }
  error.value = ''
  loading.value = true
  try {
    if (!auth.accessToken) {
      if (!username.value.trim()) throw new Error('请输入用户名')
      if (!password.value) throw new Error('请输入密码')
      await auth.login(username.value.trim(), password.value, {
        vcode: vcode.value.trim(),
        loginType: 'PASSWORD',
      })
    }
    await confirmQrLogin(qrCode.value)
    confirmed.value = true
  } catch (e) {
    error.value = extractApiError(e, '确认登录失败')
  } finally {
    loading.value = false
  }
}

onMounted(markScanned)
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-background px-4 py-8">
    <section class="w-full max-w-sm space-y-5">
      <div class="space-y-2 text-center">
        <h1 class="text-2xl font-semibold tracking-tight">扫码登录确认</h1>
        <p class="text-sm text-muted-foreground">
          {{ confirmed ? '电脑端正在完成登录' : scanned ? '请确认是否登录电脑端' : '正在读取二维码' }}
        </p>
      </div>

      <div v-if="confirmed" class="rounded-md border border-emerald-500/30 bg-emerald-500/10 px-3 py-3 text-sm text-emerald-700">
        已确认登录，请回到电脑端继续使用。
      </div>

      <form v-else class="space-y-4" @submit.prevent="confirmLogin">
        <div v-if="!auth.accessToken" class="space-y-4">
          <div class="space-y-2">
            <Label>用户名</Label>
            <Input v-model="username" autocomplete="username" placeholder="admin" />
          </div>
          <div class="space-y-2">
            <Label>密码</Label>
            <Input v-model="password" autocomplete="current-password" type="password" />
          </div>
          <div class="space-y-2">
            <Label>图形验证码</Label>
            <Input v-model="vcode" autocomplete="off" placeholder="9999" />
          </div>
        </div>

        <div v-if="auth.accessToken" class="rounded-md border bg-muted/30 px-3 py-3 text-sm text-muted-foreground">
          当前手机端已登录，可直接确认本次电脑端登录。
        </div>

        <div
          v-if="error"
          role="alert"
          class="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
        >
          {{ error }}
        </div>

        <Button type="submit" class="w-full" :disabled="loading || !qrCode">
          {{ loading ? '确认中…' : '确认登录' }}
        </Button>
      </form>
    </section>
  </main>
</template>
