<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Search } from '@lucide/vue'
import LandingNav from '@/components/landing/LandingNav.vue'
import LandingFooter from '@/components/landing/LandingFooter.vue'
import Badge from '@/components/ui/Badge.vue'
import Input from '@/components/ui/Input.vue'
import { listPublishedDocs, getPublishedDoc } from '@/api/openapiDocs'
import type { PublishedDocSummary } from '@/api/types'
import { authEndpoints, docSections, gatewayBase, openApiHeaders } from './apiWikiContent'

const route = useRoute()
const searchQuery = ref('')
const activeSection = ref('quick-start')
const publishedDocs = ref<PublishedDocSummary[]>([])
const selectedPublishedKey = ref('')
const publishedSpecPreview = ref('')
const publishedLoading = ref(false)

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
  loadPublishedDocs()
})

async function loadPublishedDocs() {
  try {
    publishedDocs.value = await listPublishedDocs()
    if (publishedDocs.value.length) {
      selectedPublishedKey.value = publishedDocs.value[0].docKey ?? ''
      await loadPublishedSpec(selectedPublishedKey.value)
    }
  } catch {
    publishedDocs.value = []
  }
}

async function loadPublishedSpec(docKey: string) {
  if (!docKey) {
    publishedSpecPreview.value = ''
    return
  }
  publishedLoading.value = true
  try {
    const spec = await getPublishedDoc(docKey)
    publishedSpecPreview.value = spec ?? ''
  } catch {
    publishedSpecPreview.value = ''
  } finally {
    publishedLoading.value = false
  }
}

