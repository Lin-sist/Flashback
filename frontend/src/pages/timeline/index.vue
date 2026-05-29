<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { recordService } from '../../services'
import { RecordStatus, type TimelineGroupVO, type TimelineItemVO } from '../../types'
import { formatDateTime, hasAuthenticatedSession } from '../../utils'

type NodeKind = 'sealed' | 'unlocked' | 'draft' | 'locked'

interface DecoratedItem {
  id: number
  raw: TimelineItemVO
  title: string
  kind: NodeKind
  statusText: string
  tagText: string
  dateText: string
  hasImage: boolean
  excerpt: string
}

const loading = ref(false)
const timelineGroups = ref<TimelineGroupVO[]>([])
const yearInput = ref('')
const appliedYear = ref('')
const timelineLoadFailed = ref(false)
const yearInputFocused = ref(false)
const filterPanelVisible = ref(false)

const flatCount = computed(() => timelineGroups.value.reduce((sum, g) => sum + g.items.length, 0))
const hasAppliedYearFilter = computed(() => Boolean(appliedYear.value))
const showLoadFailureState = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length === 0)
const showEmptyState = computed(() => !loading.value && !timelineLoadFailed.value && timelineGroups.value.length === 0)
const showStaleNotice = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length > 0)
const appliedFilterText = computed(() => hasAppliedYearFilter.value ? `${appliedYear.value} 年` : '全部')
const emptyStateText = computed(() => hasAppliedYearFilter.value ? '这一年还没有留下新的片段' : '时间长廊还没有展开第一段记忆')

const resolveRequestedYear = () => {
  const text = yearInput.value.trim()
  if (!text) return { payload: {}, yearText: '' }
  const year = Number(text)
  if (Number.isNaN(year)) return { payload: {}, yearText: '' }
  return { payload: { year }, yearText: String(year) }
}

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const openFilterPanel = async () => {
  filterPanelVisible.value = true
  await nextTick()
  yearInputFocused.value = true
}

const blurYearInput = () => { yearInputFocused.value = false }
const closeFilterPanel = () => { filterPanelVisible.value = false; yearInputFocused.value = false }

const submitYearFilter = async () => {
  yearInputFocused.value = false
  closeFilterPanel()
  await loadTimeline()
}

const resetYearFilter = async () => {
  yearInput.value = ''
  appliedYear.value = ''
  closeFilterPanel()
  await loadTimeline()
}

const resolveNodeKind = (status: RecordStatus): NodeKind => {
  if (status === RecordStatus.UNLOCKED) return 'unlocked'
  if (status === RecordStatus.SEALED) return 'sealed'
  return 'draft'
}

const resolveStatusText = (status: RecordStatus) => {
  if (status === RecordStatus.UNLOCKED) return '已解封'
  if (status === RecordStatus.SEALED) return '即将抵达'
  return '草稿'
}

const resolveTagText = (status: RecordStatus) => {
  if (status === RecordStatus.UNLOCKED) return '已解封 · 图文记忆'
  if (status === RecordStatus.SEALED) return '即将抵达'
  return '草稿'
}

const resolveSealChar = (status: RecordStatus) => {
  if (status === RecordStatus.UNLOCKED) return '封'
  if (status === RecordStatus.SEALED) return '待'
  return '稿'
}

const decoratedGroups = computed(() =>
  timelineGroups.value.map((group) => ({
    yearMonth: group.yearMonth,
    items: group.items.map<DecoratedItem>((item) => ({
      id: item.id,
      raw: item,
      title: item.title?.trim() || '未命名片段',
      kind: resolveNodeKind(item.status),
      statusText: resolveStatusText(item.status),
      tagText: resolveTagText(item.status),
      dateText: formatDateTime(item.createdAt),
      hasImage: false,
      excerpt: '',
    })),
  }))
)

const openNode = (item: TimelineItemVO) => {
  if (item.status === RecordStatus.DRAFT) {
    uni.navigateTo({ url: `/pages/record-editor/index?id=${item.id}&source=timeline` })
    return
  }
  uni.navigateTo({ url: `/pages/record-detail/index?id=${item.id}&source=timeline` })
}

