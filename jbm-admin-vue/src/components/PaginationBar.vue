<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from '@/components/ui/Button.vue'
import Select from '@/components/ui/Select.vue'

const props = defineProps<{ page: number; total: number; pageSize: number }>()
const emit = defineEmits<{ change: [page: number, pageSize?: number] }>()
const pageInput = ref(String(props.page))

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const pageSizeOptions = computed(() => [...new Set([props.pageSize, 10, 20, 50, 100])].sort((a, b) => a - b))

watch(
  () => props.page,
  (value) => {
    pageInput.value = String(value)
  },
)

function changePageSize(value: string) {
  emit('change', 1, Number(value))
}

function goToPage() {
  const value = Number(pageInput.value)
  if (!Number.isFinite(value)) {
    pageInput.value = String(props.page)
    return
  }
  const nextPage = Math.min(Math.max(1, Math.floor(value)), totalPages.value)
  pageInput.value = String(nextPage)
  emit('change', nextPage, props.pageSize)
}
</script>

<template>
  <div class="flex flex-wrap items-center justify-between gap-3 border-t px-4 py-3 text-sm text-muted-foreground">
    <div class="flex flex-wrap items-center gap-3">
      <span>共 {{ total }} 条</span>
      <div class="flex items-center gap-2">
        <span>每页</span>
        <Select :model-value="pageSize" class="h-8 w-20" @update:model-value="changePageSize">
          <option v-for="option in pageSizeOptions" :key="option" :value="option">{{ option }}</option>
        </Select>
        <span>条</span>
      </div>
    </div>
    <div class="flex flex-wrap items-center justify-end gap-2">
      <Button variant="outline" size="sm" :disabled="page <= 1" @click="emit('change', page - 1, pageSize)">
        上一页
      </Button>
      <span>{{ page }} / {{ totalPages }}</span>
      <Button
        variant="outline"
        size="sm"
        :disabled="page >= totalPages"
        @click="emit('change', page + 1, pageSize)"
      >
        下一页
      </Button>
      <div class="flex items-center gap-2">
        <span>跳至</span>
        <input
          v-model="pageInput"
          type="number"
          min="1"
          :max="totalPages"
          class="h-8 w-16 rounded-md border border-input bg-transparent px-2 text-center text-sm text-foreground shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          @keyup.enter="goToPage"
        />
        <span>页</span>
        <Button variant="outline" size="sm" @click="goToPage">确定</Button>
      </div>
    </div>
  </div>
</template>
