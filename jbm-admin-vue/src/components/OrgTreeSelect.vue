<script setup lang="ts">
import { computed, onMounted } from 'vue'
import Select from '@/components/ui/Select.vue'
import { orgOptionLabel, orgRowId, useOrgTree } from '@/composables/useOrgTree'

const props = withDefaults(
  defineProps<{
    modelValue?: number | string | null
    placeholder?: string
    required?: boolean
    disabled?: boolean
    excludeIds?: number[]
  }>(),
  {
    placeholder: '请选择组织',
    required: false,
    disabled: false,
    excludeIds: () => [],
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: number | string | null]
}>()

const { flatOrgs, loadOrgs } = useOrgTree()

const selectableOrgs = computed(() => {
  if (!props.excludeIds.length) return flatOrgs.value
  const excluded = new Set(props.excludeIds)
  return flatOrgs.value.filter((o) => {
    const id = orgRowId(o)
    return id == null || !excluded.has(id)
  })
})

onMounted(loadOrgs)

function onChange(raw: string) {
  if (!raw) {
    emit('update:modelValue', null)
    return
  }
  const n = Number(raw)
  emit('update:modelValue', Number.isNaN(n) ? raw : n)
}
</script>

<template>
  <Select
    :model-value="modelValue != null && modelValue !== '' ? String(modelValue) : ''"
    :disabled="disabled"
    @update:model-value="onChange"
  >
    <option v-if="!required" value="">{{ placeholder }}</option>
    <option v-else value="" disabled>{{ placeholder }}</option>
    <option v-for="o in selectableOrgs" :key="orgRowId(o)" :value="orgRowId(o)">
      {{ orgOptionLabel(o) }}
    </option>
  </Select>
</template>
