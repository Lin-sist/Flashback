import { defineStore } from 'pinia'
import { clearPreviewSession } from '../features/preview/preview-session'
import { authService } from '../services'
import { useRecordStore } from './record'
import { useTagStore } from './tag'
import { clearAuthenticatedSession, getToken, hasAuthenticatedSession, setToken } from '../utils'
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
    isAuthenticated: () => hasAuthenticatedSession(),
  },
  actions: {
    async register(payload: RegisterPayload) {
      return authService.register(payload)
    },
    async login(payload: LoginPayload) {
      const result = await authService.login(payload)
      this.token = result.token
      setToken(result.token)
      clearPreviewSession()

      const recordStore = useRecordStore()
      const tagStore = useTagStore()
      recordStore.clearCache()
      tagStore.clearCache()

      return result
    },
    async wechatLogin(code: string) {
      const result = await authService.wechatLogin({ code })
      this.token = result.token
      setToken(result.token)
      clearPreviewSession()

      const recordStore = useRecordStore()
      const tagStore = useTagStore()
      recordStore.clearCache()
      tagStore.clearCache()

      return result
    },
    async fetchUserInfo() {
      if (!hasAuthenticatedSession()) {
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
      const recordStore = useRecordStore()
      const tagStore = useTagStore()

      recordStore.clearCache()
      tagStore.clearCache()

      this.token = null
      this.userInfo = null
      clearAuthenticatedSession()
      uni.reLaunch({ url: '/pages/login/index' })
    },
  },
})
