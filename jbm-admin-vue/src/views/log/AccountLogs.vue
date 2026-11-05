<script setup lang="ts">
import { ref } from 'vue'
import Button from '@/components/ui/Button.vue'
import SecurityAuditPanel from './SecurityAuditPanel.vue'
import BusinessLogs from './BusinessLogs.vue'
import { useAuthStore } from '@/stores/auth'
import { isPlatformAdministrator } from '@/lib/adminAccess'
const tab = ref('security')
const auth = useAuthStore()
</script>
<template>
  <div class="space-y-4">
    <div class="flex flex-wrap gap-2" aria-label="审计日志分类">
      <Button :variant="tab === 'security' ? 'default' : 'outline'" @click="tab = 'security'">安全审计</Button>
      <Button v-if="isPlatformAdministrator(auth.user)" :variant="tab === 'business' ? 'default' : 'outline'" @click="tab = 'business'">业务过程</Button>
    </div>
    <SecurityAuditPanel v-if="tab === 'security'" />
    <BusinessLogs v-else />
  </div>
</template>
