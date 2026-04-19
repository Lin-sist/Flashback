<script setup lang="ts">
import { computed } from 'vue'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'

const props = withDefaults(
  defineProps<{
    title?: string
    transparent?: boolean
    paddingX?: string
    sideWidth?: string
  }>(),
  {
    title: '',
    transparent: false,
    paddingX: '40rpx',
    sideWidth: '88rpx',
  }
)

const { navBarHeight, navBarTotalHeight, rightSafeWidth, statusBarHeight } = useWechatNavMetrics()

const wrapperStyle = computed(() => ({
  minHeight: `${navBarTotalHeight.value}px`,
  paddingTop: `${statusBarHeight.value}px`,
}))

const navStyle = computed(() => ({
  height: `${navBarHeight.value}px`,
}))

const sideStyle = computed(() => ({
  width: props.sideWidth,
  minWidth: props.sideWidth,
}))

const leftRailStyle = computed(() => ({
  left: props.paddingX,
}))

const rightRailStyle = computed(() => ({
  right: `calc(${props.paddingX} + ${rightSafeWidth.value}px)`,
}))

const titleStyle = computed(() => {
  const titleSafeReserve = `calc(${props.paddingX} + ${props.sideWidth} + ${rightSafeWidth.value}px)`

  return {
    width: `calc(100% - (${titleSafeReserve}) - (${titleSafeReserve}))`,
  }
})
</script>

<template>
  <view class="app-top-safe-bar" :class="{ transparent: props.transparent }" :style="wrapperStyle">
    <view class="app-top-safe-bar__nav" :style="navStyle">
      <view class="app-top-safe-bar__inner">
        <view
          class="app-top-safe-bar__side app-top-safe-bar__side-left"
          :style="[sideStyle, leftRailStyle]"
        >
          <slot name="left" />
        </view>

        <view class="app-top-safe-bar__title-wrap" :style="titleStyle">
          <slot name="title">
            <text class="app-top-safe-bar__title">{{ props.title }}</text>
          </slot>
        </view>

        <view
          class="app-top-safe-bar__side app-top-safe-bar__side-right"
          :style="[sideStyle, rightRailStyle]"
        >
          <slot name="right" />
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.app-top-safe-bar {
  position: relative;
  z-index: 20;
  background: transparent;
}

.app-top-safe-bar.transparent {
  background: transparent;
}

.app-top-safe-bar__nav {
  position: relative;
  display: flex;
  align-items: center;
}

.app-top-safe-bar__inner {
  position: relative;
  width: 100%;
  height: 100%;
}

.app-top-safe-bar__side {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  min-height: 80rpx;
  display: flex;
  align-items: center;
  z-index: 2;
}

.app-top-safe-bar__side-right {
  justify-content: flex-end;
}

.app-top-safe-bar__title-wrap {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  z-index: 1;
  pointer-events: none;
}

.app-top-safe-bar__title {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
  font-size: 34rpx;
  font-weight: 500;
  letter-spacing: 4rpx;
  color: var(--fb-color-primary);
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}
</style>
