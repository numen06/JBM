<script setup lang="ts">
import SecurityAuditPanel from './SecurityAuditPanel.vue'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
</script>
<template><SecurityAuditPanel :key="auth.activeTenantId" scope="current" /></template>
