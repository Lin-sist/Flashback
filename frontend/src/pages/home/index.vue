<script setup lang="ts">
import { onHide, onShow } from '@dcloudio/uni-app'
import { computed, onUnmounted, ref } from 'vue'
import AppTopSafeBar from '../../components/common/AppTopSafeBar.vue'
import PreviewModeNotice from '../../components/common/PreviewModeNotice.vue'
import { recordService } from '../../services'
import { useRecordCoverUrls } from '../../composables/useRecordCoverUrls'
import { RecordStatus, type RecordListItemVO } from '../../types'
import { formatDateTime, hasAuthenticatedSession } from '../../utils'

type SectionState = 'idle' | 'loading' | 'ready' | 'error'

const draftCount = ref(0)
const sealedCount = ref(0)
const unlockedCount = ref(0)

const latestDraft = ref<RecordListItemVO | null>(null)
const latestUnlocked = ref<RecordListItemVO | null>(null)
const latestSealed = ref<RecordListItemVO | null>(null)

const draftState = ref<SectionState>('idle')
const sealedState = ref<SectionState>('idle')
const unlockedState = ref<SectionState>('idle')
const { coverUrls, coverErrors, loadCovers, markCoverFailed } = useRecordCoverUrls()
const homeClock = ref(Date.now())
let homeClockTimer: ReturnType<typeof setInterval> | null = null

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadHomeSummary = async () => {
  if (!ensureLogin()) return

  if (draftState.value !== 'ready') draftState.value = 'loading'
  if (sealedState.value !== 'ready') sealedState.value = 'loading'
  if (unlockedState.value !== 'ready') unlockedState.value = 'loading'

  const [draftResult, sealedResult, unlockedResult] = await Promise.allSettled([
    recordService.getRecordList(RecordStatus.SAVED, { pageNum: 1, pageSize: 1 }),
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
    latestSealed.value = sealedResult.value.list[0] || null
    sealedState.value = 'ready'
  } else {
    sealedCount.value = 0
    latestSealed.value = null
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

  const coverSources = [latestSealed.value, latestUnlocked.value]
    .filter((item): item is RecordListItemVO => Boolean(item?.cover))
    .map((item) => ({ recordId: item.id, cover: item.cover }))
  void loadCovers(coverSources)
}

const retryHomeSummary = () => {
  loadHomeSummary()
  uni.showToast({ title: '正在重新同步首页内容', icon: 'none' })
}

const goEditor = () => uni.navigateTo({ url: '/pages/record-editor/index?source=home' })

const goDraftEntry = () => {
  if (draftState.value === 'loading' && !latestDraft.value) {
    uni.showToast({ title: '草稿同步中，请稍后再试', icon: 'none' })
    return
  }
  if (draftState.value === 'error') { retryHomeSummary(); return }
  if (!latestDraft.value) { goEditor(); return }
  uni.navigateTo({ url: `/pages/record-editor/index?id=${latestDraft.value.id}&source=home` })
}

const goLatestUnlocked = () => {
  if (unlockedState.value === 'loading' && !latestUnlocked.value) {
    uni.showToast({ title: '最近解锁同步中，请稍后再试', icon: 'none' })
    return
  }
  if (unlockedState.value === 'error') { retryHomeSummary(); return }
  if (!latestUnlocked.value) {
    uni.showToast({ title: '还没有解锁记录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/record-detail/index?id=${latestUnlocked.value.id}&source=home` })
}

const goLatestSealed = () => {
  if (sealedState.value === 'error') { retryHomeSummary(); return }
  if (!latestSealed.value) {
    uni.navigateTo({ url: '/pages/record-list/index' })
    return
  }
  uni.navigateTo({ url: `/pages/record-detail/index?id=${latestSealed.value.id}&source=home` })
}

const goTimeline = () => {
  uni.switchTab({ url: '/pages/timeline/index' })
}

const goUserCenter = () => {
  uni.switchTab({ url: '/pages/user-center/index' })
}

/* arrival card display */
const arrivalTitle = computed(() => {
  if (latestSealed.value?.title?.trim()) return latestSealed.value.title.trim()
  return '致未来的信件'
})

const arrivalYear = computed(() => {
  if (latestSealed.value?.createdAt) {
    return new Date(latestSealed.value.createdAt).getFullYear()
  }
  return new Date().getFullYear()
})

const arrivalCountdownText = computed(() => {
  const unlockAt = latestSealed.value?.unlockAt
  if (!unlockAt) return '等待抵达时间同步'
  const remaining = new Date(unlockAt).getTime() - homeClock.value
  if (remaining <= 0) return '正在抵达'
  const days = Math.floor(remaining / 86400000)
  const hours = Math.floor((remaining % 86400000) / 3600000)
  const minutes = Math.floor((remaining % 3600000) / 60000)
  if (days > 0) return `${days} 天 ${hours} 小时后解封`
  if (hours > 0) return `${hours} 小时 ${minutes} 分后解封`
  return `${Math.max(1, minutes)} 分钟后解封`
})

const latestUnlockedDateText = computed(() => {
  if (!latestUnlocked.value?.unlockAt) return '最近抵达'
  return formatDateTime(latestUnlocked.value.unlockAt)
})

const showArrivalCard = computed(() =>
  sealedState.value === 'ready' && sealedCount.value > 0
)

onShow(() => {
  uni.hideTabBar({ animation: false })
  homeClock.value = Date.now()
  if (homeClockTimer) clearInterval(homeClockTimer)
  homeClockTimer = setInterval(() => {
    homeClock.value = Date.now()
  }, 1000)
  loadHomeSummary()
})

onHide(() => {
  if (homeClockTimer) {
    clearInterval(homeClockTimer)
    homeClockTimer = null
  }
})

onUnmounted(() => {
  if (homeClockTimer) clearInterval(homeClockTimer)
})
</script>

<template>
  <view class="page">
    <PreviewModeNotice />
    <AppTopSafeBar title="时光回序" padding-x="56rpx" />
    <scroll-view class="scroll-body" scroll-y enhanced :show-scrollbar="false">
      <view class="home-content">
        <view class="hero">
          <text class="headline">今天的你，</text>
          <text class="headline headline-second">想留下些什么？</text>
          <text class="subline">从一句话、一张图片或一段声音开始</text>
        </view>

        <button class="capture" hover-class="capture--pressed" @tap="goEditor">
          <text class="capture-prompt">此刻，你想留下什么？</text>
          <view class="capture-action"><view class="action-dot" /><text>留下此刻</text></view>
        </button>

        <view class="records-overview">
          <text class="section-label">我的记录</text>
          <view v-if="draftState === 'loading' && !latestDraft" class="inline-state">正在同步已经留下的记录…</view>
          <button v-else-if="draftState === 'error'" class="inline-state retry" @tap="retryHomeSummary">记录暂未同步 · 轻触重试</button>
          <button v-else-if="latestDraft" class="saved-entry" @tap="goDraftEntry">
            <text class="entry-label">已留下 · 可以继续补充</text>
            <text class="record-title">{{ latestDraft.title || '未命名片段' }}</text>
            <text class="entry-action">继续写 ›</text>
          </button>
          <view v-else class="inline-state">还没有留下记录，从此刻开始就好。</view>
        </view>

        <button v-if="showArrivalCard" class="arrival-card" @tap="goLatestSealed">
          <view v-if="latestSealed?.cover" class="cover-wrap">
            <image v-if="coverUrls[latestSealed.id]" class="cover-image" :src="coverUrls[latestSealed.id]" mode="aspectFill" @error="markCoverFailed(latestSealed.id)" />
            <view v-else class="cover-fallback">{{ coverErrors[latestSealed.id] ? '封面暂不可用' : '正在取回封面…' }}</view>
          </view>
          <view class="paper-copy">
            <view class="entry-heading"><text class="entry-label">封存中</text><text class="seal">待</text></view>
            <text class="record-title">{{ arrivalTitle }}</text>
            <text class="entry-label">封存于 {{ arrivalYear }} 年 · {{ arrivalCountdownText }}</text>
          </view>
        </button>
        <view v-else-if="sealedState === 'loading'" class="inline-state">正在同步封存中的记录…</view>
        <button v-else-if="sealedState === 'error'" class="inline-state retry" @tap="retryHomeSummary">封存记录暂未同步 · 轻触重试</button>

        <view v-if="unlockedState === 'loading' && !latestUnlocked" class="inline-state">正在同步最近抵达…</view>
        <button v-else-if="unlockedState === 'error'" class="inline-state retry" @tap="retryHomeSummary">最近抵达暂未同步 · 轻触重试</button>
        <button v-else-if="latestUnlocked" class="review-card" @tap="goLatestUnlocked">
          <view v-if="latestUnlocked.cover" class="cover-wrap">
            <image v-if="coverUrls[latestUnlocked.id]" class="cover-image" :src="coverUrls[latestUnlocked.id]" mode="aspectFill" @error="markCoverFailed(latestUnlocked.id)" />
            <view v-else class="cover-fallback">{{ coverErrors[latestUnlocked.id] ? '封面暂不可用' : '正在取回封面…' }}</view>
          </view>
          <view class="paper-copy">
            <text class="entry-label">最近抵达 · 时间回看</text>
            <text class="record-title">{{ latestUnlocked.title || '未命名片段' }}</text>
            <text class="entry-label">{{ latestUnlockedDateText }}</text>
            <text class="entry-action">再次读一读 ›</text>
          </view>
        </button>
        <view v-else-if="unlockedState === 'ready' && unlockedCount === 0" class="inline-state">还没有抵达的记录</view>
      </view>
    </scroll-view>
    <view class="bottom-nav-shell">
      <view class="bottom-nav">
        <button class="nav-item active" aria-label="首页，当前页面"><text>首页</text><view class="nav-dot" /></button>
        <button class="nav-item" @tap="goTimeline">时光轴</button>
        <button class="nav-item" @tap="goUserCenter">个人中心</button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--fb-paper);
  color: var(--fb-ink);
  font-family: var(--fb-letter-font);
}
.scroll-body { flex: 1; min-height: 0; width: 100%; }
.home-content { padding: 52rpx 56rpx 48rpx; }
.hero { text-align: center; margin-bottom: 64rpx; }
.headline { display: block; font-size: 60rpx; font-weight: 400; line-height: 1.5; letter-spacing: 3rpx; }
.headline-second { font-size: 54rpx; }
.subline { display: block; margin-top: 28rpx; font-size: 26rpx; color: var(--fb-ink-mid); line-height: 1.8; }
button { margin: 0; padding: 0; border-radius: 0; background: transparent; font: inherit; color: inherit; text-align: left; line-height: 1.6; }
button::after { border: none; }
.capture { width: 100%; min-height: 240rpx; padding: 40rpx; box-sizing: border-box; border: 1rpx solid var(--fb-letter-line); background: var(--fb-letter-surface); display: flex; flex-direction: column; justify-content: space-between; gap: 32rpx; }
.capture--pressed { background: #ede7dc; }
.capture-prompt { font-size: 32rpx; color: var(--fb-ink-mid); }
.capture-action { align-self: flex-end; display: flex; align-items: center; gap: 14rpx; padding: 18rpx 28rpx; border-radius: 999rpx; border: 1rpx solid var(--fb-letter-glass-edge); background: var(--fb-letter-glass); box-shadow: var(--fb-letter-glass-shadow); font-size: 28rpx; }
.action-dot, .nav-dot { width: 8rpx; height: 8rpx; border-radius: 50%; background: var(--fb-vermilion); flex-shrink: 0; }
.records-overview { margin-top: 48rpx; }
.section-label { display: block; font-size: 24rpx; color: var(--fb-ink-mid); margin-bottom: 16rpx; }
.inline-state { display: block; width: 100%; padding: 24rpx 0; font-size: 26rpx; color: var(--fb-ink-mid); line-height: 1.8; }
.retry { text-decoration: underline; text-underline-offset: 6rpx; }
.saved-entry { width: 100%; padding: 24rpx 0; border-top: 1rpx solid var(--fb-letter-line); border-bottom: 1rpx solid var(--fb-letter-line); }
.entry-label { display: block; font-size: 24rpx; color: var(--fb-ink-mid); line-height: 1.8; }
.record-title { display: block; font-size: 32rpx; line-height: 1.65; margin: 12rpx 0; overflow-wrap: anywhere; word-break: break-all; }
.entry-action { display: block; font-size: 26rpx; color: var(--fb-ink-mid); margin-top: 16rpx; }
.arrival-card, .review-card { width: 100%; margin-top: 32rpx; border: 1rpx solid var(--fb-letter-line); background: var(--fb-letter-paper); box-shadow: var(--fb-letter-shadow); overflow: hidden; }
.paper-copy { padding: 30rpx 36rpx; }
.entry-heading { display: flex; align-items: center; justify-content: space-between; }
.seal { width: 44rpx; height: 44rpx; display: flex; align-items: center; justify-content: center; border: 1rpx solid var(--fb-ink-mid); border-radius: 50%; font-size: 24rpx; color: var(--fb-ink-mid); }
.cover-wrap { height: 200rpx; background: var(--fb-letter-surface); }
.cover-image { width: 100%; height: 100%; display: block; }
.cover-fallback { height: 100%; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: var(--fb-ink-mid); }
.bottom-nav-shell { flex-shrink: 0; padding: 14rpx 40rpx calc(env(safe-area-inset-bottom) + 16rpx); background: var(--fb-paper); }
.bottom-nav { display: flex; border: 1rpx solid var(--fb-letter-glass-edge); border-radius: 999rpx; background: var(--fb-letter-glass); box-shadow: var(--fb-letter-glass-shadow); }
.nav-item { flex: 1; min-width: 0; min-height: 88rpx; padding: 14rpx 8rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6rpx; font-size: 26rpx; color: var(--fb-ink-mid); text-align: center; }
.nav-item.active { color: var(--fb-ink); }
</style>
