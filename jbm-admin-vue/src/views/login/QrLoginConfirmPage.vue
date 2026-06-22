<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { fetchCaptchaBase64, sendSmsCode } from '@/api/captcha'
import { confirmQrLogin, markQrScanned } from '@/api/qrcode'
import JbmLogo from '@/components/JbmLogo.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Label from '@/components/ui/Label.vue'
import {
  DEV_CAPTCHA_CODE,
  DEV_SMS_CODE,
  JBM_DEFAULT_PASSWORD,
  JBM_DEFAULT_USERNAME,
  type OAuthLoginType,
} from '@/constants/loginModes'
import { extractApiError } from '@/lib/errors'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

const loginTabs: Array<{ id: OAuthLoginType; label: string }> = [
  { id: 'PASSWORD', label: '密码' },
  { id: 'SMS', label: '短信' },
  { id: 'FACE', label: '人脸' },
  { id: 'WECHAT', label: '微信' },
  { id: 'MINIAPP', label: '小程序' },
]
const activeTab = ref<OAuthLoginType>('PASSWORD')
const username = ref(JBM_DEFAULT_USERNAME)
const password = ref(JBM_DEFAULT_PASSWORD)
const vcode = ref(DEV_CAPTCHA_CODE)
const captchaSrc = ref('')
const captchaLoading = ref(false)
const smsSending = ref(false)
const smsCooldown = ref(0)
let smsTimer: ReturnType<typeof setInterval> | undefined
const phone = ref('')
const smsCode = ref('')
const facePhone = ref('')
const faceImage = ref('')
const wechatOpenId = ref('')
const wechatCode = ref('')
const miniappPhone = ref('')
const miniappCode = ref('')
const loading = ref(false)
const scanned = ref(false)
const confirmed = ref(false)
const error = ref('')
const notice = ref('')

const qrCode = computed(() => String(route.query.code || '').trim())

async function loadCaptcha() {
  if (activeTab.value !== 'PASSWORD' && activeTab.value !== 'SMS') return
  captchaLoading.value = true
  try {
    captchaSrc.value = await fetchCaptchaBase64(120, 40)
  } catch (e) {
    error.value = extractApiError(e, '验证码加载失败')
  } finally {
    captchaLoading.value = false
  }
}

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

async function sendSms() {
  error.value = ''
  notice.value = ''
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
    notice.value = '短信验证码已发送'
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

function onFaceFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    faceImage.value = String(reader.result ?? '')
  }
  reader.readAsDataURL(file)
}

async function loginForConfirm() {
  if (activeTab.value === 'PASSWORD') {
    if (!username.value.trim()) throw new Error('请输入用户名')
    if (!password.value) throw new Error('请输入密码')
    await auth.login(username.value.trim(), password.value, {
      vcode: vcode.value.trim(),
      loginType: 'PASSWORD',
    })
    return
  }
  if (activeTab.value === 'SMS') {
    if (!phone.value.trim()) throw new Error('请输入手机号')
    if (!smsCode.value.trim()) throw new Error('请输入短信验证码')
    await auth.login(phone.value.trim(), smsCode.value.trim(), {
      vcode: vcode.value.trim() || undefined,
      loginType: 'SMS',
    })
    return
  }
  if (activeTab.value === 'FACE') {
    if (!facePhone.value.trim()) throw new Error('请输入手机号')
    if (!faceImage.value) throw new Error('请上传人脸照片')
    await auth.login(facePhone.value.trim(), faceImage.value, { loginType: 'FACE' })
    return
  }
  if (activeTab.value === 'WECHAT') {
    if (!wechatOpenId.value.trim() || !wechatCode.value.trim()) throw new Error('请填写 OpenID 与微信 code')
    await auth.login(wechatOpenId.value.trim(), wechatCode.value.trim(), { loginType: 'WECHAT' })
    return
  }
  if (activeTab.value === 'MINIAPP') {
    if (!miniappPhone.value.trim() || !miniappCode.value.trim()) throw new Error('请填写手机号与小程序 code')
    await auth.login(miniappPhone.value.trim(), miniappCode.value.trim(), { loginType: 'MINIAPP' })
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
      await loginForConfirm()
    }
    await confirmQrLogin(qrCode.value)
    confirmed.value = true
  } catch (e) {
    error.value = extractApiError(e, '确认登录失败')
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => {
  error.value = ''
  notice.value = ''
  loadCaptcha()
})

onMounted(() => {
  markScanned()
  loadCaptcha()
})

onUnmounted(() => {
  if (smsTimer) clearInterval(smsTimer)
})
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-muted/20 px-4 py-8">
    <section class="w-full max-w-sm space-y-5 rounded-lg border bg-background px-5 py-6 shadow-sm">
      <div class="space-y-4 text-center">
        <div class="flex flex-col items-center gap-3">
          <JbmLogo class="size-14 rounded-xl shadow-sm" alt="JBM" />
          <div class="space-y-1">
            <p class="text-sm font-medium text-muted-foreground">JBM 管理后台</p>
            <h1 class="text-2xl font-semibold tracking-tight">认证中心</h1>
          </div>
        </div>
        <p class="text-sm text-muted-foreground">
          {{ confirmed ? '电脑端正在完成登录' : scanned ? '扫码登录确认' : '正在读取二维码' }}
        </p>
      </div>

      <div v-if="confirmed" class="rounded-md border border-emerald-500/30 bg-emerald-500/10 px-3 py-3 text-sm text-emerald-700">
        已确认登录，请回到电脑端继续使用。
      </div>

      <form v-else class="space-y-4" @submit.prevent="confirmLogin">
        <div v-if="!auth.accessToken" class="space-y-4">
          <div class="flex flex-wrap gap-1 rounded-lg border bg-muted/30 p-1" role="tablist" aria-label="登录方式">
            <button
              v-for="tab in loginTabs"
              :key="tab.id"
              type="button"
              role="tab"
              :aria-selected="activeTab === tab.id"
              class="rounded-md px-2.5 py-1.5 text-xs font-medium transition-colors"
              :class="activeTab === tab.id ? 'bg-background text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'"
              @click="activeTab = tab.id"
            >
              {{ tab.label }}
            </button>
          </div>

          <template v-if="activeTab === 'PASSWORD'">
            <div class="space-y-2">
              <Label>用户名</Label>
              <Input v-model="username" autocomplete="username" placeholder="admin" />
            </div>
            <div class="space-y-2">
              <Label>密码</Label>
              <Input v-model="password" autocomplete="current-password" type="password" />
            </div>
          </template>

          <template v-else-if="activeTab === 'SMS'">
            <div class="space-y-2">
              <Label>手机号</Label>
              <Input v-model="phone" autocomplete="tel" placeholder="11 位手机号" type="tel" />
            </div>
            <div class="space-y-2">
              <Label>短信验证码</Label>
              <div class="flex gap-2">
                <Input v-model="smsCode" class="flex-1" autocomplete="one-time-code" placeholder="验证码" />
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
              <p class="text-xs text-muted-foreground">短信开发直通码：{{ DEV_SMS_CODE }}</p>
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
                @change="onFaceFileChange"
              />
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
              <Input v-model="vcode" class="flex-1" autocomplete="off" placeholder="9999" />
              <button
                type="button"
                class="relative flex h-10 w-[120px] shrink-0 items-center justify-center overflow-hidden rounded-md border bg-muted/50"
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
            v-if="notice"
            role="status"
            class="rounded-md border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-700"
          >
            {{ notice }}
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
