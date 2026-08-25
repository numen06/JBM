<script setup lang="ts">
import Skeleton from '@/components/ui/Skeleton.vue'

defineProps<{
  loading?: boolean
  error?: string
  empty?: boolean
  emptyText?: string
}>()
</script>

<template>
  <div class="data-table-shell overflow-x-auto rounded-lg border">
    <div v-if="loading" class="data-table-state space-y-2 p-4">
      <Skeleton v-for="i in 5" :key="i" class="h-10 w-full" />
    </div>
    <div v-else-if="error" class="data-table-state p-8 text-center text-sm text-destructive">{{ error }}</div>
    <div v-else-if="empty" class="data-table-state p-8 text-center text-sm text-muted-foreground">
      {{ emptyText ?? '暂无数据' }}
    </div>
    <slot v-else />
  </div>
</template>

<style scoped>
@media (max-width: 767px) {
  .data-table-shell {
    overflow: visible;
    border: 0;
    border-radius: 0;
  }

  .data-table-state {
    border: 1px solid var(--color-border);
    border-radius: 0.5rem;
    background: var(--color-card);
  }
}
</style>
