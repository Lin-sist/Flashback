<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import AppTopSafeBar from '../../components/common/AppTopSafeBar.vue'
import { isPreviewModeEnabled } from '../../config/app-env'
import { createPreviewSession } from '../../features/preview/preview-session'
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

const showPreviewEntry = isPreviewModeEnabled

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

const enterPreview = () => {
  if (loading.value) return
  createPreviewSession()
  uni.switchTab({ url: '/pages/home/index' })
}

onShow(() => {
  uni.hideTabBar({ animation: false })
})
</script>

<template>
  <view class="page">
    <AppTopSafeBar transparent />

    <!-- 宣纸纹理光晕 -->
    <view class="paper-glow paper-glow--tl" aria-hidden="true" />
    <view class="paper-glow paper-glow--br" aria-hidden="true" />
    <view class="paper-glow paper-glow--center" aria-hidden="true" />

    <view class="page-inner">
      <!-- 品牌名 -->
      <view class="logo">时 光 回 序</view>

      <!-- Hero 区 -->
      <view class="hero">
        <view class="headline">
          <text class="headline-line1">久违了，</text>
          <text class="headline-line2">时 间 的 旅 人</text>
        </view>
        <view class="subtitle">在此处，开启你的私人档案馆</view>

        <!-- 登录 / 注册 Tab -->
        <view class="tab-row">
          <view
            class="tab"
            :class="{ 'tab--active': mode === 'login' }"
            @tap="switchMode('login')"
          >
            <text class="tab-text">登 录</text>
          </view>
          <view class="tab-sep">·</view>
          <view
            class="tab"
            :class="{ 'tab--active': mode === 'register' }"
            @tap="switchMode('register')"
          >
            <text class="tab-text">注 册</text>
          </view>
        </view>

        <!-- 表单 -->
        <view class="field-group">
          <view v-if="mode === 'register'" class="field">
            <input
              v-model="form.nickname"
              class="field-input"
              placeholder="昵 称"
              placeholder-class="field-placeholder"
              maxlength="50"
            />
          </view>

          <view class="field">
            <input
              v-model="form.username"
              class="field-input"
              placeholder="用 户 名 / 邮 箱"
              placeholder-class="field-placeholder"
              maxlength="80"
            />
          </view>

          <view class="field field--password">
            <input
              v-model="form.password"
              class="field-input"
              :password="!passwordVisible"
              placeholder="密 码"
              placeholder-class="field-placeholder"
              maxlength="64"
            />
            <view
              class="eye-toggle"
              :class="{ 'eye-toggle--on': passwordVisible }"
              @tap="togglePassword"
              aria-label="切换密码可见性"
            />
          </view>
        </view>

        <!-- CTA 按钮 -->
        <view class="cta-wrap" @tap="onSubmit">
          <view class="cta" :class="{ 'cta--loading': loading }">
            <view class="cta-corner cta-corner--tl" aria-hidden="true" />
            <view class="cta-corner cta-corner--br" aria-hidden="true" />
            <view class="cta-dot" aria-hidden="true" />
            <text class="cta-text">{{ loading ? '请稍候...' : (mode === 'login' ? '进 入 档 案 馆' : '完 成 注 册') }}</text>
          </view>
        </view>

        <!-- 预览入口 -->
        <view v-if="showPreviewEntry" class="preview-entry" @tap="enterPreview">
          <text class="preview-text">预览进入</text>
        </view>

        <!-- 忘记密码 -->
        <view class="forgot">寻回记忆（忘记密码）</view>
      </view>
    </view>
  </view>
</template>

<style scoped>
/* ── 页面底：宣纸渐变 ── */
.page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
}

/* 宣纸光晕 */
.paper-glow {
  position: absolute;
  pointer-events: none;
  border-radius: 999rpx;
}
.paper-glow--tl {
  top: -60rpx;
  left: -80rpx;
  width: 400rpx;
  height: 400rpx;
  background: radial-gradient(ellipse, rgba(200, 185, 158, 0.18) 0%, transparent 70%);
}
.paper-glow--br {
  bottom: 80rpx;
  right: -60rpx;
  width: 360rpx;
  height: 360rpx;
  background: radial-gradient(ellipse, rgba(178, 162, 135, 0.14) 0%, transparent 70%);
}
.paper-glow--center {
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500rpx;
  height: 350rpx;
  background: radial-gradient(ellipse, rgba(250, 245, 238, 0.36) 0%, transparent 75%);
}

