<script setup lang="ts">
import { computed } from 'vue'
import { useWechatNavMetrics } from '../../../composables/useWechatNavMetrics'

const props = withDefaults(
  defineProps<{
    volNo?: string
  }>(),
  {
    volNo: 'Vol. 01',
  }
)

const emit = defineEmits<{
  (event: 'close'): void
}>()

const { cssVars, navBarHeight, navBarTotalHeight, rightSafeWidth, statusBarHeight } =
  useWechatNavMetrics()

const wrapperStyle = computed(() => ({
  ...cssVars.value,
  minHeight: `${navBarTotalHeight.value}px`,
  paddingTop: `${statusBarHeight.value}px`,
}))

const navStyle = computed(() => ({
  height: `${navBarHeight.value}px`,
}))

const closeRailStyle = computed(() => ({
  left: '56rpx',
}))

const onClose = () => {
  emit('close')
}
</script>

<template>
  <view class="immersive-editor-top-bar" :style="wrapperStyle">
    <view class="immersive-editor-top-bar__mist" aria-hidden="true" />

    <view class="immersive-editor-top-bar__nav" :style="navStyle">
      <view class="immersive-editor-top-bar__close-hit" :style="closeRailStyle" @tap="onClose">
        <view class="immersive-editor-top-bar__close-icon">
          <view class="immersive-editor-top-bar__close-line immersive-editor-top-bar__close-line-a" />
          <view class="immersive-editor-top-bar__close-line immersive-editor-top-bar__close-line-b" />
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.immersive-editor-top-bar {
  position: relative;
  z-index: 3;
}

.immersive-editor-top-bar__mist {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(110% 140% at 50% 0%, rgba(245, 240, 232, 0.72) 0%, rgba(245, 240, 232, 0) 58%),
    linear-gradient(180deg, rgba(237, 232, 220, 0.96) 0%, rgba(237, 232, 220, 0) 100%);
}

.immersive-editor-top-bar__nav {
  position: relative;
}

.immersive-editor-top-bar__vol {
  position: absolute;
  left: 40rpx;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  gap: 18rpx;
  max-width: 280rpx;
}

.immersive-editor-top-bar__vol-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 24rpx;
  letter-spacing: 0.05em;
  color: #9e9890;
  font-style: italic;
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
}

.immersive-editor-top-bar__vol-rule {
  width: 48rpx;
  height: 1rpx;
  background: rgba(200, 194, 184, 0.6);
}

.immersive-editor-top-bar__close-hit {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.34);
  border: 1rpx solid rgba(138, 149, 160, 0.12);
}

.immersive-editor-top-bar__close-icon {
  position: relative;
  width: 28rpx;
  height: 28rpx;
}

.immersive-editor-top-bar__close-line {
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 1rpx;
  border-radius: 999rpx;
  background: #c8c2b8;
}

.immersive-editor-top-bar__close-line-a {
  transform: translateY(-50%) rotate(45deg);
}

.immersive-editor-top-bar__close-line-b {
  transform: translateY(-50%) rotate(-45deg);
}
</style>
