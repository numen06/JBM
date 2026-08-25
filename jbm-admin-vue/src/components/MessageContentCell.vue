<script setup lang="ts">
import { computed, ref } from 'vue'
import { Search } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Dialog from '@/components/ui/Dialog.vue'
import type { PushMessage } from '@/api/types'

const props = defineProps<{ message: PushMessage }>()
const open = ref(false)

const content = computed(() => contentText(props.message.content))
const preview = computed(() => {
  const text = content.value.replace(/\s+/g, ' ').trim()
  if (!text || text === '-') return '-'
  return text.length > 80 ? `${text.slice(0, 80)}...` : text
})

function contentText(value: unknown) {
  if (value == null) return '-'
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}
</script>

<template>
  <div class="flex min-w-0 items-center gap-2">
    <p class="min-w-0 flex-1 line-clamp-2 text-sm leading-5 text-foreground/80" :title="content">
      {{ preview }}
    </p>
    <Button variant="ghost" size="icon" class="h-8 w-8 shrink-0" title="查看完整内容" aria-label="查看完整内容" @click="open = true">
      <Search class="h-4 w-4" />
    </Button>
  </div>

  <Dialog v-model:open="open" title="消息内容" class="max-w-3xl">
    <div class="space-y-3">
      <div>
        <p class="text-xs text-muted-foreground">标题</p>
        <p class="mt-1 font-medium">{{ message.title || '-' }}</p>
      </div>
      <div>
        <p class="text-xs text-muted-foreground">内容</p>
        <pre class="mt-1 max-h-[60vh] overflow-auto whitespace-pre-wrap break-words rounded-md border bg-muted/30 p-3 text-sm leading-6">{{ content }}</pre>
      </div>
    </div>
  </Dialog>
</template>
