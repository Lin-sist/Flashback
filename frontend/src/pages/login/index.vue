<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import AppTopSafeBar from '../../components/common/AppTopSafeBar.vue'
import { useUserStore } from '../../stores'
import { toUserMessage, validateNickname, validatePassword, validateUsername } from '../../utils'

type Mode = 'login' | 'register'

const userStore = useUserStore()
const mode = ref<Mode>('login')
const loading = ref(false)
const passwordVisible = ref(false)

const form = reactive({
  nickname: '',
  username: '',
  password: '',
})

const welcomeKicker = computed(() => (mode.value === 'login' ? '欢迎回来' : '初次相逢'))
const welcomeCopy = computed(() =>
  mode.value === 'login'
    ? '那些被认真封存的片段，仍在这里安静等你。'
    : '从这一刻起，为未来留下第一条可以回望的线索。'
)
const primaryLabel = computed(() => (mode.value === 'login' ? '进入档案馆' : '完成注册'))
const modeSummary = computed(() =>
  mode.value === 'login'
    ? '昵称会一并显示，但登录时仅校验用户名 / 邮箱与密码。'
    : '注册会校验昵称、用户名 / 邮箱与密码，并保留你此刻的名字。'
)

const switchMode = (next: Mode) => {
  if (loading.value || mode.value === next) return
  mode.value = next
  passwordVisible.value = false
}

const togglePassword = () => {
  passwordVisible.value = !passwordVisible.value
}

