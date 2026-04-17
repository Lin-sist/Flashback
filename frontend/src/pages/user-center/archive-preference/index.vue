<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { reactive } from 'vue'
import AppTopBar from '../../../components/common/AppTopBar.vue'
import PaperContainer from '../../../components/common/PaperContainer.vue'
import PrimaryButton from '../../../components/common/PrimaryButton.vue'
import { RecordType } from '../../../types'
import { getToken } from '../../../utils'

const STORAGE_KEY = 'flashback:user-center:archive-preference'

interface ArchivePreferenceState {
  defaultRecordType: RecordType
  autoSaveDraft: boolean
  openArchiveAfterSeal: boolean
  listSort: 'NEWEST' | 'OLDEST'
}

const defaultState: ArchivePreferenceState = {
  defaultRecordType: RecordType.FUTURE_LETTER,
  autoSaveDraft: true,
  openArchiveAfterSeal: true,
  listSort: 'NEWEST',
}

const form = reactive<ArchivePreferenceState>({ ...defaultState })

const recordTypeOptions = [
  { label: 'Future Letter', value: RecordType.FUTURE_LETTER },
  { label: 'Node Record', value: RecordType.NODE_RECORD },
  { label: 'Emotion Note', value: RecordType.EMOTION_NOTE },
]

const sortOptions = [
  { label: '按最新优先', value: 'NEWEST' as const },
  { label: '按最早优先', value: 'OLDEST' as const },
]

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

const saveToStorage = () => {
  uni.setStorageSync(STORAGE_KEY, { ...form })
}

const loadFromStorage = () => {
  const raw = uni.getStorageSync(STORAGE_KEY) as Partial<ArchivePreferenceState> | undefined
  if (!raw || typeof raw !== 'object') {
    return
  }

  const defaultRecordType = Object.values(RecordType).includes(raw.defaultRecordType as RecordType)
    ? (raw.defaultRecordType as RecordType)
    : defaultState.defaultRecordType

  const listSort = raw.listSort === 'OLDEST' ? 'OLDEST' : 'NEWEST'

  form.defaultRecordType = defaultRecordType
  form.autoSaveDraft = raw.autoSaveDraft !== undefined ? Boolean(raw.autoSaveDraft) : defaultState.autoSaveDraft
  form.openArchiveAfterSeal = raw.openArchiveAfterSeal !== undefined
    ? Boolean(raw.openArchiveAfterSeal)
    : defaultState.openArchiveAfterSeal
  form.listSort = listSort
}

const onRecordTypeChange = (event: { detail: { value: number } }) => {
  const target = recordTypeOptions[event.detail.value]
  if (!target) {
    return
  }
  form.defaultRecordType = target.value
}

const onSortChange = (event: { detail: { value: number } }) => {
  const target = sortOptions[event.detail.value]
  if (!target) {
    return
  }
  form.listSort = target.value
}

const onSwitchChange = (key: 'autoSaveDraft' | 'openArchiveAfterSeal', event: { detail: { value: boolean } }) => {
  form[key] = Boolean(event.detail.value)
}

const savePreference = () => {
  saveToStorage()
  uni.showToast({ title: '偏好已保存', icon: 'success' })
}

onLoad(() => {
  if (!ensureLogin()) {
    return
  }
  loadFromStorage()
})
</script>

<template>
  <view class="page">
    <AppTopBar title="档案偏好" show-back @back="goBack" />

    <PaperContainer radius="xl" class="section">
      <view class="section-title">默认书写偏好</view>
      <view class="row">
        <view>
          <view class="row-title">默认记录类型</view>
          <view class="row-subtitle">新建页首次进入时默认选中的类型</view>
        </view>
        <picker :range="recordTypeOptions" range-key="label" @change="onRecordTypeChange">
          <view class="value-pill">{{ form.defaultRecordType }}</view>
        </picker>
      </view>
      <view class="row">
        <view>
          <view class="row-title">草稿自动保存提示</view>
          <view class="row-subtitle">离开编辑页前自动尝试保存草稿</view>
        </view>
        <switch :checked="form.autoSaveDraft" color="#3b647a" @change="(event) => onSwitchChange('autoSaveDraft', event as { detail: { value: boolean } })" />
      </view>
    </PaperContainer>

    <PaperContainer radius="xl" class="section">
      <view class="section-title">档案浏览习惯</view>
      <view class="row">
        <view>
          <view class="row-title">封存后快捷跳转</view>
          <view class="row-subtitle">完成封存后优先回到我的档案</view>
        </view>
        <switch :checked="form.openArchiveAfterSeal" color="#3b647a" @change="(event) => onSwitchChange('openArchiveAfterSeal', event as { detail: { value: boolean } })" />
      </view>
      <view class="row">
        <view>
          <view class="row-title">档案默认排序</view>
          <view class="row-subtitle">列表的默认展示顺序</view>
        </view>
        <picker :range="sortOptions" range-key="label" @change="onSortChange">
          <view class="value-pill">{{ form.listSort === 'NEWEST' ? '按最新优先' : '按最早优先' }}</view>
        </picker>
      </view>
    </PaperContainer>

    <view class="footer-tip">当前设置仅保存在本机，后续将支持账号级同步。</view>
    <PrimaryButton text="保存偏好" @tap="savePreference" />
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

.value-pill {
  min-width: 180rpx;
  padding: 8rpx 14rpx;
  border-radius: var(--fb-radius-pill);
  background: #edf3f6;
  text-align: center;
  color: var(--fb-color-primary);
  font-size: var(--fb-font-meta);
}

.footer-tip {
  margin: 24rpx 0 18rpx;
  text-align: center;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}
</style>
