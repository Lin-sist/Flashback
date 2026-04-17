<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import AppTopBar from '../../components/common/AppTopBar.vue'
import BottomNavBar from '../../components/common/BottomNavBar.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import FloatingActionButton from '../../components/common/FloatingActionButton.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import { recordService } from '../../services'
import { RecordStatus, type RecordListItemVO } from '../../types'
import { formatDateTime, getToken } from '../../utils'

const loading = ref(false)
const draftCount = ref(0)
const sealedCount = ref(0)
const latestDraft = ref<RecordListItemVO | null>(null)
const latestUnlocked = ref<RecordListItemVO | null>(null)
const draftLoaded = ref(false)
const draftLoadFailed = ref(false)
const homeLoadFailed = ref(false)
const summaryLoadFailed = ref(false)
const latestUnlockedLoadFailed = ref(false)

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadHomeSummary = async () => {
  if (!ensureLogin()) {
    return
  }

  loading.value = true
  draftLoadFailed.value = false
  homeLoadFailed.value = false
  summaryLoadFailed.value = false
  latestUnlockedLoadFailed.value = false

  const [draftResult, sealedResult, unlockedResult] = await Promise.allSettled([
    recordService.getRecordList(RecordStatus.DRAFT, { pageNum: 1, pageSize: 1 }),
    recordService.getRecordList(RecordStatus.SEALED, { pageNum: 1, pageSize: 1 }),
    recordService.getUnlockedRecords(1, 1),
  ])

  if (draftResult.status === 'fulfilled') {
    draftCount.value = draftResult.value.total
    latestDraft.value = draftResult.value.list[0] || null
    draftLoaded.value = true
    draftLoadFailed.value = false
  } else {
    draftLoaded.value = false
    draftLoadFailed.value = true
    latestDraft.value = null
  }

  if (sealedResult.status === 'fulfilled') {
    sealedCount.value = sealedResult.value.total
    summaryLoadFailed.value = false
  } else {
    sealedCount.value = 0
    summaryLoadFailed.value = true
  }

  if (unlockedResult.status === 'fulfilled') {
    latestUnlocked.value = unlockedResult.value.list[0] || null
    latestUnlockedLoadFailed.value = false
  } else {
    latestUnlocked.value = null
    latestUnlockedLoadFailed.value = true
  }

  homeLoadFailed.value =
    draftResult.status === 'rejected' &&
    sealedResult.status === 'rejected' &&
    unlockedResult.status === 'rejected'

  loading.value = false
}

const retryHomeSummary = () => {
  loadHomeSummary()
  uni.showToast({ title: '正在重试加载', icon: 'none' })
}

const goEditor = () => uni.navigateTo({ url: '/pages/record-editor/index?source=home' })
const goArchive = () => uni.navigateTo({ url: '/pages/record-list/index' })

