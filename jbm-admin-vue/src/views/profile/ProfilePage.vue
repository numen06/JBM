<script setup lang="ts">
import KeyCredentialsPanel from '@/components/KeyCredentialsPanel.vue'
import { authCenterUrl } from '@/api/auth'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Camera, Mail, RotateCcw, Save, Smartphone } from '@lucide/vue'
import AvatarCropDialog from '@/components/profile/AvatarCropDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import Input from '@/components/ui/Input.vue'
import {
  bindEmail,
  bindMobile,
  sendEmailBindCode,
  sendMobileBindCode,
  updateCurrentUser,
} from '@/api/current'
import { fetchSmsCodeConfig } from '@/api/captcha'
import { uploadDoc } from '@/api/doc'
import { useDocImageSrc } from '@/composables/useDocImageSrc'
import { useFeedback } from '@/composables/useFeedback'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const feedback = useFeedback()
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarFile = ref<File | null>(null)
const avatarPreviewUrl = ref('')
const cropDialogOpen = ref(false)
const cropSourceFile = ref<File | null>(null)
const activeTab = ref<'information' | 'authentication'>('information')
const informationTab = ref<HTMLButtonElement | null>(null)
const authenticationTab = ref<HTMLButtonElement | null>(null)
const saving = ref(false)
const formError = ref('')
const debugBypass = ref(false)
const mobileTarget = ref('')
const mobileCode = ref('')
const mobileCodeSent = ref(false)
const mobileSending = ref(false)
const mobileBinding = ref(false)
const emailTarget = ref('')
const emailCode = ref('')
const emailCodeSent = ref(false)
const emailSending = ref(false)
const emailBinding = ref(false)

const form = reactive({
  nickName: '',
  realName: '',
  avatar: '',
})

const roleLabel = computed(() => {
  const roles = auth.user?.roles ?? []
  if (!roles.length) return '未分配角色'
  return roles.map((role) => role.roleName || role.roleCode).filter(Boolean).join('、')
})

const displayName = computed(() => auth.user?.nickName || auth.user?.userName || '管理员')
const avatarInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const savedAvatarSrc = useDocImageSrc(computed(() => (avatarPreviewUrl.value ? '' : form.avatar)))
const avatarSrc = computed(() => avatarPreviewUrl.value || savedAvatarSrc.value)

function fillFormFromUser() {
  form.nickName = auth.user?.nickName ?? ''
  form.realName = auth.user?.realName ?? ''
  form.avatar = auth.user?.avatar ?? ''
  mobileTarget.value = auth.user?.mobile ?? ''
  emailTarget.value = auth.user?.email ?? ''
  resetPendingAvatar()
  formError.value = ''
}

async function sendMobileCode() {
  formError.value = ''
  if (!/^1\d{10}$/.test(mobileTarget.value.trim())) {
    formError.value = '请输入正确的手机号'
    return
  }
  mobileSending.value = true
  try {
    await sendMobileBindCode(mobileTarget.value.trim())
    mobileCodeSent.value = true
    if (debugBypass.value) mobileCode.value = '99999'
    feedback.toast.success('手机验证码已发送')
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '手机验证码发送失败'
  } finally {
    mobileSending.value = false
  }
}

async function confirmMobileBind() {
  if (!mobileCode.value.trim()) {
    formError.value = '请输入手机验证码'
    return
  }
  mobileBinding.value = true
  formError.value = ''
  try {
    await bindMobile(mobileTarget.value.trim(), mobileCode.value.trim())
    await auth.fetchUser()
    mobileCode.value = ''
    mobileCodeSent.value = false
    feedback.toast.success('手机号绑定成功')
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '手机号绑定失败'
  } finally {
    mobileBinding.value = false
  }
}

async function sendEmailCode() {
  formError.value = ''
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(emailTarget.value.trim())) {
    formError.value = '请输入正确的邮箱'
    return
  }
  emailSending.value = true
  try {
    await sendEmailBindCode(emailTarget.value.trim())
    emailCodeSent.value = true
    if (debugBypass.value) emailCode.value = '99999'
    feedback.toast.success('邮箱验证码已发送')
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '邮箱验证码发送失败'
  } finally {
    emailSending.value = false
  }
}

async function confirmEmailBind() {
  if (!emailCode.value.trim()) {
    formError.value = '请输入邮箱验证码'
    return
  }
  emailBinding.value = true
  formError.value = ''
  try {
    await bindEmail(emailTarget.value.trim(), emailCode.value.trim())
    await auth.fetchUser()
    emailCode.value = ''
    emailCodeSent.value = false
    feedback.toast.success('邮箱绑定成功')
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '邮箱绑定失败'
  } finally {
    emailBinding.value = false
  }
}

function chooseAvatar() {
  avatarInput.value?.click()
}

function handleAvatarChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    formError.value = '请选择图片文件'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    formError.value = '头像图片不能超过 2MB'
    return
  }
  resetPendingAvatar()
  cropSourceFile.value = file
  cropDialogOpen.value = true
  formError.value = ''
}

