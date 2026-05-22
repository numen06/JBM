<script setup lang="ts">
import { cn } from '@/lib/utils'
</script>

<template>
  <div :class="cn('animate-pulse rounded-md bg-muted', $attrs.class as string)" />
</template>
