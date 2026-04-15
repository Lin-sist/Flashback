import type { ApiResponse } from '../types'
import { clearToken, getToken } from '../utils'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
type RequestData = UniApp.RequestOptions['data']

interface RequestOptions {
  url: string
  method?: HttpMethod
  data?: RequestData
  auth?: boolean
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const httpRequest = <T>(options: RequestOptions): Promise<T> => {
  const token = getToken()
  const authRequired = options.auth ?? true

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method ?? 'GET',
      data: options.data,
      timeout: 10000,
      header: {
        'Content-Type': 'application/json',
        ...(authRequired && token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: (res) => {
        if (res.statusCode === 401) {
          clearToken()
          uni.reLaunch({ url: '/pages/login/index' })
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
      fail: () => {
        reject(new Error('Network error'))
      },
    })
  })
}