const loadTimeline = async () => {
  if (!ensureLogin()) return
  loading.value = true
  timelineLoadFailed.value = false
  const requested = resolveRequestedYear()
  try {
    const result = await recordService.getTimeline(requested.payload)
    timelineGroups.value = result
    appliedYear.value = requested.yearText
  } catch {
    timelineLoadFailed.value = true
  } finally {
    loading.value = false
  }
}

onShow(() => {
  uni.hideTabBar({ animation: false })
  loadTimeline()
})
</script>

<template>
  <view class="page">
    <view class="paper-texture" />
    <view class="paper-glow" />

    <!-- filter overlay -->
    <view v-if="filterPanelVisible" class="filter-layer" @tap="closeFilterPanel">
      <view class="filter-sheet" @tap.stop>
        <view class="filter-sheet-head">
          <text class="filter-sheet-title">筛选年份</text>
          <text class="filter-sheet-close" @tap="closeFilterPanel">收起</text>
        </view>
        <view class="filter-input-wrap" :class="{ focused: yearInputFocused }">
          <input
            v-model="yearInput"
            class="year-filter"
            type="number"
            confirm-type="search"
            placeholder="如 2026"
            :focus="yearInputFocused"
            @confirm="submitYearFilter"
            @blur="blurYearInput"
          />
        </view>
        <text class="filter-sheet-meta">当前：{{ appliedFilterText }} · {{ flatCount }} 则</text>
        <view class="filter-actions">
          <view class="filter-action filter-action-ghost" @tap="resetYearFilter">全部年份</view>
          <view class="filter-action" @tap="submitYearFilter">确定</view>
        </view>
      </view>
    </view>

    <scroll-view class="scroll-body" scroll-y enhanced :show-scrollbar="false">

      <!-- topbar -->
      <view class="topbar">
        <text class="logo">时 光 回 序</text>
        <view class="search-btn" @tap="openFilterPanel">
          <view class="search-icon" />
        </view>
      </view>

      <!-- page header -->
      <view class="page-header">
        <text class="page-title">时 间 长 廊</text>
        <text class="page-subtitle">在此处，凝视那些被封存的往昔<text>\n</text>与尚未开启的明日。</text>
        <view class="deco-line" />
        <view v-if="showStaleNotice" class="stale-notice">
          <text>同步稍慢，当前仍显示 {{ appliedFilterText }}</text>
          <text class="stale-action" @tap="loadTimeline">重试</text>
        </view>
      </view>

      <!-- timeline body -->
      <view class="timeline-wrap">
        <view class="timeline-track" />

        <!-- loading skeleton -->
        <view v-if="loading" class="tl-content">
          <view v-for="n in 3" :key="`sk-${n}`" class="tl-item">
            <view class="tl-dot"><view class="tl-dot-inner" /></view>
            <view class="skeleton-date" />
            <view class="skeleton-title" />
          </view>
        </view>

        <!-- load failure -->
        <view v-else-if="showLoadFailureState" class="tl-content">
          <view class="state-block">
            <text class="state-title">暂时没有展开</text>
            <text class="state-desc">网络稍慢，请再试一次</text>
            <view class="state-action" @tap="loadTimeline">重新整理</view>
          </view>
        </view>

        <!-- empty -->
        <view v-else-if="showEmptyState" class="tl-content">
          <view class="state-block">
            <text class="state-title">这一段还很安静</text>
            <text class="state-desc">{{ emptyStateText }}</text>
          </view>
        </view>

        <!-- timeline groups -->
        <view v-else class="tl-content">
          <!-- "此时此刻" node at top -->
          <view class="tl-item">
            <view class="tl-dot tl-dot-now">
              <view class="tl-dot-inner tl-dot-inner-now" />
            </view>
            <text class="now-tag">此时此刻</text>
          </view>

          <view v-for="group in decoratedGroups" :key="group.yearMonth">
            <view v-for="item in group.items" :key="item.id" class="tl-item" @tap="openNode(item.raw)">

              <!-- sealed / arriving card -->
              <template v-if="item.kind === 'sealed'">
                <view class="tl-dot tl-dot-sealed">
                  <view class="tl-dot-inner tl-dot-inner-sealed" />
                </view>
                <text class="tl-date">{{ item.dateText }}</text>
                <view class="card-locked card-arriving">
                  <view class="card-meta">
                    <view class="seal"><text class="seal-char">待</text></view>
                    <text class="card-tag">即将抵达</text>
                  </view>
                  <text class="card-title card-title-dim">{{ item.title }}</text>
                  <view class="countdown-badge">
                    <view class="countdown-dot" />
                    <text class="countdown-text">封存中</text>
                  </view>
                </view>
              </template>

              <!-- unlocked card with image placeholder -->
              <template v-else-if="item.kind === 'unlocked'">
                <view class="tl-dot tl-dot-open">
                  <view class="tl-dot-inner tl-dot-inner-open" />
                </view>
                <text class="tl-date">{{ item.dateText }}</text>
                <view class="card">
                  <view class="card-img-placeholder">
                    <view class="card-img-icon" />
                  </view>
                  <view class="card-meta">
                    <view class="seal seal-open"><text class="seal-char seal-char-open">封</text></view>
                    <text class="card-tag">已解封 · 图文记忆</text>
                  </view>
                  <text class="card-title">{{ item.title }}</text>
                  <view class="card-footer">
                    <text class="card-footer-tag">MEMORY</text>
                  </view>
                </view>
              </template>

              <!-- draft / locked card -->
              <template v-else>
                <view class="tl-dot tl-dot-locked">
                  <view class="tl-dot-inner tl-dot-inner-locked" />
                </view>
                <text class="tl-date">{{ item.dateText }}</text>
                <view class="card-locked">
                  <view class="card-locked-title">
                    <view class="lock-icon" />
                    <text>{{ item.title }}</text>
                  </view>
                  <view class="countdown-badge countdown-badge-dim">
                    <view class="countdown-dot countdown-dot-dim" />
                    <text class="countdown-text">草稿</text>
                  </view>
                </view>
              </template>

            </view>
          </view>
        </view>
      </view>

      <!-- tail -->
      <view class="tail">
        <text class="tail-text">回溯的终点，亦是感知的起点</text>
      </view>

      <view class="nav-safe-area" />
    </scroll-view>

    <!-- bottom navigation -->
    <view class="bottom-nav-shell">
      <view class="bottom-nav">
        <view class="nav-item" @tap="() => uni.switchTab({ url: '/pages/home/index' })">
          <text class="nav-label">首 页</text>
        </view>
        <view class="nav-item active" @tap="() => {}">
          <text class="nav-label">时光轴</text>
          <view class="nav-dot" />
        </view>
        <view class="nav-item" @tap="() => uni.switchTab({ url: '/pages/user-center/index' })">
          <text class="nav-label">我 的</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
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
  height: 100vh;
}

