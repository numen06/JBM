<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RefreshCw, Save, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Input from '@/components/ui/Input.vue'
import {
  createAppFeature,
  disableAppFeature,
  getTenantFeatureContext,
  putMemberFeatures,
  putTenantFeatures,
  type TenantFeatureContext,
} from '@/api/tenantFeature'

const loading = ref(false)
const savingKey = ref('')
const error = ref('')
const notice = ref('')
const context = ref<TenantFeatureContext>()
const tenantDrafts = reactive<Record<string, string[]>>({})
const memberDrafts = reactive<Record<string, string[]>>({})
const featureForm = reactive({ featureCode: '', featureName: '', featureDesc: '' })

const tenantFeatureCodes = computed(() => new Set(context.value?.tenantFeatures.map(item => item.featureCode) ?? []))
const effectiveCodes = computed(() => new Set(context.value?.effectiveFeatureCodes ?? []))

async function load() {
  loading.value = true
  error.value = ''
  try {
    context.value = await getTenantFeatureContext()
    for (const tenant of context.value.tenants ?? []) tenantDrafts[tenant.tenantId] = [...tenant.featureCodes]
    for (const member of context.value.members ?? []) memberDrafts[member.userId] = [...member.featureCodes]
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '功能权益加载失败'
  } finally {
    loading.value = false
  }
}

function toggle(drafts: Record<string, string[]>, key: string, featureCode: string) {
  const values = drafts[key] ?? (drafts[key] = [])
  const index = values.indexOf(featureCode)
  if (index >= 0) values.splice(index, 1)
  else values.push(featureCode)
}

async function addFeature() {
  savingKey.value = 'catalog'
  error.value = ''
  notice.value = ''
  try {
    await createAppFeature({ ...featureForm })
    featureForm.featureCode = ''
    featureForm.featureName = ''
    featureForm.featureDesc = ''
    notice.value = '功能目录已保存'
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '功能目录保存失败'
  } finally {
    savingKey.value = ''
  }
}

async function removeFeature(featureCode: string) {
  savingKey.value = `catalog-${featureCode}`
  error.value = ''
  try {
    await disableAppFeature(featureCode)
    notice.value = '功能已停用，租户和子账号授权同步失效'
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '功能停用失败'
  } finally {
    savingKey.value = ''
  }
}

async function saveTenant(tenantId: string) {
  savingKey.value = `tenant-${tenantId}`
  error.value = ''
  try {
    await putTenantFeatures(tenantId, tenantDrafts[tenantId] ?? [])
    notice.value = '租户功能授权已更新'
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '租户授权失败'
  } finally {
    savingKey.value = ''
  }
}

