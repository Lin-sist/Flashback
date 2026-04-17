<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useUserStore } from '../../stores'
import { toUserMessage, validateNickname, validatePassword, validateUsername } from '../../utils'

const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
})

const onLogin = async () => {
  if (!validateUsername(form.username)) {
    uni.showToast({ title: 'Username >= 3 chars', icon: 'none' })
    return
  }
  if (!validatePassword(form.password)) {
    uni.showToast({ title: 'Password >= 6 chars', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.login({ ...form })
    await userStore.fetchUserInfo()
    uni.switchTab({ url: '/pages/home/index' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const onRegister = async () => {
  if (!validateUsername(form.username) || !validatePassword(form.password)) {
    uni.showToast({ title: 'Invalid username or password', icon: 'none' })
    return
  }
  if (!validateNickname(form.nickname)) {
    uni.showToast({ title: 'Nickname is required', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.register({ ...form })
    uni.showToast({ title: 'Registered', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="title">Flashback</view>
    <view class="subtitle">Mini Program user login/register</view>

    <input v-model="form.username" class="input" placeholder="Username" />
    <input v-model="form.password" class="input" type="password" placeholder="Password" />
    <input v-model="form.nickname" class="input" placeholder="Nickname (required for register)" />
    <view class="hint">This page uses `/api/auth/login`. Admin login is not wired here yet.</view>

    <button class="btn primary" :loading="loading" @tap="onLogin">Login</button>
    <button class="btn" :loading="loading" @tap="onRegister">Register</button>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 56rpx 32rpx;
}

.title {
  font-size: 48rpx;
  font-weight: 700;
}

.subtitle {
  margin-top: 10rpx;
  margin-bottom: 24rpx;
  color: #667085;
}

.input {
  width: 100%;
  margin-bottom: 20rpx;
  padding: 22rpx;
  background: #ffffff;
  border-radius: 12rpx;
}

.hint {
  margin: 8rpx 0 16rpx;
  color: #667085;
  font-size: 24rpx;
  line-height: 1.5;
}

.btn {
  margin-top: 12rpx;
  border-radius: 9999rpx;
}

.btn.primary {
  background: #0ea5e9;
  color: #ffffff;
}
</style>