/* ── filter overlay ── */
.filter-layer {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgba(48, 46, 41, 0.18);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: calc(env(safe-area-inset-top) + 120px) 32rpx 0;
}

.filter-sheet {
  width: 100%;
  padding: 30rpx;
  border-radius: 4rpx;
  background: rgba(250, 247, 242, 0.96);
  box-shadow: 0 24rpx 60rpx rgba(48, 46, 41, 0.14);
}

.filter-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-sheet-title {
  font-family: var(--fb-font-serif);
  font-size: 34rpx;
  color: var(--fb-ink);
}

.filter-sheet-close {
  font-size: 24rpx;
  color: var(--fb-ink-light);
}

.filter-input-wrap {
  margin-top: 24rpx;
  height: 88rpx;
  padding: 0 24rpx;
  border-radius: 4rpx;
  background: rgba(245, 240, 232, 0.92);
  border: 1rpx solid rgba(200, 194, 184, 0.28);
  display: flex;
  align-items: center;
}

.filter-input-wrap.focused {
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(181, 53, 42, 0.26);
}

.year-filter {
  flex: 1;
  height: 100%;
  font-size: 28rpx;
  color: var(--fb-ink);
}

.filter-sheet-meta {
  display: block;
  margin-top: 18rpx;
  font-size: 23rpx;
  color: var(--fb-ink-light);
}

