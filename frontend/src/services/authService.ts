import { httpRequest } from './httpClient'
import type {
  LoginPayload,
  LoginResponseVO,
  RegisterPayload,
  UserInfoVO,
  UserProfileUpdate,
  WechatLoginPayload,
} from '../types'
import { getPreviewUserInfo } from '../features/preview/data/preview-data'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'

const shouldUsePreviewData = () => !getToken() && hasPreviewSession()

export const authService = {
  register(payload: RegisterPayload) {
    return httpRequest<void>({
      url: '/api/auth/register',
      method: 'POST',
      data: payload,
      auth: false,
    })
  },
  login(payload: LoginPayload) {
    return httpRequest<LoginResponseVO>({
      url: '/api/auth/login',
      method: 'POST',
      data: payload,
      auth: false,
    })
  },
  wechatLogin(payload: WechatLoginPayload) {
    return httpRequest<LoginResponseVO>({
      url: '/api/auth/wechat-login',
      method: 'POST',
      data: payload,
      auth: false,
    })
  },
  getCurrentUser() {
    if (shouldUsePreviewData()) {
      return Promise.resolve(getPreviewUserInfo())
    }

    return httpRequest<UserInfoVO>({
      url: '/api/user/me',
    })
  },
  updateProfile(payload: UserProfileUpdate) {
    return httpRequest<UserInfoVO>({
      url: '/api/user/profile',
      method: 'PUT',
      data: payload,
    })
  },
}
