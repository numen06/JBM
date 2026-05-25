<script setup lang="ts">
import { RouterLink } from 'vue-router'
import JbmLogo from '@/components/JbmLogo.vue'
</script>

<template>
  <footer class="border-t bg-muted/30">
    <div class="mx-auto max-w-6xl px-4 py-12 sm:px-6">
      <div class="grid gap-8 md:grid-cols-4">
        <div class="md:col-span-2">
          <div class="flex items-center gap-2 font-semibold">
            <JbmLogo class="size-8 rounded-md" />
            JBM 开源平台
          </div>
          <p class="mt-3 max-w-md text-sm leading-6 text-muted-foreground">
            基于 OAuth2 的统一认证授权中间件，为微服务应用提供 RBAC 权限、API 网关、
            多租户隔离和 JBM OpenAPI 开放能力。
          </p>
        </div>
        <div>
          <h3 class="text-sm font-semibold">开发者</h3>
          <ul class="mt-3 space-y-2 text-sm text-muted-foreground">
            <li><RouterLink to="/docs" class="hover:text-primary">OpenAPI Wiki</RouterLink></li>
            <li><RouterLink to="/docs#oauth2-auth-code" class="hover:text-primary">OAuth2 接入</RouterLink></li>
            <li><RouterLink to="/docs#openapi-api-key" class="hover:text-primary">API Key 签名</RouterLink></li>
          </ul>
        </div>
        <div>
          <h3 class="text-sm font-semibold">账号</h3>
          <ul class="mt-3 space-y-2 text-sm text-muted-foreground">
            <li><RouterLink to="/login" class="hover:text-primary">登录</RouterLink></li>
            <li><RouterLink to="/register" class="hover:text-primary">注册</RouterLink></li>
            <li><RouterLink to="/dashboard" class="hover:text-primary">管理控制台</RouterLink></li>
          </ul>
        </div>
      </div>
      <div class="mt-10 flex flex-col items-center justify-between gap-4 border-t pt-8 text-sm text-muted-foreground sm:flex-row">
        <p>&copy; {{ new Date().getFullYear() }} JBM Cluster Platform. 开源统一认证授权平台。</p>
        <p class="text-xs">Gateway: <code class="rounded bg-muted px-1">http://127.0.0.1:7777</code></p>
      </div>
    </div>
  </footer>
</template>
