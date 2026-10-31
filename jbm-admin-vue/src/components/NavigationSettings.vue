<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Dialog from '@/components/ui/Dialog.vue'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'
import { emptyNavigation, workspaceNames, type NavigationCatalogItem, type TenantNavigationContext, type TenantNavigationPreferences, type WorkspaceKey } from '../../packages/admin/src/navigationPreferences'

const props = defineProps<{ open: boolean; embedded?: boolean; catalog: NavigationCatalogItem[]; settings: TenantNavigationContext; saving?: boolean; error?: string }>()
const emit = defineEmits<{ 'update:open': [value: boolean]; save: [value: TenantNavigationPreferences] }>()
const draft = ref(emptyNavigation())
const selected = ref<WorkspaceKey>('building')
const groups = computed(() => [...new Set(props.catalog.filter(item => item.workspace === selected.value).map(item => item.group))])
watch(() => [props.open, props.settings.tenantId, props.settings.revision], () => {
  if (!props.open) return
  draft.value = JSON.parse(JSON.stringify(props.settings.preferences))
  for (const key of Object.keys(workspaceNames)) draft.value.workspaces[key] ||= { label: '', hidden: false, order: 0 }
  for (const item of props.catalog) {
    draft.value.menus[item.path] ||= { label: '', hidden: false, order: 0 }
    draft.value.groups[`${item.workspace}:${item.group}`] ||= { label: '', hidden: false, order: 0 }
  }
  if (!props.catalog.some(item => item.workspace === selected.value)) selected.value = props.catalog[0]?.workspace || 'building'
}, { immediate: true })
function reset() {
  draft.value = emptyNavigation()
  for (const key of Object.keys(workspaceNames)) draft.value.workspaces[key] = { label: '', hidden: false, order: 0 }
  for (const item of props.catalog) {
    draft.value.menus[item.path] = { label: '', hidden: false, order: 0 }
    draft.value.groups[`${item.workspace}:${item.group}`] = { label: '', hidden: false, order: 0 }
  }
}
</script>

<template>
  <component :is="embedded ? 'section' : Dialog" v-if="!embedded || open" :open="open" title="菜单设置" :class="embedded ? 'space-y-4' : 'sm:max-w-3xl'" :aria-label="embedded ? '菜单设置编辑器' : undefined" @update:open="emit('update:open', $event)">
    <p class="mb-4 text-sm text-muted-foreground">设置对当前租户成员生效，可用功能仍以各成员权限为准。顺序越小越靠前，名称留空使用默认名称。</p>
    <div v-if="error" role="alert" class="mb-3 rounded-md border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-400">{{ error }}</div>
    <fieldset :disabled="saving || !settings.canManage" class="space-y-4">
      <div class="grid grid-cols-3 gap-2">
        <button v-for="(label, key) in workspaceNames" :key="key" type="button" class="rounded-md border px-3 py-2 text-sm" :class="selected === key ? 'border-primary bg-primary/10 text-primary' : 'border-border'" @click="selected = key">{{ draft.workspaces[key]?.label || label }}</button>
      </div>
      <div v-if="draft.workspaces[selected]" class="grid grid-cols-[1fr_80px_70px] items-end gap-3 rounded-lg border p-3">
        <label class="space-y-1 text-sm">工作区名称<Input v-model="draft.workspaces[selected]!.label" :placeholder="workspaceNames[selected]" maxlength="12" /></label>
        <label class="space-y-1 text-sm">顺序<Input :model-value="draft.workspaces[selected]!.order" @update:model-value="draft.workspaces[selected]!.order = Number($event)" type="number" min="0" max="9999" /></label>
        <label class="flex h-9 items-center gap-2 text-sm"><input type="checkbox" :checked="!draft.workspaces[selected]!.hidden" @change="draft.workspaces[selected]!.hidden = !($event.target as HTMLInputElement).checked" />显示</label>
      </div>
      <p v-if="!groups.length" class="py-8 text-center text-sm text-muted-foreground">当前账号在此工作区没有可配置的菜单</p>
      <section v-for="group in groups" :key="group" class="overflow-hidden rounded-lg border">
        <div class="grid grid-cols-[1fr_80px_70px] items-center gap-3 bg-muted/30 p-3">
          <label class="text-xs text-muted-foreground">分组名称<Input v-model="draft.groups[`${selected}:${group}`]!.label" :placeholder="group" maxlength="40" /></label>
          <label class="text-xs text-muted-foreground">顺序<Input :model-value="draft.groups[`${selected}:${group}`]!.order" @update:model-value="draft.groups[`${selected}:${group}`]!.order = Number($event)" type="number" min="0" max="9999" /></label>
          <label class="flex items-center gap-2 text-sm"><input type="checkbox" :checked="!draft.groups[`${selected}:${group}`]!.hidden" @change="draft.groups[`${selected}:${group}`]!.hidden = !($event.target as HTMLInputElement).checked" />显示</label>
        </div>
        <div v-for="item in catalog.filter(item => item.workspace === selected && item.group === group)" :key="item.path" class="grid grid-cols-[1fr_80px_70px] items-center gap-3 border-t px-3 py-2">
          <Input v-model="draft.menus[item.path]!.label" :aria-label="`${item.label}名称`" :placeholder="item.label" maxlength="40" />
          <Input :model-value="draft.menus[item.path]!.order" @update:model-value="draft.menus[item.path]!.order = Number($event)" type="number" min="0" max="9999" :aria-label="`${item.label}顺序`" />
          <label class="flex items-center gap-2 text-sm"><input type="checkbox" :aria-label="`显示${item.label}`" :checked="!draft.menus[item.path]!.hidden" @change="draft.menus[item.path]!.hidden = !($event.target as HTMLInputElement).checked" />显示</label>
        </div>
      </section>
    </fieldset>
    <div class="sticky bottom-0 mt-4 flex justify-between gap-3 border-t bg-card pt-4">
      <Button variant="outline" :disabled="saving" @click="reset">恢复默认（保存后生效）</Button>
      <Button :disabled="saving || !settings.canManage" @click="emit('save', draft)">{{ saving ? '保存中…' : '保存' }}</Button>
    </div>
  </component>
</template>
