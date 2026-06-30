import { ref, reactive } from 'vue'
import type { PageResult } from '@/types/api'

export function useCrud<T>(listApi: (query: Record<string, any>) => Promise<PageResult<T>>) {
  const loading = ref(false)
  const tableData = ref<T[]>([])
  const total = ref(0)
  const query = reactive({ pageNum: 1, pageSize: 10 })

  async function fetchData(params?: Record<string, any>) {
    loading.value = true
    try {
      const res = await listApi({ ...query, ...params })
      tableData.value = res.records
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  function resetQuery() {
    query.pageNum = 1
    fetchData()
  }

  return { loading, tableData, total, query, fetchData, resetQuery }
}