/* ── 内容层 ── */
.page-inner {
  position: relative;
  z-index: 2;
  padding: 0 56rpx;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ── 品牌名 ── */
.logo {
  padding-top: 104rpx;
  text-align: center;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.55em;
  color: #9e9890;
}

/* ── Hero ── */
.hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-top: 48rpx;
  padding-bottom: 64rpx;
}

/* 主标题 */
.headline {
  text-align: center;
  line-height: 1.55;
  margin-bottom: 24rpx;
}
.headline-line1 {
  display: block;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-weight: 300;
  font-size: 60rpx;
  letter-spacing: 0.06em;
  color: #302e29;
}
.headline-line2 {
  display: block;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-weight: 300;
  font-size: 52rpx;
  letter-spacing: 0.04em;
  color: #6b6560;
}

/* 副文案 */
.subtitle {
  text-align: center;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 24rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.08em;
  line-height: 1.8;
  margin-bottom: 80rpx;
}

/* ── Tab 行 ── */
.tab-row {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  margin-bottom: 80rpx;
}

.tab {
  padding: 12rpx 56rpx 20rpx;
  border-bottom: 3rpx solid #c8c2b8;
}
.tab-text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
  font-weight: 400;
  letter-spacing: 0.2em;
  color: #4a4640;
}
.tab--active {
  border-bottom: 5rpx solid #302e29;
}
.tab--active .tab-text {
  color: #302e29;
  font-weight: 500;
}

.tab-sep {
  align-self: stretch;
  display: flex;
  align-items: center;
  padding: 0 12rpx 20rpx;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 24rpx;
  color: #6b6560;
  border-bottom: 3rpx solid #c8c2b8;
}

/* ── 输入框 ── */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-bottom: 96rpx;
  padding: 0 48rpx;
}

.field {
  position: relative;
  padding: 30rpx 0 28rpx;
  border-bottom: 1rpx solid #c8c2b8;
}
.field + .field {
  margin-top: 16rpx;
}

.field-input {
  width: 100%;
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
  font-weight: 300;
  letter-spacing: 0.06em;
  color: #1a1814;
  text-align: center;
}

:deep(.field-placeholder) {
  color: #a09a92;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
  font-weight: 300;
  letter-spacing: 0.12em;
}

.field--password .field-input {
  padding-right: 64rpx;
}

/* 眼睛图标 */
.eye-toggle {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 44rpx;
  height: 44rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: 32rpx 32rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23a09a92' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M2 12s3.636-7 10-7 10 7 10 7-3.636 7-10 7S2 12 2 12z'/><circle cx='12' cy='12' r='3'/><line x1='4' y1='4' x2='20' y2='20'/></svg>");
}
.eye-toggle--on {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236b6560' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M2 12s3.636-7 10-7 10 7 10 7-3.636 7-10 7S2 12 2 12z'/><circle cx='12' cy='12' r='3'/></svg>");
}

/* ── CTA 按钮 ── */
.cta-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 0;
}

.cta {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 88rpx;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
  font-weight: 400;
  letter-spacing: 0.2em;
  color: #4a4640;
  background: transparent;
  border: 3rpx solid #6b6560;
  border-radius: 4rpx;
}
.cta--loading {
  opacity: 0.6;
}

/* 四角装饰 */
.cta-corner {
  position: absolute;
  width: 16rpx;
  height: 16rpx;
  border-color: #6b6560;
  border-style: solid;
}
.cta-corner--tl {
  top: -6rpx;
  left: -6rpx;
  border-width: 4rpx 0 0 4rpx;
}
.cta-corner--br {
  bottom: -6rpx;
  right: -6rpx;
  border-width: 0 4rpx 4rpx 0;
}

/* 朱砂点 */
.cta-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #b5352a;
  flex-shrink: 0;
}

.cta-text {
  color: #4a4640;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
  font-weight: 400;
  letter-spacing: 0.2em;
}

/* ── 预览入口 ── */
.preview-entry {
  margin-top: 40rpx;
  display: flex;
  justify-content: center;
}
.preview-text {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 22rpx;
  font-weight: 300;
  color: #c8c2b8;
  letter-spacing: 0.06em;
  padding: 8rpx 24rpx;
  border: 1rpx solid rgba(200, 194, 184, 0.5);
  border-radius: 4rpx;
}

/* ── 忘记密码 ── */
.forgot {
  text-align: center;
  margin-top: 72rpx;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 22rpx;
  font-weight: 300;
  color: #c8c2b8;
  letter-spacing: 0.06em;
}
</style>
