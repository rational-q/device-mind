import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

service.interceptors.response.use(
  (response) => {
    const res = response.data as Result<unknown>
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res.data as any
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
