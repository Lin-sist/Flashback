<script setup lang="ts">
defineProps<{
  text?: string
}>()
</script>

<template>
  <view class="empty-state">
    <text class="empty-text">{{ text || 'No data yet' }}</text>
  </view>
</template>

<style scoped>
.empty-state {
  padding: 48rpx 24rpx;
  text-align: center;
}

.empty-text {
  color: #98a2b3;
  font-size: 26rpx;
}
</style>
