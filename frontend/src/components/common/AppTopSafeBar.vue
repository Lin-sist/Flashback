<script setup lang="ts">
import { computed, useSlots } from 'vue'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'

const props = withDefaults(
  defineProps<{
    title?: string
    transparent?: boolean
    paddingX?: string
    sideWidth?: string
    centerTitle?: boolean
  }>(),
  {
    title: '',
    transparent: false,
    paddingX: '40rpx',
    sideWidth: '88rpx',
    centerTitle: false,
  }
)

const { navBarHeight, navBarTotalHeight, rightSafeWidth, statusBarHeight, windowWidth } = useWechatNavMetrics()
const slots = useSlots()

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
  if (props.centerTitle) {
    const inPixels = (size: string) => parseFloat(size) * (size.endsWith('rpx') ? windowWidth.value / 750 : 1)
    const padding = inPixels(props.paddingX)
    const side = inPixels(props.sideWidth)
    const reserve = Math.max(padding + (slots.left ? side : 0), padding + rightSafeWidth.value + (slots.right ? side : 0))
    return { left: `${reserve}px`, right: `${reserve}px`, width: 'auto', transform: 'translateY(-50%)' }
  }
  // 分别计算左右可用边界；胶囊只在右侧，不能再从左右各扣一遍。
  return {
    left: slots.left ? `calc(${props.paddingX} + ${props.sideWidth})` : props.paddingX,
    right: `calc(${props.paddingX} + ${rightSafeWidth.value}px${slots.right ? ` + ${props.sideWidth}` : ''})`,
    width: 'auto',
    transform: 'translateY(-50%)',
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
  color: var(--fb-color-primary, #b5352a);
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}
</style>
