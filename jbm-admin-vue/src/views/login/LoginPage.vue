<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowLeft, RefreshCw, UserRound } from '@lucide/vue'
import { exchangeAuthorizationCode, resetJaja7Seed, thirdPartyCallback } from '@/api/auth'
import { fetchCaptchaBase64, sendSmsCode } from '@/api/captcha'
import { fetchLoginQr, pollQrLogin } from '@/api/qrcode'
import { extractApiError } from '@/lib/errors'
import {
  DEV_CAPTCHA_CODE,
  DEV_SMS_CODE,
  JBM_DEFAULT_CLIENT_ID,
  JBM_DEFAULT_CLIENT_SECRET,
  JBM_DEFAULT_OAUTH_SCOPE,
  JBM_DEFAULT_PASSWORD,
  JBM_DEFAULT_USERNAME,
  JBM_SEED_CLIENT_ID,
  JBM_SEED_CLIENT_SECRET,
  JBM_SEED_PASSWORD,
  LOCAL_DEV_LOGIN_ACCOUNTS,
  LOCAL_DEV_LOGIN_ENABLED,
  LOGIN_TABS,
  OAUTH2_REDIRECT_STORAGE_KEY,
  OAUTH2_STATE_STORAGE_KEY,
  type LocalDevLoginAccount,
  type LoginTabId,
} from '@/constants/loginModes'
import { useAuthStore } from '@/stores/auth'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Label from '@/components/ui/Label.vue'
import AuthBrandPanel from '@/components/landing/AuthBrandPanel.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const isDev = import.meta.env.DEV
const localDevLoginEnabled = LOCAL_DEV_LOGIN_ENABLED
const localDevLoginAccounts = LOCAL_DEV_LOGIN_ACCOUNTS
const useDevLoginDefaults = isDev || localDevLoginEnabled

const activeTab = ref<LoginTabId>('PASSWORD')
const activeMeta = computed(() => LOGIN_TABS.find((t) => t.id === activeTab.value)!)
const moreLoginTabs = computed(() => LOGIN_TABS.filter((t) => t.id !== 'PASSWORD'))

function selectLoginTab(tab: LoginTabId) {
  activeTab.value = tab
  showMoreModes.value = true
}

const username = ref(JBM_DEFAULT_USERNAME)
const password = ref(JBM_DEFAULT_PASSWORD)
const vcode = ref(useDevLoginDefaults ? DEV_CAPTCHA_CODE : '')
const phone = ref('')
const smsCode = ref('')
const wechatOpenId = ref('')
const wechatCode = ref('')
const miniappPhone = ref('')
const miniappCode = ref('')
const facePhone = ref('')
const faceImage = ref('')

const clientId = ref(JBM_DEFAULT_CLIENT_ID)
const clientSecret = ref(JBM_DEFAULT_CLIENT_SECRET)
const showMoreModes = ref(false)
/** 生产环境默认收起 OAuth 客户端配置 */
const showAdvanced = ref(isDev)

const loading = ref(false)
const error = ref('')
const captchaSrc = ref('')
const captchaLoading = ref(false)
const smsSending = ref(false)
const smsCooldown = ref(0)
const quickLoginPending = ref('')

const qrImage = ref('')
const qrCode = ref('')
const qrPolling = ref(false)
let qrTimer: ReturnType<typeof setInterval> | undefined
let smsTimer: ReturnType<typeof setInterval> | undefined

const thirdPartyProviders = (
  import.meta.env.VITE_OAUTH_PROVIDERS?.split(',').map((s: string) => s.trim()).filter(Boolean) as string[]
) || ['local']
const tpProvider = ref(thirdPartyProviders[0] ?? 'local')
const tpCode = ref('')

const oauthRedirectUri = ref('')
const oauthScope = ref(JBM_DEFAULT_OAUTH_SCOPE)
const oauthState = ref('')
const authCode = ref('')

