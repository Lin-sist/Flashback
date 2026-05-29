<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { recordService } from '../../services'
import { RecordStatus, type RecordListItemVO } from '../../types'
import { hasAuthenticatedSession } from '../../utils'

type SectionState = 'idle' | 'loading' | 'ready' | 'error'

const ctaPressed = ref(false)
const draftCount = ref(0)
const sealedCount = ref(0)
const unlockedCount = ref(0)

const latestDraft = ref<RecordListItemVO | null>(null)
const latestUnlocked = ref<RecordListItemVO | null>(null)
const latestSealed = ref<RecordListItemVO | null>(null)

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

const loadHomeSummary = async () => {
  if (!ensureLogin()) return

  if (draftState.value !== 'ready') draftState.value = 'loading'
  if (sealedState.value !== 'ready') sealedState.value = 'loading'
  if (unlockedState.value !== 'ready') unlockedState.value = 'loading'

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

const showArrivalCard = computed(() =>
  sealedState.value === 'ready' && sealedCount.value > 0
)

onShow(() => {
  uni.hideTabBar({ animation: false })
  loadHomeSummary()
})
</script>

<template>
  <view class="page">
    <view class="paper-texture" />
    <view class="paper-glow" />

    <scroll-view class="scroll-body" scroll-y enhanced :show-scrollbar="false">

      <!-- top brand -->
      <view class="logo-bar">
        <text class="logo">时 光 回 序</text>
      </view>

      <!-- hero -->
      <view class="hero">
        <view class="deco-line" />

        <view class="headline">
          <text class="line1">今天的你，</text>
          <text class="line2">想留下些什么？</text>
        </view>

        <view class="subline">
          <text>你写下的此一刻</text>
          <text>正等待被未来的你重新读懂</text>
        </view>

        <!-- CTA button -->
        <view class="cta-wrap">
          <view class="cta" :class="{ 'cta-pressed': ctaPressed }"
            @touchstart="ctaPressed = true"
            @touchend="ctaPressed = false"
            @touchcancel="ctaPressed = false"
            @tap="goEditor">
            <view class="cta-dot" />
            <text class="cta-text">提 笔 书 写</text>
          </view>
        </view>

        <!-- arrival card -->
        <view v-if="showArrivalCard" class="arrival-wrap" @tap="goLatestSealed">
          <view class="arrival-card">
            <view class="arrival-meta">
              <view class="seal"><text class="seal-char">待</text></view>
              <text class="arrival-tag">即将抵达</text>
            </view>
            <view class="arrival-text">
              <text>一封封存于 <text class="arrival-em">{{ arrivalYear }} 年</text> 的信</text>
              <text>正跨越最后 <text class="arrival-em">3 天</text>，向你奔来</text>
            </view>
            <view class="countdown">
              <view class="pulse-dot" />
              <text class="countdown-text">72 小时后解封</text>
            </view>
          </view>
        </view>

        <!-- empty state when no sealed records -->
        <view v-else-if="sealedState === 'ready' && sealedCount === 0" class="arrival-wrap" @tap="goEditor">
          <view class="arrival-card">
            <view class="arrival-meta">
              <view class="seal seal-empty"><text class="seal-char">写</text></view>
              <text class="arrival-tag">开始记录</text>
            </view>
            <view class="arrival-text">
              <text>还没有封存的信件</text>
              <text>写下第一封，寄给未来的自己</text>
            </view>
          </view>
        </view>
      </view>

      <view class="nav-safe-area" />
    </scroll-view>

    <!-- bottom navigation -->
    <view class="bottom-nav-shell">
      <view class="bottom-nav">
        <view class="nav-item active" @tap="() => {}">
          <text class="nav-label">首 页</text>
          <view class="nav-dot" />
        </view>
        <view class="nav-item" @tap="goTimeline">
          <text class="nav-label">时光轴</text>
        </view>
        <view class="nav-item" @tap="goUserCenter">
          <text class="nav-label">我 的</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
  overflow: hidden;
}

.paper-texture {
  position: fixed;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='500' height='500'%3E%3Cfilter id='f'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.55' numOctaves='6' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0.15'/%3E%3C/filter%3E%3Crect width='500' height='500' filter='url(%23f)' opacity='0.055'/%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 0;
}

.paper-glow {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 18% 10%, rgba(200, 185, 158, 0.09) 0%, transparent 70%),
    radial-gradient(ellipse 60% 40% at 82% 25%, rgba(185, 168, 140, 0.06) 0%, transparent 65%),
    radial-gradient(ellipse 45% 55% at 70% 78%, rgba(178, 162, 135, 0.07) 0%, transparent 65%),
    radial-gradient(ellipse 65% 40% at 30% 85%, rgba(170, 155, 128, 0.05) 0%, transparent 65%),
    radial-gradient(ellipse 50% 35% at 50% 45%, rgba(250, 245, 238, 0.18) 0%, transparent 75%);
  pointer-events: none;
  z-index: 0;
}

.scroll-body {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  width: 100%;
}

/* ── logo bar ── */
.logo-bar {
  padding-top: calc(env(safe-area-inset-top) + 52px);
  text-align: center;
}

.logo {
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.55em;
  color: var(--fb-ink-light);
}

/* ── hero ── */
.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 56rpx 48rpx;
}

/* deco line */
.deco-line {
  width: 64rpx;
  height: 1rpx;
  background: var(--fb-ink-faint);
  margin-bottom: 72rpx;
}

