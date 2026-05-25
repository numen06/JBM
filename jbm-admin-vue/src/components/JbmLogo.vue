<script setup lang="ts">
import logoUrl from '@/assets/jbm-logo.svg'

defineProps<{
  alt?: string
}>()
</script>

<template>
  <img :src="logoUrl" :alt="alt ?? 'JBM logo'" />
</template>
