import { ref, type Ref } from 'vue'

export function useCrudForm<T extends object>(factory: () => T) {
  const dialogOpen = ref(false)
  const editing = ref(false)
  const saving = ref(false)
  const form = ref(factory()) as Ref<T>
  const formError = ref('')

  function openCreate() {
    editing.value = false
    form.value = factory()
    formError.value = ''
    dialogOpen.value = true
  }

  function openEdit(row: Partial<T>) {
    editing.value = true
    form.value = { ...factory(), ...row }
    formError.value = ''
    dialogOpen.value = true
  }

  function closeDialog() {
    dialogOpen.value = false
  }

  return {
    dialogOpen,
    editing,
    saving,
    form,
    formError,
    openCreate,
    openEdit,
    closeDialog,
  }
}