const authorizeUrlPreview = computed(() => {
  if (!oauthRedirectUri.value) return ''
  const state = oauthState.value || '（点击跳转时自动生成）'
  return (
    `/oauth2/authorize?response_type=code` +
    `&client_id=${encodeURIComponent(clientId.value)}` +
    `&redirect_uri=${encodeURIComponent(oauthRedirectUri.value)}` +
    `&scope=${encodeURIComponent(oauthScope.value || 'all')}` +
    `&state=${encodeURIComponent(String(state))}`
  )
})

function initOAuthRedirectUri() {
  oauthRedirectUri.value = buildCallbackUrl()
}

async function loadCaptcha() {
  captchaLoading.value = true
  if (activeTab.value !== 'PASSWORD' && activeTab.value !== 'SMS') {
    captchaLoading.value = false
    return
  }
  if (!useDevLoginDefaults) {
    vcode.value = ''
  }
  try {
    captchaSrc.value = await fetchCaptchaBase64(120, 40)
  } catch (e) {
    captchaSrc.value = ''
    error.value = extractApiError(e, '验证码加载失败')
  } finally {
    captchaLoading.value = false
    if (useDevLoginDefaults && !vcode.value.trim()) {
      vcode.value = DEV_CAPTCHA_CODE
    }
  }
}

function stopQrPoll() {
  qrPolling.value = false
  if (qrTimer) {
    clearInterval(qrTimer)
    qrTimer = undefined
  }
}

async function loadQrSession() {
  stopQrPoll()
  error.value = ''
  qrImage.value = ''
  const redirectUri = `${window.location.origin}/login/callback`
  try {
    const session = await fetchLoginQr({
      clientId: clientId.value,
      redirectUri,
      width: 200,
      height: 200,
    })
    qrImage.value = session.image.startsWith('data:')
      ? session.image
      : `data:image/png;base64,${session.image}`
    qrCode.value = session.code
    startQrPoll()
  } catch (e) {
    error.value = extractApiError(e, '二维码加载失败')
  }
}

function startQrPoll() {
  if (!qrCode.value) return
  qrPolling.value = true
  qrTimer = setInterval(async () => {
    try {
      const r = await pollQrLogin(qrCode.value)
      if (r.done && r.token) {
        stopQrPoll()
        await finishLogin(() => auth.loginWithToken(r.token!))
      }
    } catch {
      /* 轮询中继续等待 */
    }
  }, 2000)
}

async function sendSms() {
  error.value = ''
  if (!phone.value.trim()) {
    error.value = '请输入手机号'
    return
  }
  if (!vcode.value.trim()) {
    error.value = '请先输入图形验证码'
    return
  }
  smsSending.value = true
  try {
    await sendSmsCode(phone.value.trim(), vcode.value.trim())
    smsCooldown.value = 60
    smsTimer = setInterval(() => {
      smsCooldown.value -= 1
      if (smsCooldown.value <= 0 && smsTimer) {
        clearInterval(smsTimer)
        smsTimer = undefined
      }
    }, 1000)
  } catch (e) {
    error.value = extractApiError(e, '短信发送失败')
    await loadCaptcha()
  } finally {
    smsSending.value = false
  }
}

function onFaceFile(ev: Event) {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    faceImage.value = String(reader.result ?? '')
  }
  reader.readAsDataURL(file)
}

function syncFormFromDom(form: HTMLFormElement) {
  const read = (testId: string) =>
    (form.querySelector(`[data-testid="${testId}"]`) as HTMLInputElement)?.value?.trim() ?? ''
  const u = read('login-username')
  const p = read('login-password')
  const v = read('login-vcode')
  const cid = read('login-client-id')
  const cs = read('login-client-secret')
  if (u) username.value = u
  if (p) password.value = p
  if (v) vcode.value = v
  if (cid) clientId.value = cid
  if (cs) clientSecret.value = cs
}

