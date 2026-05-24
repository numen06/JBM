<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Search } from '@lucide/vue'
import LandingNav from '@/components/landing/LandingNav.vue'
import LandingFooter from '@/components/landing/LandingFooter.vue'
import Badge from '@/components/ui/Badge.vue'
import Input from '@/components/ui/Input.vue'
import { authEndpoints, docSections, gatewayBase } from './apiWikiContent'

const route = useRoute()
const searchQuery = ref('')
const activeSection = ref('quick-start')

const groups = computed(() => {
  const map = new Map<string, typeof docSections>()
  for (const s of docSections) {
    if (!map.has(s.group)) map.set(s.group, [])
    map.get(s.group)!.push(s)
  }
  return [...map.entries()]
})

const filteredSections = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return docSections
  return docSections.filter((s) => s.title.toLowerCase().includes(q) || s.id.includes(q))
})

function scrollTo(id: string) {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function methodColor(method: string) {
  const map: Record<string, string> = {
    GET: 'bg-emerald-100 text-emerald-800',
    POST: 'bg-blue-100 text-blue-800',
    PUT: 'bg-amber-100 text-amber-800',
    DELETE: 'bg-red-100 text-red-800',
  }
  return map[method] ?? 'bg-muted text-foreground'
}

onMounted(() => {
  const hash = route.hash.replace('#', '')
  if (hash) scrollTo(hash)
})

watch(
  () => route.hash,
  (hash) => {
    const id = hash.replace('#', '')
    if (id) activeSection.value = id
  },
)
</script>

<template>
  <div class="min-h-screen bg-background">
    <LandingNav />

    <div class="mx-auto flex max-w-7xl gap-8 px-4 py-8 sm:px-6 lg:py-10">
      <aside class="hidden w-56 shrink-0 lg:block">
        <div class="sticky top-20 space-y-6">
          <div class="relative">
            <Search class="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
            <Input v-model="searchQuery" placeholder="搜索文档…" class="pl-9" />
          </div>
          <nav class="space-y-5">
            <div v-for="[group, items] in groups" :key="group">
              <p class="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{{ group }}</p>
              <ul class="space-y-0.5">
                <li v-for="item in items.filter((i) => filteredSections.some((f) => f.id === i.id))" :key="item.id">
                  <button
                    type="button"
                    class="w-full rounded-md px-2 py-1.5 text-left text-sm transition-colors"
                    :class="
                      activeSection === item.id
                        ? 'bg-primary/10 font-medium text-primary'
                        : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                    "
                    @click="scrollTo(item.id)"
                  >
                    {{ item.title }}
                  </button>
                </li>
              </ul>
            </div>
          </nav>
        </div>
      </aside>

      <main class="min-w-0 flex-1 pb-16">
        <div class="mb-8 rounded-lg border bg-muted/30 px-4 py-3 text-sm">
          <span class="text-muted-foreground">API Gateway 基址：</span>
          <code class="rounded bg-background px-2 py-0.5 font-mono text-primary">{{ gatewayBase }}</code>
          <span class="ml-3 text-muted-foreground">鉴权头：</span>
          <code class="rounded bg-background px-2 py-0.5 font-mono text-xs">Authorization: Bearer &lt;token&gt;</code>
        </div>

        <section id="quick-start" class="scroll-mt-24 border-b pb-12">
          <h1 class="text-3xl font-bold tracking-tight">JBM OpenAPI 文档</h1>
          <p class="mt-4 text-lg text-muted-foreground">
            本文档面向<strong>第三方开发者</strong>与<strong>内部业务开发者</strong>，说明如何通过 JBM OAuth2 完成注册、创建应用、获取凭证并接入认证体系。
          </p>
          <div class="mt-6 grid gap-4 sm:grid-cols-2">
            <RouterLink
              to="/register"
              class="rounded-lg border p-4 transition-colors hover:border-primary/40 hover:bg-primary/5"
            >
              <p class="font-semibold">第三方开发者</p>
              <p class="mt-1 text-sm text-muted-foreground">注册账号 → 创建 OAuth2 应用 → 集成授权码登录</p>
            </RouterLink>
            <RouterLink
              to="/dashboard"
              class="rounded-lg border p-4 transition-colors hover:border-primary/40 hover:bg-primary/5"
            >
              <p class="font-semibold">内部开发者</p>
              <p class="mt-1 text-sm text-muted-foreground">登录控制台 → 管理用户/角色/菜单 → 调用 Center API</p>
            </RouterLink>
          </div>
        </section>

        <section id="register-account" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">1. 注册 JBM 账号</h2>
          <p class="mt-3 text-muted-foreground">
            访问
            <RouterLink to="/register" class="text-primary hover:underline">注册页面</RouterLink>
            或调用注册 API。注册成功后使用用户名密码登录控制台。
          </p>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>POST {{ gatewayBase }}/oauth2/register
Content-Type: application/x-www-form-urlencoded
X-Password-Encrypted: true

userName=mydev&amp;password=&lt;RSA加密&gt;&amp;vcode=9999&amp;client_id=demo&amp;client_secret=demo123</code></pre>
          <p class="mt-3 text-sm text-muted-foreground">
            密码须先调用 <code class="rounded bg-muted px-1">GET /oauth2/publicKey?client_id=...</code> 获取 RSA 公钥后加密。
          </p>
        </section>

        <section id="create-app" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">2. 创建应用（获取 Client ID / Secret）</h2>
          <ol class="mt-4 list-decimal space-y-2 pl-5 text-muted-foreground">
            <li>登录后进入控制台 → <strong>应用管理</strong></li>
            <li>新建 OAuth2 应用，填写应用名称与回调地址 <code class="rounded bg-muted px-1">redirect_uri</code></li>
            <li>保存后获得 <code class="rounded bg-muted px-1">client_id</code> 与 <code class="rounded bg-muted px-1">client_secret</code></li>
            <li>在业务系统中配置上述凭证，勿泄露 Secret</li>
          </ol>
        </section>

        <section id="choose-mode" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">3. 选择接入方式</h2>
          <div class="mt-4 overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b text-left">
                  <th class="py-2 pr-4">模式</th>
                  <th class="py-2 pr-4">适用场景</th>
                  <th class="py-2">grant_type</th>
                </tr>
              </thead>
              <tbody class="text-muted-foreground">
                <tr class="border-b">
                  <td class="py-3 pr-4 font-medium text-foreground">授权码</td>
                  <td class="py-3 pr-4">Web / 移动端（推荐）</td>
                  <td class="py-3"><code>authorization_code</code></td>
                </tr>
                <tr class="border-b">
                  <td class="py-3 pr-4 font-medium text-foreground">密码</td>
                  <td class="py-3 pr-4">受信任的第一方应用</td>
                  <td class="py-3"><code>password</code></td>
                </tr>
                <tr>
                  <td class="py-3 pr-4 font-medium text-foreground">刷新</td>
                  <td class="py-3 pr-4">Token 续期</td>
                  <td class="py-3"><code>refresh_token</code></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section id="oauth2-auth-code" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">OAuth2 授权码模式（推荐）</h2>
          <p class="mt-3 text-muted-foreground">标准三方登录流程，用户浏览器跳转 JBM 授权页，确认后回调携带 code。</p>
          <ol class="mt-4 list-decimal space-y-3 pl-5 text-sm text-muted-foreground">
            <li>
              引导用户访问授权 URL：
              <pre class="mt-2 overflow-x-auto rounded-lg bg-slate-950 p-3 text-slate-50"><code>{{ gatewayBase }}/oauth2/authorize?response_type=code&amp;client_id=YOUR_CLIENT_ID&amp;redirect_uri=YOUR_CALLBACK&amp;scope=all&amp;state=RANDOM_STATE</code></pre>
            </li>
            <li>用户登录并授权，浏览器重定向至 <code class="rounded bg-muted px-1">redirect_uri?code=xxx&amp;state=xxx</code></li>
            <li>
              服务端用 code 换 Token：
              <pre class="mt-2 overflow-x-auto rounded-lg bg-slate-950 p-3 text-slate-50"><code>POST {{ gatewayBase }}/oauth2/token
grant_type=authorization_code&amp;code=xxx&amp;client_id=...&amp;client_secret=...&amp;redirect_uri=...</code></pre>
            </li>
            <li>后续 API 请求携带 <code class="rounded bg-muted px-1">Authorization: Bearer {access_token}</code></li>
          </ol>
        </section>

        <section id="oauth2-password" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">OAuth2 密码模式</h2>
          <p class="mt-3 text-muted-foreground">适用于 JBM 官方管理后台等第一方应用，密码经 RSA 加密传输。</p>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>POST {{ gatewayBase }}/oauth2/token
Content-Type: application/x-www-form-urlencoded
X-Password-Encrypted: true

grant_type=password&amp;client_id=demo&amp;client_secret=demo123&amp;username=admin&amp;password=&lt;RSA&gt;&amp;scope=all&amp;loginType=PASSWORD&amp;vcode=9999</code></pre>
        </section>

        <section id="oauth2-refresh" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">刷新 Token</h2>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>POST {{ gatewayBase }}/oauth2/refresh
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&amp;client_id=demo&amp;client_secret=demo123&amp;refresh_token=YOUR_REFRESH_TOKEN</code></pre>
        </section>

        <section id="api-auth" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">认证接口 /oauth2/*</h2>
          <div class="mt-6 space-y-8">
            <div v-for="ep in authEndpoints" :key="ep.path + ep.method" class="rounded-lg border p-5">
              <div class="flex flex-wrap items-center gap-2">
                <span
                  class="rounded px-2 py-0.5 font-mono text-xs font-semibold"
                  :class="methodColor(ep.method)"
                >
                  {{ ep.method }}
                </span>
                <code class="font-mono text-sm">{{ ep.path }}</code>
              </div>
              <p class="mt-2 text-sm text-muted-foreground">{{ ep.desc }}</p>
              <table v-if="ep.params?.length" class="mt-4 w-full text-sm">
                <thead>
                  <tr class="border-b text-left text-muted-foreground">
                    <th class="py-1 pr-3">参数</th>
                    <th class="py-1 pr-3">类型</th>
                    <th class="py-1 pr-3">必填</th>
                    <th class="py-1">说明</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="p in ep.params" :key="p.name" class="border-b border-dashed">
                    <td class="py-2 pr-3 font-mono text-xs">{{ p.name }}</td>
                    <td class="py-2 pr-3 text-muted-foreground">{{ p.type }}</td>
                    <td class="py-2 pr-3">{{ p.required ? '是' : '否' }}</td>
                    <td class="py-2 text-muted-foreground">{{ p.desc }}</td>
                  </tr>
                </tbody>
              </table>
              <pre v-if="ep.request" class="mt-4 overflow-x-auto rounded-md bg-slate-950 p-3 text-xs text-slate-50"><code>{{ ep.request }}</code></pre>
              <pre v-if="ep.response" class="mt-2 overflow-x-auto rounded-md bg-slate-900 p-3 text-xs text-slate-300"><code>{{ ep.response }}</code></pre>
            </div>
          </div>
        </section>

        <section id="api-user" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">用户接口 /user/*</h2>
          <p class="mt-3 text-muted-foreground">需 Bearer Token，由 Center 服务提供（经 Gateway 转发）。</p>
          <ul class="mt-4 space-y-2 text-sm text-muted-foreground">
            <li><Badge variant="outline" class="mr-2">GET</Badge><code>/user/info/statistics</code> — 用户统计</li>
            <li><Badge variant="outline" class="mr-2">GET</Badge><code>/user/list</code> — 用户列表（需权限）</li>
            <li><Badge variant="outline" class="mr-2">POST</Badge><code>/user/save</code> — 创建用户</li>
            <li><Badge variant="outline" class="mr-2">GET</Badge><code>/current/user</code> — 当前登录用户信息</li>
          </ul>
        </section>

        <section id="api-authority" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">权限接口 /authority/*</h2>
          <ul class="mt-4 space-y-2 text-sm text-muted-foreground">
            <li><code>/authority/menu</code> — 当前用户菜单树</li>
            <li><code>/authority/role/list</code> — 角色列表</li>
            <li><code>/authority/action/list</code> — 按钮权限</li>
          </ul>
        </section>

        <section id="api-developer" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">开发者接口 /developer/*</h2>
          <ul class="mt-4 space-y-2 text-sm text-muted-foreground">
            <li><code>/developer/list</code> — 开发者列表</li>
            <li><code>/developer/apikey/list</code> — API Key 管理</li>
            <li><code>/app/list</code> — OAuth2 应用列表</li>
          </ul>
        </section>

        <section id="sdk-frontend" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">前端接入示例（Vue 3）</h2>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>// 1. 跳转授权
const state = crypto.randomUUID()
sessionStorage.setItem('oauth_state', state)
location.href = `${GATEWAY}/oauth2/authorize?response_type=code&amp;client_id=${CLIENT_ID}&amp;redirect_uri=${encodeURIComponent(CALLBACK)}&amp;scope=all&amp;state=${state}`

// 2. 回调页用 code 换 token
const body = new URLSearchParams({
  grant_type: 'authorization_code',
  code,
  client_id: CLIENT_ID,
  client_secret: CLIENT_SECRET,
  redirect_uri: CALLBACK,
})
const res = await fetch(`${GATEWAY}/oauth2/token`, { method: 'POST', body })
const { access_token } = await res.json()

// 3. 请求 API
fetch(`${GATEWAY}/current/user`, {
  headers: { Authorization: `Bearer ${access_token}` },
})</code></pre>
        </section>

        <section id="sdk-backend" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">后端接入示例（Spring Boot）</h2>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>@Configuration
@EnableJbmOAuth2ResourceServer
public class SecurityConfig {
    // 配置 resource server，校验 JBM 颁发的 JWT / Token
}

// Feign 调用 Center API 时传递 Token
@RequestHeader("Authorization") String bearerToken</code></pre>
        </section>

        <section id="faq" class="scroll-mt-24 py-12">
          <h2 class="text-2xl font-bold">常见问题</h2>
          <dl class="mt-6 space-y-6">
            <div>
              <dt class="font-semibold">invalid client_secret 怎么办？</dt>
              <dd class="mt-1 text-sm text-muted-foreground">
                确认 client_id / client_secret 与「应用管理」中一致；开发环境可使用 demo / demo123。
              </dd>
            </div>
            <div>
              <dt class="font-semibold">密码模式报密码未加密？</dt>
              <dd class="mt-1 text-sm text-muted-foreground">
                须先 GET /oauth2/publicKey，RSA 加密密码，并设置请求头 X-Password-Encrypted: true。
              </dd>
            </div>
            <div>
              <dt class="font-semibold">Gateway 地址是什么？</dt>
              <dd class="mt-1 text-sm text-muted-foreground">
                本地默认 {{ gatewayBase }}，所有 /oauth2、/user、/current 等 API 均通过 Gateway 访问。
              </dd>
            </div>
          </dl>
        </section>
      </main>
    </div>

    <LandingFooter />
  </div>
</template>
