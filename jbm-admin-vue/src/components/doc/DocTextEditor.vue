<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { EditorState } from '@codemirror/state'
import { EditorView, keymap, lineNumbers } from '@codemirror/view'
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands'
import { syntaxHighlighting, defaultHighlightStyle, bracketMatching } from '@codemirror/language'
import { javascript } from '@codemirror/lang-javascript'
import { json } from '@codemirror/lang-json'
import { markdown } from '@codemirror/lang-markdown'
import { python } from '@codemirror/lang-python'
import { sql } from '@codemirror/lang-sql'
import { xml } from '@codemirror/lang-xml'
import { java } from '@codemirror/lang-java'
import type { DocEditorLanguage } from '@/utils/docContent'

const props = withDefaults(
  defineProps<{
    modelValue: string
    readonly?: boolean
    language?: DocEditorLanguage
    loading?: boolean
  }>(),
  {
    readonly: false,
    language: 'plain',
    loading: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  ready: []
}>()

const host = ref<HTMLDivElement | null>(null)
let view: EditorView | null = null
let suppressUpdate = false

function languageExtension(language: DocEditorLanguage) {
  switch (language) {
    case 'javascript':
      return javascript({ jsx: true, typescript: true })
    case 'json':
      return json()
    case 'markdown':
      return markdown()
    case 'python':
      return python()
    case 'sql':
      return sql()
    case 'xml':
      return xml()
    case 'java':
      return java()
    default:
      return []
  }
}

function createEditorState(doc: string, readonly: boolean) {
  return EditorState.create({
    doc,
    extensions: [
      lineNumbers(),
      history(),
      bracketMatching(),
      syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
      languageExtension(props.language),
      keymap.of([...defaultKeymap, ...historyKeymap]),
      EditorView.lineWrapping,
      EditorView.updateListener.of((update) => {
        if (!update.docChanged || suppressUpdate) return
        emit('update:modelValue', update.state.doc.toString())
      }),
      EditorView.editable.of(!readonly),
      EditorState.readOnly.of(readonly),
      EditorView.theme({
        '&': {
          height: '100%',
          fontSize: '13px',
        },
        '.cm-scroller': {
          fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
          lineHeight: '1.5',
        },
        '&.cm-focused': {
          outline: 'none',
        },
      }),
    ],
  })
}

function mountEditor() {
  if (!host.value || props.loading) return
  destroyEditor()
  view = new EditorView({
    state: createEditorState(props.modelValue, props.readonly),
    parent: host.value,
  })
  emit('ready')
}

function destroyEditor() {
  view?.destroy()
  view = null
}

function syncDoc(value: string) {
  if (!view) return
  const current = view.state.doc.toString()
  if (current === value) return
  suppressUpdate = true
  view.dispatch({
    changes: { from: 0, to: view.state.doc.length, insert: value },
  })
  suppressUpdate = false
}

watch(
  () => props.modelValue,
  (value) => syncDoc(value),
)

watch(
  () => [props.language, props.loading, props.readonly] as const,
  () => mountEditor(),
)

onMounted(() => mountEditor())
onBeforeUnmount(() => destroyEditor())

function focus() {
  view?.focus()
}

defineExpose({ focus })
</script>

<template>
  <div class="relative h-[68vh] overflow-hidden rounded-md border bg-background">
    <div
      v-if="loading"
      class="absolute inset-0 z-10 flex items-center justify-center bg-background/80 text-sm text-muted-foreground"
    >
      正在加载内容...
    </div>
    <div ref="host" class="h-full" />
  </div>
</template>
