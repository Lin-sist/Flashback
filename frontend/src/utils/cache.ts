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
