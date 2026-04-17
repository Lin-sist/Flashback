<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { reactive } from 'vue'
import AppTopBar from '../../../components/common/AppTopBar.vue'
import PaperContainer from '../../../components/common/PaperContainer.vue'
import PrimaryButton from '../../../components/common/PrimaryButton.vue'
import { useUserStore } from '../../../stores'
import { getToken } from '../../../utils'

const STORAGE_KEY_PREFIX = 'flashback:user-center:notify-settings'
const userStore = useUserStore()

interface NotifySettingsState {
  unlockReminder: boolean
  dailyReviewReminder: boolean
  emailReminder: boolean
  privacyMaskEmail: boolean
  profileSearchable: boolean
}

const defaultState: NotifySettingsState = {
  unlockReminder: true,
  dailyReviewReminder: false,
  emailReminder: false,
  privacyMaskEmail: true,
  profileSearchable: false,
}

const form = reactive<NotifySettingsState>({ ...defaultState })
let storageKey = `${STORAGE_KEY_PREFIX}:guest`

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const goBack = () => {
  uni.navigateBack({
    delta: 1,
    fail: () => {
      uni.switchTab({ url: '/pages/user-center/index' })
    },
  })
}

const resolveStorageKey = async () => {
  const user = userStore.userInfo || await userStore.fetchUserInfo().catch(() => null)

  if (user?.id) {
    return `${STORAGE_KEY_PREFIX}:uid:${user.id}`
  }

  if (user?.username) {
    return `${STORAGE_KEY_PREFIX}:uname:${user.username}`
  }

  if (user?.email) {
    return `${STORAGE_KEY_PREFIX}:email:${user.email}`
  }

  const token = getToken()
  if (token) {
    return `${STORAGE_KEY_PREFIX}:token:${token.slice(0, 16)}`
  }

  return `${STORAGE_KEY_PREFIX}:guest`
}

const saveToStorage = () => {
  uni.setStorageSync(storageKey, { ...form })
}

const loadFromStorage = () => {
  const raw = uni.getStorageSync(storageKey) as Partial<NotifySettingsState> | undefined
  if (!raw || typeof raw !== 'object') {
    return
  }

  form.unlockReminder = raw.unlockReminder !== undefined ? Boolean(raw.unlockReminder) : defaultState.unlockReminder
  form.dailyReviewReminder = raw.dailyReviewReminder !== undefined
    ? Boolean(raw.dailyReviewReminder)
    : defaultState.dailyReviewReminder
  form.emailReminder = raw.emailReminder !== undefined ? Boolean(raw.emailReminder) : defaultState.emailReminder
  form.privacyMaskEmail = raw.privacyMaskEmail !== undefined
    ? Boolean(raw.privacyMaskEmail)
    : defaultState.privacyMaskEmail
  form.profileSearchable = raw.profileSearchable !== undefined
    ? Boolean(raw.profileSearchable)
    : defaultState.profileSearchable
}

const onSwitchChange = (key: keyof NotifySettingsState, event: { detail: { value: boolean } }) => {
  form[key] = Boolean(event.detail.value)
}

const saveSettings = () => {
  saveToStorage()
  uni.showToast({ title: '设置已保存', icon: 'success' })
}

onLoad(async () => {
  if (!ensureLogin()) {
    return
  }

  storageKey = await resolveStorageKey()
  loadFromStorage()
})
</script>

<template>
  <view class="page">
    <AppTopBar title="提醒设置" show-back @back="goBack" />

    <PaperContainer radius="xl" class="section">
      <view class="section-title">解锁提醒</view>
      <view class="row">
        <view>
          <view class="row-title">解封当日提醒</view>
          <view class="row-subtitle">记录到达解封时间时提醒你回看</view>
        </view>
        <switch :checked="form.unlockReminder" color="#3b647a" @change="(event) => onSwitchChange('unlockReminder', event as { detail: { value: boolean } })" />
      </view>
      <view class="row">
        <view>
          <view class="row-title">每日回顾提醒</view>
          <view class="row-subtitle">每天固定时段提醒回顾已解锁内容</view>
        </view>
        <switch :checked="form.dailyReviewReminder" color="#3b647a" @change="(event) => onSwitchChange('dailyReviewReminder', event as { detail: { value: boolean } })" />
      </view>
      <view class="row">
        <view>
          <view class="row-title">邮件提醒（占位）</view>
          <view class="row-subtitle">后续将支持邮箱通知通道</view>
        </view>
        <switch :checked="form.emailReminder" color="#3b647a" @change="(event) => onSwitchChange('emailReminder', event as { detail: { value: boolean } })" />
      </view>
    </PaperContainer>

    <PaperContainer radius="xl" class="section">
      <view class="section-title">隐私与安全</view>
      <view class="row">
        <view>
          <view class="row-title">个人页隐藏邮箱</view>
          <view class="row-subtitle">在展示资料中默认不暴露邮箱信息</view>
        </view>
        <switch :checked="form.privacyMaskEmail" color="#3b647a" @change="(event) => onSwitchChange('privacyMaskEmail', event as { detail: { value: boolean } })" />
      </view>
      <view class="row">
        <view>
          <view class="row-title">允许被搜索（占位）</view>
          <view class="row-subtitle">后续账号体系完善后生效</view>
        </view>
        <switch :checked="form.profileSearchable" color="#3b647a" @change="(event) => onSwitchChange('profileSearchable', event as { detail: { value: boolean } })" />
      </view>
    </PaperContainer>

    <view class="footer-tip">当前设置仅保存在本机，后续将与账号配置同步。</view>
    <PrimaryButton text="保存设置" @tap="saveSettings" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx var(--fb-space-page) 40rpx;
  background: var(--fb-color-bg);
}

.section {
  margin-top: 18rpx;
}

.section-title {
  font-size: var(--fb-font-body-sub);
  color: var(--fb-color-text-muted);
  margin-bottom: 8rpx;
}

.row {
  min-height: 108rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #eef2f4;
}

.row:last-child {
  border-bottom: none;
}

.row-title {
  font-size: var(--fb-font-body);
  color: var(--fb-color-text);
}

.row-subtitle {
  margin-top: 6rpx;
  font-size: var(--fb-font-meta);
  color: var(--fb-color-text-muted);
}

.footer-tip {
  margin: 24rpx 0 18rpx;
  text-align: center;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}
</style>
