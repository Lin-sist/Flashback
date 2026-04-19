<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import BottomNavBar from '../../components/common/BottomNavBar.vue'
import { recordService } from '../../services'
import { RecordStatus, type TimelineGroupVO, type TimelineItemVO } from '../../types'
import { formatDateTime, getToken } from '../../utils'

type CorridorNodeKind = 'sealed' | 'unlocked' | 'draft'

interface DecoratedTimelineItem {
  id: number
  raw: TimelineItemVO
  title: string
  kind: CorridorNodeKind
  statusText: string
  dateText: string
}

const loading = ref(false)
const timelineGroups = ref<TimelineGroupVO[]>([])
const yearInput = ref('')
const appliedYear = ref('')
const timelineLoadFailed = ref(false)
const yearInputFocused = ref(false)
const filterPanelVisible = ref(false)

const flatCount = computed(() => timelineGroups.value.reduce((sum, group) => sum + group.items.length, 0))
const hasAppliedYearFilter = computed(() => Boolean(appliedYear.value))
const showLoadFailureState = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length === 0)
const showEmptyState = computed(() => !loading.value && !timelineLoadFailed.value && timelineGroups.value.length === 0)
const showStaleNotice = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length > 0)
const appliedFilterText = computed(() => hasAppliedYearFilter.value ? `${appliedYear.value} 年` : '全部')
const emptyStateText = computed(() => hasAppliedYearFilter.value ? '这一年还没有留下新的片段' : '时间长廊还没有展开第一段记忆')
const corridorSummaryText = computed(() => {
  if (loading.value) {
    return '整理中'
  }

  if (showLoadFailureState.value) {
    return '暂时未展开'
  }

  if (showEmptyState.value) {
    return hasAppliedYearFilter.value ? `${appliedYear.value} 年暂无记录` : '还没有记录'
  }

  if (hasAppliedYearFilter.value) {
    return `${appliedYear.value} 年 · ${flatCount.value} 则`
  }

  return flatCount.value > 0 ? `共 ${flatCount.value} 则` : '暂无记录'
})

const resolveRequestedYear = () => {
  const text = yearInput.value.trim()
  if (!text) {
    return { payload: {}, yearText: '' }
  }

  const year = Number(text)
  if (Number.isNaN(year)) {
    return { payload: {}, yearText: '' }
  }

  return { payload: { year }, yearText: String(year) }
}

