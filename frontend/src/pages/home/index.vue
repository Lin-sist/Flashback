<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import AppPageShell from '../../components/common/AppPageShell.vue'
import { recordService } from '../../services'
import { RecordStatus, type RecordListItemVO } from '../../types'
import { hasAuthenticatedSession } from '../../utils'

type SectionState = 'idle' | 'loading' | 'ready' | 'error'

const draftCount = ref(0)
const sealedCount = ref(0)
const unlockedCount = ref(0)

const latestDraft = ref<RecordListItemVO | null>(null)
const latestUnlocked = ref<RecordListItemVO | null>(null)

const draftState = ref<SectionState>('idle')
const sealedState = ref<SectionState>('idle')
const unlockedState = ref<SectionState>('idle')

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const statusCountText = (state: SectionState, count: number) => {
  if (state === 'loading' && count === 0) {
    return '··'
  }

  if (state === 'error' && count === 0) {
    return '--'
  }

  return String(count)
}

const draftDisplayCount = computed(() => statusCountText(draftState.value, draftCount.value))
const sealedDisplayCount = computed(() => statusCountText(sealedState.value, sealedCount.value))
const unlockedDisplayCount = computed(() =>
  statusCountText(unlockedState.value, unlockedCount.value)
)

const loadHomeSummary = async () => {
  if (!ensureLogin()) {
    return
  }

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
  <AppPageShell
    class="page"
    title="时光回序"
    current="home"
    padding-x="36rpx"
    content-bottom="202rpx"
  >
    <template #top-title>
      <text class="brand-title">时光回序</text>
    </template>

    <view class="hero">
      <view class="hero-title">
        <text>今天的你，</text>
        <text>想留下些什么？</text>
      </view>
      <view class="hero-subtitle">
        <text>你写下的东西，会被好好保存。</text>
        <text>后来的你，会有机会重新读懂它。</text>
      </view>
    </view>

    <view class="write-card">
      <view class="write-card-title">写下此刻</view>
      <view class="write-card-rule" />
      <view class="write-card-desc">
        <text>把今天的心情、困惑或期待，</text>
        <text>先好好放在这里。</text>
      </view>
      <view class="write-button" @tap="goEditor">
        <view class="icon icon-edit" />
        <text>开始书写</text>
      </view>
      <view class="paper-tail">
        <view class="paper-tail-line" />
        <view class="paper-tail-dot" />
        <view class="paper-tail-line" />
      </view>
    </view>

    <view class="status-panel">
      <view class="status-item" :class="{ 'is-error': draftState === 'error' }" @tap="goDraftEntry">
        <view class="status-topline">
          <view class="icon icon-draft" />
          <text class="status-count">{{ draftDisplayCount }}</text>
        </view>
        <text class="status-label">未写完</text>
      </view>

      <view class="status-divider" />

      <view class="status-item" :class="{ 'is-error': sealedState === 'error' }" @tap="onSealedSummaryTap">
        <view class="status-topline">
          <view class="icon icon-hourglass" />
          <text class="status-count">{{ sealedDisplayCount }}</text>
        </view>
        <text class="status-label">保存中</text>
      </view>

      <view class="status-divider" />

      <view
        class="status-item"
        :class="{ 'is-error': unlockedState === 'error' }"
        @tap="goLatestUnlocked"
      >
        <view class="status-topline">
          <view class="icon icon-arrived" />
          <text class="status-count">{{ unlockedDisplayCount }}</text>
        </view>
        <text class="status-label">已抵达</text>
      </view>
    </view>
  </AppPageShell>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f8f9fa;
}

.brand-title {
  max-width: 100%;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 34rpx;
  font-weight: 400;
  letter-spacing: 8rpx;
  color: #1e2327;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.hero {
  margin-top: 100rpx;
  text-align: center;
}

.hero-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 58rpx;
  line-height: 1.5;
  font-weight: 400;
  color: #111518;
  letter-spacing: 0;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.hero-subtitle {
  margin-top: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  font-size: 28rpx;
  line-height: 1.6;
  color: #7f8c93;
  letter-spacing: 0;
}

