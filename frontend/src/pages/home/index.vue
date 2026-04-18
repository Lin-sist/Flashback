<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import BottomNavBar from '../../components/common/BottomNavBar.vue'
import { recordService } from '../../services'
import { RecordStatus, type DateTimeValue, type RecordListItemVO } from '../../types'
import { formatDateTime, getToken } from '../../utils'

type SectionState = 'idle' | 'loading' | 'ready' | 'error'

const loading = ref(false)

const draftCount = ref(0)
const sealedCount = ref(0)
const unlockedCount = ref(0)

const latestDraft = ref<RecordListItemVO | null>(null)
const latestUnlocked = ref<RecordListItemVO | null>(null)

const draftState = ref<SectionState>('idle')
const sealedState = ref<SectionState>('idle')
const unlockedState = ref<SectionState>('idle')

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const formatMonthDay = (value?: DateTimeValue) => {
  if (value === undefined || value === null) {
    return '尚未解封'
  }

  const normalized = typeof value === 'string' && !value.includes('T') ? value.replace(' ', 'T') : value
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return '尚未解封'
  }

  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const totalArchiveCount = computed(() => draftCount.value + sealedCount.value + unlockedCount.value)
const allSectionsFailed = computed(
  () =>
    draftState.value === 'error' &&
    sealedState.value === 'error' &&
    unlockedState.value === 'error'
)

const draftTitle = computed(() => {
  if (draftState.value === 'loading' && !latestDraft.value) {
    return '正在整理你的草稿'
  }

  if (draftState.value === 'error' && !latestDraft.value) {
    return '草稿入口暂时不可用'
  }

  return latestDraft.value?.title?.trim() || '从这里写下新的篇章'
})

const draftChipText = computed(() => {
  if (draftState.value === 'error') {
    return '轻触重新同步草稿入口'
  }

  if (draftState.value === 'loading' && !latestDraft.value) {
    return '正在同步草稿入口'
  }

  if (draftCount.value > 0) {
    return `当前草稿 ${draftCount.value} 条`
  }

  return '还没有草稿，点这里开始'
})

const draftMeta = computed(() => {
  if (!latestDraft.value) {
    return draftState.value === 'error' ? '保留现有入口，重试后可继续编辑最近草稿' : '继续完成此篇章'
  }

  return `最近更新 ${formatDateTime(latestDraft.value.createdAt)}`
})

const sealedValue = computed(() => {
  if (sealedState.value === 'loading' && sealedCount.value === 0) {
    return '··'
  }

  if (sealedState.value === 'error' && sealedCount.value === 0) {
    return '--'
  }

  return String(sealedCount.value)
})

const sealedLabel = computed(() => {
  if (sealedState.value === 'error') {
    return '封存摘要同步失败'
  }

  return '封存记录总数'
})

const sealedMeta = computed(() => {
  if (sealedState.value === 'error') {
    return '轻触卡片重新加载'
  }

  if (sealedState.value === 'loading' && sealedCount.value === 0) {
    return '正在对齐封存摘要'
  }

  if (sealedCount.value <= 0) {
    return '完成封存后，会在这里累计'
  }

  return '进入我的档案查看等待解锁的记忆'
})

const unlockValue = computed(() => {
  if (unlockedState.value === 'loading' && !latestUnlocked.value) {
    return '同步中'
  }

  if (unlockedState.value === 'error' && !latestUnlocked.value) {
    return '暂不可用'
  }

  if (!latestUnlocked.value) {
    return '尚未解封'
  }

  return formatMonthDay(latestUnlocked.value.unlockAt || latestUnlocked.value.createdAt)
})

const unlockTitle = computed(() => {
  if (unlockedState.value === 'error' && !latestUnlocked.value) {
    return '最近解锁暂时不可用'
  }

  return latestUnlocked.value?.title?.trim() || '第一封解锁的记忆，会在这里出现'
})

const unlockMeta = computed(() => {
  if (unlockedState.value === 'error') {
    return '轻触卡片重新加载'
  }

  if (unlockedState.value === 'loading' && !latestUnlocked.value) {
    return '正在同步最近解锁'
  }

  if (!latestUnlocked.value) {
    return '写下新的记忆，等待它抵达未来'
  }

  return `进入详情页查看原文 · ${formatDateTime(latestUnlocked.value.createdAt)}`
})

const archiveMeta = computed(() => {
  if (allSectionsFailed.value) {
    return '摘要同步失败后，仍可从这里进入真实档案页'
  }

  if (loading.value && totalArchiveCount.value === 0) {
    return '正在同步属于你的时光印记'
  }

  if (totalArchiveCount.value > 0) {
    return `当前共 ${totalArchiveCount.value} 条记录，进入列表检索与管理`
  }

  return '管理所有属于你的时光印记'
})

