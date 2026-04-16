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
  background: var(--fb-color-surface);
  border-radius: var(--fb-radius-xl);
  padding: 24rpx;
  box-shadow: var(--fb-shadow-soft);
}

.group-title {
  font-size: var(--fb-font-body-sub);
  color: var(--fb-color-text-muted);
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
  font-size: var(--fb-font-body);
  color: var(--fb-color-text);
}

.item-subtitle {
  margin-top: 4rpx;
  font-size: var(--fb-font-meta);
  color: var(--fb-color-text-muted);
}

.arrow {
  color: var(--fb-color-text-muted);
  font-size: 30rpx;
}
</style>
