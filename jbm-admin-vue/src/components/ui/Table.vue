<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, onUpdated, ref } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  mobileTitle?: string
  mobileColumns?: string[]
}>()
const tableRef = ref<HTMLTableElement | null>(null)
let observer: MutationObserver | undefined

function syncMobileLabels() {
  const table = tableRef.value
  if (!table) return
  const labels = [...table.querySelectorAll(':scope > thead > tr > th')].map((cell) => cell.textContent?.trim() ?? '')
  const preferredTitles = ['名称', '用户名', '标题', '账号', '应用', '组织', '角色', '服务名', '编码']
  const mobileTitle = props.mobileTitle || labels.find((label) =>
    preferredTitles.some((candidate) => label === candidate || label.endsWith(candidate)),
  ) || labels.find((label) => label && label !== 'ID' && label !== '状态' && label !== '操作') || ''
  for (const row of table.querySelectorAll(':scope > tbody > tr')) {
    const cells = [...row.querySelectorAll(':scope > td')]
    const summaryIndexes = cells
      .map((_, index) => index)
      .filter((index) => {
        const label = labels[index] ?? ''
        return label && label !== mobileTitle && label !== 'ID' && label !== '状态' && label !== '操作'
      })
      .slice(0, 4)
    cells.forEach((cell, index) => {
      const label = labels[index] ?? ''
      const isTitle = Boolean(mobileTitle && label === mobileTitle)
      const isStatus = label === '状态' || label.endsWith('状态')
      const alwaysVisible = !label || cell.hasAttribute('colspan') || isTitle || isStatus || label === '操作'
      const visibleByDefault = summaryIndexes.includes(index)
      cell.setAttribute('data-label', label)
      cell.toggleAttribute('data-mobile-title', isTitle)
      cell.toggleAttribute('data-mobile-status', isStatus)
      cell.toggleAttribute(
        'data-mobile-hidden',
        Boolean(!alwaysVisible && (props.mobileColumns ? !props.mobileColumns.includes(label) : !visibleByDefault)),
      )
    })
  }
}

onMounted(() => {
  syncMobileLabels()
  observer = new MutationObserver(syncMobileLabels)
  if (tableRef.value) observer.observe(tableRef.value, { childList: true, subtree: true })
})

onUpdated(() => nextTick(syncMobileLabels))
onBeforeUnmount(() => observer?.disconnect())
</script>

<template>
  <div class="responsive-table-scroll relative w-full overflow-auto">
    <table ref="tableRef" :class="cn('responsive-table w-full caption-bottom text-sm', $attrs.class as string)">
      <slot />
    </table>
  </div>
</template>

<style scoped>
@media (max-width: 767px) {
  .responsive-table-scroll {
    overflow: visible;
  }

  .responsive-table,
  .responsive-table :deep(tbody) {
    display: block;
    width: 100%;
  }

  .responsive-table :deep(thead) {
    display: none;
  }

  .responsive-table :deep(tbody) {
    display: grid;
    gap: 0.75rem;
  }

  .responsive-table :deep(tbody > tr) {
    display: block;
    position: relative;
    overflow: hidden;
    padding: 1rem;
    border: 1px solid var(--color-border);
    border-radius: 0.75rem;
    background:
      linear-gradient(135deg, color-mix(in srgb, var(--color-primary) 4%, transparent), transparent 45%),
      var(--color-card);
    box-shadow: 0 2px 10px color-mix(in srgb, var(--color-primary) 9%, transparent);
  }

  .responsive-table :deep(tbody > tr > td) {
    display: grid;
    grid-template-columns: 5.5rem minmax(0, 1fr);
    gap: 0.75rem;
    align-items: start;
    max-width: none !important;
    min-height: 0;
    padding: 0.375rem 0 !important;
    border: 0;
    text-align: left !important;
    white-space: normal;
    overflow-wrap: anywhere;
  }

  .responsive-table :deep(tbody > tr > td::before) {
    content: attr(data-label);
    color: var(--color-muted-foreground);
    font-size: 0.75rem;
    font-weight: 500;
  }

  .responsive-table :deep(tbody > tr > td[data-mobile-title]) {
    grid-template-columns: minmax(0, 1fr);
    gap: 0.125rem;
    margin-bottom: 0.625rem;
    padding-right: 5.5rem !important;
    padding-bottom: 0.875rem !important;
    border-bottom: 1px solid var(--color-border);
    color: var(--color-foreground);
    font-size: 1rem;
    font-weight: 650;
    line-height: 1.35;
  }

  .responsive-table :deep(tbody > tr > td[data-mobile-title]::before) {
    font-size: 0.6875rem;
    font-weight: 500;
  }

  .responsive-table :deep(tbody > tr > td[data-mobile-status]) {
    display: block;
    position: absolute;
    top: 1rem;
    right: 1rem;
    max-width: 5rem !important;
    padding: 0 !important;
    text-align: right !important;
  }

  .responsive-table :deep(tbody > tr > td[data-mobile-status]::before) {
    content: none;
  }

  .responsive-table :deep(tbody > tr > td[data-mobile-hidden]) {
    display: none;
  }

  .responsive-table :deep(tbody > tr > td[data-label='操作']) {
    display: block;
    margin-top: 0.625rem;
    padding-top: 0.875rem !important;
    border-top: 1px solid var(--color-border);
  }

  .responsive-table :deep(tbody > tr > td[data-label='操作']::before) {
    content: none;
  }

  .responsive-table :deep(tbody > tr > td[data-label='操作'] > *) {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .responsive-table :deep(tbody > tr > td[data-label='']),
  .responsive-table :deep(tbody > tr > td[colspan]) {
    display: block;
  }

  .responsive-table :deep(tbody > tr > td[data-label='']::before),
  .responsive-table :deep(tbody > tr > td[colspan]::before) {
    content: none;
  }
}
</style>
