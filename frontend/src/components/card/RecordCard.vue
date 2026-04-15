<script setup lang="ts">
import { mapRecordStatus, mapRecordType } from '../../utils'
import type { RecordListItemVO } from '../../types'

defineProps<{
  item: RecordListItemVO
}>()
</script>

<template>
  <view class="record-card">
    <view class="record-title">{{ item.title || 'Untitled' }}</view>
    <view class="record-meta">
      <text>{{ mapRecordType(item.recordType) }}</text>
      <text>{{ mapRecordStatus(item.status) }}</text>
    </view>
    <view class="record-preview">{{ item.contentPreview }}</view>
  </view>
</template>

<style scoped>
.record-card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(16, 24, 40, 0.06);
}

.record-title {
  font-size: 30rpx;
  font-weight: 600;
}

.record-meta {
  margin-top: 10rpx;
  color: #667085;
  font-size: 24rpx;
  display: flex;
  gap: 16rpx;
}

.record-preview {
  margin-top: 16rpx;
  color: #475467;
  font-size: 26rpx;
  line-height: 1.5;
}
</style>
