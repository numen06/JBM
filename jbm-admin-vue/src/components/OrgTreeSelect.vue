<script setup lang="ts">
import { onMounted } from 'vue'
import Select from '@/components/ui/Select.vue'
import { orgOptionLabel, orgRowId, useOrgTree } from '@/composables/useOrgTree'

const props = withDefaults(
  defineProps<{
    modelValue?: number | string | null
    placeholder?: string
    required?: boolean
    disabled?: boolean
  }>(),
  {
    placeholder: '请选择组织',
    required: false,
    disabled: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: number | string | null]
}>()

const { flatOrgs, loadOrgs } = useOrgTree()

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
    <option v-for="o in flatOrgs" :key="orgRowId(o)" :value="orgRowId(o)">
      {{ orgOptionLabel(o) }}
    </option>
  </Select>
</template>