.filter-actions {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
}

.filter-action {
  flex: 1;
  min-height: 84rpx;
  border-radius: 4rpx;
  background: var(--fb-vermilion);
  color: #ffffff;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-action-ghost {
  background: rgba(245, 240, 232, 0.96);
  color: var(--fb-ink-mid);
}

/* ── topbar ── */
.topbar {
  padding-top: calc(env(safe-area-inset-top) + 52px);
  padding-left: 56rpx;
  padding-right: 56rpx;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.logo {
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.55em;
  color: var(--fb-ink-light);
}

.search-btn {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.5;
}

.search-icon {
  width: 22rpx;
  height: 22rpx;
  border-radius: 50%;
  border: 2rpx solid var(--fb-ink-mid);
  position: relative;
}

.search-icon::after {
  content: '';
  position: absolute;
  width: 10rpx;
  height: 2rpx;
  background: var(--fb-ink-mid);
  transform: rotate(45deg);
  bottom: -4rpx;
  right: -4rpx;
}

/* ── page header ── */
.page-header {
  padding: 56rpx 56rpx 0;
}

.page-title {
  display: block;
  font-family: var(--fb-font-serif);
  font-size: 56rpx;
  font-weight: 300;
  color: var(--fb-ink);
  letter-spacing: 0.04em;
  line-height: 1.3;
}

.page-subtitle {
  display: block;
  margin-top: 16rpx;
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.06em;
  line-height: 1.8;
}

.deco-line {
  width: 64rpx;
  height: 1rpx;
  background: var(--fb-ink-faint);
  margin-top: 36rpx;
}

.stale-notice {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: var(--fb-ink-mid);
  display: flex;
  gap: 16rpx;
}

.stale-action {
  color: var(--fb-vermilion);
}

/* ── timeline ── */
.timeline-wrap {
  position: relative;
  margin-top: 48rpx;
  padding: 0 56rpx 0 80rpx;
}

.timeline-track {
  position: absolute;
  left: 68rpx;
  top: 0;
  bottom: 0;
  width: 1rpx;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(181, 53, 42, 0.12) 6%,
    rgba(181, 53, 42, 0.12) 94%,
    transparent
  );
}

.tl-content {
  position: relative;
  z-index: 1;
}

/* ── timeline item ── */
.tl-item {
  position: relative;
  padding-left: 0;
  margin-bottom: 72rpx;
}

/* ── dots ── */
.tl-dot {
  position: absolute;
  left: -26rpx;
  top: 8rpx;
  width: 26rpx;
  height: 26rpx;
  border-radius: 50%;
  border: 2rpx solid var(--fb-ink-faint);
  background: #faf7f2;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translateX(-50%);
}

.tl-dot-now {
  border-color: rgba(181, 53, 42, 0.65);
  background: rgba(181, 53, 42, 0.09);
  animation: breathe-now 3s ease-in-out infinite;
}

.tl-dot-sealed {
  border-color: rgba(181, 53, 42, 0.5);
  background: rgba(181, 53, 42, 0.07);
  animation: breathe-sealed 3.6s ease-in-out infinite 0.4s;
}

.tl-dot-open {
  border-color: var(--fb-ink-faint);
}

.tl-dot-locked {
  border-color: var(--fb-ink-faint);
  opacity: 0.5;
}

.tl-dot-inner {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: var(--fb-ink-faint);
}

.tl-dot-inner-now {
  background: var(--fb-vermilion);
  opacity: 1;
  animation: pulse-inner 2.4s ease-in-out infinite;
}

.tl-dot-inner-sealed {
  background: var(--fb-vermilion);
  opacity: 0.8;
  animation: pulse-inner 3s ease-in-out infinite 0.6s;
}

