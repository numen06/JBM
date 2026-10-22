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
  updatePassword,
} from '@/api/current'
import { fetchSmsCodeConfig } from '@/api/captcha'
import { uploadDoc } from '@/api/doc'
import { useDocImageSrc } from '@/composables/useDocImageSrc'
import { useFeedback } from '@/composables/useFeedback'
import { useAuthStore } from '@/stores/auth'
import { passwordPolicyError } from '@/lib/passwordPolicy'

const auth = useAuthStore()
const feedback = useFeedback()
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarFile = ref<File | null>(null)
const avatarPreviewUrl = ref('')
const cropDialogOpen = ref(false)
const cropSourceFile = ref<File | null>(null)
const profileTabs = [
  { id: 'information', label: '个人资料', description: '管理头像、昵称和真实姓名' },
  { id: 'authentication', label: '账号安全', description: '修改登录密码，绑定手机和邮箱' },
  { id: 'keys', label: '登录密钥', description: '下载密钥文件，选择文件即可登录对应账号' },
] as const
const activeTab = ref<(typeof profileTabs)[number]['id']>('information')
const tabButtons = ref<HTMLButtonElement[]>([])
function navigateTabs(event: KeyboardEvent) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const current = profileTabs.findIndex(tab => tab.id === activeTab.value)
  const index = event.key === 'Home' ? 0 : event.key === 'End' ? profileTabs.length - 1
    : (current + (event.key === 'ArrowRight' ? 1 : -1) + profileTabs.length) % profileTabs.length
  activeTab.value = profileTabs[index]!.id
  tabButtons.value[index]?.focus()
}
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
const passwordForm = reactive({ originPassword: '', currentPassword: '', confirmPassword: '' })
const passwordSaving = ref(false)
const passwordError = ref('')
const passwordNotice = ref('')

