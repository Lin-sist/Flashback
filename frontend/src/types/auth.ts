export interface RegisterPayload {
  username: string
  password: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResponseVO {
  token: string
}

export interface UserInfoVO {
  id: string
  username: string
  nickname?: string
  email?: string
  avatar?: string
  status?: 'ENABLED' | 'DISABLED'
  createdAt?: number
  updatedAt?: number
}

export interface UserProfileUpdate {
  nickname?: string
  email?: string
  avatar?: string
}
