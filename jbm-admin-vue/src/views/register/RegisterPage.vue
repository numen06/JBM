<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ArrowLeft, RefreshCw } from '@lucide/vue'
import { register } from '@/api/auth'
import { fetchCaptchaBase64 } from '@/api/captcha'
import { DEV_CAPTCHA_CODE, JBM_DEFAULT_CLIENT_ID, JBM_DEFAULT_CLIENT_SECRET } from '@/constants/loginModes'
import { extractApiError } from '@/lib/errors'
import AuthBrandPanel from '@/components/landing/AuthBrandPanel.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Label from '@/components/ui/Label.vue'

const router = useRouter()
const isDev = import.meta.env.DEV

const userName = ref('')
const password = ref('')
const confirmPassword = ref('')
const nickName = ref('')
const email = ref('')
const mobile = ref('')
const vcode = ref(isDev ? DEV_CAPTCHA_CODE : '')

const loading = ref(false)
const error = ref('')
const success = ref('')
const captchaSrc = ref('')
const captchaLoading = ref(false)

async function loadCaptcha() {
  captchaLoading.value = true
  if (!isDev) vcode.value = ''
  try {
    captchaSrc.value = await fetchCaptchaBase64(120, 40)
  } catch (e) {
    captchaSrc.value = ''
    error.value = extractApiError(e, '验证码加载失败')
  } finally {
    captchaLoading.value = false
    if (isDev && !vcode.value.trim()) vcode.value = DEV_CAPTCHA_CODE
  }
}

async function onSubmit() {
  error.value = ''
  success.value = ''
  if (!userName.value.trim()) {
    error.value = '请输入用户名'
    return
  }
  if (userName.value.trim().length < 2) {
    error.value = '用户名至少 2 个字符'
    return
  }
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  if (password.value.length < 6) {
    error.value = '密码至少 6 位'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (!vcode.value.trim()) {
    error.value = '请输入图形验证码'
    return
  }
  loading.value = true
  try {
    await register({
      userName: userName.value.trim(),
      password: password.value,
      nickName: nickName.value.trim() || undefined,
      email: email.value.trim() || undefined,
      mobile: mobile.value.trim() || undefined,
      vcode: vcode.value.trim(),
      clientId: JBM_DEFAULT_CLIENT_ID,
      clientSecret: JBM_DEFAULT_CLIENT_SECRET,
    })
    success.value = '注册成功，即将跳转到登录页…'
    setTimeout(() => {
      router.push({ name: 'login', query: { username: userName.value.trim() } })
    }, 1500)
  } catch (e) {
    error.value = extractApiError(e, '注册失败')
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<template>
  <div class="flex min-h-screen flex-col lg:flex-row">
    <div class="hidden min-h-screen lg:block lg:w-[42%] xl:w-[45%]">
      <AuthBrandPanel title="加入 JBM 社区" subtitle="注册开发者账号，开始接入 OAuth2" />
    </div>

    <div class="flex flex-1 flex-col justify-center bg-background px-4 py-10 sm:px-8 lg:px-12">
      <div class="mx-auto w-full max-w-md">
        <RouterLink to="/" class="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-primary">
          <ArrowLeft class="size-4" />
          返回首页
        </RouterLink>

        <div class="mb-8 lg:hidden">
          <h1 class="text-2xl font-bold">注册 JBM 账号</h1>
          <p class="mt-1 text-sm text-muted-foreground">创建账号后即可登录控制台、创建 OAuth2 应用</p>
        </div>

        <div class="hidden lg:block">
          <h2 class="text-2xl font-bold tracking-tight">创建账号</h2>
          <p class="mt-2 text-sm text-muted-foreground">填写以下信息完成注册</p>
        </div>

        <form class="mt-8 space-y-4" @submit.prevent="onSubmit">
          <div class="space-y-2">
            <Label>用户名 <span class="text-destructive">*</span></Label>
            <Input v-model="userName" placeholder="2-20 位字符" autocomplete="username" />
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div class="space-y-2">
              <Label>密码 <span class="text-destructive">*</span></Label>
              <Input v-model="password" type="password" placeholder="至少 6 位" autocomplete="new-password" />
            </div>
            <div class="space-y-2">
              <Label>确认密码 <span class="text-destructive">*</span></Label>
              <Input v-model="confirmPassword" type="password" placeholder="再次输入密码" autocomplete="new-password" />
            </div>
          </div>

          <div class="space-y-2">
            <Label>昵称</Label>
            <Input v-model="nickName" placeholder="可选，默认同用户名" />
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div class="space-y-2">
              <Label>邮箱</Label>
              <Input v-model="email" type="email" placeholder="可选" autocomplete="email" />
            </div>
            <div class="space-y-2">
              <Label>手机号</Label>
              <Input v-model="mobile" type="tel" placeholder="可选" autocomplete="tel" />
            </div>
          </div>

          <div class="space-y-2">
            <Label>图形验证码 <span class="text-destructive">*</span></Label>
            <div class="flex gap-2">
              <Input
                v-model="vcode"
                class="flex-1"
                :placeholder="isDev ? `开发可填 ${DEV_CAPTCHA_CODE}` : '请输入验证码'"
                autocomplete="off"
                maxlength="8"
              />
              <button
                type="button"
                class="relative flex h-9 w-[120px] shrink-0 items-center justify-center overflow-hidden rounded-md border bg-muted/50"
                :disabled="captchaLoading"
                title="点击刷新验证码"
                @click="loadCaptcha"
              >
                <img v-if="captchaSrc" :src="captchaSrc" alt="验证码" class="h-full w-full object-contain" />
                <span v-else class="text-xs text-muted-foreground">{{ captchaLoading ? '加载中…' : '点击加载' }}</span>
                <RefreshCw class="absolute right-1 top-1 size-3 text-muted-foreground opacity-60" aria-hidden="true" />
              </button>
            </div>
          </div>

          <div
            v-if="error"
            role="alert"
            class="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {{ error }}
          </div>
          <div
            v-if="success"
            role="status"
            class="rounded-md border border-primary/30 bg-primary/10 px-3 py-2 text-sm text-primary"
          >
            {{ success }}
          </div>

          <Button type="submit" class="w-full" :disabled="loading || captchaLoading">
            {{ loading ? '注册中…' : '注册' }}
          </Button>

          <p class="text-center text-sm text-muted-foreground">
            已有账号？
            <RouterLink to="/login" class="font-medium text-primary hover:underline">立即登录</RouterLink>
          </p>
        </form>
      </div>
    </div>
  </div>
</template>
