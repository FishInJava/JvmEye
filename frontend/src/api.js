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

/**
 * 只取响应体。
 *
 * 后端返回的是扁平 JSON(没有 code/msg/data 外层信封),所有接口都通过这里解包,
 * 将来若要改成统一信封,只改这一处。
 */
const payload = (response) => response.data

/**
 * 提取后端错误信息。
 *
 * 后端错误体是 RFC 7807 ProblemDetail,错误原因在 `detail` 字段;
 * 同时兼容旧字段 `message`,避免后端回滚时前端提示失效。
 */
export function errorMessage(error, fallback = '请求失败') {
  const data = error?.response?.data
  return data?.detail || data?.message || error?.message || fallback
}

export const targetsApi = {
  /** 列出本机 JVM。 @returns {Promise<Array>} 目标进程列表 */
  list: (includeSelf = false) =>
    payload(api.get('/targets', { params: { includeSelf } })).then((data) => data.targets || [])
}

export const monitorApi = {
  /** 连接指定 pid。 @returns {Promise<{connected: boolean, pid: number, displayName: string, connectedAt: number}>} */
  connect: (pid) => payload(api.post(`/targets/${pid}/connect`)),
  /** 断开指定 pid。 @returns {Promise<{connected: boolean, pid: number, closed: number}>} */
  disconnect: (pid) => payload(api.delete(`/targets/${pid}/disconnect`)),
  /** 断开全部。 @returns {Promise<{connected: boolean, closed: number}>} */
  disconnectAll: () => payload(api.delete('/targets/disconnect')),
  /** 当前连接状态。 @returns {Promise<{connected: boolean, pid: number, displayName: string, bufferedPoints: number, connectedAt: number, closeReason: string|null}>} */
  status: () => payload(api.get('/monitor/status'))
}

export const metricsApi = {
  /** 实时指标快照。 @returns {Promise<object>} 结构见 docs/API.md */
  current: () => payload(api.get('/metrics/current')),
  /** 最近 points 点历史。 @returns {Promise<{points: Array<object>}>} */
  history: (points = 300) => payload(api.get('/metrics/history', { params: { points } }))
}

export const diagnosticsApi = {
  /** 完整线程 Dump。 @returns {Promise<{threads: Array<object>, deadlockedCount: number}>} */
  threadDump: () => payload(api.get('/diagnostics/thread-dump')),
  /** 类直方图(会触发目标 Full GC)。 @returns {Promise<{entries: Array<object>, totalBytes: number}>} */
  histogram: (top = 30) => payload(api.get('/diagnostics/histogram', { params: { top } }))
}

export default api
