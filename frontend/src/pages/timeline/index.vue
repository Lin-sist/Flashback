<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import BottomNavBar from '../../components/common/BottomNavBar.vue'
import { recordService } from '../../services'
import { RecordStatus, RecordType, type TimelineGroupVO, type TimelineItemVO } from '../../types'
import { formatDateTime, getToken } from '../../utils'

type CorridorNodeKind = 'sealed' | 'unlocked' | 'draft'

interface DecoratedTimelineItem {
  id: number
  raw: TimelineItemVO
  title: string
  kind: CorridorNodeKind
  statusText: string
  typeLabel: string
  typeHint: string
  dateText: string
  primaryTags: string[]
}

const loading = ref(false)
const timelineGroups = ref<TimelineGroupVO[]>([])
const yearInput = ref('')
const appliedYear = ref('')
const timelineLoadFailed = ref(false)
const yearInputFocused = ref(false)

const flatCount = computed(() => timelineGroups.value.reduce((sum, group) => sum + group.items.length, 0))
const hasAppliedYearFilter = computed(() => Boolean(appliedYear.value))
const showLoadFailureState = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length === 0)
const showEmptyState = computed(() => !loading.value && !timelineLoadFailed.value && timelineGroups.value.length === 0)
const showStaleNotice = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length > 0)
const appliedFilterText = computed(() => hasAppliedYearFilter.value ? `${appliedYear.value} 年` : '全部年份')
const emptyStateText = computed(() => hasAppliedYearFilter.value ? '这一年还没有留下新的片段' : '时间长廊还没有展开第一段记忆')
const corridorSummaryText = computed(() => {
  if (loading.value) {
    return '旧日片段正在沿着时间重新归位。'
  }

  if (showLoadFailureState.value) {
    return '这一次整理没有完成，稍后再试，时间会把它们重新带回来。'
  }

  if (showEmptyState.value) {
    return hasAppliedYearFilter.value
      ? `正在寻找 ${appliedYear.value} 年的回响，目前还没有新的记录停驻在这里。`
      : '沿着纵线向下看去，第一段被写下的时刻还在等待出现。'
  }

  if (hasAppliedYearFilter.value) {
    return `此刻陈列的是 ${appliedYear.value} 年留下的 ${flatCount.value} 段时间纹理。`
  }

  return flatCount.value > 0
    ? `共整理出 ${flatCount.value} 段被认真写下的时刻，沿着时间向下缓缓展开。`
    : '把每一段经过都交还给时间，长廊会从这里继续生长。'
})

const recordTypeMetaMap: Record<RecordType, { label: string, hint: string }> = {
  [RecordType.FUTURE_LETTER]: {
    label: '写给未来',
    hint: '留给未来的轻声问候',
  },
  [RecordType.NODE_RECORD]: {
    label: '阶段印记',
    hint: '某个阶段被认真按下保存',
  },
  [RecordType.EMOTION_NOTE]: {
    label: '心绪小记',
    hint: '当时的情绪被轻轻放进这里',
  },
}

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

const focusYearInput = () => {
  yearInputFocused.value = true
}

const blurYearInput = () => {
  yearInputFocused.value = false
}

const submitYearFilter = () => {
  yearInputFocused.value = false
  loadTimeline()
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
    return '已抵达此刻'
  }

  if (status === RecordStatus.SEALED) {
    return '暂未开启'
  }

  return '此时此刻'
}

