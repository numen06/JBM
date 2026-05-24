<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { ArrowLeft, BookOpen, Github, Shield, Zap } from '@lucide/vue'

defineProps<{
  title?: string
  subtitle?: string
  showBack?: boolean
}>()
</script>

<template>
  <div
    class="relative flex flex-col justify-between overflow-hidden bg-gradient-to-br from-primary via-primary to-blue-700 p-8 text-primary-foreground lg:p-12"
  >
    <div class="absolute inset-0 opacity-10">
      <div class="absolute -right-20 -top-20 size-80 rounded-full bg-white/20 blur-3xl" />
      <div class="absolute -bottom-20 -left-20 size-60 rounded-full bg-white/10 blur-3xl" />
    </div>

    <div class="relative z-10">
      <RouterLink
        v-if="showBack !== false"
        to="/"
        class="mb-8 inline-flex items-center gap-1.5 text-sm text-primary-foreground/80 transition-colors hover:text-primary-foreground"
      >
        <ArrowLeft class="size-4" />
        返回首页
      </RouterLink>

      <div class="flex items-center gap-3">
        <span class="flex size-12 items-center justify-center rounded-xl bg-white/15 text-lg font-bold backdrop-blur-sm">
          JBM
        </span>
        <div>
          <h1 class="text-2xl font-bold tracking-tight lg:text-3xl">{{ title ?? 'JBM 开源平台' }}</h1>
          <p class="mt-1 text-sm text-primary-foreground/80">
            {{ subtitle ?? '统一认证授权 · 微服务权限管理' }}
          </p>
        </div>
      </div>

      <p class="mt-6 max-w-sm text-sm leading-relaxed text-primary-foreground/85">
        面向开发者与企业的开源 OAuth2 认证平台。注册账号、创建应用、获取 Client 凭证，即可快速接入您的业务系统。
      </p>
    </div>

    <div class="relative z-10 mt-10 space-y-4">
      <div class="flex items-start gap-3 text-sm">
        <Shield class="mt-0.5 size-4 shrink-0 text-primary-foreground/90" />
        <div>
          <p class="font-medium">OAuth2 标准协议</p>
          <p class="text-primary-foreground/75">授权码、密码、刷新 Token 等多种模式</p>
        </div>
      </div>
      <div class="flex items-start gap-3 text-sm">
        <Zap class="mt-0.5 size-4 shrink-0 text-primary-foreground/90" />
        <div>
          <p class="font-medium">开箱即用</p>
          <p class="text-primary-foreground/75">RBAC 权限、API 网关、多租户支持</p>
        </div>
      </div>
      <div class="flex items-center gap-4 pt-2">
        <RouterLink
          to="/docs"
          class="inline-flex items-center gap-1.5 rounded-md bg-white/15 px-3 py-1.5 text-xs font-medium backdrop-blur-sm transition-colors hover:bg-white/25"
        >
          <BookOpen class="size-3.5" />
          API 文档
        </RouterLink>
        <a
          href="https://github.com"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center gap-1.5 rounded-md bg-white/15 px-3 py-1.5 text-xs font-medium backdrop-blur-sm transition-colors hover:bg-white/25"
        >
          <Github class="size-3.5" />
          GitHub
        </a>
      </div>
    </div>
  </div>
</template>
