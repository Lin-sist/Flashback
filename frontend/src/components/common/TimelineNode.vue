<script setup lang="ts">
import type { RecordStatus, RecordType } from '../../types'

const props = defineProps<{
  title: string
  subtitle: string
  dateText: string
  tags?: string[]
  status: RecordStatus
  recordType: RecordType
  variant: 'text' | 'highlight' | 'warm'
  showLine?: boolean
}>()
</script>

<template>
  <view class="node-wrap">
    <view class="axis">
      <view class="dot"></view>
      <view v-if="props.showLine" class="line"></view>
    </view>
    <view class="node" :class="props.variant">
      <view class="meta-row">
        <text class="date">{{ props.dateText }}</text>
        <text class="status">{{ props.status }}</text>
      </view>
      <view class="title">{{ props.title || '未命名记录' }}</view>
      <view class="subtitle">{{ props.subtitle }}</view>
      <view v-if="props.tags?.length" class="tags">
        <text class="tag" v-for="tag in props.tags" :key="tag"># {{ tag }}</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.node-wrap {
  position: relative;
  display: flex;
  gap: 18rpx;
}

.axis {
  width: 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 9rpx;
  background: #3b647a;
  margin-top: 8rpx;
}

.line {
  width: 2rpx;
  flex: 1;
  margin-top: 8rpx;
  background: #d2d9dd;
}

.node {
  flex: 1;
  border-radius: 32rpx;
  padding: 24rpx;
  background: transparent;
}

.node.highlight {
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(26, 26, 26, 0.06);
}

.node.warm {
  background: #fdf2d9;
  box-shadow: 0 8rpx 24rpx rgba(26, 26, 26, 0.06);
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.date {
  font-size: 24rpx;
  color: #7f8c93;
}

.status {
  font-size: 24rpx;
  color: #3b647a;
}

.title {
  margin-top: 10rpx;
  font-size: 36rpx;
  color: #1a1a1a;
  font-weight: 600;
}

.subtitle {
  margin-top: 8rpx;
  font-size: 28rpx;
  color: #7f8c93;
  line-height: 1.6;
}

.tags {
  margin-top: 14rpx;
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.tag {
  font-size: 24rpx;
  color: #7f8c93;
}
</style>

