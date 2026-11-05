<script setup lang="ts">
import { ref } from 'vue'
import Button from '@/components/ui/Button.vue'
import SecurityAuditPanel from './SecurityAuditPanel.vue'
import { useAuthStore } from '@/stores/auth'
const tab = ref('operation')
const auth = useAuthStore()
</script>
<template>
  <div class="space-y-4">
    <div class="flex flex-wrap gap-2" aria-label="审计日志分类">
      <Button :variant="tab === 'operation' ? 'default' : 'outline'" @click="tab = 'operation'">业务操作</Button>
      <Button :variant="tab === 'login' ? 'default' : 'outline'" @click="tab = 'login'">登录记录</Button>
      <Button :variant="tab === 'access' ? 'default' : 'outline'" @click="tab = 'access'">接口访问</Button>
    </div>
    <SecurityAuditPanel :key="`${tab}:${auth.activeTenantId}`" :category="tab" :title="tab === 'operation' ? '业务操作' : tab === 'login' ? '登录记录' : '接口访问'" />
  </div>
</template>
