import { reactive } from 'vue'

export type FeedbackVariant = 'default' | 'destructive'
export type FeedbackKind = 'alert' | 'confirm' | 'prompt'

export interface FeedbackOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  variant?: FeedbackVariant
}

export interface PromptOptions extends FeedbackOptions {
  defaultValue?: string
  placeholder?: string
}

interface FeedbackRequest extends PromptOptions {
  id: number
  kind: FeedbackKind
  resolve: (value: boolean | string | null) => void
}

const state = reactive<{
  active: FeedbackRequest | null
  queue: FeedbackRequest[]
}>({
  active: null,
  queue: [],
})

let nextId = 1

function enqueue(request: FeedbackRequest) {
  if (state.active) state.queue.push(request)
  else state.active = request
}

function finish(value: boolean | string | null) {
  const current = state.active
  if (!current) return
  current.resolve(value)
  state.active = state.queue.shift() ?? null
}

function open(kind: FeedbackKind, options: FeedbackOptions | PromptOptions) {
  return new Promise<boolean | string | null>((resolve) => {
    enqueue({
      id: nextId++,
      kind,
      title: options.title,
      message: options.message,
      confirmText: options.confirmText,
      cancelText: options.cancelText,
      variant: options.variant,
      defaultValue: 'defaultValue' in options ? options.defaultValue : undefined,
      placeholder: 'placeholder' in options ? options.placeholder : undefined,
      resolve,
    })
  })
}

export function useFeedback() {
  return {
    state,
    alert(options: string | FeedbackOptions) {
      const payload = typeof options === 'string' ? { message: options } : options
      return open('alert', {
        title: payload.title ?? '提示',
        confirmText: payload.confirmText ?? '知道了',
        variant: payload.variant ?? 'default',
        message: payload.message,
      }).then(() => undefined)
    },
    confirm(options: string | FeedbackOptions) {
      const payload = typeof options === 'string' ? { message: options } : options
      return open('confirm', {
        title: payload.title ?? '确认操作',
        confirmText: payload.confirmText ?? '确认',
        cancelText: payload.cancelText ?? '取消',
        variant: payload.variant ?? 'default',
        message: payload.message,
      }).then(Boolean)
    },
    prompt(options: PromptOptions) {
      return open('prompt', {
        title: options.title ?? '请输入',
        confirmText: options.confirmText ?? '确认',
        cancelText: options.cancelText ?? '取消',
        variant: options.variant ?? 'default',
        message: options.message,
        defaultValue: options.defaultValue ?? '',
        placeholder: options.placeholder,
      }).then((value) => (typeof value === 'string' ? value : null))
    },
    finish,
  }
}
