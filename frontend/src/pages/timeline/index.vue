<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import EmptyState from '../../components/common/EmptyState.vue'
import { recordService } from '../../services'
import type { RecordListItemVO } from '../../types'
import { formatDateTime, getToken } from '../../utils'

const list = ref<RecordListItemVO[]>([])
const loading = ref(false)

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadTimeline = async () => {
  if (!ensureLogin()) {
    return
  }
  loading.value = true
  try {
    const result = await recordService.getTimeline(1, 20)
    list.value = result.list
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onShow(() => {
  loadTimeline()
})
</script>

<template>
  <view class="page">
    <view v-if="loading" class="loading">Loading...</view>
    <view v-else-if="list.length === 0">
      <EmptyState text="No timeline data" />
    </view>
    <view v-else class="timeline-list">
      <view class="timeline-item" v-for="item in list" :key="item.id">
        <view class="dot"></view>
        <view class="line"></view>
        <view class="content">
          <view class="title">{{ item.title || 'Untitled' }}</view>
          <view class="time">{{ formatDateTime(item.createdAt) }}</view>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.loading {
  color: #667085;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.timeline-item {
  position: relative;
  padding-left: 36rpx;
  background: #ffffff;
  border-radius: 12rpx;
  padding-top: 18rpx;
  padding-bottom: 18rpx;
  padding-right: 18rpx;
}

.dot {
  position: absolute;
  left: 12rpx;
  top: 22rpx;
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #0ea5e9;
}

.line {
  position: absolute;
  left: 16rpx;
  top: 34rpx;
  bottom: -14rpx;
  width: 2rpx;
  background: #d0d5dd;
}

.timeline-item:last-child .line {
  display: none;
}

.title {
  font-size: 28rpx;
  font-weight: 600;
}

.time {
  margin-top: 6rpx;
  color: #667085;
  font-size: 24rpx;
}
</style>