const ensureLogin = () => {
  if (!getToken()) {
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

const blurYearInput = () => {
  yearInputFocused.value = false
}

const closeFilterPanel = () => {
  filterPanelVisible.value = false
  yearInputFocused.value = false
}

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

const resolveNodeKind = (status: RecordStatus): CorridorNodeKind => {
  if (status === RecordStatus.UNLOCKED) {
    return 'unlocked'
  }

  if (status === RecordStatus.SEALED) {
    return 'sealed'
  }

  return 'draft'
}

const resolveStatusText = (status: RecordStatus) => {
  if (status === RecordStatus.UNLOCKED) {
    return '已解锁'
  }

  if (status === RecordStatus.SEALED) {
    return '未启封'
  }

  return '草稿'
}

const decoratedTimelineGroups = computed(() =>
  timelineGroups.value.map((group) => ({
    yearMonth: group.yearMonth,
    itemCountText: `${group.items.length} 则`,
    items: group.items.map<DecoratedTimelineItem>((item) => {
      return {
        id: item.id,
        raw: item,
        title: item.title?.trim() || '未命名片段',
        kind: resolveNodeKind(item.status),
        statusText: resolveStatusText(item.status),
        dateText: formatDateTime(item.createdAt),
      }
    }),
  }))
)

const openTimelineNode = (item: TimelineItemVO) => {
  if (item.status === RecordStatus.DRAFT) {
    uni.navigateTo({ url: `/pages/record-editor/index?id=${item.id}&source=timeline` })
    return
  }

  if (item.status === RecordStatus.SEALED) {
    uni.showToast({
      title: '该记录已封存，暂不可查看',
      icon: 'none',
    })
    return
  }

  uni.navigateTo({ url: `/pages/record-detail/index?id=${item.id}&source=timeline` })
}

const loadTimeline = async () => {
  if (!ensureLogin()) {
    return
  }

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
    <view class="page-glow page-glow-top" />
    <view class="page-glow page-glow-bottom" />

    <view class="top-bar">
      <view class="top-icon-btn" @tap="openFilterPanel">
        <view class="icon icon-search" />
      </view>
      <view class="brand">时光回序</view>
      <view class="top-bar-side" />
    </view>

    <view class="hero">
      <view class="hero-title">时间长廊</view>
      <view class="hero-meta-row">
        <view class="hero-filter-pill" @tap="openFilterPanel">
          <view class="icon icon-search icon-search-soft" />
          <text class="hero-filter-text">{{ appliedFilterText }}</text>
        </view>
        <view class="hero-count">{{ corridorSummaryText }}</view>
      </view>

      <view v-if="showStaleNotice" class="notice-panel">
        <view class="notice-title">同步稍慢</view>
        <view class="notice-desc">当前仍显示 {{ appliedFilterText }}</view>
        <text class="notice-action" @tap="loadTimeline">重试</text>
      </view>
    </view>

    <view v-if="filterPanelVisible" class="filter-layer" @tap="closeFilterPanel">
      <view class="filter-sheet" @tap.stop>
        <view class="filter-sheet-head">
          <view class="filter-sheet-title">筛选年份</view>
          <view class="filter-sheet-close" @tap="closeFilterPanel">收起</view>
        </view>
        <view class="filter-input-wrap" :class="{ focused: yearInputFocused }">
          <view class="icon icon-search icon-search-soft" />
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
        <view class="filter-sheet-meta">当前：{{ appliedFilterText }} · {{ flatCount }} 则</view>
        <view class="filter-actions">
          <view class="filter-action filter-action-ghost" @tap="resetYearFilter">全部年份</view>
          <view class="filter-action" @tap="submitYearFilter">确定</view>
        </view>
      </view>
    </view>

    <view class="corridor-shell">
      <view class="corridor-line" />

      <view v-if="loading" class="corridor-content">
        <view class="state-card state-card-loading">
          <view class="state-title">正在归位</view>
          <view class="state-desc">时间片段整理中</view>
        </view>

        <view v-for="groupIndex in 2" :key="`loading-${groupIndex}`" class="loading-group">
          <view class="group-head group-head-loading">
            <view class="group-knot" />
            <view class="skeleton skeleton-group-label" />
          </view>

          <view v-for="nodeIndex in 2" :key="`loading-${groupIndex}-${nodeIndex}`" class="loading-node">
            <view class="node-pin node-pin-loading" />
            <view class="loading-card">
              <view class="skeleton skeleton-kicker" />
              <view class="skeleton skeleton-title" />
              <view class="skeleton skeleton-meta" />
            </view>
          </view>
        </view>
      </view>

      <view v-else-if="showLoadFailureState" class="corridor-content">
        <view class="state-card">
          <view class="state-title">暂时没有展开</view>
          <view class="state-desc">网络稍慢，请再试一次</view>
          <view class="state-action" @tap="loadTimeline">重新整理</view>
        </view>
      </view>

      <view v-else-if="showEmptyState" class="corridor-content">
        <view class="state-card state-card-empty">
          <view class="state-title">这一段还很安静</view>
          <view class="state-desc">{{ emptyStateText }}</view>
          <view class="state-action state-action-soft" @tap="loadTimeline">刷新</view>
        </view>
      </view>

      <view v-else class="corridor-content">
        <view v-for="group in decoratedTimelineGroups" :key="group.yearMonth" class="group-section">
          <view class="group-head">
            <view class="group-knot" />
            <view class="group-copy">
              <view class="group-label">{{ group.yearMonth }}</view>
              <view class="group-meta">{{ group.itemCountText }}</view>
            </view>
          </view>

          <view class="node-list">
            <view
              v-for="item in group.items"
              :key="item.id"
              class="hall-node"
              :class="[`hall-node-${item.kind}`]"
              @tap="openTimelineNode(item.raw)"
            >
              <view class="node-pin" :class="[`node-pin-${item.kind}`]" />

              <view v-if="item.kind === 'sealed'" class="node-body node-body-sealed">
                <view class="sealed-row">
                  <text class="sealed-date">{{ item.dateText }}</text>
                  <text class="sealed-status">{{ item.statusText }}</text>
                </view>
                <view class="sealed-title">{{ item.title }}</view>
              </view>

              <view v-else-if="item.kind === 'draft'" class="node-body node-body-draft">
                <view class="draft-row">
                  <text class="draft-date">{{ item.dateText }}</text>
                  <text class="draft-status">{{ item.statusText }}</text>
                </view>
                <view class="draft-title">{{ item.title }}</view>
              </view>

              <view v-else class="node-body node-body-unlocked">
                <view class="unlocked-head">
                  <text class="unlocked-date">{{ item.dateText }}</text>
                  <text class="unlocked-kicker">{{ item.statusText }}</text>
                </view>
                <view class="unlocked-title">{{ item.title }}</view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="tail">
      <view class="tail-line" />
      <view class="tail-text">写下的片刻，会留在这里。</view>
    </view>

    <BottomNavBar current="timeline" />
  </view>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  padding: 20rpx 40rpx 260rpx;
  overflow: hidden;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, #f7f9fb 0%, #f2f5f7 52%, #eef2f4 100%);
}

.page-glow {
  position: absolute;
  border-radius: 999rpx;
  pointer-events: none;
  filter: blur(14rpx);
}

.page-glow-top {
  top: 64rpx;
  right: -80rpx;
  width: 260rpx;
  height: 260rpx;
  background: rgba(243, 208, 148, 0.16);
}

.page-glow-bottom {
  left: -60rpx;
  bottom: 280rpx;
  width: 220rpx;
  height: 220rpx;
  background: rgba(134, 156, 170, 0.12);
}

.top-bar {
  position: relative;
  z-index: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8rpx;
}

.top-icon-btn,
.top-bar-side {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
}

.top-icon-btn {
  background: rgba(255, 255, 255, 0.58);
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.65) inset,
    0 10rpx 24rpx rgba(80, 98, 110, 0.08);
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
  position: relative;
  z-index: 1;
  margin-top: 54rpx;
}

.hero-title {
  font-size: 80rpx;
  line-height: 1.08;
  font-weight: 700;
  color: #111418;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.hero-meta-row {
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.hero-filter-pill {
  min-height: 68rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(163, 175, 183, 0.18);
  display: flex;
  align-items: center;
  gap: 12rpx;
  box-shadow: 0 10rpx 22rpx rgba(72, 95, 111, 0.05);
}

.hero-filter-text {
  font-size: 24rpx;
  color: #556169;
  letter-spacing: 1rpx;
}

.hero-count {
  font-size: 24rpx;
  color: #97a1a7;
}

.filter-layer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 12;
  background: rgba(18, 22, 26, 0.18);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 124rpx 32rpx 0;
}

.filter-sheet {
  width: 100%;
  padding: 30rpx;
  border-radius: 34rpx;
  background: rgba(252, 252, 250, 0.96);
  box-shadow: 0 24rpx 60rpx rgba(34, 40, 45, 0.16);
  backdrop-filter: blur(14rpx);
}

.filter-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.filter-sheet-title {
  font-size: 34rpx;
  color: #191d21;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.filter-sheet-close {
  font-size: 24rpx;
  color: #8a949a;
}

.filter-input-wrap.focused {
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(84, 113, 131, 0.26);
}

.filter-input-wrap {
  margin-top: 24rpx;
  height: 88rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: rgba(241, 244, 246, 0.92);
  border: 1rpx solid rgba(165, 176, 183, 0.18);
  display: flex;
  align-items: center;
  gap: 16rpx;
  transition: all 0.2s ease;
}

.year-filter {
  flex: 1;
  height: 100%;
  font-size: 28rpx;
  color: #1a1a1a;
}

.filter-sheet-meta {
  margin-top: 18rpx;
  font-size: 23rpx;
  color: #94a0a6;
}

.filter-actions {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
}

.filter-action {
  flex: 1;
  min-height: 84rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: #3a4a55;
  color: #ffffff;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 22rpx rgba(58, 74, 85, 0.18);
}

.filter-action-ghost {
  background: rgba(239, 243, 246, 0.96);
  color: #62717a;
  box-shadow: none;
}

.notice-panel {
  margin-top: 24rpx;
  padding: 20rpx 24rpx;
  border-radius: 24rpx;
  background: rgba(244, 236, 220, 0.66);
}

.notice-title {
  font-size: 26rpx;
  color: #4f4633;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.notice-desc {
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #7a705e;
}

.notice-action {
  margin-top: 12rpx;
  display: inline-flex;
  color: #5a6f7e;
  font-size: 24rpx;
}

.corridor-shell {
  position: relative;
  margin-top: 44rpx;
  padding-left: 80rpx;
}

.corridor-line {
  position: absolute;
  left: 34rpx;
  top: 12rpx;
  bottom: 34rpx;
  width: 2rpx;
  background: linear-gradient(180deg, rgba(169, 179, 186, 0.15) 0%, rgba(130, 146, 156, 0.46) 40%, rgba(169, 179, 186, 0.12) 100%);
}

.corridor-content {
  position: relative;
  z-index: 1;
}

.state-card {
  position: relative;
  margin-left: 6rpx;
  padding: 34rpx 34rpx 32rpx;
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.82);
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.76) inset,
    0 18rpx 40rpx rgba(72, 95, 111, 0.08);
  backdrop-filter: blur(10rpx);
}

