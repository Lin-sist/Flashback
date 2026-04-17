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
  bottom: calc(env(safe-area-inset-bottom) + var(--fb-bottom-nav-height) + 52rpx);
  width: 108rpx;
  height: 108rpx;
  border-radius: 54rpx;
  background: var(--fb-color-primary);
  box-shadow: var(--fb-shadow-strong);
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