.write-card {
  margin-top: 82rpx;
  min-height: 648rpx;
  padding: 84rpx 44rpx 50rpx;
  border-radius: 42rpx;
  border: 1rpx solid rgba(218, 224, 227, 0.58);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, #ffffff 44%, #fefefe 100%);
  box-shadow:
    0 1rpx 0 rgba(255, 255, 255, 0.9) inset,
    0 20rpx 42rpx rgba(74, 88, 98, 0.065),
    0 44rpx 88rpx rgba(86, 98, 108, 0.055);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.write-card-title {
  font-size: 46rpx;
  line-height: 1.3;
  font-weight: 400;
  color: #171b1f;
  letter-spacing: 4rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.write-card-rule {
  width: 72rpx;
  height: 1rpx;
  margin-top: 60rpx;
  background: rgba(205, 211, 214, 0.42);
}

.write-card-desc {
  margin-top: 66rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 28rpx;
  line-height: 1.9;
  color: #858e94;
  letter-spacing: 1rpx;
  font-style: italic;
}

.write-button {
  margin-top: 84rpx;
  width: 294rpx;
  height: 86rpx;
  border-radius: 22rpx;
  background: #708490;
  box-shadow: 0 14rpx 26rpx rgba(112, 132, 144, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 26rpx;
  color: #ffffff;
  font-size: 28rpx;
  line-height: 1.2;
  letter-spacing: 8rpx;
}

.paper-tail {
  margin-top: 88rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.paper-tail-line {
  width: 42rpx;
  height: 1rpx;
  background: rgba(221, 213, 199, 0.62);
}

.paper-tail-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: rgba(221, 213, 199, 0.72);
}

.status-panel {
  margin-top: 90rpx;
  min-height: 146rpx;
  border-radius: 28rpx;
  border: 1rpx solid rgba(223, 227, 230, 0.68);
  background:
    linear-gradient(rgba(178, 185, 190, 0.05) 1rpx, transparent 1rpx),
    linear-gradient(90deg, rgba(178, 185, 190, 0.045) 1rpx, transparent 1rpx),
    rgba(255, 255, 255, 0.66);
  background-size: 26rpx 26rpx;
  box-shadow: 0 12rpx 26rpx rgba(84, 98, 108, 0.032);
  display: flex;
  align-items: center;
}

.status-item {
  flex: 1;
  min-width: 0;
  height: 146rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.status-item.is-error .status-count {
  color: #9a8580;
}

.status-topline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  min-height: 40rpx;
}

.status-count {
  color: #a8b0b6;
  font-size: 24rpx;
  line-height: 1;
  font-weight: 400;
}

.status-label {
  margin-top: 22rpx;
  color: #7f8990;
  font-size: 26rpx;
  line-height: 1;
}

.status-divider {
  width: 1rpx;
  height: 50rpx;
  background: rgba(213, 218, 221, 0.58);
}

.icon {
  width: 36rpx;
  height: 36rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-edit {
  width: 34rpx;
  height: 34rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ffffff' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><path d='M4 20h4l10-10-4-4L4 16v4z'/><path d='M14 6l4 4'/><line x1='4' y1='20' x2='12' y2='20'/></svg>");
}

.icon-draft {
  width: 34rpx;
  height: 34rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa4ab' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M4 6.5A2.5 2.5 0 0 1 6.5 4H10l2 2h5.5A2.5 2.5 0 0 1 20 8.5v8A2.5 2.5 0 0 1 17.5 19h-11A2.5 2.5 0 0 1 4 16.5z'/></svg>");
}

.icon-hourglass {
  width: 34rpx;
  height: 34rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa4ab' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M6 3h12'/><path d='M6 21h12'/><path d='M8 3v4.5c0 1.3.7 2.5 1.8 3.2L12 12l2.2-1.3A3.8 3.8 0 0 0 16 7.5V3'/><path d='M8 21v-4.5c0-1.3.7-2.5 1.8-3.2L12 12l2.2 1.3a3.8 3.8 0 0 1 1.8 3.2V21'/></svg>");
}

.icon-arrived {
  width: 34rpx;
  height: 34rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa4ab' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M4 7.5 12 13l8-5.5'/><path d='M4 7.5v9A1.5 1.5 0 0 0 5.5 18h13a1.5 1.5 0 0 0 1.5-1.5v-9'/><path d='M4 7.5 12 3l8 4.5'/></svg>");
}
</style>
