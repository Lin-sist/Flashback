<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTopBar from '../../components/common/AppTopBar.vue'
import BottomNavBar from '../../components/common/BottomNavBar.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import TimelineNode from '../../components/common/TimelineNode.vue'
import { recordService } from '../../services'
import { RecordStatus, type TimelineGroupVO, type TimelineItemVO } from '../../types'
import { formatDateTime, getToken } from '../../utils'

const loading = ref(false)
const timelineGroups = ref<TimelineGroupVO[]>([])
const yearInput = ref('')
const appliedYear = ref('')
const timelineLoadFailed = ref(false)

const flatCount = computed(() => timelineGroups.value.reduce((sum, group) => sum + group.items.length, 0))
const hasAppliedYearFilter = computed(() => Boolean(appliedYear.value))
const showLoadFailureState = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length === 0)
const showEmptyState = computed(() => !loading.value && !timelineLoadFailed.value && timelineGroups.value.length === 0)
const showStaleNotice = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length > 0)
const appliedFilterText = computed(() => hasAppliedYearFilter.value ? `${appliedYear.value} 年` : '全部年份')
const emptyStateText = computed(() => hasAppliedYearFilter.value ? '这一年还没有记录' : '时间轴暂时为空，去写下第一条记忆吧')

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

const resolveVariant = (item: TimelineItemVO) => {
  if (item.status === RecordStatus.UNLOCKED) {
    return 'warm'
  }
  if (item.status === RecordStatus.SEALED) {
    return 'highlight'
  }
  return 'text'
}

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
    <AppTopBar title="Flashback" right-text="筛选" @right-tap="loadTimeline" />

    <view class="hero">
      <view class="hero-title">时间长廊</view>
      <view class="hero-desc">把每一段记录按时间纵向铺开，看到自己如何一步步走到今天。</view>
      <input v-model="yearInput" class="year-filter" type="number" placeholder="按年份筛选，如 2026" @confirm="loadTimeline" />
      <view class="hero-meta">当前展示：{{ appliedFilterText }} · 共 {{ flatCount }} 条记录</view>
    </view>

    <view v-if="showStaleNotice" class="inline-error">
      筛选加载失败，当前展示的是 {{ appliedFilterText }} 的时间轴
      <text class="inline-retry" @tap="loadTimeline">重试</text>
    </view>

    <view v-if="loading" class="state">正在加载时间轴...</view>
    <view v-else-if="showLoadFailureState" class="state-wrap">
      <EmptyState text="网络有点慢，时间轴暂时没加载出来" />
      <PrimaryButton text="重试加载" ghost @tap="loadTimeline" />
    </view>
    <view v-else-if="showEmptyState" class="state-wrap">
      <EmptyState :text="emptyStateText" />
      <PrimaryButton text="刷新时间轴" ghost @tap="loadTimeline" />
    </view>

    <view v-else class="group-list">
      <view class="group" v-for="group in timelineGroups" :key="group.yearMonth">
        <view class="group-title">{{ group.yearMonth }}</view>
        <view class="node-list">
          <view v-for="(item, index) in group.items" :key="item.id" class="node-item" @tap="openTimelineNode(item)">
            <TimelineNode
              :title="item.title"
              :subtitle="item.recordType"
              :date-text="formatDateTime(item.createdAt)"
              :tags="item.tagNames"
              :status="item.status"
              :record-type="item.recordType"
              :variant="resolveVariant(item)"
              :show-line="index < group.items.length - 1"
            />
          </view>
        </view>
      </view>
    </view>

    <view class="tail">You can only understand time by walking through it.</view>

    <BottomNavBar current="timeline" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 24rpx 180rpx;
  background: #f8fafb;
}

.hero {
  margin-top: 22rpx;
}

.hero-title {
  font-size: 52rpx;
  color: #1a1a1a;
  font-weight: 600;
}

.hero-desc {
  margin-top: 10rpx;
  color: #7f8c93;
  font-size: 28rpx;
  line-height: 1.8;
}

.year-filter {
  margin-top: 16rpx;
  background: #ffffff;
  border-radius: 999rpx;
  padding: 0 24rpx;
  height: 76rpx;
  font-size: 28rpx;
}

.hero-meta {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #7f8c93;
}

.state {
  margin-top: 30rpx;
  text-align: center;
  color: #7f8c93;
  font-size: 24rpx;
}

.state-wrap {
  margin-top: 20rpx;
}

.inline-error {
  margin-top: 14rpx;
  color: #7f8c93;
  font-size: 24rpx;
}

.inline-retry {
  margin-left: 10rpx;
  color: #3b647a;
}

.group-list {
  margin-top: 26rpx;
  display: flex;
  flex-direction: column;
  gap: 42rpx;
}

.group-title {
  font-size: 36rpx;
  color: #3b647a;
  margin-bottom: 16rpx;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.node-item {
  width: 100%;
}

.tail {
  margin-top: 34rpx;
  text-align: center;
  color: #a8b2b7;
  font-size: 24rpx;
}
</style>

