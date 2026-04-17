<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import AppTopBar from '../../components/common/AppTopBar.vue'
import FloatingActionButton from '../../components/common/FloatingActionButton.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import { recordService } from '../../services'
import { RecordStatus, type RecordListItemVO } from '../../types'
import { formatDateTime, getToken } from '../../utils'

const loading = ref(false)
const draftCount = ref(0)
const sealedCount = ref(0)
const latestDraft = ref<RecordListItemVO | null>(null)
const latestSealed = ref<RecordListItemVO | null>(null)
const latestUnlocked = ref<RecordListItemVO | null>(null)

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
  try {
    const [draftPage, sealedPage, unlockedPage] = await Promise.all([
      recordService.getRecordList(RecordStatus.DRAFT, { pageNum: 1, pageSize: 1 }),
      recordService.getRecordList(RecordStatus.SEALED, { pageNum: 1, pageSize: 1 }),
      recordService.getUnlockedRecords(1, 1),
    ])
    draftCount.value = draftPage.total
    sealedCount.value = sealedPage.total
    latestDraft.value = draftPage.list[0] || null
    latestSealed.value = sealedPage.list[0] || null
    latestUnlocked.value = unlockedPage.list[0] || null
  } catch {
    draftCount.value = 0
    sealedCount.value = 0
    latestDraft.value = null
    latestSealed.value = null
    latestUnlocked.value = null
  } finally {
    loading.value = false
  }
}

const goEditor = () => uni.navigateTo({ url: '/pages/record-editor/index?source=home' })
const goArchive = () => uni.navigateTo({ url: '/pages/record-list/index' })

const goDraftEntry = () => {
  if (!latestDraft.value) {
    goEditor()
    return
  }

  uni.navigateTo({
    url: `/pages/record-editor/index?id=${latestDraft.value.id}&source=home`,
  })
}

const toDate = (value?: string | number) => {
  if (value === undefined || value === null) {
    return null
  }
  const normalized = typeof value === 'string' && !value.includes('T') ? value.replace(' ', 'T') : value
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

const buildSealedTipText = () => {
  if (sealedCount.value <= 0) {
    return '还没有封存记录'
  }

  const unlockAt = toDate(latestSealed.value?.unlockAt)
  if (!unlockAt) {
    return `已封存 ${sealedCount.value} 条记录`
  }

  const diff = unlockAt.getTime() - Date.now()
  if (diff <= 0) {
    return '有记忆已到解封时间，去我的档案看看'
  }

  const remainDays = Math.max(1, Math.ceil(diff / (24 * 60 * 60 * 1000)))
  return `距离解封还有 ${remainDays} 天`
}

const onSealedSummaryTap = () => {
  uni.showToast({
    title: buildSealedTipText(),
    icon: 'none',
  })
}

const goLatestUnlocked = () => {
  if (!latestUnlocked.value) {
    uni.showToast({ title: '还没有解锁记录', icon: 'none' })
    return
  }

  uni.navigateTo({ url: `/pages/record-detail/index?id=${latestUnlocked.value.id}` })
}

onShow(loadHomeSummary)
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
        <view class="card-meta">当前草稿 {{ draftCount }} 条</view>
      </PaperContainer>

      <PaperContainer radius="xl" class="summary-card" @tap="onSealedSummaryTap">
        <view class="card-kicker">封存摘要</view>
        <view class="stat-value">{{ sealedCount }}</view>
        <view class="card-meta">已封存记录</view>
      </PaperContainer>

      <PaperContainer radius="xl" warm class="unlock-card" @tap="goLatestUnlocked">
        <view class="card-kicker">最近解锁</view>
        <view class="card-title">{{ latestUnlocked?.title || '还没有解锁记录' }}</view>
        <view class="card-meta">{{ latestUnlocked ? formatDateTime(latestUnlocked.createdAt) : '去写下第一条记忆吧' }}</view>
      </PaperContainer>

      <PaperContainer radius="xl" class="archive-entry" @tap="goArchive">
        <view>
          <view class="card-title">我的档案</view>
          <view class="card-meta">进入列表进行检索、筛选和管理</view>
        </view>
        <text class="arrow">›</text>
      </PaperContainer>
    </view>

    <view class="scene-decoration">
      <view class="scene-title">Light in the drawer</view>
      <view class="scene-subtitle">慢一点，时间会替你收好答案</view>
    </view>

    <FloatingActionButton text="＋" @tap="goEditor" />
    <view v-if="loading" class="loading">同步中...</view>
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

.loading {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 100rpx;
  text-align: center;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}
</style>