const loadHomeSummary = async () => {
  if (!ensureLogin()) {
    return
  }

  loading.value = true

  if (draftState.value !== 'ready') {
    draftState.value = 'loading'
  }
  if (sealedState.value !== 'ready') {
    sealedState.value = 'loading'
  }
  if (unlockedState.value !== 'ready') {
    unlockedState.value = 'loading'
  }

  const [draftResult, sealedResult, unlockedResult] = await Promise.allSettled([
    recordService.getRecordList(RecordStatus.DRAFT, { pageNum: 1, pageSize: 1 }),
    recordService.getRecordList(RecordStatus.SEALED, { pageNum: 1, pageSize: 1 }),
    recordService.getUnlockedRecords(1, 1),
  ])

  if (draftResult.status === 'fulfilled') {
    draftCount.value = draftResult.value.total
    latestDraft.value = draftResult.value.list[0] || null
    draftState.value = 'ready'
  } else {
    draftCount.value = 0
    latestDraft.value = null
    draftState.value = 'error'
  }

  if (sealedResult.status === 'fulfilled') {
    sealedCount.value = sealedResult.value.total
    sealedState.value = 'ready'
  } else {
    sealedCount.value = 0
    sealedState.value = 'error'
  }

  if (unlockedResult.status === 'fulfilled') {
    unlockedCount.value = unlockedResult.value.total
    latestUnlocked.value = unlockedResult.value.list[0] || null
    unlockedState.value = 'ready'
  } else {
    unlockedCount.value = 0
    latestUnlocked.value = null
    unlockedState.value = 'error'
  }

  loading.value = false
}

const retryHomeSummary = () => {
  loadHomeSummary()
  uni.showToast({ title: '正在重新同步首页内容', icon: 'none' })
}

const goEditor = () => uni.navigateTo({ url: '/pages/record-editor/index?source=home' })
const goArchive = () => uni.navigateTo({ url: '/pages/record-list/index' })

const goDraftEntry = () => {
  if (draftState.value === 'loading' && !latestDraft.value) {
    uni.showToast({ title: '草稿同步中，请稍后再试', icon: 'none' })
    return
  }

  if (draftState.value === 'error') {
    retryHomeSummary()
    return
  }

  if (!latestDraft.value) {
    goEditor()
    return
  }

  uni.navigateTo({
    url: `/pages/record-editor/index?id=${latestDraft.value.id}&source=home`,
  })
}

const onSealedSummaryTap = () => {
  if (sealedState.value === 'error') {
    retryHomeSummary()
    return
  }

  goArchive()
}

const goLatestUnlocked = () => {
  if (unlockedState.value === 'loading' && !latestUnlocked.value) {
    uni.showToast({ title: '最近解锁同步中，请稍后再试', icon: 'none' })
    return
  }

  if (unlockedState.value === 'error') {
    retryHomeSummary()
    return
  }

  if (!latestUnlocked.value) {
    uni.showToast({ title: '还没有解锁记录', icon: 'none' })
    return
  }

  uni.navigateTo({
    url: `/pages/record-detail/index?id=${latestUnlocked.value.id}&source=home`,
  })
}

onShow(() => {
  uni.hideTabBar({ animation: false })
  loadHomeSummary()
})
</script>

