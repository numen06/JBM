<template>
  <section class="message-toolbar rounded-lg border bg-card shadow-sm" aria-label="消息筛选与操作">
    <form class="message-filters" role="search" aria-label="搜索消息" @submit.prevent="$emit('search')">
      <div class="message-search"><slot name="search" /></div>
      <div class="message-filter-fields"><slot name="filters" /></div>
    </form>
    <div class="message-toolbar-footer">
      <div class="message-status" role="group" aria-label="消息阅读状态"><slot name="status" /></div>
      <div class="message-actions"><slot name="actions" /></div>
    </div>
    <div v-if="$slots.batch" class="message-batch"><slot name="batch" /></div>
  </section>
</template>

<script setup lang="ts">
defineEmits<{ search: [] }>()
</script>

<style scoped>
.message-toolbar{padding:16px;margin-bottom:16px;container-type:inline-size}
.message-filters{display:flex;align-items:center;flex-wrap:wrap;gap:12px}
.message-search{display:flex;align-items:center;gap:8px;flex:1 1 320px;min-width:0}
.message-search :deep(input){flex:1;min-width:0;width:100%;height:36px}
.message-search :deep(button){flex-shrink:0;height:36px}
.message-filter-fields{display:flex;flex-wrap:wrap;gap:12px;min-width:0}
.message-filter-fields :deep(select){width:140px;height:36px}
.message-filter-fields :deep(.message-recipient){width:220px;min-width:0}
.message-toolbar-footer{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:12px;border-top:1px solid var(--color-border);padding-top:12px;margin-top:16px}
.message-status{display:inline-flex;gap:4px;padding:3px;border-radius:6px;background:var(--color-muted)}
.message-actions{display:flex;align-items:center;flex-wrap:wrap;gap:8px;margin-left:auto}
.message-batch{display:flex;align-items:center;flex-wrap:wrap;gap:8px;border-top:1px solid var(--color-border);padding-top:12px;margin-top:12px}
@container(max-width:540px){.message-search,.message-filter-fields{flex-basis:100%}.message-filter-fields :deep(select){flex:1;min-width:0;width:0}.message-filter-fields :deep(.message-recipient){width:100%}.message-filter-fields{gap:8px}}
</style>