async function finishLogin(action: () => Promise<boolean>) {
  loading.value = true
  auth.clientId = clientId.value
  auth.clientSecret = clientSecret.value
  try {
    await action()
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    error.value = extractApiError(e, '登录失败')
    if (activeTab.value === 'PASSWORD' || activeTab.value === 'SMS') {
      await loadCaptcha()
    }
    if (activeTab.value === 'SCAN') {
      await loadQrSession()
    }
  } finally {
    loading.value = false
  }
}

function applyLocalDevLoginAccount(account: LocalDevLoginAccount) {
  activeTab.value = 'PASSWORD'
  showMoreModes.value = false
  error.value = ''
  username.value = account.username
  password.value = account.password
  vcode.value = DEV_CAPTCHA_CODE
  clientId.value = account.clientId
  clientSecret.value = account.clientSecret
  auth.clientId = account.clientId
  auth.clientSecret = account.clientSecret
  localStorage.setItem('jbm_client_id', account.clientId)
  localStorage.setItem('jbm_client_secret', account.clientSecret)
}

async function onLocalDevQuickLogin(account: LocalDevLoginAccount) {
  if (loading.value) return
  quickLoginPending.value = account.id
  applyLocalDevLoginAccount(account)
  try {
    await finishLogin(() =>
      auth.login(account.username, account.password, { vcode: DEV_CAPTCHA_CODE, loginType: 'PASSWORD' }),
    )
  } finally {
    quickLoginPending.value = ''
  }
}

async function onSubmit(ev: Event) {
  error.value = ''
  const form = ev.currentTarget as HTMLFormElement | null
  if (form) syncFormFromDom(form)

  if (activeTab.value === 'PASSWORD') {
    if (!vcode.value.trim()) {
      error.value = '请输入图形验证码'
      return
    }
    if (!password.value) {
      error.value = '请输入密码'
      return
    }
    await finishLogin(() =>
      auth.login(username.value, password.value, { vcode: vcode.value.trim(), loginType: 'PASSWORD' }),
    )
    return
  }

  if (activeTab.value === 'SMS') {
    if (!phone.value.trim()) {
      error.value = '请输入手机号'
      return
    }
    if (!smsCode.value.trim()) {
      error.value = '请输入短信验证码'
      return
    }
    await finishLogin(() =>
      auth.login(phone.value.trim(), smsCode.value.trim(), { loginType: 'SMS', vcode: vcode.value.trim() || undefined }),
    )
    return
  }

  if (activeTab.value === 'FACE') {
    if (!facePhone.value.trim()) {
      error.value = '请输入手机号'
      return
    }
    if (!faceImage.value) {
      error.value = '请上传人脸照片'
      return
    }
    await finishLogin(() =>
      auth.login(facePhone.value.trim(), faceImage.value, { loginType: 'FACE' }),
    )
    return
  }

  if (activeTab.value === 'WECHAT') {
    if (!wechatOpenId.value.trim() || !wechatCode.value.trim()) {
      error.value = '请填写 OpenID 与微信 code'
      return
    }
    await finishLogin(() =>
      auth.login(wechatOpenId.value.trim(), wechatCode.value.trim(), { loginType: 'WECHAT' }),
    )
    return
  }

  if (activeTab.value === 'MINIAPP') {
    if (!miniappPhone.value.trim() || !miniappCode.value.trim()) {
      error.value = '请填写手机号与小程序 code'
      return
    }
    await finishLogin(() =>
      auth.login(miniappPhone.value.trim(), miniappCode.value.trim(), { loginType: 'MINIAPP' }),
    )
  }
}

function buildCallbackUrl(provider?: string) {
  const resolved = router.resolve({
    name: 'login-callback',
    query: provider ? { provider } : {},
  })
  return `${window.location.origin}${resolved.href}`
}

