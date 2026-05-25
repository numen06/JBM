<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UiFeedbackHost from '@/components/UiFeedbackHost.vue'

const auth = useAuthStore()

onMounted(() => {
  auth.init()
})
</script>

<template>
  <RouterView />
  <UiFeedbackHost />
</template>
