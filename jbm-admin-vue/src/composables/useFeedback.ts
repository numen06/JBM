import { reactive } from 'vue'

export type FeedbackVariant = 'default' | 'destructive'
export type FeedbackKind = 'alert' | 'confirm' | 'prompt'
export type ToastVariant = 'success' | 'info' | 'warning' | 'error'

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

export interface ToastOptions {
  title?: string
  message: string
  variant?: ToastVariant
  duration?: number
}

export interface ToastRequest extends Required<Omit<ToastOptions, 'duration'>> {
  id: number
  duration: number
}

const state = reactive<{
  active: FeedbackRequest | null
  queue: FeedbackRequest[]
  toasts: ToastRequest[]
}>({
  active: null,
  queue: [],
  toasts: [],
})

let nextId = 1
let nextToastId = 1

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

function dismissToast(id: number) {
  const index = state.toasts.findIndex((item) => item.id === id)
  if (index >= 0) state.toasts.splice(index, 1)
}

function toast(options: string | ToastOptions) {
  const payload = typeof options === 'string' ? { message: options } : options
  const variant = payload.variant ?? 'info'
  const item: ToastRequest = {
    id: nextToastId++,
    title:
      payload.title ??
      {
        success: '操作成功',
        info: '提示',
        warning: '请注意',
        error: '操作失败',
      }[variant],
    message: payload.message,
    variant,
    duration: payload.duration ?? 3600,
  }
  state.toasts.push(item)
  if (item.duration > 0) {
    window.setTimeout(() => dismissToast(item.id), item.duration)
  }
  return item.id
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
    toast: Object.assign(toast, {
      success(message: string, title?: string) {
        return toast({ title, message, variant: 'success' })
      },
      info(message: string, title?: string) {
        return toast({ title, message, variant: 'info' })
      },
      warning(message: string, title?: string) {
        return toast({ title, message, variant: 'warning' })
      },
      error(message: string, title?: string) {
        return toast({ title, message, variant: 'error' })
      },
    }),
    dismissToast,
    finish,
  }
}
