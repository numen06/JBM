import { ref, onMounted, type Ref } from 'vue'
import type { DataPaging } from '@/api/types'

export function usePagedList<T>(
  fetcher: (page: number, size: number) => Promise<DataPaging<T>>,
  pageSize = 20,
) {
  const items: Ref<T[]> = ref([])
  const total = ref(0)
  const page = ref(1)
  const loading = ref(false)
  const error = ref('')

  async function load(p = page.value) {
    loading.value = true
    error.value = ''
    try {
      const data = await fetcher(p, pageSize)
      items.value = data.contents ?? []
      total.value = data.total ?? 0
      page.value = p
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载失败'
      items.value = []
    } finally {
      loading.value = false
    }
  }

  onMounted(() => load(1))

  return { items, total, page, loading, error, load, pageSize }
}
