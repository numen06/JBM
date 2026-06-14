<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Search, X } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import { cn } from '@/lib/utils'
import { getUser, listUsers } from '@/api/user'
import type { BaseUser } from '@/api/types'

const props = withDefaults(
  defineProps<{
    modelValue?: number | null
    includeBroadcast?: boolean
    placeholder?: string
  }>(),
  {
    modelValue: null,
    includeBroadcast: false,
    placeholder: '搜索用户',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const keyword = ref('')
const loading = ref(false)
const open = ref(false)
const users = ref<BaseUser[]>([])
const selectedUser = ref<BaseUser | null>(null)

const selectedLabel = computed(() => {
  if (props.modelValue === 0) return '全局/广播 (0)'
  if (!selectedUser.value) return ''
  const name = selectedUser.value.nickName || selectedUser.value.userName || `用户 ${selectedUser.value.userId}`
  return `${name}${selectedUser.value.userId ? ` (${selectedUser.value.userId})` : ''}`
})

function userTitle(user: BaseUser) {
  return user.nickName || user.userName || `用户 ${user.userId ?? '-'}`
}

function userMeta(user: BaseUser) {
  return [user.userName, user.mobile, user.email, user.userId ? `ID ${user.userId}` : '']
    .filter(Boolean)
    .join(' / ')
}

async function searchUsers() {
  loading.value = true
  try {
    const page = await listUsers(1, 8, keyword.value)
    users.value = page?.contents ?? []
  } finally {
    loading.value = false
  }
}

function selectBroadcast() {
  selectedUser.value = null
  keyword.value = ''
  open.value = false
  emit('update:modelValue', 0)
}

function selectUser(user: BaseUser) {
  if (user.userId == null) return
  selectedUser.value = user
  keyword.value = ''
  open.value = false
  emit('update:modelValue', Number(user.userId))
}

function clear() {
  selectedUser.value = null
  keyword.value = ''
  users.value = []
  open.value = false
  emit('update:modelValue', null)
}

function handleFocus() {
  open.value = true
  if (!users.value.length) void searchUsers()
}

let searchTimer: number | undefined
watch(keyword, () => {
  open.value = true
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => void searchUsers(), 250)
})

watch(
  () => props.modelValue,
  async (value) => {
    if (value == null || value === 0) {
      selectedUser.value = null
      return
    }
    if (selectedUser.value?.userId === value) return
    try {
      selectedUser.value = await getUser(Number(value))
    } catch {
      selectedUser.value = { userId: value }
    }
  },
  { immediate: true },
)
</script>

<template>
  <div :class="cn('relative min-w-56', $attrs.class as string)">
    <div class="flex items-center gap-2">
      <div class="relative flex-1">
        <Search class="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          v-model="keyword"
          :placeholder="selectedLabel || placeholder"
          class="pl-8 pr-8"
          @focus="handleFocus"
          @keydown.escape="open = false"
        />
        <button
          v-if="modelValue != null || keyword"
          type="button"
          class="absolute right-2 top-1/2 inline-flex h-5 w-5 -translate-y-1/2 items-center justify-center rounded text-muted-foreground hover:bg-accent hover:text-foreground"
          @click="clear"
        >
          <X class="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
    <div
      v-if="open"
      class="absolute z-30 mt-1 max-h-72 w-full overflow-auto rounded-md border bg-popover p-1 text-sm shadow-md"
      @mousedown.prevent
    >
      <button
        v-if="includeBroadcast"
        type="button"
        class="flex w-full flex-col rounded px-3 py-2 text-left hover:bg-accent"
        @click="selectBroadcast"
      >
        <span class="font-medium">全局/广播</span>
        <span class="text-xs text-muted-foreground">接收用户 ID 0</span>
      </button>
      <div v-if="loading" class="px-3 py-2 text-muted-foreground">搜索中...</div>
      <button
        v-for="user in users"
        :key="user.userId"
        type="button"
        class="flex w-full flex-col rounded px-3 py-2 text-left hover:bg-accent"
        @click="selectUser(user)"
      >
        <span class="font-medium">{{ userTitle(user) }}</span>
        <span class="truncate text-xs text-muted-foreground">{{ userMeta(user) || '-' }}</span>
      </button>
      <div v-if="!loading && !users.length" class="px-3 py-2 text-muted-foreground">暂无匹配用户</div>
    </div>
    <Button v-if="open" class="sr-only" variant="ghost" size="sm" @click="open = false">关闭</Button>
  </div>
</template>