const goDraftEntry = () => {
  if (loading.value) {
    uni.showToast({ title: '草稿同步中，请稍后再试', icon: 'none' })
    return
  }

  if (draftLoadFailed.value || !draftLoaded.value) {
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

const buildSealedTipText = () => {
  if (summaryLoadFailed.value) {
    return '摘要暂时不可用，请稍后重试'
  }

  if (loading.value) {
    return '摘要同步中，请稍后再试'
  }

  if (sealedCount.value <= 0) {
    return '还没有封存记录'
  }

  return `已封存 ${sealedCount.value} 条记录，请前往我的档案查看解封时间`
}

const onSealedSummaryTap = () => {
  if (summaryLoadFailed.value) {
    retryHomeSummary()
    return
  }

  uni.showToast({
    title: buildSealedTipText(),
    icon: 'none',
  })
}

const goLatestUnlocked = () => {
  if (latestUnlockedLoadFailed.value) {
    retryHomeSummary()
    return
  }

  if (!latestUnlocked.value) {
    uni.showToast({ title: '还没有解锁记录', icon: 'none' })
    return
  }

  uni.navigateTo({ url: `/pages/record-detail/index?id=${latestUnlocked.value.id}&source=home` })
}

onShow(() => {
  uni.hideTabBar({ animation: false })
  loadHomeSummary()
})
</script>

<template>
  <view class="page">
    <AppTopBar title="Flashback" right-text="历史" @right-tap="goArchive" />

    <view class="hero-block">
      <view class="hero-title">你写下的每一刻，都在时间里慢慢发光</view>
      <view class="hero-subtitle">安静地封存，温柔地等待，直到适合重读的那一天。</view>
    </view>

    <view class="section-list">
      <PaperContainer radius="xl" class="draft-card" @tap="goDraftEntry">
        <view class="card-kicker">草稿入口</view>
        <view class="card-title">继续书写你的这一卷</view>
        <view class="card-meta">{{ draftLoadFailed ? '草稿暂时不可用，点按重试' : `当前草稿 ${draftCount} 条` }}</view>
      </PaperContainer>

      <PaperContainer radius="xl" class="summary-card" @tap="onSealedSummaryTap">
        <view class="card-kicker">封存摘要</view>
        <view class="stat-value">{{ summaryLoadFailed ? '--' : sealedCount }}</view>
        <view class="card-meta">{{ summaryLoadFailed ? '摘要加载失败，点按可重试' : '已封存记录' }}</view>
      </PaperContainer>

      <PaperContainer radius="xl" warm class="unlock-card" @tap="goLatestUnlocked">
        <view class="card-kicker">最近解锁</view>
        <view class="card-title">{{ latestUnlockedLoadFailed ? '解锁记录暂时不可用' : latestUnlocked?.title || '还没有解锁记录' }}</view>
        <view class="card-meta">
          {{ latestUnlockedLoadFailed ? '点按卡片可重试' : latestUnlocked ? formatDateTime(latestUnlocked.createdAt) : '去写下第一条记忆吧' }}
        </view>
      </PaperContainer>

      <PaperContainer radius="xl" class="archive-entry" @tap="goArchive">
        <view>
          <view class="card-title">我的档案</view>
          <view class="card-meta">进入列表进行检索、筛选和管理</view>
        </view>
        <text class="arrow">›</text>
      </PaperContainer>
    </view>

    <view v-if="homeLoadFailed" class="state-wrap">
      <EmptyState text="网络有点慢，首页摘要暂时没加载出来" />
      <view class="retry-link" @tap="retryHomeSummary">点击重试</view>
    </view>

    <view class="scene-decoration">
      <view class="scene-title">Light in the drawer</view>
      <view class="scene-subtitle">慢一点，时间会替你收好答案</view>
    </view>

    <FloatingActionButton text="＋" @tap="goEditor" />
    <view v-if="loading" class="loading">正在同步首页内容...</view>

    <BottomNavBar current="home" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx var(--fb-space-page) 220rpx;
  background: var(--fb-color-bg);
}

.hero-block {
  margin-top: var(--fb-space-hero);
}

.hero-title {
  font-size: var(--fb-font-title-main);
  line-height: 1.3;
  color: var(--fb-color-text);
  font-weight: 600;
}

.hero-subtitle {
  margin-top: 16rpx;
  font-size: var(--fb-font-body-sub);
  line-height: 1.8;
  color: var(--fb-color-text-muted);
}

.section-list {
  margin-top: var(--fb-space-hero);
  display: flex;
  flex-direction: column;
  gap: var(--fb-space-section);
}

.draft-card,
.summary-card,
.unlock-card,
.archive-entry {
  box-shadow: var(--fb-shadow-strong);
}

.archive-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-kicker {
  font-size: var(--fb-font-meta);
  color: var(--fb-color-text-muted);
}

.card-title {
  margin-top: 10rpx;
  font-size: var(--fb-font-title-sub);
  color: var(--fb-color-text);
  font-weight: 600;
}

.card-meta {
  margin-top: 12rpx;
  font-size: var(--fb-font-body-sub);
  color: var(--fb-color-text-muted);
}

.stat-value {
  margin-top: 8rpx;
  font-size: var(--fb-font-number);
  color: var(--fb-color-primary);
  font-weight: 600;
}

.arrow {
  font-size: 38rpx;
  color: var(--fb-color-text-muted);
}

.scene-decoration {
  margin-top: 40rpx;
  padding: 28rpx;
  border-radius: var(--fb-radius-lg);
  background: linear-gradient(135deg, #f4f8fa 0%, #fdf4df 100%);
}

.scene-title {
  font-size: var(--fb-font-title-sub);
  color: var(--fb-color-primary);
  letter-spacing: 1rpx;
}

.scene-subtitle {
  margin-top: 8rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}

.state-wrap {
  margin-top: 16rpx;
  border-radius: var(--fb-radius-lg);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: var(--fb-shadow-soft);
}

.retry-link {
  padding-bottom: 20rpx;
  text-align: center;
  color: var(--fb-color-primary);
  font-size: var(--fb-font-meta);
}

.loading {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(env(safe-area-inset-bottom) + var(--fb-bottom-nav-height) + 40rpx);
  text-align: center;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}
</style>
