<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTopBar from '../../components/common/AppTopBar.vue'
import TimelineNode from '../../components/common/TimelineNode.vue'
import { recordService } from '../../services'
import { RecordStatus, type TimelineGroupVO, type TimelineItemVO } from '../../types'
import { formatDateTime, getToken } from '../../utils'

const loading = ref(false)
const timelineGroups = ref<TimelineGroupVO[]>([])
const yearInput = ref('')

const flatCount = computed(() => timelineGroups.value.reduce((sum, group) => sum + group.items.length, 0))

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

const loadTimeline = async () => {
  if (!ensureLogin()) {
    return
  }
  loading.value = true
  try {
    const year = Number(yearInput.value)
    const result = await recordService.getTimeline(Number.isNaN(year) ? {} : { year })
    timelineGroups.value = result
  } catch {
    timelineGroups.value = []
  } finally {
    loading.value = false
  }
}

onShow(loadTimeline)
</script>

<template>
  <view class="page">
    <AppTopBar title="Flashback" left-text="⌕" right-text="筛选" @right-tap="loadTimeline" />

    <view class="hero">
      <view class="hero-title">时间长廊</view>
      <view class="hero-desc">把每一段记录按时间纵向铺开，看到自己如何一步步走到今天。</view>
      <input v-model="yearInput" class="year-filter" type="number" placeholder="按年份筛选，如 2026" @confirm="loadTimeline" />
      <view class="hero-meta">共 {{ flatCount }} 条记录</view>
    </view>

    <view v-if="loading" class="state">载入中...</view>
    <view v-else-if="timelineGroups.length === 0" class="state">时间轴暂时为空</view>

    <view v-else class="group-list">
      <view class="group" v-for="group in timelineGroups" :key="group.yearMonth">
        <view class="group-title">{{ group.yearMonth }}</view>
        <view class="node-list">
          <TimelineNode
            v-for="(item, index) in group.items"
            :key="item.id"
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

    <view class="tail">You can only understand time by walking through it.</view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx var(--fb-space-page) 180rpx;
  background: var(--fb-color-bg);
}

.hero {
  margin-top: 22rpx;
}

.hero-title {
  font-size: var(--fb-font-title-main);
  color: var(--fb-color-text);
  font-weight: 600;
}

.hero-desc {
  margin-top: 10rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-body-sub);
  line-height: 1.8;
}

.year-filter {
  margin-top: 16rpx;
  background: var(--fb-color-surface);
  border-radius: var(--fb-radius-pill);
  padding: 0 24rpx;
  height: 76rpx;
  font-size: var(--fb-font-body-sub);
}

.hero-meta {
  margin-top: 10rpx;
  font-size: var(--fb-font-meta);
  color: var(--fb-color-text-muted);
}

.state {
  margin-top: 30rpx;
  text-align: center;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}

.group-list {
  margin-top: 26rpx;
  display: flex;
  flex-direction: column;
  gap: 42rpx;
}

.group-title {
  font-size: var(--fb-font-title-sub);
  color: var(--fb-color-primary);
  margin-bottom: 16rpx;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.tail {
  margin-top: 34rpx;
  text-align: center;
  color: #a8b2b7;
  font-size: var(--fb-font-meta);
}
</style>
