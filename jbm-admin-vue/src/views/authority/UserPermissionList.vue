<script setup lang="ts">
import { ref } from 'vue'
import { Search, Save } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import FormField from '@/components/FormField.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Badge from '@/components/ui/Badge.vue'
import { listUsers } from '@/api/user'
import {
  listAuthorityMenus,
  listAuthorityCatalog,
  getUserAuthorities,
  putUserAuthorities,
  type AuthorityMenu,
  type OpenAuthority,
} from '@/api/authority'
import { listActions } from '@/api/action'
import type { BaseAction, BaseUser } from '@/api/types'
import { useFeedback } from '@/composables/useFeedback'
import { useMenuActionPermissions } from '@/composables/useMenuActionPermissions'
import { toSnowflakeIdString } from '@/lib/snowflakeId'

const feedback = useFeedback()
const keyword = ref('')
const searching = ref(false)
const searchError = ref('')
const userResults = ref<BaseUser[]>([])
const selectedUser = ref<BaseUser | null>(null)

const allMenus = ref<AuthorityMenu[]>([])
const menuActions = ref<Record<string, BaseAction[]>>({})
const authorityCatalog = ref<OpenAuthority[]>([])
const permLoading = ref(false)
const permSaving = ref(false)
const permError = ref('')

const {
  selectedAuthorityIds,
  authorityIdForActionCode,
  isMenuFullyChecked,
  isMenuIndeterminate,
  toggleMenu,
  toggleAction,
  selectAllActionsForMenu,
  clearAllActionsForMenu,
  ensureMenuPermissionsBeforeSave,
  resetSelected,
} = useMenuActionPermissions(authorityCatalog, menuActions)

function syncMenuCheckbox(el: unknown, menuId?: number) {
  if (el instanceof HTMLInputElement && menuId) {
    el.indeterminate = isMenuIndeterminate(menuId)
  }
}

async function searchUsers() {
  const kw = keyword.value.trim()
  if (!kw) {
    searchError.value = '请输入用户名或关键字'
    return
  }
  searching.value = true
  searchError.value = ''
  try {
    const data = await listUsers(1, 20, kw)
    userResults.value = data.contents ?? []
    if (!userResults.value.length) searchError.value = '未找到用户'
  } catch (e) {
    searchError.value = e instanceof Error ? e.message : '查询失败'
    userResults.value = []
  } finally {
    searching.value = false
  }
}

async function selectUser(user: BaseUser) {
  if (!user.userId) return
  selectedUser.value = user
  permError.value = ''
  permLoading.value = true
  try {
    const [menus, catalog, granted, allActions] = await Promise.all([
      listAuthorityMenus(),
      listAuthorityCatalog('1'),
      getUserAuthorities(user.userId),
      listActions(undefined, 1, 500),
    ])
    authorityCatalog.value = catalog ?? []
    allMenus.value = menus.filter((m) => m.menuId && m.path && m.path !== '/')
    const byMenu: Record<string, BaseAction[]> = {}
    for (const a of allActions.contents ?? []) {
      if (a.menuId == null) continue
      const key = toSnowflakeIdString(a.menuId)
      if (!byMenu[key]) byMenu[key] = []
      byMenu[key].push(a)
    }
    menuActions.value = byMenu
    resetSelected(granted.map((g) => String(g.authorityId)).filter(Boolean))
  } catch (e) {
    permError.value = e instanceof Error ? e.message : '加载权限失败'
  } finally {
    permLoading.value = false
  }
}

