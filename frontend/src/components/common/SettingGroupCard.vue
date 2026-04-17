<script setup lang="ts">
export interface SettingItem {
  key: string
  title: string
  subtitle?: string
}

const props = defineProps<{
  title: string
  items: SettingItem[]
}>()

const emit = defineEmits<{
  (event: 'itemTap', key: string): void
}>()

const onTap = (key: string) => emit('itemTap', key)
</script>

<template>
  <view class="group">
    <view class="group-title">{{ props.title }}</view>
    <view class="item" v-for="item in props.items" :key="item.key" @tap="onTap(item.key)">
      <view>
        <view class="item-title">{{ item.title }}</view>
        <view v-if="item.subtitle" class="item-subtitle">{{ item.subtitle }}</view>
      </view>
      <text class="arrow">›</text>
    </view>
  </view>
</template>

<style scoped>
.group {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(26, 26, 26, 0.06);
}

.group-title {
  font-size: 28rpx;
  color: #7f8c93;
  margin-bottom: 12rpx;
}

.item {
  min-height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #eef2f4;
}

.item:last-child {
  border-bottom: none;
}

.item-title {
  font-size: 32rpx;
  color: #1a1a1a;
}

.item-subtitle {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: #7f8c93;
}

.arrow {
  color: #7f8c93;
  font-size: 30rpx;
}
</style>

