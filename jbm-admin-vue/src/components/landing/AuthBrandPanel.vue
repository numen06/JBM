<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { ArrowLeft, BookOpen, Code2, KeyRound, ShieldCheck, Split } from '@lucide/vue'
import JbmLogo from '@/components/JbmLogo.vue'

defineProps<{
  title?: string
  subtitle?: string
  showBack?: boolean
}>()
</script>

<template>
  <div class="relative flex min-h-screen flex-col justify-between overflow-hidden bg-slate-950 p-8 text-slate-100 lg:p-12">
    <div class="relative z-10">
      <RouterLink
        v-if="showBack !== false"
        to="/"
        class="mb-8 inline-flex items-center gap-1.5 text-sm text-slate-300 transition-colors hover:text-white"
      >
        <ArrowLeft class="size-4" />
        返回首页
      </RouterLink>

      <div class="flex items-center gap-3">
        <JbmLogo class="size-12 rounded-lg" />
        <div>
          <h1 class="text-2xl font-bold tracking-tight lg:text-3xl">{{ title ?? 'JBM 开源平台' }}</h1>
          <p class="mt-1 text-sm text-slate-400">
            {{ subtitle ?? '统一认证授权 · 微服务权限管理' }}
          </p>
        </div>
      </div>

      <p class="mt-6 max-w-sm text-sm leading-relaxed text-slate-300">
        面向开发者与企业的开源 OAuth2 认证平台。注册账号、创建子应用、获取 Client 凭证与 API Key，
        即可把 JBM 接入到自己的业务系统。
      </p>
    </div>

    <div class="relative z-10 my-10 rounded-lg border border-white/10 bg-white/[0.03] p-4 font-mono text-xs shadow-2xl">
      <div class="mb-3 flex items-center justify-between border-b border-white/10 pb-3 text-slate-400">
        <span>openapi-flow</span>
        <span>verified</span>
      </div>
      <div class="space-y-3">
        <p><span class="text-emerald-300">1.</span> POST /oauth2/register</p>
        <p><span class="text-sky-300">2.</span> POST /oauth2/doLogin → code</p>
        <p><span class="text-amber-300">3.</span> PUT /apikey/{{ '{id}' }}/authority</p>
        <p><span class="text-violet-300">4.</span> GET /api/open/** + X-JBM-Signature</p>
      </div>
    </div>

    <div class="relative z-10 space-y-4">
      <div class="flex items-start gap-3 text-sm">
        <ShieldCheck class="mt-0.5 size-4 shrink-0 text-emerald-300" />
        <div>
          <p class="font-medium">OAuth2 标准协议</p>
          <p class="text-slate-400">授权码、密码、刷新 Token、RSA 密码加密</p>
        </div>
      </div>
      <div class="flex items-start gap-3 text-sm">
        <Split class="mt-0.5 size-4 shrink-0 text-sky-300" />
        <div>
          <p class="font-medium">集群职责清晰</p>
          <p class="text-slate-400">Gateway、Auth、Center 分层处理认证和授权</p>
        </div>
      </div>
      <div class="flex items-start gap-3 text-sm">
        <KeyRound class="mt-0.5 size-4 shrink-0 text-amber-300" />
        <div>
          <p class="font-medium">开放 API Key</p>
          <p class="text-slate-400">按授权清单和签名规则访问 JBM OpenAPI</p>
        </div>
      </div>
      <div class="flex items-center gap-4 pt-2">
        <RouterLink
          to="/docs"
          class="inline-flex items-center gap-1.5 rounded-md bg-white/10 px-3 py-1.5 text-xs font-medium transition-colors hover:bg-white/15"
        >
          <BookOpen class="size-3.5" />
          API Wiki
        </RouterLink>
        <a
          href="https://github.com"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center gap-1.5 rounded-md bg-white/10 px-3 py-1.5 text-xs font-medium transition-colors hover:bg-white/15"
        >
          <Code2 class="size-3.5" />
          GitHub
        </a>
      </div>
    </div>
  </div>
</template>