.state-card::before {
  content: '';
  position: absolute;
  left: -58rpx;
  top: 42rpx;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: #8fa0aa;
  box-shadow: 0 0 0 10rpx rgba(255, 255, 255, 0.92);
}

.state-card-loading {
  margin-bottom: 38rpx;
}

.state-card-empty {
  background: rgba(250, 246, 239, 0.8);
}

.state-title {
  font-size: 40rpx;
  line-height: 1.3;
  color: #1a1a1a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.state-desc {
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #7f8c93;
}

.state-action {
  margin-top: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 74rpx;
  padding: 0 30rpx;
  border-radius: 999rpx;
  background: #3a4a55;
  color: #ffffff;
  font-size: 26rpx;
}

.state-action-soft {
  background: #efe4cb;
  color: #6d5a32;
}

.loading-group {
  margin-top: 34rpx;
}

.group-section + .group-section {
  margin-top: 52rpx;
}

.group-head,
.loading-node,
.hall-node {
  position: relative;
}

.group-head {
  margin-left: 6rpx;
  min-height: 56rpx;
}

.group-copy {
  padding-left: 26rpx;
}

.group-knot {
  position: absolute;
  left: -58rpx;
  top: 10rpx;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: #d9c18c;
  box-shadow: 0 0 0 10rpx rgba(255, 255, 255, 0.92);
}

