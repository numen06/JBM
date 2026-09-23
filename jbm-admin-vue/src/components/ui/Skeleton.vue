<script setup lang="ts">
import { cn } from '@/lib/utils'
</script>

<template>
  <div aria-hidden="true" :class="cn('jbm-skeleton rounded-md bg-muted', $attrs.class as string)" />
</template>
