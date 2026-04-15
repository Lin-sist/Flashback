import { httpRequest } from './httpClient'
import type {
  LoginPayload,
  LoginResponseVO,
  RegisterPayload,
  UserInfoVO,
  UserProfileUpdate,
} from '../types'

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
  getCurrentUser() {
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