.tl-dot-inner-open {
  background: var(--fb-ink-mid);
  opacity: 0.8;
  animation: breathe-open 4s ease-in-out infinite 1s;
}

.tl-dot-inner-locked {
  background: var(--fb-ink-faint);
  opacity: 0.6;
}

/* "此时此刻" tag */
.now-tag {
  display: inline-flex;
  align-items: center;
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-vermilion);
  letter-spacing: 0.1em;
  opacity: 0.9;
  padding-left: 8rpx;
  gap: 10rpx;
}

.now-tag::before {
  content: '';
  display: block;
  width: 7rpx;
  height: 7rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  flex-shrink: 0;
  animation: pulse-inner 2.4s ease-in-out infinite;
}

/* date label */
.tl-date {
  display: block;
  font-family: var(--fb-font-sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.08em;
  margin-bottom: 16rpx;
}

/* ── regular card (unlocked) ── */
.card {
  position: relative;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  overflow: hidden;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 4rpx 24rpx rgba(140, 120, 90, 0.06),
    0 2rpx 6rpx rgba(140, 120, 90, 0.04);
}

.card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 32rpx;
  bottom: 32rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.55) 25%, rgba(181, 53, 42, 0.55) 75%, transparent);
  border-radius: 2rpx;
}

.card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 24rpx;
  height: 24rpx;
  background: linear-gradient(225deg, rgba(230, 218, 200, 0.9) 0%, rgba(230, 218, 200, 0.9) 48%, rgba(252, 249, 244, 0) 50%);
  border-left: 1rpx solid rgba(188, 174, 152, 0.22);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.22);
}

/* image placeholder */
.card-img-placeholder {
  width: 100%;
  height: 240rpx;
  background: linear-gradient(135deg, #e8e0d5 0%, #d0c7ba 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-img-icon {
  width: 96rpx;
  height: 72rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 48 36' fill='none'><path d='M0 28L12 14L20 22L30 8L48 28' stroke='%239e9890' stroke-width='1' fill='none'/><ellipse cx='38' cy='8' rx='5' ry='5' stroke='%239e9890' stroke-width='1'/></svg>");
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  opacity: 0.25;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
  padding: 36rpx 40rpx 0 44rpx;
}

.seal {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  border: 2rpx solid var(--fb-vermilion);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  opacity: 0.8;
  box-shadow: 0 0 0 1rpx rgba(181, 53, 42, 0.1);
}

.seal-open {
  border-color: var(--fb-ink-mid);
  opacity: 0.6;
}

.seal-char {
  font-family: var(--fb-font-serif);
  font-size: 18rpx;
  color: var(--fb-vermilion);
}

.seal-char-open {
  color: var(--fb-ink-mid);
}

.card-tag {
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.08em;
}

.card-title {
  display: block;
  font-family: var(--fb-font-serif);
  font-size: 30rpx;
  font-weight: 300;
  color: var(--fb-ink);
  letter-spacing: 0.03em;
  line-height: 1.6;
  margin-bottom: 12rpx;
  padding: 0 40rpx 0 44rpx;
}

.card-title-dim {
  color: var(--fb-ink-mid);
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 0 40rpx 36rpx 44rpx;
  margin-top: 20rpx;
}

.card-footer-tag {
  font-family: var(--fb-font-sans);
  font-size: 18rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.12em;
  border: 1rpx solid var(--fb-ink-faint);
  padding: 4rpx 16rpx;
  border-radius: 2rpx;
}

/* ── locked / sealed card ── */
.card-locked {
  position: relative;
  background: rgba(250, 247, 242, 0.45);
  border: 1rpx solid rgba(188, 174, 152, 0.18);
  border-radius: 2rpx;
  padding: 28rpx 40rpx 28rpx 44rpx;
  overflow: hidden;
}

.card-locked::before {
  content: '';
  position: absolute;
  left: 0;
  top: 24rpx;
  bottom: 24rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.3) 25%, rgba(181, 53, 42, 0.3) 75%, transparent);
}