.group-label {
  font-size: 42rpx;
  line-height: 1.2;
  color: #1a1a1a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.group-meta {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #a8b0b5;
}

.node-list {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.hall-node {
  padding-left: 6rpx;
}

.node-pin {
  position: absolute;
  left: -52rpx;
  top: 30rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  box-shadow: 0 0 0 10rpx rgba(255, 255, 255, 0.9);
}

.node-pin-unlocked {
  background: #e4b55c;
  box-shadow: 0 0 0 10rpx rgba(255, 250, 243, 0.92), 0 0 20rpx rgba(228, 181, 92, 0.28);
}

.node-pin-sealed {
  background: rgba(182, 192, 197, 0.72);
}

.node-pin-draft {
  background: rgba(108, 124, 134, 0.62);
}

.node-pin-loading {
  background: #d4dde1;
}

.node-body {
  position: relative;
}

.node-body-sealed {
  padding: 16rpx 6rpx 14rpx 0;
  opacity: 0.74;
}

.sealed-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.sealed-status,
.sealed-date {
  font-size: 22rpx;
  color: #a3adb2;
}

.sealed-title {
  margin-top: 10rpx;
  font-size: 30rpx;
  line-height: 1.45;
  color: #5f696f;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.node-body-draft {
  padding: 22rpx 24rpx 24rpx;
  border-radius: 30rpx;
  background: rgba(241, 244, 246, 0.76);
  box-shadow: 0 10rpx 24rpx rgba(72, 95, 111, 0.04);
}

.draft-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.draft-status,
.draft-date {
  font-size: 22rpx;
  color: #758188;
}

.draft-title {
  margin-top: 16rpx;
  font-size: 32rpx;
  line-height: 1.42;
  color: #293136;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.node-body-unlocked {
  padding: 30rpx 30rpx 34rpx;
  border-radius: 36rpx;
  background:
    radial-gradient(circle at right top, rgba(239, 205, 145, 0.2) 0%, rgba(239, 205, 145, 0) 36%),
    linear-gradient(180deg, rgba(255, 253, 249, 0.98) 0%, rgba(248, 240, 225, 0.98) 100%);
  border: 1rpx solid rgba(214, 183, 127, 0.22);
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.82) inset,
    0 24rpx 52rpx rgba(116, 100, 67, 0.12);
}

