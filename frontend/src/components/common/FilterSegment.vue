<script setup lang="ts">
export interface SegmentOption {
  label: string
  value: string
}

const props = defineProps<{
  modelValue: string
  options: SegmentOption[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'change', value: string): void
}>()

const select = (value: string) => {
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<template>
  <view class="segment">
    <view
      class="item"
      :class="{ active: option.value === props.modelValue }"
      v-for="option in props.options"
      :key="option.value"
      @tap="select(option.value)"
    >
      {{ option.label }}
    </view>
  </view>
</template>

<style scoped>
.segment {
  display: flex;
  align-items: center;
  background: #edf2f5;
  border-radius: 999rpx;
  padding: 6rpx;
  gap: 6rpx;
  overflow-x: auto;
}

.item {
  min-width: 120rpx;
  text-align: center;
  border-radius: 999rpx;
  padding: 12rpx 20rpx;
  color: #7f8c93;
  font-size: 24rpx;
  white-space: nowrap;
}

.item.active {
  color: #1a1a1a;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(26, 26, 26, 0.06);
}
</style>

