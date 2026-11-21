import axios from 'axios'
import { message } from 'ant-design-vue'
import type { R } from '@blog/shared'

const TOKEN_KEY = 'blog.admin.token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/** 统一响应壳：{ code, message, data }，data 为 null 时后端会省略该字段。 */
const http = axios.create({
  baseURL: '/api/blog',
  timeout: 20000
})

/** 后端 PageResult 使用 records 字段，前端统一归一化为 list */
function normalize<T>(data: T): T {
  if (data && typeof data === 'object' && Array.isArray((data as any).records)) {
    ;(data as any).list = (data as any).records
  }
  return data
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export interface HttpError extends Error {
  notified?: boolean
}

export function isNotified(e: unknown): boolean {
  return !!(e as HttpError | null | undefined)?.notified
}

function rejectWith(msg: string): Promise<never> {
  message.error(msg)
  const error = new Error(msg) as HttpError
  error.notified = true
  return Promise.reject(error)
}

/** 401 → 清本地令牌，落在登录页（hash 路由）。带 redirect 便于登录后原路返回。 */
function toLogin() {
  clearToken()
  const redirect = encodeURIComponent(window.location.hash.replace(/^#/, '') || '/dashboard')
  window.location.hash = `#/login?redirect=${redirect}`
}

http.interceptors.response.use(
  (res) => {
    const body = res.data as R
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return normalize(body.data)
      if (body.code === 401) {
        toLogin()
        return rejectWith('登录已过期，请重新登录')
      }
      return rejectWith(body.message || '请求失败')
    }
    return normalize(body)
  },
  (err) => {
    const status = err?.response?.status
    if (status === 401) {
      toLogin()
      return rejectWith('登录已过期，请重新登录')
    }
    const msg = err?.response?.data?.message || err.message || '网络异常'
    return rejectWith(msg)
  }
)

export default http