.node-body-unlocked::after {
  content: '';
  position: absolute;
  right: 26rpx;
  top: 22rpx;
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(240, 204, 140, 0.22) 0%, rgba(240, 204, 140, 0) 72%);
  pointer-events: none;
}

.unlocked-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.unlocked-kicker {
  min-height: 42rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  font-size: 21rpx;
  letter-spacing: 2rpx;
  color: #8c6a28;
  display: inline-flex;
  align-items: center;
}

.unlocked-date {
  font-size: 22rpx;
  color: #947844;
}

.unlocked-title {
  position: relative;
  margin-top: 14rpx;
  font-size: 42rpx;
  line-height: 1.32;
  color: #1c2125;
  font-weight: 600;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
  z-index: 1;
}

.group-head-loading {
  min-height: 44rpx;
}

.loading-node + .loading-node {
  margin-top: 20rpx;
}

.loading-card {
  margin-left: 6rpx;
  padding: 28rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 14rpx 32rpx rgba(72, 95, 111, 0.05);
}

.skeleton {
  border-radius: 999rpx;
  background: linear-gradient(90deg, rgba(231, 236, 239, 0.82) 0%, rgba(243, 246, 248, 0.98) 50%, rgba(231, 236, 239, 0.82) 100%);
}

.skeleton-group-label {
  width: 180rpx;
  height: 36rpx;
  margin-left: 26rpx;
}

.skeleton-kicker {
  width: 200rpx;
  height: 24rpx;
}

.skeleton-title {
  width: 100%;
  max-width: 420rpx;
  height: 36rpx;
  margin-top: 18rpx;
}

.skeleton-meta {
  width: 280rpx;
  height: 24rpx;
  margin-top: 18rpx;
}

.tail {
  position: relative;
  margin-top: 48rpx;
  padding-left: 84rpx;
}

.tail-line {
  position: absolute;
  left: 34rpx;
  top: 0;
  width: 2rpx;
  height: 88rpx;
  background: linear-gradient(180deg, rgba(169, 179, 186, 0.32) 0%, rgba(169, 179, 186, 0) 100%);
}

.tail-text {
  max-width: 560rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: #a0a8ae;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.icon {
  width: 36rpx;
  height: 36rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  flex-shrink: 0;
}

.icon-search {
  width: 34rpx;
  height: 34rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><circle cx='11' cy='11' r='7'/><line x1='21' y1='21' x2='16.65' y2='16.65'/></svg>");
}

.icon-search-soft {
  width: 30rpx;
  height: 30rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2389979f' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><circle cx='11' cy='11' r='7'/><line x1='21' y1='21' x2='16.65' y2='16.65'/></svg>");
}

</style>
