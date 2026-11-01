<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Building2, Cable, Check, EyeOff, LayoutList, RotateCcw, Save, Search, Shield } from 'lucide-vue-next'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'
import { emptyNavigation, workspaceNames, type NavigationCatalogItem, type TenantNavigationContext, type TenantNavigationPreferences, type WorkspaceKey } from '../../packages/admin/src/navigationPreferences'

const props = defineProps<{ workspace?: WorkspaceKey; catalog: NavigationCatalogItem[]; settings: TenantNavigationContext; saving?: boolean; error?: string }>()
const emit = defineEmits<{ save: [value: TenantNavigationPreferences]; change: [dirty: boolean] }>()
const draft = ref(emptyNavigation())
const baseline = ref('')
const selected = ref<WorkspaceKey>(props.workspace || 'building')
const selectedGroup = ref('')
const query = ref('')
const icons = { building: Building2, iot: Cable, jbm: Shield }
const workspaceItems = computed(() => props.catalog.filter(item => item.workspace === selected.value))
const groups = computed(() => [...new Set(workspaceItems.value.map(item => item.group))])
const matches = (item: NavigationCatalogItem) => !query.value.trim() || [item.label, draft.value.menus[item.path]?.label, item.group, draft.value.groups[`${selected.value}:${item.group}`]?.label].some(value => value?.toLowerCase().includes(query.value.trim().toLowerCase()))
const filteredGroups = computed(() => groups.value.filter(group => workspaceItems.value.some(item => item.group === group && matches(item))))
const activeGroup = computed(() => filteredGroups.value.includes(selectedGroup.value) ? selectedGroup.value : filteredGroups.value[0] || '')
const groupKey = computed(() => `${selected.value}:${activeGroup.value}`)
const items = computed(() => workspaceItems.value.filter(item => item.group === activeGroup.value && matches(item)))
const dirty = computed(() => JSON.stringify(draft.value) !== baseline.value)
const hiddenParent = computed(() => draft.value.workspaces[selected.value]?.hidden || draft.value.groups[groupKey.value]?.hidden)
const count = (workspace: WorkspaceKey) => props.catalog.filter(item => item.workspace === workspace).length
const groupCount = (group: string) => workspaceItems.value.filter(item => item.group === group).length
const visibleCount = computed(() => workspaceItems.value.filter(item => !draft.value.workspaces[selected.value]?.hidden && !draft.value.groups[`${selected.value}:${item.group}`]?.hidden && !draft.value.menus[item.path]?.hidden).length)
function fillDefaults() {
  for (const key of Object.keys(workspaceNames)) draft.value.workspaces[key] ||= { label: '', hidden: false, order: 0 }
  for (const item of props.catalog) {
    draft.value.menus[item.path] ||= { label: '', hidden: false, order: 0 }
    draft.value.groups[`${item.workspace}:${item.group}`] ||= { label: '', hidden: false, order: 0 }
  }
}
function restoreSaved() {
  draft.value = JSON.parse(JSON.stringify(props.settings.preferences))
  fillDefaults()
  baseline.value = JSON.stringify(draft.value)
  if (!props.catalog.some(item => item.workspace === selected.value)) selected.value = props.catalog[0]?.workspace || 'building'
}
watch(() => [props.settings.tenantId, props.settings.revision], restoreSaved, { immediate: true })
watch(() => props.catalog, () => { const changed = dirty.value; fillDefaults(); if (!changed) baseline.value = JSON.stringify(draft.value) })
watch(selected, () => { query.value = ''; selectedGroup.value = '' })
watch(dirty, value => emit('change', value))
function reset() { draft.value = emptyNavigation(); fillDefaults() }
function save() { if (!props.saving && props.settings.canManage && dirty.value) emit('save', JSON.parse(JSON.stringify(draft.value))) }
</script>

