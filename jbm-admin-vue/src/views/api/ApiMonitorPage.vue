<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { Activity, Gauge, Route } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardContent from '@/components/ui/CardContent.vue'
</script>

<template>
  <div class="space-y-6">
    <PageHeader
      title="API 监控"
      description="调用统计与可观测性；当前版本为规划占位，限流与路由策略请在网关管理中配置。"
    />

    <Card>
      <CardHeader>
        <CardTitle class="flex items-center gap-2 text-base">
          <Activity class="h-5 w-5 text-muted-foreground" />
          功能规划
        </CardTitle>
      </CardHeader>
      <CardContent class="space-y-4 text-sm text-muted-foreground">
        <p>后续将对接网关或 Center 的调用指标接口，展示：</p>
        <ul class="list-inside list-disc space-y-1">
          <li>按服务 / 路径的 QPS、延迟、错误率</li>
          <li>Top 慢接口与 4xx/5xx 排行</li>
          <li>与限流策略命中次数联动</li>
        </ul>
        <p>在指标 API 就绪前，可使用以下现有能力：</p>
        <div class="flex flex-wrap gap-2 pt-2">
          <RouterLink
            to="/gateway/rate-limit"
            class="inline-flex h-9 items-center rounded-md border border-input bg-background px-4 text-sm font-medium hover:bg-accent"
          >
            <Gauge class="mr-1 h-4 w-4" />
            限流管理
          </RouterLink>
          <RouterLink
            to="/gateway/routes"
            class="inline-flex h-9 items-center rounded-md border border-input bg-background px-4 text-sm font-medium hover:bg-accent"
          >
            <Route class="mr-1 h-4 w-4" />
            路由管理
          </RouterLink>
          <RouterLink
            to="/api/registry"
            class="inline-flex h-9 items-center rounded-md border border-input bg-background px-4 text-sm font-medium hover:bg-accent"
          >
            API 注册中心
          </RouterLink>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
