import { getDocBlob } from '@/api/doc'
import { onBeforeUnmount, ref, watch, type MaybeRefOrGetter, toValue } from 'vue'

function isDirectUrl(value: string) {
  return /^(data:|https?:\/\/|blob:|\/)/i.test(value)
}

export function useDocImageSrc(source: MaybeRefOrGetter<string | undefined>) {
  const imageSrc = ref('')
  let objectUrl = ''

  function clear() {
    if (objectUrl) {
      URL.revokeObjectURL(objectUrl)
      objectUrl = ''
    }
    imageSrc.value = ''
  }

  watch(
    () => toValue(source),
    async (value) => {
      clear()
      const path = value?.trim()
      if (!path) return
      if (isDirectUrl(path)) {
        imageSrc.value = path
        return
      }
      try {
        const blob = await getDocBlob(path)
        objectUrl = URL.createObjectURL(blob)
        imageSrc.value = objectUrl
      } catch {
        imageSrc.value = ''
      }
    },
    { immediate: true },
  )

  onBeforeUnmount(clear)

  return imageSrc
}