const decoratedTimelineGroups = computed(() =>
  timelineGroups.value.map((group) => ({
    yearMonth: group.yearMonth,
    itemCountText: `${group.items.length} 段记忆停驻于此`,
    items: group.items.map<DecoratedTimelineItem>((item) => {
      const typeMeta = recordTypeMetaMap[item.recordType] ?? {
        label: '未命名片段',
        hint: '被写下的一段时间',
      }

      return {
        id: item.id,
        raw: item,
        title: item.title?.trim() || '未命名片段',
        kind: resolveNodeKind(item.status),
        statusText: resolveStatusText(item.status),
        typeLabel: typeMeta.label,
        typeHint: typeMeta.hint,
        dateText: formatDateTime(item.createdAt),
        primaryTags: item.tagNames?.length ? item.tagNames.slice(0, 3) : [typeMeta.label],
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
      <view class="top-icon-btn" @tap="focusYearInput">
        <view class="icon icon-search" />
      </view>
      <view class="brand">时光回序</view>
      <view class="top-bar-side" />
    </view>

    <view class="hero">
      <view class="hero-kicker">TIMELINE MEMORY WALK</view>
      <view class="hero-title">时间长廊</view>
      <view class="hero-desc">沿着时间留下的纹理，翻看那些被写下、被封存，也被重新照亮的时刻。</view>

      <view class="filter-card">
        <view class="filter-card-head">
          <text class="filter-label">年份检索</text>
          <text class="filter-meta">{{ appliedFilterText }} · {{ flatCount }} 条片段</text>
        </view>

        <view class="filter-row">
          <view class="filter-input-wrap" :class="{ focused: yearInputFocused }">
            <view class="icon icon-search icon-search-soft" />
            <input
              v-model="yearInput"
              class="year-filter"
              type="number"
              confirm-type="search"
              placeholder="按年份筛选，如 2026"
              :focus="yearInputFocused"
              @confirm="submitYearFilter"
              @blur="blurYearInput"
            />
          </view>
          <view class="filter-action" @tap="submitYearFilter">整理</view>
        </view>

        <view class="filter-tip">{{ corridorSummaryText }}</view>
      </view>

      <view v-if="showStaleNotice" class="notice-panel">
        <view class="notice-title">时间回声未完全同步</view>
        <view class="notice-desc">筛选刷新没有成功，当前仍展示 {{ appliedFilterText }} 的时间长廊。</view>
        <text class="notice-action" @tap="loadTimeline">重新整理</text>
      </view>
    </view>

    <view class="corridor-shell">
      <view class="corridor-line" />

      <view v-if="loading" class="corridor-content">
        <view class="state-card state-card-loading">
          <view class="state-kicker">ARRANGING MEMORIES</view>
          <view class="state-title">时间正在把片段归位</view>
          <view class="state-desc">长廊的轮廓已经亮起，正在把真实记录依次铺开。</view>
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
          <view class="state-kicker">TIMELINE RETRY</view>
          <view class="state-title">这一段长廊暂时没有展开</view>
          <view class="state-desc">网络有些慢，但登录校验、筛选入口和真实跳转规则都还在，重新整理即可继续查看。</view>
          <view class="state-action" @tap="loadTimeline">重新整理时间长廊</view>
        </view>
      </view>

      <view v-else-if="showEmptyState" class="corridor-content">
        <view class="state-card state-card-empty">
          <view class="state-kicker">FIRST MEMORY</view>
          <view class="state-title">这一段走廊此刻还很安静</view>
          <view class="state-desc">{{ emptyStateText }}</view>
          <view class="state-action state-action-soft" @tap="loadTimeline">刷新时间长廊</view>
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
                  <view class="icon icon-lock-muted" />
                  <view class="sealed-copy">
                    <view class="sealed-topline">
                      <text class="sealed-status">{{ item.statusText }}</text>
                      <text class="sealed-date">{{ item.dateText }}</text>
                    </view>
                    <view class="sealed-title">{{ item.title }}</view>
                    <view class="sealed-meta">{{ item.typeHint }}</view>
                  </view>
                </view>
              </view>

              <view v-else-if="item.kind === 'draft'" class="node-body node-body-draft">
                <view class="draft-capsule">
                  <text class="draft-capsule-text">{{ item.statusText }}</text>
                  <text class="draft-capsule-date">{{ item.dateText }}</text>
                </view>
                <view class="draft-title">{{ item.title }}</view>
                <view class="draft-meta">{{ item.typeHint }}</view>
              </view>

              <view v-else class="node-body node-body-unlocked">
                <view class="unlocked-head">
                  <text class="unlocked-kicker">{{ item.statusText }}</text>
                  <text class="unlocked-date">{{ item.dateText }}</text>
                </view>
                <view class="unlocked-title">{{ item.title }}</view>
                <view class="unlocked-type">{{ item.typeLabel }}</view>
                <view class="tag-row">
                  <text v-for="tag in item.primaryTags" :key="tag" class="tag-chip">{{ tag }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="tail">
      <view class="tail-line" />
      <view class="tail-text">时间不会停下，但被认真写下的片刻，会一直在这里发出微弱而清晰的光。</view>
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
  margin-top: 40rpx;
}

.hero-kicker {
  font-size: 20rpx;
  letter-spacing: 6rpx;
  color: #a5afb5;
}

.hero-title {
  margin-top: 18rpx;
  font-size: 76rpx;
  line-height: 1.12;
  font-weight: 700;
  color: #111418;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.hero-desc {
  margin-top: 24rpx;
  max-width: 620rpx;
  font-size: 28rpx;
  line-height: 1.8;
  color: #8f989e;
}

.filter-card {
  margin-top: 36rpx;
  padding: 28rpx 28rpx 26rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.76);
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.72) inset,
    0 18rpx 40rpx rgba(72, 95, 111, 0.08);
  backdrop-filter: blur(12rpx);
}

.filter-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.filter-label {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: #9aa3a9;
}

.filter-meta {
  font-size: 22rpx;
  color: #8b959b;
}

.filter-row {
  margin-top: 18rpx;
  display: flex;
  gap: 16rpx;
}

.filter-input-wrap {
  flex: 1;
  height: 84rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: rgba(241, 244, 246, 0.88);
  border: 1rpx solid rgba(165, 176, 183, 0.18);
  display: flex;
  align-items: center;
  gap: 16rpx;
  transition: all 0.2s ease;
}

.filter-input-wrap.focused {
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(84, 113, 131, 0.26);
}

.year-filter {
  flex: 1;
  height: 100%;
  font-size: 28rpx;
  color: #1a1a1a;
}

.filter-action {
  min-width: 132rpx;
  height: 84rpx;
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

.filter-tip {
  margin-top: 18rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: #8f989e;
}

.notice-panel {
  margin-top: 20rpx;
  padding: 24rpx 28rpx;
  border-radius: 28rpx;
  background: rgba(244, 236, 220, 0.78);
  box-shadow: 0 16rpx 36rpx rgba(118, 101, 65, 0.08);
}

.notice-title {
  font-size: 28rpx;
  color: #4f4633;
  font-weight: 600;
}

.notice-desc {
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.75;
  color: #7a705e;
}

.notice-action {
  margin-top: 16rpx;
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

.state-kicker {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: #9aa3a9;
}

.state-title {
  margin-top: 10rpx;
  font-size: 40rpx;
  line-height: 1.3;
  color: #1a1a1a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.state-desc {
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.85;
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
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.group-meta {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #99a2a8;
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
}

.node-pin-sealed {
  background: #b6c0c5;
}

.node-pin-draft {
  background: #6c7c86;
}

.node-pin-loading {
  background: #d4dde1;
}

.node-body {
  position: relative;
}

.node-body-sealed {
  padding: 18rpx 4rpx 18rpx 0;
}

.sealed-row {
  display: flex;
  align-items: flex-start;
  gap: 18rpx;
}

.sealed-copy {
  min-width: 0;
  flex: 1;
}

.sealed-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.sealed-status,
.sealed-date {
  font-size: 22rpx;
  color: #9ca5ab;
}

.sealed-title {
  margin-top: 10rpx;
  font-size: 30rpx;
  line-height: 1.5;
  color: #576268;
}

.sealed-meta {
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #a0a8ae;
}

.node-body-draft {
  padding: 24rpx 28rpx 26rpx;
  border-radius: 30rpx;
  background: rgba(239, 243, 246, 0.92);
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.68) inset,
    0 14rpx 32rpx rgba(72, 95, 111, 0.06);
}

.draft-capsule {
  display: inline-flex;
  align-items: center;
  gap: 16rpx;
  min-height: 54rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.8);
}

.draft-capsule-text,
.draft-capsule-date {
  font-size: 22rpx;
  color: #6f7a80;
}

.draft-title {
  margin-top: 16rpx;
  font-size: 34rpx;
  line-height: 1.4;
  color: #1f2529;
  font-weight: 600;
}

.draft-meta {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.75;
  color: #8b959b;
}

.node-body-unlocked {
  padding: 28rpx 28rpx 30rpx;
  border-radius: 34rpx;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(251, 245, 235, 0.94) 100%);
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.78) inset,
    0 20rpx 44rpx rgba(116, 100, 67, 0.08);
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
  font-size: 22rpx;
  letter-spacing: 2rpx;
  color: #8c6a28;
}

.unlocked-date {
  font-size: 22rpx;
  color: #947844;
}

.unlocked-title {
  position: relative;
  margin-top: 14rpx;
  font-size: 38rpx;
  line-height: 1.38;
  color: #1f2529;
  font-weight: 600;
  z-index: 1;
}

.unlocked-type {
  position: relative;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #8d7750;
  z-index: 1;
}

.tag-row {
  position: relative;
  margin-top: 18rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  z-index: 1;
}

.tag-chip {
  min-height: 48rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.82);
  color: #6f7a80;
  font-size: 22rpx;
  display: inline-flex;
  align-items: center;
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
  line-height: 1.9;
  color: #a0a8ae;
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

.icon-lock-muted {
  width: 28rpx;
  height: 28rpx;
  margin-top: 2rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23a8b1b6' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><rect x='5' y='11' width='14' height='10' rx='2'/><path d='M8 11V7a4 4 0 0 1 8 0v4'/></svg>");
}
</style>
