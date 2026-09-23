<script setup lang="ts">
import { cn } from '@/lib/utils'
import { ref } from 'vue'
import { useJbmDialog } from '@jbm7/vue-core'

defineOptions({ inheritAttrs: false })
const props = defineProps<{ open?: boolean; title?: string }>()
const emit = defineEmits<{ 'update:open': [value: boolean] }>()
const panel = ref<HTMLElement | null>(null)
useJbmDialog(() => props.open, panel, () => emit('update:open', false))
</script>

<template>
  <Teleport to="body">
    <Transition name="jbm-dialog">
    <div
      v-if="open"
      class="jbm-dialog-portal jbm-dialog-overlay fixed inset-0 z-50 flex items-end justify-center overflow-y-auto bg-black/50 p-0 sm:items-center sm:p-4"
      @click.self="$emit('update:open', false)"
    >
      <div
        ref="panel" role="dialog" aria-modal="true" :aria-label="title || '对话框'" tabindex="-1"
        :class="
          cn(
            'jbm-dialog-panel relative z-50 flex max-h-[94dvh] w-full max-w-lg flex-col rounded-t-lg border bg-card shadow-lg sm:max-h-[calc(100dvh-2rem)] sm:rounded-lg',
            $attrs.class as string,
          )
        "
        @click.stop
      >
        <div v-if="title" class="flex shrink-0 items-center justify-between gap-3 border-b px-3 py-2.5 sm:px-6 sm:py-4">
          <h2 class="min-w-0 truncate text-base font-semibold sm:text-lg">{{ title }}</h2>
          <button
            type="button"
            class="shrink-0 rounded-sm px-2 py-1 text-sm opacity-70 hover:opacity-100"
            aria-label="关闭"
            @click="$emit('update:open', false)"
          >
            关闭
          </button>
        </div>
        <div class="min-h-0 overflow-y-auto p-3 sm:p-6">
          <slot />
        </div>
      </div>
    </div>
    </Transition>
  </Teleport>
</template>