<template>
  <section class="navigation-settings" aria-label="菜单设置编辑器">
    <p v-if="error" role="alert" class="navigation-error">{{ error }}</p>
    <fieldset class="navigation-layout" :disabled="saving || !settings.canManage">
      <aside class="navigation-rail" aria-label="选择工作区和菜单分组">
        <p class="navigation-eyebrow">工作区</p>
        <div class="workspace-options" role="group" aria-label="工作区">
          <button v-for="(label, key) in workspaceNames" :key="key" type="button" :aria-pressed="selected === key" class="workspace-option" :class="{ active: selected === key }" @click="selected = key">
            <component :is="icons[key]" class="navigation-icon" /><span class="workspace-option-copy"><strong>{{ draft.workspaces[key]?.label || label }}</strong><small>{{ count(key) }} 个菜单</small></span><EyeOff v-if="draft.workspaces[key]?.hidden" class="navigation-small-icon" aria-label="工作区已隐藏" />
          </button>
        </div>
        <div class="group-navigation">
          <p class="navigation-eyebrow">菜单分组 <span>{{ groups.length }}</span></p>
          <nav aria-label="菜单分组"><button v-for="group in filteredGroups" :key="group" type="button" :aria-current="activeGroup === group ? 'true' : undefined" class="group-option" :class="{ active: activeGroup === group }" @click="selectedGroup = group"><span>{{ draft.groups[`${selected}:${group}`]?.label || group }}</span><small>{{ groupCount(group) }}</small></button></nav>
        </div>
      </aside>

      <div class="navigation-content">
        <section class="navigation-card workspace-settings" aria-label="工作区设置">
          <div class="navigation-card-heading"><span class="navigation-icon-tile"><component :is="icons[selected]" class="navigation-icon" /></span><div><h3>{{ workspaceNames[selected] }}工作区</h3><p>{{ visibleCount }} / {{ workspaceItems.length }} 个菜单显示中</p></div></div>
          <div v-if="draft.workspaces[selected]" class="navigation-fields">
            <label class="navigation-name-field"><span>显示名称</span><Input v-model="draft.workspaces[selected]!.label" :placeholder="workspaceNames[selected]" :aria-label="`${workspaceNames[selected]}工作区名称`" maxlength="12" /></label>
            <label><span>顺序</span><Input :model-value="draft.workspaces[selected]!.order" @update:model-value="draft.workspaces[selected]!.order = Number($event)" type="number" min="0" max="9999" :aria-label="`${workspaceNames[selected]}工作区顺序`" /></label>
            <label class="navigation-toggle"><input type="checkbox" role="switch" :aria-label="`显示${workspaceNames[selected]}工作区`" :checked="!draft.workspaces[selected]!.hidden" @change="draft.workspaces[selected]!.hidden = !($event.target as HTMLInputElement).checked" /><span>显示工作区</span></label>
          </div>
        </section>

        <section class="navigation-card menu-settings" aria-label="菜单分组设置">
          <div class="menu-toolbar"><div><h3>菜单配置</h3><p>名称留空使用默认值，顺序越小越靠前。</p></div><label class="navigation-search"><Search class="navigation-small-icon" /><input v-model="query" type="search" aria-label="搜索菜单" placeholder="搜索菜单或分组" /></label></div>
          <label class="mobile-group-select"><span>当前分组</span><select :value="activeGroup" aria-label="选择菜单分组" @change="selectedGroup = ($event.target as HTMLSelectElement).value"><option v-for="group in filteredGroups" :key="group" :value="group">{{ draft.groups[`${selected}:${group}`]?.label || group }}（{{ groupCount(group) }}）</option></select></label>
          <template v-if="activeGroup && draft.groups[groupKey]">
            <div class="group-settings">
              <div class="group-heading"><LayoutList class="navigation-small-icon" /><strong>{{ activeGroup }}</strong><span>{{ groupCount(activeGroup) }} 个菜单</span></div>
              <div class="navigation-fields">
                <label><span>分组名称</span><Input v-model="draft.groups[groupKey]!.label" :placeholder="activeGroup" aria-label="分组名称" maxlength="40" /></label>
                <label><span>顺序</span><Input :model-value="draft.groups[groupKey]!.order" @update:model-value="draft.groups[groupKey]!.order = Number($event)" type="number" min="0" max="9999" aria-label="分组顺序" /></label>
                <label class="navigation-toggle"><input type="checkbox" role="switch" aria-label="显示当前分组" :checked="!draft.groups[groupKey]!.hidden" @change="draft.groups[groupKey]!.hidden = !($event.target as HTMLInputElement).checked" /><span>显示分组</span></label>
              </div>
            </div>
            <p v-if="hiddenParent" class="navigation-parent-notice"><EyeOff class="navigation-small-icon" />{{ draft.workspaces[selected]?.hidden ? '工作区已隐藏' : '当前分组已隐藏' }}，以下菜单将随之隐藏，仍可继续编辑。</p>
            <div class="menu-columns" aria-hidden="true"><span>菜单</span><span>显示名称</span><span>顺序</span><span>显示</span></div>
            <div class="menu-rows">
              <div v-for="item in items" :key="item.path" class="menu-row" :class="{ 'menu-row--hidden': hiddenParent || draft.menus[item.path]?.hidden }">
                <div class="menu-original"><span class="menu-dot" /><strong>{{ item.label }}</strong></div>
                <label class="menu-label-field"><span class="mobile-field-label">显示名称</span><Input v-model="draft.menus[item.path]!.label" :aria-label="`${item.label}名称`" :placeholder="item.label" maxlength="40" /></label>
                <label class="menu-order-field"><span class="mobile-field-label">顺序</span><Input :model-value="draft.menus[item.path]!.order" @update:model-value="draft.menus[item.path]!.order = Number($event)" type="number" min="0" max="9999" :aria-label="`${item.label}顺序`" /></label>
                <label class="navigation-toggle menu-visibility"><input type="checkbox" role="switch" :aria-label="`显示${item.label}`" :checked="!draft.menus[item.path]!.hidden" @change="draft.menus[item.path]!.hidden = !($event.target as HTMLInputElement).checked" /><span class="mobile-field-label">显示</span></label>
              </div>
            </div>
          </template>
          <div v-else class="navigation-empty"><Search v-if="query" class="navigation-icon" /><LayoutList v-else class="navigation-icon" /><h4>{{ query ? '没有匹配的菜单' : '暂无可配置菜单' }}</h4><p>{{ query ? '试试其他名称，或清空搜索条件。' : '当前账号在此工作区没有可配置的菜单。' }}</p><button v-if="query" type="button" @click="query = ''">清空搜索</button></div>
        </section>
        <p class="navigation-help"><Shield class="navigation-small-icon" />设置对当前租户成员生效；可用功能仍由成员权限决定。</p>
      </div>
    </fieldset>
    <footer class="navigation-save-bar">
      <div class="navigation-save-state" role="status"><span v-if="dirty" class="unsaved-dot" /><Check v-else class="navigation-small-icon" /><span>{{ saving ? '正在保存…' : dirty ? '有未保存的修改' : '设置已同步' }}</span><small>保存所有工作区的设置</small></div>
      <div class="navigation-actions"><Button variant="ghost" :disabled="saving || !settings.canManage" @click="reset"><RotateCcw class="navigation-small-icon" />恢复默认</Button><Button v-if="dirty" variant="outline" :disabled="saving" @click="restoreSaved">撤销修改</Button><Button :disabled="saving || !settings.canManage || !dirty" @click="save"><Save class="navigation-small-icon" />{{ saving ? '保存中…' : '保存更改' }}</Button></div>
    </footer>
  </section>
