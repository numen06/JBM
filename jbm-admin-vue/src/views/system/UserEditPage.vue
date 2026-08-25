<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Save } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import FormField from '@/components/FormField.vue'
import OrgTreeSelect from '@/components/OrgTreeSelect.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Select from '@/components/ui/Select.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import { orgRowId, useOrgTree } from '@/composables/useOrgTree'
import { useFeedback } from '@/composables/useFeedback'
import {
  getUser,
  createUser,
  updateUser,
  getUserRoles,
  getUserAccounts,
  getUserOrgs,
} from '@/api/user'
import { listAllRoles } from '@/api/role'
import { optionalSnowflakeIdParam, toSnowflakeIdString } from '@/lib/snowflakeId'
import type { BaseAccount, BaseRole, BaseUser } from '@/api/types'

const route = useRoute()
const router = useRouter()
const feedback = useFeedback()
const { flatOrgs, loadOrgs } = useOrgTree()

const isCreate = computed(() => route.name === 'user-new')
const userId = computed(() =>
  isCreate.value ? undefined : toSnowflakeIdString(route.params.userId as string),
)

const loading = ref(false)
const saving = ref(false)
const formError = ref('')
const createMode = ref<'existing' | 'new'>('existing')
const form = ref<BaseUser>({
  userName: '',
  nickName: '',
  mobile: '',
  email: '',
  status: 1,
  password: '',
  companyId: undefined,
  departmentId: undefined,
})

const allRoles = ref<BaseRole[]>([])
const selectedRoleIds = ref<string[]>([])
const selectedExtraOrgIds = ref<string[]>([])
const userAccounts = ref<BaseAccount[]>([])

const extraOrgOptions = computed(() =>
  flatOrgs.value.filter((o) => {
    const id = orgRowId(o)
    if (id == null) return false
    const primary = form.value.companyId != null && String(form.value.companyId) !== ''
      ? String(form.value.companyId)
      : null
    return primary == null || id !== primary
  }),
)

const pageTitle = computed(() => (isCreate.value ? '添加租户成员' : '编辑用户'))

const accountTypeLabel: Record<string, string> = {
  username: '用户名',
  mobile: '手机号',
  email: '邮箱',
}

onMounted(async () => {
  await loadOrgs()
  try {
    allRoles.value = await listAllRoles()
  } catch {
    allRoles.value = []
  }
  if (!isCreate.value && userId.value) {
    await loadUser(userId.value)
  }
})

