<script setup lang="ts">
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  BookOpen,
  Boxes,
  Code2,
  GitBranch,
  KeyRound,
  Layers3,
  LockKeyhole,
  Network,
  Rocket,
  ShieldCheck,
  Users,
} from '@lucide/vue'
import LandingNav from '@/components/landing/LandingNav.vue'
import LandingFooter from '@/components/landing/LandingFooter.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'

const capabilities = [
  {
    icon: Users,
    title: '多租户与数据隔离',
    desc: '租户、组织、角色、用户分层建模，业务数据按租户上下文隔离。',
  },
  {
    icon: Boxes,
    title: '子应用接入',
    desc: '为每个业务系统发放 OAuth2 Client，统一回调、Scope 与权限授权。',
  },
  {
    icon: KeyRound,
    title: 'JBM OpenAPI',
    desc: 'API Key、签名调用、client_token 与授权 API 清单面向第三方开放。',
  },
  {
    icon: LockKeyhole,
    title: '加密传输',
    desc: '密码 RSA 公钥加密，网关统一校验 Token、签名与内部可信请求。',
  },
]

const clusterPractices = [
  'Gateway 只负责统一入口、鉴权转发和内部头签发',
  'Auth 独立承担 OAuth2、登录、注册和 Token 生命周期',
  'Center 维护用户、租户、RBAC、应用与开放权限',
  '服务间信任依赖 Sa-Token IdToken 或网关验证后的 API Key 上下文',
]

const steps = [
  {
    step: '01',
    title: '注册开发者账号',
    desc: '从公开注册页创建账号，使用加密密码提交到 OAuth2 注册接口。',
    link: '/register',
  },
  {
    step: '02',
    title: '创建子应用',
    desc: '登录控制台，为业务系统配置 Client ID、Secret、回调地址和 Scope。',
    link: '/docs#create-app',
  },
  {
    step: '03',
    title: '申请开放 API',
    desc: '成为开发者后创建 API Key，授权后使用签名或 client_token 调用。',
    link: '/docs#openapi-api-key',
  },
]
</script>

