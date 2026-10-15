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
  <view class="page fb-theme">
    <PreviewModeNotice />
    <AppTopSafeBar title="时光回序" padding-x="56rpx" center-title>
      <template #title><text class="brand-mark">时光回序</text></template>
    </AppTopSafeBar>
    <scroll-view class="scroll-body" scroll-y enhanced :show-scrollbar="false">
      <view class="home-content">
        <view class="hero">
          <text class="headline">今天的你，</text>
          <text class="headline headline-second">想留下些什么？</text>
          <text class="subline">你写下的此一刻，正等待被未来的你重新读懂</text>
        </view>

        <button class="capture" aria-label="留下此刻，开始书写" hover-class="capture--pressed" @tap="goEditor">
          <view class="capture-corner capture-corner--tl" aria-hidden="true" />
          <view class="capture-corner capture-corner--tr" aria-hidden="true" />
          <view class="capture-corner capture-corner--bl" aria-hidden="true" />
          <view class="capture-corner capture-corner--br" aria-hidden="true" />
          <text class="capture-prompt">此刻，你想留下什么？</text>
        </button>

        <button v-if="showArrivalCard" class="arrival-card" @tap="goLatestSealed">
          <view v-if="latestSealed?.cover" class="cover-wrap">
            <image v-if="coverUrls[latestSealed.id]" class="cover-image" :src="coverUrls[latestSealed.id]" mode="aspectFill" @error="markCoverFailed(latestSealed.id)" />
            <view v-else class="cover-fallback">{{ coverErrors[latestSealed.id] ? '封面暂不可用' : '正在取回封面…' }}</view>
          </view>
          <view class="paper-copy arrival-copy">
            <view class="arrival-copy-text">
            <text class="record-title">{{ arrivalTitle }}</text>
            <text class="entry-label">封存中 · {{ arrivalYear }} 年留下 · {{ arrivalCountdownText }}</text>
            </view>
            <text class="seal">待</text>
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
            <view class="recent-main">
              <text class="record-title">{{ latestUnlocked.title || '未命名片段' }}</text>
              <text class="entry-action">再次读一读 ›</text>
            </view>
          </view>
        </button>
        <view v-else-if="unlockedState === 'ready' && unlockedCount === 0" class="inline-state">还没有抵达的记录</view>
        <view class="saved-summary">
          <view v-if="draftState === 'loading' && !latestDraft" class="inline-state">正在同步已留下的记录…</view>
          <button v-else-if="draftState === 'error'" class="inline-state retry" @tap="retryHomeSummary">已留下记录暂未同步 · 轻触重试</button>
          <button v-else-if="latestDraft" class="saved-entry" @tap="goDraftEntry">
            <text class="entry-label">已留下</text>
            <view class="saved-entry-main">
              <text class="saved-entry-title">{{ latestDraft.title || '未命名片段' }}</text>
              <text class="entry-action">继续补充 ›</text>
            </view>
          </button>
          <view v-else class="inline-state">还没有留下记录，从此刻开始就好。</view>
        </view>
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
  background: var(--fb-paper, #faf7f2);
  color: var(--fb-ink, #302e29);
  font-family: var(--fb-letter-font, 'Songti SC', 'STSong', 'SimSun', Georgia, serif);
}
.scroll-body { flex: 1; min-height: 0; width: 100%; }
.brand-mark { font-family: var(--fb-letter-font, 'Songti SC', 'STSong', 'SimSun', Georgia, serif); font-size: 24rpx; font-weight: 400; letter-spacing: 8rpx; text-indent: 8rpx; color: var(--fb-ink-light, #9e9890); white-space: nowrap; }
.home-content { padding: 52rpx 56rpx 48rpx; }
.hero { text-align: center; margin-bottom: 64rpx; }
.headline { display: block; font-size: 60rpx; font-weight: 400; line-height: 1.5; letter-spacing: 3rpx; }
.headline-second { font-size: 54rpx; }
.subline { display: block; margin-top: 28rpx; font-size: 26rpx; color: var(--fb-ink-mid, #6b6560); line-height: 1.8; }
button { margin: 0; padding: 0; border-radius: 0; background: transparent; font: inherit; color: inherit; text-align: left; line-height: 1.6; }
button::after { border: none; }
.capture { position: relative; width: 100%; min-height: 254rpx; padding: 52rpx; box-sizing: border-box; border: 1rpx solid var(--fb-letter-line, rgba(188, 174, 152, 0.28)); border-radius: 4rpx; background: var(--fb-letter-surface, #f3eee4); box-shadow: inset 0 4rpx 8rpx rgba(120,100,70,.16), inset 4rpx 2rpx 6rpx rgba(120,100,70,.08), inset 0 -2rpx 0 rgba(255,255,255,.65); display: flex; align-items: center; }
.capture--pressed { background: #ede7dc; }
.capture-prompt { font-size: 32rpx; color: var(--fb-ink-mid, #6b6560); }
.capture-corner { position: absolute; width: 28rpx; height: 28rpx; border-color: rgba(104,98,90,.6); border-style: solid; pointer-events: none; }
.capture-corner--tl { top: 18rpx; left: 18rpx; border-width: 1rpx 0 0 1rpx; }
.capture-corner--tr { top: 18rpx; right: 18rpx; border-width: 1rpx 1rpx 0 0; }
.capture-corner--bl { bottom: 18rpx; left: 18rpx; border-width: 0 0 1rpx 1rpx; }
.capture-corner--br { bottom: 18rpx; right: 18rpx; border-width: 0 1rpx 1rpx 0; }
.nav-dot { width: 6rpx; height: 6rpx; border-radius: 50%; background: var(--fb-vermilion, #b5352a); flex-shrink: 0; }
.saved-summary { margin-top: 32rpx; }
.inline-state { display: block; width: 100%; padding: 24rpx 0; font-size: 26rpx; color: var(--fb-ink-mid, #6b6560); line-height: 1.8; }
.retry { text-decoration: underline; text-underline-offset: 6rpx; }
.saved-entry { width: 100%; min-height: 152rpx; padding: 24rpx 38rpx 24rpx 48rpx; box-sizing: border-box; border: 1rpx solid var(--fb-letter-line, rgba(188,174,152,.28)); background: var(--fb-letter-paper, #fcf9f4); box-shadow: 0 2rpx 4rpx rgba(125,105,75,.06), 0 10rpx 24rpx rgba(125,105,75,.06); }
.saved-entry-main { display: flex; align-items: baseline; justify-content: space-between; gap: 20rpx; margin-top: 8rpx; }
.saved-entry-title { flex: 1; min-width: 0; font-size: 30rpx; line-height: 1.7; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.saved-entry .entry-label { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.saved-entry .entry-action { flex-shrink: 0; margin-top: 0; font-size: 24rpx; border-bottom: 1rpx solid rgba(107,101,96,.45); }
.entry-label { display: block; font-size: 24rpx; color: var(--fb-ink-mid, #6b6560); line-height: 1.8; }
.record-title { display: block; font-size: 32rpx; line-height: 1.65; margin: 12rpx 0; overflow-wrap: anywhere; word-break: break-all; }
.entry-action { display: block; font-size: 26rpx; color: var(--fb-ink-mid, #6b6560); margin-top: 16rpx; }
.arrival-card, .review-card { position: relative; width: 100%; margin-top: 32rpx; border: 1rpx solid var(--fb-letter-line, rgba(188, 174, 152, 0.28)); background: var(--fb-letter-paper, #fcf9f4); box-shadow: var(--fb-letter-shadow, 0 2rpx 4rpx rgba(125, 105, 75, 0.10), 0 16rpx 40rpx rgba(125, 105, 75, 0.10)); overflow: hidden; }
.arrival-card { margin-top: 48rpx; }
.arrival-card::before { content: ''; position: absolute; top: 0; bottom: 0; left: 0; width: 3rpx; background: linear-gradient(to bottom, transparent, rgba(181,53,42,.5) 15%, rgba(181,53,42,.5) 85%, transparent); pointer-events: none; z-index: 1; }
.arrival-card::after, .review-card::after { content: ''; position: absolute; top: 0; right: 0; width: 32rpx; height: 32rpx; background: linear-gradient(225deg,#f4efe5 0%,#f4efe5 49%,#e3dbc9 50%,#e3dbc9 100%); box-shadow: -3rpx 3rpx 5rpx rgba(120,100,70,.16); pointer-events: none; }
.paper-copy { padding: 28rpx 38rpx 28rpx 48rpx; }
.arrival-copy { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; }
.arrival-copy-text { flex: 1; min-width: 0; }
.seal { flex-shrink: 0; width: 48rpx; height: 48rpx; display: flex; align-items: center; justify-content: center; border: 2rpx solid rgba(181,53,42,.75); border-radius: 50%; font-size: 24rpx; color: var(--fb-vermilion, #b5352a); }
.recent-main { display: flex; align-items: baseline; justify-content: space-between; gap: 20rpx; }
.recent-main .record-title { flex: 1; min-width: 0; margin-bottom: 0; }
.recent-main .entry-action { flex-shrink: 0; margin-top: 0; border-bottom: 1rpx solid rgba(107,101,96,.45); font-size: 24rpx; }
.cover-wrap { height: 200rpx; background: var(--fb-letter-surface, #f3eee4); }
.cover-image { width: 100%; height: 100%; display: block; }
.cover-fallback { height: 100%; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: var(--fb-ink-mid, #6b6560); }
.bottom-nav-shell { flex-shrink: 0; padding: 14rpx 24rpx calc(env(safe-area-inset-bottom) + 16rpx); background: var(--fb-paper, #faf7f2); }
.bottom-nav { display: flex; min-height: 112rpx; border: 1rpx solid rgba(188,174,152,.35); border-radius: 24rpx; background: #f7f2e9; box-shadow: 0 -4rpx 36rpx rgba(125,105,75,.10); }
.nav-item { flex: 1; min-width: 0; min-height: 88rpx; padding: 14rpx 8rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6rpx; font-size: 26rpx; color: var(--fb-ink-mid, #6b6560); text-align: center; }
.nav-item.active { color: var(--fb-ink, #302e29); }
</style>
