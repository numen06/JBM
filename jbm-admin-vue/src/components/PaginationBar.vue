<script setup lang="ts">
import Button from '@/components/ui/Button.vue'

const props = defineProps<{ page: number; total: number; pageSize: number }>()
const emit = defineEmits<{ change: [page: number] }>()

function totalPages() {
  return Math.max(1, Math.ceil(props.total / props.pageSize))
}
</script>

<template>
  <div class="flex items-center justify-between border-t px-4 py-3 text-sm text-muted-foreground">
    <span>共 {{ total }} 条</span>
    <div class="flex items-center gap-2">
      <Button variant="outline" size="sm" :disabled="page <= 1" @click="emit('change', page - 1)">
        上一页
      </Button>
      <span>{{ page }} / {{ totalPages() }}</span>
      <Button
        variant="outline"
        size="sm"
        :disabled="page >= totalPages()"
        @click="emit('change', page + 1)"
      >
        下一页
      </Button>
    </div>
  </div>
</template>
