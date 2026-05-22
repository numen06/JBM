<script setup lang="ts">
import { cn } from '@/lib/utils'

defineProps<{ open?: boolean; title?: string }>()
defineEmits<{ 'update:open': [value: boolean] }>()
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      @click.self="$emit('update:open', false)"
    >
      <div
        :class="
          cn(
            'relative z-50 w-full max-w-lg rounded-lg border bg-background p-6 shadow-lg',
            $attrs.class as string,
          )
        "
        @click.stop
      >
        <div v-if="title" class="mb-4 flex items-center justify-between">
          <h2 class="text-lg font-semibold">{{ title }}</h2>
          <button
            type="button"
            class="rounded-sm opacity-70 hover:opacity-100"
            @click="$emit('update:open', false)"
          >
            ✕
          </button>
        </div>
        <slot />
      </div>
    </div>
  </Teleport>
</template>
