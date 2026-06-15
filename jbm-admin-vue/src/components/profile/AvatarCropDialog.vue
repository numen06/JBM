<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import Dialog from '@/components/ui/Dialog.vue'
import Button from '@/components/ui/Button.vue'

const props = defineProps<{
  open: boolean
  file: File | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  confirm: [file: File]
}>()

const VIEWPORT_SIZE = 280
const OUTPUT_SIZE = 256

const imageUrl = ref('')
const imageEl = ref<HTMLImageElement | null>(null)
const naturalSize = ref({ width: 0, height: 0 })
const offset = ref({ x: 0, y: 0 })
const zoom = ref(1)
const dragging = ref(false)
const dragStart = ref({ x: 0, y: 0, offsetX: 0, offsetY: 0 })
const processing = ref(false)

const baseScale = computed(() => {
  const { width, height } = naturalSize.value
  if (!width || !height) return 1
  return Math.max(VIEWPORT_SIZE / width, VIEWPORT_SIZE / height)
})

const displaySize = computed(() => {
  const scale = baseScale.value * zoom.value
  return {
    width: naturalSize.value.width * scale,
    height: naturalSize.value.height * scale,
  }
})

const imageStyle = computed(() => ({
  width: `${displaySize.value.width}px`,
  height: `${displaySize.value.height}px`,
  transform: `translate(calc(-50% + ${offset.value.x}px), calc(-50% + ${offset.value.y}px))`,
}))

function clampOffset(nextX: number, nextY: number) {
  const { width, height } = displaySize.value
  const half = VIEWPORT_SIZE / 2
  const minX = width >= VIEWPORT_SIZE ? half - width / 2 : 0
  const maxX = width >= VIEWPORT_SIZE ? width / 2 - half : 0
  const minY = height >= VIEWPORT_SIZE ? half - height / 2 : 0
  const maxY = height >= VIEWPORT_SIZE ? height / 2 - half : 0
  return {
    x: Math.min(maxX, Math.max(minX, nextX)),
    y: Math.min(maxY, Math.max(minY, nextY)),
  }
}

function resetTransform() {
  offset.value = { x: 0, y: 0 }
  zoom.value = 1
}

function revokeImageUrl() {
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
    imageUrl.value = ''
  }
}

function loadFile(file: File | null) {
  revokeImageUrl()
  naturalSize.value = { width: 0, height: 0 }
  resetTransform()
  if (!file) return
  imageUrl.value = URL.createObjectURL(file)
}

function handleImageLoad(event: Event) {
  const img = event.target as HTMLImageElement
  imageEl.value = img
  naturalSize.value = { width: img.naturalWidth, height: img.naturalHeight }
  resetTransform()
}

function startDrag(event: PointerEvent) {
  dragging.value = true
  dragStart.value = {
    x: event.clientX,
    y: event.clientY,
    offsetX: offset.value.x,
    offsetY: offset.value.y,
  }
  ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
}

function onDrag(event: PointerEvent) {
  if (!dragging.value) return
  const dx = event.clientX - dragStart.value.x
  const dy = event.clientY - dragStart.value.y
  const next = clampOffset(dragStart.value.offsetX + dx, dragStart.value.offsetY + dy)
  offset.value = next
}

function endDrag(event: PointerEvent) {
  dragging.value = false
  ;(event.currentTarget as HTMLElement).releasePointerCapture(event.pointerId)
}

function handleWheel(event: WheelEvent) {
  event.preventDefault()
  const delta = event.deltaY > 0 ? -0.08 : 0.08
  const nextZoom = Math.min(3, Math.max(1, Number((zoom.value + delta).toFixed(2))))
  zoom.value = nextZoom
  offset.value = clampOffset(offset.value.x, offset.value.y)
}

async function cropToFile(): Promise<File> {
  const img = imageEl.value
  if (!img || !props.file) {
    throw new Error('图片尚未加载完成')
  }
  const scale = baseScale.value * zoom.value
  const center = VIEWPORT_SIZE / 2
  const imgLeft = center + offset.value.x - displaySize.value.width / 2
  const imgTop = center + offset.value.y - displaySize.value.height / 2
  const srcX = -imgLeft / scale
  const srcY = -imgTop / scale
  const srcSize = VIEWPORT_SIZE / scale

  const canvas = document.createElement('canvas')
  canvas.width = OUTPUT_SIZE
  canvas.height = OUTPUT_SIZE
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('无法创建裁剪画布')

  ctx.drawImage(img, srcX, srcY, srcSize, srcSize, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE)

  const blob = await new Promise<Blob | null>((resolve) => {
    canvas.toBlob(resolve, 'image/jpeg', 0.92)
  })
  if (!blob) throw new Error('头像裁剪失败')

  const baseName = props.file.name.replace(/\.[^.]+$/, '') || 'avatar'
  return new File([blob], `${baseName}.jpg`, { type: 'image/jpeg', lastModified: Date.now() })
}

async function handleConfirm() {
  processing.value = true
  try {
    const cropped = await cropToFile()
    emit('confirm', cropped)
    emit('update:open', false)
  } finally {
    processing.value = false
  }
}

function handleCancel() {
  emit('update:open', false)
}

watch(
  () => props.file,
  (file) => loadFile(file),
  { immediate: true },
)

watch(
  () => props.open,
  (open) => {
    if (!open) {
      revokeImageUrl()
      naturalSize.value = { width: 0, height: 0 }
      resetTransform()
    } else if (props.file) {
      loadFile(props.file)
    }
  },
)

onBeforeUnmount(revokeImageUrl)
</script>

<template>
  <Dialog
    :open="open"
    title="裁剪头像"
    class="max-w-md"
    @update:open="$emit('update:open', $event)"
  >
    <p class="mb-4 text-sm text-muted-foreground">
      拖动图片调整位置，滚轮或滑块缩放，头像将裁剪为圆形。
    </p>

    <div
      class="relative mx-auto overflow-hidden rounded-full border bg-muted"
      :style="{ width: `${VIEWPORT_SIZE}px`, height: `${VIEWPORT_SIZE}px` }"
      @pointerdown="startDrag"
      @pointermove="onDrag"
      @pointerup="endDrag"
      @pointercancel="endDrag"
      @wheel="handleWheel"
    >
      <div class="absolute inset-0">
        <img
          v-if="imageUrl"
          :src="imageUrl"
          alt="待裁剪头像"
          draggable="false"
          class="absolute left-1/2 top-1/2 max-w-none select-none touch-none"
          :style="imageStyle"
          @load="handleImageLoad"
        />
      </div>
      <div class="pointer-events-none absolute inset-0 rounded-full ring-1 ring-border/60" />
    </div>

    <div class="mt-4 space-y-2">
      <label class="text-xs text-muted-foreground">缩放</label>
      <input
        v-model.number="zoom"
        type="range"
        min="1"
        max="3"
        step="0.01"
        class="w-full accent-primary"
        @input="offset = clampOffset(offset.x, offset.y)"
      />
    </div>

    <div class="mt-6 flex justify-end gap-2 border-t pt-4">
      <Button variant="outline" type="button" :disabled="processing" @click="handleCancel">
        取消
      </Button>
      <Button type="button" :disabled="processing || !imageUrl" @click="handleConfirm">
        {{ processing ? '处理中...' : '确认' }}
      </Button>
    </div>
  </Dialog>
</template>