async function loadUser(id: string) {
  loading.value = true
  formError.value = ''
  try {
    const [user, roles, accounts, userOrgs] = await Promise.all([
      getUser(id),
      getUserRoles(id),
      getUserAccounts(id),
      getUserOrgs(id),
    ])
    form.value = { ...user, password: '' }
    selectedRoleIds.value = roles
      .map((r) => (r.roleId != null ? String(r.roleId) : ''))
      .filter(Boolean)
    userAccounts.value = accounts
    selectedExtraOrgIds.value = userOrgs
      .map((u) => (u.orgId != null ? String(u.orgId) : ''))
      .filter(Boolean)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '用户信息加载失败'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({ name: 'users' })
}

function toggleRole(roleId?: number | string) {
  if (roleId == null) return
  const id = String(roleId)
  const idx = selectedRoleIds.value.indexOf(id)
  if (idx >= 0) selectedRoleIds.value.splice(idx, 1)
  else selectedRoleIds.value.push(id)
}

function toggleExtraOrg(orgId?: string | number) {
  if (orgId == null) return
  const id = String(orgId)
  const idx = selectedExtraOrgIds.value.indexOf(id)
  if (idx >= 0) selectedExtraOrgIds.value.splice(idx, 1)
  else selectedExtraOrgIds.value.push(id)
}

async function handleSave() {
  if (!form.value.userName?.trim()) {
    formError.value = '用户名不能为空'
    return
  }
  if (isCreate.value && createMode.value === 'new' && !form.value.password?.trim()) {
    formError.value = '新建用户须填写密码'
    return
  }
  saving.value = true
  formError.value = ''
  const companyId = optionalSnowflakeIdParam(form.value.companyId)
  const departmentId = optionalSnowflakeIdParam(form.value.departmentId)
  const orgIds = selectedExtraOrgIds.value
  try {
    if (isCreate.value) {
      await createUser({
        ...form.value,
        companyId,
        departmentId,
        orgIds,
        roleIds: selectedRoleIds.value,
        existingOnly: createMode.value === 'existing',
        ...(createMode.value === 'existing' ? { password: undefined, nickName: undefined } : {}),
      })
      feedback.toast.success(createMode.value === 'existing' ? '已有账号已加入当前租户' : '子账号已创建')
    } else if (userId.value) {
      await updateUser(userId.value, {
        nickName: form.value.nickName,
        status: form.value.status,
        companyId,
        departmentId,
        orgIds,
        ...(form.value.password ? { password: form.value.password } : {}),
        roleIds: selectedRoleIds.value,
      })
      feedback.toast.success('用户已保存')
    }
    goBack()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader :title="pageTitle" description="已有全局账号直接加入当前租户；没有账号时再创建，禁止重复建号。">
      <template #actions>
        <Button variant="outline" @click="goBack">
          <ArrowLeft class="mr-1 h-4 w-4" />
          返回列表
        </Button>
        <Button :disabled="loading || saving" @click="handleSave">
          <Save class="mr-1 h-4 w-4" />
          {{ saving ? '保存中…' : '保存' }}
        </Button>
      </template>
    </PageHeader>

    <div v-if="loading" class="py-12 text-center text-sm text-muted-foreground">加载中…</div>
    <Card v-else>
      <CardContent class="space-y-6 pt-6">
        <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>

        <section class="space-y-4">
          <h2 class="text-sm font-semibold text-muted-foreground">基本信息</h2>
          <div v-if="isCreate" class="flex w-fit rounded-md border p-1">
            <Button size="sm" :variant="createMode === 'existing' ? 'default' : 'ghost'" @click="createMode = 'existing'">添加已有账号</Button>
            <Button size="sm" :variant="createMode === 'new' ? 'default' : 'ghost'" @click="createMode = 'new'">创建新账号</Button>
          </div>
          <p v-if="isCreate && createMode === 'existing'" class="text-sm text-muted-foreground">
            输入对方已注册的用户名，只建立当前租户成员关系，不修改其密码、手机、邮箱和主租户。
          </p>
          <div class="grid gap-4 md:grid-cols-2">
            <FormField label="用户名" required>
              <Input v-model="form.userName" :disabled="!isCreate" placeholder="登录名" />
            </FormField>
            <FormField v-if="!isCreate || createMode === 'new'" label="昵称">
              <Input v-model="form.nickName" placeholder="显示名称" />
            </FormField>
            <FormField v-if="!isCreate" label="手机">
              <Input :model-value="form.mobile || ''" disabled placeholder="由用户本人验证绑定" />
            </FormField>
            <FormField v-if="!isCreate" label="邮箱">
              <Input :model-value="form.email || ''" disabled placeholder="由用户本人验证绑定" />
            </FormField>
            <FormField v-if="!isCreate || createMode === 'new'" label="状态">
              <Select v-model="form.status">
                <option :value="1">正常</option>
                <option :value="0">禁用</option>
              </Select>
            </FormField>
            <FormField v-if="!isCreate || createMode === 'new'" :label="isCreate ? '初始密码' : '新密码（留空不改）'" :required="isCreate && createMode === 'new'">
              <Input v-model="form.password" type="password" autocomplete="new-password" />
            </FormField>
          </div>
        </section>

        <section class="space-y-4">
          <h2 class="text-sm font-semibold text-muted-foreground">组织</h2>
          <div class="grid gap-4 md:grid-cols-2">
            <FormField label="所属组织">
              <OrgTreeSelect v-model="form.companyId" placeholder="— 未选择 —" />
            </FormField>
            <FormField label="部门（可选）">
              <OrgTreeSelect v-model="form.departmentId" placeholder="— 未选择 —" />
            </FormField>
          </div>
          <FormField v-if="extraOrgOptions.length" label="跨组织数据授权（可选）">
            <p class="mb-2 text-xs text-muted-foreground">
              除主组织外，可授权访问其他组织的用户数据（不含主组织本身）。
            </p>
            <div class="flex max-h-48 flex-wrap gap-2 overflow-y-auto rounded border p-3">
              <label
                v-for="o in extraOrgOptions"
                :key="`extra-${orgRowId(o)}`"
                class="flex cursor-pointer items-center gap-1.5 rounded border px-2 py-1 text-sm"
              >
                <input
                  type="checkbox"
                  :checked="selectedExtraOrgIds.includes(String(orgRowId(o)))"
                  @change="toggleExtraOrg(orgRowId(o))"
                />
                {{ o.orgName }}
              </label>
            </div>
          </FormField>
        </section>

        <section v-if="!isCreate && userAccounts.length" class="space-y-4">
          <h2 class="text-sm font-semibold text-muted-foreground">登录凭证</h2>
          <ul class="space-y-1 rounded border bg-muted/30 p-3 text-sm">
            <li v-for="acc in userAccounts" :key="acc.accountId" class="font-mono">
              <span class="text-muted-foreground">{{ accountTypeLabel[acc.accountType ?? ''] ?? acc.accountType }}：</span>
              {{ acc.account }}
            </li>
          </ul>
          <p class="text-xs text-muted-foreground">
            手机和邮箱只能由用户本人在用户中心通过验证码绑定或更换。
          </p>
        </section>

        <section v-if="allRoles.length" class="space-y-4">
          <h2 class="text-sm font-semibold text-muted-foreground">角色</h2>
          <div class="flex flex-wrap gap-2">
            <label
              v-for="r in allRoles"
              :key="r.roleId"
              class="flex cursor-pointer items-center gap-1.5 rounded border px-2 py-1 text-sm"
            >
              <input
                type="checkbox"
                :checked="selectedRoleIds.includes(String(r.roleId))"
                @change="toggleRole(r.roleId)"
              />
              {{ r.roleName }}
            </label>
          </div>
        </section>
      </CardContent>
    </Card>
  </div>
</template>
