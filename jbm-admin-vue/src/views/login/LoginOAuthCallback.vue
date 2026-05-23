<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { exchangeAuthCode, exchangeAuthorizationCode, thirdPartyCallback } from '@/api/auth'
import { OAUTH2_REDIRECT_STORAGE_KEY, OAUTH2_STATE_STORAGE_KEY } from '@/constants/loginModes'
import { extractApiError } from '@/lib/errors'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const status = ref<'loading' | 'error'>('loading')
const message = ref('正在完成登录…')

onMounted(async () => {
  const oauthError = (route.query.error as string)?.trim()
  if (oauthError) {
    status.value = 'error'
    message.value =
      (route.query.error_description as string)?.trim() ||
      `授权被拒绝：${oauthError}`
    return
  }

  const code = (route.query.code as string)?.trim()
  const provider = (route.query.provider as string)?.trim()
  if (!code) {
    status.value = 'error'
    message.value = '缺少授权码 code'
    return
  }
  try {
    const redirectUri =
      sessionStorage.getItem(OAUTH2_REDIRECT_STORAGE_KEY) ||
      `${window.location.origin}${route.path}`

    if (!provider) {
      const returnedState = (route.query.state as string)?.trim()
      const savedState = sessionStorage.getItem(OAUTH2_STATE_STORAGE_KEY)
      if (savedState && returnedState && savedState !== returnedState) {
        throw new Error('state 校验失败，请重新发起授权')
      }
    }

    const token = provider
      ? await thirdPartyCallback({
          provider,
          code,
          clientId: auth.clientId,
          clientSecret: auth.clientSecret,
          redirectUri,
          state: route.query.state as string | undefined,
        })
      : await exchangeAuthorizationCode({
          code,
          redirectUri,
          clientId: auth.clientId,
          clientSecret: auth.clientSecret,
        }).catch(() => exchangeAuthCode(code))

    sessionStorage.removeItem(OAUTH2_STATE_STORAGE_KEY)
    sessionStorage.removeItem(OAUTH2_REDIRECT_STORAGE_KEY)
    const needChange = await auth.loginWithToken(token)
    if (needChange) {
      router.replace({ name: 'login', query: { redirect: (route.query.redirect as string) || '/dashboard' } })
      return
    }
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch (e) {
    status.value = 'error'
    message.value = extractApiError(e, '登录回调失败')
  }
})
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 p-4">
    <div class="w-full max-w-md rounded-lg border bg-card p-6 text-center shadow-sm">
      <p class="text-sm text-muted-foreground" :class="status === 'error' ? 'text-destructive' : ''">
        {{ message }}
      </p>
      <button
        v-if="status === 'error'"
        type="button"
        class="mt-4 text-sm text-primary underline"
        @click="router.push({ name: 'login' })"
      >
        返回登录页
      </button>
    </div>
  </div>
</template>