<template>
  <view class="page">
    <view class="top-bar">
      <view class="top-bar-side" />
      <view class="brand">时光回序</view>
      <view class="top-bar-side right" @tap="goArchive">
        <view class="icon icon-history" />
      </view>
    </view>

    <view class="hero">
      <view class="hero-title">那些被封存的<br />碎片</view>
      <view class="hero-subtitle">在时间的灰烬里，寻回那些不曾褪色的真实片段。</view>
    </view>

    <view class="draft-card" :class="{ 'is-error': draftState === 'error' }" @tap="goDraftEntry">
      <view class="draft-head">
        <text class="kicker">DRAFT ENTRY</text>
        <view class="icon icon-edit" />
      </view>
      <view class="draft-title">{{ draftTitle }}</view>
      <view class="draft-meta">{{ draftMeta }}</view>
      <view class="draft-chip" :class="{ 'draft-chip-error': draftState === 'error' }">
        <view class="icon icon-info" />
        <text class="draft-chip-text">{{ draftChipText }}</text>
      </view>
    </view>

    <view class="summary-row">
      <view
        class="summary-card summary-card-cool"
        :class="{ 'is-error': sealedState === 'error' }"
        @tap="onSealedSummaryTap"
      >
        <view class="icon icon-archive" />
        <view class="summary-value">{{ sealedValue }}</view>
        <view class="summary-label">{{ sealedLabel }}</view>
        <view class="summary-meta">{{ sealedMeta }}</view>
      </view>

      <view
        class="summary-card summary-card-warm"
        :class="{ 'is-error': unlockedState === 'error' }"
        @tap="goLatestUnlocked"
      >
        <view class="icon icon-lock" />
        <view class="summary-value summary-value-date">{{ unlockValue }}</view>
        <view class="summary-label">最近解封的记忆</view>
        <view class="summary-meta summary-meta-warm">{{ unlockTitle }}</view>
      </view>
    </view>

    <view class="summary-inline-tip" :class="{ 'is-muted': unlockedState !== 'error' }">
      {{ unlockMeta }}
    </view>

    <view class="archive-entry" @tap="goArchive">
      <view class="archive-icon-wrap">
        <view class="icon icon-folder" />
      </view>
      <view class="archive-text">
        <view class="archive-title">我的档案</view>
        <view class="archive-meta">{{ archiveMeta }}</view>
      </view>
      <text class="archive-arrow">›</text>
    </view>

    <view v-if="allSectionsFailed" class="state-panel">
      <view class="state-kicker">HOME STATUS</view>
      <view class="state-title">首页摘要暂时没有同步成功</view>
      <view class="state-desc">登录校验与真实页面入口仍然保留，你可以重试同步，或先进入我的档案继续操作。</view>
      <view class="state-action" @tap="retryHomeSummary">重新同步</view>
    </view>

    <view class="scene-wrap">
      <image class="scene-image" src="/static/home-scene.jpg" mode="aspectFill" />
    </view>

    <view class="fab" @tap="goEditor">
      <text class="fab-plus">+</text>
    </view>

    <view v-if="loading && !allSectionsFailed" class="sync-pill">正在同步首页内容</view>

    <BottomNavBar current="home" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 40rpx 260rpx;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0) 36%),
    linear-gradient(180deg, #f7f9fb 0%, #f3f6f8 100%);
}

.top-bar {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8rpx;
}

.top-bar-side {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.brand {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 500;
  letter-spacing: 4rpx;
  color: var(--fb-color-primary);
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.hero {
  margin-top: 40rpx;
}

.hero-title {
  font-size: 76rpx;
  line-height: 1.15;
  font-weight: 700;
  color: #111418;
  letter-spacing: 2rpx;
}

.hero-subtitle {
  margin-top: 28rpx;
  font-size: 28rpx;
  line-height: 1.75;
  color: #9aa3a9;
  max-width: 540rpx;
}

.draft-card {
  margin-top: 64rpx;
  padding: 36rpx 36rpx 32rpx;
  border-radius: 36rpx;
  background: #eef2f5;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 18rpx 40rpx rgba(72, 95, 111, 0.08);
}

.draft-card.is-error,
.summary-card.is-error,
.archive-entry.is-error {
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.55) inset,
    0 18rpx 40rpx rgba(108, 118, 126, 0.12);
}

.draft-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kicker {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: #9aa3a9;
  font-weight: 500;
}

.draft-title {
  margin-top: 14rpx;
  font-size: 42rpx;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 1rpx;
  line-height: 1.35;
}

.draft-meta {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #7f8c93;
  line-height: 1.7;
}

.draft-chip {
  margin-top: 28rpx;
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.82);
}

.draft-chip-error {
  background: rgba(255, 255, 255, 0.62);
}

.draft-chip-text {
  font-size: 26rpx;
  color: #6f7a80;
}

.summary-row {
  margin-top: 24rpx;
  display: flex;
  gap: 20rpx;
}

.summary-card {
  flex: 1;
  min-height: 280rpx;
  padding: 28rpx 28rpx 24rpx;
  border-radius: 32rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 18rpx 40rpx rgba(72, 95, 111, 0.08);
}

.summary-card-cool {
  background: #eef2f5;
}