watch(selectedPublishedKey, (key) => {
  loadPublishedSpec(key)
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
            <Input v-model="searchQuery" placeholder="搜索文档..." class="pl-9" />
          </div>
          <nav class="space-y-5">
            <div v-for="[group, items] in groups" :key="group">
              <p class="mb-2 text-xs font-semibold uppercase text-muted-foreground">{{ group }}</p>
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
          <span class="ml-3 text-muted-foreground">用户鉴权：</span>
          <code class="rounded bg-background px-2 py-0.5 font-mono text-xs">Authorization: Bearer &lt;token&gt;</code>
        </div>

        <section id="quick-start" class="scroll-mt-24 border-b pb-12">
          <h1 class="text-3xl font-bold tracking-tight">JBM OpenAPI Wiki</h1>
          <p class="mt-4 text-lg leading-8 text-muted-foreground">
            本文档从用户视角说明如何注册登录 JBM、创建子应用、申请开放 API、授权 API Key，
            并通过加密传输和签名请求接入 JBM OpenAPI。
          </p>
          <div class="mt-6 grid gap-4 sm:grid-cols-2">
            <RouterLink to="/register" class="rounded-lg border p-4 transition-colors hover:border-primary/40 hover:bg-primary/5">
              <p class="font-semibold">第三方开发者</p>
              <p class="mt-1 text-sm text-muted-foreground">注册账号 -> 创建 OAuth2 应用 -> 申请 API Key -> 签名调用</p>
            </RouterLink>
            <RouterLink to="/dashboard" class="rounded-lg border p-4 transition-colors hover:border-primary/40 hover:bg-primary/5">
              <p class="font-semibold">内部业务开发者</p>
              <p class="mt-1 text-sm text-muted-foreground">登录控制台 -> 管理用户/角色/菜单 -> 通过 Gateway 调用 Center API</p>
            </RouterLink>
          </div>
        </section>

        <section id="platform-capability" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">平台能力地图</h2>
          <div class="mt-4 overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b text-left">
                  <th class="py-2 pr-4">能力</th>
                  <th class="py-2 pr-4">用户动作</th>
                  <th class="py-2">系统边界</th>
                </tr>
              </thead>
              <tbody class="text-muted-foreground">
                <tr class="border-b">
                  <td class="py-3 pr-4 font-medium text-foreground">种子数据</td>
                  <td class="py-3 pr-4">初始化 demo client、管理员、基础菜单与开放权限</td>
                  <td class="py-3">保证新环境可完成注册、登录和授权验证</td>
                </tr>
                <tr class="border-b">
                  <td class="py-3 pr-4 font-medium text-foreground">多租户</td>
                  <td class="py-3 pr-4">账号归属租户，角色和数据按租户上下文过滤</td>
                  <td class="py-3">Center 负责租户模型，Gateway 透传可信上下文</td>
                </tr>
                <tr class="border-b">
                  <td class="py-3 pr-4 font-medium text-foreground">子应用接入</td>
                  <td class="py-3 pr-4">创建 OAuth2 Client，配置回调地址和授权范围</td>
                  <td class="py-3">Auth 签发 Token，业务系统只保存自己的 client_secret</td>
                </tr>
                <tr>
                  <td class="py-3 pr-4 font-medium text-foreground">客户端授权访问</td>
                  <td class="py-3 pr-4">申请开发者、创建 API Key、绑定 API 权限</td>
                  <td class="py-3">Gateway 校验签名与授权，后端只接受可信转发</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section id="register-account" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">1. 注册 JBM 账号</h2>
          <p class="mt-3 text-muted-foreground">
            访问
            <RouterLink to="/register" class="text-primary hover:underline">注册页面</RouterLink>
            或调用注册 API。密码必须先通过 <code class="rounded bg-muted px-1">/oauth2/publicKey</code> 获取公钥后加密。
          </p>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>POST {{ gatewayBase }}/oauth2/register
Content-Type: application/x-www-form-urlencoded
X-Password-Encrypted: true

userName=mydev&amp;password=&lt;RSA_ENCRYPTED&gt;&amp;vcode=9999&amp;client_id=demo</code></pre>
        </section>

        <section id="create-app" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">2. 创建子应用</h2>
          <ol class="mt-4 list-decimal space-y-2 pl-5 text-muted-foreground">
            <li>登录控制台，进入应用管理或开放平台应用页面。</li>
            <li>填写应用名称、租户归属、回调地址 <code class="rounded bg-muted px-1">redirect_uri</code> 和 Scope。</li>
            <li>保存后获取 <code class="rounded bg-muted px-1">client_id</code> 与 <code class="rounded bg-muted px-1">client_secret</code>。</li>
            <li>生产环境只在服务端保存 Secret，不放入浏览器或移动端包体。</li>
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
                  <td class="py-3 pr-4">第三方 Web / 移动端登录，推荐使用</td>
                  <td class="py-3"><code>authorization_code</code></td>
                </tr>
                <tr class="border-b">
                  <td class="py-3 pr-4 font-medium text-foreground">密码</td>
                  <td class="py-3 pr-4">JBM 官方后台或受信任第一方应用</td>
                  <td class="py-3"><code>password</code></td>
                </tr>
                <tr>
                  <td class="py-3 pr-4 font-medium text-foreground">客户端凭证</td>
                  <td class="py-3 pr-4">服务端应用访问开放 API</td>
                  <td class="py-3"><code>client_credentials</code></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section id="oauth2-auth-code" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">OAuth2 授权码模式</h2>
          <p class="mt-3 text-muted-foreground">用户浏览器跳转 JBM 授权页，登录并授权后回调业务系统，业务服务端再用 code 换 Token。</p>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>{{ gatewayBase }}/oauth2/authorize?response_type=code&amp;client_id=YOUR_CLIENT_ID&amp;redirect_uri=YOUR_CALLBACK&amp;scope=all&amp;state=RANDOM_STATE</code></pre>
        </section>

        <section id="oauth2-login-code" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">登录授权码</h2>
          <p class="mt-3 text-muted-foreground">第一方应用登录先获取一次性 code，再使用 authorization_code 换取 Token。</p>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>POST {{ gatewayBase }}/oauth2/doLogin
Content-Type: application/x-www-form-urlencoded
X-Password-Encrypted: true

response_type=code&amp;client_id=demo&amp;redirect_uri=YOUR_CALLBACK&amp;state=RANDOM_STATE&amp;username=admin&amp;password=&lt;RSA&gt;&amp;scope=all&amp;loginType=PASSWORD&amp;vcode=9999</code></pre>
        </section>

        <section id="oauth2-refresh" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">刷新 Token</h2>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>POST {{ gatewayBase }}/oauth2/refresh
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&amp;client_id=demo&amp;refresh_token=YOUR_REFRESH_TOKEN</code></pre>
        </section>

        <section id="openapi-api-key" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">API Key 与签名调用</h2>
          <p class="mt-3 text-muted-foreground">
            开发者通过控制台申请 API Key，并由管理员绑定可访问的 OpenAPI 权限。调用时网关校验签名、时间戳、
            nonce 与授权清单，验证成功后再转发给后端服务。
          </p>
          <div class="mt-5 overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b text-left">
                  <th class="py-2 pr-4">Header</th>
                  <th class="py-2">说明</th>
                </tr>
              </thead>
              <tbody class="text-muted-foreground">
                <tr v-for="header in openApiHeaders" :key="header.name" class="border-b">
                  <td class="py-3 pr-4 font-mono text-xs text-foreground">{{ header.name }}</td>
                  <td class="py-3">{{ header.desc }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>GET {{ gatewayBase }}/api/open/v1/user/profile
X-JBM-Api-Key: ak_xxx
X-JBM-Timestamp: 1760000000000
X-JBM-Nonce: 1d48f4c2-9a2c-47ac
X-JBM-Signature: hex(hmac_sha256(secret, canonicalRequest))</code></pre>
        </section>

        <section id="openapi-isolation" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">租户与数据隔离</h2>
          <p class="mt-3 text-muted-foreground">
            多租户场景下，登录 Token、开发者身份、应用、API Key 与业务数据都应携带租户上下文。
            后端服务只信任 Gateway 或 Auth 生成的上下文，不信任外部请求伪造的内部头。
          </p>
          <ul class="mt-4 space-y-2 text-sm text-muted-foreground">
            <li>外部 Bearer Token 必须由 Auth 校验，不能因为携带内部头而绕过。</li>
            <li>服务间请求使用 Sa-Token IdToken 或网关验证后的 API Key 上下文。</li>
            <li>租户字段应参与查询过滤、权限判断、审计日志和 API Key 授权范围。</li>
          </ul>
        </section>

        <section id="api-auth" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">认证接口 /oauth2/*</h2>
          <div class="mt-6 space-y-8">
            <div v-for="ep in authEndpoints" :key="ep.path + ep.method" class="rounded-lg border p-5">
              <div class="flex flex-wrap items-center gap-2">
                <span class="rounded px-2 py-0.5 font-mono text-xs font-semibold" :class="methodColor(ep.method)">
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
          <p class="mt-3 text-muted-foreground">需要 Bearer Token，由 Center 服务提供，经 Gateway 转发。</p>
          <ul class="mt-4 space-y-2 text-sm text-muted-foreground">
            <li><Badge variant="outline" class="mr-2">GET</Badge><code>/user/info/statistics</code> - 用户统计</li>
            <li><Badge variant="outline" class="mr-2">GET</Badge><code>/user/list</code> - 用户列表，需要权限</li>
            <li><Badge variant="outline" class="mr-2">POST</Badge><code>/user/save</code> - 创建用户</li>
          </ul>
        </section>

        <section id="api-authority" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">权限接口 /authority/*</h2>
          <p class="mt-3 text-muted-foreground">维护开放 API 权限点、API Key 授权关系和 RBAC 权限。</p>
        </section>

        <section id="api-developer" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">开发者接口 /developer/*</h2>
          <p class="mt-3 text-muted-foreground">开发者申请、管理员审批、应用与 API Key 管理均通过该域完成。</p>
        </section>

        <section id="sdk-frontend" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">前端接入示例</h2>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>const url = `${gatewayBase}/oauth2/authorize`
location.href = `${url}?response_type=code&amp;client_id=${clientId}&amp;redirect_uri=${callback}&amp;scope=all&amp;state=${state}`</code></pre>
        </section>

        <section id="sdk-backend" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">后端接入示例</h2>
          <pre class="mt-4 overflow-x-auto rounded-lg bg-slate-950 p-4 text-sm text-slate-50"><code>String canonical = method + "\\n" + path + "\\n" + query + "\\n" + bodyHash + "\\n" + timestamp + "\\n" + nonce;
String signature = hmacSha256Hex(apiSecret, canonical);</code></pre>
        </section>

        <section id="published-api" class="scroll-mt-24 border-b py-12">
          <h2 class="text-2xl font-bold">已发布开放 API 文档</h2>
          <p class="mt-3 text-muted-foreground">
            本节仅展示经管理员发布确认的 API 快照，不包含内部接口目录，也不提供在线测试。
          </p>
          <div v-if="!publishedDocs.length" class="mt-6 rounded-lg border border-dashed p-6 text-sm text-muted-foreground">
            暂无已发布接口文档，请先通过管理端「API 文档与调试」发布公开快照。
          </div>
          <div v-else class="mt-6 space-y-4">
            <select
              v-model="selectedPublishedKey"
              class="rounded-md border bg-background px-3 py-2 text-sm"
            >
              <option v-for="doc in publishedDocs" :key="doc.docKey" :value="doc.docKey">
                {{ doc.title }} ({{ doc.version }})
              </option>
            </select>
            <p v-if="publishedDocs.find((d) => d.docKey === selectedPublishedKey)?.publishedSummary" class="text-sm text-muted-foreground">
              {{ publishedDocs.find((d) => d.docKey === selectedPublishedKey)?.publishedSummary }}
            </p>
            <div v-if="publishedLoading" class="text-sm text-muted-foreground">加载中...</div>
            <pre
              v-else-if="publishedSpecPreview"
              class="max-h-[480px] overflow-auto rounded-lg bg-slate-950 p-4 text-xs text-slate-100"
            ><code>{{ publishedSpecPreview }}</code></pre>
          </div>
        </section>

        <section id="faq" class="scroll-mt-24 py-12">
          <h2 class="text-2xl font-bold">常见问题</h2>
          <div class="mt-4 space-y-4 text-sm text-muted-foreground">
            <p><strong class="text-foreground">为什么不能直接信任内部头？</strong> 外部请求可以伪造 Header，必须由 Gateway 或 Auth 校验后签发可信上下文。</p>
            <p><strong class="text-foreground">密码为什么需要 RSA？</strong> 即使在开发环境，也保持与生产一致的加密传输约束。</p>
            <p><strong class="text-foreground">API Key 和用户 Token 有什么区别？</strong> 用户 Token 代表登录用户，API Key 代表开发者或应用的开放接口访问权限。</p>
            <p><strong class="text-foreground">开发者未审批时为什么不能创建 API Key？</strong> 自助注册用户需先提交开发者申请并由管理员审批；未通过前开放平台仅展示申请入口，API Key 创建按钮保持禁用。</p>
            <p><strong class="text-foreground">API Key 被禁用或过期怎么办？</strong> 调用开放接口将返回 401/403；请在控制台检查 Key 状态、授权应用与过期时间，必要时重新创建并保存一次性 Secret。</p>
            <p><strong class="text-foreground">IP 不在白名单会怎样？</strong> 网关 IP 限制策略命中后拒绝请求；请确认调用方出口 IP 已加入对应策略，或联系管理员调整白名单。</p>
            <p><strong class="text-foreground">限流触发后怎么办？</strong> 返回 429 或网关统一错误体；请降低调用频率、申请更高配额，或由管理员在「网关限流」中调整策略。</p>
            <p><strong class="text-foreground">跨租户访问为什么失败？</strong> 扩展字段、API Key、网关策略等按请求头 <code>tenantId</code> 隔离；租户 A 的 Token/Key 不能读取或修改租户 B 的资源。未传租户时默认使用模块配置的 default-tenant-id（通常为 0）。</p>
          </div>
        </section>
      </main>
    </div>

    <LandingFooter />
  </div>
</template>
