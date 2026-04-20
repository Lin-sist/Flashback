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
  right: `calc(${rightSafeWidth.value}px + 12px)`,
}))

const onClose = () => {
  emit('close')
}
</script>

<template>
  <view class="immersive-editor-top-bar" :style="wrapperStyle">
    <view class="immersive-editor-top-bar__mist" aria-hidden="true" />

    <view class="immersive-editor-top-bar__nav" :style="navStyle">
      <view class="immersive-editor-top-bar__vol">
        <text class="immersive-editor-top-bar__vol-text">{{ props.volNo }}</text>
        <view class="immersive-editor-top-bar__vol-rule" />
      </view>

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
    radial-gradient(110% 140% at 50% 0%, rgba(255, 255, 255, 0.72) 0%, rgba(255, 255, 255, 0) 58%),
    linear-gradient(180deg, rgba(238, 241, 243, 0.96) 0%, rgba(238, 241, 243, 0) 100%);
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
  letter-spacing: 2rpx;
  color: #879197;
  font-style: italic;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Times New Roman', serif;
}

.immersive-editor-top-bar__vol-rule {
  width: 56rpx;
  height: 1rpx;
  background: rgba(135, 145, 151, 0.35);
}

.immersive-editor-top-bar__close-hit {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.immersive-editor-top-bar__close-icon {
  position: relative;
  width: 34rpx;
  height: 34rpx;
}

.immersive-editor-top-bar__close-line {
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 2rpx;
  border-radius: 999rpx;
  background: #5a646a;
}

.immersive-editor-top-bar__close-line-a {
  transform: translateY(-50%) rotate(45deg);
}

.immersive-editor-top-bar__close-line-b {
  transform: translateY(-50%) rotate(-45deg);
}
</style>
