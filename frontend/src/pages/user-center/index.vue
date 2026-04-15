<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { reactive } from 'vue'
import { useUserStore } from '../../stores'
import { getToken, toUserMessage } from '../../utils'

const userStore = useUserStore()

const profile = reactive({
  nickname: '',
  email: '',
})

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadProfile = async () => {
  if (!ensureLogin()) {
    return
  }
  try {
    const user = await userStore.fetchUserInfo()
    profile.nickname = user?.nickname || ''
    profile.email = user?.email || ''
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
}

onShow(() => {
  loadProfile()
})

const saveProfile = async () => {
  try {
    await userStore.updateProfile({ nickname: profile.nickname, email: profile.email })
    uni.showToast({ title: 'Profile updated', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
}

const logout = () => {
  userStore.logout()
}
</script>

<template>
  <view class="page">
    <view class="section">
      <input v-model="profile.nickname" class="input" placeholder="Nickname" />
      <input v-model="profile.email" class="input" placeholder="Email" />
      <button class="btn" @tap="saveProfile">Save Profile</button>
    </view>

    <button class="btn danger" @tap="logout">Logout</button>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.section {
  background: #ffffff;
  border-radius: 14rpx;
  padding: 20rpx;
}

.input {
  width: 100%;
  padding: 18rpx;
  background: #f8fafc;
  border-radius: 10rpx;
  margin-bottom: 12rpx;
  box-sizing: border-box;
}

.btn {
  margin-top: 10rpx;
  border-radius: 9999rpx;
  background: #0ea5e9;
  color: #ffffff;
}

.btn.danger {
  background: #ef4444;
}
</style>
