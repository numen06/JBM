<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, onUpdated, ref } from 'vue'
import { cn } from '@/lib/utils'

const tableRef = ref<HTMLTableElement | null>(null)
let observer: MutationObserver | undefined

function syncMobileLabels() {
  const table = tableRef.value
  if (!table) return
  const labels = [...table.querySelectorAll('thead th')].map((cell) => cell.textContent?.trim() ?? '')
  for (const row of table.querySelectorAll('tbody tr')) {
    ;[...row.querySelectorAll(':scope > td')].forEach((cell, index) => {
      cell.setAttribute('data-label', labels[index] ?? '')
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
    overflow: hidden;
    border: 1px solid hsl(var(--border));
    border-radius: 0.75rem;
    background: hsl(var(--card));
  }

  .responsive-table :deep(tbody > tr > td) {
    display: grid;
    grid-template-columns: minmax(6.5rem, 38%) minmax(0, 1fr);
    gap: 0.75rem;
    align-items: start;
    max-width: none !important;
    min-height: 2.75rem;
    padding: 0.75rem 1rem !important;
    border-bottom: 1px solid hsl(var(--border));
    text-align: left !important;
    white-space: normal;
    overflow-wrap: anywhere;
  }

  .responsive-table :deep(tbody > tr > td:last-child) {
    border-bottom: 0;
  }

  .responsive-table :deep(tbody > tr > td::before) {
    content: attr(data-label);
    color: hsl(var(--muted-foreground));
    font-size: 0.75rem;
    font-weight: 500;
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
