<script setup lang="ts">
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

const heroTitleLines = ['久违了，', '时间的旅人']

const subtitle = computed(() =>
  mode.value === 'login' ? '在此处，开启你的私人档案馆' : '在此处，建立你的私人档案馆'
)

const primaryLabel = computed(() => (mode.value === 'login' ? '进入档案馆' : '开启档案馆'))

const switchMode = (next: Mode) => {
  if (loading.value || mode.value === next) return
  mode.value = next
}

const togglePassword = () => {
  passwordVisible.value = !passwordVisible.value
}

const onForgot = () => {
  uni.showToast({ title: '寻回记忆的入口，稍后再为你打开', icon: 'none' })
}

const onSubmit = async () => {
  if (loading.value) return

  if (!validateUsername(form.username)) {
    uni.showToast({ title: '请填写至少 3 位的用户名 / 邮箱', icon: 'none' })
    return
  }
  if (!validatePassword(form.password)) {
    uni.showToast({ title: '密码至少需要 6 位', icon: 'none' })
    return
  }

  if (mode.value === 'register' && !validateNickname(form.nickname)) {
    uni.showToast({ title: '请先留下你的昵称', icon: 'none' })
    return
  }

  loading.value = true
  try {
    if (mode.value === 'login') {
      await userStore.login({ username: form.username, password: form.password })
      await userStore.fetchUserInfo()
      uni.switchTab({ url: '/pages/home/index' })
    } else {
      await userStore.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username,
      })
      uni.showToast({ title: '档案馆已为你开启', icon: 'none' })
      mode.value = 'login'
    }
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="page">
    <AppTopSafeBar title="时光回序" transparent />

    <view class="content">
      <view class="hero">
        <view class="hero-title">
          <text v-for="line in heroTitleLines" :key="line" class="hero-title-line">{{ line }}</text>
        </view>
        <view class="hero-subtitle">{{ subtitle }}</view>
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
          <input
            v-model="form.nickname"
            class="field-input"
            placeholder="昵称"
            placeholder-class="field-placeholder"
            maxlength="50"
          />
        </view>

        <view class="field">
          <input
            v-model="form.username"
            class="field-input"
            placeholder="用户名 / 邮箱"
            placeholder-class="field-placeholder"
            maxlength="80"
          />
        </view>

        <view class="field field-password">
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

      <view class="action">
        <button class="pill-button" :loading="loading" @tap="onSubmit">
          {{ primaryLabel }}
        </button>
      </view>

      <view class="forgot" @tap="onForgot">
        <text class="forgot-text">寻回记忆（忘记密码）</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0) 38%),
    radial-gradient(circle at 92% 10%, rgba(227, 232, 236, 0.55) 0%, rgba(227, 232, 236, 0) 32%),
    linear-gradient(180deg, #f5f7f8 0%, #eef1f3 100%);
}

.content {
  padding: 24rpx 72rpx 80rpx;
  display: flex;
  flex-direction: column;
}

/* Hero */
.hero {
  margin-top: 180rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.hero-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18rpx;
  color: #2d3d48;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.hero-title-line {
  font-size: 60rpx;
  line-height: 1.2;
  letter-spacing: 10rpx;
  font-weight: 500;
}

.hero-subtitle {
  margin-top: 48rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: #9aa5ac;
  letter-spacing: 4rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

/* Mode switch */
.mode-switch {
  margin-top: 180rpx;
  display: flex;
  justify-content: center;
  gap: 88rpx;
}

.mode-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
  padding: 4rpx 8rpx 0;
}

.mode-text {
  font-size: 30rpx;
  letter-spacing: 8rpx;
  color: #c0c8cd;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  transition: color 160ms ease;
}

.mode-item.active .mode-text {
  color: #2d3d48;
}

.mode-underline {
  width: 54rpx;
  height: 2rpx;
  background: transparent;
  border-radius: 2rpx;
  transition: background 160ms ease;
}

.mode-item.active .mode-underline {
  background: #7a8891;
}

/* Form */
.form {
  margin-top: 96rpx;
  display: flex;
  flex-direction: column;
  gap: 70rpx;
}

.field {
  position: relative;
  padding-bottom: 18rpx;
  border-bottom: 1rpx dashed #c7ced2;
}

.field-input {
  width: 100%;
  height: 64rpx;
  text-align: center;
  background: transparent;
  border: none;
  font-size: 30rpx;
  letter-spacing: 4rpx;
  color: #2d3d48;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.field-password .field-input {
  padding-right: 64rpx;
}

/* Scoped placeholder class for uni-app input placeholder-class */
:deep(.field-placeholder) {
  color: #b9c1c6;
  font-size: 30rpx;
  letter-spacing: 4rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.eye {
  position: absolute;
  right: 8rpx;
  top: 50%;
  transform: translateY(-50%);
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

/* Action */
.action {
  margin-top: 128rpx;
  display: flex;
  justify-content: center;
}

.pill-button {
  width: 360rpx;
  height: 92rpx;
  line-height: 92rpx;
  padding: 0;
  background: transparent;
  border: 1rpx solid #9faab1;
  border-radius: 999rpx;
  color: #2d3d48;
  font-size: 30rpx;
  letter-spacing: 10rpx;
  font-weight: 400;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.pill-button::after {
  border: none;
}

.pill-button[loading]::before {
  margin-right: 12rpx;
}

/* Forgot */
.forgot {
  margin-top: 72rpx;
  display: flex;
  justify-content: center;
}

.forgot-text {
  font-size: 24rpx;
  letter-spacing: 3rpx;
  color: #a6afb5;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}
</style>
