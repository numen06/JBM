<script setup lang="ts">
import Dialog from '@/components/ui/Dialog.vue'
import Button from '@/components/ui/Button.vue'

defineProps<{
  open: boolean
  title: string
  saving?: boolean
  wide?: boolean
}>()

defineEmits<{
  'update:open': [value: boolean]
  save: []
}>()
</script>

<template>
  <Dialog
    :open="open"
    :title="title"
    :class="wide ? 'max-w-2xl' : 'max-w-md'"
    @update:open="$emit('update:open', $event)"
  >
    <div class="max-h-[70vh] space-y-4 overflow-y-auto pr-1">
      <slot />
    </div>
    <div class="mt-6 flex justify-end gap-2 border-t pt-4">
      <Button variant="outline" type="button" @click="$emit('update:open', false)">取消</Button>
      <Button type="button" :disabled="saving" @click="$emit('save')">
        {{ saving ? '保存中…' : '保存' }}
      </Button>
    </div>
  </Dialog>
</template>
