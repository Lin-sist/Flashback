import { defineStore } from 'pinia'
import { authService } from '../services'
import { clearToken, getToken, setToken } from '../utils'
import type { LoginPayload, RegisterPayload, UserProfileUpdate } from '../types'
import type { UserInfoVO } from '../types'

interface UserState {
  token: string | null
  userInfo: UserInfoVO | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken(),
    userInfo: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    async register(payload: RegisterPayload) {
      return authService.register(payload)
    },
    async login(payload: LoginPayload) {
      const result = await authService.login(payload)
      this.token = result.token
      setToken(result.token)
      return result
    },
    async fetchUserInfo() {
      if (!this.token) {
        this.userInfo = null
        return null
      }
      const userInfo = await authService.getCurrentUser()
      this.userInfo = userInfo
      return userInfo
    },
    async updateProfile(payload: UserProfileUpdate) {
      const userInfo = await authService.updateProfile(payload)
      this.userInfo = userInfo
      return userInfo
    },
    logout() {
      this.token = null
      this.userInfo = null
      clearToken()
      uni.reLaunch({ url: '/pages/login/index' })
    },
  },
})