async function saveMember(userId: string) {
  savingKey.value = `member-${userId}`
  error.value = ''
  try {
    await putMemberFeatures(userId, memberDrafts[userId] ?? [])
    notice.value = '子账号功能已更新'
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '子账号授权失败'
  } finally {
    savingKey.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-5">
    <PageHeader title="功能权益" description="平台确定租户功能上限，租户管理员只能在该上限内向子账号分配。">
      <template #actions>
        <Button variant="outline" :disabled="loading" @click="load"><RefreshCw class="size-4" />刷新</Button>
      </template>
    </PageHeader>

    <p v-if="error" class="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{{ error }}</p>
    <p v-if="notice" class="rounded-md border border-primary/30 bg-primary/10 p-3 text-sm">{{ notice }}</p>
    <div v-if="loading" class="py-12 text-center text-sm text-muted-foreground">加载中…</div>

    <template v-else-if="context">
      <Card>
        <CardContent class="space-y-3 pt-6">
          <div>
            <h2 class="font-semibold">当前账号实际功能</h2>
            <p class="text-sm text-muted-foreground">实际权限还需同时满足角色操作权限与园区数据范围。</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <Badge v-for="feature in context.catalog.filter(item => effectiveCodes.has(item.featureCode))" :key="feature.featureCode">
              {{ feature.featureName }}
            </Badge>
            <span v-if="!context.effectiveFeatureCodes.length" class="text-sm text-muted-foreground">暂未获得业务功能</span>
          </div>
        </CardContent>
      </Card>

      <Card v-if="context.platform">
        <CardContent class="space-y-4 pt-6">
          <div>
            <h2 class="font-semibold">应用功能目录</h2>
            <p class="text-sm text-muted-foreground">由平台管理员维护通用功能编码。</p>
          </div>
          <form class="grid gap-3 md:grid-cols-[1fr_1fr_2fr_auto]" @submit.prevent="addFeature">
            <Input v-model="featureForm.featureCode" required placeholder="如 energy.solar" />
            <Input v-model="featureForm.featureName" required placeholder="功能名称" />
            <Input v-model="featureForm.featureDesc" placeholder="说明" />
            <Button type="submit" :disabled="savingKey === 'catalog'">保存功能</Button>
          </form>
          <div class="flex flex-wrap gap-2">
            <span v-for="feature in context.catalog" :key="feature.featureCode" class="inline-flex items-center gap-1 rounded-md border px-2 py-1 text-xs">
              {{ feature.featureName }} · {{ feature.featureCode }}
              <button type="button" class="rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive" :disabled="savingKey === `catalog-${feature.featureCode}`" :aria-label="`停用${feature.featureName}`" @click="removeFeature(feature.featureCode)"><Trash2 class="size-3" /></button>
            </span>
          </div>
        </CardContent>
      </Card>

      <Card v-if="context.platform">
        <CardContent class="space-y-4 pt-6">
          <div>
            <h2 class="font-semibold">租户功能授权</h2>
            <p class="text-sm text-muted-foreground">平台管理员主动开通或收回，收回后子账号授权同步失效。</p>
          </div>
          <div v-for="tenant in context.tenants ?? []" :key="tenant.tenantId" class="space-y-3 rounded-md border p-4">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div><strong>{{ tenant.tenantName || tenant.tenantCode || tenant.tenantId }}</strong><span class="ml-2 font-mono text-xs text-muted-foreground">{{ tenant.tenantId }}</span></div>
              <Button size="sm" :disabled="savingKey === `tenant-${tenant.tenantId}`" @click="saveTenant(tenant.tenantId)"><Save class="size-4" />保存授权</Button>
            </div>
            <div class="flex flex-wrap gap-3">
              <label v-for="feature in context.catalog" :key="`${tenant.tenantId}-${feature.featureCode}`" class="flex cursor-pointer items-center gap-2 rounded border px-3 py-2 text-sm">
                <input type="checkbox" :checked="tenantDrafts[tenant.tenantId]?.includes(feature.featureCode)" @change="toggle(tenantDrafts, tenant.tenantId, feature.featureCode)" />
                {{ feature.featureName }}
              </label>
            </div>
          </div>
          <p v-if="!(context.tenants ?? []).length" class="text-sm text-muted-foreground">当前应用暂无接入租户。</p>
        </CardContent>
      </Card>

      <Card v-else>
        <CardContent class="space-y-4 pt-6">
          <div>
            <h2 class="font-semibold">本租户已开通功能</h2>
            <p class="text-sm text-muted-foreground">功能由平台运营管理员主动开通。</p>
          </div>
          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <div v-for="feature in context.catalog" :key="feature.featureCode" class="rounded-md border p-4">
              <div class="flex items-center justify-between gap-2"><strong>{{ feature.featureName }}</strong><Badge :variant="tenantFeatureCodes.has(feature.featureCode) ? 'default' : 'secondary'">{{ tenantFeatureCodes.has(feature.featureCode) ? '已开通' : '未开通' }}</Badge></div>
              <p class="mt-2 text-sm text-muted-foreground">{{ feature.featureDesc || feature.featureCode }}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card v-if="context.tenantAdmin && !context.platform">
        <CardContent class="space-y-4 pt-6">
          <div>
            <h2 class="font-semibold">子账号功能分配</h2>
            <p class="text-sm text-muted-foreground">只显示并允许分配本租户已开通的功能。</p>
          </div>
          <div v-for="member in context.members ?? []" :key="member.userId" class="space-y-3 rounded-md border p-4">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div><strong>{{ member.nickName || member.userName }}</strong><span class="ml-2 text-xs text-muted-foreground">{{ member.userName }}</span></div>
              <Button v-if="member.userId !== context.userId" size="sm" :disabled="savingKey === `member-${member.userId}`" @click="saveMember(member.userId)"><Save class="size-4" />保存分配</Button>
              <Badge v-else variant="outline">租户管理员</Badge>
            </div>
            <div class="flex flex-wrap gap-3">
              <label v-for="feature in context.catalog.filter(item => tenantFeatureCodes.has(item.featureCode))" :key="`${member.userId}-${feature.featureCode}`" class="flex items-center gap-2 rounded border px-3 py-2 text-sm" :class="member.userId === context.userId ? 'opacity-60' : 'cursor-pointer'">
                <input type="checkbox" :disabled="member.userId === context.userId" :checked="member.userId === context.userId || memberDrafts[member.userId]?.includes(feature.featureCode)" @change="toggle(memberDrafts, member.userId, feature.featureCode)" />
                {{ feature.featureName }}
              </label>
              <span v-if="!context.tenantFeatures.length" class="text-sm text-muted-foreground">租户尚未开通任何功能</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </template>
  </div>
</template>
