<script setup lang="ts">
import { hasPreviewSession } from '../../features/preview/preview-session'

const visible = hasPreviewSession()
</script>

<template>
  <view v-if="visible" class="preview-mode-notice" aria-label="概念预览，示例数据，只读">
    概念预览 · 示例数据 · 只读
  </view>
</template>

<style scoped>
.preview-mode-notice {
  position: fixed;
  right: 24rpx;
  bottom: calc(132rpx + env(safe-area-inset-bottom));
  z-index: 1800;
  padding: 10rpx 18rpx;
  border: 1rpx solid rgba(156, 58, 45, 0.24);
  border-radius: 999rpx;
  background: rgba(250, 247, 242, 0.94);
  box-shadow: 0 8rpx 24rpx rgba(56, 42, 32, 0.1);
  color: rgba(126, 49, 40, 0.9);
  font-size: 20rpx;
  line-height: 1.4;
  letter-spacing: 0.06em;
  pointer-events: none;
}
</style>
