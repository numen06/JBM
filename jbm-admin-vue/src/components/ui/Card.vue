<script setup lang="ts">
import { cn } from '@/lib/utils'
</script>

<template>
  <div class="jbm-panel" :class="cn('rounded-lg border bg-card text-card-foreground shadow-sm', $attrs.class as string)">
    <slot />
  </div>
</template>