async function onThirdPartyManual() {
  error.value = ''
  if (!tpCode.value.trim()) {
    error.value = '请输入第三方授权码 code'
    return
  }
  await finishLogin(async () => {
    const token = await thirdPartyCallback({
      provider: tpProvider.value,
      code: tpCode.value.trim(),
      clientId: clientId.value,
      clientSecret: clientSecret.value,
      redirectUri: buildCallbackUrl(tpProvider.value),
      state: undefined,
    })
    return auth.loginWithToken(token)
  })
}

function startOAuthCodeLogin() {
  error.value = ''
  auth.clientId = clientId.value
  auth.clientSecret = clientSecret.value
  const redirect = oauthRedirectUri.value.trim() || buildCallbackUrl()
  oauthRedirectUri.value = redirect
  const state = crypto.randomUUID?.() ?? `state_${Date.now()}`
  oauthState.value = state
  sessionStorage.setItem(OAUTH2_STATE_STORAGE_KEY, state)
  sessionStorage.setItem(OAUTH2_REDIRECT_STORAGE_KEY, redirect)
  window.location.href =
    `/oauth2/authorize?response_type=code` +
    `&client_id=${encodeURIComponent(clientId.value)}` +
    `&redirect_uri=${encodeURIComponent(redirect)}` +
    `&scope=${encodeURIComponent(oauthScope.value || 'all')}` +
    `&state=${encodeURIComponent(state)}`
}

async function onAuthCodeExchange() {
  error.value = ''
  if (!authCode.value.trim()) {
    error.value = '请输入授权码 code'
    return
  }
  const redirect = oauthRedirectUri.value.trim() || buildCallbackUrl()
  await finishLogin(async () => {
    const token = await exchangeAuthorizationCode({
      code: authCode.value.trim(),
      redirectUri: redirect,
      clientId: clientId.value,
      clientSecret: clientSecret.value,
    })
    return auth.loginWithToken(token)
  })
}

watch(activeTab, (tab) => {
  error.value = ''
  if (tab === 'PASSWORD' || tab === 'SMS') {
    loadCaptcha()
  }
  if (tab === 'AUTH_CODE') {
    initOAuthRedirectUri()
  }
  if (tab === 'SCAN') {
    loadQrSession()
  } else {
    stopQrPoll()
  }
})

const seedResetLoading = ref(false)

async function onResetJaja7Seed() {
  seedResetLoading.value = true
  error.value = ''
  try {
    const r = await resetJaja7Seed()
    clientId.value = JBM_SEED_CLIENT_ID
    clientSecret.value = JBM_SEED_CLIENT_SECRET
    password.value = JBM_SEED_PASSWORD
    auth.clientId = JBM_SEED_CLIENT_ID
    auth.clientSecret = JBM_SEED_CLIENT_SECRET
    localStorage.setItem('jbm_client_id', JBM_SEED_CLIENT_ID)
    localStorage.setItem('jbm_client_secret', JBM_SEED_CLIENT_SECRET)
    error.value = `已恢复种子凭证（appId=${r.jbmAppCredentialsReset}），请用密码 ${JBM_SEED_PASSWORD} 登录`
  } catch (e) {
    error.value = extractApiError(
      e,
      '恢复失败：请重启 Auth（jaja7）后重试，或暂用 demo / demo123 + Admin@123',
    )
  } finally {
    seedResetLoading.value = false
  }
}

function applyJaja7LoginDefaults() {
  username.value = JBM_DEFAULT_USERNAME
  password.value = JBM_DEFAULT_PASSWORD
  if (useDevLoginDefaults) {
    vcode.value = DEV_CAPTCHA_CODE
  }
  clientId.value = JBM_DEFAULT_CLIENT_ID
  clientSecret.value = JBM_DEFAULT_CLIENT_SECRET
  oauthScope.value = JBM_DEFAULT_OAUTH_SCOPE
  auth.clientId = JBM_DEFAULT_CLIENT_ID
  auth.clientSecret = JBM_DEFAULT_CLIENT_SECRET
  localStorage.setItem('jbm_client_id', JBM_DEFAULT_CLIENT_ID)
  localStorage.setItem('jbm_client_secret', JBM_DEFAULT_CLIENT_SECRET)
  // 密码框在部分浏览器自动化下不触发 input，确保提交前有值
  if (!clientSecret.value) clientSecret.value = JBM_DEFAULT_CLIENT_SECRET
  if (!password.value) password.value = JBM_DEFAULT_PASSWORD
}