function handleAvatarCropped(file: File) {
  resetPendingAvatar()
  avatarFile.value = file
  avatarPreviewUrl.value = URL.createObjectURL(file)
  cropSourceFile.value = null
}

function handleCropDialogClose(open: boolean) {
  cropDialogOpen.value = open
  if (!open) {
    cropSourceFile.value = null
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

async function saveProfile() {
  saving.value = true
  formError.value = ''
  try {
    let avatar = form.avatar
    if (avatarFile.value) {
      avatar = await uploadDoc(avatarFile.value, 'avatars')
    }
    await updateCurrentUser({
      nickName: form.nickName.trim() || undefined,
      realName: form.realName.trim() || undefined,
      avatar: avatar || undefined,
    })
    await auth.fetchUser()
    fillFormFromUser()
    feedback.toast.success('个人信息已保存')
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function resetPendingAvatar() {
  avatarFile.value = null
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
    avatarPreviewUrl.value = ''
  }
  if (avatarInput.value) avatarInput.value.value = ''
}

watch(() => auth.user, fillFormFromUser, { immediate: true })
onMounted(async () => {
  try {
    debugBypass.value = (await fetchSmsCodeConfig()).debugBypass
  } catch {
    debugBypass.value = false
  }
})

onBeforeUnmount(resetPendingAvatar)
</script>

<template>
  <div class="profile-page">
    <PageHeader title="用户中心" description="管理个人信息与账号认证">
      <template v-if="activeTab === 'information'" #actions>
        <Button variant="outline" :disabled="saving" @click="fillFormFromUser">
          <RotateCcw class="h-4 w-4" />
          重置
        </Button>
        <Button :disabled="saving" @click="saveProfile">
          <Save class="h-4 w-4" />
          {{ saving ? '保存中...' : '保存' }}
        </Button>
      </template>
    </PageHeader>
    <div class="profile-tabs" role="tablist" aria-label="用户中心功能">
      <button id="profile-information-tab" ref="informationTab" type="button" role="tab" :aria-selected="activeTab === 'information'" aria-controls="profile-information-panel" :tabindex="activeTab === 'information' ? 0 : -1" @click="activeTab = 'information'" @keydown.right.prevent="activeTab = 'authentication'; authenticationTab?.focus()" @keydown.left.prevent="activeTab = 'authentication'; authenticationTab?.focus()" @keydown.end.prevent="activeTab = 'authentication'; authenticationTab?.focus()">个人信息</button>
      <button id="profile-authentication-tab" ref="authenticationTab" type="button" role="tab" :aria-selected="activeTab === 'authentication'" aria-controls="profile-authentication-panel" :tabindex="activeTab === 'authentication' ? 0 : -1" @click="activeTab = 'authentication'" @keydown.right.prevent="activeTab = 'information'; informationTab?.focus()" @keydown.left.prevent="activeTab = 'information'; informationTab?.focus()" @keydown.home.prevent="activeTab = 'information'; informationTab?.focus()">认证管理</button>
    </div>
    <p v-if="formError" class="mb-4 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
      {{ formError }}
    </p>

    <section v-show="activeTab === 'information'" id="profile-information-panel" role="tabpanel" aria-labelledby="profile-information-tab" tabindex="0">
    <div class="profile-information-grid">
      <Card>
        <CardHeader>
          <CardTitle>账号</CardTitle>
        </CardHeader>
        <CardContent class="space-y-5">
          <div class="flex flex-col items-center gap-3">
            <img
              v-if="avatarSrc"
              :src="avatarSrc"
              alt="头像"
              class="h-28 w-28 rounded-full border object-cover"
            />
            <div
              v-else
              class="flex h-28 w-28 items-center justify-center rounded-full border bg-muted text-3xl font-semibold"
            >
              {{ avatarInitial }}
            </div>
            <input
              ref="avatarInput"
              type="file"
              accept="image/*"
              class="hidden"
              @change="handleAvatarChange"
            />
            <Button variant="outline" size="sm" @click="chooseAvatar">
              <Camera class="h-4 w-4" />
              上传头像
            </Button>
          </div>

          <div class="space-y-3 text-sm">
            <div>
              <p class="text-xs text-muted-foreground">用户名</p>
              <p class="font-medium">{{ auth.user?.userName || '-' }}</p>
            </div>
            <div>
              <p class="text-xs text-muted-foreground">显示名称</p>
              <p class="font-medium">{{ displayName }}</p>
            </div>
            <div>
              <p class="text-xs text-muted-foreground">角色</p>
              <p class="leading-6">{{ roleLabel }}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>基础信息</CardTitle>
        </CardHeader>
        <CardContent class="space-y-4">
          <FormField label="昵称">
            <Input v-model="form.nickName" placeholder="用于后台显示" />
          </FormField>
          <FormField label="真实姓名">
            <Input v-model="form.realName" placeholder="真实姓名" />
          </FormField>
          <div class="grid gap-4 sm:grid-cols-2">
            <FormField label="手机号">
              <Input :model-value="auth.user?.mobile || ''" disabled placeholder="未设置" />
            </FormField>
            <FormField label="邮箱">
              <Input :model-value="auth.user?.email || ''" disabled placeholder="未设置" />
            </FormField>
          </div>
          <p class="text-xs text-muted-foreground">
            手机号和邮箱由用户管理或账号绑定流程维护，当前页面只保存个人资料。
          </p>
        </CardContent>
      </Card>
    </div>

    </section>
    <section v-show="activeTab === 'authentication'" id="profile-authentication-panel" role="tabpanel" aria-labelledby="profile-authentication-tab" tabindex="0">
    <Card>
      <CardHeader>
        <CardTitle>账号绑定</CardTitle>
      </CardHeader>
      <CardContent class="space-y-6">
        <p class="text-sm text-muted-foreground">
          手机和邮箱不参与用户名注册。首次绑定或更换时，必须输入发送到目标凭证的验证码。
          <span v-if="debugBypass">当前为调试模式，验证码为 99999。</span>
        </p>

        <div class="profile-bindings-grid">
          <section class="space-y-3 rounded-lg border p-4">
            <div class="flex items-center gap-2 font-medium">
              <Smartphone class="h-4 w-4" />
              手机号
            </div>
            <p class="text-xs text-muted-foreground">当前绑定：{{ auth.user?.mobile || '未绑定' }}</p>
            <div class="flex gap-2">
              <Input v-model="mobileTarget" type="tel" maxlength="11" placeholder="请输入目标手机号" autocomplete="tel" />
              <Button variant="outline" class="shrink-0" :disabled="mobileSending" @click="sendMobileCode">
                {{ mobileSending ? '发送中…' : '发送验证码' }}
              </Button>
            </div>
            <div v-if="mobileCodeSent" class="flex gap-2">
              <Input v-model="mobileCode" maxlength="8" placeholder="手机验证码" autocomplete="one-time-code" />
              <Button class="shrink-0" :disabled="mobileBinding" @click="confirmMobileBind">
                {{ mobileBinding ? '确认中…' : '确认绑定' }}
              </Button>
            </div>
          </section>

          <section class="space-y-3 rounded-lg border p-4">
            <div class="flex items-center gap-2 font-medium">
              <Mail class="h-4 w-4" />
              邮箱
            </div>
            <p class="text-xs text-muted-foreground">当前绑定：{{ auth.user?.email || '未绑定' }}</p>
            <div class="flex gap-2">
              <Input v-model="emailTarget" type="email" placeholder="请输入目标邮箱" autocomplete="email" />
              <Button variant="outline" class="shrink-0" :disabled="emailSending" @click="sendEmailCode">
                {{ emailSending ? '发送中…' : '发送验证码' }}
              </Button>
            </div>
            <div v-if="emailCodeSent" class="flex gap-2">
              <Input v-model="emailCode" maxlength="8" placeholder="邮箱验证码" autocomplete="one-time-code" />
              <Button class="shrink-0" :disabled="emailBinding" @click="confirmEmailBind">
                {{ emailBinding ? '确认中…' : '确认绑定' }}
              </Button>
            </div>
          </section>
        </div>
      </CardContent>
    </Card>

    <div class="profile-credentials-grid">
      <KeyCredentialsPanel kind="PASSKEY" :auth-base="authCenterUrl('')" :access-token="auth.accessToken" />
      <KeyCredentialsPanel kind="SSH_KEY" :auth-base="authCenterUrl('')" :access-token="auth.accessToken" />
    </div>

    </section>

    <AvatarCropDialog
      :open="cropDialogOpen"
      :file="cropSourceFile"
      @update:open="handleCropDialogClose"
      @confirm="handleAvatarCropped"
    />
  </div>
</template>

<style scoped>
.profile-page{min-width:0;container-type:inline-size}
.profile-tabs{display:flex;gap:24px;border-bottom:1px solid var(--color-border);margin-bottom:20px}
.profile-tabs button{padding:12px 2px;border-bottom:2px solid transparent;color:var(--color-muted-foreground);font-size:14px;font-weight:500;white-space:nowrap}
.profile-tabs button[aria-selected=true]{border-bottom-color:var(--color-primary);color:var(--color-primary)}
.profile-tabs button:focus-visible{outline:2px solid var(--color-ring);outline-offset:3px;border-radius:4px}
.profile-information-grid,.profile-credentials-grid,.profile-bindings-grid{display:grid;gap:16px;min-width:0}
.profile-information-grid>*{min-width:0}.profile-credentials-grid{margin-top:16px}.profile-credentials-grid :deep(section){margin:0;min-width:0}
.profile-bindings-grid :deep(input){min-width:0}
@container(min-width:760px){.profile-information-grid{grid-template-columns:280px minmax(0,1fr)}.profile-credentials-grid,.profile-bindings-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
