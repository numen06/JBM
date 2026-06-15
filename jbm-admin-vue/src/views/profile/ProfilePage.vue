<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Camera, RotateCcw, Save } from '@lucide/vue'
import AvatarCropDialog from '@/components/profile/AvatarCropDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import Input from '@/components/ui/Input.vue'
import { updateCurrentUser } from '@/api/current'
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
const saving = ref(false)
const formError = ref('')

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
  resetPendingAvatar()
  formError.value = ''
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

onBeforeUnmount(resetPendingAvatar)
</script>

<template>
  <div>
    <PageHeader title="个人中心" description="编辑当前登录用户的基础资料和头像">
      <template #actions>
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

    <div class="grid gap-4 lg:grid-cols-[280px_minmax(0,1fr)]">
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
          <p v-if="formError" class="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {{ formError }}
          </p>
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

    <AvatarCropDialog
      :open="cropDialogOpen"
      :file="cropSourceFile"
      @update:open="handleCropDialogClose"
      @confirm="handleAvatarCropped"
    />
  </div>
</template>