<template>
  <div class="min-h-screen w-full overflow-x-hidden bg-background">
    <LandingNav />

    <section class="border-b bg-[linear-gradient(180deg,#f8fafc_0%,#ffffff_78%)]">
      <div class="mx-auto grid w-full max-w-[1680px] gap-10 px-4 py-14 sm:px-6 lg:min-h-[560px] lg:grid-cols-[minmax(0,0.94fr)_minmax(420px,0.86fr)] lg:items-center lg:px-8 lg:py-16 xl:gap-16 xl:px-10">
        <div class="flex flex-col justify-center">
          <p class="mb-4 inline-flex w-fit items-center gap-2 rounded-full border bg-background px-3 py-1 text-xs font-medium text-muted-foreground shadow-sm">
            <GitBranch class="size-3.5 text-emerald-600" />
            开源社区平台 · OAuth2 · JBM OpenAPI
          </p>
          <h1 class="max-w-4xl text-balance text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            JBM
            <span class="text-primary"> 统一认证授权平台</span>
          </h1>
          <p class="mt-6 max-w-4xl text-lg leading-8 text-muted-foreground">
            面向开源社区、企业中台和第三方开发者的认证授权底座。从注册登录、子应用接入、
            多租户隔离到 API Key 签名调用，提供一条可验证的 OpenAPI 接入路径。
          </p>
          <div class="mt-8 flex flex-col gap-3 sm:flex-row">
            <RouterLink to="/register">
              <Button size="lg" class="gap-2 px-7">
                <Rocket class="size-4" />
                注册接入
              </Button>
            </RouterLink>
            <RouterLink to="/docs">
              <Button variant="outline" size="lg" class="gap-2 px-7">
                <BookOpen class="size-4" />
                OpenAPI Wiki
              </Button>
            </RouterLink>
            <a href="https://github.com" target="_blank" rel="noopener noreferrer">
              <Button variant="ghost" size="lg" class="gap-2">
                <Code2 class="size-4" />
                GitHub
              </Button>
            </a>
          </div>
          <div class="mt-8 grid max-w-4xl gap-3 text-sm text-muted-foreground sm:grid-cols-3">
            <div class="rounded-md border bg-background px-3 py-2">
              <span class="font-semibold text-foreground">OAuth2</span>
              <p>授权码 / 密码 / 刷新令牌</p>
            </div>
            <div class="rounded-md border bg-background px-3 py-2">
              <span class="font-semibold text-foreground">RBAC</span>
              <p>用户 / 角色 / 菜单 / 动作</p>
            </div>
            <div class="rounded-md border bg-background px-3 py-2">
              <span class="font-semibold text-foreground">API Key</span>
              <p>签名鉴权 / 授权访问</p>
            </div>
          </div>
        </div>

        <div class="relative flex items-center lg:justify-end">
          <div class="mx-auto w-full max-w-4xl rounded-lg border bg-slate-950 p-3 font-mono text-xs text-slate-100 shadow-2xl sm:p-4 sm:text-sm lg:mr-0 lg:max-w-[760px]">
            <div class="mb-4 flex items-center justify-between border-b border-white/10 pb-3">
              <div class="flex items-center gap-2">
                <span class="size-2.5 rounded-full bg-red-400" />
                <span class="size-2.5 rounded-full bg-amber-400" />
                <span class="size-2.5 rounded-full bg-emerald-400" />
              </div>
              <span class="text-xs text-slate-400">jbm-openapi.yaml</span>
            </div>
            <div class="space-y-4">
              <div>
                <p class="text-emerald-300">POST /oauth2/register</p>
                <p class="mt-1 text-slate-400">X-Password-Encrypted: true</p>
              </div>
              <div class="rounded-md border border-white/10 bg-white/5 p-3">
                <p><span class="text-sky-300">tenant</span>: community</p>
                <p><span class="text-sky-300">client_id</span>: jbm-demo-app</p>
                <p><span class="text-sky-300">scope</span>: openapi.read openapi.write</p>
              </div>
              <div>
                <p class="text-amber-300">GET /api/open/v1/user/profile</p>
                <p class="mt-1 text-slate-400">Authorization: Bearer client_token</p>
                <p class="text-slate-400">X-JBM-Signature: HMAC-SHA256(...)</p>
              </div>
              <div class="grid grid-cols-3 gap-2 text-xs">
                <span class="rounded bg-emerald-500/15 px-2 py-1 text-emerald-200">isolated</span>
                <span class="rounded bg-sky-500/15 px-2 py-1 text-sky-200">authorized</span>
                <span class="rounded bg-amber-500/15 px-2 py-1 text-amber-200">encrypted</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="mx-auto w-full max-w-[1680px] px-4 py-16 sm:px-6 lg:px-8 xl:px-10">
      <div class="grid gap-5 md:grid-cols-[minmax(0,1fr)_auto] md:items-end">
        <div>
          <h2 class="text-2xl font-bold tracking-tight sm:text-3xl">平台能力地图</h2>
          <p class="mt-3 max-w-2xl text-muted-foreground">
            围绕主流分布式集群做法，把认证、授权、开放接口和租户隔离拆成清晰边界。
          </p>
        </div>
        <RouterLink to="/docs#platform-capability" class="inline-flex items-center gap-1 text-sm font-medium text-primary">
          查看接入细节
          <ArrowRight class="size-4" />
        </RouterLink>
      </div>
      <div class="mt-10 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Card v-for="item in capabilities" :key="item.title" class="min-h-44 border-border/70">
          <CardHeader class="pb-2">
            <div class="mb-2 flex size-10 items-center justify-center rounded-md border bg-muted text-primary">
              <component :is="item.icon" class="size-5" />
            </div>
            <CardTitle class="text-base">{{ item.title }}</CardTitle>
          </CardHeader>
          <CardContent>
            <p class="text-sm leading-6 text-muted-foreground">{{ item.desc }}</p>
          </CardContent>
        </Card>
      </div>
    </section>

    <section class="border-y bg-muted/30 py-16">
      <div class="mx-auto grid w-full max-w-[1680px] gap-10 px-4 sm:px-6 lg:grid-cols-[minmax(300px,0.74fr)_minmax(0,1.26fr)] lg:px-8 xl:px-10">
        <div>
          <Layers3 class="size-9 text-primary" />
          <h2 class="mt-4 text-2xl font-bold tracking-tight">分布式集群边界</h2>
          <p class="mt-3 text-muted-foreground">
            Gateway、Auth、Center 各司其职，内部服务不盲信外部 Bearer Token，避免伪造内部头绕过鉴权。
          </p>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <div v-for="practice in clusterPractices" :key="practice" class="flex items-start gap-3 rounded-md border bg-background p-3 text-sm">
            <ShieldCheck class="mt-0.5 size-4 shrink-0 text-emerald-600" />
            <span>{{ practice }}</span>
          </div>
        </div>
      </div>
    </section>

    <section id="quick-start" class="mx-auto w-full max-w-[1680px] px-4 py-16 sm:px-6 lg:px-8 xl:px-10">
      <div class="max-w-3xl">
        <h2 class="text-2xl font-bold tracking-tight sm:text-3xl">从用户视角接入 JBM</h2>
        <p class="mt-3 text-muted-foreground">从注册登录开始，逐步完成子应用接入、API 授权和加密调用。</p>
      </div>
      <div class="mt-10 grid gap-4 md:grid-cols-3">
        <RouterLink
          v-for="s in steps"
          :key="s.step"
          :to="s.link"
          class="group min-h-56 rounded-lg border bg-card p-6 transition-colors hover:border-primary/50 hover:bg-primary/5"
        >
          <span class="text-sm font-semibold text-primary">{{ s.step }}</span>
          <h3 class="mt-4 text-lg font-semibold group-hover:text-primary">{{ s.title }}</h3>
          <p class="mt-2 text-sm leading-6 text-muted-foreground">{{ s.desc }}</p>
          <span class="mt-5 inline-flex items-center gap-1 text-sm font-medium text-primary">
            继续
            <ArrowRight class="size-4 transition-transform group-hover:translate-x-0.5" />
          </span>
        </RouterLink>
      </div>
    </section>

    <section class="border-t bg-background py-14">
      <div class="mx-auto flex w-full max-w-[1680px] flex-col gap-6 px-4 sm:px-6 md:flex-row md:items-center md:justify-between lg:px-8 xl:px-10">
        <div>
          <Network class="size-8 text-primary" />
          <h2 class="mt-3 text-2xl font-bold">准备开放你的第一个 JBM API？</h2>
          <p class="mt-2 max-w-2xl text-muted-foreground">
            注册账号、创建应用、申请开发者、授权 API Key，然后按 Wiki 的签名规则调用 JBM OpenAPI。
          </p>
        </div>
        <div class="flex gap-3">
          <RouterLink to="/register">
            <Button size="lg">开始注册</Button>
          </RouterLink>
          <RouterLink to="/docs#openapi-api-key">
            <Button variant="outline" size="lg">查看签名</Button>
          </RouterLink>
        </div>
      </div>
    </section>

    <LandingFooter />
  </div>
</template>