.card-arriving {
  opacity: 0.8;
  background: rgba(252, 249, 244, 0.6);
  border-color: rgba(181, 53, 42, 0.18);
}

.card-arriving::before {
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.5) 25%, rgba(181, 53, 42, 0.5) 75%, transparent);
}

.card-arriving .card-meta {
  padding: 0;
  margin-bottom: 12rpx;
}

.card-arriving .card-title {
  padding: 0;
  margin-bottom: 0;
}

.card-locked-title {
  display: flex;
  align-items: center;
  gap: 16rpx;
  font-family: var(--fb-font-serif);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.03em;
}

.lock-icon {
  width: 22rpx;
  height: 26rpx;
  position: relative;
  flex-shrink: 0;
  opacity: 0.6;
}

.lock-icon::before {
  content: '';
  position: absolute;
  width: 18rpx;
  height: 12rpx;
  border-radius: 2rpx;
  background: var(--fb-ink-light);
  bottom: 0;
  left: 2rpx;
}

.lock-icon::after {
  content: '';
  position: absolute;
  width: 14rpx;
  height: 12rpx;
  border: 3rpx solid var(--fb-ink-light);
  border-bottom: none;
  border-radius: 8rpx 8rpx 0 0;
  top: 0;
  left: 4rpx;
}

.countdown-badge {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 12rpx;
}

.countdown-badge-dim {
  opacity: 0.45;
}

.countdown-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  opacity: 0.8;
}

.countdown-dot-dim {
  background: var(--fb-ink-faint);
  opacity: 0.7;
}

.countdown-text {
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.06em;
}

/* ── skeleton ── */
.skeleton-date {
  width: 160rpx;
  height: 20rpx;
  border-radius: 999rpx;
  background: rgba(200, 194, 184, 0.3);
  margin-bottom: 16rpx;
}

.skeleton-title {
  width: 80%;
  height: 28rpx;
  border-radius: 999rpx;
  background: rgba(200, 194, 184, 0.2);
}

/* ── state block ── */
.state-block {
  padding: 40rpx 0;
}

.state-title {
  display: block;
  font-family: var(--fb-font-serif);
  font-size: 36rpx;
  color: var(--fb-ink);
}

.state-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--fb-ink-mid);
  line-height: 1.7;
}

.state-action {
  margin-top: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 74rpx;
  padding: 0 30rpx;
  border-radius: 4rpx;
  background: var(--fb-vermilion);
  color: #ffffff;
  font-size: 26rpx;
}

/* ── tail ── */
.tail {
  padding: 48rpx 56rpx 0;
  text-align: center;
}

.tail-text {
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-faint);
  letter-spacing: 0.12em;
}

/* ── nav safe area ── */
.nav-safe-area {
  height: calc(128rpx + env(safe-area-inset-bottom));
}

/* ── bottom navigation ── */
.bottom-nav-shell {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 80;
  padding: 0 0 env(safe-area-inset-bottom);
  border-top: 1rpx solid rgba(200, 194, 184, 0.3);
  background: rgba(250, 247, 242, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(48, 46, 41, 0.04);
}

.bottom-nav {
  height: 104rpx;
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.nav-item {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}

.nav-label {
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.2em;
  color: var(--fb-ink-light);
}

.nav-item.active .nav-label {
  color: var(--fb-ink);
  font-weight: 400;
}

.nav-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  opacity: 0.9;
}

@keyframes pulse-inner {
  0%, 100% { opacity: 0.3; transform: scale(0.75); }
  50% { opacity: 1; transform: scale(1.2); }
}

@keyframes breathe-now {
  0%, 100% { box-shadow: 0 0 0 0 rgba(181, 53, 42, 0); }
  50% { box-shadow: 0 0 0 3rpx rgba(181, 53, 42, 0.1); }
}

@keyframes breathe-sealed {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.75; }
}

@keyframes breathe-open {
  0%, 100% { opacity: 0.8; }
  50% { opacity: 0.45; }
}
</style>
