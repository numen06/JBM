<script setup lang="ts">
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  BookOpen,
  Code2,
  KeyRound,
  Layers,
  Network,
  Rocket,
  Shield,
  Users,
} from '@lucide/vue'
import LandingNav from '@/components/landing/LandingNav.vue'
import LandingFooter from '@/components/landing/LandingFooter.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'

const features = [
  {
    icon: KeyRound,
    title: 'OAuth2 认证',
    desc: '标准 OAuth2 协议，支持授权码、密码、刷新 Token 等多种模式，RSA 密码加密传输。',
  },
  {
    icon: Shield,
    title: 'RBAC 权限',
    desc: '用户、角色、菜单、按钮四级权限模型，细粒度控制 API 与页面访问。',
  },
  {
    icon: Network,
    title: '微服务网关',
    desc: 'Spring Cloud Gateway 动态路由、限流、IP 白名单，统一 API 入口。',
  },
  {
    icon: Users,
    title: '多租户支持',
    desc: '租户隔离、应用管理、开发者与 API Key，适合 SaaS 与开放平台场景。',
  },
]

const techStack = [
  { name: 'Spring Boot', tag: '2.6' },
  { name: 'Vue 3', tag: '3.5' },
  { name: 'Sa-Token', tag: 'OAuth2' },
  { name: 'Redis', tag: 'Session' },
  { name: 'MyBatis-Plus', tag: 'ORM' },
  { name: 'Nacos', tag: 'Registry' },
]

const steps = [
  {
    step: '01',
    title: '注册 JBM 账号',
    desc: '创建开发者账号，获取平台访问权限。',
    link: '/register',
  },
  {
    step: '02',
    title: '创建应用',
    desc: '在控制台创建 OAuth2 应用，获取 Client ID 与 Client Secret。',
    link: '/docs#create-app',
  },
  {
    step: '03',
    title: '接入认证',
    desc: '按 API 文档集成 OAuth2，完成业务系统单点登录与 API 鉴权。',
    link: '/docs#oauth2-auth-code',
  },
]
</script>

<template>
  <div class="min-h-screen bg-background">
    <LandingNav />

    <section class="relative overflow-hidden border-b bg-gradient-to-b from-primary/5 to-background">
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-primary/10 via-transparent to-transparent" />
      <div class="relative mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:py-28">
        <div class="mx-auto max-w-3xl text-center">
          <p class="mb-4 inline-flex items-center rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-xs font-medium text-primary">
            开源 · OAuth2 · 微服务
          </p>
          <h1 class="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
            JBM
            <span class="text-primary"> 统一认证授权平台</span>
          </h1>
          <p class="mt-6 text-lg text-muted-foreground sm:text-xl">
            基于 OAuth2 的微服务权限管理中间件。为开发者提供标准 OpenAPI、RBAC 权限与 API 网关，让您的应用快速接入企业级认证体系。
          </p>
          <div class="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <RouterLink to="/register">
              <Button size="lg" class="gap-2 px-8">
                <Rocket class="size-4" />
                快速开始
              </Button>
            </RouterLink>
            <RouterLink to="/docs">
              <Button variant="outline" size="lg" class="gap-2 px-8">
                <BookOpen class="size-4" />
                API 文档
              </Button>
            </RouterLink>
            <a href="https://github.com" target="_blank" rel="noopener noreferrer">
              <Button variant="ghost" size="lg" class="gap-2">
                <Code2 class="size-4" />
                GitHub
              </Button>
            </a>
          </div>
        </div>
      </div>
    </section>

    <section class="mx-auto max-w-6xl px-4 py-20 sm:px-6">
      <div class="text-center">
        <h2 class="text-2xl font-bold tracking-tight sm:text-3xl">核心能力</h2>
        <p class="mt-3 text-muted-foreground">为企业与开发者提供完整的身份认证与权限管理解决方案</p>
      </div>
      <div class="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <Card v-for="f in features" :key="f.title" class="border-border/60 transition-shadow hover:shadow-md">
          <CardHeader class="pb-2">
            <div class="mb-2 flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <component :is="f.icon" class="size-5" />
            </div>
            <CardTitle class="text-base">{{ f.title }}</CardTitle>
          </CardHeader>
          <CardContent>
            <p class="text-sm text-muted-foreground">{{ f.desc }}</p>
          </CardContent>
        </Card>
      </div>
    </section>

    <section class="border-y bg-muted/30 py-20">
      <div class="mx-auto max-w-6xl px-4 sm:px-6">
        <div class="flex flex-col items-center gap-8 lg:flex-row lg:justify-between">
          <div>
            <h2 class="text-2xl font-bold tracking-tight">技术栈</h2>
            <p class="mt-2 text-muted-foreground">成熟稳定的技术选型，易于扩展与二次开发</p>
          </div>
          <div class="flex flex-wrap justify-center gap-3">
            <span
              v-for="t in techStack"
              :key="t.name"
              class="inline-flex items-center gap-2 rounded-full border bg-background px-4 py-2 text-sm font-medium shadow-sm"
            >
              {{ t.name }}
              <span class="rounded bg-primary/10 px-1.5 py-0.5 text-xs text-primary">{{ t.tag }}</span>
            </span>
          </div>
        </div>
      </div>
    </section>

    <section id="quick-start" class="mx-auto max-w-6xl px-4 py-20 sm:px-6">
      <div class="text-center">
        <h2 class="text-2xl font-bold tracking-tight sm:text-3xl">三步接入 JBM</h2>
        <p class="mt-3 text-muted-foreground">从注册到集成，跟随开发者文档即可完成 OAuth2 接入</p>
      </div>
      <div class="mt-12 grid gap-8 md:grid-cols-3">
        <RouterLink
          v-for="s in steps"
          :key="s.step"
          :to="s.link"
          class="group relative rounded-xl border bg-card p-6 transition-all hover:border-primary/40 hover:shadow-lg"
        >
          <span class="text-4xl font-bold text-primary/20">{{ s.step }}</span>
          <h3 class="mt-4 text-lg font-semibold group-hover:text-primary">{{ s.title }}</h3>
          <p class="mt-2 text-sm text-muted-foreground">{{ s.desc }}</p>
          <span class="mt-4 inline-flex items-center gap-1 text-sm font-medium text-primary">
            了解更多
            <ArrowRight class="size-4 transition-transform group-hover:translate-x-0.5" />
          </span>
        </RouterLink>
      </div>
    </section>

    <section class="border-t bg-primary/5 py-16">
      <div class="mx-auto max-w-3xl px-4 text-center sm:px-6">
        <Layers class="mx-auto size-10 text-primary" />
        <h2 class="mt-4 text-2xl font-bold">准备好接入了吗？</h2>
        <p class="mt-3 text-muted-foreground">
          注册账号，创建 OAuth2 应用，查阅 OpenAPI 文档，将 JBM 认证能力集成到您的项目中。
        </p>
        <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <RouterLink to="/register">
            <Button size="lg">免费注册</Button>
          </RouterLink>
          <RouterLink to="/docs">
            <Button variant="outline" size="lg">查看 API Wiki</Button>
          </RouterLink>
        </div>
      </div>
    </section>

    <LandingFooter />
  </div>
</template>