onMounted(() => {
  const qUser = route.query.username
  if (typeof qUser === 'string' && qUser.trim()) {
    username.value = qUser.trim()
  }
  applyJaja7LoginDefaults()
  loadCaptcha()
})

onUnmounted(() => {
  stopQrPoll()
  if (smsTimer) clearInterval(smsTimer)
})
</script>

<template>
  <div class="flex min-h-screen flex-col lg:flex-row">
    <div class="hidden min-h-screen lg:block lg:w-[42%] xl:w-[45%]">
      <AuthBrandPanel title="欢迎回来" subtitle="登录 JBM 开源平台" />
    </div>

    <div class="flex flex-1 flex-col justify-center bg-background px-4 py-8 sm:px-8 lg:px-12">
      <div class="mx-auto w-full max-w-md">
        <RouterLink to="/" class="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-primary">
          <ArrowLeft class="size-4" />
          返回首页
        </RouterLink>

        <div class="mb-6 lg:hidden">
          <h1 class="text-2xl font-bold">登录</h1>
        </div>

        <div class="hidden lg:block">
          <h2 class="text-2xl font-bold tracking-tight">登录账号</h2>
          <p class="mt-2 text-sm text-muted-foreground">{{ activeMeta.description }}</p>
        </div>

        <div
          v-if="showMoreModes"
          class="mb-4 mt-6 flex flex-wrap gap-1 rounded-lg border bg-muted/30 p-1"
          role="tablist"
          aria-label="登录方式"
        >
          <button
            v-for="tab in LOGIN_TABS"
            :key="tab.id"
            type="button"
            role="tab"
            :aria-selected="activeTab === tab.id"
            class="rounded-md px-2.5 py-1.5 text-xs font-medium transition-colors sm:text-sm"
            :class="
              activeTab === tab.id
                ? 'bg-background text-foreground shadow-sm'
                : 'text-muted-foreground hover:text-foreground'
            "
            @click="activeTab = tab.id"
          >
            {{ tab.label }}
          </button>
        </div>

        <div v-else class="mt-6">
          <p class="text-sm text-muted-foreground">使用用户名和密码登录</p>
        </div>

        <div v-if="localDevLoginEnabled" class="mt-4 rounded-md border bg-muted/20 p-3">
          <div class="mb-3 flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-medium">本地开发模式</p>
              <p class="text-xs text-muted-foreground">使用运行时 OAuth 客户端与开发验证码</p>
            </div>
            <span class="rounded bg-emerald-500/10 px-2 py-1 text-xs font-medium text-emerald-700">DEV</span>
          </div>
          <div class="grid gap-2 sm:grid-cols-3">
            <Button
              v-for="account in localDevLoginAccounts"
              :key="account.id"
              type="button"
              variant="outline"
              class="h-auto min-h-16 flex-col items-start gap-1 whitespace-normal px-3 py-2 text-left"
              :disabled="loading"
              :title="`${account.username} / ${account.role}`"
              @click="onLocalDevQuickLogin(account)"
            >
              <span class="flex w-full items-center gap-2">
                <UserRound class="size-4 shrink-0" />
                <span class="truncate text-sm font-medium">
                  {{ quickLoginPending === account.id ? '登录中…' : account.label }}
                </span>
              </span>
              <span class="text-xs font-normal text-muted-foreground">{{ account.username }} · {{ account.description }}</span>
            </Button>
          </div>
        </div>

        <form
          v-if="activeTab !== 'SCAN' && activeTab !== 'THIRD_PARTY' && activeTab !== 'AUTH_CODE'"
          class="mt-4 space-y-4"
          @submit.prevent="onSubmit($event)"
        >
          <template v-if="activeTab === 'PASSWORD'">
            <div class="space-y-2">
              <Label>用户名</Label>
              <Input v-model="username" data-testid="login-username" placeholder="admin" autocomplete="username" />
            </div>
            <div class="space-y-2">
              <Label>密码</Label>
              <Input
                v-model="password"
                data-testid="login-password"
                type="password"
                :placeholder="JBM_DEFAULT_PASSWORD"
                autocomplete="current-password"
              />
            </div>
          </template>

          <template v-else-if="activeTab === 'SMS'">
            <div class="space-y-2">
              <Label>手机号</Label>
              <Input v-model="phone" type="tel" placeholder="11 位手机号" autocomplete="tel" />
            </div>
            <div class="space-y-2">
              <Label>短信验证码</Label>
              <div class="flex gap-2">
                <Input v-model="smsCode" class="flex-1" placeholder="开发可填 99999" autocomplete="one-time-code" />
                <Button
                  type="button"
                  variant="outline"
                  class="shrink-0"
                  :disabled="smsSending || smsCooldown > 0 || captchaLoading"
                  @click="sendSms"
                >
                  {{ smsCooldown > 0 ? `${smsCooldown}s` : smsSending ? '发送中…' : '获取验证码' }}
                </Button>
              </div>
            </div>
          </template>

          <template v-else-if="activeTab === 'FACE'">
            <div class="space-y-2">
              <Label>手机号</Label>
              <Input v-model="facePhone" type="tel" placeholder="已注册实名的手机号" />
            </div>
            <div class="space-y-2">
              <Label>人脸照片</Label>
              <input
                type="file"
                accept="image/*"
                capture="user"
                class="block w-full text-sm text-muted-foreground file:mr-3 file:rounded-md file:border file:bg-muted file:px-3 file:py-1.5"
                @change="onFaceFile"
              />
              <p v-if="faceImage" class="text-xs text-muted-foreground">已选择图片（{{ Math.round(faceImage.length / 1024) }} KB）</p>
            </div>
          </template>

          <template v-else-if="activeTab === 'WECHAT'">
            <div class="space-y-2">
              <Label>微信 OpenID</Label>
              <Input v-model="wechatOpenId" placeholder="openid" />
            </div>
            <div class="space-y-2">
              <Label>微信授权 code</Label>
              <Input v-model="wechatCode" placeholder="oauth code" />
            </div>
          </template>

          <template v-else-if="activeTab === 'MINIAPP'">
            <div class="space-y-2">
              <Label>手机号</Label>
              <Input v-model="miniappPhone" type="tel" placeholder="11 位手机号" />
            </div>
            <div class="space-y-2">
              <Label>小程序登录 code</Label>
              <Input v-model="miniappCode" placeholder="wx.login 返回的 code" />
            </div>
          </template>

          <div v-if="activeTab === 'PASSWORD' || activeTab === 'SMS'" class="space-y-2">
            <Label>图形验证码</Label>
            <div class="flex gap-2">
              <Input
                v-model="vcode"
                data-testid="login-vcode"
                class="flex-1"
                :placeholder="`开发可填 ${DEV_CAPTCHA_CODE}`"
                autocomplete="off"
                maxlength="8"
              />
              <button
                type="button"
                class="relative flex h-10 w-[120px] shrink-0 items-center justify-center overflow-hidden rounded-md border bg-muted/50"
                :disabled="captchaLoading"
                title="点击刷新验证码"
                @click="loadCaptcha"
              >
                <img v-if="captchaSrc" :src="captchaSrc" alt="验证码" class="h-full w-full object-contain" />
                <span v-else class="text-xs text-muted-foreground">
                  {{ captchaLoading ? '加载中…' : '点击加载' }}
                </span>
                <RefreshCw class="absolute right-1 top-1 size-3 text-muted-foreground opacity-60" aria-hidden="true" />
              </button>
            </div>
            <p v-if="activeTab === 'SMS'" class="text-xs text-muted-foreground">
              发送短信前需通过图形验证码；短信开发直通码：{{ DEV_SMS_CODE }}
            </p>
          </div>

          <details class="rounded-md border px-3 py-2 text-sm" :open="showAdvanced" @toggle="showAdvanced = ($event.target as HTMLDetailsElement).open">
            <summary class="cursor-pointer font-medium text-muted-foreground">OAuth2 客户端（高级）</summary>
            <div class="mt-3 grid grid-cols-2 gap-3">
              <div class="space-y-2">
                <Label>Client ID</Label>
                <Input v-model="clientId" data-testid="login-client-id" :placeholder="JBM_DEFAULT_CLIENT_ID" />
              </div>
              <div class="space-y-2">
                <Label>Client Secret</Label>
                <Input
                  v-model="clientSecret"
                  data-testid="login-client-secret"
                  type="password"
                  :placeholder="JBM_DEFAULT_CLIENT_SECRET"
                />
              </div>
            </div>
          </details>

          <div
            v-if="error"
            role="alert"
            class="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {{ error }}
          </div>

          <Button type="submit" class="w-full" :disabled="loading || (captchaLoading && activeTab === 'PASSWORD')">
            {{ loading ? '登录中…' : '登录' }}
          </Button>
          <p v-if="isDev" class="text-xs text-muted-foreground">
            当前默认 Client：<code class="rounded bg-muted px-1">{{ clientId }}</code>。
            若提示无效 client_secret，先点下方恢复种子或确认 .env 与库一致。
          </p>
          <Button
            v-if="isDev"
            type="button"
            variant="outline"
            class="w-full text-xs"
            :disabled="seedResetLoading"
            @click="onResetJaja7Seed"
          >
            {{ seedResetLoading ? '恢复中…' : '恢复 JBM 种子应用凭证（需 Auth 已重启加载新代码）' }}
          </Button>

          <details v-if="!showMoreModes" class="rounded-md border px-3 py-2 text-sm">
            <summary class="cursor-pointer font-medium text-muted-foreground">更多登录方式</summary>
            <div class="mt-3 flex flex-wrap gap-2">
              <button
                v-for="tab in moreLoginTabs"
                :key="tab.id"
                type="button"
                class="rounded-md border px-3 py-1.5 text-xs hover:bg-muted"
                @click="selectLoginTab(tab.id)"
              >
                {{ tab.label }}
              </button>
            </div>
          </details>

          <p class="text-center text-sm text-muted-foreground">
            没有账号？
            <RouterLink to="/register" class="font-medium text-primary hover:underline">立即注册</RouterLink>
          </p>
        </form>

        <div v-else-if="activeTab === 'AUTH_CODE'" class="mt-4 space-y-4">
          <p class="text-sm text-muted-foreground">
            标准 OAuth2 授权码流程：跳转授权页登录并确认 → 回调携带
            <code class="rounded bg-muted px-1 text-xs">code</code>
            → 使用
            <code class="rounded bg-muted px-1 text-xs">grant_type=authorization_code</code>
            换取 Token。
          </p>
          <div class="grid grid-cols-2 gap-3">
            <div class="col-span-2 space-y-2">
              <Label>Client ID</Label>
              <Input v-model="clientId" data-testid="login-client-id" />
            </div>
            <div class="col-span-2 space-y-2">
              <Label>Client Secret</Label>
              <Input v-model="clientSecret" data-testid="login-client-secret" type="password" />
            </div>
            <div class="col-span-2 space-y-2">
              <Label>redirect_uri（须与应用中登记一致）</Label>
              <Input v-model="oauthRedirectUri" placeholder="http://localhost:5173/login/callback" />
            </div>
            <div class="space-y-2">
              <Label>scope</Label>
              <Input v-model="oauthScope" placeholder="all" />
            </div>
            <div class="space-y-2">
              <Label>state</Label>
              <Input v-model="oauthState" placeholder="跳转时自动生成" />
            </div>
          </div>
          <Button type="button" class="w-full" @click="startOAuthCodeLogin">
            1. 跳转 OAuth2 授权页（/oauth2/authorize）
          </Button>
          <details class="rounded-md border px-3 py-2 text-xs text-muted-foreground">
            <summary class="cursor-pointer font-medium">预览授权 URL</summary>
            <p class="mt-2 break-all font-mono">{{ authorizeUrlPreview }}</p>
          </details>
          <div class="relative py-1 text-center text-xs text-muted-foreground">
            <span class="bg-card px-2">或手动粘贴回调中的 code</span>
          </div>
          <div class="space-y-2">
            <Label>授权码 code</Label>
            <Input v-model="authCode" placeholder="回调 URL ?code= 参数" />
          </div>
          <Button type="button" class="w-full" variant="outline" :disabled="loading" @click="onAuthCodeExchange">
            2. 用授权码换 Token（POST /oauth2/token）
          </Button>
          <div
            v-if="error"
            role="alert"
            class="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {{ error }}
          </div>
        </div>

        <div v-else-if="activeTab === 'SCAN'" class="space-y-4">
          <p class="text-sm text-muted-foreground">
            请使用已登录 JBM 的移动端扫描下方二维码，并在手机上确认登录。
          </p>
          <div class="flex justify-center rounded-md border bg-white p-4">
            <img
              v-if="qrImage"
              :src="qrImage"
              alt="登录二维码"
              class="size-[200px] object-contain"
            />
            <span v-else class="flex size-[200px] items-center justify-center text-sm text-muted-foreground">
              加载二维码…
            </span>
          </div>
          <p class="text-center text-xs text-muted-foreground">
            {{ qrPolling ? '等待扫码确认…' : '二维码已过期可刷新' }}
          </p>
          <Button type="button" class="w-full" variant="outline" :disabled="loading" @click="loadQrSession">
            刷新二维码
          </Button>
          <details class="rounded-md border px-3 py-2 text-sm">
            <summary class="cursor-pointer font-medium text-muted-foreground">OAuth2 客户端</summary>
            <div class="mt-3 space-y-2">
              <Label>Client ID</Label>
              <Input v-model="clientId" />
            </div>
          </details>
          <div
            v-if="error"
            role="alert"
            class="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {{ error }}
          </div>
        </div>

        <div v-else class="space-y-4">
          <p class="text-sm text-muted-foreground">
            使用已在 bootstrap 中配置的第三方 IdP（如 local），将回调中的 code 交给 Auth 服务映射为系统用户。
          </p>
          <div class="space-y-2">
            <Label>Client ID</Label>
            <Input v-model="clientId" />
          </div>
          <div class="relative py-2 text-center text-xs text-muted-foreground">
            <span class="bg-card px-2">第三方授权码</span>
          </div>
          <div class="space-y-2">
            <Label>提供商</Label>
            <select
              v-model="tpProvider"
              class="flex h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
            >
              <option v-for="p in thirdPartyProviders" :key="p" :value="p">{{ p }}</option>
            </select>
          </div>
          <div class="space-y-2">
            <Label>授权码 code</Label>
            <Input v-model="tpCode" placeholder="第三方回调 URL 中的 code" />
          </div>
          <Button type="button" class="w-full" :disabled="loading" @click="onThirdPartyManual">
            {{ loading ? '登录中…' : '第三方 code 换 Token' }}
          </Button>
          <div
            v-if="error"
            role="alert"
            class="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {{ error }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
