import axios from 'axios'
import router from './router'

/**
 * 统一的 API 客户端。
 *
 * - baseURL `/api`,withCredentials 携带 Session Cookie(JSESSIONID)
 * - 响应拦截器:401/403 或返回 HTML(会话失效被重定向到 /login)时统一跳登录页
 */
const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 20000
})

api.interceptors.response.use(
  (response) => {
    const contentType = response.headers?.['content-type'] || ''
    if (typeof response.data === 'string' && contentType.includes('text/html')) {
      // 后端把未登录请求 302 到了 /login,浏览器跟随后拿到 HTML
      router.push('/login')
      return Promise.reject(new Error('会话已失效,请重新登录'))
    }
    return response
  },
  (error) => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

/** 提取后端错误信息。 */
export function errorMessage(error, fallback = '请求失败') {
  return error?.response?.data?.message || error?.message || fallback
}

export const targetsApi = {
  list: (includeSelf = false) =>
    api.get('/targets', { params: { includeSelf } }).then((r) => r.data.targets || [])
}

export const monitorApi = {
  connect: (pid) => api.post(`/targets/${pid}/connect`).then((r) => r.data),
  disconnect: (pid) => api.delete(`/targets/${pid}/disconnect`).then((r) => r.data),
  disconnectAll: () => api.delete('/targets/disconnect').then((r) => r.data),
  status: () => api.get('/monitor/status').then((r) => r.data)
}

export const metricsApi = {
  current: () => api.get('/metrics/current').then((r) => r.data),
  history: (points = 300) => api.get('/metrics/history', { params: { points } }).then((r) => r.data)
}

export const diagnosticsApi = {
  threadDump: () => api.get('/diagnostics/thread-dump').then((r) => r.data),
  histogram: (top = 30) => api.get('/diagnostics/histogram', { params: { top } }).then((r) => r.data)
}

export default api
