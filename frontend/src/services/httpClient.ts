import type { ApiResponse } from '../types'
import { clearToken, getToken, hasAuthenticatedSession } from '../utils'
import { isDataOwnershipMutationBlocked } from '../features/data-ownership/mutation-state'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
type RequestData = UniApp.RequestOptions['data']

interface RequestOptions {
  url: string
  method?: HttpMethod
  data?: RequestData
  auth?: boolean
  timeout?: number
}

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://127.0.0.1:8080'

export const httpRequest = <T>(options: RequestOptions): Promise<T> => {
  const method = options.method ?? 'GET'
  if (method !== 'GET' && isDataOwnershipMutationBlocked()
      && (options.url.startsWith('/api/records')
        || options.url.startsWith('/api/agent')
        || options.url.startsWith('/api/time-chapters'))) {
    return Promise.reject(new Error('清除全部记录正在进行，暂时不能新建、编辑或写入 Agent 内容'))
  }
  const token = getToken()
  const authRequired = options.auth ?? true

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method,
      data: options.data,
      timeout: options.timeout ?? 10000,
      header: {
        'Content-Type': 'application/json',
        ...(authRequired && token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: (res) => {
        if (res.statusCode === 401) {
          clearToken()
          if (!hasAuthenticatedSession()) {
            uni.reLaunch({ url: '/pages/login/index' })
          }
          reject(new Error('Login expired'))
          return
        }

        const payload = res.data as ApiResponse<T>

        if (!payload || typeof payload !== 'object') {
          reject(new Error('Invalid response'))
          return
        }

        if (payload.code !== 0) {
          reject(new Error(payload.message || 'Request failed'))
          return
        }

        resolve(payload.data)
      },
      fail: (error) => {
        const errMsg =
          error && typeof error === 'object' && 'errMsg' in error && typeof error.errMsg === 'string'
            ? error.errMsg
            : 'Network error'
        reject(new Error(errMsg))
      },
    })
  })
}