.summary-card-warm {
  background: linear-gradient(180deg, #f6dca9 0%, #f1ca87 100%);
}

.summary-value {
  margin-top: 18rpx;
  font-size: 80rpx;
  line-height: 1;
  font-weight: 300;
  color: #1a1a1a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.summary-value-date {
  font-size: 44rpx;
  font-weight: 500;
  letter-spacing: 2rpx;
  line-height: 1.25;
}

.summary-label {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #6f7a80;
}

.summary-card-warm .summary-label {
  color: #765f2c;
}

.summary-meta {
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #8a959c;
}

.summary-meta-warm {
  color: #6e5928;
}

.summary-inline-tip {
  margin-top: 12rpx;
  padding: 0 8rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #896f38;
}

.summary-inline-tip.is-muted {
  color: #8f989e;
}

.archive-entry {
  margin-top: 24rpx;
  padding: 24rpx 28rpx;
  border-radius: 32rpx;
  background: #eef2f5;
  display: flex;
  align-items: center;
  gap: 24rpx;
  box-shadow: 0 18rpx 40rpx rgba(72, 95, 111, 0.08);
}

.archive-icon-wrap {
  width: 76rpx;
  height: 76rpx;
  border-radius: 999rpx;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.archive-text {
  flex: 1;
  min-width: 0;
}

.archive-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.archive-meta {
  margin-top: 6rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #9aa3a9;
}

.archive-arrow {
  font-size: 44rpx;
  color: #9aa3a9;
  line-height: 1;
  padding-right: 8rpx;
}

.state-panel {
  margin-top: 24rpx;
  padding: 28rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 18rpx 40rpx rgba(72, 95, 111, 0.08);
  backdrop-filter: blur(10rpx);
}

.state-kicker {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: #9aa3a9;
}

.state-title {
  margin-top: 10rpx;
  font-size: 34rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.state-desc {
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: #7f8c93;
}

.state-action {
  margin-top: 20rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  padding: 0 30rpx;
  border-radius: 999rpx;
  background: #3a4a55;
  color: #ffffff;
  font-size: 26rpx;
}

.scene-wrap {
  margin-top: 28rpx;
  border-radius: 32rpx;
  overflow: hidden;
  background: #e8ecef;
  box-shadow: 0 18rpx 40rpx rgba(72, 95, 111, 0.08);
}

.scene-image {
  width: 100%;
  height: 360rpx;
  display: block;
}

.fab {
  position: fixed;
  right: 56rpx;
  bottom: calc(env(safe-area-inset-bottom) + 160rpx);
  width: 108rpx;
  height: 108rpx;
  border-radius: 999rpx;
  background: #3a4a55;
  box-shadow: 0 12rpx 28rpx rgba(58, 74, 85, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 90;
}

.fab-plus {
  color: #ffffff;
  font-size: 60rpx;
  line-height: 1;
  font-weight: 300;
  margin-top: -4rpx;
}

.sync-pill {
  position: fixed;
  left: 50%;
  bottom: calc(env(safe-area-inset-bottom) + 132rpx);
  transform: translateX(-50%);
  max-width: calc(100vw - 120rpx);
  padding: 14rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12rpx 28rpx rgba(58, 74, 85, 0.12);
  color: #6f7a80;
  font-size: 24rpx;
  z-index: 85;
}

.icon {
  width: 36rpx;
  height: 36rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-history {
  width: 40rpx;
  height: 40rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><path d='M3 12a9 9 0 1 0 3-6.7'/><polyline points='3 4 3 9 8 9'/><polyline points='12 7 12 12 15 14'/></svg>");
}

.icon-edit {
  width: 40rpx;
  height: 40rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%235a6870' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M4 20h4l10-10-4-4L4 16v4z'/><path d='M14 6l4 4'/><line x1='4' y1='20' x2='12' y2='20'/></svg>");
}

.icon-info {
  width: 32rpx;
  height: 32rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%235a6870'><circle cx='12' cy='12' r='10'/><circle cx='12' cy='8' r='1.3' fill='white'/><rect x='11' y='11' width='2' height='7' rx='1' fill='white'/></svg>");
}

.icon-archive {
  width: 44rpx;
  height: 44rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='4' width='18' height='4' rx='1'/><path d='M5 8v11a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8'/><line x1='10' y1='13' x2='14' y2='13'/></svg>");
}

.icon-lock {
  width: 44rpx;
  height: 44rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%237a5a20' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><rect x='5' y='11' width='14' height='10' rx='2'/><path d='M8 11V7a4 4 0 0 1 8 0v4'/></svg>");
}

.icon-folder {
  width: 42rpx;
  height: 42rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M3 6a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'/><circle cx='15' cy='13' r='2.4'/><path d='M15 9.8v1.2M15 15v1.2M18.2 13h-1.2M13 13h-1.2M17.3 10.7l-0.85 0.85M13.55 14.45l-0.85 0.85M17.3 15.3l-0.85-0.85M13.55 11.55l-0.85-0.85'/></svg>");
}
</style>
