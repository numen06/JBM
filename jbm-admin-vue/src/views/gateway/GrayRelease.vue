<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/ui/Card.vue'
import CardContent from '@/components/ui/CardContent.vue'
</script>

<template>
  <div class="space-y-6">
    <PageHeader title="灰度发布" description="按服务版本、Header 或流量比例进行灰度路由。" />
    <Card>
      <CardContent class="space-y-3 pt-8 pb-8 text-center">
        <p class="text-lg font-medium">待接入灰度规则模型</p>
        <p class="text-sm text-muted-foreground">
          灰度规则表与 Gateway 路由联动尚未完成，当前页面为占位入口。
        </p>
      </CardContent>
    </Card>
  </div>
</template>
