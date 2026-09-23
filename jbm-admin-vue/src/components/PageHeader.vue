<script setup lang="ts">
import { JbmPageHeading } from '@jbm7/vue-core'
defineProps<{ title: string; description?: string }>()
</script>

<template>
  <JbmPageHeading v-bind="$props"><slot name="actions" /></JbmPageHeading>
</template>
