<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Dialog from '@/components/ui/Dialog.vue'
import Badge from '@/components/ui/Badge.vue'
import { getUser, getUserRoles, getUserAccounts, getUserOrgs } from '@/api/user'
import { useOrgTree } from '@/composables/useOrgTree'
import type { SnowflakeId } from '@/lib/snowflakeId'
import type { BaseAccount, BaseRole, BaseUser } from '@/api/types'

const props = defineProps<{
  open: boolean
  userId?: SnowflakeId | null
}>()

defineEmits<{
  'update:open': [value: boolean]
}>()

const { orgLabel, loadOrgs } = useOrgTree()

const loading = ref(false)
const user = ref<BaseUser | null>(null)
const roles = ref<BaseRole[]>([])
const accounts = ref<BaseAccount[]>([])
const extraOrgIds = ref<string[]>([])

const accountTypeLabel: Record<string, string> = {
  username: '用户名',
  mobile: '手机号',
  email: '邮箱',
}

const statusLabel = computed(() => {
  if (user.value?.status === 1) return '正常'
  if (user.value?.status === 0) return '禁用'
  return '其他'
})

const statusVariant = computed(() => (user.value?.status === 1 ? 'default' : 'secondary'))

async function loadUserDetail() {
  const id = props.userId
  if (!props.open || id == null) return
  loading.value = true
  user.value = null
  roles.value = []
  accounts.value = []
  extraOrgIds.value = []
  try {
    await loadOrgs()
    const [detail, userRoles, userAccounts, userOrgs] = await Promise.all([
      getUser(id),
      getUserRoles(id),
      getUserAccounts(id),
      getUserOrgs(id),
    ])
    user.value = detail
    roles.value = userRoles
    accounts.value = userAccounts
    extraOrgIds.value = userOrgs
      .map((o) => (o.orgId != null ? String(o.orgId) : ''))
      .filter(Boolean)
  } catch {
    user.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.userId] as const,
  () => {
    if (props.open) loadUserDetail()
  },
)
</script>

<template>
  <Dialog :open="open" title="查看用户" class="max-w-lg" @update:open="$emit('update:open', $event)">
    <div v-if="loading" class="py-8 text-center text-sm text-muted-foreground">加载中…</div>
    <div v-else-if="!user" class="py-8 text-center text-sm text-muted-foreground">用户信息加载失败</div>
    <div v-else class="space-y-4">
      <div class="grid gap-3 sm:grid-cols-2">
        <div>
          <p class="text-xs text-muted-foreground">用户 ID</p>
          <p class="mt-1 font-mono text-sm">{{ user.userId }}</p>
        </div>
        <div>
          <p class="text-xs text-muted-foreground">状态</p>
          <Badge class="mt-1" :variant="statusVariant">{{ statusLabel }}</Badge>
        </div>
        <div>
          <p class="text-xs text-muted-foreground">用户名</p>
          <p class="mt-1 text-sm">{{ user.userName || '-' }}</p>
        </div>
        <div>
          <p class="text-xs text-muted-foreground">昵称</p>
          <p class="mt-1 text-sm">{{ user.nickName || '-' }}</p>
        </div>
        <div>
          <p class="text-xs text-muted-foreground">手机</p>
          <p class="mt-1 text-sm">{{ user.mobile || '-' }}</p>
        </div>
        <div>
          <p class="text-xs text-muted-foreground">邮箱</p>
          <p class="mt-1 text-sm">{{ user.email || '-' }}</p>
        </div>
        <div>
          <p class="text-xs text-muted-foreground">所属组织</p>
          <p class="mt-1 text-sm">{{ orgLabel(user.companyId) }}</p>
        </div>
        <div>
          <p class="text-xs text-muted-foreground">部门</p>
          <p class="mt-1 text-sm">{{ orgLabel(user.departmentId) }}</p>
        </div>
      </div>

      <div v-if="accounts.length">
        <p class="text-xs text-muted-foreground">登录凭证</p>
        <ul class="mt-1 space-y-1 rounded border bg-muted/30 p-2 text-sm">
          <li v-for="acc in accounts" :key="acc.accountId" class="font-mono">
            <span class="text-muted-foreground">{{ accountTypeLabel[acc.accountType ?? ''] ?? acc.accountType }}：</span>
            {{ acc.account }}
          </li>
        </ul>
      </div>

      <div v-if="roles.length">
        <p class="text-xs text-muted-foreground">角色</p>
        <div class="mt-1 flex flex-wrap gap-1.5">
          <Badge v-for="r in roles" :key="r.roleId" variant="outline">{{ r.roleName }}</Badge>
        </div>
      </div>

      <div v-if="extraOrgIds.length">
        <p class="text-xs text-muted-foreground">跨组织数据授权</p>
        <div class="mt-1 flex flex-wrap gap-1.5">
          <Badge v-for="orgId in extraOrgIds" :key="orgId" variant="secondary">{{ orgLabel(orgId) }}</Badge>
        </div>
      </div>
    </div>
  </Dialog>
</template>