</template>

<style scoped>
.navigation-settings{min-width:0;color:hsl(var(--foreground));--nav-line:hsl(var(--border));--nav-muted:hsl(var(--muted-foreground));--nav-surface:hsl(var(--card));--nav-accent:hsl(var(--primary))}
.navigation-layout{display:grid;grid-template-columns:188px minmax(0,1fr);align-items:start;gap:24px;min-width:0;margin:0;padding:0;border:0}
.navigation-rail{min-width:0;padding:16px 12px;border:1px solid var(--nav-line);border-radius:12px;background:var(--nav-surface)}
.navigation-eyebrow{display:flex;justify-content:space-between;padding:0 10px;margin:0 0 10px;font-size:11px;font-weight:600;letter-spacing:.08em;color:var(--nav-muted)}
.navigation-icon{width:19px;height:19px;flex-shrink:0}.navigation-small-icon{width:15px;height:15px;flex-shrink:0}
.workspace-options{display:grid;gap:6px}.workspace-option{display:flex;align-items:center;gap:10px;width:100%;min-width:0;padding:12px 10px;border:1px solid transparent;border-radius:8px;text-align:left;transition:background .15s}
.workspace-option:hover,.group-option:hover{background:hsl(var(--muted)/.5)}.workspace-option.active{border-color:hsl(var(--primary)/.25);background:hsl(var(--primary)/.08);color:var(--nav-accent)}
.workspace-option-copy{display:grid;gap:3px;min-width:0;flex:1}.workspace-option-copy strong{font-size:14px;font-weight:600;overflow-wrap:anywhere}.workspace-option-copy small{font-size:11px;font-weight:400;color:var(--nav-muted)}
.group-navigation{margin-top:24px;padding-top:20px;border-top:1px solid var(--nav-line)}.group-navigation nav{display:grid;gap:3px}
.group-option{display:flex;align-items:center;justify-content:space-between;gap:8px;width:100%;padding:9px 10px;border-radius:6px;text-align:left;font-size:13px;color:var(--nav-muted)}.group-option span{overflow-wrap:anywhere}.group-option small{font-size:11px;opacity:.7}.group-option.active{background:hsl(var(--muted));color:hsl(var(--foreground));font-weight:600}
.navigation-content{display:grid;gap:20px;min-width:0}.navigation-card{min-width:0;border:1px solid var(--nav-line);border-radius:12px;background:var(--nav-surface)}
.workspace-settings{padding:22px 24px}.navigation-card-heading{display:flex;align-items:center;gap:12px;margin-bottom:20px}.navigation-icon-tile{display:grid;place-items:center;width:40px;height:40px;border:1px solid hsl(var(--primary)/.18);border-radius:10px;background:hsl(var(--primary)/.07);color:var(--nav-accent)}
.navigation-card h3{margin:0;font-size:15px;line-height:1.5;font-weight:600}.navigation-card-heading p,.menu-toolbar p{margin:3px 0 0;font-size:12px;color:var(--nav-muted)}
.navigation-fields{display:grid;grid-template-columns:minmax(0,1fr) 88px 116px;gap:18px;align-items:end}.navigation-fields label:not(.navigation-toggle){display:grid;gap:7px;min-width:0;font-size:12px;color:var(--nav-muted)}
.navigation-fields :deep(input),.menu-row :deep(input){min-width:0;width:100%}.navigation-toggle{display:flex;align-items:center;gap:9px;min-height:36px;font-size:12px;white-space:nowrap;cursor:pointer}.navigation-toggle input{appearance:none!important;position:relative;width:30px!important;height:18px!important;min-width:30px!important;flex-shrink:0;border:1px solid hsl(var(--muted-foreground)/.35);border-radius:20px;background:hsl(var(--muted));transition:background .15s;cursor:pointer}.navigation-toggle input:after{content:'';position:absolute;top:2px;left:2px;width:12px;height:12px;border-radius:50%;background:hsl(var(--muted-foreground));transition:transform .15s}.navigation-toggle input:checked{background:var(--nav-accent);border-color:var(--nav-accent)}.navigation-toggle input:checked:after{transform:translateX(12px);background:hsl(var(--primary-foreground))}.navigation-toggle input:focus-visible{outline:2px solid var(--nav-accent);outline-offset:3px}
.menu-toolbar{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:22px 24px;border-bottom:1px solid var(--nav-line)}
.navigation-search{display:flex;align-items:center;gap:8px;max-width:240px;min-width:0;padding:8px 10px;border:1px solid var(--nav-line);border-radius:7px;color:var(--nav-muted)}.navigation-search:focus-within{outline:2px solid hsl(var(--primary)/.4)}.navigation-search input{width:100%;min-width:0;border:0;outline:0;background:transparent;font-size:12px;color:hsl(var(--foreground))}
.group-settings{padding:20px 24px;background:hsl(var(--muted)/.2);border-bottom:1px solid var(--nav-line)}.group-heading{display:flex;align-items:center;gap:8px;margin-bottom:16px;color:var(--nav-muted)}.group-heading strong{font-size:13px;font-weight:600;color:hsl(var(--foreground))}.group-heading span{margin-left:auto;font-size:11px}
.menu-columns,.menu-row{display:grid;grid-template-columns:minmax(100px,1fr) minmax(120px,1.3fr) 76px 48px;align-items:center;gap:16px;padding:12px 24px}.menu-columns{font-size:11px;color:var(--nav-muted);background:hsl(var(--muted)/.12)}.menu-row{min-height:62px;border-top:1px solid var(--nav-line)}.menu-row:hover{background:hsl(var(--muted)/.15)}.menu-row--hidden .menu-original{color:var(--nav-muted)}.menu-original{display:flex;align-items:center;gap:10px;min-width:0}.menu-original strong{font-size:13px;font-weight:500;overflow-wrap:anywhere}.menu-dot{width:5px;height:5px;flex-shrink:0;border-radius:2px;background:hsl(var(--muted-foreground)/.35)}.menu-visibility{justify-content:center}
.navigation-parent-notice{display:flex;align-items:center;gap:8px;margin:0;padding:10px 24px;background:hsl(var(--muted)/.4);color:var(--nav-muted);font-size:12px}
.navigation-help{display:flex;align-items:flex-start;gap:7px;padding:0 2px;margin:0;font-size:11px;line-height:1.7;color:var(--nav-muted)}.navigation-help svg{margin-top:2px}
.navigation-empty{display:grid;justify-items:center;gap:8px;padding:56px 20px;text-align:center;color:var(--nav-muted)}.navigation-empty h4{margin:6px 0 0;font-size:14px;font-weight:500;color:hsl(var(--foreground))}.navigation-empty p{margin:0;font-size:12px}.navigation-empty button{margin-top:8px;font-size:12px;color:var(--nav-accent)}
.navigation-save-bar{position:sticky;bottom:0;z-index:10;display:flex;align-items:center;justify-content:space-between;gap:16px;margin-top:24px;padding:16px 20px;border:1px solid var(--nav-line);border-radius:10px;background:var(--nav-surface);box-shadow:0 -5px 20px hsl(var(--background)/.4)}.navigation-save-state{display:flex;align-items:center;gap:8px;font-size:12px;color:var(--nav-muted)}.navigation-save-state small{padding-left:10px;border-left:1px solid var(--nav-line);font-size:11px}.unsaved-dot{width:6px;height:6px;border-radius:50%;background:#e7a840}.navigation-actions{display:flex;align-items:center;gap:8px}.navigation-actions :deep(button){gap:7px;white-space:nowrap}.navigation-error{margin-bottom:16px;padding:12px 16px;border:1px solid hsl(var(--destructive)/.3);border-radius:8px;background:hsl(var(--destructive)/.08);color:hsl(var(--destructive));font-size:13px}
.mobile-group-select,.mobile-field-label{display:none}
@media(max-width:1100px){.navigation-layout{grid-template-columns:164px minmax(0,1fr);gap:16px}.navigation-save-state small{display:none}.navigation-fields{grid-template-columns:minmax(0,1fr) 76px 104px;gap:12px}.menu-columns,.menu-row{grid-template-columns:minmax(80px,1fr) minmax(100px,1.2fr) 66px 36px;gap:10px;padding-left:18px;padding-right:18px}.workspace-settings,.menu-toolbar,.group-settings{padding:18px}}
@media(max-width:760px){.navigation-layout{grid-template-columns:minmax(0,1fr);gap:16px}.navigation-rail{padding:10px;border-radius:10px}.navigation-rail>.navigation-eyebrow,.group-navigation{display:none}.workspace-options{grid-template-columns:repeat(3,minmax(0,1fr))}.workspace-option{justify-content:center;gap:7px;padding:10px 6px}.workspace-option-copy{flex:initial;gap:0}.workspace-option-copy strong{font-size:13px}.workspace-option-copy small{display:none}.navigation-content{gap:16px}.mobile-group-select{display:flex;align-items:center;gap:12px;padding:14px 18px;border-bottom:1px solid var(--nav-line);font-size:12px;color:var(--nav-muted)}.mobile-group-select select{flex:1;min-width:0;padding:8px 10px;border:1px solid var(--nav-line);border-radius:6px;background:var(--nav-surface);color:hsl(var(--foreground))}.navigation-save-bar{margin-top:16px;padding:12px}.navigation-save-state{font-size:11px}.navigation-actions{gap:5px}.navigation-actions :deep(button){padding-left:10px;padding-right:10px;font-size:12px}.navigation-actions :deep(svg){display:none}}
@media(max-width:500px){.workspace-option>.navigation-icon{width:16px;height:16px}.menu-toolbar{align-items:stretch;flex-direction:column;gap:12px}.navigation-search{max-width:none}.navigation-fields{grid-template-columns:minmax(0,1fr) 76px;gap:14px}.navigation-toggle{grid-column:1/-1;min-height:24px}.navigation-fields .navigation-toggle{justify-content:space-between;flex-direction:row-reverse}.navigation-card-heading{margin-bottom:18px}.menu-columns{display:none}.menu-row{grid-template-columns:minmax(0,1fr) 76px;gap:12px;padding:16px 18px}.menu-original{grid-column:1;grid-row:1}.menu-visibility{grid-column:2;grid-row:1;justify-content:flex-end}.menu-label-field{grid-column:1;grid-row:2}.menu-order-field{grid-column:2;grid-row:2}.mobile-field-label{display:block;font-size:11px;color:var(--nav-muted)}.menu-label-field,.menu-order-field{display:grid;gap:6px}.menu-visibility .mobile-field-label{display:none}.navigation-save-bar{flex-wrap:wrap;gap:10px}.navigation-save-state{flex-basis:100%}.navigation-actions{width:100%}.navigation-actions :deep(button:last-child){margin-left:auto}.navigation-parent-notice{align-items:flex-start;padding:12px 18px;line-height:1.6}.navigation-parent-notice svg{margin-top:2px}}
</style>