async function savePermissions() {
  if (!selectedUser.value?.userId) return
  permSaving.value = true
  permError.value = ''
  try {
    const ids = ensureMenuPermissionsBeforeSave()
    await putUserAuthorities(selectedUser.value.userId, ids)
    feedback.toast.success('用户权限已更新。', '已保存')
  } catch (e) {
    permError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    permSaving.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <PageHeader
      title="用户权限"
      description="为指定用户直接分配菜单与按钮权限（叠加其角色权限）。"
    />

    <Card>
      <CardContent class="space-y-4 pt-6">
        <div class="flex flex-wrap items-end gap-3">
          <FormField label="查找用户" class="min-w-[16rem] flex-1">
            <Input
              v-model="keyword"
              placeholder="用户名 / 昵称"
              @keyup.enter="searchUsers"
            />
          </FormField>
          <Button :disabled="searching" @click="searchUsers">
            <Search class="mr-1 h-4 w-4" />
            查询
          </Button>
        </div>
        <p v-if="searchError" class="text-sm text-destructive">{{ searchError }}</p>
        <div v-if="userResults.length" class="flex flex-wrap gap-2">
          <Button
            v-for="u in userResults"
            :key="u.userId"
            :variant="selectedUser?.userId === u.userId ? 'default' : 'outline'"
            size="sm"
            @click="selectUser(u)"
          >
            {{ u.userName }}
            <span v-if="u.nickName" class="ml-1 text-muted-foreground">({{ u.nickName }})</span>
          </Button>
        </div>
      </CardContent>
    </Card>

    <template v-if="selectedUser">
      <div class="flex items-center justify-between gap-4">
        <div>
          <h2 class="text-lg font-semibold">
            {{ selectedUser.userName }}
            <Badge v-if="selectedUser.status === 1" class="ml-2">启用</Badge>
            <Badge v-else variant="secondary" class="ml-2">停用</Badge>
          </h2>
          <p class="text-sm text-muted-foreground">
            勾选按钮会自动勾选所属菜单；取消菜单会同步取消其下按钮。
          </p>
        </div>
        <Button :disabled="permSaving || permLoading" @click="savePermissions">
          <Save class="mr-1 h-4 w-4" />
          {{ permSaving ? '保存中…' : '保存权限' }}
        </Button>
      </div>

      <p v-if="permError" class="text-sm text-destructive">{{ permError }}</p>
      <p v-if="permLoading" class="text-sm text-muted-foreground">加载权限中…</p>

      <div v-else class="max-h-144 space-y-3 overflow-y-auto rounded-lg border p-4">
        <div
          v-for="m in allMenus"
          :key="m.menuId"
          class="rounded border px-3 py-2"
        >
          <div class="flex flex-wrap items-center gap-2">
            <label class="flex flex-1 cursor-pointer items-center gap-2 text-sm font-medium">
              <input
                type="checkbox"
                :checked="isMenuFullyChecked(m.menuId)"
                :ref="(el) => syncMenuCheckbox(el, m.menuId)"
                @change="toggleMenu(m.menuId)"
              />
              {{ m.menuName }}
              <span class="font-mono text-xs font-normal text-muted-foreground">{{ m.path }}</span>
            </label>
            <div v-if="m.menuId && (menuActions[toSnowflakeIdString(m.menuId)]?.length ?? 0) > 0" class="flex gap-1">
              <Button variant="outline" size="sm" type="button" @click="selectAllActionsForMenu(m.menuId)">
                全选按钮
              </Button>
              <Button variant="outline" size="sm" type="button" @click="clearAllActionsForMenu(m.menuId)">
                清空按钮
              </Button>
            </div>
          </div>
          <div
            v-if="m.menuId && (menuActions[toSnowflakeIdString(m.menuId)]?.length ?? 0) > 0"
            class="mt-2 ml-6 flex flex-wrap gap-2"
          >
            <label
              v-for="act in menuActions[toSnowflakeIdString(m.menuId)]"
              :key="act.actionId"
              class="flex cursor-pointer items-center gap-1 rounded bg-muted/40 px-2 py-1 text-xs"
            >
              <input
                type="checkbox"
                :checked="
                  !!act.actionCode &&
                  selectedAuthorityIds.has(authorityIdForActionCode(act.actionCode) ?? '')
                "
                @change="toggleAction(act.actionCode, m.menuId)"
              />
              {{ act.actionName }}
              <span class="font-mono text-muted-foreground">ACTION_{{ act.actionCode }}</span>
            </label>
          </div>
        </div>
      </div>
    </template>

    <p v-else class="text-sm text-muted-foreground">请先查询并选择一名用户。</p>
  </div>
</template>
