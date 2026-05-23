<script setup lang="ts">
import Label from '@/components/ui/Label.vue'

defineProps<{
  label: string
  forId?: string
  required?: boolean
}>()
</script>

<template>
  <div class="space-y-2">
    <Label :for="forId">
      {{ label }}
      <span v-if="required" class="text-destructive">*</span>
    </Label>
    <slot />
  </div>
</template>
