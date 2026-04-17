<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    text?: string
  }>(),
  {
    text: '+',
  }
)

const emit = defineEmits<{
  (event: 'tap'): void
}>()

const onTap = () => emit('tap')
</script>

<template>
  <view class="fab" @tap="onTap">
    <text class="fab-text">{{ props.text }}</text>
  </view>
</template>

<style scoped>
.fab {
  position: fixed;
  right: 32rpx;
  bottom: calc(env(safe-area-inset-bottom) + 108rpx + 52rpx);
  width: 108rpx;
  height: 108rpx;
  border-radius: 54rpx;
  background: #3b647a;
  box-shadow: 0 16rpx 40rpx rgba(59, 100, 122, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.fab-text {
  color: #ffffff;
  font-size: 48rpx;
  line-height: 1;
}
</style>

