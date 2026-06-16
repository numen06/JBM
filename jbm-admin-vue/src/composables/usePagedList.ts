import { ref, onMounted, type Ref } from 'vue'
import type { DataPaging } from '@/api/types'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export interface PagedListLoadOptions {
  silent?: boolean
}

export function usePagedList<T>(
  fetcher: (page: number, size: number) => Promise<DataPaging<T>>,
  pageSize = DEFAULT_PAGE_SIZE,
  autoLoad = true,
) {
  const items: Ref<T[]> = ref([])
  const total = ref(0)
  const page = ref(1)
  const currentPageSize = ref(pageSize)
  const loading = ref(false)
  const error = ref('')

  async function load(
    p = page.value,
    nextPageSize = currentPageSize.value,
    options?: PagedListLoadOptions,
  ) {
    const silent = options?.silent === true
    if (!silent) {
      loading.value = true
      error.value = ''
    }
    try {
      currentPageSize.value = nextPageSize
      const data = await fetcher(p, currentPageSize.value)
      items.value = data.contents ?? []
      total.value = data.total ?? 0
      page.value = p
      if (silent) {
        error.value = ''
      }
    } catch (e) {
      if (!silent) {
        error.value = e instanceof Error ? e.message : '加载失败'
        items.value = []
      }
    } finally {
      if (!silent) {
        loading.value = false
      }
    }
  }

  onMounted(() => {
    if (autoLoad) load(1)
  })

  return { items, total, page, loading, error, load, pageSize: currentPageSize }
}
