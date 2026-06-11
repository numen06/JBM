<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPublishedDoc } from '@/api/openapiDocs'

const route = useRoute()
const loading = ref(false)
const content = ref('')
const error = ref('')

const docKey = computed(() => String(route.params.docKey || ''))
const isHtml = computed(() => /^\s*(<!doctype html|<html[\s>])/i.test(content.value))

async function loadDoc() {
  if (!docKey.value) {
    error.value = '文档不存在'
    return
  }
  loading.value = true
  error.value = ''
  try {
    content.value = await getPublishedDoc(docKey.value)
    if (!content.value) {
      error.value = '文档不存在或已下线'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '文档加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDoc)
watch(docKey, loadDoc)
</script>

<template>
  <div class="min-h-screen bg-background">
    <div v-if="loading" class="p-6 text-sm text-muted-foreground">加载中...</div>
    <div v-else-if="error" class="p-6 text-sm text-destructive">{{ error }}</div>
    <iframe
      v-else-if="isHtml"
      :srcdoc="content"
      title="OpenAPI 文档"
      class="h-screen w-full border-0"
    />
    <pre v-else class="min-h-screen overflow-auto bg-slate-950 p-6 text-xs text-slate-100"><code>{{ content }}</code></pre>
  </div>
</template>
