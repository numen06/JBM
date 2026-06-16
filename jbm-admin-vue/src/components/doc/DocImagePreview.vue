<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RotateCcw, ZoomIn, ZoomOut } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'

const props = withDefaults(
  defineProps<{
    src: string
    alt?: string
    loading?: boolean
  }>(),
  {
    alt: '图片预览',
    loading: false,
  },
)

const zoom = ref(1)
const naturalSize = ref({ width: 0, height: 0 })

const zoomLabel = computed(() => `${Math.round(zoom.value * 100)}%`)

function resetZoom() {
  zoom.value = 1
}

function zoomIn() {
  zoom.value = Math.min(4, Number((zoom.value + 0.25).toFixed(2)))
}

function zoomOut() {
  zoom.value = Math.max(0.25, Number((zoom.value - 0.25).toFixed(2)))
}

function onWheel(event: WheelEvent) {
  event.preventDefault()
  if (event.deltaY < 0) zoomIn()
  else zoomOut()
}

function onImageLoad(event: Event) {
  const image = event.target as HTMLImageElement
  naturalSize.value = {
    width: image.naturalWidth,
    height: image.naturalHeight,
  }
}

watch(
  () => props.src,
  () => {
    resetZoom()
    naturalSize.value = { width: 0, height: 0 }
  },
)
</script>

<template>
  <div class="space-y-2">
    <div class="flex flex-wrap items-center justify-between gap-2">
      <div class="text-xs text-muted-foreground">
        <span v-if="naturalSize.width && naturalSize.height">
          {{ naturalSize.width }} × {{ naturalSize.height }} px
        </span>
        <span v-else>图片预览</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="min-w-12 text-center text-xs text-muted-foreground">{{ zoomLabel }}</span>
        <Button size="sm" variant="outline" title="缩小" :disabled="loading || zoom <= 0.25" @click="zoomOut">
          <ZoomOut class="h-4 w-4" />
        </Button>
        <Button size="sm" variant="outline" title="放大" :disabled="loading || zoom >= 4" @click="zoomIn">
          <ZoomIn class="h-4 w-4" />
        </Button>
        <Button size="sm" variant="outline" title="重置缩放" :disabled="loading || zoom === 1" @click="resetZoom">
          <RotateCcw class="h-4 w-4" />
        </Button>
      </div>
    </div>

    <div
      class="relative h-[68vh] overflow-auto rounded-md border bg-[linear-gradient(45deg,#f4f4f5_25%,transparent_25%,transparent_75%,#f4f4f5_75%,#f4f4f5),linear-gradient(45deg,#f4f4f5_25%,transparent_25%,transparent_75%,#f4f4f5_75%,#f4f4f5)] bg-[length:16px_16px] bg-[position:0_0,8px_8px]"
      @wheel="onWheel"
    >
      <div
        v-if="loading"
        class="absolute inset-0 z-10 flex items-center justify-center bg-background/80 text-sm text-muted-foreground"
      >
        正在加载图片...
      </div>
      <div v-else-if="src" class="flex min-h-full min-w-full items-center justify-center p-4">
        <img
          :src="src"
          :alt="alt"
          draggable="false"
          class="max-w-none select-none transition-transform duration-150"
          :style="{ transform: `scale(${zoom})` }"
          @load="onImageLoad"
        />
      </div>
      <div v-else class="flex h-full items-center justify-center text-sm text-muted-foreground">暂无可预览图片</div>
    </div>
  </div>
</template>
