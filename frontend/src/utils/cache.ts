import { clearPreviewSession, hasPreviewSession } from '../features/preview/preview-session'

const TOKEN_KEY = 'flashback:token'

export const setToken = (token: string) => {
  uni.setStorageSync(TOKEN_KEY, token)
}

export const getToken = (): string | null => {
  const token = uni.getStorageSync(TOKEN_KEY)
  return token ? String(token) : null
}

export const clearToken = () => {
  uni.removeStorageSync(TOKEN_KEY)
}

export const hasAuthenticatedSession = () => Boolean(getToken() || hasPreviewSession())

export const clearAuthenticatedSession = () => {
  clearToken()
  clearPreviewSession()
}
