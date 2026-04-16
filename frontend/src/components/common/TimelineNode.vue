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
  background: var(--fb-color-primary);
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
  border-radius: var(--fb-radius-xl);
  padding: 24rpx;
  background: transparent;
}

.node.highlight {
  background: var(--fb-color-surface);
  box-shadow: var(--fb-shadow-soft);
}

.node.warm {
  background: var(--fb-color-paper-emphasis);
  box-shadow: var(--fb-shadow-soft);
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.date {
  font-size: var(--fb-font-meta);
  color: var(--fb-color-text-muted);
}

.status {
  font-size: var(--fb-font-meta);
  color: var(--fb-color-primary);
}

.title {
  margin-top: 10rpx;
  font-size: var(--fb-font-title-sub);
  color: var(--fb-color-text);
  font-weight: 600;
}

.subtitle {
  margin-top: 8rpx;
  font-size: var(--fb-font-body-sub);
  color: var(--fb-color-text-muted);
  line-height: 1.6;
}

.tags {
  margin-top: 14rpx;
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.tag {
  font-size: var(--fb-font-meta);
  color: var(--fb-color-text-muted);
}
</style>
