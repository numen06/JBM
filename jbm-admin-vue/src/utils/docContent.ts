import type { BaseDoc } from '@/api/doc'

export type DocEditorLanguage =
  | 'javascript'
  | 'json'
  | 'markdown'
  | 'python'
  | 'sql'
  | 'xml'
  | 'java'
  | 'plain'

const TEXT_EDITABLE_TYPES = new Set([
  'application/json',
  'application/xml',
  'application/x-ndjson',
  'application/yaml',
  'text/yaml',
])

const TEXT_EDITABLE_EXTENSION = /\.(bat|conf|csv|css|env|html?|ini|java|js|json|log|md|properties|py|sql|text|toml|ts|txt|vue|xml|ya?ml)$/i

const OFFICE_EXTENSION = /\.(docx?|xlsx?|pptx?|ppsx?|potx?)$/i

const IMAGE_EXTENSION = /\.(avif|bmp|gif|heic|heif|ico|jpe?g|png|svg|tiff?|webp)$/i

export function isTextEditable(doc: Pick<BaseDoc, 'contentType' | 'docPath' | 'docName'>) {
  const type = (doc.contentType || '').split(';')[0].trim().toLowerCase()
  const path = (doc.docPath || doc.docName || '').toLowerCase()
  return (
    type.startsWith('text/') ||
    TEXT_EDITABLE_TYPES.has(type) ||
    TEXT_EDITABLE_EXTENSION.test(path)
  )
}

export function isImageDoc(doc: Pick<BaseDoc, 'contentType' | 'docPath' | 'docName'>) {
  const type = (doc.contentType || '').split(';')[0].trim().toLowerCase()
  const path = (doc.docPath || doc.docName || '').toLowerCase()
  return type.startsWith('image/') || IMAGE_EXTENSION.test(path)
}

export function isOfficeDoc(doc: Pick<BaseDoc, 'contentType' | 'docPath' | 'docName'>) {
  const type = (doc.contentType || '').toLowerCase()
  const path = (doc.docPath || doc.docName || '').toLowerCase()
  return (
    /word|document|officedocument|excel|spreadsheet|powerpoint|presentation/.test(type) ||
    OFFICE_EXTENSION.test(path)
  )
}

export function canPreviewEdit(doc: Pick<BaseDoc, 'contentType' | 'docPath' | 'docName'>) {
  return isTextEditable(doc) || isOfficeDoc(doc)
}

export function guessDocLanguage(docPath?: string): DocEditorLanguage {
  const path = (docPath || '').toLowerCase()
  if (/\.(js|mjs|cjs|ts|tsx|jsx|vue)$/.test(path)) return 'javascript'
  if (/\.json$/.test(path)) return 'json'
  if (/\.md$/.test(path)) return 'markdown'
  if (/\.py$/.test(path)) return 'python'
  if (/\.sql$/.test(path)) return 'sql'
  if (/\.(xml|html?)$/.test(path)) return 'xml'
  if (/\.java$/.test(path)) return 'java'
  return 'plain'
}

export function contentLabel(doc: Pick<BaseDoc, 'contentType'>) {
  const value = doc.contentType || ''
  if (value.includes('pdf')) return 'PDF'
  if (value.includes('image')) return '图片'
  if (value.includes('text')) return '文本'
  if (/word|document|officedocument/.test(value)) return '文档'
  if (/excel|spreadsheet/.test(value)) return '表格'
  if (/powerpoint|presentation/.test(value)) return '演示'
  return value || '文件'
}
