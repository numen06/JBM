<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { AlertTriangle, CheckCircle2, Info, X } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import { useFeedback } from '@/composables/useFeedback'

const feedback = useFeedback()
const inputValue = ref('')
const inputRef = ref<InstanceType<typeof Input> | null>(null)

const active = computed(() => feedback.state.active)
const isDestructive = computed(() => active.value?.variant === 'destructive')
const toasts = computed(() => feedback.state.toasts)

watch(
  active,
  async (value) => {
    inputValue.value = value?.defaultValue ?? ''
    if (value?.kind === 'prompt') {
      await nextTick()
      const el = inputRef.value?.$el as HTMLInputElement | undefined
      el?.focus()
      el?.select()
    }
  },
  { immediate: true },
)

function cancel() {
  feedback.finish(active.value?.kind === 'alert' ? true : null)
}

function confirm() {
  if (active.value?.kind === 'prompt') {
    feedback.finish(inputValue.value)
    return
  }
  feedback.finish(true)
}

function toastClasses(variant: string) {
  if (variant === 'success') return 'border-emerald-200 bg-emerald-50 text-emerald-900'
  if (variant === 'warning') return 'border-amber-200 bg-amber-50 text-amber-950'
  if (variant === 'error') return 'border-destructive/30 bg-destructive/10 text-destructive'
  return 'border-primary/20 bg-primary/10 text-primary'
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed right-4 top-4 z-[110] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-3 sm:right-5 sm:top-5">
      <section
        v-for="toast in toasts"
        :key="toast.id"
        class="rounded-lg border p-4 shadow-lg"
        :class="toastClasses(toast.variant)"
        role="status"
      >
        <div class="flex gap-3">
          <CheckCircle2 v-if="toast.variant === 'success'" class="mt-0.5 h-5 w-5 shrink-0" />
          <AlertTriangle
            v-else-if="toast.variant === 'warning' || toast.variant === 'error'"
            class="mt-0.5 h-5 w-5 shrink-0"
          />
          <Info v-else class="mt-0.5 h-5 w-5 shrink-0" />
          <div class="min-w-0 flex-1">
            <h2 class="text-sm font-semibold leading-5">{{ toast.title }}</h2>
            <p class="mt-1 whitespace-pre-line text-sm leading-5 opacity-85">{{ toast.message }}</p>
          </div>
          <button
            type="button"
            class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-md opacity-70 hover:bg-black/5 hover:opacity-100"
            title="关闭"
            @click="feedback.dismissToast(toast.id)"
          >
            <X class="h-4 w-4" />
          </button>
        </div>
      </section>
    </div>

    <div
      v-if="active"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-black/45 p-4"
      @keydown.esc="cancel"
      @click.self="cancel"
    >
      <section
        role="dialog"
        aria-modal="true"
        class="w-full max-w-md rounded-lg border bg-background shadow-xl"
      >
        <header class="flex items-start gap-3 border-b px-5 py-4">
          <div
            class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full"
            :class="isDestructive ? 'bg-destructive/10 text-destructive' : 'bg-primary/10 text-primary'"
          >
            <AlertTriangle v-if="isDestructive" class="h-5 w-5" />
            <Info v-else class="h-5 w-5" />
          </div>
          <div class="min-w-0 flex-1">
            <h2 class="text-base font-semibold leading-6">{{ active.title }}</h2>
            <p class="mt-1 whitespace-pre-line text-sm leading-6 text-muted-foreground">
              {{ active.message }}
            </p>
          </div>
          <button
            type="button"
            class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground"
            title="关闭"
            @click="cancel"
          >
            <X class="h-4 w-4" />
          </button>
        </header>

        <div v-if="active.kind === 'prompt'" class="px-5 py-4">
          <Input
            ref="inputRef"
            v-model="inputValue"
            :placeholder="active.placeholder"
            @keyup.enter="confirm"
          />
        </div>

        <footer class="flex justify-end gap-2 px-5 py-4">
          <Button
            v-if="active.kind !== 'alert'"
            variant="outline"
            @click="cancel"
          >
            {{ active.cancelText }}
          </Button>
          <Button
            :variant="isDestructive ? 'destructive' : 'default'"
            @click="confirm"
          >
            {{ active.confirmText }}
          </Button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
