import { defineComponent, h, nextTick, onBeforeUnmount, watch, type Component, type PropType, type Ref } from 'vue'

const dialogs: symbol[] = []
let bodyOverflow = ''

/** Keep focus and scroll inside the topmost shared dialog; preserve nested dialogs. */
export function useJbmDialog(open: () => boolean | undefined, panel: Ref<HTMLElement | null>, close: () => void) {
  const id = Symbol('dialog')
  let returnFocus: HTMLElement | null = null
  let generation = 0
  const focusable = () => Array.from(panel.value?.querySelectorAll<HTMLElement>('a[href],button:not(:disabled),input:not(:disabled):not([type="hidden"]),select:not(:disabled),textarea:not(:disabled),[tabindex]:not([tabindex="-1"])') ?? [])
    .filter(element => element.tabIndex >= 0 && element.getClientRects().length > 0 && !element.closest('[inert]'))
  function keydown(event: KeyboardEvent) {
    if (dialogs.at(-1) !== id || event.defaultPrevented) return
    if (event.key === 'Escape') { event.preventDefault(); close(); return }
    if (event.key !== 'Tab') return
    const items = focusable()
    const first = items[0]
    const last = items.at(-1)
    const outside = !panel.value?.contains(document.activeElement)
    if (!first) { event.preventDefault(); panel.value?.focus() }
    else if (event.shiftKey && (document.activeElement === first || outside)) { event.preventDefault(); last?.focus() }
    else if (!event.shiftKey && (document.activeElement === last || outside)) { event.preventDefault(); first.focus() }
  }
  function cleanup() {
    generation += 1
    const index = dialogs.indexOf(id)
    if (index < 0) return
    const topmost = dialogs.at(-1) === id
    dialogs.splice(index, 1)
    document.removeEventListener('keydown', keydown)
    if (!dialogs.length) document.body.style.overflow = bodyOverflow
    if (topmost && returnFocus?.isConnected) returnFocus.focus()
  }
  watch(open, async value => {
    if (!value) { cleanup(); return }
    const current = ++generation
    await nextTick()
    if (current !== generation || !open() || !panel.value || dialogs.includes(id)) return
    returnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    if (!dialogs.length) { bodyOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden' }
    dialogs.push(id)
    document.addEventListener('keydown', keydown)
    ;(focusable()[0] ?? panel.value).focus()
  }, { immediate: true, flush: 'post' })
  onBeforeUnmount(cleanup)
}

/** Animate the arrival of an exact value, never an invented intermediate reading. */
export const JbmValue = defineComponent({
  name: 'JbmValue',
  props: { value: { type: [String, Number] as PropType<string | number>, required: true } },
  setup(props) {
    return () => h('span', { class: 'jbm-value' }, [
      h('span', { key: String(props.value), class: 'jbm-value__reading' }, String(props.value)),
    ])
  },
})

/** Shared anatomy, independent of host Tailwind versions and utility order. */
export const JbmMetricCard = defineComponent({
  name: 'JbmMetricCard',
  props: {
    label: { type: String, required: true },
    value: { type: [String, Number] as PropType<string | number>, required: true },
    unit: { type: String, default: '' },
    hint: { type: String, default: '' },
    icon: { type: [Object, Function] as PropType<Component>, default: undefined },
    tone: { type: String as PropType<'default' | 'success' | 'warning' | 'danger' | 'info'>, default: 'default' },
    emphasis: { type: String as PropType<'normal' | 'primary'>, default: 'normal' },
    loading: { type: Boolean, default: false },
  },
  setup(props, { slots }) {
    return () => h('article', { class: 'jbm-panel jbm-metric', 'data-emphasis': props.emphasis, 'data-tone': props.tone, 'aria-busy': props.loading }, [
      h('div', { class: 'jbm-metric__header' }, [
        h('div', { class: 'jbm-metric__label', title: props.label }, props.label),
        props.icon ? h('div', { class: 'jbm-metric__icon', 'aria-hidden': 'true' }, [h(props.icon)]) : null,
      ]),
      props.loading ? h('div', { class: 'jbm-skeleton jbm-metric__placeholder', 'aria-label': '加载中' })
        : h('div', { class: 'jbm-metric__value' }, [h(JbmValue, { value: props.value }), props.unit && props.value !== '—' ? h('small', { class: 'jbm-metric__unit' }, props.unit) : null, slots.status?.()]),
      props.hint ? h('div', { class: 'jbm-metric__hint', title: props.hint }, props.hint) : null,
    ])
  },
})

export const JbmPageHeading = defineComponent({
  name: 'JbmPageHeading',
  props: { title: { type: String, required: true }, description: { type: String, default: '' } },
  setup(props, { slots }) {
    return () => h('header', { class: 'jbm-page-heading' }, [
      h('div', { class: 'jbm-page-heading__copy' }, [h('h1', {}, props.title), props.description ? h('p', {}, props.description) : null]),
      slots.default ? h('div', { class: 'jbm-page-heading__actions' }, slots.default()) : null,
    ])
  },
})