async function changePassword() {
  if (passwordSaving.value) return
  passwordError.value = ''
  passwordNotice.value = ''
  if (!passwordForm.originPassword || !passwordForm.currentPassword || !passwordForm.confirmPassword) {
    passwordError.value = '请填写当前密码、新密码和确认密码'
    return
  }
  const policyError = passwordPolicyError(passwordForm.currentPassword)
  if (policyError) {
    passwordError.value = policyError
    return
  }
  if (passwordForm.currentPassword !== passwordForm.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  if (passwordForm.currentPassword === passwordForm.originPassword) {
    passwordError.value = '新密码不能与当前密码相同'
    return
  }
  passwordSaving.value = true
  try {
    await updatePassword({ ...passwordForm })
    auth.clearMustChangePassword()
    passwordForm.originPassword = ''
    passwordForm.currentPassword = ''
    passwordForm.confirmPassword = ''
    passwordNotice.value = '密码修改成功，下次登录请使用新密码'
  } catch (error) {
    passwordError.value = error instanceof Error ? error.message : '修改密码失败，请稍后重试'
  } finally {
    passwordSaving.value = false
  }
}

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
    <PageHeader title="个人中心" description="管理个人资料、账号安全与登录方式">
      <template v-if="activeTab === 'information'" #actions>
        <Button variant="outline" :disabled="saving" @click="fillFormFromUser">
          <RotateCcw class="h-4 w-4" />
          重置
        </Button>
        <Button :disabled="saving" @click="saveProfile">
          <Save class="h-4 w-4" />
          {{ saving ? '保存中...' : '保存资料' }}
        </Button>
      </template>
    </PageHeader>
    <div class="profile-tabs" role="tablist" aria-label="用户中心功能">
      <button v-for="tab in profileTabs" :id="`profile-${tab.id}-tab`" :key="tab.id" ref="tabButtons" type="button" role="tab" :aria-selected="activeTab === tab.id" :aria-controls="`profile-${tab.id}-panel`" :tabindex="activeTab === tab.id ? 0 : -1" @click="activeTab = tab.id" @keydown="navigateTabs">{{ tab.label }}</button>
    </div>
    <p class="profile-tab-description">{{ profileTabs.find(tab => tab.id === activeTab)?.description }}</p>
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
            绑定或更换手机号、邮箱，请前往“账号安全”。
          </p>
        </CardContent>
      </Card>
    </div>

    </section>
    <section v-show="activeTab === 'authentication'" id="profile-authentication-panel" role="tabpanel" aria-labelledby="profile-authentication-tab" tabindex="0">
    <div class="profile-security-grid">
    <Card>
      <CardHeader><CardTitle>修改密码</CardTitle></CardHeader>
      <CardContent>
        <form class="max-w-xl space-y-4" @submit.prevent="changePassword">
          <FormField label="当前密码" for-id="profile-current-password" required>
            <Input id="profile-current-password" v-model="passwordForm.originPassword" type="password" autocomplete="current-password" placeholder="请输入当前登录密码" :disabled="passwordSaving" required />
          </FormField>
          <FormField label="新密码" for-id="profile-new-password" required>
            <Input id="profile-new-password" v-model="passwordForm.currentPassword" type="password" autocomplete="new-password" minlength="8" maxlength="128" placeholder="请输入新密码" :disabled="passwordSaving" required aria-describedby="profile-password-policy" />
          </FormField>
          <FormField label="确认新密码" for-id="profile-confirm-password" required>
            <Input id="profile-confirm-password" v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" maxlength="128" placeholder="请再次输入新密码" :disabled="passwordSaving" required />
          </FormField>
          <p id="profile-password-policy" class="text-xs leading-5 text-muted-foreground">8–128 位，至少包含大写字母、小写字母、数字、特殊字符中的 3 类。</p>
          <p v-if="passwordError" role="alert" class="text-sm text-destructive">{{ passwordError }}</p>
          <p v-if="passwordNotice" role="status" class="text-sm text-primary">{{ passwordNotice }}</p>
          <Button type="submit" :disabled="passwordSaving">{{ passwordSaving ? '正在修改…' : '修改密码' }}</Button>
        </form>
      </CardContent>
    </Card>
    <Card>
      <CardHeader>
        <CardTitle>账号绑定</CardTitle>
      </CardHeader>
      <CardContent class="space-y-6">
        <p class="text-sm text-muted-foreground">
          绑定手机或邮箱后可用于账号验证。更换绑定时，请完成新手机或邮箱的验证码验证。
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
    </div>

    </section>

    <section v-show="activeTab === 'keys'" id="profile-keys-panel" role="tabpanel" aria-labelledby="profile-keys-tab" tabindex="0">
    <div class="profile-key-overview">
      <div><h2>密钥文件登录</h2><p>为不同账号分别保存密钥文件，在登录页选择文件即可进入对应账号。</p></div>
      <span class="profile-account-badge">当前账号：{{ auth.user?.userName || '-' }}</span>
    </div>
    <div class="profile-file-key">
      <KeyCredentialsPanel kind="FILE_KEY" :auth-base="authCenterUrl('')" :access-token="auth.accessToken" />
    </div>
    <details class="profile-advanced-keys">
      <summary>其他登录方式 <span>通行密钥与 SSH 签名</span></summary>
      <div class="profile-credentials-grid">
        <KeyCredentialsPanel kind="PASSKEY" :auth-base="authCenterUrl('')" :access-token="auth.accessToken" />
        <KeyCredentialsPanel kind="SSH_KEY" :auth-base="authCenterUrl('')" :access-token="auth.accessToken" />
      </div>
    </details>

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
.profile-tabs{display:flex;gap:24px;border-bottom:1px solid var(--color-border);margin-bottom:12px;overflow-x:auto}
.profile-tab-description{margin:0 0 20px;color:var(--color-muted-foreground);font-size:13px}
.profile-tabs button{padding:12px 2px;border-bottom:2px solid transparent;color:var(--color-muted-foreground);font-size:14px;font-weight:500;white-space:nowrap}
.profile-tabs button[aria-selected=true]{border-bottom-color:var(--color-primary);color:var(--color-primary)}
.profile-tabs button:focus-visible{outline:2px solid var(--color-ring);outline-offset:3px;border-radius:4px}
.profile-information-grid,.profile-credentials-grid,.profile-bindings-grid,.profile-security-grid{display:grid;gap:16px;min-width:0}
.profile-information-grid>*{min-width:0}.profile-credentials-grid{margin-top:16px}.profile-credentials-grid :deep(section){margin:0;min-width:0}
.profile-bindings-grid :deep(input){min-width:0}
.profile-key-overview{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;flex-wrap:wrap;margin-bottom:16px}
.profile-key-overview h2{font-size:16px;font-weight:600}.profile-key-overview p{margin-top:6px;font-size:13px;line-height:1.7;color:var(--color-muted-foreground)}
.profile-account-badge{border:1px solid var(--color-border);border-radius:6px;padding:6px 10px;font-size:12px;white-space:nowrap}
.profile-file-key :deep(section){margin:0;padding:24px}.profile-file-key :deep(label){max-width:480px}.profile-file-key :deep(ul){margin-top:20px}
.profile-advanced-keys{margin-top:20px;border-top:1px solid var(--color-border);padding-top:16px}.profile-advanced-keys summary{cursor:pointer;font-size:13px;font-weight:500}.profile-advanced-keys summary span{margin-left:8px;color:var(--color-muted-foreground);font-weight:400}
@container(min-width:760px){.profile-information-grid{grid-template-columns:280px minmax(0,1fr)}.profile-credentials-grid,.profile-bindings-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
@container(min-width:1100px){.profile-security-grid{grid-template-columns:minmax(320px,.85fr) minmax(0,1.4fr)}}
</style>