/* headline */
.headline {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  line-height: 1.5;
}

.line1 {
  font-family: var(--fb-font-serif);
  font-size: 64rpx;
  font-weight: 300;
  color: var(--fb-ink);
  letter-spacing: 0.06em;
  display: block;
  margin-bottom: 12rpx;
  text-align: center;
  width: 100%;
}

.line2 {
  font-family: var(--fb-font-serif);
  font-size: 56rpx;
  font-weight: 300;
  color: var(--fb-ink-mid);
  letter-spacing: 0.04em;
  display: block;
  text-align: center;
  width: 100%;
}

/* subline */
.subline {
  margin-top: 56rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  font-family: var(--fb-font-serif);
  font-size: 26rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  line-height: 2;
  letter-spacing: 0.04em;
  text-align: center;
}

/* ── CTA button ── */
.cta-wrap {
  margin-top: 96rpx;
  display: flex;
  justify-content: center;
}

.cta {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 72rpx;
  font-family: var(--fb-font-serif);
  font-size: 30rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: var(--fb-ink);
  background: transparent;
  border: 1rpx solid rgba(200, 194, 184, 0.6);
  border-radius: 4rpx;
  box-shadow: inset 0 0 0 1rpx rgba(181, 53, 42, 0.06);
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.cta-pressed {
  background: rgba(181, 53, 42, 0.04);
  box-shadow: inset 0 0 0 1rpx rgba(181, 53, 42, 0.18), inset 0 2rpx 8rpx rgba(181, 53, 42, 0.06);
  border-color: rgba(181, 53, 42, 0.25);
}

/* corner decorations — top-left */
.cta::before {
  content: '';
  position: absolute;
  top: -2rpx;
  left: -2rpx;
  width: 14rpx;
  height: 14rpx;
  border-color: var(--fb-ink-mid);
  border-style: solid;
  border-width: 2rpx 0 0 2rpx;
}

/* corner decorations — bottom-right */
.cta::after {
  content: '';
  position: absolute;
  bottom: -2rpx;
  right: -2rpx;
  width: 14rpx;
  height: 14rpx;
  border-color: var(--fb-ink-mid);
  border-style: solid;
  border-width: 0 2rpx 2rpx 0;
}

.cta-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  opacity: 0.7;
  flex-shrink: 0;
}

.cta-text {
  font-family: var(--fb-font-serif);
  font-size: 30rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: var(--fb-ink);
}

/* ── arrival card ── */
.arrival-wrap {
  margin-top: 104rpx;
  width: 100%;
}

.arrival-card {
  position: relative;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  padding: 44rpx 48rpx 40rpx 56rpx;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 4rpx 24rpx rgba(140, 120, 90, 0.08),
    0 2rpx 8rpx rgba(140, 120, 90, 0.05);
  overflow: hidden;
}

/* right-top corner fold */
.arrival-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 32rpx;
  height: 32rpx;
  background: linear-gradient(
    225deg,
    rgba(218, 205, 185, 0.95) 0%,
    rgba(218, 205, 185, 0.95) 48%,
    rgba(252, 249, 244, 0) 50%
  );
  border-left: 1rpx solid rgba(188, 174, 152, 0.3);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.3);
}

/* left vermilion accent */
.arrival-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 40rpx;
  bottom: 40rpx;
  width: 3rpx;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(181, 53, 42, 0.5) 25%,
    rgba(181, 53, 42, 0.5) 75%,
    transparent
  );
  border-radius: 2rpx;
}

.arrival-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.seal {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  border: 2rpx solid var(--fb-vermilion);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  opacity: 0.75;
}

.seal-empty {
  border-color: var(--fb-ink-light);
  opacity: 0.5;
}

.seal-char {
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  color: var(--fb-vermilion);
  letter-spacing: 0;
}

.seal-empty .seal-char {
  color: var(--fb-ink-light);
}

.arrival-tag {
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.1em;
}

.arrival-text {
  font-family: var(--fb-font-serif);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--fb-ink-mid);
  line-height: 1.85;
  letter-spacing: 0.03em;
  display: flex;
  flex-direction: column;
}

.arrival-em {
  color: var(--fb-ink);
  font-weight: 400;
}

.countdown {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 20rpx;
}

.pulse-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  opacity: 0.9;
  flex-shrink: 0;
  animation: pulse-home 2.4s ease-in-out infinite;
}

@keyframes pulse-home {
  0%, 100% { opacity: 0.35; transform: scale(0.8); }
  50% { opacity: 0.9; transform: scale(1.15); }
}

.countdown-text {
  font-size: 22rpx;
  font-family: var(--fb-font-serif);
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.06em;
}

/* ── nav safe area ── */
.nav-safe-area {
  height: 24rpx;
}

/* ── bottom navigation ── */
.bottom-nav-shell {
  position: relative;
  z-index: 80;
  padding: 0 0 env(safe-area-inset-bottom);
  background: transparent;
  border-top: 1rpx solid rgba(200, 194, 184, 0.3);
}

.bottom-nav {
  height: auto;
  padding: 32rpx 0 68rpx;
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 8rpx 32rpx;
}

.nav-label {
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.2em;
  color: var(--fb-ink-light);
  transition: color 0.2s;
}

.nav-item.active .nav-label {
  color: var(--fb-ink);
  font-weight: 300;
}

.nav-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  margin-top: 4rpx;
}
</style>
