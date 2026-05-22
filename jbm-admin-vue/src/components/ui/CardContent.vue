<script setup lang="ts">
import { cn } from '@/lib/utils'
</script>

<template>
  <div :class="cn('p-6 pt-0', $attrs.class as string)">
    <slot />
  </div>
</template>