const onLogin = async () => {
  if (!validateUsername(form.username)) {
    uni.showToast({ title: '请填写至少 3 位的用户名 / 邮箱', icon: 'none' })
    return
  }

  if (!validatePassword(form.password)) {
    uni.showToast({ title: '密码至少需要 6 位', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
    })
    await userStore.fetchUserInfo()
    uni.switchTab({ url: '/pages/home/index' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const onRegister = async () => {
  if (!validateUsername(form.username)) {
    uni.showToast({ title: '请填写至少 3 位的用户名 / 邮箱', icon: 'none' })
    return
  }

  if (!validatePassword(form.password)) {
    uni.showToast({ title: '密码至少需要 6 位', icon: 'none' })
    return
  }

  if (!validateNickname(form.nickname)) {
    uni.showToast({ title: '请先留下你的昵称', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
    })
    form.password = ''
    passwordVisible.value = false
    mode.value = 'login'
    uni.showToast({ title: '注册完成，请登录进入档案馆', icon: 'none' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const onSubmit = async () => {
  if (loading.value) return

  if (mode.value === 'login') {
    await onLogin()
    return
  }

  await onRegister()
}

onShow(() => {
  uni.hideTabBar({ animation: false })
})
</script>

<template>
  <view class="page">
    <AppTopSafeBar transparent />

    <view class="page-glow page-glow-top" />
    <view class="page-glow page-glow-bottom" />

    <view class="content">
      <view class="hero">
        <view class="hero-kicker">{{ welcomeKicker }}</view>
        <view class="brand-title">时光回序</view>
        <view class="hero-copy">{{ welcomeCopy }}</view>
      </view>

      <view class="mode-switch">
        <view
          class="mode-item"
          :class="{ active: mode === 'login' }"
          @tap="switchMode('login')"
        >
          <text class="mode-text">登录</text>
          <view class="mode-underline" />
        </view>
        <view
          class="mode-item"
          :class="{ active: mode === 'register' }"
          @tap="switchMode('register')"
        >
          <text class="mode-text">注册</text>
          <view class="mode-underline" />
        </view>
      </view>

      <view class="form">
        <view class="field">
          <text class="field-label">昵称</text>
          <input
            v-model="form.nickname"
            class="field-input"
            placeholder="昵称"
            placeholder-class="field-placeholder"
            maxlength="50"
          />
        </view>

        <view class="field">
          <text class="field-label">用户名 / 邮箱</text>
          <input
            v-model="form.username"
            class="field-input"
            placeholder="用户名 / 邮箱"
            placeholder-class="field-placeholder"
            maxlength="80"
          />
        </view>

        <view class="field field-password">
          <text class="field-label">密码</text>
          <input
            v-model="form.password"
            class="field-input"
            :password="!passwordVisible"
            placeholder="密码"
            placeholder-class="field-placeholder"
            maxlength="64"
          />
          <view
            class="eye"
            :class="{ 'eye-on': passwordVisible }"
            @tap="togglePassword"
            aria-label="切换密码可见性"
          />
        </view>
      </view>

      <view class="mode-summary">
        {{ modeSummary }}
      </view>

      <view class="action">
        <button class="pill-button" :loading="loading" @tap="onSubmit">
          {{ primaryLabel }}
        </button>
      </view>

      <view class="action-note">
        {{ mode === 'login' ? '已有账号可直接进入，昵称不会在登录时额外拦截。' : '完成注册后会回到登录态，不会自动跳转其他页面。' }}
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0) 38%),
    radial-gradient(circle at 88% 16%, rgba(240, 225, 196, 0.28) 0%, rgba(240, 225, 196, 0) 26%),
    radial-gradient(circle at 8% 86%, rgba(205, 219, 227, 0.18) 0%, rgba(205, 219, 227, 0) 30%),
    linear-gradient(180deg, #f8f9fa 0%, #f1f4f6 100%);
}

.page-glow {
  position: absolute;
  border-radius: 999rpx;
  filter: blur(18rpx);
  pointer-events: none;
  opacity: 0.8;
}

.page-glow-top {
  top: 100rpx;
  right: -90rpx;
  width: 280rpx;
  height: 280rpx;
  background: rgba(240, 214, 169, 0.24);
}

.page-glow-bottom {
  left: -80rpx;
  bottom: 180rpx;
  width: 260rpx;
  height: 260rpx;
  background: rgba(165, 185, 198, 0.16);
}

.content {
  position: relative;
  z-index: 1;
  padding: 8rpx 72rpx 80rpx;
  display: flex;
  flex-direction: column;
}

.hero {
  margin-top: 176rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.hero-kicker {
  font-size: 24rpx;
  letter-spacing: 8rpx;
  color: #97a1a8;
}

.brand-title {
  margin-top: 28rpx;
  font-size: 68rpx;
  line-height: 1.16;
  color: #1f262b;
  letter-spacing: 14rpx;
  font-weight: 600;
  text-indent: 14rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.hero-copy {
  margin-top: 40rpx;
  max-width: 470rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: #8d979d;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.mode-switch {
  margin-top: 172rpx;
  display: flex;
  justify-content: center;
  gap: 72rpx;
}

.mode-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
  padding: 4rpx 8rpx 0;
}

.mode-text {
  font-size: 28rpx;
  letter-spacing: 6rpx;
  color: #b0b9bf;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  transition: color 160ms ease;
}

.mode-item.active .mode-text {
  color: #32424c;
}

.mode-underline {
  width: 48rpx;
  height: 2rpx;
  background: transparent;
  border-radius: 2rpx;
  transition: background 160ms ease;
}

.mode-item.active .mode-underline {
  background: #7a8891;
}

.form {
  margin-top: 88rpx;
  display: flex;
  flex-direction: column;
  gap: 54rpx;
}

.field {
  position: relative;
  padding: 0 6rpx 18rpx;
  border-bottom: 1rpx solid rgba(163, 175, 183, 0.34);
}

.field-label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 22rpx;
  line-height: 1;
  letter-spacing: 4rpx;
  color: #a2acb2;
  text-align: center;
}

.field-input {
  width: 100%;
  height: 64rpx;
  text-align: center;
  background: transparent;
  border: none;
  font-size: 30rpx;
  letter-spacing: 3rpx;
  color: #27343d;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.field-password .field-input {
  padding: 0 64rpx;
}

:deep(.field-placeholder) {
  color: #bcc5ca;
  font-size: 30rpx;
  letter-spacing: 3rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.eye {
  position: absolute;
  right: 6rpx;
  bottom: 18rpx;
  width: 44rpx;
  height: 44rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: 36rpx 36rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa5ac' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'><path d='M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12z'/><circle cx='12' cy='12' r='2.6'/><line x1='4' y1='4' x2='20' y2='20'/></svg>");
  opacity: 0.85;
}

.eye.eye-on {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%237a8891' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'><path d='M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12z'/><circle cx='12' cy='12' r='2.6'/></svg>");
  opacity: 1;
}

.mode-summary {
  margin-top: 40rpx;
  text-align: center;
  font-size: 22rpx;
  line-height: 1.9;
  color: #98a2a8;
  letter-spacing: 1rpx;
}

.action {
  margin-top: 98rpx;
  display: flex;
  justify-content: center;
}

.pill-button {
  width: 380rpx;
  height: 92rpx;
  line-height: 92rpx;
  padding: 0;
  background: linear-gradient(180deg, rgba(251, 252, 252, 0.96) 0%, rgba(241, 245, 247, 0.96) 100%);
  border: 1rpx solid rgba(151, 164, 173, 0.52);
  border-radius: 999rpx;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.88) inset,
    0 14rpx 28rpx rgba(81, 96, 107, 0.08);
  color: #2f3e47;
  font-size: 30rpx;
  letter-spacing: 8rpx;
  font-weight: 400;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.pill-button::after {
  border: none;
}

.pill-button[loading]::before {
  margin-right: 12rpx;
}

.action-note {
  margin-top: 36rpx;
  text-align: center;
  font-size: 22rpx;
  line-height: 1.8;
  color: #a5aeb4;
}
</style>
