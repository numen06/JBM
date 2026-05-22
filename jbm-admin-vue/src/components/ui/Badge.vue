<script setup lang="ts">
import { cn } from '@/lib/utils'

withDefaults(
  defineProps<{ variant?: 'default' | 'secondary' | 'outline' | 'destructive' }>(),
  { variant: 'default' },
)
</script>

<template>
  <span
    :class="
      cn(
        'inline-flex items-center rounded-md border px-2.5 py-0.5 text-xs font-semibold transition-colors',
        variant === 'default' && 'border-transparent bg-primary text-primary-foreground',
        variant === 'secondary' && 'border-transparent bg-secondary text-secondary-foreground',
        variant === 'outline' && 'text-foreground',
        variant === 'destructive' && 'border-transparent bg-destructive text-destructive-foreground',
      )
    "
  >
    <slot />
  </span>
</template>
